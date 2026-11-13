package com.hiyako.app.ui.screens

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.NexusService
import com.hiyako.app.data.GitHubApi
import com.hiyako.app.data.WorkspaceManager
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.data.db.ChatEntity
import com.hiyako.app.data.db.ConversationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.regex.Pattern

data class Message(
    val content: String, 
    val isUser: Boolean, 
    val isLoading: Boolean = false,
    val type: String = "TEXT"
)
data class AiModel(val name: String, val id: String)

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val conversationDao = db.conversationDao()

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow<Int?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private val _isBuilding = MutableStateFlow(false)
    val isBuilding = _isBuilding.asStateFlow()
    
    private val _chatTitle = MutableStateFlow("New Chat")
    val chatTitle = _chatTitle.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModel("Default (Auto)", ""))
    val selectedModel = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AiModel>>(listOf(AiModel("Loading models...", "")))
    val availableModels = _availableModels.asStateFlow()
    private val _filteredModels = MutableStateFlow<List<AiModel>>(emptyList())
    val filteredModels = _filteredModels.asStateFlow()

    private var currentRequestId: Long = 0

    init {
        // مراقبة قائمة المحادثات
        viewModelScope.launch {
            conversationDao.getAllConversations().collectLatest { convos ->
                _conversations.value = convos
                if (convos.isEmpty()) {
                    createNewConversation()
                } else if (_activeConversationId.value == null) {
                    selectConversation(convos.first().id)
                }
            }
        }
        
        // مراقبة رسائل المحادثة النشطة
        viewModelScope.launch {
            _activeConversationId.collectLatest { id ->
                if (id != null) {
                    _chatTitle.value = conversationDao.getAllConversations().let { it } // Simplified for now
                    chatDao.getMessagesForChat(id).collectLatest { entities ->
                        _messages.value = entities.map { entity ->
                            val isThinking = entity.role == "ai" && entity.content == "Thinking..."
                            Message(entity.content, entity.role == "user", isLoading = isThinking, type = entity.messageType)
                        }
                    }
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val newId = conversationDao.insertConversation(ConversationEntity(title = "New Chat"))
            _activeConversationId.value = newId.toInt()
        }
    }

    fun selectConversation(id: Int) {
        _activeConversationId.value = id
    }

    fun clearChat() {
        val currentId = _activeConversationId.value ?: return
        viewModelScope.launch {
            chatDao.clearChat(currentId)
            conversationDao.updateTitle(currentId, "New Chat")
        }
    }

    fun setReady() { }
    fun selectModel(model: AiModel) { _selectedModel.value = model }
    
    fun onModelsReceived(jsonString: String) {
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            val modelsList = mutableListOf<AiModel>()
            modelsList.add(AiModel("Default (Auto)", ""))
            for (i in 0 until jsonArray.length()) {
                val id = jsonArray.getJSONObject(i).optString("id")
                if (id.isNotBlank()) modelsList.add(AiModel(jsonArray.getJSONObject(i).optString("name", id), id))
            }
            _availableModels.value = modelsList.sortedBy { it.name }
            _filteredModels.value = _availableModels.value
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun filterModels(query: String) {
        _filteredModels.value = if (query.isBlank()) _availableModels.value else _availableModels.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun sendMessage(text: String, webView: android.webkit.WebView?, forceType: String? = null) {
        val currentId = _activeConversationId.value ?: return
        if (text.isBlank()) return
        
        val msgType = forceType ?: when {
            text.startsWith("ارسم", ignoreCase = true) -> "IMAGE"
            text.startsWith("سويلي صورة", ignoreCase = true) -> "IMAGE"
            text.startsWith("اصنع صورة", ignoreCase = true) -> "IMAGE"
            text.startsWith("generate an image", ignoreCase = true) -> "IMAGE"
            text.startsWith("create an image", ignoreCase = true) -> "IMAGE"
            else -> "TEXT"
        }

        executeSendMessage(text, webView, msgType, currentId)
    }

    private fun executeSendMessage(text: String, webView: android.webkit.WebView?, type: String, conversationId: Int) {
        if (_messages.value.isEmpty() || _messages.value.size == 1) {
            viewModelScope.launch { conversationDao.updateTitle(conversationId, text.take(25) + if (text.length > 25) "..." else "") }
        }

        val intent = Intent(getApplication(), NexusService::class.java)
        ContextCompat.startForegroundService(getApplication(), intent)

        viewModelScope.launch {
            chatDao.insertMessage(ChatEntity(conversationId = conversationId, role = "user", content = text))
            chatDao.insertMessage(ChatEntity(conversationId = conversationId, role = "ai", content = "Thinking...", messageType = type)) 
        }

        val js: String = when (type) {
            "IMAGE" -> "window.generateImage(\"${text.replace("\"", "\\\"")}\");"
            else -> {
                var fullPrompt = ""
                if (WorkspaceManager.isLoaded) {
                    val mentionedFiles = WorkspaceManager.projectFiles.filterKeys { path ->
                        text.contains(path.substringAfterLast("/")) || text.contains(path)
                    }
                    if (mentionedFiles.isNotEmpty()) {
                        fullPrompt += "Here are the files:\n"
                        mentionedFiles.forEach { (path, content) -> fullPrompt += "--- $path ---\n$content\n" }
                    } else {
                        fullPrompt += "Project files:\n" + WorkspaceManager.projectFiles.keys.joinToString("\n")
                    }
                    fullPrompt += "\nUser Request: $text\nOutput FULL code with [FILE: path] format if needed."
                } else {
                    fullPrompt = text
                }
                "window.askPuter(\"${fullPrompt.replace("\"", "\\\"").replace("\n", "\\n")}\", \"${_selectedModel.value.id}\");"
            }
        }
        webView?.evaluateJavascript(js, null)
    }

    fun stopGeneration() {
        val currentId = _activeConversationId.value ?: return
        currentRequestId++
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            chatDao.deleteThinkingMessage(currentId)
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "⏹️ Generation stopped.", messageType = "TEXT"))
        }
    }

    fun startMultiAgent(appIdea: String, customRoles: List<String>, webView: android.webkit.WebView?) { /*...*/ }
    fun startGhostMode(errorLog: String, webView: android.webkit.WebView?) { /*...*/ }

    fun onImageReceived(url: String) {
        val currentId = _activeConversationId.value ?: return
        currentRequestId++
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val imageFile = File(context.filesDir, "gen_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    if (url.startsWith("data:image")) {
                        val base64Data = url.substringAfter(",")
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        imageFile.writeBytes(imageBytes)
                    } else if (url.startsWith("http")) {
                        val client = OkHttpClient()
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { input ->
                                    imageFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                        }
                    }
                }
                chatDao.deleteThinkingMessage(currentId)
                chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = imageFile.absolutePath, messageType = "IMAGE"))
            } catch (e: Exception) {
                chatDao.deleteThinkingMessage(currentId)
                chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "❌ Error: ${e.message}", messageType = "TEXT"))
            }
        }
    }

    fun onResponse(res: String) {
        val currentId = _activeConversationId.value ?: return
        currentRequestId++
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            chatDao.deleteThinkingMessage(currentId)
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = res, messageType = "TEXT"))
        }
    }

    fun applyChanges(token: String, aiResponse: String, context: Context) { /*...*/ }

    private fun sendNotification(title: String, message: String) { /*...*/ }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onImageReceived(url: String) { vm.onImageReceived(url) }
    @JavascriptInterface fun onVideoReceived(url: String) { /*...*/ }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

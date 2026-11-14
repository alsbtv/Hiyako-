package com.hiyako.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.NexusService
import com.hiyako.app.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.Locale

data class Message(val content: String, val isUser: Boolean, val isLoading: Boolean = false, val type: String = "TEXT", val attachmentUri: String? = null, val senderName: String? = null)
data class AiModel(val name: String, val id: String)

class AiViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val conversationDao = db.conversationDao()
    private val memoryDao = db.memoryDao()
    private val prefs = application.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
    private var tts: TextToSpeech? = null

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations = _conversations.asStateFlow()
    private val _activeConversationId = MutableStateFlow<Int?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _chatTitle = MutableStateFlow("New Chat")
    val chatTitle = _chatTitle.asStateFlow()
    private val _selectedModel = MutableStateFlow(AiModel("Default (Auto)", ""))
    val selectedModel = _selectedModel.asStateFlow()
    private val _availableModels = MutableStateFlow<List<AiModel>>(emptyList())
    val availableModels = _availableModels.asStateFlow()
    private val _memories = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val memories = _memories.asStateFlow()

    var isGroupChatMode by mutableStateOf(false)
    var selectedGroupModels by mutableStateOf<List<AiModel>>(emptyList())
    var isMultiSelectMode by mutableStateOf(false)
    val selectedChatIds = androidx.compose.runtime.mutableStateListOf<Int>()
    var isThinkingMode by mutableStateOf(false)
    var isSearchMode by mutableStateOf(false)
    var themeMode by mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark")
    var fontSize by mutableStateOf(prefs.getFloat("fontSize", 1f))

    init {
        tts = TextToSpeech(application, this)
        viewModelScope.launch {
            conversationDao.getAllConversations().collectLatest { convos ->
                _conversations.value = convos
                if (convos.isEmpty() && _activeConversationId.value == null) {
                    _activeConversationId.value = conversationDao.insertConversation(ConversationEntity(title = "New Chat")).toInt()
                } else if (_activeConversationId.value == null) { _activeConversationId.value = convos.first().id }
            }
        }
        viewModelScope.launch { memoryDao.getAllMemories().collectLatest { _memories.value = it } }
        viewModelScope.launch {
            _activeConversationId.collectLatest { id ->
                if (id != null) {
                    conversationDao.getAllConversations().collectLatest { convos ->
                        val currentConvo = convos.find { it.id == id }
                        _chatTitle.value = currentConvo?.title ?: "New Chat"
                        isGroupChatMode = currentConvo?.isGroup ?: false
                    }
                    chatDao.getMessagesForChat(id).collectLatest { entities ->
                        _messages.value = entities.map { entity -> Message(entity.content, entity.role == "user", isLoading = entity.content == "Thinking...", type = entity.messageType, attachmentUri = entity.attachmentUri, senderName = entity.senderName) }
                    }
                } else { _messages.value = emptyList() }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    fun setReady() { }
    fun selectModel(model: AiModel) { _selectedModel.value = model }
    fun toggleThink() { isThinkingMode = !isThinkingMode }
    fun toggleSearch() { isSearchMode = !isSearchMode }
    
    fun createNewConversation(isGroup: Boolean = false) { 
        viewModelScope.launch { _activeConversationId.value = conversationDao.insertConversation(ConversationEntity(title = if(isGroup) "New Group" else "New Chat", isGroup = isGroup)).toInt() } 
    }
    
    fun selectConversation(id: Int) { _activeConversationId.value = id }
    fun deleteChat(id: Int) { viewModelScope.launch { conversationDao.deleteConversation(id); if (_activeConversationId.value == id) _activeConversationId.value = null } }
    fun renameChat(id: Int, newTitle: String) { viewModelScope.launch { conversationDao.updateTitle(id, newTitle) } }
    fun togglePin(id: Int, isPinned: Boolean) { viewModelScope.launch { conversationDao.updatePin(id, isPinned) } }
    fun clearAllChats() { viewModelScope.launch { chatDao.clearAllMessages(); conversationDao.deleteAllConversations(); _activeConversationId.value = null } }
    fun addMemory(content: String) { viewModelScope.launch { memoryDao.insertMemory(MemoryEntity(content = content)) } }
    fun clearMemories() { viewModelScope.launch { memoryDao.clearMemories() } }
    fun updateTheme(mode: String) { themeMode = mode; prefs.edit().putString("theme", mode).apply() }
    fun updateFontSize(size: Float) { fontSize = size; prefs.edit().putFloat("fontSize", size).apply() }

    fun onModelsReceived(jsonString: String) {
        try {
            val jsonArray = JSONArray(jsonString)
            val modelsList = mutableListOf<AiModel>()
            modelsList.add(AiModel("Default (Auto)", ""))
            for (i in 0 until jsonArray.length()) {
                val id = jsonArray.getJSONObject(i).optString("id")
                if (id.isNotBlank()) modelsList.add(AiModel(jsonArray.getJSONObject(i).optString("name", id), id))
            }
            _availableModels.value = modelsList.sortedBy { it.name }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun sendMessage(text: String, attachmentUri: Uri?, attachmentType: String?, webView: android.webkit.WebView?) {
        val currentId = _activeConversationId.value ?: return
        if (text.isBlank() && attachmentUri == null) return

        if (_messages.value.isEmpty()) webView?.evaluateJavascript("window.generateTitle(\"${text.take(50).replace("\"", "\\\"")}\");", null)

        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), NexusService::class.java))

        viewModelScope.launch {
            var finalText = text
            var finalType = "TEXT"
            if (attachmentUri != null) {
                if (attachmentType?.startsWith("image/") == true) {
                    finalType = "IMAGE"
                    val base64Image = encodeImageToBase64(attachmentUri)
                    finalText = "[IMAGE_BASE64:$base64Image] $text"
                } else { finalType = "FILE"; finalText = "File Attached: $text" }
            }

            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "user", content = text, messageType = finalType, attachmentUri = attachmentUri.toString()))
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "Thinking...", messageType = "TEXT")) 
            
            val memoryPrompt = if (_memories.value.isNotEmpty()) "User Memories:\n" + _memories.value.joinToString("\n") { "- ${it.content}" } + "\n\n" else ""
            var finalPrompt = memoryPrompt + finalText
            if (isThinkingMode) finalPrompt = "Think step by step before answering.\n$finalPrompt"
            if (isSearchMode) finalPrompt = "Use web search if needed.\n$finalPrompt"
            
            if (isGroupChatMode && selectedGroupModels.isNotEmpty()) {
                val firstModel = selectedGroupModels.first().id
                val js = "window.askPuter(\"${finalPrompt.replace("\"", "\\\"").replace("\n", "\\n")}\", \"$firstModel\");"
                webView?.evaluateJavascript(js, null)
            } else {
                val js = "window.askPuter(\"${finalPrompt.replace("\"", "\\\"").replace("\n", "\\n")}\", \"${_selectedModel.value.id}\");"
                webView?.evaluateJavascript(js, null)
            }
        }
    }

    fun regenerateResponse(webView: android.webkit.WebView?) {
        val currentId = _activeConversationId.value ?: return
        val lastUserMsg = _messages.value.lastOrNull { it.isUser }?.content ?: return
        viewModelScope.launch {
            chatDao.deleteThinkingMessage(currentId)
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "Thinking...", messageType = "TEXT"))
        }
        var finalPrompt = lastUserMsg
        if (isThinkingMode) finalPrompt = "Think step by step before answering.\n$lastUserMsg"
        val js = "window.askPuter(\"${finalPrompt.replace("\"", "\\\"").replace("\n", "\\n")}\", \"${_selectedModel.value.id}\");"
        webView?.evaluateJavascript(js, null)
    }

    fun onTitleGenerated(title: String) {
        val currentId = _activeConversationId.value ?: return
        if (title.isNotBlank()) viewModelScope.launch { conversationDao.updateTitle(currentId, title) }
    }

    fun onResponse(res: String) {
        val currentId = _activeConversationId.value ?: return
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            chatDao.deleteThinkingMessage(currentId)
            if (res.contains("[GENERATE_FILE:")) {
                val fileName = res.substringAfter("[GENERATE_FILE:").substringBefore("]")
                val content = res.substringAfter("]").trim()
                createLocalFile(fileName, content)
                chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "تم إنشاء الملف: $fileName", messageType = "TEXT"))
            } else {
                chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = res, messageType = "TEXT"))
            }
        }
    }

    private fun createLocalFile(fileName: String, content: String) {
        val dir = getApplication<Application>().getExternalFilesDir(null)
        val file = File(dir, fileName)
        file.writeText(content)
    }

    private suspend fun encodeImageToBase64(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: ByteArray(0)
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    override fun onCleared() { tts?.shutdown(); super.onCleared() }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
    @JavascriptInterface fun onTitleGenerated(title: String) { vm.onTitleGenerated(title) }
}

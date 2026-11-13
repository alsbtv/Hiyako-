package com.hiyako.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.NexusService
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

data class Message(val content: String, val isUser: Boolean, val isLoading: Boolean = false, val type: String = "TEXT")
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
    
    private val _chatTitle = MutableStateFlow("New Chat")
    val chatTitle = _chatTitle.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModel("Default (Auto)", ""))
    val selectedModel = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AiModel>>(emptyList())
    val availableModels = _availableModels.asStateFlow()
    private val _filteredModels = MutableStateFlow<List<AiModel>>(emptyList())
    val filteredModels = _filteredModels.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    init {
        viewModelScope.launch {
            conversationDao.getAllConversations().collectLatest { convos ->
                _conversations.value = convos
                if (_activeConversationId.value == null) {
                    if (convos.isEmpty()) {
                        val newId = conversationDao.insertConversation(ConversationEntity(title = "New Chat"))
                        _activeConversationId.value = newId.toInt()
                    } else {
                        _activeConversationId.value = convos.first().id
                    }
                }
            }
        }
        
        viewModelScope.launch {
            _activeConversationId.collectLatest { id ->
                if (id != null) {
                    conversationDao.getAllConversations().collectLatest { convos ->
                        _chatTitle.value = convos.find { it.id == id }?.title ?: "New Chat"
                    }
                    chatDao.getMessagesForChat(id).collectLatest { entities ->
                        _messages.value = entities.map { entity ->
                            Message(entity.content, entity.role == "user", isLoading = entity.content == "Thinking...", type = entity.messageType)
                        }
                    }
                } else { _messages.value = emptyList() }
            }
        }
    }

    fun setReady() { }
    fun toggleThink() { _isThinking.value = !_isThinking.value }
    fun selectModel(model: AiModel) { _selectedModel.value = model }
    fun createNewConversation() { viewModelScope.launch { _activeConversationId.value = conversationDao.insertConversation(ConversationEntity(title = "New Chat")).toInt() } }
    fun selectConversation(id: Int) { _activeConversationId.value = id }
    fun deleteChat(id: Int) { viewModelScope.launch { conversationDao.deleteConversation(id); if (_activeConversationId.value == id) _activeConversationId.value = null } }
    fun renameChat(id: Int, newTitle: String) { viewModelScope.launch { conversationDao.updateTitle(id, newTitle) } }
    fun clearChat() { val id = _activeConversationId.value ?: return; viewModelScope.launch { chatDao.clearChat(id) } }

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

    fun sendMessage(text: String, webView: android.webkit.WebView?) {
        val currentId = _activeConversationId.value ?: return
        if (text.isBlank()) return

        // التسمية بالذكاء الاصطناعي إذا كانت المحادثة جديدة
        if (_messages.value.isEmpty()) {
            webView?.evaluateJavascript("window.generateTitle(\"${text.replace("\"", "\\\"")}\");", null)
        }

        val intent = Intent(getApplication(), NexusService::class.java)
        ContextCompat.startForegroundService(getApplication(), intent)

        viewModelScope.launch {
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "user", content = text))
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = "Thinking...", messageType = "TEXT")) 
        }

        var finalPrompt = text
        if (_isThinking.value) finalPrompt = "Think step by step before answering.\n$text"
        
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
            chatDao.insertMessage(ChatEntity(conversationId = currentId, role = "ai", content = res, messageType = "TEXT"))
        }
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
    @JavascriptInterface fun onTitleGenerated(title: String) { vm.onTitleGenerated(title) }
}

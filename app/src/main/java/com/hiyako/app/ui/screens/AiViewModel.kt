package com.hiyako.app.ui.screens

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.data.GitHubApi
import com.hiyako.app.data.WorkspaceManager
import com.hiyako.app.data.db.AppDatabase
import com.hiyako.app.data.db.ChatEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class Message(
    val content: String, 
    val isUser: Boolean, 
    val isLoading: Boolean = false, 
    val hasCodeChanges: Boolean = false, 
    val changedFiles: List<String> = emptyList()
)
data class AiModel(val name: String, val id: String)

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _isReady = MutableStateFlow(false)
    
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

    init {
        viewModelScope.launch {
            chatDao.getAllMessages().collect { entities ->
                _messages.value = entities.map { Message(it.content, it.role == "user", hasCodeChanges = it.hasCodeChanges) }
            }
        }
    }

    fun setReady() { _isReady.value = true }
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

    fun sendMessage(text: String, webView: android.webkit.WebView?) {
        if (text.isBlank() || !_isReady.value) return
        
        // تعيين عنوان الشات بناءً على أول رسالة
        if (_messages.value.isEmpty() || _messages.value.size == 1) {
            _chatTitle.value = text.take(25) + if (text.length > 25) "..." else ""
        }

        viewModelScope.launch {
            chatDao.insertMessage(ChatEntity(role = "user", content = text))
            chatDao.insertMessage(ChatEntity(role = "ai", content = "Thinking...")) 
        }

        // بناء السياق بطريقة أذكى (نرسل هيكل الملفات فقط لتخفيف الحمل إذا كان المشروع كبير)
        var fullPrompt = ""
        if (WorkspaceManager.isLoaded) {
            fullPrompt += "You are an expert Android Developer modifying '${WorkspaceManager.activeRepo}'.\n"
            fullPrompt += "Reply with FULL modified code of any file you change using this format:\n[FILE: path/to/filename.kt]\n```kotlin\n<CODE>\n```\n"
            
            // إرسال مسارات الملفات فقط كمرجع، وتفاصيل الملفات المطلوبة فقط لتقليل الحمل
            fullPrompt += "The project contains the following files:\n"
            WorkspaceManager.projectFiles.keys.forEach { path -> fullPrompt += "- $path\n" }
            fullPrompt += "\nIf you need to see the content of a specific file to make the changes, ask me first. Otherwise, assume standard implementations.\n"
            fullPrompt += "User Request: $text\n"
        } else {
            fullPrompt = text
        }

        val safeText = fullPrompt.replace("\"", "\\\"").replace("\n", "\\n")
        val js = "window.askPuter(\"$safeText\", \"${_selectedModel.value.id}\");"
        webView?.evaluateJavascript(js, null)
    }

    fun onResponse(res: String) {
        viewModelScope.launch {
            // استخراج الملفات المعدلة باستخدام Regex
            val changedFiles = mutableListOf<String>()
            val pattern = Pattern.compile("\\[FILE:\\s*(.*?)\\]")
            val matcher = pattern.matcher(res)
            while (matcher.find()) {
                matcher.group(1)?.let { changedFiles.add(it.trim()) }
            }

            val hasChanges = changedFiles.isNotEmpty()
            val newMsg = ChatEntity(role = "ai", content = res, hasCodeChanges = hasChanges)
            
            chatDao.clearChat()
            chatDao.insertMessage(ChatEntity(role = "user", content = "...")) 
            chatDao.insertMessage(newMsg)
            
            // إرسال إشعار تفصيلي
            if (hasChanges) {
                sendNotification("Nexus AI", "Modified ${changedFiles.size} files.")
            } else {
                sendNotification("Nexus AI", "New response received.")
            }
        }
    }

    fun applyChanges(token: String, aiResponse: String, context: Context) {
        if (WorkspaceManager.activeRepo.isBlank()) return
        _isBuilding.value = true
        val parts = WorkspaceManager.activeRepo.split("/")
        val owner = parts[0]
        val repo = parts[1]

        val pattern = Pattern.compile("\\[FILE:\\s*(.*?)\\]\\s*```[a-zA-Z]*\\n([\\s\\S]*?)```")
        val matcher = pattern.matcher(aiResponse)
        
        val filesToUpdate = mutableMapOf<String, String>()
        while (matcher.find()) {
            val path = matcher.group(1)?.trim() ?: continue
            val code = matcher.group(2)?.trim() ?: continue
            filesToUpdate[path] = code
        }

        if (filesToUpdate.isEmpty()) {
            _isBuilding.value = false
            return
        }

        viewModelScope.launch {
            var successCount = 0
            filesToUpdate.forEach { (path, code) ->
                val success = GitHubApi.updateFile(token, owner, repo, path, code, "AI Auto-Update: Modified $path")
                if (success) successCount++
            }
            
            _isBuilding.value = false
            val successMsg = "✅ Successfully updated $successCount files on GitHub."
            chatDao.insertMessage(ChatEntity(role = "ai", content = successMsg))
            sendNotification("Build Started", "Pushed $successCount files. GitHub Actions is building...")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nexus_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus AI", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

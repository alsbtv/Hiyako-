package com.hiyako.app.ui.screens

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class Message(
    val content: String, 
    val isUser: Boolean, 
    val isLoading: Boolean = false
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
                _messages.value = entities.map { Message(it.content, it.role == "user") }
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
        
        if (_messages.value.isEmpty() || _messages.value.size == 1) {
            _chatTitle.value = text.take(25) + if (text.length > 25) "..." else ""
        }

        val intent = Intent(getApplication(), NexusService::class.java)
        ContextCompat.startForegroundService(getApplication(), intent)

        viewModelScope.launch {
            // إضافة رسالة المستخدم فقط، دون مسح القديم
            chatDao.insertMessage(ChatEntity(role = "user", content = text))
            chatDao.insertMessage(ChatEntity(role = "ai", content = "Thinking...")) 
        }

        var fullPrompt = ""
        if (WorkspaceManager.isLoaded) {
            fullPrompt += "You are an expert Android Developer modifying '${WorkspaceManager.activeRepo}'.\n"
            fullPrompt += "When you perform actions, you MUST list them before the code using EXACT tags:\n"
            fullPrompt += "[READ: path]\n[MODIFY: path]\n[ADD: path]\n[DELETE: path]\n\n"
            
            // الذكاء الاصطناعي: إرسال الملفات المذكورة في البرومبت فقط
            val mentionedFiles = WorkspaceManager.projectFiles.filterKeys { path ->
                text.contains(path.substringAfterLast("/")) || text.contains(path) || text.contains(path.substringAfterLast("/").removeSuffix(".kt"))
            }

            if (mentionedFiles.isNotEmpty()) {
                fullPrompt += "Here are the FULL contents of the files you need to look at:\n\n"
                mentionedFiles.forEach { (path, content) ->
                    fullPrompt += "--- START FILE: $path ---\n$content\n--- END FILE: $path ---\n\n"
                }
            } else {
                // إذا لم يذكر ملفاً، نرسل قائمة الملفات فقط ليطلب ما يريد
                fullPrompt += "The project contains the following files:\n"
                WorkspaceManager.projectFiles.keys.forEach { path -> fullPrompt += "- $path\n" }
                fullPrompt += "\nPlease identify which files you need to see, or proceed if you already know.\n"
            }
            
            fullPrompt += "User Request: $text\n"
            fullPrompt += "Output FULL modified code using this format:\n[FILE: path]\n```kotlin\n<CODE>\n```\n"
        } else {
            fullPrompt = text
        }

        val safeText = fullPrompt.replace("\"", "\\\"").replace("\n", "\\n")
        val js = "window.askPuter(\"$safeText\", \"${_selectedModel.value.id}\");"
        webView?.evaluateJavascript(js, null)
    }

    fun onResponse(res: String) {
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))

        viewModelScope.launch {
            // تحديث رسالة "التفكير" بالرد الفعلي دون مسح التاريخ
            // في قاعدة البيانات، نقوم بمسح آخر رسالة "Thinking..." ونضع الرد الجديد
            // للتبسيط، سنقوم فقط بإضافة الرد (سيتم إضافة تحسين لاحقاً لإزالة رسالة Thinking من الـ DB)
            
            // لإصلاح المشكلة، سنقوم بإضافة الرد فقط
            chatDao.deleteThinkingMessage()
            chatDao.insertMessage(ChatEntity(role = "ai", content = res))
            sendNotification("Nexus AI", "New response received.")
        }
    }

    fun applyChanges(token: String, aiResponse: String, context: Context) {
        if (WorkspaceManager.activeRepo.isBlank()) return
        _isBuilding.value = true
        val parts = WorkspaceManager.activeRepo.split("/")
        val owner = parts[0]
        val repo = parts[1]

        val intent = Intent(context, NexusService::class.java)
        ContextCompat.startForegroundService(context, intent)

        val filePattern = Pattern.compile("\\[FILE:\\s*(.*?)\\]\\s*```[a-zA-Z]*\\n([\\s\\S]*?)```")
        val fileMatcher = filePattern.matcher(aiResponse)
        val filesToUpdate = mutableMapOf<String, String>()
        while (fileMatcher.find()) {
            val path = fileMatcher.group(1)?.trim() ?: continue
            val code = fileMatcher.group(2)?.trim() ?: continue
            filesToUpdate[path] = code
        }

        val deletePattern = Pattern.compile("\\[DELETE:\\s*(.*?)\\]")
        val deleteMatcher = deletePattern.matcher(aiResponse)
        val filesToDelete = mutableListOf<String>()
        while (deleteMatcher.find()) {
            deleteMatcher.group(1)?.trim()?.let { filesToDelete.add(it) }
        }

        if (filesToUpdate.isEmpty() && filesToDelete.isEmpty()) {
            _isBuilding.value = false
            context.stopService(Intent(context, NexusService::class.java))
            return
        }

        viewModelScope.launch {
            var successCount = 0
            filesToUpdate.forEach { (path, code) ->
                if (GitHubApi.updateFile(token, owner, repo, path, code, "AI Auto-Update: Modified $path")) successCount++
            }
            filesToDelete.forEach { path ->
                if (GitHubApi.deleteFile(token, owner, repo, path, "AI Auto-Update: Deleted $path")) successCount++
            }
            
            _isBuilding.value = false
            context.stopService(Intent(context, NexusService::class.java))
            val successMsg = "✅ Successfully processed $successCount files on GitHub."
            chatDao.insertMessage(ChatEntity(role = "ai", content = successMsg))
            sendNotification("Build Started", "Pushed changes. Building...")
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

        notificationManager.notify(2, notification)
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

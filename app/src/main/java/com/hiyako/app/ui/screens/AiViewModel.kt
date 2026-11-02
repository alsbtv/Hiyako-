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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            try {
                chatDao.getAllMessages().collect { entities ->
                    _messages.value = entities.map { entity ->
                        val isThinking = entity.role == "ai" && entity.content == "Thinking..."
                        Message(
                            content = entity.content, 
                            isUser = entity.role == "user", 
                            isLoading = isThinking,
                            type = entity.messageType
                        )
                    }
                }
            } catch (e: Exception) {
                chatDao.clearChat()
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

    fun sendMessage(text: String, webView: android.webkit.WebView?, forceType: String? = null) {
        if (text.isBlank() || !_isReady.value) return
        
        if (_messages.value.isEmpty() || _messages.value.size == 1) {
            _chatTitle.value = text.take(25) + if (text.length > 25) "..." else ""
        }

        val intent = Intent(getApplication(), NexusService::class.java)
        ContextCompat.startForegroundService(getApplication(), intent)

        val isImageRequest = forceType == "IMAGE" || text.contains("image", true) || text.contains("صورة", true) || text.contains("ارسم", true)
        val isVideoRequest = forceType == "VIDEO" || text.contains("video", true) || text.contains("فيديو", true)
        val msgType = if (isImageRequest) "IMAGE" else if (isVideoRequest) "VIDEO" else "TEXT"

        viewModelScope.launch {
            chatDao.insertMessage(ChatEntity(role = "user", content = text))
            chatDao.insertMessage(ChatEntity(role = "ai", content = "Thinking...", messageType = msgType)) 
        }

        val js: String = when {
            isImageRequest -> "window.generateImage(\"${text.replace("\"", "\\\"")}\");"
            isVideoRequest -> "window.generateVideo(\"${text.replace("\"", "\\\"")}\");"
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

    // استلام الصورة: تحميلها وحفظها كملف محلي
    fun onImageReceived(url: String) {
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val imageFile = File(context.filesDir, "gen_${System.currentTimeMillis()}.png")
                
                withContext(Dispatchers.IO) {
                    if (url.startsWith("data:image")) {
                        // فك تشفير base64 وحفظ الملف
                        val base64Data = url.substringAfter(",")
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        imageFile.writeBytes(imageBytes)
                    } else if (url.startsWith("http")) {
                        // تحميل من الرابط
                        val client = OkHttpClient()
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { input ->
                                    imageFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            } else {
                                throw Exception("Failed to download image: ${response.code}")
                            }
                        }
                    } else {
                        throw Exception("Invalid image URL format")
                    }
                }
                
                chatDao.deleteThinkingMessage()
                chatDao.insertMessage(ChatEntity(role = "ai", content = imageFile.absolutePath, messageType = "IMAGE"))
                sendNotification("Nexus AI", "Image generated!")
            } catch (e: Exception) {
                e.printStackTrace()
                chatDao.deleteThinkingMessage()
                chatDao.insertMessage(ChatEntity(role = "ai", content = "❌ Error: ${e.message}", messageType = "TEXT"))
                sendNotification("Nexus AI", "Image generation failed.")
            }
        }
    }

    fun onResponse(res: String) {
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            chatDao.deleteThinkingMessage()
            chatDao.insertMessage(ChatEntity(role = "ai", content = res, messageType = "TEXT"))
            sendNotification("Nexus AI", "New response received.")
        }
    }

    fun onVideoReceived(url: String) {
        getApplication<Application>().stopService(Intent(getApplication(), NexusService::class.java))
        viewModelScope.launch {
            chatDao.deleteThinkingMessage()
            chatDao.insertMessage(ChatEntity(role = "ai", content = url, messageType = "VIDEO"))
            sendNotification("Nexus AI", "Video generated!")
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
            chatDao.insertMessage(ChatEntity(role = "ai", content = successMsg, messageType = "TEXT"))
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
    @JavascriptInterface fun onImageReceived(url: String) { vm.onImageReceived(url) }
    @JavascriptInterface fun onVideoReceived(url: String) { vm.onVideoReceived(url) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

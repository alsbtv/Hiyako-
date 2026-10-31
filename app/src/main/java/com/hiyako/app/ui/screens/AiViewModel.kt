package com.hiyako.app.ui.screens

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiyako.app.data.GitHubApi
import com.hiyako.app.data.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class Message(val content: String, val isUser: Boolean, val isLoading: Boolean = false, val hasCodeChanges: Boolean = false)
data class AiModel(val name: String, val id: String)

class AiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _isReady = MutableStateFlow(false)
    
    private val _isBuilding = MutableStateFlow(false)
    val isBuilding = _isBuilding.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModel("Default (Auto)", ""))
    val selectedModel = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<AiModel>>(listOf(AiModel("Loading models...", "")))
    val availableModels = _availableModels.asStateFlow()
    private val _filteredModels = MutableStateFlow<List<AiModel>>(emptyList())
    val filteredModels = _filteredModels.asStateFlow()

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
        _messages.value = _messages.value + Message(text, true)
        _messages.value = _messages.value + Message("", false, true)
        
        var fullPrompt = ""
        if (WorkspaceManager.isLoaded) {
            fullPrompt += "You are an expert Android Developer. You are modifying the project '${WorkspaceManager.activeRepo}'.\n"
            fullPrompt += "I will provide you with the current code. You must reply with the FULL modified code of any file you change.\n"
            fullPrompt += "CRITICAL INSTRUCTION: You must format your output EXACTLY like this for EACH file you modify:\n"
            fullPrompt += "[FILE: path/to/filename.kt]\n"
            fullPrompt += "```kotlin\n"
            fullPrompt += "<FULL FILE CODE HERE>\n"
            fullPrompt += "```\n\n"
            fullPrompt += "Here are the current files:\n"
            WorkspaceManager.projectFiles.forEach { (path, content) ->
                fullPrompt += "--- File: $path ---\n$content\n\n"
            }
            fullPrompt += "User Request: $text\n"
        } else {
            fullPrompt = text
        }

        val safeText = fullPrompt.replace("\"", "\\\"").replace("\n", "\\n")
        val js = "window.askPuter(\"$safeText\", \"${_selectedModel.value.id}\");"
        webView?.evaluateJavascript(js, null)
    }

    fun onResponse(res: String) {
        val hasChanges = res.contains("[FILE:")
        _messages.value = _messages.value.filter { !it.isLoading } + Message(res, false, hasCodeChanges = hasChanges)
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
            _messages.value = _messages.value + Message("✅ Successfully updated $successCount files on GitHub. The build should start automatically.", false)
        }
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

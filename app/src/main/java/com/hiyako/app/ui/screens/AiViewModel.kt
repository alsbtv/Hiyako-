package com.hiyako.app.ui.screens

import android.webkit.JavascriptInterface
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Message(val content: String, val isUser: Boolean, val isLoading: Boolean = false)
data class AiModel(val name: String, val id: String)

class AiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    
    private val _selectedModel = MutableStateFlow(AiModel("Default (Auto)", ""))
    val selectedModel = _selectedModel.asStateFlow()

    // البداية بنموذج افتراضي فقط، وبعدها يتم جلب الكل
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
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name", id)
                if (id.isNotBlank()) {
                    modelsList.add(AiModel(name, id))
                }
            }
            _availableModels.value = modelsList.sortedBy { it.name }
            _filteredModels.value = _availableModels.value
        } catch (e: Exception) {
            e.printStackTrace()
            _availableModels.value = listOf(AiModel("Error loading models", ""))
        }
    }

    fun filterModels(query: String) {
        _filteredModels.value = if (query.isBlank()) _availableModels.value else _availableModels.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun sendMessage(text: String, webView: android.webkit.WebView?) {
        if (text.isBlank() || !_isReady.value) return
        _messages.value = _messages.value + Message(text, true)
        _messages.value = _messages.value + Message("", false, true)
        
        val safeText = text.replace("\"", "\\\"").replace("\n", "\\n")
        val modelId = _selectedModel.value.id
        val js = "window.askPuter(\"$safeText\", \"$modelId\");"
        webView?.evaluateJavascript(js, null)
    }

    fun onResponse(res: String) {
        _messages.value = _messages.value.filter { !it.isLoading } + Message(res, false)
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onModelsReceived(json: String) { vm.onModelsReceived(json) }
}

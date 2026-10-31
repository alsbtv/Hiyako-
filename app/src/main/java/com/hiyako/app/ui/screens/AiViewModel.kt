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

    val availableModels = listOf(
        AiModel("GPT-4o", "gpt-4o"),
        AiModel("Claude 3.5 Sonnet", "claude-3-5-sonnet"),
        AiModel("Gemini 1.5 Pro", "google/gemini-1.5-pro"),
        AiModel("Llama 3 70B", "meta-llama/llama-3-70b-instruct"),
        AiModel("DeepSeek V3", "deepseek-chat"),
        AiModel("Mistral Large", "mistralai/mistral-large")
    )
    private val _filteredModels = MutableStateFlow(availableModels)
    val filteredModels = _filteredModels.asStateFlow()

    fun setReady() { _isReady.value = true }
    fun selectModel(model: AiModel) { _selectedModel.value = model }
    fun filterModels(query: String) {
        _filteredModels.value = if (query.isBlank()) availableModels else availableModels.filter { it.name.contains(query, ignoreCase = true) }
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
}

package com.hiyako.app.ui.screens

import android.webkit.JavascriptInterface
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Message(val content: String, val isUser: Boolean, val type: String = "TEXT", val isLoading: Boolean = false)

class AiViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private val _savedMedia = MutableStateFlow<List<String>>(emptyList())
    val savedMedia = _savedMedia.asStateFlow()

    private val _mode = MutableStateFlow("Chat")
    val mode = _mode.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)

    fun setMode(m: String) { 
        _mode.value = m 
        if (m != "Library") _messages.value = emptyList() 
    }
    
    fun newChat() { _messages.value = emptyList() }
    fun setReady() { _isReady.value = true }

    fun sendMessage(text: String, webView: android.webkit.WebView?) {
        if (text.isBlank() || !_isReady.value || _mode.value == "Library") return
        
        _messages.value = _messages.value + Message(text, true, _mode.value)
        _messages.value = _messages.value + Message("", false, _mode.value, true)
        
        val safeText = text.replace("\"", "\\\"").replace("\n", "\\n")
        
        val finalPrompt = when(_mode.value) {
            "Research" -> "You are a deep research AI. Provide a highly detailed, factual, and comprehensive answer with sources if possible: $safeText"
            "Writing" -> "You are an expert writer. Write a well-structured, professional, and detailed report about: $safeText"
            else -> safeText
        }

        val js = when(_mode.value) {
            "Images" -> "window.generateImage(\"$safeText\");"
            "Video" -> "window.generateVideo(\"$safeText\");"
            else -> "window.askPuter(\"$finalPrompt\");"
        }
        webView?.evaluateJavascript(js, null)
    }

    fun onResponse(res: String) { 
        _messages.value = _messages.value.filter { !it.isLoading } + Message(res, false, "TEXT") 
    }
    
    fun onImage(res: String) { 
        _messages.value = _messages.value.filter { !it.isLoading } + Message(res, false, "IMAGE")
        _savedMedia.value = _savedMedia.value + res
    }
    
    fun onVideo(res: String) { 
        _messages.value = _messages.value.filter { !it.isLoading } + Message(res, false, "VIDEO")
        _savedMedia.value = _savedMedia.value + res
    }
}

class AiBridge(private val vm: AiViewModel) {
    @JavascriptInterface fun onReady() { vm.setReady() }
    @JavascriptInterface fun onResponse(res: String) { vm.onResponse(res) }
    @JavascriptInterface fun onImage(res: String) { vm.onImage(res) }
    @JavascriptInterface fun onVideo(res: String) { vm.onVideo(res) }
}

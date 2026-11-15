package com.hiyako.app.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

interface PuterListener {
    fun onEngineReady()
    fun onModelsReceived(json: String)
    fun onChunk(reqId: String, text: String)
    fun onDone(reqId: String)
    fun onError(reqId: String, message: String)
    fun onTitle(convoId: Int, title: String)
    fun onAuth(signedIn: Boolean, username: String)
    fun provideImage(reqId: String): String
}

/**
 * Owns the hidden WebView that runs Puter.js. It lives as long as the ViewModel,
 * so switching screens never destroys the page or an in-flight request.
 */
class PuterEngine(context: Context, private val listener: PuterListener) {

    private val appContext: Context = context.applicationContext

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _popup = MutableStateFlow<WebView?>(null)
    val popup: StateFlow<WebView?> = _popup.asStateFlow()

    val webView: WebView

    init {
        webView = createWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val wv = WebView(appContext)
        wv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.setSupportMultipleWindows(true)
        wv.settings.javaScriptCanOpenWindowsAutomatically = true
        wv.webViewClient = WebViewClient()
        wv.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (resultMsg == null) return false
                val popupView = WebView(appContext)
                popupView.settings.javaScriptEnabled = true
                popupView.settings.domStorageEnabled = true
                popupView.settings.javaScriptCanOpenWindowsAutomatically = true
                popupView.webViewClient = WebViewClient()
                popupView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        closePopup()
                    }
                }
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = popupView
                resultMsg.sendToTarget()
                _popup.value = popupView
                return true
            }
        }
        wv.addJavascriptInterface(Bridge(), "AndroidBridge")
        wv.loadDataWithBaseURL(BASE_URL, PAGE_HTML, "text/html", "UTF-8", null)
        return wv
    }

    fun closePopup() {
        val p = _popup.value
        _popup.value = null
        if (p != null) {
            p.post {
                try {
                    p.stopLoading()
                    p.loadUrl("about:blank")
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun ask(reqId: String, messagesJson: String, model: String, hasImage: Boolean) {
        val js = "window.ask(" + JSONObject.quote(reqId) + "," + JSONObject.quote(messagesJson) + "," +
            JSONObject.quote(model) + "," + hasImage + ");"
        run(js)
    }

    fun makeTitle(convoId: Int, text: String) {
        run("window.makeTitle(" + convoId + "," + JSONObject.quote(text) + ");")
    }

    fun refreshModels() {
        run("window.fetchModels();")
    }

    fun checkAuth() {
        run("window.checkAuth();")
    }

    fun signIn() {
        run("window.signIn();")
    }

    fun reload() {
        _ready.value = false
        webView.post { webView.loadDataWithBaseURL(BASE_URL, PAGE_HTML, "text/html", "UTF-8", null) }
    }

    fun destroy() {
        try {
            webView.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun run(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }

    inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            _ready.value = true
            listener.onEngineReady()
        }

        @JavascriptInterface
        fun onModels(json: String) {
            listener.onModelsReceived(json)
        }

        @JavascriptInterface
        fun onChunk(reqId: String, text: String) {
            listener.onChunk(reqId, text)
        }

        @JavascriptInterface
        fun onDone(reqId: String) {
            listener.onDone(reqId)
        }

        @JavascriptInterface
        fun onError(reqId: String, message: String) {
            listener.onError(reqId, message)
        }

        @JavascriptInterface
        fun onTitle(convoId: String, title: String) {
            val id = convoId.toIntOrNull() ?: return
            listener.onTitle(id, title)
        }

        @JavascriptInterface
        fun onAuth(signedIn: String, username: String) {
            listener.onAuth(signedIn == "true", username)
        }

        @JavascriptInterface
        fun getImage(reqId: String): String {
            return listener.provideImage(reqId)
        }
    }

    companion object {
        // Keep this base URL identical to the old version so an existing Puter session in localStorage survives.
        private const val BASE_URL = "https://appassets.androidplatform.net"

        private val PAGE_HTML = """
            <html><head><meta charset='utf-8'><script src='https://js.puter.com/v2/'></script></head><body>
            <script>
            function errText(e) {
                try {
                    if (!e) return 'Unknown error';
                    if (typeof e === 'string') return e;
                    if (e.message) return e.message;
                    if (e.error && e.error.message) return e.error.message;
                    if (e.error && typeof e.error === 'string') return e.error;
                    return JSON.stringify(e);
                } catch (x) { return 'Unknown error'; }
            }
            function contentText(c) {
                if (c === null || c === undefined) return '';
                if (typeof c === 'string') return c;
                if (Array.isArray(c)) {
                    return c.map(function (p) { return (p && p.text) ? p.text : (typeof p === 'string' ? p : ''); }).join('');
                }
                if (c.text) return c.text;
                return JSON.stringify(c);
            }
            function respText(r) {
                if (r === null || r === undefined) return '';
                if (typeof r === 'string') return r;
                if (r.message) return contentText(r.message.content);
                if (r.text) return r.text;
                return JSON.stringify(r);
            }
            window.fetchModels = function () {
                try {
                    puter.ai.listModels().then(function (models) {
                        var result = models.map(function (m) {
                            if (typeof m === 'string') return { id: m, name: m };
                            return { id: m.id, name: m.name || m.id };
                        });
                        AndroidBridge.onModels(JSON.stringify(result));
                    }).catch(function (e) {});
                } catch (e) {}
            };
            window.ask = async function (reqId, msgsJson, model, hasImage) {
                try {
                    var msgs = JSON.parse(msgsJson);
                    var opts = {};
                    if (model) { opts.model = model; }
                    var resp;
                    if (hasImage) {
                        var img = AndroidBridge.getImage(reqId);
                        var prompt = contentText(msgs[msgs.length - 1].content);
                        resp = await puter.ai.chat(prompt, img, false, opts);
                    } else {
                        opts.stream = true;
                        resp = await puter.ai.chat(msgs, false, opts);
                    }
                    if (resp && resp.success === false) { throw resp; }
                    if (resp && typeof resp[Symbol.asyncIterator] === 'function') {
                        for await (var part of resp) {
                            if (part && part.text) { AndroidBridge.onChunk(reqId, part.text); }
                        }
                    } else {
                        var t = respText(resp);
                        if (t) { AndroidBridge.onChunk(reqId, t); }
                    }
                    AndroidBridge.onDone(reqId);
                } catch (e) {
                    AndroidBridge.onError(reqId, errText(e));
                }
            };
            window.makeTitle = function (convoId, text) {
                try {
                    var p = 'Write a short title (2 to 5 words) for a conversation that starts with the message below. Use the same language as the message. Output ONLY the title, with no quotes and no trailing punctuation.\n\nMessage: ' + String(text).substring(0, 300);
                    puter.ai.chat(p).then(function (r) {
                        var t = respText(r);
                        if (t) { AndroidBridge.onTitle(String(convoId), t.trim()); }
                    }).catch(function (e) {});
                } catch (e) {}
            };
            window.checkAuth = function () {
                try {
                    if (puter.auth.isSignedIn()) {
                        puter.auth.getUser().then(function (u) {
                            AndroidBridge.onAuth('true', (u && u.username) ? u.username : '');
                        }).catch(function (e) { AndroidBridge.onAuth('true', ''); });
                    } else {
                        AndroidBridge.onAuth('false', '');
                    }
                } catch (e) { AndroidBridge.onAuth('false', ''); }
            };
            window.signIn = function () {
                try {
                    puter.auth.signIn().then(function () {
                        window.checkAuth();
                        window.fetchModels();
                    }).catch(function (e) {});
                } catch (e) {}
            };
            (function boot() {
                if (typeof puter === 'undefined') { setTimeout(boot, 300); return; }
                AndroidBridge.onReady();
                window.fetchModels();
                window.checkAuth();
            })();
            </script></body></html>
        """.trimIndent()
    }
}

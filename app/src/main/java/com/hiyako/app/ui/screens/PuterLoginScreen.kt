package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hiyako.app.ui.components.GlassIconButton
import com.hiyako.app.ui.components.HiyakoButton
import com.hiyako.app.ui.components.appear
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary

private const val PUTER_SIGN_IN_URL = "https://puter.com/action/sign-in"

/**
 * Full-screen Puter sign-in page. Cookies are shared with the hidden AI engine, so tapping Done
 * reloads the engine and it picks up the new session.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PuterLoginScreen(onBack: () -> Unit, vm: AiViewModel) {
    var loading by remember { mutableStateOf(true) }
    val webHolder = remember { arrayOfNulls<WebView>(1) }

    BackHandler {
        val view = webHolder[0]
        if (view != null && view.canGoBack()) view.goBack() else onBack()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).appear(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "رجوع"), onBack)
            Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
                Text(
                    tr("Sign in to Puter", "تسجيل الدخول إلى Puter"),
                    color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Sign in on the page below, then tap Done", "سجّل الدخول من الصفحة أدناه ثم اضغط تم"),
                    color = TextMuted, fontSize = 12.sp
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(24.dp))) {
            AndroidView(
                factory = { ctx ->
                    val view = WebView(ctx)
                    view.settings.javaScriptEnabled = true
                    view.settings.domStorageEnabled = true
                    view.settings.javaScriptCanOpenWindowsAutomatically = true
                    view.settings.setSupportMultipleWindows(true)
                    val cookies = CookieManager.getInstance()
                    cookies.setAcceptCookie(true)
                    cookies.setAcceptThirdPartyCookies(view, true)

                    view.webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            v: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                            val popup = WebView(ctx)
                            popup.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(pv: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    view.loadUrl(url)
                                    return true
                                }
                            }
                            transport.webView = popup
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    view.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) { loading = true }
                        override fun onPageFinished(v: WebView?, url: String?) { loading = false }
                    }

                    view.loadUrl(PUTER_SIGN_IN_URL)
                    webHolder[0] = view
                    view
                },
                modifier = Modifier.fillMaxSize()
            )
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentBlue)
            }
        }

        HiyakoButton(
            text = tr("Done", "تم"),
            onClick = {
                vm.engine.reload()
                onBack()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

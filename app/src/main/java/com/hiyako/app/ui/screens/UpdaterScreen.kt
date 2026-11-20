package com.hiyako.app.ui.screens

import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiyako.app.ui.components.*
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun UpdaterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf(tr("Click below to fetch the latest APK from GitHub.", "اضغط بالأسفل لجلب أحدث نسخة من GitHub.")) }
    
    val apkUrl = "https://github.com/alsbtv/Hiyako-/releases/latest/download/app-debug.apk" 

    ScreenScaffold(tr("Updates", "التحديثات"), onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.CloudDownload, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("Hiyako Updater", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(statusText, color = TextSecondary)
            Spacer(Modifier.height(32.dp))

            HiyakoButton(
                text = tr("Download & Install", "تنزيل وتثبيت"),
                onClick = {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                        return@HiyakoButton
                    }
                    isDownloading = true
                    statusText = tr("Downloading update...", "جارٍ التنزيل...")
                    scope.launch(Dispatchers.IO) {
                        try {
                            val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
                            val response = client.newCall(Request.Builder().url(apkUrl).build()).execute()
                            response.use { resp ->
                                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                                val body = resp.body ?: error("Empty body")
                                if (body.contentLength() <= 0) error("Invalid APK size")
                                val file = File(context.cacheDir, "update.apk")
                                body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                                withContext(Dispatchers.Main) {
                                    statusText = tr("Download complete. Installing...", "اكتمل التنزيل. جارٍ التثبيت...")
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file), "application/vnd.android.package-archive")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(intent) } catch (e: Exception) { statusText = "No installer found." }
                                    isDownloading = false
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { statusText = "Error: ${e.message}"; isDownloading = false }
                        }
                    }
                },
                loading = isDownloading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

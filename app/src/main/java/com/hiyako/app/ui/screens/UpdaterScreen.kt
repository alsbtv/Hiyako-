package com.hiyako.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Click below to fetch the latest APK from GitHub.") }
    
    // يجب استبدال هذا برابط الـ Artifact المباشر من GitHub Actions
    // للأسف GitHub لا يوفر رابطاً مباشراً بدون مصادقة، لذا سنستخدم
    // رابط الـ Release (إن وجد) أو نطلب من المستخدم وضعه.
    val apkUrl = "https://github.com/alsbtv/Hiyako-/releases/latest/download/app-debug.apk" 

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            TopAppBar(
                title = { Text("App Updater", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CloudDownload, null, tint = AccentBlue, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("Nexus Auto-Updater", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(statusText, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    isDownloading = true
                    statusText = "Downloading update..."
                    scope.launch(Dispatchers.IO) {
                        try {
                            val client = OkHttpClient()
                            val request = Request.Builder().url(apkUrl).build()
                            val response = client.newCall(request).execute()
                            
                            if (response.isSuccessful) {
                                val body = response.body
                                val file = File(context.cacheDir, "update.apk")
                                val fos = FileOutputStream(file)
                                body?.byteStream()?.copyTo(fos)
                                fos.close()
                                
                                withContext(Dispatchers.Main) {
                                    statusText = "Download complete. Installing..."
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(intent)
                                    isDownloading = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    statusText = "Error: Could not download APK. Check URL."
                                    isDownloading = false
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                statusText = "Error: ${e.message}"
                                isDownloading = false
                            }
                        }
                    }
                },
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Download & Install Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

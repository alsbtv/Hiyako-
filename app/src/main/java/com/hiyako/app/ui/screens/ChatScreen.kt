package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled", "UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AiViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val savedMedia by vm.savedMedia.collectAsState()
    val mode by vm.mode.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val modes = listOf("Chat", "Images", "Video", "Research", "Writing", "Library")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = OledBlack, modifier = Modifier.width(280.dp)) {
                Text("Nexus AI", color = AccentCyan, modifier = Modifier.padding(16.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Divider(color = Color.DarkGray)
                
                Row(Modifier.fillMaxWidth().clickable { vm.newChat(); scope.launch { drawerState.close() } }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = AccentCyan)
                    Spacer(Modifier.width(12.dp))
                    Text("New Chat", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.DarkGray)

                modes.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { vm.setMode(item); scope.launch { drawerState.close() } }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when(item) {
                            "Chat" -> Icons.Default.Chat
                            "Images" -> Icons.Default.Image
                            "Video" -> Icons.Default.PlayArrow
                            "Research" -> Icons.Default.Search
                            "Writing" -> Icons.Default.Edit
                            else -> Icons.Default.List
                        }
                        Icon(icon, null, tint = if(mode==item) AccentCyan else TextSecondary)
                        Spacer(Modifier.width(12.dp))
                        Text(item, color = if(mode==item) AccentCyan else TextPrimary, fontSize = 16.sp)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            topBar = {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = TextPrimary) }
                    Text("Nexus: $mode", color = TextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
            },
            bottomBar = {
                if (mode != "Library") {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Row(Modifier.background(CardDark, RoundedCornerShape(8.dp)).clickable { showMenu = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(mode, color = TextPrimary, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(CardDark)) {
                                modes.filter { it != "Library" }.forEach { item ->
                                    DropdownMenuItem(text = { Text(item, color = TextPrimary) }, onClick = { vm.setMode(item); showMenu = false })
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Describe...", color = TextSecondary) }, shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = TextPrimary))
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { vm.sendMessage(input, webView); input = "" }, modifier = Modifier.background(AccentCyan, RoundedCornerShape(8.dp))) {
                            Icon(Icons.Default.Send, null, tint = Color.Black)
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (mode == "Library") {
                    if (savedMedia.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No media saved yet.", color = TextSecondary) }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedMedia) { url ->
                                if (url.contains(".mp4") || url.contains("video")) {
                                    Box(Modifier.aspectRatio(1f).background(CardDark), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayArrow, null, tint = AccentCyan, modifier = Modifier.size(48.dp))
                                    }
                                } else {
                                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.aspectRatio(1f), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                        items(messages) { msg ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                                if (msg.isLoading) {
                                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(24.dp))
                                } else {
                                    Box(Modifier.background(if (msg.isUser) AccentCyan else CardDark, RoundedCornerShape(8.dp)).padding(12.dp)) {
                                        if (msg.type == "IMAGE") {
                                            AsyncImage(model = msg.content, contentDescription = null, modifier = Modifier.size(200.dp), contentScale = ContentScale.Crop)
                                        } else if (msg.type == "VIDEO") {
                                            val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(msg.content)); prepare(); playWhenReady = true } }
                                            DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
                                            AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } }, modifier = Modifier.size(250.dp, 150.dp))
                                        } else {
                                            Text(msg.content, color = if (msg.isUser) Color.Black else TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            addJavascriptInterface(AiBridge(vm), "AndroidBridge")
                            val html = """
                                <html><head><script src='https://js.puter.com/v2/'></script></head><body>
                                <script>
                                    window.onload = function() { AndroidBridge.onReady(); };
                                    window.askPuter = function(p) { puter.ai.chat(p).then(r => AndroidBridge.onResponse(r.toString())).catch(e => AndroidBridge.onResponse('Error')); };
                                    window.generateImage = function(p) { puter.ai.txt2img(p).then(r => AndroidBridge.onImage(r.src)).catch(e => AndroidBridge.onImage('Error')); };
                                    window.generateVideo = function(p) { puter.ai.txt2vid(p).then(r => AndroidBridge.onVideo(r.src)).catch(e => AndroidBridge.onVideo('Error')); };
                                </script></body></html>
                            """.trimIndent()
                            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            webView = this
                        }
                    },
                    modifier = Modifier.size(0.dp)
                )
            }
        }
    }
}

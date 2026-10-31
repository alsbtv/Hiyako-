package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.components.ChatInputBar
import com.hiyako.app.ui.components.MessageBubble
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AiViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val filteredModels by vm.filteredModels.collectAsState()
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showModelSheet by remember { mutableStateOf(false) }
    var searchModelQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = OledBlack, modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = "", onValueChange = {}, placeholder = { Text("Search chat content...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = CardDark, unfocusedBorderColor = Color.Transparent),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth().clickable { scope.launch { drawerState.close() } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = TextPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text("New Chat", color = TextPrimary, fontSize = 16.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Divider(color = CardDark)
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(TextSecondary, CircleShape), contentAlignment = Alignment.Center) { Text("W", color = Color.Black) }
                        Spacer(Modifier.width(12.dp))
                        Text("بسام هرار", color = TextPrimary, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.MoreVert, null, tint = TextPrimary)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            modifier = Modifier.imePadding().fillMaxSize(),
            topBar = {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null, tint = TextPrimary) }
                    Row {
                        IconButton(onClick = { }) { Icon(Icons.Default.VolumeOff, null, tint = TextPrimary) }
                        IconButton(onClick = { }) { Icon(Icons.Default.AddCircleOutline, null, tint = TextPrimary) }
                    }
                }
            },
            bottomBar = {
                Column(Modifier.padding(bottom = 8.dp)) {
                    ChatInputBar(input = input, onInputChange = { input = it }, onSend = { vm.sendMessage(input, webView); input = "" }, isLoading = false)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        AssistChip(onClick = { showModelSheet = true }, label = { Text(selectedModel.name, color = AccentBlue) }, leadingIcon = { Icon(Icons.Default.Language, null, tint = AccentBlue, modifier = Modifier.size(16.dp)) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (messages.isEmpty()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hi! What's on your mind today?", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                        items(messages) { msg ->
                            MessageBubble(text = msg.content, isUser = msg.isUser, isLoading = msg.isLoading)
                        }
                    }
                }

                AndroidView(
                    factory = { ctx -> WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        addJavascriptInterface(AiBridge(vm), "AndroidBridge")
                        val html = """
                            <html><head><script src='https://js.puter.com/v2/'></script></head><body>
                            <script>
                                window.onload = function() {
                                    let check = setInterval(() => { 
                                        if (typeof puter !== 'undefined') { 
                                            clearInterval(check); 
                                            AndroidBridge.onReady(); 
                                            window.fetchModels();
                                        } 
                                    }, 500);
                                };
                                window.fetchModels = function() {
                                    puter.ai.listModels().then(models => {
                                        let result = models.map(m => ({ id: m.id, name: m.name || m.id }));
                                        AndroidBridge.onModelsReceived(JSON.stringify(result));
                                    }).catch(e => AndroidBridge.onModelsReceived("[]"));
                                };
                                window.askPuter = function(p, m) {
                                    let opts = m ? { model: m } : {};
                                    puter.ai.chat(p, opts)
                                        .then(r => {
                                            let text = "";
                                            if (r && r.message && r.message.content) text = r.message.content;
                                            else if (r && r.result && r.result.message && r.result.message.content) text = r.result.message.content;
                                            else text = r.toString();
                                            AndroidBridge.onResponse(text);
                                        })
                                        .catch(e => AndroidBridge.onResponse('Error: ' + e));
                                };
                            </script></body></html>
                        """.trimIndent()
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        webView = this
                    }}, modifier = Modifier.size(1.dp).alpha(0f)
                )

                if (showModelSheet) {
                    ModalBottomSheet(onDismissRequest = { showModelSheet = false }, containerColor = CardDark) {
                        Column(Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                            Text("Select Model (500+ Available)", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = searchModelQuery, onValueChange = { searchModelQuery = it; vm.filterModels(it) },
                                placeholder = { Text("Search models...", color = TextSecondary) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                            LazyColumn {
                                items(filteredModels) { model ->
                                    Row(Modifier.fillMaxWidth().clickable { vm.selectModel(model); showModelSheet = false }.padding(16.dp)) {
                                        Text(model.name, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

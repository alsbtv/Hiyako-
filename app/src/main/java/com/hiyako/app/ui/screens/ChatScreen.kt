package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Message
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.ui.components.ChatInputBar
import com.hiyako.app.ui.components.MessageBubble
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onNavigateToSettings: () -> Unit = {}, vm: AiViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val filteredModels by vm.filteredModels.collectAsState()
    val isThinking by vm.isThinking.collectAsState()
    val isAnyLoading = messages.any { it.isLoading }
    val activeConversationId by vm.activeConversationId.collectAsState()
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showLoginView by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var searchModelQuery by remember { mutableStateOf("") }
    var expandedMenuId by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DeepSurface, modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = "", onValueChange = {}, placeholder = { Text("Search...", color = TextMuted, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = CardDark, unfocusedBorderColor = Color.Transparent),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().clickable { vm.createNewConversation(); scope.launch { drawerState.close() } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("New Chat", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = BorderColor)

                    LazyColumn(Modifier.weight(1f)) {
                        conversations.groupBy { getGroupName(it.timestamp) }.forEach { (groupName, chats) ->
                            item { Text(groupName, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)) }
                            items(chats) { chat ->
                                Box {
                                    Row(Modifier.fillMaxWidth().clickable { vm.selectConversation(chat.id); scope.launch { drawerState.close() } }.padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        if (activeConversationId == chat.id) Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        IconButton(onClick = { expandedMenuId = chat.id }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandedMenuId == chat.id,
                                        onDismissRequest = { expandedMenuId = null },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        DropdownMenuItem(text = { Text("Rename") }, onClick = { vm.renameChat(chat.id, "Renamed Chat"); expandedMenuId = null })
                                        DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { vm.deleteChat(chat.id); expandedMenuId = null })
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = BorderColor)
                    Row(Modifier.fillMaxWidth().clickable { scope.launch { drawerState.close() }; onNavigateToSettings() }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(TextSecondary, CircleShape), contentAlignment = Alignment.Center) { Text("W", color = Color.Black, fontSize = 14.sp) }
                        Spacer(Modifier.width(12.dp))
                        Text("Settings", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack, modifier = Modifier.imePadding().fillMaxSize(),
            topBar = {
                if (!showLoginView) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) { 
                            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(24.dp)) 
                        }
                        Row(Modifier.clickable { showModelSheet = true }, verticalAlignment = Alignment.CenterVertically) {
                            Text(chatTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
                        }
                        IconButton(onClick = { showLoginView = true }, modifier = Modifier.size(36.dp)) { 
                            Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(24.dp)) 
                        }
                    }
                }
            },
            bottomBar = {
                if (!showLoginView) {
                    ChatInputBar(
                        input = input, 
                        onInputChange = { input = it }, 
                        onSend = { vm.sendMessage(input, webView); input = "" }, 
                        onStop = { }, 
                        isLoading = isAnyLoading,
                        onAttachClick = { },
                        isThinking = isThinking,
                        onThinkClick = { vm.toggleThink() }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(OledBlack)) {
                if (!showLoginView) {
                    if (messages.isEmpty()) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("كيف أساعدك اليوم؟", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                            items(messages) { msg -> MessageBubble(text = msg.content, isUser = msg.isUser, isLoading = msg.isLoading, type = msg.type) }
                        }
                    }
                }
                
                if (showModelSheet) {
                    ModalBottomSheet(onDismissRequest = { showModelSheet = false }, containerColor = CardDark) {
                        Column(Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                            Text("Select Model", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = searchModelQuery, onValueChange = { searchModelQuery = it; vm.filterModels(it) }, placeholder = { Text("Search models...") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            LazyColumn {
                                items(filteredModels) { model ->
                                    Row(Modifier.fillMaxWidth().clickable { vm.selectModel(model); showModelSheet = false }.padding(vertical = 12.dp)) { 
                                        Text(model.name, color = TextPrimary, fontSize = 15.sp) 
                                    }
                                }
                            }
                        }
                    }
                }

                AndroidView(
                    factory = { ctx -> WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {}
                        addJavascriptInterface(AiBridge(vm), "AndroidBridge")
                        val html = """
                            <html><head><script src='https://js.puter.com/v2/'></script></head><body>
                            <script>
                                window.onload = function() { let check = setInterval(() => { if (typeof puter !== 'undefined') { clearInterval(check); AndroidBridge.onReady(); window.fetchModels(); } }, 500); };
                                window.fetchModels = function() { puter.ai.listModels().then(models => { let result = models.map(m => ({ id: m.id, name: m.name || m.id })); AndroidBridge.onModelsReceived(JSON.stringify(result)); }).catch(e => {}); };
                                window.generateTitle = function(p) {
                                    puter.ai.chat("Generate a 2-4 word title for a chat that starts with: " + p + ". Output ONLY the title, no quotes.").then(r => {
                                        let text = (typeof r === 'string') ? r : (r.message ? r.message.content : "");
                                        if(text) AndroidBridge.onTitleGenerated(text.trim());
                                    }).catch(e => {});
                                };
                                window.askPuter = function(p, m) {
                                    let timer = setTimeout(() => { AndroidBridge.onResponse("Error: Timeout."); }, 60000);
                                    let opts = { stream: false };
                                    if (m && m !== "") opts.model = m;
                                    puter.ai.chat(p, opts).then(r => { clearTimeout(timer); let text = (typeof r === 'string') ? r : (r.message ? r.message.content : JSON.stringify(r)); AndroidBridge.onResponse(text); }).catch(e => { clearTimeout(timer); AndroidBridge.onResponse('Error: ' + JSON.stringify(e)); });
                                };
                            </script></body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://appassets.androidplatform.net", html, "text/html", "UTF-8", null)
                        webView = this
                    }}, modifier = Modifier.size(1.dp).alpha(0f)
                )

                if (showLoginView) {
                    AndroidView(
                        factory = { ctx -> WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadUrl("https://puter.com/action/sign-in")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url?.contains("dashboard") == true || url?.contains("app") == true) {
                                        showLoginView = false
                                        webView?.evaluateJavascript("window.fetchModels();", null)
                                    }
                                }
                            }
                        }}, modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

fun getGroupName(timestamp: Long): String {
    val now = Calendar.getInstance(); val msgDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    if (now.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == msgDate.get(Calendar.DAY_OF_YEAR)) return "Today"
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (now.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == msgDate.get(Calendar.DAY_OF_YEAR)) return "Yesterday"
    return "Older"
}

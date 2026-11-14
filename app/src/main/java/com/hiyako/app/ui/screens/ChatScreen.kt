package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Message
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onNavigateToSettings: () -> Unit = {}, vm: AiViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentType by remember { mutableStateOf<String?>(null) }
    
    val messages by vm.messages.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    val activeConversationId by vm.activeConversationId.collectAsState()
    val isAnyLoading = messages.any { it.isLoading }
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showLoginView by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var expandedMenuId by remember { mutableStateOf<Int?>(null) }

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { selectedAttachmentUri = uri; selectedAttachmentType = "file" }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { selectedAttachmentUri = uri; selectedAttachmentType = "image" }
    }

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
                    Row(Modifier.fillMaxWidth().clickable { vm.createNewConversation(isGroup = true); scope.launch { drawerState.close() } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("New Group Chat", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = BorderColor)

                    LazyColumn(Modifier.weight(1f)) {
                        items(conversations) { chat ->
                            Box {
                                Row(
                                    Modifier.fillMaxWidth().background(if (vm.selectedChatIds.contains(chat.id)) SecondarySurface else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { 
                                            if (vm.isMultiSelectMode) {
                                                if (vm.selectedChatIds.contains(chat.id)) vm.selectedChatIds.remove(chat.id) else vm.selectedChatIds.add(chat.id)
                                            } else {
                                                vm.selectConversation(chat.id); scope.launch { drawerState.close() }
                                            }
                                        }.padding(vertical = 10.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (vm.isMultiSelectMode) {
                                        Checkbox(checked = vm.selectedChatIds.contains(chat.id), onCheckedChange = { 
                                            if (it) vm.selectedChatIds.add(chat.id) else vm.selectedChatIds.remove(chat.id)
                                        }, colors = CheckboxDefaults.colors(checkedColor = AccentBlue))
                                    } else {
                                        if (chat.isPinned) { Icon(Icons.Default.PushPin, null, tint = TextSecondary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                                        Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { expandedMenuId = chat.id }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                                    }
                                }
                                DropdownMenu(expanded = expandedMenuId == chat.id, onDismissRequest = { expandedMenuId = null }, properties = PopupProperties(focusable = true)) {
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { vm.renameChat(chat.id, "Renamed Chat"); expandedMenuId = null })
                                    DropdownMenuItem(text = { Text(if (chat.isPinned) "Unpin" else "Pin") }, onClick = { vm.togglePin(chat.id, !chat.isPinned); expandedMenuId = null })
                                    DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { vm.deleteChat(chat.id); expandedMenuId = null })
                                }
                            }
                        }
                    }

                    if (vm.isMultiSelectMode) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { vm.selectedChatIds.forEach { vm.togglePin(it, true) }; vm.isMultiSelectMode = false; vm.selectedChatIds.clear() }) { Text("Pin", color = TextSecondary) }
                            TextButton(onClick = { vm.selectedChatIds.forEach { vm.deleteChat(it) }; vm.isMultiSelectMode = false; vm.selectedChatIds.clear() }) { Text("Delete", color = Color.Red) }
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
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Text(chatTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        IconButton(onClick = { showLoginView = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    }
                }
            },
            bottomBar = {
                if (!showLoginView) {
                    Column {
                        if (selectedAttachmentUri != null) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📎 Attachment ready: ${selectedAttachmentUri!!.lastPathSegment}", color = AccentBlue, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedAttachmentUri = null; selectedAttachmentType = null }) { Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        ChatInputBar(
                            input = input, 
                            onInputChange = { input = it }, 
                            onSend = { vm.sendMessage(input, selectedAttachmentUri, selectedAttachmentType, webView); input = ""; selectedAttachmentUri = null }, 
                            isLoading = isAnyLoading,
                            onAttachClick = { showAttachmentSheet = true },
                            isThinking = vm.isThinkingMode,
                            onThinkClick = { vm.toggleThink() },
                            isSearch = vm.isSearchMode,
                            onSearchClick = { vm.toggleSearch() }
                        )
                    }
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
                            items(messages) { msg -> 
                                MessageBubble(text = msg.content, isUser = msg.isUser, isLoading = msg.isLoading, type = msg.type, attachmentUri = msg.attachmentUri, senderName = msg.senderName, onRegenerate = { vm.regenerateResponse(webView) }) 
                            }
                        }
                    }
                }
                
                if (showAttachmentSheet) {
                    ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }, containerColor = CardDark) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAttachmentSheet = false; imagePickerLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                                    Text("Image", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAttachmentSheet = false; filePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                                    Text("File", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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

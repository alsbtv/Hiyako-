package com.hiyako.app.ui.screens

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Message
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hiyako.app.data.WorkspaceManager
import com.hiyako.app.ui.components.ChatInputBar
import com.hiyako.app.ui.components.EmptyStateGrid
import com.hiyako.app.ui.components.MessageBubble
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToWorkspace: () -> Unit = {},
    onNavigateToUpdater: () -> Unit = {},
    vm: AiViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val filteredModels by vm.filteredModels.collectAsState()
    val isBuilding by vm.isBuilding.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showModelSheet by remember { mutableStateOf(false) }
    var searchModelQuery by remember { mutableStateOf("") }
    var showLoginView by remember { mutableStateOf(false) }
    var forceType by remember { mutableStateOf<String?>(null) }
    
    var showAgentConfig by remember { mutableStateOf(false) }
    var appIdeaInput by remember { mutableStateOf("") }
    var agentsInput by remember { mutableStateOf("Architect, UI Designer, Logic Developer, Reviewer") }

    var showGhostModeInput by remember { mutableStateOf(false) }
    var ghostErrorInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = OledBlack, modifier = Modifier.width(280.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = "", onValueChange = {}, placeholder = { Text("Search chat content...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = CardDark, unfocusedBorderColor = Color.Transparent),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Row(Modifier.fillMaxWidth().clickable { 
                        scope.launch { drawerState.close() }
                        onNavigateToWorkspace() 
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Workspace (GitHub)", color = AccentBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Row(Modifier.fillMaxWidth().clickable { 
                        scope.launch { drawerState.close() }
                        onNavigateToUpdater() 
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Update, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Update App", color = AccentBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Row(Modifier.fillMaxWidth().clickable { 
                        scope.launch { drawerState.close() }
                        showLoginView = true 
                        webView?.evaluateJavascript("window.startSignIn();", null)
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Puter Account / Sign In", color = TextPrimary, fontSize = 15.sp)
                    }

                    Spacer(Modifier.weight(1f))
                    Divider(color = CardDark)
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(TextSecondary, CircleShape), contentAlignment = Alignment.Center) { Text("W", color = Color.Black, fontSize = 14.sp) }
                        Spacer(Modifier.width(12.dp))
                        Text("بسام هرار", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.MoreVert, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            modifier = Modifier.imePadding().fillMaxSize(),
            topBar = {
                if (!showLoginView) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) { 
                                Icon(Icons.Default.Menu, null, tint = TextPrimary, modifier = Modifier.size(24.dp)) 
                            }
                            Text(chatTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            
                            IconButton(onClick = { 
                                showLoginView = true 
                                webView?.evaluateJavascript("window.startSignIn();", null)
                            }, modifier = Modifier.size(36.dp)) { 
                                Icon(Icons.Default.AccountCircle, null, tint = TextPrimary, modifier = Modifier.size(24.dp)) 
                            }
                        }
                        
                        if (isBuilding) {
                            Row(Modifier.fillMaxWidth().background(CardDark).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Uploading changes to GitHub...", color = AccentBlue, fontSize = 12.sp)
                            }
                        } else if (WorkspaceManager.isLoaded) {
                            Row(Modifier.fillMaxWidth().background(CardDark).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Active: ${WorkspaceManager.activeRepo}", color = AccentBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!showLoginView) {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        ChatInputBar(input = input, onInputChange = { input = it }, onSend = { vm.sendMessage(input, webView, forceType); input = ""; forceType = null }, isLoading = false)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            AssistChip(
                                onClick = { showModelSheet = true }, 
                                label = { Text(selectedModel.name, color = AccentBlue, fontSize = 12.sp) }, 
                                leadingIcon = { Icon(Icons.Default.Language, null, tint = AccentBlue, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (!showLoginView) {
                    if (messages.isEmpty()) {
                        EmptyStateGrid { action ->
                            when(action) {
                                "Build App" -> {
                                    appIdeaInput = "Build an app that "
                                    showAgentConfig = true
                                }
                                "Ghost Fix" -> {
                                    showGhostModeInput = true
                                }
                                "Image" -> {
                                    forceType = "IMAGE"
                                    input = "Generate an image of "
                                }
                                "Video" -> {
                                    forceType = "VIDEO"
                                    input = "Generate a video of "
                                }
                                "Deep Research" -> input = "Do deep research on "
                                "Writing" -> input = "Write me an article about "
                            }
                        }
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                            items(messages) { msg ->
                                MessageBubble(
                                    text = msg.content, 
                                    isUser = msg.isUser, 
                                    isLoading = msg.isLoading,
                                    type = msg.type
                                )
                            }
                        }
                    }
                }

                if (showAgentConfig) {
                    AlertDialog(
                        onDismissRequest = { showAgentConfig = false },
                        title = { Text("Configure AI Agents", color = TextPrimary) },
                        text = {
                            Column {
                                Text("App Idea:", color = TextSecondary, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = appIdeaInput,
                                    onValueChange = { appIdeaInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = AccentBlue)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("AI Agents (comma separated):", color = TextSecondary, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = agentsInput,
                                    onValueChange = { agentsInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = AccentBlue)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val roles = agentsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    vm.startMultiAgent(appIdeaInput, roles, webView)
                                    showAgentConfig = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Start Team")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAgentConfig = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = OledBlack
                    )
                }

                if (showGhostModeInput) {
                    AlertDialog(
                        onDismissRequest = { showGhostModeInput = false },
                        title = { Text("Ghost Mode (Auto-Fix)", color = TextPrimary) },
                        text = {
                            Column {
                                Text("Paste the build error log below:", color = TextSecondary, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ghostErrorInput,
                                    onValueChange = { ghostErrorInput = it },
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = CardDark,
                                        unfocusedContainerColor = CardDark,
                                        focusedBorderColor = AccentBlue
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    vm.startGhostMode(ghostErrorInput, webView)
                                    showGhostModeInput = false
                                    ghostErrorInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Fix Code")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGhostModeInput = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = OledBlack
                    )
                }

                AndroidView(
                    factory = { ctx -> WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean { return false }
                        }
                        
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                val context = view?.context ?: return false
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val popupWebView = WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean { return false }
                                    }
                                }
                                val dialog = Dialog(context).apply {
                                    setContentView(popupWebView)
                                    window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                                    setOnDismissListener { popupWebView.destroy() }
                                    show()
                                }
                                transport.webView = popupWebView
                                resultMsg.sendToTarget()
                                return true
                            }
                        }
                        
                        addJavascriptInterface(AiBridge(vm), "AndroidBridge")
                        val html = """
                            <html><head><script src='https://js.puter.com/v2/'></script></head><body style="background: #0F0F0F; color: white;">
                            <script>
                                window.onload = function() {
                                    let check = setInterval(() => { if (typeof puter !== 'undefined') { clearInterval(check); AndroidBridge.onReady(); window.fetchModels(); } }, 500);
                                };
                                window.startSignIn = function() {
                                    puter.auth.signIn().then(() => {
                                        AndroidBridge.onResponse('Signed in successfully!');
                                        window.fetchModels();
                                    }).catch(e => {
                                        AndroidBridge.onResponse('Sign in error: ' + JSON.stringify(e));
                                    });
                                };
                                window.fetchModels = function() {
                                    puter.ai.listModels().then(models => {
                                        let result = models.map(m => ({ id: m.id, name: m.name || m.id }));
                                        AndroidBridge.onModelsReceived(JSON.stringify(result));
                                    }).catch(e => AndroidBridge.onModelsReceived("[]"));
                                };
                                window.askPuter = function(p, m) {
                                    let opts = { stream: false };
                                    if (m && m !== "") opts.model = m;
                                    puter.ai.chat(p, opts)
                                        .then(r => {
                                            let text = (typeof r === 'string') ? r : (r.message ? r.message.content : (r.result ? r.result.message.content : JSON.stringify(r)));
                                            AndroidBridge.onResponse(text);
                                        }).catch(e => AndroidBridge.onResponse('Error: ' + JSON.stringify(e)));
                                };
                                window.generateImage = function(p) {
                                    puter.ai.txt2img(p).then(r => {
                                        if (r && r.src && typeof r.src === 'string') {
                                            AndroidBridge.onImageReceived(r.src);
                                        } else {
                                            AndroidBridge.onResponse('Error: Image generation failed or returned invalid URL.');
                                        }
                                    }).catch(e => AndroidBridge.onResponse('Error: ' + JSON.stringify(e)));
                                };
                                window.generateVideo = function(p) {
                                    AndroidBridge.onVideoReceived("Processing video..."); 
                                    puter.ai.txt2vid(p).then(r => {
                                        AndroidBridge.onResponse('Video generation is not supported natively yet via Puter.js API.');
                                    }).catch(e => AndroidBridge.onResponse('Error: ' + JSON.stringify(e)));
                                };
                            </script></body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://appassets.androidplatform.net", html, "text/html", "UTF-8", null)
                        webView = this
                    }}, 
                    modifier = if (showLoginView) Modifier.fillMaxSize() else Modifier.size(1.dp).alpha(0f)
                )

                if (showLoginView) {
                    IconButton(
                        onClick = { showLoginView = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).background(CardDark, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                if (showModelSheet) {
                    ModalBottomSheet(onDismissRequest = { showModelSheet = false }, containerColor = CardDark) {
                        Column(Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
                            Text("Select Model (500+)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = searchModelQuery, 
                                onValueChange = { searchModelQuery = it; vm.filterModels(it) }, 
                                placeholder = { Text("Search models...", color = TextSecondary, fontSize = 14.sp) }, 
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            LazyColumn {
                                items(filteredModels) { model ->
                                    Row(Modifier.fillMaxWidth().clickable { vm.selectModel(model); showModelSheet = false }.padding(vertical = 12.dp, horizontal = 4.dp)) { 
                                        Text(model.name, color = TextPrimary, fontSize = 15.sp) 
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

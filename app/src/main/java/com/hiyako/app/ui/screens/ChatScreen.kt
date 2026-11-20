package com.hiyako.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hiyako.app.data.db.ConversationEntity
import com.hiyako.app.ui.components.*
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*
import com.hiyako.app.util.queryDisplayName
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(48.dp).background(ElevatedSurface, RoundedCornerShape(24.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp), cursorBrush = SolidColor(AccentBlue),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = TextMuted, fontSize = 14.sp)
                    inner()
                }
            }
        )
    }
}

private fun dateBucket(ts: Long, now: Long = System.currentTimeMillis()): Int {
    val c1 = Calendar.getInstance().apply { timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val c2 = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val days = (c2.timeInMillis - c1.timeInMillis) / 86_400_000L
    return when { days <= 0 -> 0; days == 1L -> 1; days <= 7 -> 2; else -> 3 }
}

@Composable
private fun EmptyChat(onPick: (String) -> Unit) {
    val items = listOf(
        Icons.Outlined.Lightbulb to tr("Explain a complex idea simply", "اشرح لي فكرة معقدة ببساطة"),
        Icons.Outlined.Edit to tr("Help me write a message", "ساعدني في كتابة رسالة"),
        Icons.Outlined.CalendarToday to tr("Plan my day step by step", "رتّب لي يومي خطوة بخطوة"),
        Icons.Outlined.Code to tr("Write code for an idea", "اكتب كوداً لفكرة لدي")
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(140.dp).background(Brush.radialGradient(listOf(AccentBlue.copy(alpha = 0.30f), Color.Transparent))))
            Box(Modifier.size(60.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                Text("✦", color = OnAccent, fontSize = 26.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(tr("How can I help you today?", "كيف أقدر أساعدك اليوم؟"), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        items.forEachIndexed { i, (icon, text) ->
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(300, delayMillis = i * 80)) + slideInVertically(tween(300, delayMillis = i * 80)) { it / 4 }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(SecondarySurface).border(1.dp, BorderColor, RoundedCornerShape(18.dp)).clickable { onPick(text) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(text, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ConversationEntity, active: Boolean, onClick: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (active) SecondarySurface else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress).padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chat.isPinned) {
            Icon(Icons.Default.PushPin, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(onNavigateToSettings: () -> Unit = {}, vm: AiViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var stickToBottom by remember { mutableStateOf(true) }
    var showTopMenu by remember { mutableStateOf(false) }

    val messages by vm.messages.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    val activeId by vm.activeConversationId.collectAsState()
    val generating by vm.generating.collectAsState()
    val errors by vm.errors.collectAsState()

    val partial: String? = activeId?.let { generating[it] }
    val errorText: String? = activeId?.let { errors[it] }
    val isGenerating = partial != null
    val lastAiId: Int? = messages.lastOrNull()?.takeIf { !it.isUser }?.id
    val totalItems = messages.size + (if (partial != null) 1 else 0) + (if (errorText != null) 1 else 0)

    val attachmentMime = remember(attachmentUri) { attachmentUri?.let { context.contentResolver.getType(it) } ?: "" }
    val attachmentName = remember(attachmentUri) { attachmentUri?.let { queryDisplayName(context, it) } ?: "" }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) attachmentUri = uri }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) attachmentUri = uri }

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling -> if (!scrolling) stickToBottom = !listState.canScrollForward }
    }
    LaunchedEffect(activeId) { stickToBottom = true }
    LaunchedEffect(partial, totalItems, activeId) { if (stickToBottom && totalItems > 0) listState.animateScrollToItem(totalItems - 1) }

    val send: () -> Unit = {
        if (!vm.canSend()) {
            Toast.makeText(context, tr("Connecting to the AI engine. Check your internet and try again.", "جارٍ الاتصال بمحرك الذكاء الاصطناعي. تحقق من الإنترنت وحاول مرة أخرى."), Toast.LENGTH_SHORT).show()
        } else {
            vm.sendMessage(input, attachmentUri)
            input = ""
            attachmentUri = null
            stickToBottom = true
        }
    }

    val filteredConversations = remember(conversations, query) { if (query.isBlank()) conversations else conversations.filter { it.title.contains(query, true) } }
    val pinned = remember(filteredConversations) { filteredConversations.filter { it.isPinned } }
    val recent = remember(filteredConversations) { filteredConversations.filter { !it.isPinned } }
    val bucketLabels = listOf(tr("Today", "اليوم"), tr("Yesterday", "أمس"), tr("Last 7 days", "آخر 7 أيام"), tr("Older", "أقدم"))

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DeepSurface, modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = OnAccent, fontSize = 15.sp) }
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Hiyako", "هياكو"), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    SearchField(query, { query = it }, tr("Search chats", "ابحث في المحادثات"))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AccentBlue.copy(alpha = 0.15f)).clickable { vm.createNewConversation(); scope.launch { drawerState.close() } }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("New chat", "محادثة جديدة"), color = AccentBlue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(Modifier.weight(1f)) {
                        if (pinned.isNotEmpty()) {
                            item { Text(tr("Pinned", "المثبتة"), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp, start = 4.dp)) }
                            items(pinned, key = { "p${it.id}" }) { chat ->
                                ChatRow(chat, chat.id == activeId, onClick = { vm.selectConversation(chat.id); scope.launch { drawerState.close() } }, onLongPress = {})
                            }
                        }
                        val grouped = recent.groupBy { dateBucket(it.timestamp) }.toSortedMap()
                        grouped.forEach { (bucket, list) ->
                            item { Text(bucketLabels[bucket], color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp, start = 4.dp)) }
                            items(list, key = { "r${it.id}" }) { chat ->
                                ChatRow(chat, chat.id == activeId, onClick = { vm.selectConversation(chat.id); scope.launch { drawerState.close() } }, onLongPress = {})
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { scope.launch { drawerState.close() }; onNavigateToSettings() }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Text(vm.auth.value.username.firstOrNull()?.uppercase() ?: "?", color = OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Settings", "الإعدادات"), color = TextPrimary, fontSize = 15.sp)
                            Text(tr("Manage account", "إدارة الحساب"), color = TextMuted, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            topBar = {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, tr("Menu", "القائمة"), tint = TextPrimary) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(chatTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, tr("More", "المزيد"), tint = TextPrimary)
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(tr("Export chat", "تصدير المحادثة")) },
                                onClick = {
                                    showTopMenu = false
                                    val exported = messages.joinToString("\n\n") { m ->
                                        (if (m.isUser) tr("You", "أنت") else "AI") + ":\n" + m.content
                                    }
                                    if (exported.isBlank()) {
                                        Toast.makeText(context, tr("Nothing to export yet", "لا توجد رسائل بعد"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, exported)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, tr("Share", "مشاركة")))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error sharing", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Column(Modifier.background(OledBlack)) {
                    if (attachmentUri != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).background(SecondarySurface, RoundedCornerShape(14.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (attachmentMime.startsWith("image/")) {
                                AsyncImage(model = attachmentUri, contentDescription = "Attachment", modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(attachmentName, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { attachmentUri = null }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, tr("Remove", "إزالة"), tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    ComposerBar(
                        input = input, onInputChange = { input = it }, onSend = send, onStop = { vm.stopGeneration() },
                        isGenerating = isGenerating, canSend = input.isNotBlank() || attachmentUri != null, onAttachClick = { showAttachmentSheet = true },
                        isThinking = vm.isThinkingMode, onThinkClick = { vm.toggleThink() }, isSearch = vm.isSearchMode, onSearchClick = { vm.toggleSearch() }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (messages.isEmpty() && partial == null && errorText == null) {
                    EmptyChat(onPick = { input = it })
                } else {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                            items(messages, key = { it.id }) { msg ->
                                if (msg.isUser) UserMessage(msg.content, msg.type, msg.attachmentUri, msg.senderName)
                                else AssistantMessage(msg.content, showRegenerate = msg.id == lastAiId && !isGenerating, onRegenerate = { vm.regenerate() }, onSpeak = { vm.speak(msg.content) }, onReport = { vm.reportMessage(msg.id, msg.content) })
                            }
                            if (partial != null) {
                                item(key = "generating") { if (partial.isEmpty()) TypingDots() else AssistantMessage(text = partial, showActions = false) }
                            }
                            if (errorText != null) {
                                item(key = "error") { ErrorCard(message = errorText, onRetry = { vm.regenerate() }) }
                            }
                        }
                        val showJump by remember { derivedStateOf { listState.canScrollForward } }
                        AnimatedVisibility(
                            visible = showJump, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        ) {
                            SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0)); stickToBottom = true } }, containerColor = ElevatedSurface, contentColor = TextPrimary) {
                                Icon(Icons.Default.KeyboardArrowDown, tr("Scroll to bottom", "الانتقال للأسفل"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }, containerColor = CardDark) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAttachmentSheet = false; imagePicker.launch("image/*") }.padding(16.dp)) {
                    Icon(Icons.Default.Image, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                    Text(tr("Image", "صورة"), color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showAttachmentSheet = false; filePicker.launch("*/*") }.padding(16.dp)) {
                    Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                    Text(tr("File", "ملف"), color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

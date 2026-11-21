package com.hiyako.app.ui.screens

import com.hiyako.app.ui.theme.Brand
import androidx.compose.foundation.layout.statusBarsPadding
import com.hiyako.app.providers.AccountStore
import androidx.compose.foundation.gestures.scrollBy
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
        AuroraOrb(96.dp)
        Spacer(Modifier.height(16.dp))
        Text(tr("How can I help you today?", "كيف أقدر أساعدك اليوم؟"), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(chat: ConversationEntity, active: Boolean, onClick: () -> Unit, onLongPress: () -> Unit, selectMode: Boolean = false, selected: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (active || selected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress).padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectMode) {
            Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (selected) AccentBlue else TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        if (chat.isPinned) {
            Icon(Icons.Default.PushPin, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) DangerColor else AccentBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) DangerColor else TextPrimary, fontSize = 16.sp)
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

    val messages by vm.messages.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    val activeId by vm.activeConversationId.collectAsState()
    val generating by vm.generating.collectAsState()
    val errors by vm.errors.collectAsState()
    val auth by vm.auth.collectAsState()
    val models by vm.availableModels.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    var showModelSheet by remember { mutableStateOf(false) }
    var menuChat by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }

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
    LaunchedEffect(partial, totalItems, activeId) { if (stickToBottom && totalItems > 0) listState.scrollBy(100000f) }

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
                        Box(Modifier.size(36.dp).background(AccentBrush, CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = OnAccent, fontSize = 16.sp) }
                        Spacer(Modifier.width(10.dp))
                        Text(tr(Brand.EN, Brand.AR), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(14.dp))
                    SearchField(query, { query = it }, tr("Search chats", "ابحث في المحادثات"))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AccentBrush).clickable { vm.createNewConversation(); scope.launch { drawerState.close() } }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = OnAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("New chat", "محادثة جديدة"), color = OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (selectMode) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("${selectedIds.size} selected", "${selectedIds.size} محددة"), color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (selectedIds.isNotEmpty()) confirmDeleteSelected = true }) { Icon(Icons.Default.Delete, tr("Delete", "حذف"), tint = DangerColor) }
                            IconButton(onClick = { selectMode = false; selectedIds = emptySet() }) { Icon(Icons.Default.Close, tr("Cancel", "إلغاء"), tint = TextSecondary) }
                        }
                    }

                    LazyColumn(Modifier.weight(1f)) {
                        if (pinned.isNotEmpty()) {
                            item { Text(tr("Pinned", "المثبتة"), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp, start = 4.dp)) }
                            items(pinned, key = { "p${it.id}" }) { chat ->
                                ChatRow(chat, chat.id == activeId,
                                    onClick = {
                                        if (selectMode) selectedIds = if (chat.id in selectedIds) selectedIds - chat.id else selectedIds + chat.id
                                        else { vm.selectConversation(chat.id); scope.launch { drawerState.close() } }
                                    },
                                    onLongPress = { if (selectMode) selectedIds = selectedIds + chat.id else menuChat = chat },
                                    selectMode = selectMode, selected = chat.id in selectedIds)
                            }
                        }
                        val grouped = recent.groupBy { dateBucket(it.timestamp) }.toSortedMap()
                        grouped.forEach { (bucket, list) ->
                            item { Text(bucketLabels[bucket], color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp, start = 4.dp)) }
                            items(list, key = { "r${it.id}" }) { chat ->
                                ChatRow(chat, chat.id == activeId,
                                    onClick = {
                                        if (selectMode) selectedIds = if (chat.id in selectedIds) selectedIds - chat.id else selectedIds + chat.id
                                        else { vm.selectConversation(chat.id); scope.launch { drawerState.close() } }
                                    },
                                    onLongPress = { if (selectMode) selectedIds = selectedIds + chat.id else menuChat = chat },
                                    selectMode = selectMode, selected = chat.id in selectedIds)
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { scope.launch { drawerState.close() }; onNavigateToSettings() }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(38.dp).background(AccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                            Text(auth.username.firstOrNull()?.uppercase() ?: "?", color = OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            containerColor = Color.Transparent,
            topBar = {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(Icons.Default.Menu, tr("Menu", "القائمة"), { scope.launch { drawerState.open() } })
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(chatTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(AccentBlue.copy(alpha = 0.16f)).border(1.dp, AccentBlue.copy(alpha = 0.40f), RoundedCornerShape(50))
                                .clickable { showModelSheet = true }.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).background(AccentTwo, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedModel.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp))
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    GlassIconButton(Icons.Default.Add, tr("New chat", "محادثة جديدة"), { vm.createNewConversation() })
                }
            },
            bottomBar = {
                Column {
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
                            SmallFloatingActionButton(onClick = { scope.launch { listState.scrollToItem((totalItems - 1).coerceAtLeast(0)); listState.scrollBy(100000f); stickToBottom = true } }, containerColor = CardDark, contentColor = AccentBlue) {
                                Icon(Icons.Default.KeyboardArrowDown, tr("Scroll to bottom", "الانتقال للأسفل"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showModelSheet) {
        val accountLabels = remember(models) { AccountStore.load(context).associate { it.id to it.label } }
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }, containerColor = CardDark) {
            Text(tr("Choose a model", "اختر النموذج"), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(Modifier.heightIn(max = 460.dp).padding(horizontal = 12.dp)) {
                items(models, key = { it.accountId + "|" + it.id }) { m ->
                    val isSelected = m.id == selectedModel.id && m.accountId == selectedModel.accountId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { vm.selectModel(m); showModelSheet = false }.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (m.accountId.isEmpty()) "Puter" else accountLabels[m.accountId] ?: m.accountId, color = TextMuted, fontSize = 11.sp)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    menuChat?.let { chat ->
        ModalBottomSheet(onDismissRequest = { menuChat = null }, containerColor = CardDark) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(chat.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                SheetAction(Icons.Default.Edit, tr("Rename", "إعادة التسمية")) { renameText = chat.title; renameTarget = chat; menuChat = null }
                SheetAction(Icons.Default.PushPin, if (chat.isPinned) tr("Unpin", "إلغاء التثبيت") else tr("Pin", "تثبيت")) { vm.togglePin(chat.id, !chat.isPinned); menuChat = null }
                SheetAction(Icons.Default.Share, tr("Export chat", "تصدير المحادثة")) {
                    menuChat = null
                    scope.launch {
                        val exported = vm.exportChatText(chat.id)
                        if (exported.isBlank()) Toast.makeText(context, tr("Nothing to export yet", "لا توجد رسائل بعد"), Toast.LENGTH_SHORT).show()
                        else chatShareText(context, exported)
                    }
                }
                SheetAction(Icons.Default.CheckCircle, tr("Select multiple", "تحديد متعدد")) { selectMode = true; selectedIds = setOf(chat.id); menuChat = null }
                SheetAction(Icons.Default.Delete, tr("Delete", "حذف"), danger = true) { deleteTarget = chat; menuChat = null }
            }
        }
    }

    renameTarget?.let { chat ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = CardDark,
            title = { Text(tr("Rename chat", "إعادة تسمية المحادثة"), color = TextPrimary) },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.renameChat(chat.id, renameText); renameTarget = null }) { Text(tr("Save", "حفظ"), color = AccentBlue) } },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
        )
    }

    deleteTarget?.let { chat ->
        ConfirmDialog(
            title = tr("Delete this chat?", "حذف هذه المحادثة؟"),
            message = tr("\"${chat.title}\" will be deleted. It cannot be undone.", "سيتم حذف \"${chat.title}\". لا يمكن التراجع."),
            confirmLabel = tr("Delete", "حذف"), danger = true,
            onConfirm = { vm.deleteChat(chat.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }

    if (confirmDeleteSelected) {
        ConfirmDialog(
            title = tr("Delete ${selectedIds.size} chats?", "حذف ${selectedIds.size} محادثات؟"),
            message = tr("The selected chats will be deleted. It cannot be undone.", "سيتم حذف المحادثات المحددة. لا يمكن التراجع."),
            confirmLabel = tr("Delete", "حذف"), danger = true,
            onConfirm = { selectedIds.forEach { vm.deleteChat(it) }; selectedIds = emptySet(); selectMode = false; confirmDeleteSelected = false },
            onDismiss = { confirmDeleteSelected = false }
        )
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

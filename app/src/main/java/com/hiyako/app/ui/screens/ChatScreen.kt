package com.hiyako.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hiyako.app.data.db.ConversationEntity
import com.hiyako.app.ui.components.AnimatedLoadingText
import com.hiyako.app.ui.components.ChatInputBar
import com.hiyako.app.ui.components.ErrorBubble
import com.hiyako.app.ui.components.MessageBubble
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.BorderColor
import com.hiyako.app.ui.theme.CardDark
import com.hiyako.app.ui.theme.DeepSurface
import com.hiyako.app.ui.theme.ElevatedSurface
import com.hiyako.app.ui.theme.OledBlack
import com.hiyako.app.ui.theme.SecondarySurface
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.TextSecondary
import com.hiyako.app.util.queryDisplayName
import kotlinx.coroutines.launch

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(ElevatedSurface, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, textDirection = TextDirection.Content),
            cursorBrush = SolidColor(AccentBlue),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    models: List<AiModel>,
    selectedId: String,
    onSelect: (AiModel) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var q by remember { mutableStateOf("") }
    val filtered = remember(models, q) {
        if (q.isBlank()) models else models.filter { it.name.contains(q, true) || it.id.contains(q, true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 16.dp)) {
            Text("Choose a model", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (models.isEmpty()) {
                Text(
                    "No models loaded yet. Check your internet connection and Puter sign-in, then refresh.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                TextButton(onClick = onRefresh) { Text("Refresh", color = AccentBlue) }
            } else {
                SearchField(q, { q = it }, "Search models...")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(filtered, key = { it.id }) { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(m) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(m.name, color = TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (m.id.isNotEmpty() && m.id != m.name) {
                                    Text(m.id, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (m.id == selectedId) {
                                Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
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
    var showModelSheet by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleteTargets by remember { mutableStateOf<List<Int>?>(null) }
    var query by remember { mutableStateOf("") }
    var expandedMenuId by remember { mutableStateOf<Int?>(null) }
    var stickToBottom by remember { mutableStateOf(true) }

    val messages by vm.messages.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val chatTitle by vm.chatTitle.collectAsState()
    val activeId by vm.activeConversationId.collectAsState()
    val generating by vm.generating.collectAsState()
    val errors by vm.errors.collectAsState()
    val models by vm.availableModels.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val auth by vm.auth.collectAsState()

    val partial: String? = activeId?.let { generating[it] }
    val errorText: String? = activeId?.let { errors[it] }
    val isGenerating = partial != null
    val lastAiId: Int? = messages.lastOrNull()?.takeIf { !it.isUser }?.id
    val totalItems = messages.size + (if (partial != null) 1 else 0) + (if (errorText != null) 1 else 0)

    val attachmentMime = remember(attachmentUri) {
        attachmentUri?.let { context.contentResolver.getType(it) } ?: ""
    }
    val attachmentName = remember(attachmentUri) {
        attachmentUri?.let { queryDisplayName(context, it) } ?: ""
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) attachmentUri = uri
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) attachmentUri = uri
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) stickToBottom = !listState.canScrollForward
        }
    }
    LaunchedEffect(activeId) { stickToBottom = true }
    LaunchedEffect(partial, totalItems, activeId) {
        if (stickToBottom && totalItems > 0) listState.scrollBy(100000f)
    }

    BackHandler(enabled = vm.isMultiSelectMode) {
        vm.isMultiSelectMode = false
        vm.selectedChatIds.clear()
    }

    val send: () -> Unit = {
        if (!vm.canSend()) {
            Toast.makeText(context, "Connecting to the AI engine... check your internet and try again", Toast.LENGTH_SHORT).show()
        } else {
            vm.sendMessage(input, attachmentUri)
            input = ""
            attachmentUri = null
            stickToBottom = true
        }
    }

    val filteredConversations = remember(conversations, query) {
        if (query.isBlank()) conversations else conversations.filter { it.title.contains(query, ignoreCase = true) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DeepSurface, modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    SearchField(query, { query = it }, "Search chats...")
                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.createNewConversation()
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("New Chat", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = BorderColor)

                    LazyColumn(Modifier.weight(1f)) {
                        items(filteredConversations, key = { it.id }) { chat ->
                            val selected = vm.selectedChatIds.contains(chat.id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected || chat.id == activeId) SecondarySurface else Color.Transparent
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (vm.isMultiSelectMode) {
                                                if (selected) vm.selectedChatIds.remove(chat.id) else vm.selectedChatIds.add(chat.id)
                                            } else {
                                                vm.selectConversation(chat.id)
                                                scope.launch { drawerState.close() }
                                            }
                                        },
                                        onLongClick = {
                                            if (!vm.isMultiSelectMode) {
                                                vm.isMultiSelectMode = true
                                                vm.selectedChatIds.clear()
                                                vm.selectedChatIds.add(chat.id)
                                            }
                                        }
                                    )
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (vm.isMultiSelectMode) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = {
                                            if (it) vm.selectedChatIds.add(chat.id) else vm.selectedChatIds.remove(chat.id)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                    )
                                    Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                } else {
                                    if (chat.isPinned) {
                                        Icon(Icons.Default.PushPin, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Box {
                                        IconButton(onClick = { expandedMenuId = chat.id }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, "Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        DropdownMenu(expanded = expandedMenuId == chat.id, onDismissRequest = { expandedMenuId = null }) {
                                            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                                renameTarget = chat
                                                expandedMenuId = null
                                            })
                                            DropdownMenuItem(text = { Text(if (chat.isPinned) "Unpin" else "Pin") }, onClick = {
                                                vm.togglePin(chat.id, !chat.isPinned)
                                                expandedMenuId = null
                                            })
                                            DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = {
                                                deleteTargets = listOf(chat.id)
                                                expandedMenuId = null
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (vm.isMultiSelectMode) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                vm.isMultiSelectMode = false
                                vm.selectedChatIds.clear()
                            }) { Text("Cancel", color = TextSecondary) }
                            TextButton(onClick = {
                                vm.selectedChatIds.toList().forEach { vm.togglePin(it, true) }
                                vm.isMultiSelectMode = false
                                vm.selectedChatIds.clear()
                            }) { Text("Pin", color = TextSecondary) }
                            TextButton(
                                onClick = { if (vm.selectedChatIds.isNotEmpty()) deleteTargets = vm.selectedChatIds.toList() }
                            ) { Text("Delete (" + vm.selectedChatIds.size + ")", color = Color.Red) }
                        }
                    }

                    HorizontalDivider(color = BorderColor)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigateToSettings()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(32.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Text(
                                auth.username.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Settings", color = TextPrimary, fontSize = 15.sp)
                            Text(
                                if (auth.signedIn) (if (auth.username.isNotEmpty()) auth.username else "Signed in") else "Not signed in",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = OledBlack,
            modifier = Modifier.imePadding().fillMaxSize(),
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, start = 8.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                    Column(
                        Modifier.weight(1f).clickable { showModelSheet = true },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            chatTitle,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selectedModel.name,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = {
                        if (!auth.signedIn) {
                            vm.signIn()
                        } else {
                            Toast.makeText(context, "Signed in as " + auth.username, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            "Account",
                            tint = if (auth.signedIn) AccentBlue else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            bottomBar = {
                Column(Modifier.background(OledBlack)) {
                    if (attachmentUri != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(SecondarySurface, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (attachmentMime.startsWith("image/")) {
                                AsyncImage(
                                    model = attachmentUri,
                                    contentDescription = "Attachment",
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                attachmentName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { attachmentUri = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    ChatInputBar(
                        input = input,
                        onInputChange = { input = it },
                        onSend = send,
                        onStop = { vm.stopGeneration() },
                        isGenerating = isGenerating,
                        canSend = input.isNotBlank() || attachmentUri != null,
                        onAttachClick = { showAttachmentSheet = true },
                        isThinking = vm.isThinkingMode,
                        onThinkClick = { vm.toggleThink() },
                        isSearch = vm.isSearchMode,
                        onSearchClick = { vm.toggleSearch() }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(OledBlack)) {
                if (messages.isEmpty() && partial == null && errorText == null) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("كيف أساعدك اليوم؟", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(
                                text = msg.content,
                                isUser = msg.isUser,
                                type = msg.type,
                                attachmentUri = msg.attachmentUri,
                                attachmentName = msg.senderName,
                                showRegenerate = msg.id == lastAiId && !isGenerating,
                                onRegenerate = { vm.regenerate() },
                                onSpeak = { vm.speak(msg.content) }
                            )
                        }
                        if (partial != null) {
                            item(key = "generating") {
                                if (partial.isEmpty()) {
                                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { AnimatedLoadingText() }
                                } else {
                                    MessageBubble(text = partial, isUser = false, showActions = false)
                                }
                            }
                        }
                        if (errorText != null) {
                            item(key = "error") {
                                ErrorBubble(message = errorText, onRetry = { vm.regenerate() })
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showAttachmentSheet = false
                        imagePicker.launch("image/*")
                    }.padding(16.dp)
                ) {
                    Icon(Icons.Default.Image, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                    Text("Image", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showAttachmentSheet = false
                        filePicker.launch("*/*")
                    }.padding(16.dp)
                ) {
                    Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                    Text("File", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showModelSheet) {
        ModelPickerSheet(
            models = models,
            selectedId = selectedModel.id,
            onSelect = {
                vm.selectModel(it)
                showModelSheet = false
            },
            onRefresh = { vm.refreshModels() },
            onDismiss = { showModelSheet = false }
        )
    }

    val renaming = renameTarget
    if (renaming != null) {
        var newTitle by remember(renaming.id) { mutableStateOf(renaming.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameChat(renaming.id, newTitle)
                    renameTarget = null
                }) { Text("Save", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    val toDelete = deleteTargets
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTargets = null },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete chat") },
            text = { Text("Delete " + toDelete.size + " chat(s)? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete.forEach { vm.deleteChat(it) }
                    vm.isMultiSelectMode = false
                    vm.selectedChatIds.clear()
                    deleteTargets = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargets = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

package com.nexus.ai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexus.ai.ui.components.NexusInputBar
import com.nexus.ai.ui.components.NexusTopBar
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val attachmentUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onMenuClick: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var currentModel by remember { mutableStateOf("Claude Sonnet 4.6") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    
    // حالة حفظ المرفق المختار حالياً
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // مشغل اختيار الصور (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri
        }
    }

    // مشغل اختيار الملفات (Document Picker)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri
        }
    }

    Scaffold(
        topBar = {
            NexusTopBar(
                chatTitle = if (messages.isEmpty()) "Nexus AI" else "محادثة جديدة",
                modelName = currentModel,
                onMenuClick = onMenuClick,
                onModelClick = { showModelSheet = true },
                onNewChatClick = { messages = emptyList() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    EmptyState(onSuggestionClick = { text -> inputText = text })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message)
                        }
                    }
                }
            }

            // عرض الصورة المصغرة للمرفق إذا كان موجوداً
            if (selectedAttachmentUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .size(80.dp)
                ) {
                    AsyncImage(
                        model = selectedAttachmentUri,
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedAttachmentUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                            .size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
            }

            NexusInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() || selectedAttachmentUri != null) {
                        messages = messages + ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            text = inputText,
                            isUser = true,
                            attachmentUri = selectedAttachmentUri
                        )
                        inputText = ""
                        selectedAttachmentUri = null // مسح المرفق بعد الإرسال
                    }
                },
                onAttachClick = { showAttachSheet = true }
            )
        }
    }

    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ModelSelectionSheet(
                currentModel = currentModel,
                onModelSelect = { model ->
                    currentModel = model
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showModelSheet = false
                    }
                }
            )
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AttachmentSheet(
                onPhotoClick = {
                    showAttachSheet = false
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onDocumentClick = {
                    showAttachSheet = false
                    documentPickerLauncher.launch("*/*")
                },
                onCameraClick = {
                    showAttachSheet = false
                    // الكاميرا تتطلب إعدادات إضافية، سنتركها للنسخة القادمة ونربطها بالمعرض حالياً
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
        }
    }
}

@Composable
fun EmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("كيف أساعدك اليوم؟", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        val suggestions = listOf("بناء تطبيق", "إصلاح كود", "توليد صورة", "بحث عميق")
        suggestions.forEach { suggestion ->
            Text(
                text = suggestion,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().clickable { onSuggestionClick(suggestion) }.padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isUser) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.attachmentUri != null) {
                        AsyncImage(
                            model = message.attachmentUri,
                            contentDescription = "Attached Image",
                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(text = message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    }
                }
            }
        } else {
            Text(
                text = message.text,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun ModelSelectionSheet(currentModel: String, onModelSelect: (String) -> Unit) {
    val models = listOf("Claude Sonnet 4.6" to "Coding", "GPT-4o" to "Vision", "Gemini 2.5 Pro" to "1M Context", "DeepSeek V3" to "Free", "FLUX" to "Image")
    Column(modifier = Modifier.padding(16.dp)) {
        Text("اختر النموذج", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search models...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        models.forEach { (name, tag) ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onModelSelect(name) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(tag, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                if (name == currentModel) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AttachmentSheet(
    onPhotoClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("المرفقات", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AttachmentButton(icon = Icons.Default.CameraAlt, label = "Camera", onClick = onCameraClick)
            AttachmentButton(icon = Icons.Default.Image, label = "Photo", onClick = onPhotoClick)
            AttachmentButton(icon = Icons.Default.Description, label = "Document", onClick = onDocumentClick)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AttachmentButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

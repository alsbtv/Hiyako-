package com.hiyako.app.ui.components

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AnimatedLoadingText() {
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = if (dotCount == 3) 1 else dotCount + 1
        }
    }
    Text(text = "Thinking${".".repeat(dotCount)}", color = AccentBlue, fontSize = 14.sp)
}

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    isLoading: Boolean = false,
    type: String = "TEXT"
) {
    var isThinkingExpanded by remember { mutableStateOf(false) }
    val hasThinkingBlock = !isUser && text.length > 100 && !isLoading

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isLoading) {
                AnimatedLoadingText()
            } else if (isUser) {
                Surface(color = UserBubble, shape = RoundedCornerShape(20.dp)) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = text,
                            color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp,
                            textAlign = TextAlign.End,
                            style = TextStyle(textDirection = TextDirection.Content)
                        )
                    }
                }
            } else {
                if (hasThinkingBlock) {
                    Row(
                        modifier = Modifier.clickable { isThinkingExpanded = !isThinkingExpanded }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thinking", color = TextSecondary, fontSize = 14.sp)
                        Icon(
                            if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isThinkingExpanded) {
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Box(Modifier.width(2.dp).height(40.dp).background(BorderColor)) 
                            Spacer(Modifier.width(8.dp))
                            Text("Analyzing your request... (This is a simulated thinking process)", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                
                when (type) {
                    "IMAGE" -> {
                        val imageModel: Any = if (text.startsWith("http") || text.startsWith("data:image")) text else java.io.File(text)
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Generated Image",
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> Text(
                        text = text.replace("**", "").replace("```", "").trim(),
                        color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp,
                        textAlign = TextAlign.Start,
                        style = TextStyle(textDirection = TextDirection.Content)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    onStop: () -> Unit = {},
    onAttachClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .background(SecondarySurface, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextSecondary)
            }
            
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(AccentBlue),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) Text("Type a message...", color = TextMuted, fontSize = 15.sp)
                        innerTextField()
                    }
                }
            )
            
            Surface(shape = RoundedCornerShape(16.dp), color = ElevatedSurface) {
                Text("Think", color = AccentBlue, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Spacer(Modifier.width(8.dp))
            
            if (isLoading) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank()) TextPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onCameraClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recentImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(Unit) {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            var count = 0
            while (it.moveToNext() && count < 20) {
                val id = it.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                images.add(contentUri)
                count++
            }
        }
        recentImages = images
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentButton(Icons.Default.CameraAlt, "Camera", onCameraClick)
                AttachmentButton(Icons.Default.Image, "Photo", onPhotoClick)
                AttachmentButton(Icons.Default.Description, "Document", onDocumentClick)
            }
            Spacer(Modifier.height(16.dp))
            if (recentImages.isNotEmpty()) {
                Text("Recent", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentImages) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Recent Image",
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AttachmentButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).background(SecondarySurface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = TextPrimary)
    }
}

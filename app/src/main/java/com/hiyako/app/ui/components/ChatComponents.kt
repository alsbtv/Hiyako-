package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ألوان Deep Core
val NexusVoid = Color(0xFF05060F)
val NexusCyan = Color(0xFF00E5FF)
val NexusMagenta = Color(0xFFFF3DCB)
val NexusGlass = Color(0x331A2147)
val NexusText = Color(0xFFEAF2FF)

@Composable
fun AnimatedLoadingText(type: String) {
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = if (dotCount == 3) 1 else dotCount + 1
        }
    }
    val baseText = when (type) {
        "IMAGE" -> "Creating image"
        "VIDEO" -> "Generating video"
        else -> "Thinking"
    }
    Text(text = "$baseText${".".repeat(dotCount)}", color = NexusCyan, fontSize = 14.sp)
}

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    isLoading: Boolean = false,
    type: String = "TEXT",
    hasCodeChanges: Boolean = false,
    onApplyChanges: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(Modifier.size(28.dp).background(NexusCyan, CircleShape).padding(top = 2.dp), contentAlignment = Alignment.Center) {
                Text("N", color = NexusVoid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = NexusGlass,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 20.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) NexusCyan.copy(alpha = 0.5f) else NexusMagenta.copy(alpha = 0.5f)
                )
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (isLoading) {
                        AnimatedLoadingText(type = type)
                    } else {
                        when (type) {
                            "IMAGE" -> {
                                val imageModel: Any = if (text.startsWith("http") || text.startsWith("data:image")) text else java.io.File(text)
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Generated Image",
                                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(text) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> Text(
                                text = text.replace("**", "").replace("```", "").trim(),
                                color = NexusText, fontSize = 15.sp, lineHeight = 22.sp,
                                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                                style = TextStyle(textDirection = TextDirection.Content)
                            )
                        }
                    }
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
    onStop: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusGlass, RoundedCornerShape(30.dp))
                .border(1.dp, NexusCyan.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textStyle = TextStyle(color = NexusText, fontSize = 15.sp, textDirection = TextDirection.Content),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(NexusCyan),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) Text("Message Nexus...", color = NexusText.copy(alpha = 0.5f), fontSize = 15.sp)
                        innerTextField()
                    }
                }
            )
            
            if (isLoading) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank(),
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (input.isNotBlank()) NexusCyan else Color.Transparent
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank()) NexusVoid else NexusText.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateGrid(onActionClick: (String) -> Unit) {
    val actions = listOf(
        Triple("Build App", Icons.Default.Build, NexusCyan),
        Triple("Ghost Fix", Icons.Default.BugReport, Color.Red),
        Triple("Image", Icons.Default.Image, NexusMagenta),
        Triple("Video", Icons.Default.VideoCall, Color(0xFF9C27B0)),
        Triple("Deep Research", Icons.Default.Search, Color(0xFF4CAF50)),
        Triple("Writing", Icons.Default.Edit, Color(0xFFFF9800))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions.size) { index ->
            val (title, icon, color) = actions[index]
            Card(
                modifier = Modifier.aspectRatio(1.2f).clickable { onActionClick(title) },
                colors = CardDefaults.cardColors(containerColor = NexusGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = NexusText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
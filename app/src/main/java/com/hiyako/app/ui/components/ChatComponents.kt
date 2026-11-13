package com.hiyako.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
    Text(text = "$baseText${".".repeat(dotCount)}", color = AccentBlue, fontSize = 14.sp)
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
            Box(Modifier.size(28.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                Text("N", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) UserBubble else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(Modifier.padding(horizontal = if (isUser) 16.dp else 0.dp, vertical = if (isUser) 10.dp else 0.dp)) {
                    if (isLoading) {
                        AnimatedLoadingText(type = type)
                    } else {
                        when (type) {
                            "IMAGE" -> {
                                val imageModel: Any = if (text.startsWith("http") || text.startsWith("data:image")) text else java.io.File(text)
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Generated Image",
                                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(text) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> Text(
                                text = text.replace("**", "").replace("```", "").trim(),
                                color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp,
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
                .imePadding()
                .background(SecondarySurface, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(AccentBlue),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) Text("Message Nexus...", color = TextMuted, fontSize = 15.sp)
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
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank()) TextPrimary else TextMuted,
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
        Triple("Build App", Icons.Default.Build, AccentBlue),
        Triple("Ghost Fix", Icons.Default.BugReport, Color.Red),
        Triple("Image", Icons.Default.Image, AccentBlue),
        Triple("Video", Icons.Default.VideoCall, AccentBlue),
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
                colors = CardDefaults.cardColors(containerColor = SecondarySurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

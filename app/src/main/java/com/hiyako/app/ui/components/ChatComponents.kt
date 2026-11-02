package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.CardDark
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.TextSecondary

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    isLoading: Boolean = false,
    type: String = "TEXT"
) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(Modifier.size(24.dp).background(AccentBlue, CircleShape).padding(top = 2.dp), contentAlignment = Alignment.Center) {
                Text("N", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) CardDark else Color.Transparent,
                shape = RoundedCornerShape(topStart = if (isUser) 16.dp else 0.dp, topEnd = if (isUser) 0.dp else 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...", color = TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        when (type) {
                            "IMAGE" -> {
                                AsyncImage(
                                    model = text,
                                    contentDescription = "Generated Image",
                                    modifier = Modifier.size(250.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            "VIDEO" -> {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(text)); prepare(); playWhenReady = true } }
                                DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
                                AndroidView(
                                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } },
                                    modifier = Modifier.size(250.dp, 150.dp).background(Color.Black, RoundedCornerShape(8.dp))
                                )
                            }
                            else -> {
                                Text(
                                    text = text.replace("**", "").replace("```", "").trim(), 
                                    color = TextPrimary, 
                                    fontSize = 15.sp, 
                                    lineHeight = 22.sp, 
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
}

@Composable
fun ChatInputBar(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit, isLoading: Boolean) {
    Surface(tonalElevation = 2.dp, color = CardDark) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input, onValueChange = onInputChange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentBlue), maxLines = 4,
                decorationBox = { innerTextField -> Box { if (input.isEmpty()) Text("Type a message...", color = TextSecondary, fontSize = 15.sp); innerTextField() } }
            )
            FilledIconButton(onClick = onSend, enabled = input.isNotBlank() && !isLoading, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent)) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (input.isNotBlank()) Color.White else TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// واجهة المربعات (Grid) للشاشة الفارغة
@Composable
fun EmptyStateGrid(
    onActionClick: (String) -> Unit
) {
    val actions = listOf(
        Triple("Image", Icons.Default.Image, Color(0xFFE91E63)),
        Triple("Video", Icons.Default.VideoCall, Color(0xFF9C27B0)),
        Triple("Build App", Icons.Default.Build, Color(0xFF2196F3)),
        Triple("Deep Research", Icons.Default.Search, Color(0xFF4CAF50)),
        Triple("Writing", Icons.Default.Edit, Color(0xFFFF9800)),
        Triple("Free Chat", Icons.Default.Chat, Color(0xFF00BCD4))
    )

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions.size) { index ->
            val (title, icon, color) = actions[index]
            Card(
                modifier = Modifier.aspectRatio(1.2f).clickable { onActionClick(title) },
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun AnimatedLoadingText() {
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) { while (true) { delay(500); dotCount = if (dotCount == 3) 1 else dotCount + 1 } }
    Text(text = "Thinking${".".repeat(dotCount)}", color = AccentBlue, fontSize = 14.sp)
}

@Composable
fun MessageBubble(text: String, isUser: Boolean, isLoading: Boolean = false, type: String = "TEXT") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f, fill = false), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (isLoading) AnimatedLoadingText()
            else if (isUser) {
                Surface(color = UserBubble, shape = RoundedCornerShape(20.dp)) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.Content))
                    }
                }
            } else {
                when (type) {
                    "IMAGE" -> {
                        val imageModel: Any = if (text.startsWith("http") || text.startsWith("data:image")) text else java.io.File(text)
                        AsyncImage(model = imageModel, contentDescription = "Image", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    }
                    else -> Text(text.replace("**", "").replace("```", "").trim(), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Start, style = TextStyle(textDirection = TextDirection.Content))
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit, isLoading: Boolean, onStop: () -> Unit, onAttachClick: () -> Unit, isThinking: Boolean, onThinkClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.fillMaxWidth().imePadding().background(SecondarySurface, RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null, tint = TextSecondary) }
            BasicTextField(value = input, onValueChange = onInputChange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content), cursorBrush = SolidColor(AccentBlue), maxLines = 4, decorationBox = { innerTextField -> Box { if (input.isEmpty()) Text("Type a message...", color = TextMuted, fontSize = 15.sp); innerTextField() } })
            
            Surface(shape = RoundedCornerShape(16.dp), color = if (isThinking) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface, modifier = Modifier.clickable { onThinkClick() }) {
                Text("Think", color = if (isThinking) AccentBlue else TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Spacer(Modifier.width(8.dp))
            
            if (isLoading) {
                FilledIconButton(onClick = onStop, modifier = Modifier.size(36.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Red)) { Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            } else {
                FilledIconButton(onClick = onSend, enabled = input.isNotBlank(), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (input.isNotBlank()) TextPrimary else TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

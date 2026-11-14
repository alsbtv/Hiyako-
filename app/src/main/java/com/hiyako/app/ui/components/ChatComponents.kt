package com.hiyako.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
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
fun CollapsibleCodeBlock(code: String, language: String = "code") {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(language, color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row {
                    IconButton(onClick = { 
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.ContentCopy, "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(24.dp)) { 
                        Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Toggle", tint = TextSecondary, modifier = Modifier.size(16.dp)) 
                    }
                }
            }
            if (isExpanded) Text(code, color = Color(0xFFE0E0E0), fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun MessageBubble(
    text: String, isUser: Boolean, isLoading: Boolean = false, type: String = "TEXT",
    attachmentUri: String? = null, senderName: String? = null, onRegenerate: () -> Unit = {}, onSpeak: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f, fill = false), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (!isUser && senderName != null) Text(senderName, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            if (isLoading) AnimatedLoadingText()
            else if (isUser) {
                Surface(color = UserBubble, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (attachmentUri != null && type == "IMAGE") {
                            AsyncImage(model = attachmentUri, contentDescription = "Img", modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.End, style = TextStyle(textDirection = TextDirection.Content))
                    }
                }
            } else {
                if (text.contains("```")) {
                    val parts = text.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 0) { if (part.isNotBlank()) Text(part.trim(), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp) }
                        else { val lines = part.lines(); val lang = lines.firstOrNull() ?: "code"; CollapsibleCodeBlock(lines.drop(1).joinToString("\n"), lang) }
                    }
                } else {
                    Text(text.replace("**", "").trim(), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Start, style = TextStyle(textDirection = TextDirection.Content))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI", text))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.ContentCopy, "Copy", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.VolumeUp, "Speak", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Refresh, "Regen", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    input: String, onInputChange: (String) -> Unit, onSend: () -> Unit, isLoading: Boolean,
    onAttachClick: () -> Unit, isThinking: Boolean, onThinkClick: () -> Unit, isSearch: Boolean, onSearchClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.fillMaxWidth().imePadding().background(SecondarySurface, RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null, tint = TextSecondary) }
            BasicTextField(value = input, onValueChange = onInputChange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp), cursorBrush = SolidColor(AccentBlue), maxLines = 4, decorationBox = { inner -> Box { if (input.isEmpty()) Text("Message...", color = TextMuted, fontSize = 15.sp); inner() } })
            Row(Modifier.padding(horizontal = 4.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = if (isThinking) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface, modifier = Modifier.clickable { onThinkClick() }) {
                    Text("Think", color = if (isThinking) AccentBlue else TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
                Spacer(Modifier.width(4.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = if (isSearch) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface, modifier = Modifier.clickable { onSearchClick() }) {
                    Text("Search", color = if (isSearch) AccentBlue else TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
            IconButton(onClick = onVoiceClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Mic, null, tint = TextSecondary) }
            FilledIconButton(onClick = onSend, enabled = input.isNotBlank(), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent), modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (input.isNotBlank()) TextPrimary else TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.CardDark
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.TextSecondary

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    isLoading: Boolean = false
) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(Modifier.size(28.dp).background(AccentBlue, CircleShape), contentAlignment = Alignment.Center) {
                Text("N", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) CardDark else Color.Transparent,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 0.dp,
                    topEnd = if (isUser) 0.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            ) {
                Box(Modifier.padding(12.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(20.dp))
                    } else {
                        Text(text = text, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
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
    isLoading: Boolean
) {
    Surface(tonalElevation = 2.dp, color = CardDark) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentBlue),
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) Text("Type a message...", color = TextSecondary)
                        innerTextField()
                    }
                }
            )
            FilledIconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (input.isNotBlank()) AccentBlue else Color.DarkGray)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

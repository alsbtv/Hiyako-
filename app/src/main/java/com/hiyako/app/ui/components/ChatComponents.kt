package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
    isLoading: Boolean = false,
    hasCodeChanges: Boolean = false,
    onApplyChanges: (String) -> Unit = {}
) {
    val arrangement = if (isUser) Arrangement.End else Arrangement.Start
    // إزالة علامات ** التي تظهر كنص
    val cleanText = text.replace("**", "").replace("```", "").trim()

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
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 0.dp,
                    topEnd = if (isUser) 0.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = cleanText,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                            style = TextStyle(textDirection = TextDirection.Content)
                        )
                    }
                }
            }
            
            if (hasCodeChanges && !isUser) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onApplyChanges(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Build, null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Apply & Build", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentBlue),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) Text("Type a message...", color = TextSecondary, fontSize = 15.sp)
                        innerTextField()
                    }
                }
            )
            FilledIconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (input.isNotBlank()) AccentBlue else Color.Transparent)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (input.isNotBlank()) Color.White else TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

package com.hiyako.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.ElevatedSurface
import com.hiyako.app.ui.theme.SecondarySurface
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.TextSecondary
import com.hiyako.app.ui.theme.UserBubble
import com.hiyako.app.util.isRtlText
import kotlinx.coroutines.delay

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

@Composable
fun AnimatedLoadingText() {
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            dotCount = if (dotCount == 3) 1 else dotCount + 1
        }
    }
    Text(
        text = "Thinking" + ".".repeat(dotCount),
        color = AccentBlue,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun CollapsibleCodeBlock(code: String, language: String = "code") {
    var isExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language, color = Color(0xFFA1A1AA), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { copyToClipboard(context, "Code", code) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.ContentCopy, "Copy", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                "Toggle",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (isExpanded) {
                    Text(
                        code,
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lightweight Markdown rendering (headings, lists, bold, inline code, code fences)
// ---------------------------------------------------------------------------

sealed interface MdBlock
data class MdText(val text: String, val heading: Int = 0, val marker: String? = null) : MdBlock
data class MdCode(val lang: String, val code: String) : MdBlock

private val numberedRegex = Regex("^(\\d+)[.)]\\s+(.*)$")

fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val sb = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(lines[i])
                i++
            }
            out.add(MdCode(if (lang.isBlank()) "code" else lang, sb.toString()))
            i++
            continue
        }
        when {
            line.isBlank() -> {}
            trimmed.startsWith("### ") -> out.add(MdText(trimmed.removePrefix("### "), 3))
            trimmed.startsWith("## ") -> out.add(MdText(trimmed.removePrefix("## "), 2))
            trimmed.startsWith("# ") -> out.add(MdText(trimmed.removePrefix("# "), 1))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> out.add(MdText(trimmed.substring(2), 0, "•"))
            else -> {
                val m = numberedRegex.matchEntire(trimmed)
                if (m != null) {
                    out.add(MdText(m.groupValues[2], 0, m.groupValues[1] + "."))
                } else {
                    out.add(MdText(line, 0))
                }
            }
        }
        i++
    }
    return out
}

private fun inlineMarkdown(src: String, codeBackground: Color, codeColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < src.length) {
            if (src.startsWith("**", i)) {
                val end = src.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(src.substring(i + 2, end)) }
                    i = end + 2
                    continue
                }
            }
            if (src[i] == '`') {
                val end = src.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground, color = codeColor)) {
                        append(src.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(src[i])
            i++
        }
    }
}

@Composable
fun MarkdownText(text: String) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(Modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdCode -> CollapsibleCodeBlock(block.code, block.lang)
                is MdText -> {
                    val styled = inlineMarkdown(block.text, ElevatedSurface, TextPrimary)
                    val size = when (block.heading) {
                        1 -> 21.sp
                        2 -> 18.sp
                        3 -> 16.sp
                        else -> 15.sp
                    }
                    val weight = if (block.heading > 0) FontWeight.Bold else FontWeight.Normal
                    val marker = block.marker
                    if (marker != null) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(marker, color = TextSecondary, fontSize = size, lineHeight = 24.sp, modifier = Modifier.width(26.dp))
                            Text(
                                styled,
                                color = TextPrimary,
                                fontSize = size,
                                lineHeight = 24.sp,
                                fontWeight = weight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            styled,
                            color = TextPrimary,
                            fontSize = size,
                            lineHeight = 24.sp,
                            fontWeight = weight,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Message bubble
// ---------------------------------------------------------------------------

@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    type: String = "TEXT",
    attachmentUri: String? = null,
    attachmentName: String? = null,
    showActions: Boolean = true,
    showRegenerate: Boolean = false,
    onRegenerate: () -> Unit = {},
    onSpeak: () -> Unit = {},
    onReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val rtl = remember(text) { isRtlText(text) }
    val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            if (isUser) {
                Surface(color = UserBubble, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 310.dp)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (attachmentUri != null && type == "IMAGE") {
                            AsyncImage(
                                model = attachmentUri,
                                contentDescription = "Image",
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (text.isNotBlank()) Spacer(Modifier.height(8.dp))
                        }
                        if (type == "FILE") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(attachmentName ?: "File", color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (text.isNotBlank()) Spacer(Modifier.height(8.dp))
                        }
                        if (text.isNotBlank()) {
                            SelectionContainer {
                                Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp)
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    SelectionContainer { MarkdownText(text) }
                    if (showActions) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { copyToClipboard(context, "AI", text) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ContentCopy, "Copy", tint = TextMuted, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.VolumeUp, "Speak", tint = TextMuted, modifier = Modifier.size(17.dp))
                            }
                            if (onReport != null) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onReport, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Warning, "Report", tint = TextMuted, modifier = Modifier.size(17.dp))
                                }
                            }
                            if (showRegenerate) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Refresh, "Regenerate", tint = TextMuted, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorBubble(message: String, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(color = Color(0x33EF4444), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(message, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f, fill = false))
                }
                TextButton(onClick = onRetry) { Text("Retry", color = AccentBlue) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Input bar
// ---------------------------------------------------------------------------

@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    canSend: Boolean,
    onAttachClick: () -> Unit,
    isThinking: Boolean,
    onThinkClick: () -> Unit,
    isSearch: Boolean,
    onSearchClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(SecondarySurface, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, "Attach", tint = TextSecondary)
        }
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textDirection = TextDirection.Content),
            cursorBrush = SolidColor(AccentBlue),
            maxLines = 6,
            decorationBox = { inner ->
                Box {
                    if (input.isEmpty()) Text("Message...", color = TextMuted, fontSize = 15.sp)
                    inner()
                }
            }
        )
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ToggleChip("Think", isThinking, onThinkClick)
            Spacer(Modifier.width(4.dp))
            ToggleChip("Search", isSearch, onSearchClick)
        }
        Spacer(Modifier.width(4.dp))
        if (isGenerating) {
            FilledIconButton(
                onClick = onStop,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentBlue),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Stop, "Stop", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else {
            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = ElevatedSurface
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = if (canSend) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (active) AccentBlue else ElevatedSurface,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = if (active) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

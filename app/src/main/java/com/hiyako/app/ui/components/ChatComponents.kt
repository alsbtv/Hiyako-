package com.hiyako.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.BorderColor
import com.hiyako.app.ui.theme.ElevatedSurface
import com.hiyako.app.ui.theme.OledBlack
import com.hiyako.app.ui.theme.SecondarySurface
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary
import com.hiyako.app.ui.theme.TextSecondary
import com.hiyako.app.ui.theme.UserBubble
import androidx.compose.ui.layout.ContentScale

fun chatCopyText(context: Context, text: String) {
    try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Hiyako", text))
        Toast.makeText(context, tr("Copied", "تم النسخ"), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, tr("Could not copy", "تعذّر النسخ"), Toast.LENGTH_SHORT).show()
    }
}

fun chatShareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, tr("Share", "مشاركة")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, tr("Nothing to share with", "لا يوجد تطبيق للمشاركة"), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun UserMessage(text: String, type: String, attachmentUri: String?, attachmentName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp))
                .background(UserBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (attachmentUri != null && type == "IMAGE") {
                AsyncImage(
                    model = attachmentUri,
                    contentDescription = tr("Image", "صورة"),
                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                if (text.isNotBlank()) Spacer(Modifier.height(8.dp))
            }
            if (type == "FILE") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        attachmentName ?: tr("File", "ملف"),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (text.isNotBlank()) Spacer(Modifier.height(8.dp))
            }
            if (text.isNotBlank()) {
                SelectionContainer {
                    Text(text, color = TextPrimary, fontSize = 15.sp, lineHeight = 23.sp)
                }
            }
        }
    }
}

@Composable
fun AssistantMessage(
    text: String,
    showActions: Boolean = true,
    showRegenerate: Boolean = false,
    onRegenerate: () -> Unit = {},
    onSpeak: () -> Unit = {},
    onReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(AccentBlue.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = AccentBlue, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            SelectionContainer { MarkdownText(text) }
            if (showActions) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionIcon(Icons.Outlined.ContentCopy, tr("Copy", "نسخ")) { chatCopyText(context, text) }
                    ActionIcon(Icons.Outlined.VolumeUp, tr("Read aloud", "قراءة بصوت")) { onSpeak() }
                    ActionIcon(Icons.Filled.Share, tr("Share", "مشاركة")) { chatShareText(context, text) }
                    if (showRegenerate) {
                        ActionIcon(Icons.Outlined.Refresh, tr("Regenerate", "إعادة التوليد")) { onRegenerate() }
                    }
                    if (onReport != null) {
                        ActionIcon(Icons.Outlined.Flag, tr("Report", "إبلاغ")) { onReport() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = label, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 180),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(Modifier.size(7.dp).background(TextSecondary.copy(alpha = alpha), CircleShape))
            if (index < 2) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0x1AEF4444), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tr("Something went wrong", "حدث خطأ"), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(message, color = TextSecondary, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onRetry) {
            Text(tr("Retry", "إعادة"), color = AccentBlue)
        }
    }
}

private sealed class MdBlock {
    class Para(val text: String) : MdBlock()
    class Head(val level: Int, val text: String) : MdBlock()
    class Bullets(val items: List<String>) : MdBlock()
    class Numbers(val items: List<String>) : MdBlock()
    class Code(val lang: String, val code: String) : MdBlock()
    class Quote(val text: String) : MdBlock()
}

private val numberedRegex = Regex("^\\d+[.)]\\s+")
private val headingRegex = Regex("^(#{1,6})\\s*(.*)$")

private fun parseMarkdownBlocks(src: String): List<MdBlock> {
    val out = ArrayList<MdBlock>()
    val para = ArrayList<String>()
    val bullets = ArrayList<String>()
    val numbers = ArrayList<String>()
    val quote = ArrayList<String>()

    fun flush() {
        if (para.isNotEmpty()) { out.add(MdBlock.Para(para.joinToString("\n"))); para.clear() }
        if (bullets.isNotEmpty()) { out.add(MdBlock.Bullets(bullets.toList())); bullets.clear() }
        if (numbers.isNotEmpty()) { out.add(MdBlock.Numbers(numbers.toList())); numbers.clear() }
        if (quote.isNotEmpty()) { out.add(MdBlock.Quote(quote.joinToString("\n"))); quote.clear() }
    }

    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        val heading = headingRegex.find(trimmed)
        when {
            trimmed.startsWith("```") -> {
                flush()
                val lang = trimmed.removePrefix("```").trim()
                val code = ArrayList<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) { code.add(lines[i]); i++ }
                out.add(MdBlock.Code(lang, code.joinToString("\n")))
            }
            trimmed.isEmpty() -> flush()
            heading != null -> {
                flush()
                out.add(MdBlock.Head(heading.groupValues[1].length, heading.groupValues[2].trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                if (bullets.isEmpty()) flush()
                bullets.add(trimmed.substring(2).trim())
            }
            numberedRegex.containsMatchIn(trimmed) -> {
                if (numbers.isEmpty()) flush()
                numbers.add(trimmed.replace(numberedRegex, "").trim())
            }
            trimmed.startsWith("> ") -> {
                if (quote.isEmpty()) flush()
                quote.add(trimmed.substring(2).trim())
            }
            else -> {
                if (para.isEmpty()) flush()
                para.add(lines[i])
            }
        }
        i++
    }
    flush()
    return out
}

private fun inlineStyled(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 2, end)) }
                i = end + 2
            } else { append("**"); i += 2 }
        } else if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = AccentBlue)) { append(text.substring(i + 1, end)) }
                i = end + 1
            } else { append('`'); i++ }
        } else {
            append(text[i]); i++
        }
    }
}

@Composable
fun MarkdownText(text: String) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Para -> Text(inlineStyled(block.text), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp)
                is MdBlock.Head -> Text(inlineStyled(block.text), color = TextPrimary, fontSize = (22 - block.level * 2).sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item ->
                        Row {
                            Text("•  ", color = AccentBlue, fontSize = 15.sp, lineHeight = 24.sp)
                            Text(inlineStyled(item), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
                is MdBlock.Numbers -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEachIndexed { index, item ->
                        Row {
                            Text("${index + 1}.  ", color = AccentBlue, fontSize = 15.sp, lineHeight = 24.sp)
                            Text(inlineStyled(item), color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
                is MdBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(AccentBlue, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(inlineStyled(block.text), color = TextSecondary, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                }
                is MdBlock.Code -> CollapsibleCodeBlock(block.lang, block.code)
            }
        }
    }
}

@Composable
fun CollapsibleCodeBlock(language: String, code: String) {
    val context = LocalContext.current
    val lineCount = code.lines().size
    val collapsible = lineCount > 12
    var expanded by remember(code) { mutableStateOf(!collapsible) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0D0D10)).border(1.dp, Color(0xFF26262C), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF17171C)).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(language.ifBlank { "code" }, color = Color(0xFF9CA3AF), fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (collapsible) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(30.dp)) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = tr("Expand or collapse", "فتح أو طي"), tint = Color(0xFF9CA3AF))
                }
            }
            IconButton(onClick = { chatCopyText(context, code) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = tr("Copy code", "نسخ الكود"), tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
            }
        }
        if (expanded) {
            SelectionContainer {
                Box(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(code, color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
fun ComposerBar(
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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.padding(start = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChip(tr("Think", "تفكير"), isThinking, onThinkClick)
            ToggleChip(tr("Search", "بحث"), isSearch, onSearchClick)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(ElevatedSurface).padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttachClick, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Add, contentDescription = tr("Attach", "إرفاق"), tint = TextSecondary)
            }
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, lineHeight = 22.sp),
                cursorBrush = SolidColor(AccentBlue),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f).padding(vertical = 11.dp, horizontal = 4.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (input.isEmpty()) Text(tr("Message", "اكتب رسالتك"), color = TextMuted, fontSize = 16.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(4.dp))
            val active = isGenerating || canSend
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (active) AccentBlue else BorderColor).clickable(enabled = active) {
                    if (isGenerating) onStop() else onSend()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isGenerating) tr("Stop", "إيقاف") else tr("Send", "إرسال"),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (active) AccentBlue.copy(alpha = 0.18f) else ElevatedSurface).border(1.dp, if (active) AccentBlue else BorderColor, RoundedCornerShape(50)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (active) AccentBlue else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SuggestionCard(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(SecondarySurface).border(1.dp, BorderColor, RoundedCornerShape(20.dp)).clickable { onClick() }.padding(14.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

val ComposerBackground: Color get() = OledBlack

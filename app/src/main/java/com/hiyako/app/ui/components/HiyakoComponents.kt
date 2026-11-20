package com.hiyako.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*

@Composable
fun HiyakoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val container = when (kind) {
        ButtonKind.Primary -> AccentBlue
        ButtonKind.Tonal -> ElevatedSurface
        ButtonKind.Danger -> DangerColor
    }
    val content = when (kind) {
        ButtonKind.Primary -> OnAccent
        ButtonKind.Tonal -> TextPrimary
        ButtonKind.Danger -> Color(0xFF1A0000)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = BorderColor, disabledContentColor = TextMuted
        )
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
        } else {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

enum class ButtonKind { Primary, Tonal, Danger }

@Composable
fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SecondarySurface),
            content = content
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val tint = if (danger) DangerColor else AccentBlue
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (danger) DangerColor else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(BorderColor))
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector, title: String, message: String,
    actionLabel: String? = null, onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(32.dp)) }
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            HiyakoButton(actionLabel, onAction, kind = ButtonKind.Tonal)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = OledBlack,
        floatingActionButton = floatingActionButton,
        topBar = {
            TopAppBar(
                title = { Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "رجوع"), tint = TextPrimary)
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack, scrolledContainerColor = DeepSurface
                )
            )
        },
        content = content
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, danger: Boolean = false,
                  onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(24.dp),
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = TextSecondary, lineHeight = 22.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (danger) DangerColor else AccentBlue, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
    )
}

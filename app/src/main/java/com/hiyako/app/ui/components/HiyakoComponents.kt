package com.hiyako.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.*
import kotlinx.coroutines.delay

/** Fades and slides a view in the first time it appears. */
fun Modifier.appear(delayMs: Int = 0, distance: Float = 10f): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distance
    }
}

/** Frosted surface: translucent fill with a glowing gradient outline. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.clip(shape).background(SecondarySurface, shape).border(1.dp, BorderColor, shape)

/** Round glass icon button used in top bars. */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy), label = "glassIcon")
    Box(
        modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glass(CircleShape)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = TextPrimary, modifier = Modifier.size(22.dp)) }
}

/** Quiet logo mark: a soft ring that slowly breathes. */
@Composable
fun AuroraOrb(size: Dp = 120.dp) {
    val transition = rememberInfiniteTransition(label = "orb")
    val breathe by transition.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    Box(Modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size * 1.3f).graphicsLayer { scaleX = breathe; scaleY = breathe }
                .border(1.dp, AccentBlue.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            Modifier.size(size).graphicsLayer { scaleX = breathe; scaleY = breathe }
                .background(AccentBlue.copy(alpha = 0.14f), CircleShape)
                .border(1.dp, AccentBlue.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("✦", color = AccentBlue, fontSize = (size.value * 0.34f).sp) }
    }
}

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
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioNoBouncy), label = "btn")
    val active = enabled && !loading
    val shape = RoundedCornerShape(16.dp)
    val fill: Brush = when {
        !enabled -> SolidColor(BorderColor)
        kind == ButtonKind.Primary -> AccentBrush
        kind == ButtonKind.Tonal -> SolidColor(ElevatedSurface)
        else -> SolidColor(DangerColor)
    }
    val content = when {
        !enabled -> TextMuted
        kind == ButtonKind.Primary -> OnAccent
        kind == ButtonKind.Tonal -> TextPrimary
        else -> Color(0xFF1A0000)
    }
    Row(
        modifier
            .heightIn(min = 52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(fill, shape)
            .then(if (kind == ButtonKind.Tonal) Modifier.border(1.dp, BorderColor, shape) else Modifier)
            .clickable(enabled = active, interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
        } else {
            if (icon != null) { Icon(icon, null, tint = content, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = content, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

enum class ButtonKind { Primary, Tonal, Danger }

@Composable
fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().appear()) {
        if (title != null) {
            Text(
                title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
            )
        }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)), content = content)
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
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (danger) DangerColor else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(BorderColor))
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
        modifier.fillMaxWidth().padding(32.dp).appear(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(34.dp)) }
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
        containerColor = Color.Transparent,
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
                    containerColor = Color.Transparent, scrolledContainerColor = DeepSurface.copy(alpha = 0.92f)
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
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, color = TextSecondary, lineHeight = 22.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (danger) DangerColor else AccentBlue, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = TextSecondary) } }
    )
}
-e 

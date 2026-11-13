package com.hiyako.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val OledBlack = Color(0xFF000000)
val DeepSurface = Color(0xFF0A0A0A)
val SecondarySurface = Color(0xFF141414)
val UserBubble = Color(0xFF2A2A2A)
val BorderColor = Color(0xFF27272A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val AccentBlue = Color(0xFF3B82F6)
val CardDark = Color(0xFF141414)
val TextMuted = Color(0xFF71717A)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = SecondarySurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}

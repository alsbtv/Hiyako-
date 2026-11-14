package com.hiyako.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val OledBlack = Color(0xFF000000)
val DeepSurface = Color(0xFF0A0A0A)
val SecondarySurface = Color(0xFF141414)
val ElevatedSurface = Color(0xFF1B1B1B)
val UserBubble = Color(0xFF2A2A2A)
val BorderColor = Color(0xFF27272A)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF71717A)
val AccentBlue = Color(0xFF3B82F6)
val CardDark = Color(0xFF141414)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF4F4F5)
val LightUserBubble = Color(0xFFE4E4E7)
val LightTextPrimary = Color(0xFF18181B)
val LightTextSecondary = Color(0xFF52525B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue, onPrimary = TextPrimary, background = OledBlack, onBackground = TextPrimary, surface = DeepSurface, onSurface = TextPrimary, surfaceVariant = SecondarySurface, onSurfaceVariant = TextSecondary, outline = BorderColor
)
private val LightColorScheme = lightColorScheme(
    primary = AccentBlue, onPrimary = Color.White, background = LightBackground, onBackground = LightTextPrimary, surface = LightSurface, onSurface = LightTextPrimary, surfaceVariant = LightUserBubble, onSurfaceVariant = LightTextSecondary, outline = Color.LightGray
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

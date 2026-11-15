package com.hiyako.app.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Snapshot-state holder so every color below follows the active theme automatically,
 * without having to change the call sites in the other screens.
 */
object ThemeState {
    var isDark by mutableStateOf(true)
}

val OledBlack: Color get() = if (ThemeState.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
val DeepSurface: Color get() = if (ThemeState.isDark) Color(0xFF0A0A0A) else Color(0xFFFAFAFA)
val SecondarySurface: Color get() = if (ThemeState.isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
val ElevatedSurface: Color get() = if (ThemeState.isDark) Color(0xFF1B1B1B) else Color(0xFFE9E9EC)
val UserBubble: Color get() = if (ThemeState.isDark) Color(0xFF2A2A2A) else Color(0xFFE4E4E7)
val BorderColor: Color get() = if (ThemeState.isDark) Color(0xFF27272A) else Color(0xFFD4D4D8)
val TextPrimary: Color get() = if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF18181B)
val TextSecondary: Color get() = if (ThemeState.isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
val TextMuted: Color get() = if (ThemeState.isDark) Color(0xFF71717A) else Color(0xFF8A8A93)
val CardDark: Color get() = if (ThemeState.isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
val AccentBlue = Color(0xFF3B82F6)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF4F4F5)
val LightUserBubble = Color(0xFFE4E4E7)
val LightTextPrimary = Color(0xFF18181B)
val LightTextSecondary = Color(0xFF52525B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightUserBubble,
    onSurfaceVariant = LightTextSecondary,
    outline = Color.LightGray
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    if (ThemeState.isDark != darkTheme) ThemeState.isDark = darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = colorScheme.background.toArgb()
            window.statusBarColor = bg
            window.navigationBarColor = bg
            window.setBackgroundDrawable(ColorDrawable(bg))
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

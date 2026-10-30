package com.ejemplo.hiyako.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentColor,
    onPrimary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    secondary = TextSecondary
)

@Composable
fun HiyakoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

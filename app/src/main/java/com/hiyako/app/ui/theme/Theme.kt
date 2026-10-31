package com.hiyako.app.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val OledBlack = Color(0xFF000000)
val AccentCyan = Color(0xFF00E5FF)
val TextPrimary = Color(0xFFFFFFFF)
private val DarkColorScheme = darkColorScheme(primary = AccentCyan, background = OledBlack, surface = Color(0xFF1A1A1A))
@Composable fun AppTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = DarkColorScheme, content = content) }

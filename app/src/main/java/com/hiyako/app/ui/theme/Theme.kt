package com.hiyako.app.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val OledBlack = Color(0xFF0F0F0F)
val CardDark = Color(0xFF1E1E1E)
val TextPrimary = Color(0xFFE3E3E3)
val TextSecondary = Color(0xFF888888)
val AccentBlue = Color(0xFF3B82F6)
private val DarkColorScheme = darkColorScheme(primary = AccentBlue, background = OledBlack, surface = CardDark)
@Composable fun AppTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = DarkColorScheme, content = content) }

package com.ejemplo.hiyako.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BaseColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HiyakoTheme(
    themeColor: String = "Red",
    content: @Composable () -> Unit
) {
    val primaryColor = when (themeColor) {
        "Green" -> GreenPrimary
        "Purple" -> PurplePrimary
        "Blue" -> BluePrimary
        else -> RedPrimary
    }
    val secondaryColor = when (themeColor) {
        "Green" -> GreenSecondary
        "Purple" -> PurpleSecondary
        "Blue" -> BlueSecondary
        else -> RedSecondary
    }

    val colorScheme = BaseColorScheme.copy(
        primary = primaryColor,
        secondary = secondaryColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

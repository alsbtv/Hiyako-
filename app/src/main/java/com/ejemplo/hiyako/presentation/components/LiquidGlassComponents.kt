package com.ejemplo.hiyako.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.7f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.White.copy(alpha = glassAlpha * 0.3f),
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = glassAlpha * 0.2f))
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.White.copy(alpha = glassAlpha * 0.5f),
            shape = shape,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
fun LiquidGlassCardDark(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.Black.copy(alpha = glassAlpha * 0.4f),
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = glassAlpha * 0.3f))
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.Black.copy(alpha = glassAlpha * 0.6f),
            shape = shape,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

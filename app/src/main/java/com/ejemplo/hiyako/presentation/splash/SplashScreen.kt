package com.ejemplo.hiyako.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(1000), label = "alpha")
    val scaleAnim = animateFloatAsState(targetValue = if (startAnimation) 1f else 0.5f, animationSpec = tween(1000), label = "scale")

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000)
        navController.navigate("home") { popUpTo("splash") { inclusive = true } }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B131D)), contentAlignment = Alignment.Center) {
        Text(text = "Hiyako", color = Color(0xFFE91E63), fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(alphaAnim.value).scale(scaleAnim.value))
    }
}

package com.hiyako.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.Brand
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Quiet launch screen: the mark fades in, then the name, then everything fades out into the app. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val mark = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { mark.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { delay(350); name.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { delay(700); tagline.animateTo(1f, tween(600)) }
        }
        delay(450)
        exit.animateTo(1f, tween(400))
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = 1f - exit.value },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        alpha = mark.value
                        scaleX = 0.92f + 0.08f * mark.value
                        scaleY = 0.92f + 0.08f * mark.value
                    }
                    .background(AccentBlue.copy(alpha = 0.14f), CircleShape)
                    .border(1.dp, AccentBlue.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = AccentBlue, fontSize = 40.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                tr(Brand.EN, Brand.AR),
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { alpha = name.value; translationY = (1f - name.value) * 10f }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Your AI, everywhere", "ذكاؤك الاصطناعي في كل مكان"),
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer { alpha = tagline.value }
            )
        }
    }
}

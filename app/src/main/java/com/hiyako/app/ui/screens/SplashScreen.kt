package com.hiyako.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiyako.app.ui.i18n.tr
import com.hiyako.app.ui.theme.AccentBlue
import com.hiyako.app.ui.theme.AccentBrush
import com.hiyako.app.ui.theme.AccentThree
import com.hiyako.app.ui.theme.AccentTwo
import com.hiyako.app.ui.theme.Brand
import com.hiyako.app.ui.theme.OnAccent
import com.hiyako.app.ui.theme.TextMuted
import com.hiyako.app.ui.theme.TextPrimary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated launch screen: pulse rings, a colour ring that draws itself, orbiting lights and a logo that
 * springs in. It plays over the app's aurora background, then fades out into the app.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val pop = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val transition = rememberInfiniteTransition(label = "splash")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "spin")
    val wave by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "wave")

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
            launch { ring.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
            launch { delay(650); title.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { delay(1000); tagline.animateTo(1f, tween(600)) }
        }
        delay(500)
        exit.animateTo(1f, tween(450))
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = 1f - exit.value
            scaleX = 1f + exit.value * 0.12f
            scaleY = 1f + exit.value * 0.12f
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(280.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = 74.dp.toPx()
                    for (k in 0..2) {
                        val phase = (wave + k / 3f) % 1f
                        drawCircle(
                            color = AccentBlue.copy(alpha = (1f - phase) * 0.35f),
                            radius = baseRadius + phase * 62.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    val ringRadius = 86.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(listOf(AccentBlue, AccentTwo, AccentThree, AccentBlue), center),
                        startAngle = -90f,
                        sweepAngle = 360f * ring.value,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2f, ringRadius * 2f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val orbit = 104.dp.toPx()
                    val colors = listOf(AccentBlue, AccentTwo, AccentThree)
                    for (k in 0..2) {
                        val angle = Math.toRadians((spin + k * 120f).toDouble())
                        val dot = Offset(
                            center.x + (Math.cos(angle) * orbit).toFloat(),
                            center.y + (Math.sin(angle) * orbit).toFloat()
                        )
                        drawCircle(
                            brush = Brush.radialGradient(listOf(colors[k].copy(alpha = 0.9f), colors[k].copy(alpha = 0f)), center = dot, radius = 16.dp.toPx()),
                            radius = 16.dp.toPx(),
                            center = dot
                        )
                        drawCircle(color = colors[k], radius = 4.dp.toPx(), center = dot)
                    }
                }
                Box(
                    Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            rotationZ = (1f - pop.value.coerceIn(0f, 1f)) * -120f
                        }
                        .background(AccentBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = OnAccent, fontSize = 50.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                tr(Brand.EN, Brand.AR),
                color = TextPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 40f
                    scaleX = 0.85f + 0.15f * title.value
                    scaleY = 0.85f + 0.15f * title.value
                }
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

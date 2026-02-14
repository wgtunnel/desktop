package com.zaneschepke.wireguardautotunnel.desktop.ui.common.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.AlertRed
import com.zaneschepke.wireguardautotunnel.desktop.ui.theme.SilverTree

@Composable
fun PulsingStatusLed(isHealthy: Boolean, modifier: Modifier = Modifier) {
    val color = if (isHealthy) SilverTree else AlertRed

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val scale by
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 2.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "PulseScale",
        )

    val alpha by
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "PulseAlpha",
        )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(28.dp)) {
        Box(
            Modifier.size(12.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(color, CircleShape)
        )

        Box(
            Modifier.size(10.dp)
                .background(color, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotActivityIndicator(
    modifier: Modifier = Modifier,
    dotCount: Int = 5,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp,
    activeColor: Color = Color(0xFF00FF00), // bright green
    inactiveColor: Color = Color(0x55FFFFFF) // semi-transparent white/gray
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = dotCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotAnimation"
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until dotCount) {
            val isActive = i < dotAnimation
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color = if (isActive) activeColor else inactiveColor, shape = CircleShape)
            )
        }
    }
}

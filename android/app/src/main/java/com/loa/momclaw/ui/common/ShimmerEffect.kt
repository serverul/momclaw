package com.loa.momclaw.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple shimmer loading placeholder components for Material3 compliance.
 * Use these in loading states to give visual feedback while content loads.
 */
object ShimmerEffect {

    /**
     * A single shimmer line placeholder
     */
    @Composable
    fun ShimmerLine(
        width: Dp = 200.dp,
        height: Dp = 16.dp,
        shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
    }

    /**
     * Circular shimmer placeholder (for avatars, icons)
     */
    @Composable
    fun ShimmerCircle(size: Dp = 40.dp) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmerCircle")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerCircleAlpha"
        )

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
    }

    /**
     * Message shimmer placeholder (for chat loading)
     */
    @Composable
    fun ShimmerMessage(isUser: Boolean = false) {
        Column {
            if (isUser) {
                Row(modifier = Modifier.width(240.dp)) {
                    ShimmerLine(width = 240.dp, height = 48.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                }
            } else {
                Row(modifier = Modifier.width(200.dp)) {
                    ShimmerCircle(size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        ShimmerLine(width = 180.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerLine(width = 140.dp, height = 12.dp)
                    }
                }
            }
        }
    }

    /**
     * Model card shimmer placeholder
     */
    @Composable
    fun ShimmerModelCard() {
        Row {
            ShimmerCircle(size = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                ShimmerLine(width = 160.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLine(width = 80.dp, height = 12.dp)
            }
        }
    }
}

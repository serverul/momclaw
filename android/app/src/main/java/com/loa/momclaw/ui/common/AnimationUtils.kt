package com.loa.momclaw.ui.common

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/**
 * Shared animation utilities to optimize performance by providing
 * reusable composable animation state producers.
 */
object AnimationUtils {

    /**
     * Pulsing animation - oscillates between initialValue and targetValue
     * Used for loading dots and indicators
     */
    @Composable
    fun rememberPulsingState(
        initialValue: Float = 0.3f,
        targetValue: Float = 1f,
        durationMs: Int = 600,
        delayMs: Int = 0
    ): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
        return infiniteTransition.animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, delayMillis = delayMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    }

    /**
     * Blinking animation - oscillates alpha between two values
     * Used for streaming cursor
     */
    @Composable
    fun rememberBlinkingState(
        initialValue: Float = 1f,
        targetValue: Float = 0.2f,
        durationMs: Int = 530
    ): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "blinking")
        return infiniteTransition.animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blink"
        )
    }

    /**
     * Rotation animation - continuous 0 to 360
     * Used for loading spinners
     */
    @Composable
    fun rememberRotationState(
        durationMs: Int = 1000
    ): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing)
            ),
            label = "rotation"
        )
    }
}

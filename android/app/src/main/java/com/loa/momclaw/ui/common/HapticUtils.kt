package com.loa.momclaw.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Haptic feedback utilities for Material3 compliance
 * Provides tactile feedback for user interactions
 */
object HapticUtils {

    /**
     * Creates a haptic feedback manager
     */
    @Composable
    fun rememberHapticManager(): HapticManager {
        val context = LocalContext.current
        return remember { HapticManager(context) }
    }
}

/**
 * Manages haptic feedback for user interactions
 */
class HapticManager(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    /**
     * Light tap feedback - for general button presses
     */
    fun lightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    /**
     * Medium tap feedback - for important actions
     */
    fun mediumTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /**
     * Heavy tap feedback - for destructive actions or warnings
     */
    fun heavyTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    /**
     * Success feedback - pattern for successful operations
     */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 10, 50, 10),
                    intArrayOf(0, 100, 0, 50),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 10, 50, 10), -1)
        }
    }

    /**
     * Error feedback - pattern for errors
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 50, 30, 50, 30),
                    intArrayOf(0, 100, 0, 100, 0, 100),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 50, 30, 50, 30), -1)
        }
    }

    /**
     * Tick feedback - for scrolling or selection changes
     */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5, 30))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(5)
        }
    }

    /**
     * Double click feedback
     */
    fun doubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 30, 20),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 20, 30, 20), -1)
        }
    }
}

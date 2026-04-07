package com.loa.momclaw.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Premium screen transition animations
 * 10/10 UI features
 */
object ScreenTransitions {
    
    /**
     * Fade in + slide up transition (default for new screens)
     */
    @Composable
    fun fadeInSlideUp(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it / 4 } // Slide from bottom
        )
    }
    
    /**
     * Fade out + slide down transition (default for exiting screens)
     */
    @Composable
    fun fadeOutSlideDown(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            ),
            targetOffsetY = { it / 4 }
        )
    }
    
    /**
     * Scale in transition (for dialogs/modals)
     */
    @Composable
    fun scaleIn(): EnterTransition {
        return fadeIn(
            animationSpec = tween(200)
        ) + androidx.compose.animation.scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialScale = 0.9f
        )
    }
    
    /**
     * Scale out transition (for dialogs/modals)
     */
    @Composable
    fun scaleOut(): ExitTransition {
        return fadeOut(
            animationSpec = tween(150)
        ) + androidx.compose.animation.scaleOut(
            animationSpec = tween(150, easing = FastOutLinearInEasing),
            targetScale = 0.9f
        )
    }
    
    /**
     * Slide from right (for forward navigation)
     */
    @Composable
    fun slideInFromRight(): EnterTransition {
        return slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { it }
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }
    
    /**
     * Slide to right (for back navigation)
     */
    @Composable
    fun slideOutToRight(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(200, easing = FastOutLinearInEasing),
            targetOffsetX = { it }
        ) + fadeOut(
            animationSpec = tween(200)
        )
    }
    
    /**
     * Slide from left (for back navigation)
     */
    @Composable
    fun slideInFromLeft(): EnterTransition {
        return slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetX = { -it }
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }
    
    /**
     * Slide to left (for forward navigation)
     */
    @Composable
    fun slideOutToLeft(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(200, easing = FastOutLinearInEasing),
            targetOffsetX = { -it }
        ) + fadeOut(
            animationSpec = tween(200)
        )
    }
}

/**
 * Modifier extension for animated visibility with standard transitions
 */
fun Modifier.animateVisibility(
    visible: Boolean,
    enter: EnterTransition = fadeIn() + slideInVertically(),
    exit: ExitTransition = fadeOut() + slideOutVertically()
): Modifier {
    // This is just a placeholder - actual implementation would use AnimatedVisibility
    return this
}

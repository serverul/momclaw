package com.loa.momclaw.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Responsive utilities for proper WindowInsets handling.
 * Ensures content doesn't overlap with system bars or IME.
 */
object ResponsiveUtils {
    
    /**
     * Modifier that handles system bars (status bar, navigation bar) and IME.
     */
    @Composable
    fun Modifier.safeContentPadding(): Modifier {
        return this
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    }
    
    /**
     * Modifier for screens with bottom navigation.
     */
    @Composable
    fun Modifier.withBottomNavPadding(): Modifier {
        return this
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    }
    
    /**
     * Column with proper content padding.
     */
    @Composable
    fun SafeColumn(
        modifier: Modifier = Modifier,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        horizontalAlignment: Alignment.HorizontalHorizontally = Alignment.Start,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = modifier.safeContentPadding(),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
    
    /**
     * Screen dimension utilities.
     */
    @Composable
    fun rememberScreenSize(): ScreenSize {
        val windowInsets = WindowInsets.systemBars
        val screenHeight = windowInsets.getBottom(LocalDensity.current) - windowInsets.getTop(LocalDensity.current)
        
        return when {
            screenHeight < 600.dp -> ScreenSize.Compact
            screenHeight < 800.dp -> ScreenSize.Medium
            else -> ScreenSize.Expanded
        }
    }
    
    /**
     * Responsive padding based on screen size.
     */
    @Composable
    fun Modifier.responsivePadding(
        small: Dp = 8.dp,
        medium: Dp = 16.dp,
        large: Dp = 24.dp
    ): Modifier {
        val screenSize = rememberScreenSize()
        val padding = when (screenSize) {
            ScreenSize.Compact -> small
            ScreenSize.Medium -> medium
            ScreenSize.Expanded -> large
        }
        
        return this.padding(padding)
    }
}

/**
 * Screen size classification.
 */
enum class ScreenSize {
    Compact,   // Small phones
    Medium,    // Large phones, small tablets
    Expanded   // Tablets, large screens
}

/**
 * Extension functions for common responsive patterns.
 */
@Composable
fun Modifier.safeDrawingPadding(): Modifier {
    return this.padding(WindowInsets.systemBars.asPaddingValues())
}

@Composable
fun Modifier.safeStatusBarPadding(): Modifier {
    return this.statusBarsPadding()
}

@Composable
fun Modifier.safeNavigationBarPadding(): Modifier {
    return this.navigationBarsPadding()
}

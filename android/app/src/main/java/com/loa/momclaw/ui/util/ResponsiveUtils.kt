package com.loa.momclaw.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive utilities for proper WindowInsets handling and adaptive layouts.
 * Ensures content doesn't overlap with system bars or IME and adapts to different screen sizes.
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
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
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
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        
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
    
    /**
     * Responsive content max width for tablets.
     */
    @Composable
    fun Modifier.adaptiveContentWidth(
        compactMaxWidth: Dp = Dp.Infinity,
        expandedMaxWidth: Dp = 600.dp
    ): Modifier {
        val screenSize = rememberScreenSize()
        return if (screenSize == ScreenSize.Expanded) {
            this
                .fillMaxWidth()
                .widthIn(max = expandedMaxWidth)
        } else {
            this.fillMaxWidth()
        }
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
 * Layout type based on screen dimensions.
 */
enum class LayoutType {
    SingleColumn,    // Phone portrait
    TwoColumn,       // Phone landscape, small tablet
    ThreeColumn      // Large tablet
}

/**
 * Determine layout type based on window size class.
 */
@Composable
fun rememberLayoutType(windowSizeClass: WindowSizeClass): LayoutType {
    return remember(windowSizeClass) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> LayoutType.SingleColumn
            WindowWidthSizeClass.Medium -> LayoutType.TwoColumn
            else -> LayoutType.ThreeColumn
        }
    }
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

/**
 * Responsive dimension values.
 */
object ResponsiveDimensions {
    val CardElevation = 2.dp
    val CardElevationPressed = 4.dp
    val ListItemHeight = 56.dp
    val ListItemHeightCompact = 48.dp
    val BottomNavHeight = 80.dp
    val TopBarHeight = 64.dp
    val TopBarHeightCompact = 56.dp
    val MinTouchTarget = 48.dp
    val ButtonHeight = 48.dp
    val ButtonHeightLarge = 56.dp
    val ChipHeight = 32.dp
    val IconSize = 24.dp
    val IconSizeSmall = 18.dp
    val IconSizeLarge = 32.dp
    val AvatarSize = 40.dp
    val AvatarSizeLarge = 56.dp
    val CardCornerRadius = 16.dp
    val ButtonCornerRadius = 24.dp
}

/**
 * Responsive grid for adaptive layouts.
 */
@Composable
fun rememberGridColumns(
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3
): Int {
    val screenSize = ResponsiveUtils.rememberScreenSize()
    return when (screenSize) {
        ScreenSize.Compact -> compact
        ScreenSize.Medium -> medium
        ScreenSize.Expanded -> expanded
    }
}

/**
 * Content padding for lists based on screen size.
 */
@Composable
fun rememberContentPadding(): PaddingValues {
    val screenSize = ResponsiveUtils.rememberScreenSize()
    return when (screenSize) {
        ScreenSize.Compact -> PaddingValues(8.dp)
        ScreenSize.Medium -> PaddingValues(16.dp)
        ScreenSize.Expanded -> PaddingValues(24.dp, 16.dp)
    }
}

/**
 * Horizontal padding for cards/content.
 */
@Composable
fun rememberHorizontalPadding(): Dp {
    val screenSize = ResponsiveUtils.rememberScreenSize()
    return when (screenSize) {
        ScreenSize.Compact -> 8.dp
        ScreenSize.Medium -> 16.dp
        ScreenSize.Expanded -> 24.dp
    }
}

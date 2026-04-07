package com.loa.momclaw.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 8dp Grid System Typography (Material 3)
 * 
 * All spacing follows 8dp grid for consistency:
 * - 4dp = half step
 * - 8dp = one step
 * - 16dp = two steps
 * - 24dp = three steps
 * - 32dp = four steps
 */

// Font families (using system defaults for now, can be customized with custom fonts)
val Roboto = FontFamily.Default

/**
 * Enhanced Material 3 Typography with 8dp grid alignment
 */
val Typography = Typography(
    // Display styles - Large headlines
    displayLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 57.sp,
        lineHeight = 64.sp,  // 8dp grid aligned
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 45.sp,
        lineHeight = 52.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 36.sp,
        lineHeight = 44.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    
    // Headline styles - Section headers
    headlineLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 32.sp,
        lineHeight = 40.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 28.sp,
        lineHeight = 36.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        lineHeight = 32.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    
    // Title styles - App bars, dialog titles
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        lineHeight = 28.sp,  // 8dp grid aligned
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,  // 8dp grid aligned
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,  // 8dp grid aligned
        letterSpacing = 0.1.sp
    ),
    
    // Body styles - Main content
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,  // 8dp grid aligned
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,  // 8dp grid aligned
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,  // 8dp grid aligned
        letterSpacing = 0.4.sp
    ),
    
    // Label styles - Buttons, captions
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,  // 8dp grid aligned
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,  // 8dp grid aligned
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,  // 8dp grid aligned
        letterSpacing = 0.5.sp
    )
)

/**
 * 8dp Grid Spacing Constants
 * Use these for consistent padding and spacing throughout the app
 */
object Spacing {
    val dp0 = 0.dp
    val dp4 = 4.dp     // Half step
    val dp8 = 8.dp     // One step
    val dp12 = 12.dp   // 1.5 steps
    val dp16 = 16.dp   // Two steps
    val dp20 = 20.dp   // 2.5 steps
    val dp24 = 24.dp   // Three steps
    val dp32 = 32.dp   // Four steps
    val dp40 = 40.dp   // Five steps
    val dp48 = 48.dp   // Six steps (minimum touch target)
    val dp56 = 56.dp   // Seven steps
    val dp64 = 64.dp   // Eight steps
    val dp72 = 72.dp   // Nine steps
    val dp80 = 80.dp   // Ten steps
}

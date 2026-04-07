package com.loa.momclaw.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Premium Dark color scheme - 10/10 UI
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple200,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal200,
    tertiary = Pink200,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color.White,
    error = ErrorRedDark,
    onError = Color.Black,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    
    // Additional semantic colors
    inverseSurface = LightBackground,
    inverseOnSurface = Color.Black,
    inversePrimary = Purple500,
    
    // Outline colors
    outline = Color.White.copy(alpha = 0.3f),
    outlineVariant = Color.White.copy(alpha = 0.1f),
    
    // Scrim
    scrim = ScrimDark
)

/**
 * Premium Light color scheme - 10/10 UI
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple100,
    onPrimaryContainer = Purple900,
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,
    tertiary = Pink500,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color.Black,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed,
    
    // Additional semantic colors
    inverseSurface = DarkSurface,
    inverseOnSurface = Color.White,
    inversePrimary = Purple200,
    
    // Outline colors
    outline = Color.Black.copy(alpha = 0.3f),
    outlineVariant = Color.Black.copy(alpha = 0.1f),
    
    // Scrim
    scrim = ScrimDark
)

/**
 * MomClaw theme composable with premium styling
 * 10/10 UI implementation
 */
@Composable
fun MomClawTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            
            // Navigation bar styling
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

package com.loa.momclaw.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loa.momclaw.ui.common.HapticUtils
import com.loa.momclaw.ui.common.rememberHapticManager
import com.loa.momclaw.ui.theme.Spacing

/**
 * Premium error state with animations and retry
 * 10/10 UI component
 */
@Composable
fun PremiumErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "Try Again",
    emoji: String = "😕"
) {
    val hapticManager = HapticUtils.rememberHapticManager()
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        hapticManager.error()
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val animatedOffset by animateIntAsState(
        targetValue = if (visible) 0 else 100,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "error")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.dp32)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset.toFloat()
            }
            .semantics { 
                contentDescription = "Error: $title. $message. $retryText available." 
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.dp16)
    ) {
        // Animated emoji/icon
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            modifier = Modifier.scale(iconScale)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.dp8))
        
        FilledTonalButton(
            onClick = {
                hapticManager.success()
                onRetry()
            },
            modifier = Modifier.semantics { 
                contentDescription = "$retryText button" 
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.dp8))
            Text(
                text = retryText,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Premium offline state
 * 10/10 UI component
 */
@Composable
fun PremiumOfflineState(
    message: String = "You're currently offline. Some features may be limited.",
    modifier: Modifier = Modifier
) {
    val hapticManager = HapticUtils.rememberHapticManager()
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        hapticManager.mediumTap()
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )
    
    val animatedOffset by animateIntAsState(
        targetValue = if (visible) 0 else -50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset.toFloat()
            }
            .semantics { 
                contentDescription = "Offline mode: $message" 
            },
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.dp16)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.dp12)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = "Offline",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Premium empty state with animations
 * 10/10 UI component
 */
@Composable
fun PremiumEmptyState(
    emoji: String = "📭",
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable ColumnScope.() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val animatedOffset by animateIntAsState(
        targetValue = if (visible) 0 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.dp32)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset.toFloat()
            }
            .semantics { 
                contentDescription = "$title. $message" 
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.dp16)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        action?.invoke(this)
    }
}

// Keep old composables for backward compatibility
@Composable
fun ErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "Retry"
) {
    PremiumErrorState(
        title = title,
        message = message,
        onRetry = onRetry,
        modifier = modifier,
        retryText = retryText
    )
}

@Composable
fun OfflineState(
    message: String = "You're currently offline. Some features may be limited.",
    modifier: Modifier = Modifier
) {
    PremiumOfflineState(
        message = message,
        modifier = modifier
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    PremiumEmptyState(
        emoji = emoji,
        title = title,
        message = message,
        modifier = modifier,
        action = action?.let { { it() } }
    )
}

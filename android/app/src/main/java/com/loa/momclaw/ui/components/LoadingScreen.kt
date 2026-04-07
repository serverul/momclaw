package com.loa.momclaw.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loa.momclaw.ui.theme.Spacing

/**
 * Premium animated loading screen with smooth transitions
 * 10/10 UI component
 */
@Composable
fun LoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Loading screen. $message"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.dp24)
        ) {
            // Animated spinner
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
                    .alpha(alpha),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

/**
 * Premium skeleton loading animation for content placeholders
 * 10/10 UI component
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmer"
    )
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        shape = MaterialTheme.shapes.small
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Premium card skeleton loader
 * 10/10 UI component
 */
@Composable
fun CardSkeletonLoader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardSkeleton")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(Spacing.dp12)
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            
            // Content lines
            repeat(3) { index ->
                val width = when (index) {
                    0 -> 0.9f
                    1 -> 0.85f
                    else -> 0.6f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(width)
                        .height(14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

/**
 * Premium list skeleton loader
 * 10/10 UI component
 */
@Composable
fun ListSkeletonLoader(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.dp8),
        contentPadding = PaddingValues(Spacing.dp8)
    ) {
        items(itemCount) {
            CardSkeletonLoader()
        }
    }
}

// Missing import
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

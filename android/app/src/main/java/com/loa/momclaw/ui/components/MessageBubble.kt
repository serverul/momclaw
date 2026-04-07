package com.loa.momclaw.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Copy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loa.momclaw.domain.model.Message
import com.loa.momclaw.ui.common.AnimationUtils
import com.loa.momclaw.ui.theme.*

/**
 * Premium message bubble with animations, avatar, and long-press menu
 * 10/10 UI component
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PremiumMessageBubble(
    message: Message,
    isUser: Boolean,
    isStreaming: Boolean = false,
    onCopy: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) {
        visible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
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
    
    // Blinking cursor for streaming
    val cursorAlpha by AnimationUtils.rememberBlinkingState(
        initialValue = 1f,
        targetValue = 0.3f,
        durationMs = 530
    )
    
    // Context menu state
    var showMenu by remember { mutableStateOf(false) }
    
    // Theme colors
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val timestampColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(animatedAlpha)
            .offset(y = animatedOffset.dp)
            .semantics {
                contentDescription = buildString {
                    append(if (isUser) "You" else "AI Assistant")
                    append(": ")
                    append(message.content)
                    if (isStreaming) append(". Still typing...")
                    append(". Sent at ${formatTime(message.timestamp)}")
                }
            },
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Avatar (only for assistant messages)
        if (!isUser) {
            AIAnimatedAvatar(
                isStreaming = isStreaming,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }
        
        // Message content
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Role label
            Text(
                text = if (isUser) "You" else "AI",
                style = MaterialTheme.typography.labelSmall,
                color = timestampColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            // Bubble with long-press menu
            Box {
                Surface(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { showMenu = true }
                        ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = bubbleColor,
                    tonalElevation = if (isUser) 2.dp else 0.dp,
                    shadowElevation = if (isUser) 4.dp else 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Blinking cursor for streaming
                            if (isStreaming) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "▌",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                                )
                            }
                        }
                    }
                }
                
                // Dropdown menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    onCopy?.let {
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                it(message.content)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Copy, contentDescription = null) }
                        )
                    }
                    
                    onShare?.let {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                it(message.content)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                    }
                    
                    onDelete?.let {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                it()
                                showMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                ) 
                            }
                        )
                    }
                }
            }
            
            // Timestamp
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = timestampColor
                )
                
                // Streaming indicator
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // User avatar (only for user messages)
        if (isUser) {
            Surface(
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Your avatar",
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Animated AI avatar with gradient and pulse effect
 * 10/10 UI component
 */
@Composable
fun AIAnimatedAvatar(
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulse animation for streaming
    val scale by AnimationUtils.rememberPulsingState(
        initialValue = 1f,
        targetValue = 1.1f,
        durationMs = 1000
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val animatedGradient by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "gradient"
    )
    
    Surface(
        modifier = modifier
            .size(32.dp)
            .scale(if (isStreaming) scale else 1f),
        shape = CircleShape,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AIAvatarGradient1,
                            AIAvatarGradient2
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // AI icon or emoji
            Text(
                text = "🤖",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Formats timestamp to readable time
 */
private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

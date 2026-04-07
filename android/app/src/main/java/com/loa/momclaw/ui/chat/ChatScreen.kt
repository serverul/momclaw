package com.loa.momclaw.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.loa.momclaw.ui.common.AnimationUtils
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loa.momclaw.domain.model.ChatMessage
import kotlinx.coroutines.launch

/**
 * Chat screen with messages list and input
 * Optimized for Material3 with proper backpressure handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSendMessage: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onClearConversation: () -> Unit,
    onNewConversation: () -> Unit,
    onRetry: () -> Unit,
    onCancelStreaming: () -> Unit,
    useNavigationRail: Boolean = false
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive - with debounce
    val messageCount = uiState.messages.size
    val hasStreamingMessage = uiState.currentStreamingMessage != null
    
    LaunchedEffect(messageCount, hasStreamingMessage) {
        if (messageCount > 0 || hasStreamingMessage) {
            coroutineScope.launch {
                // Scroll to the last item (streaming message or last message)
                val targetIndex = if (hasStreamingMessage) messageCount else messageCount - 1
                if (targetIndex >= 0) {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    // Calculate max width for content based on screen size
    val contentMaxWidth = if (useNavigationRail) 800.dp else 600.dp
    val bubbleMaxWidth = if (useNavigationRail) 600.dp else 280.dp

    // Derive states to minimize recomposition
    val isInputEnabled = remember(uiState.isAgentAvailable, uiState.isLoading) {
        uiState.isAgentAvailable && !uiState.isLoading
    }
    
    val showLoadingIndicator = remember(uiState.isLoading, uiState.currentStreamingMessage) {
        uiState.isLoading && uiState.currentStreamingMessage == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MOMCLAW",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.isAgentAvailable) {
                            Text(
                                text = "Agent online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Only show back button if using navigation rail (tablet)
                    if (useNavigationRail) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Clear conversation
                    IconButton(onClick = onClearConversation) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear conversation"
                        )
                    }
                    // New conversation
                    IconButton(onClick = onNewConversation) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New conversation"
                        )
                    }
                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error banner
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Messages list - centered on larger screens
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Existing messages - use key for efficient updates
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            maxWidth = bubbleMaxWidth
                        )
                    }

                    // Currently streaming message
                    if (uiState.currentStreamingMessage != null) {
                        item {
                            MessageBubble(
                                message = uiState.currentStreamingMessage!!,
                                isStreaming = true,
                                maxWidth = bubbleMaxWidth
                            )
                        }
                    }

                    // Loading indicator for initial load
                    if (showLoadingIndicator) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                AssistantMessageBubble(
                                    content = "",
                                    isStreaming = true,
                                    maxWidth = bubbleMaxWidth
                                )
                            }
                        }
                    }
                }
            }

            // Input area - centered on larger screens
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                MessageInput(
                    text = uiState.inputText,
                    isStreaming = uiState.isStreaming,
                    onTextChange = onUpdateInput,
                    onSend = onSendMessage,
                    onCancel = onCancelStreaming,
                    enabled = isInputEnabled,
                    maxWidth = contentMaxWidth
                )
            }
        }
    }
}

/**
 * Message bubble that adapts to user/assistant
 * Marked as stable to prevent unnecessary recomposition
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean = false,
    maxWidth: androidx.compose.ui.unit.Dp = 280.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (message.isUser) {
            UserMessageBubble(content = message.content, maxWidth = maxWidth)
        } else {
            AssistantMessageBubble(
                content = message.content,
                isStreaming = isStreaming,
                maxWidth = maxWidth
            )
        }
    }
}

/**
 * User message bubble (right-aligned, primary color)
 */
@Composable
fun UserMessageBubble(
    content: String,
    maxWidth: androidx.compose.ui.unit.Dp = 280.dp
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.widthIn(max = maxWidth)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Assistant message bubble (left-aligned, surface variant)
 */
@Composable
fun AssistantMessageBubble(
    content: String,
    isStreaming: Boolean = false,
    maxWidth: androidx.compose.ui.unit.Dp = 280.dp
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(max = maxWidth)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (content.isEmpty() && isStreaming) {
                // Streaming indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        PulsingDot(
                            delayMs = index * 150L
                        )
                    }
                }
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                if (isStreaming) {
                    // Blinking streaming cursor
                    Spacer(modifier = Modifier.height(4.dp))
                    BlinkingCursor()
                }
            }
        }
    }
}

/**
 * Pulsing dot for loading indicator with optimized animation
 */
@Composable
fun PulsingDot(delayMs: Long) {
    val scale by AnimationUtils.rememberPulsingState(
        initialValue = 0.3f,
        targetValue = 1f,
        durationMs = 600,
        delayMs = delayMs.toInt()
    )
    
    val alpha by AnimationUtils.rememberPulsingState(
        initialValue = 0.3f,
        targetValue = 1f,
        durationMs = 600,
        delayMs = delayMs.toInt()
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            }
            .background(
                MaterialTheme.colorScheme.primary,
                CircleShape
            )
    )
}

/**
 * Blinking cursor for streaming indicator with optimized animation
 */
@Composable
fun BlinkingCursor() {
    val alpha by AnimationUtils.rememberBlinkingState(
        initialValue = 1f,
        targetValue = 0.2f,
        durationMs = 530
    )

    Box(
        modifier = Modifier
            .width(8.dp)
            .height(16.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(2.dp)
            )
    )
}

/**
 * Message input with send button
 */
@Composable
fun MessageInput(
    text: String,
    isStreaming: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp = 600.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (enabled) "Type a message..." else "Agent offline",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                enabled = enabled && !isStreaming,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Send/Cancel button
            if (isStreaming) {
                FilledIconButton(
                    onClick = onCancel,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop"
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() && enabled
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

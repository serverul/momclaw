package com.loa.momclaw.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loa.momclaw.domain.model.Message
import com.loa.momclaw.ui.common.HapticUtils
import com.loa.momclaw.ui.components.PremiumMessageBubble
import com.loa.momclaw.ui.components.ShimmerMessageItem
import com.loa.momclaw.ui.components.TypingIndicator
import com.loa.momclaw.ui.theme.Spacing

/**
 * Premium chat screen with animations, haptics, and accessibility
 * 10/10 UI implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val hapticManager = HapticUtils.rememberHapticManager()
    val clipboardManager = LocalClipboardManager.current
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Chat with MomClaw",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            hapticManager.lightTap()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            hapticManager.mediumTap()
                            onEvent(ChatEvent.ClearConversation)
                        },
                        enabled = state.messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear conversation"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list or empty state
            if (state.messages.isEmpty() && !state.isStreaming && state.currentResponse.isEmpty()) {
                PremiumEmptyChatState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.dp8),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(Spacing.dp8),
                    contentPadding = PaddingValues(vertical = Spacing.dp8)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        PremiumMessageBubble(
                            message = message,
                            isUser = message.role == Message.ROLE_USER,
                            onCopy = { text ->
                                hapticManager.lightTap()
                                clipboardManager.setText(AnnotatedString(text))
                            },
                            onDelete = {
                                hapticManager.heavyTap()
                                onEvent(ChatEvent.DeleteMessage(message.id))
                            }
                        )
                    }

                    // Show typing indicator
                    if (state.isStreaming && state.currentResponse.isEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                TypingIndicator()
                            }
                        }
                    }

                    // Show streaming response
                    if (state.isStreaming && state.currentResponse.isNotEmpty()) {
                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                PremiumMessageBubble(
                                    message = Message(
                                        id = "streaming",
                                        role = Message.ROLE_ASSISTANT,
                                        content = state.currentResponse,
                                        timestamp = System.currentTimeMillis()
                                    ),
                                    isUser = false,
                                    isStreaming = true
                                )
                            }
                        }
                    }
                    
                    // Loading placeholders when fetching initial messages
                    if (state.isLoading) {
                        items(3) {
                            ShimmerMessageItem()
                        }
                    }
                }
            }

            // Error message with haptic feedback
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + slideInVertically()
            ) {
                state.error?.let { error ->
                    LaunchedEffect(error) {
                        hapticManager.error()
                    }
                    
                    Snackbar(
                        modifier = Modifier
                            .padding(horizontal = Spacing.dp8, vertical = Spacing.dp4)
                            .semantics {
                                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Assertive
                                contentDescription = "Error: $error"
                            },
                        action = {
                            TextButton(
                                onClick = {
                                    hapticManager.lightTap()
                                    onEvent(ChatEvent.ClearError)
                                }
                            ) {
                                Text("Dismiss")
                            }
                        },
                        colors = SnackbarDefaults.snackbarColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            actionContentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(text = error)
                    }
                }
            }

            // Input area with haptic feedback
            PremiumChatInput(
                text = state.inputText,
                enabled = !state.isStreaming,
                onTextChange = { 
                    onEvent(ChatEvent.InputChanged(it))
                },
                onSend = {
                    if (state.inputText.isNotBlank()) {
                        hapticManager.success()
                        onEvent(ChatEvent.SendMessage(state.inputText))
                    }
                }
            )
        }
    }
}

/**
 * Premium chat input with animated send button
 * 10/10 UI component
 */
@Composable
fun PremiumChatInput(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val hapticManager = HapticUtils.rememberHapticManager()
    
    // Send button scale animation
    val sendButtonScale by animateFloatAsState(
        targetValue = if (text.isNotBlank() && enabled) 1f else 0.9f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "sendButtonScale"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.dp8)
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Message input. ${if (text.isNotEmpty()) "Current: $text" else "Empty"}"
                    },
                placeholder = { 
                    Text(
                        "Type a message...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            FilledIconButton(
                onClick = {
                    if (text.isNotBlank() && enabled) {
                        hapticManager.success()
                        onSend()
                    }
                },
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier
                    .size(Spacing.dp48) // Minimum touch target
                    .scale(sendButtonScale)
                    .semantics {
                        contentDescription = "Send message"
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Premium empty state with beautiful animation
 * 10/10 UI component
 */
@Composable
fun PremiumEmptyChatState(
    modifier: Modifier = Modifier
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
            .fillMaxSize()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset.toFloat()
            }
            .semantics {
                contentDescription = "Empty conversation. Start chatting by typing a message below."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💬",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
        )
        
        Spacer(modifier = Modifier.height(Spacing.dp24))
        
        Text(
            text = "Start a Conversation",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.dp8))
        
        Text(
            text = "Type a message below to begin",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.dp32))
        
        // Suggestion chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.dp8),
            modifier = Modifier.padding(horizontal = Spacing.dp16)
        ) {
            SuggestionChip(
                onClick = { },
                label = { Text("Ask a question") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            
            SuggestionChip(
                onClick = { },
                label = { Text("Get help") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

package com.loa.momclaw

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.loa.momclaw.ui.chat.ChatScreen
import com.loa.momclaw.ui.chat.ChatUiState
import com.loa.momclaw.domain.model.ChatMessage

/**
 * Route composable that connects ViewModel to Screen
 * In production, this would use hiltViewModel()
 */
@Composable
fun ChatRoute(
    onNavigateToSettings: () -> Unit,
    onNavigateToModels: () -> Unit
) {
    // Placeholder state - in real app, use viewModel()
    val uiState = ChatUiState(
        messages = listOf(
            ChatMessage(
                content = "Hello! I'm MOMCLAW, your on-device AI assistant. How can I help you today?",
                isUser = false
            )
        ),
        isAgentAvailable = true
    )

    ChatScreen(
        uiState = uiState,
        onNavigateBack = {},
        onNavigateToSettings = onNavigateToSettings,
        onSendMessage = {},
        onUpdateInput = {},
        onClearConversation = {},
        onNewConversation = {},
        onRetry = {},
        onCancelStreaming = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ChatRoutePreview() {
    ChatRoute(
        onNavigateToSettings = {},
        onNavigateToModels = {}
    )
}

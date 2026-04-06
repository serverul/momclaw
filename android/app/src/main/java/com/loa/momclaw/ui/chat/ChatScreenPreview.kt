package com.loa.momclaw.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.ui.theme.MOMCLAWTheme

/**
 * Preview parameter provider for ChatUiState
 */
class ChatUiStateProvider : PreviewParameterProvider<ChatUiState> {
    override val values = sequenceOf(
        // Empty state
        ChatUiState(
            messages = emptyList(),
            inputText = "",
            isLoading = false,
            isStreaming = false,
            isAgentAvailable = true
        ),
        // With messages
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = "1",
                    content = "Salut! Cu ce te pot ajuta?",
                    isUser = false
                ),
                ChatMessage(
                    id = "2",
                    content = "Vreau să aflu despre MomClAW",
                    isUser = true
                ),
                ChatMessage(
                    id = "3",
                    content = "MomClAW este un asistent AI local pentru Android, care rulează modele de limbaj direct pe dispozitiv, fără conexiune la internet.",
                    isUser = false
                )
            ),
            inputText = "",
            isLoading = false,
            isStreaming = false,
            isAgentAvailable = true
        ),
        // Streaming state
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = "1",
                    content = "Spune-mi o poveste",
                    isUser = true
                )
            ),
            currentStreamingMessage = ChatMessage(
                id = "2",
                content = "A fost odată ca niciodată, într-un tărâm îndepărtat...",
                isUser = false,
                isStreaming = true
            ),
            inputText = "",
            isLoading = false,
            isStreaming = true,
            isAgentAvailable = true
        ),
        // Error state
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = "1",
                    content = "Test",
                    isUser = true
                )
            ),
            inputText = "",
            isLoading = false,
            isStreaming = false,
            error = "Nu s-a putut conecta la agent",
            isAgentAvailable = false
        ),
        // Loading state
        ChatUiState(
            messages = emptyList(),
            inputText = "",
            isLoading = true,
            isStreaming = false,
            isAgentAvailable = true
        )
    )
}

/**
 * Preview for ChatScreen - Empty state
 */
@Preview(name = "Chat - Empty", showBackground = true)
@Composable
fun ChatScreenEmptyPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = emptyList(),
                isAgentAvailable = true
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {}
        )
    }
}

/**
 * Preview for ChatScreen - With messages
 */
@Preview(name = "Chat - Messages", showBackground = true)
@Composable
fun ChatScreenMessagesPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Salut! Cu ce te pot ajuta?", false),
                    ChatMessage("2", "Vreau să aflu despre MomClAW", true),
                    ChatMessage("3", "MomClAW este un asistent AI local care rulează direct pe dispozitivul tău Android, fără a necesita o conexiune la internet.", false)
                ),
                isAgentAvailable = true
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {}
        )
    }
}

/**
 * Preview for ChatScreen - Streaming
 */
@Preview(name = "Chat - Streaming", showBackground = true)
@Composable
fun ChatScreenStreamingPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Spune-mi o poveste", true)
                ),
                currentStreamingMessage = ChatMessage(
                    "2",
                    "A fost odată ca niciodată, într-un tărâm îndepărtat, un regat plin de magie...",
                    false,
                    isStreaming = true
                ),
                isStreaming = true,
                isAgentAvailable = true
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {}
        )
    }
}

/**
 * Preview for ChatScreen - Error
 */
@Preview(name = "Chat - Error", showBackground = true)
@Composable
fun ChatScreenErrorPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Test", true)
                ),
                error = "Eroare de conexiune la agent",
                isAgentAvailable = false
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {}
        )
    }
}

/**
 * Preview for ChatScreen - Dark Theme
 */
@Preview(name = "Chat - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatScreenDarkPreview() {
    MOMCLAWTheme(darkTheme = true) {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Salut!", false),
                    ChatMessage("2", "Noroc!", true)
                ),
                isAgentAvailable = true
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {}
        )
    }
}

/**
 * Preview for ChatScreen - Tablet
 */
@Preview(name = "Chat - Tablet", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun ChatScreenTabletPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Salut! Cu ce te pot ajuta?", false),
                    ChatMessage("2", "Vreau să aflu despre MomClAW", true),
                    ChatMessage("3", "MomClAW rulează modele AI local pe Android.", false)
                ),
                isAgentAvailable = true
            ),
            onSendMessage = {},
            onUpdateInput = {},
            onClearConversation = {},
            onNewConversation = {},
            onRetry = {},
            onCancelStreaming = {},
            useNavigationRail = true
        )
    }
}

/**
 * Preview for individual message bubbles
 */
@Preview(name = "Message Bubbles", showBackground = true)
@Composable
fun MessageBubblesPreview() {
    MOMCLAWTheme {
        Column {
            // User message
            MessageBubble(
                message = ChatMessage("1", "Acesta este un mesaj de la utilizator", true),
                maxWidth = 280.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Assistant message
            MessageBubble(
                message = ChatMessage("2", "Acesta este un răspuns de la asistentul AI.", false),
                maxWidth = 280.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Streaming message
            MessageBubble(
                message = ChatMessage("3", "Acest mesaj este în curs de streaming...", false, isStreaming = true),
                isStreaming = true,
                maxWidth = 280.dp
            )
        }
    }
}

// Required imports for Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

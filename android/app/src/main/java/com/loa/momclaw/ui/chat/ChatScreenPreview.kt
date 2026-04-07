package com.loa.momclaw.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.ui.theme.MOMCLAWTheme

/**
 * Compose Previews for ChatScreen
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

@Preview(name = "Chat - Messages", showBackground = true)
@Composable
fun ChatScreenMessagesPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Salut! Cu ce te pot ajuta?", false),
                    ChatMessage("2", "Vreau să aflu despre MOMCLAW", true),
                    ChatMessage("3", "MOMCLAW este un asistent AI local care rulează direct pe dispozitivul tău Android.", false)
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

@Preview(name = "Chat - Streaming", showBackground = true)
@Composable
fun ChatScreenStreamingPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(ChatMessage("1", "Spune-mi o poveste", true)),
                currentStreamingMessage = ChatMessage(
                    id = "2",
                    content = "A fost odată ca niciodată, într-un tărâm îndepărtat, un regat plin de magie...",
                    isUser = false,
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

@Preview(name = "Chat - Error", showBackground = true)
@Composable
fun ChatScreenErrorPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(ChatMessage("1", "Test", true)),
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

@Preview(name = "Chat - Tablet", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun ChatScreenTabletPreview() {
    MOMCLAWTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage("1", "Salut!", false),
                    ChatMessage("2", "Vreau info despre MOMCLAW", true),
                    ChatMessage("3", "MOMCLAW rulează AI local pe Android.", false)
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

@Preview(name = "Message Bubbles", showBackground = true)
@Composable
fun MessageBubblesPreview() {
    MOMCLAWTheme {
        Column {
            MessageBubble(
                message = ChatMessage("1", "Mesaj de la utilizator", true),
                maxWidth = 280.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            MessageBubble(
                message = ChatMessage("2", "Răspuns de la asistent.", false),
                maxWidth = 280.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            MessageBubble(
                message = ChatMessage("3", "", false, isStreaming = true),
                isStreaming = true,
                maxWidth = 280.dp
            )
        }
    }
}

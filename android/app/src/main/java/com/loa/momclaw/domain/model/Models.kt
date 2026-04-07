package com.loa.momclaw.domain.model

/**
 * Domain model representing a chat message.
 */
data class Message(
    val id: Long = 0,
    val conversationId: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
    }
}

/**
 * Domain model representing a conversation.
 */
data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

/**
 * Domain model representing an AI model.
 */
data class Model(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val downloaded: Boolean = false,
    val loaded: Boolean = false,
    val downloadUrl: String? = null,
    val localPath: String? = null
)

/**
 * Domain model for agent settings.
 */
data class AgentSettings(
    val systemPrompt: String = "You are MOMCLAW, a helpful AI assistant running offline on this device.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val darkMode: Boolean = false,
    val autoSave: Boolean = true
)

/**
 * Domain model for chat state.
 */
data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val currentResponse: String = "",
    val error: String? = null,
    val conversationId: Long = 0
)

/**
 * Sealed class representing chat events.
 */
sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class InputChanged(val text: String) : ChatEvent()
    object ClearConversation : ChatEvent()
    object LoadConversation : ChatEvent()
}

/**
 * Sealed class representing UI state.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

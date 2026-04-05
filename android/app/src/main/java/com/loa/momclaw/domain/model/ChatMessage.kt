package com.loa.momclaw.domain.model

/**
 * Domain model for a chat message
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isComplete: Boolean = true,
    val tokensUsed: Int = 0,
    val model: String? = null
) {
    /**
     * Create a copy with appended content (for streaming)
     */
    fun appendContent(token: String): ChatMessage {
        return copy(
            content = content + token,
            isStreaming = true,
            isComplete = false
        )
    }

    /**
     * Mark message as complete
     */
    fun complete(): ChatMessage {
        return copy(
            isStreaming = false,
            isComplete = true
        )
    }
}

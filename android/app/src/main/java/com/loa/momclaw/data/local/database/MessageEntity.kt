package com.loa.momclaw.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loa.momclaw.domain.model.ChatMessage

/**
 * Room entity for storing chat messages
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isStreaming: Boolean = false,
    val isComplete: Boolean = true,
    val tokensUsed: Int = 0,
    val model: String? = null
) {
    /**
     * Convert to domain model
     */
    fun toDomainModel(): ChatMessage {
        return ChatMessage(
            id = id,
            content = content,
            isUser = isUser,
            timestamp = timestamp,
            isStreaming = isStreaming,
            isComplete = isComplete,
            tokensUsed = tokensUsed,
            model = model
        )
    }

    companion object {
        /**
         * Convert from domain model
         */
        fun fromDomainModel(message: ChatMessage, conversationId: String): MessageEntity {
            return MessageEntity(
                id = message.id,
                conversationId = conversationId,
                content = message.content,
                isUser = message.isUser,
                timestamp = message.timestamp,
                isStreaming = message.isStreaming,
                isComplete = message.isComplete,
                tokensUsed = message.tokensUsed,
                model = message.model
            )
        }
    }
}

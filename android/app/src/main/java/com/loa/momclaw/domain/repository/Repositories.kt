package com.loa.momclaw.domain.repository

import com.loa.momclaw.domain.model.Message
import com.loa.momclaw.domain.model.Conversation
import com.loa.momclaw.domain.model.AgentSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations.
 */
interface ChatRepository {
    /**
     * Gets all messages for a conversation.
     */
    fun getConversationMessages(conversationId: Long): Flow<List<Message>>

    /**
     * Gets the current active conversation messages.
     */
    fun getCurrentConversation(): Flow<List<Message>>

    /**
     * Saves a message to the database.
     */
    suspend fun saveMessage(message: Message): Result<Long>

    /**
     * Clears all messages in a conversation.
     */
    suspend fun clearConversation(conversationId: Long): Result<Unit>

    /**
     * Clears the current conversation.
     */
    suspend fun clearCurrentConversation(): Result<Unit>

    /**
     * Gets all conversations.
     */
    fun getAllConversations(): Flow<List<Conversation>>

    /**
     * Creates a new conversation.
     */
    suspend fun createConversation(title: String): Result<Long>

    /**
     * Deletes a conversation.
     */
    suspend fun deleteConversation(conversationId: Long): Result<Unit>

    /**
     * Gets the current active conversation ID.
     */
    suspend fun getCurrentConversationId(): Long

    /**
     * Sets the current active conversation.
     */
    suspend fun setCurrentConversation(conversationId: Long)
}

/**
 * Repository interface for settings operations.
 */
interface SettingsRepository {
    /**
     * Gets the current agent settings.
     */
    fun getSettings(): Flow<AgentSettings>

    /**
     * Updates agent settings.
     */
    suspend fun updateSettings(settings: AgentSettings): Result<Unit>

    /**
     * Resets settings to defaults.
     */
    suspend fun resetSettings(): Result<Unit>
}

/**
 * Repository interface for model operations.
 */
interface ModelRepository {
    /**
     * Gets available models.
     */
    suspend fun getAvailableModels(): Result<List<com.loa.momclaw.domain.model.Model>>

    /**
     * Downloads a model.
     */
    suspend fun downloadModel(modelId: String): Result<Unit>

    /**
     * Loads a model into memory.
     */
    suspend fun loadModel(modelId: String): Result<Unit>

    /**
     * Deletes a downloaded model.
     */
    suspend fun deleteModel(modelId: String): Result<Unit>

    /**
     * Gets the currently loaded model.
     */
    suspend fun getCurrentModel(): String?
}

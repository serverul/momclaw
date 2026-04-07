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
    suspend fun getAvailableModels(): Result<List<com.loa.momclaw.domain.model.Model>>
    suspend fun downloadModel(modelId: String): Result<Unit>
    suspend fun loadModel(modelId: String): Result<Unit>
    suspend fun deleteModel(modelId: String): Result<Unit>
    suspend fun getCurrentModel(): String?
    fun getDownloadProgress(modelId: String): kotlinx.coroutines.flow.Flow<com.loa.momclaw.data.download.ModelDownloadManager.DownloadProgress>
    fun getAllDownloadProgress(): kotlinx.coroutines.flow.StateFlow<Map<String, com.loa.momclaw.data.download.ModelDownloadManager.DownloadProgress>>
    suspend fun getStorageInfo(): com.loa.momclaw.data.repository.StorageInfo
    suspend fun cancelDownload(modelId: String)
}

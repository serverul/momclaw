package com.loa.momclaw.domain.repository

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Repository for managing chat data and agent communication
 * 
 * Performance optimized:
 * - Batch database updates during streaming (not per-token)
 * - Atomic state management with ReentrantLock
 * - Proper resource cleanup
 */
class ChatRepository(
    private val messageDao: MessageDao,
    private val agentClient: AgentClient,
    private val settingsPreferences: SettingsPreferences
) {
    // Current conversation ID with thread-safe access
    private var currentConversationId: String = UUID.randomUUID().toString()
    private val conversationLock = ReentrantLock()
    
    // Streaming optimization: batch update settings
    companion object {
        private const val BATCH_UPDATE_INTERVAL_MS = 500L  // Update DB every 500ms max
        private const val BATCH_UPDATE_TOKEN_COUNT = 10    // Or every 10 tokens
    }

    /**
     * Get all messages for the current conversation
     */
    fun getMessages(): Flow<List<ChatMessage>> {
        val convId = conversationLock.withLock { currentConversationId }
        return messageDao.getMessagesForConversation(convId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /**
     * Get all conversation IDs
     */
    fun getAllConversations(): Flow<List<String>> {
        return messageDao.getAllConversations()
    }

    /**
     * Get current configuration
     */
    fun getConfig(): Flow<AgentConfig> {
        return settingsPreferences.agentConfig
    }

    /**
     * Send a message to the agent and save to database
     */
    suspend fun sendMessage(content: String): Result<ChatMessage> {
        val convId = conversationLock.withLock { currentConversationId }
        
        // Create user message
        val userMessage = ChatMessage(
            content = content,
            isUser = true
        )
        
        // Save user message
        messageDao.insertMessage(
            MessageEntity.fromDomainModel(userMessage, convId)
        )

        // Get conversation history for context
        val history = getMessageHistory()

        // Send to agent
        val result = agentClient.sendMessage(content, history)

        return result.map { responseContent ->
            // Create assistant message
            val assistantMessage = ChatMessage(
                content = responseContent,
                isUser = false
            )
            
            // Save assistant message
            messageDao.insertMessage(
                MessageEntity.fromDomainModel(assistantMessage, convId)
            )
            
            assistantMessage
        }
    }

    /**
     * Send a message with streaming response
     * 
     * PERFORMANCE FIX: Database is updated only at:
     * 1. Start of stream (placeholder message)
     * 2. Every batch interval OR token count (whichever comes first)
     * 3. End of stream (final message)
     * 
     * NOT on every single token anymore.
     */
    fun sendMessageStream(content: String): Flow<StreamState> = flow {
        var assistantMessage: ChatMessage? = null
        var messageId: String? = null
        var tokenCount = 0
        var lastUpdateTime = 0L
        val streamingContent = StringBuilder()
        
        val convId = conversationLock.withLock { currentConversationId }
        
        try {
            // Create and save user message
            val userMessage = ChatMessage(
                content = content,
                isUser = true
            )
            messageDao.insertMessage(
                MessageEntity.fromDomainModel(userMessage, convId)
            )
            emit(StreamState.UserMessageSaved(userMessage))

            // Create placeholder assistant message
            assistantMessage = ChatMessage(
                content = "",
                isUser = false,
                isStreaming = true,
                isComplete = false
            )
            val entity = MessageEntity.fromDomainModel(assistantMessage, convId)
            messageDao.insertMessage(entity)
            messageId = entity.id
            emit(StreamState.StreamingStarted(assistantMessage))

            // Get conversation history
            val history = getMessageHistory()

            // Stream tokens with batched DB updates
            agentClient.sendMessageStream(content, history).collect { token ->
                streamingContent.append(token)
                tokenCount++
                
                val updatedMessage = assistantMessage.copy(
                    content = streamingContent.toString()
                )
                
                // Emit token to UI immediately (no DB wait)
                emit(StreamState.TokenReceived(updatedMessage, token))
                
                // Batch DB update - only on interval OR token count threshold
                val now = System.currentTimeMillis()
                val shouldUpdateDb = (now - lastUpdateTime >= BATCH_UPDATE_INTERVAL_MS) ||
                                     (tokenCount % BATCH_UPDATE_TOKEN_COUNT == 0)
                
                if (shouldUpdateDb) {
                    messageDao.updateMessage(
                        MessageEntity.fromDomainModel(updatedMessage, convId).copy(id = messageId)
                    )
                    lastUpdateTime = now
                }
            }

            // Final update - mark complete
            val finalMessage = assistantMessage.copy(
                content = streamingContent.toString(),
                isStreaming = false,
                isComplete = true
            )
            messageDao.updateMessage(
                MessageEntity.fromDomainModel(finalMessage, convId).copy(id = messageId)
            )
            emit(StreamState.StreamingComplete(finalMessage))
            
        } catch (e: Exception) {
            // Handle error - update message if exists
            assistantMessage?.let { msg ->
                val errorMessage = msg.copy(
                    content = streamingContent.toString().ifEmpty { "Error: ${e.message}" },
                    isStreaming = false,
                    isComplete = true
                )
                messageId?.let { id ->
                    messageDao.updateMessage(
                        MessageEntity.fromDomainModel(errorMessage, convId).copy(id = id)
                    )
                }
            }
            emit(StreamState.Error(e))
        }
    }

    /**
     * Get message history for context
     */
    private suspend fun getMessageHistory(): List<ChatMessage> {
        val convId = conversationLock.withLock { currentConversationId }
        return messageDao.getMessagesPaginated(convId, limit = 20, offset = 0)
            .map { it.toDomainModel() }
    }

    /**
     * Clear current conversation
     */
    suspend fun clearConversation() {
        val convId = conversationLock.withLock { currentConversationId }
        messageDao.deleteConversation(convId)
    }

    /**
     * Start a new conversation
     */
    suspend fun startNewConversation(): String {
        return conversationLock.withLock {
            currentConversationId = UUID.randomUUID().toString()
            settingsPreferences.setLastConversationId(currentConversationId)
            currentConversationId
        }
    }

    /**
     * Switch to an existing conversation
     */
    suspend fun switchToConversation(conversationId: String) {
        conversationLock.withLock {
            currentConversationId = conversationId
            settingsPreferences.setLastConversationId(conversationId)
        }
    }

    /**
     * Delete a specific conversation
     */
    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversation(conversationId)
        conversationLock.withLock {
            if (conversationId == currentConversationId) {
                currentConversationId = UUID.randomUUID().toString()
            }
        }
    }

    /**
     * Clear all messages
     */
    suspend fun clearAllMessages() {
        messageDao.deleteAllMessages()
        conversationLock.withLock {
            currentConversationId = UUID.randomUUID().toString()
        }
    }

    /**
     * Check if agent is available
     */
    suspend fun isAgentAvailable(): Boolean {
        return agentClient.isAvailable()
    }

    /**
     * Get available models
     */
    suspend fun getAvailableModels() = agentClient.getAvailableModels()

    /**
     * Load a model
     */
    suspend fun loadModel(modelId: String) = agentClient.loadModel(modelId)

    /**
     * Get current conversation ID
     */
    fun getCurrentConversationId(): String = conversationLock.withLock { currentConversationId }
}

/**
 * States for streaming message responses
 */
sealed class StreamState {
    data class UserMessageSaved(val message: ChatMessage) : StreamState()
    data class StreamingStarted(val message: ChatMessage) : StreamState()
    data class TokenReceived(val message: ChatMessage, val token: String) : StreamState()
    data class StreamingComplete(val message: ChatMessage) : StreamState()
    data class Error(val exception: Throwable) : StreamState()
}

package com.loa.momclaw.domain.repository

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for managing chat data and agent communication
 */
class ChatRepository(
    private val messageDao: MessageDao,
    private val agentClient: AgentClient,
    private val settingsPreferences: SettingsPreferences
) {
    // Current conversation ID
    private var currentConversationId: String = UUID.randomUUID().toString()

    /**
     * Get all messages for the current conversation
     */
    fun getMessages(): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(currentConversationId)
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
        // Create user message
        val userMessage = ChatMessage(
            content = content,
            isUser = true
        )
        
        // Save user message
        messageDao.insertMessage(
            MessageEntity.fromDomainModel(userMessage, currentConversationId)
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
                MessageEntity.fromDomainModel(assistantMessage, currentConversationId)
            )
            
            assistantMessage
        }
    }

    /**
     * Send a message with streaming response
     * Improved error handling: emits StreamState.Error on failures
     */
    fun sendMessageStream(content: String): Flow<StreamState> {
        return kotlinx.coroutines.flow.flow {
            var assistantMessage: ChatMessage? = null
            
            try {
                // Create and save user message
                val userMessage = ChatMessage(
                    content = content,
                    isUser = true
                )
                messageDao.insertMessage(
                    MessageEntity.fromDomainModel(userMessage, currentConversationId)
                )
                emit(StreamState.UserMessageSaved(userMessage))

                // Create placeholder assistant message
                assistantMessage = ChatMessage(
                    content = "",
                    isUser = false,
                    isStreaming = true,
                    isComplete = false
                )
                messageDao.insertMessage(
                    MessageEntity.fromDomainModel(assistantMessage, currentConversationId)
                )
                emit(StreamState.StreamingStarted(assistantMessage))

                // Get conversation history
                val history = getMessageHistory()

                // Stream tokens
                val streamingMessage = StringBuilder()
                agentClient.sendMessageStream(content, history).collect { token ->
                    streamingMessage.append(token)
                    val updatedMessage = assistantMessage.copy(
                        content = streamingMessage.toString()
                    )
                    messageDao.updateMessage(
                        MessageEntity.fromDomainModel(updatedMessage, currentConversationId)
                    )
                    emit(StreamState.TokenReceived(updatedMessage, token))
                }

                // Mark complete
                val finalMessage = assistantMessage.copy(
                    content = streamingMessage.toString(),
                    isStreaming = false,
                    isComplete = true
                )
                messageDao.updateMessage(
                    MessageEntity.fromDomainModel(finalMessage, currentConversationId)
                )
                emit(StreamState.StreamingComplete(finalMessage))
                
            } catch (e: Exception) {
                // Handle error - update message if exists
                assistantMessage?.let { msg ->
                    val errorMessage = msg.copy(
                        content = "Error: ${e.message}",
                        isStreaming = false,
                        isComplete = true
                    )
                    messageDao.updateMessage(
                        MessageEntity.fromDomainModel(errorMessage, currentConversationId)
                    )
                }
                emit(StreamState.Error(e))
            }
        }
    }

    /**
     * Get message history for context
     */
    private suspend fun getMessageHistory(): List<ChatMessage> {
        return messageDao.getMessagesPaginated(currentConversationId, limit = 20, offset = 0)
            .map { it.toDomainModel() }
    }

    /**
     * Clear current conversation
     */
    suspend fun clearConversation() {
        messageDao.deleteConversation(currentConversationId)
    }

    /**
     * Start a new conversation
     */
    suspend fun startNewConversation(): String {
        currentConversationId = UUID.randomUUID().toString()
        settingsPreferences.setLastConversationId(currentConversationId)
        return currentConversationId
    }

    /**
     * Switch to an existing conversation
     */
    suspend fun switchToConversation(conversationId: String) {
        currentConversationId = conversationId
        settingsPreferences.setLastConversationId(conversationId)
    }

    /**
     * Delete a specific conversation
     */
    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversation(conversationId)
        if (conversationId == currentConversationId) {
            currentConversationId = UUID.randomUUID().toString()
        }
    }

    /**
     * Clear all messages
     */
    suspend fun clearAllMessages() {
        messageDao.deleteAllMessages()
        currentConversationId = UUID.randomUUID().toString()
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
    fun getCurrentConversationId(): String = currentConversationId
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

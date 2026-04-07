package com.loa.momclaw.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.data.remote.MessageDto
import com.loa.momclaw.domain.model.*
import com.loa.momclaw.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing chat screen state and business logic.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentClient: AgentClient
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        loadConversation()
    }

    /**
     * Handles chat events.
     */
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> sendMessage(event.text)
            is ChatEvent.InputChanged -> updateInput(event.text)
            is ChatEvent.ClearConversation -> clearConversation()
            is ChatEvent.LoadConversation -> loadConversation()
            is ChatEvent.ClearError -> clearError()
        }
    }

    /**
     * Sends a message to the agent and streams the response.
     */
    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                // Add user message
                val conversationId = _state.value.conversationId
                val userMessage = Message(
                    conversationId = conversationId,
                    role = Message.ROLE_USER,
                    content = text,
                    timestamp = System.currentTimeMillis()
                )
                
                chatRepository.saveMessage(userMessage)
                
                _state.update { it.copy(
                    inputText = "",
                    isStreaming = true,
                    messages = _state.value.messages + userMessage,
                    error = null
                )}

                // Prepare messages for agent
                val agentMessages = _state.value.messages.map { msg ->
                    MessageDto(msg.role, msg.content)
                } + MessageDto(Message.ROLE_USER, text)

                // Stream response from agent
                val responseBuilder = StringBuilder()
                
                agentClient.chat(agentMessages)
                    .catch { e ->
                        Log.e(TAG, "Error streaming response", e)
                        _state.update { it.copy(
                            error = "Error: ${e.message}",
                            isStreaming = false
                        )}
                    }
                    .collect { token ->
                        responseBuilder.append(token)
                        _state.update { it.copy(
                            currentResponse = responseBuilder.toString()
                        )}
                    }

                // Save assistant message
                val assistantMessage = Message(
                    conversationId = conversationId,
                    role = Message.ROLE_ASSISTANT,
                    content = responseBuilder.toString(),
                    timestamp = System.currentTimeMillis()
                )
                
                chatRepository.saveMessage(assistantMessage)
                
                _state.update { it.copy(
                    messages = _state.value.messages + assistantMessage,
                    currentResponse = "",
                    isStreaming = false
                )}
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                _state.update { it.copy(
                    error = "Failed to send message: ${e.message}",
                    isStreaming = false
                )}
            }
        }
    }

    /**
     * Updates the input text.
     */
    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    /**
     * Clears the current conversation.
     */
    private fun clearConversation() {
        viewModelScope.launch {
            try {
                chatRepository.clearCurrentConversation()
                _state.update { ChatState(conversationId = _state.value.conversationId) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear conversation", e)
                _state.update { it.copy(error = "Failed to clear conversation") }
            }
        }
    }

    /**
     * Loads the current conversation from database.
     */
    private fun loadConversation() {
        viewModelScope.launch {
            try {
                // Get or create conversation ID
                val conversationId = chatRepository.getCurrentConversationId()
                
                _state.update { it.copy(conversationId = conversationId) }
                
                // Load messages
                chatRepository.getCurrentConversation()
                    .catch { e ->
                        Log.e(TAG, "Error loading conversation", e)
                        _state.update { it.copy(error = "Failed to load conversation") }
                    }
                    .collect { messages ->
                        _state.update { it.copy(messages = messages) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversation", e)
                _state.update { it.copy(error = "Failed to load conversation") }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

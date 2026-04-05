package com.loa.momclaw.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.domain.repository.StreamState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Chat screen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val currentStreamingMessage: ChatMessage? = null,
    val error: String? = null,
    val isAgentAvailable: Boolean = false,
    val config: AgentConfig? = null
)

/**
 * ViewModel for Chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        observeMessages()
        checkAgentAvailability()
        observeConfig()
    }

    /**
     * Observe messages from repository
     */
    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.getMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    /**
     * Observe configuration changes
     */
    private fun observeConfig() {
        viewModelScope.launch {
            chatRepository.getConfig().collect { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    /**
     * Check if agent is available
     */
    private fun checkAgentAvailability() {
        viewModelScope.launch {
            val isAvailable = chatRepository.isAgentAvailable()
            _uiState.update { it.copy(isAgentAvailable = isAvailable) }
        }
    }

    /**
     * Update input text
     */
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Send a message
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return

        // Clear input
        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        // Cancel any existing streaming job
        streamingJob?.cancel()

        // Start streaming
        streamingJob = viewModelScope.launch {
            chatRepository.sendMessageStream(text).collect { state ->
                when (state) {
                    is StreamState.UserMessageSaved -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            isStreaming = true,
                            currentStreamingMessage = null
                        )}
                    }
                    is StreamState.StreamingStarted -> {
                        _uiState.update { it.copy(
                            currentStreamingMessage = state.message
                        )}
                    }
                    is StreamState.TokenReceived -> {
                        _uiState.update { it.copy(
                            currentStreamingMessage = state.message
                        )}
                    }
                    is StreamState.StreamingComplete -> {
                        _uiState.update { it.copy(
                            isStreaming = false,
                            isLoading = false,
                            currentStreamingMessage = null
                        )}
                    }
                    is StreamState.Error -> {
                        _uiState.update { it.copy(
                            error = state.exception.message,
                            isStreaming = false,
                            isLoading = false,
                            currentStreamingMessage = null
                        )}
                    }
                }
            }
        }
    }

    /**
     * Clear current conversation
     */
    fun clearConversation() {
        viewModelScope.launch {
            chatRepository.clearConversation()
            _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Start a new conversation
     */
    fun startNewConversation() {
        viewModelScope.launch {
            chatRepository.startNewConversation()
            _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Retry last failed operation
     */
    fun retry() {
        _uiState.update { it.copy(error = null) }
        checkAgentAvailability()
    }

    /**
     * Cancel current streaming
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update { it.copy(
            isStreaming = false,
            isLoading = false,
            currentStreamingMessage = null
        )}
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}

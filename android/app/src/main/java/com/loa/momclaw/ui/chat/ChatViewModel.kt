package com.loa.momclaw.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.domain.repository.StreamState
import com.loa.momclaw.util.StreamBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val config: AgentConfig? = null,
    // Performance tracking
    val tokenCount: Int = 0,
    val lastUpdateTime: Long = 0L
)

/**
 * ViewModel for Chat screen with optimized streaming
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var streamBuffer: StreamBuffer? = null
    private var currentStreamingId: String? = null

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
     * Send a message with optimized streaming
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return

        // Clear input
        _uiState.update { it.copy(
            inputText = "",
            isLoading = true,
            error = null,
            tokenCount = 0,
            lastUpdateTime = System.currentTimeMillis()
        )}

        // Cancel any existing streaming job
        streamingJob?.cancel()
        streamBuffer?.clear()

        // Create new stream buffer for this conversation
        streamBuffer = StreamBuffer(viewModelScope, batchIntervalMs = 50, minBatchSize = 5)

        // Start streaming
        streamingJob = viewModelScope.launch {
            var tokenCount = 0
            var streamingContent = StringBuilder()
            var lastUpdateTime = System.currentTimeMillis()

            chatRepository.sendMessageStream(text).collect { state ->
                when (state) {
                    is StreamState.UserMessageSaved -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            isStreaming = true,
                            currentStreamingMessage = null
                        )}
                        currentStreamingId = state.message.id
                    }

                    is StreamState.StreamingStarted -> {
                        streamingContent = StringBuilder()
                        _uiState.update { it.copy(
                            currentStreamingMessage = state.message
                        )}
                    }

                    is StreamState.TokenReceived -> {
                        // Use buffer for UI updates
                        tokenCount++
                        streamingContent.append(state.token)

                        // Throttled UI update - only update every 50ms or every 5 tokens
                        val now = System.currentTimeMillis()
                        val shouldUpdate = (now - lastUpdateTime) >= 50 || tokenCount % 5 == 0

                        if (shouldUpdate) {
                            _uiState.update { it.copy(
                                currentStreamingMessage = state.message,
                                tokenCount = tokenCount,
                                lastUpdateTime = now
                            )}
                            lastUpdateTime = now
                        }
                    }

                    is StreamState.StreamingComplete -> {
                        // Final update with complete message
                        _uiState.update { it.copy(
                            isStreaming = false,
                            isLoading = false,
                            currentStreamingMessage = null,
                            tokenCount = tokenCount,
                            lastUpdateTime = System.currentTimeMillis()
                        )}
                        streamingContent.clear()
                    }

                    is StreamState.Error -> {
                        _uiState.update { it.copy(
                            error = state.exception.message ?: "Unknown error occurred",
                            isStreaming = false,
                            isLoading = false,
                            currentStreamingMessage = null
                        )}
                        streamingContent.clear()
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
        streamBuffer?.clear()
        streamBuffer = null
        currentStreamingId = null
        _uiState.update { it.copy(
            isStreaming = false,
            isLoading = false,
            currentStreamingMessage = null
        )}
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
        streamBuffer?.clear()
    }
}

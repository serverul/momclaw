package com.loa.momclaw.ui.chat

import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.domain.repository.StreamState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ChatViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @Mock
    private lateinit var mockRepository: ChatRepository

    private lateinit var viewModel: ChatViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behavior
        whenever(mockRepository.getMessages()).thenReturn(flowOf(emptyList()))
        whenever(mockRepository.getConfig()).thenReturn(flowOf(AgentConfig.DEFAULT))
        whenever(mockRepository.isAgentAvailable()).thenReturn(true)
        
        viewModel = ChatViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertNull(state.error)
        assertTrue(state.isAgentAvailable)
    }

    @Test
    fun testUpdateInputText() {
        viewModel.updateInputText("Hello")
        assertEquals("Hello", viewModel.uiState.value.inputText)
    }

    @Test
    fun testSendMessageClearsInput() = runTest {
        val message = "Test message"
        whenever(mockRepository.sendMessageStream(any())).thenReturn(flowOf(StreamState.StreamingComplete(
            ChatMessage(content = "Response", isUser = false)
        )))
        
        viewModel.updateInputText(message)
        viewModel.sendMessage()
        
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun testSendMessageWithEmptyTextDoesNothing() = runTest {
        viewModel.updateInputText("")
        viewModel.sendMessage()
        
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testClearConversationClearsError() = runTest {
        viewModel.clearConversation()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testStartNewConversationClearsError() = runTest {
        viewModel.startNewConversation()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testRetryClearsErrorAndChecksAvailability() = runTest {
        viewModel.retry()
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isAgentAvailable)
    }

    @Test
    fun testCancelStreamingResetsState() = runTest {
        viewModel.updateInputText("Test")
        viewModel.sendMessage()
        viewModel.cancelStreaming()
        
        assertFalse(viewModel.uiState.value.isStreaming)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.currentStreamingMessage)
    }

    @Test
    fun testAgentUnavailableShowsInState() = runTest {
        whenever(mockRepository.isAgentAvailable()).thenReturn(false)
        
        val newViewModel = ChatViewModel(mockRepository)
        
        assertFalse(newViewModel.uiState.value.isAgentAvailable)
    }

    @Test
    fun testConfigUpdatesAreReflected() = runTest {
        val customConfig = AgentConfig(
            systemPrompt = "Custom prompt",
            temperature = 0.5f,
            maxTokens = 1024
        )
        whenever(mockRepository.getConfig()).thenReturn(flowOf(customConfig))
        
        val newViewModel = ChatViewModel(mockRepository)
        
        assertEquals(customConfig, newViewModel.uiState.value.config)
    }

    @Test
    fun testErrorHandlingFromRepository() = runTest {
        val errorMessage = "Network error"
        whenever(mockRepository.sendMessageStream(any())).thenReturn(
            flowOf(StreamState.Error(Exception(errorMessage)))
        )
        
        viewModel.updateInputText("Test")
        viewModel.sendMessage()
        
        // Wait for state update
        advanceUntilIdle()
        
        assertEquals(errorMessage, viewModel.uiState.value.error)
    }
}

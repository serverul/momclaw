package com.loa.momclaw.integration

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.domain.repository.StreamState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-End Integration Tests
 * Tests the complete flow from user action to backend response
 * 
 * Covers:
 * - Complete startup sequence validation
 * - Message flow: UI -> Repository -> AgentClient -> Backend
 * - State propagation across all layers
 * - Error handling through entire stack
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndIntegrationTest {

    @Mock
    private lateinit var mockMessageDao: MessageDao

    @Mock
    private lateinit var mockAgentClient: AgentClient

    @Mock
    private lateinit var mockSettingsPreferences: SettingsPreferences

    private lateinit var chatRepository: ChatRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup default responses
        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(emptyList()))
        whenever(mockMessageDao.getAllConversations()).thenReturn(flowOf(emptyList()))
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(AgentConfig.DEFAULT))
        whenever(mockSettingsPreferences.lastConversationId).thenReturn(flowOf(null))
        
        chatRepository = ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
    }

    /**
     * Test 1: Complete message flow - user sends message, repository processes, agent responds
     */
    @Test
    fun testCompleteMessageFlowSuccess() = runTest {
        // Setup: Track all saved messages
        val savedMessages = mutableListOf<MessageEntity>()
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            savedMessages.add(invocation.getArgument(0))
            Unit
        }
        
        // Setup: Agent returns successful response
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("AI response to your message")
        )

        // Execute: Send message through repository
        val result = chatRepository.sendMessage("Hello, AI!")

        // Verify: Message flow completed successfully
        assertTrue(result.isSuccess)
        val responseMessage = result.getOrThrow()
        assertEquals("AI response to your message", responseMessage.content)
        assertTrue(!responseMessage.isUser)

        // Verify: Both user and assistant messages were saved
        assertEquals(2, savedMessages.size)
        assertTrue(savedMessages[0].isUser)
        assertEquals("Hello, AI!", savedMessages[0].content)
        assertTrue(!savedMessages[1].isUser)
        assertEquals("AI response to your message", savedMessages[1].content)
    }

    /**
     * Test 2: Complete streaming flow with token-by-token updates
     */
    @Test
    fun testCompleteStreamingFlow() = runTest {
        // Setup: Track message updates
        val messageUpdates = mutableListOf<MessageEntity>()
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            messageUpdates.add(invocation.getArgument(0))
            Unit
        }
        whenever(mockMessageDao.updateMessage(any())).thenAnswer { invocation ->
            messageUpdates.add(invocation.getArgument(0))
            Unit
        }

        // Setup: Agent streams tokens
        val tokens = listOf("Hello", " there", " user")
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        // Execute: Send streaming message
        val states = chatRepository.sendMessageStream("Test message").toList()

        // Verify: All streaming states received
        assertTrue(states.any { it is StreamState.UserMessageSaved })
        assertTrue(states.any { it is StreamState.StreamingStarted })
        assertTrue(states.any { it is StreamState.TokenReceived })
        assertTrue(states.any { it is StreamState.StreamingComplete })

        // Verify: Final message contains all tokens
        val completeState = states.filterIsInstance<StreamState.StreamingComplete>().first()
        assertEquals("Hello there user", completeState.message.content)
        assertTrue(completeState.message.isComplete)
    }

    /**
     * Test 3: Error propagation from backend to UI
     */
    @Test
    fun testErrorPropagationThroughStack() = runTest {
        // Setup: Agent fails with specific error
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Network timeout"))
        )

        // Execute: Send message
        val result = chatRepository.sendMessage("Will fail")

        // Verify: Error propagates correctly
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network timeout") == true)
    }

    /**
     * Test 4: Configuration changes propagate through all layers
     */
    @Test
    fun testConfigurationPropagation() = runTest {
        // Setup: Custom configuration
        val customConfig = AgentConfig(
            systemPrompt = "Custom system prompt",
            temperature = 0.5f,
            maxTokens = 1024
        )
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(customConfig))

        // Execute: Get configuration
        val config = chatRepository.getConfig().first()

        // Verify: Configuration propagated correctly
        assertEquals("Custom system prompt", config.systemPrompt)
        assertEquals(0.5f, config.temperature)
        assertEquals(1024, config.maxTokens)
    }

    /**
     * Test 5: Conversation management across sessions
     */
    @Test
    fun testConversationManagementFlow() = runTest {
        // Setup: Track conversation IDs
        val conversationIds = mutableListOf<String>()
        whenever(mockSettingsPreferences.setLastConversationId(any())).thenAnswer { invocation ->
            conversationIds.add(invocation.getArgument(0))
            Unit
        }

        // Execute: Start new conversation
        val convId1 = chatRepository.startNewConversation()
        
        // Execute: Start another conversation
        val convId2 = chatRepository.startNewConversation()

        // Verify: Different conversation IDs generated
        assertTrue(convId1.isNotEmpty())
        assertTrue(convId2.isNotEmpty())
        assertTrue(convId1 != convId2)
        assertEquals(2, conversationIds.size)
    }

    /**
     * Test 6: Message history retrieval for context
     */
    @Test
    fun testMessageHistoryRetrievalForContext() = runTest {
        // Setup: Existing conversation with history
        val historyMessages = listOf(
            MessageEntity("Question 1", true, System.currentTimeMillis(), "conv1"),
            MessageEntity("Answer 1", false, System.currentTimeMillis(), "conv1"),
            MessageEntity("Question 2", true, System.currentTimeMillis(), "conv1")
        )
        whenever(mockMessageDao.getMessagesPaginated(any(), any(), any())).thenReturn(historyMessages)
        
        // Setup: Track what history was passed to agent
        val historyCaptor = argumentCaptor<List<ChatMessage>>()
        whenever(mockAgentClient.sendMessage(any(), historyCaptor.capture())).thenReturn(
            Result.success("Response")
        )
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)

        // Execute: Send new message
        chatRepository.sendMessage("Question 3")

        // Verify: History was passed to agent
        assertTrue(historyCaptor.firstValue.size >= 3)
    }

    /**
     * Test 7: Agent availability check integration
     */
    @Test
    fun testAgentAvailabilityCheckIntegration() = runTest {
        // Setup: Agent available
        whenever(mockAgentClient.isAvailable()).thenReturn(true)

        // Execute: Check availability
        val isAvailable = chatRepository.isAgentAvailable()

        // Verify: Availability reported correctly
        assertTrue(isAvailable)
        
        // Setup: Agent unavailable
        whenever(mockAgentClient.isAvailable()).thenReturn(false)
        
        // Execute: Check again
        val isAvailableNow = chatRepository.isAgentAvailable()
        
        // Verify: Unavailability reported
        assertTrue(!isAvailableNow)
    }

    /**
     * Test 8: Complete conversation switch flow
     */
    @Test
    fun testConversationSwitchFlow() = runTest {
        // Setup: Track conversation switches
        val switchedToIds = mutableListOf<String>()
        whenever(mockSettingsPreferences.setLastConversationId(any())).thenAnswer { invocation ->
            switchedToIds.add(invocation.getArgument(0))
            Unit
        }

        // Execute: Switch to existing conversation
        chatRepository.switchToConversation("existing-conv-id")

        // Verify: Switch completed
        assertEquals(1, switchedToIds.size)
        assertEquals("existing-conv-id", switchedToIds[0])
        assertEquals("existing-conv-id", chatRepository.getCurrentConversationId())
    }

    /**
     * Test 9: Conversation deletion cascade
     */
    @Test
    fun testConversationDeletionCascade() = runTest {
        // Setup: Track deletions
        val deletedIds = mutableListOf<String>()
        whenever(mockMessageDao.deleteConversation(any())).thenAnswer { invocation ->
            deletedIds.add(invocation.getArgument(0))
            Unit
        }

        // Execute: Delete conversation
        chatRepository.deleteConversation("conv-to-delete")

        // Verify: Deletion propagated
        assertEquals(1, deletedIds.size)
        assertEquals("conv-to-delete", deletedIds[0])
    }

    /**
     * Test 10: Clear all messages cascade
     */
    @Test
    fun testClearAllMessagesCascade() = runTest {
        // Setup: Track clear operation
        var clearAllCalled = false
        whenever(mockMessageDao.deleteAllMessages()).thenAnswer {
            clearAllCalled = true
            Unit
        }

        // Execute: Clear all
        chatRepository.clearAllMessages()

        // Verify: Clear propagated
        assertTrue(clearAllCalled)
    }
}

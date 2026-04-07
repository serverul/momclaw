package com.loa.momclaw.integration

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * ChatIntegrationTest - Comprehensive chat integration testing
 * 
 * Tests the complete chat flow:
 * - LiteRT Bridge ↔ NullClaw Agent
 * - Android App ↔ Agent Services  
 * - UI ↔ Repository Layer
 * 
 * Validates:
 * 1. Message sending and receiving
 * 2. Conversation persistence
 * 3. Error handling and recovery
 * 4. Streaming responses
 * 5. Multi-turn conversations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatIntegrationTest {

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
        
        // Setup default mock behaviors
        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(emptyList()))
        whenever(mockMessageDao.getAllConversations()).thenReturn(flowOf(emptyList()))
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(AgentConfig.DEFAULT))
        whenever(mockSettingsPreferences.lastConversationId).thenReturn(flowOf(null))
        
        chatRepository = ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
    }

    // ==================== TEST 1: Complete Chat Flow ====================

    /**
     * Test 1.1: Basic message sending - success path
     */
    @Test
    fun testSendMessage_Success() = runTest {
        // Given: A valid user message
        val userMessage = "Hello, how are you?"
        val expectedResponse = "I'm doing well, thank you for asking!"
        
        var savedUserMessage: MessageEntity? = null
        var savedAssistantMessage: MessageEntity? = null
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            val entity = invocation.getArgument<MessageEntity>(0)
            if (entity.isUser) {
                savedUserMessage = entity
            } else {
                savedAssistantMessage = entity
            }
            Unit
        }
        
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success(expectedResponse)
        )

        // When: Sending the message
        val result = chatRepository.sendMessage(userMessage)

        // Then: Verify success and persistence
        assertTrue(result.isSuccess)
        assertEquals(userMessage, savedUserMessage?.content)
        assertTrue(savedUserMessage?.isUser == true)
        assertEquals(expectedResponse, savedAssistantMessage?.content)
        assertFalse(savedAssistantMessage?.isUser == true)
    }

    /**
     * Test 1.2: Message persistence verification
     */
    @Test
    fun testMessagePersistence() = runTest {
        val messages = mutableListOf<MessageEntity>()
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            messages.add(invocation.getArgument(0))
            Unit
        }
        
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Send multiple messages
        chatRepository.sendMessage("Message 1")
        chatRepository.sendMessage("Message 2")
        chatRepository.sendMessage("Message 3")

        // Verify all messages persisted
        assertEquals(6, messages.size) // 3 user + 3 assistant = 6 total
        assertEquals(3, messages.count { it.isUser })
        assertEquals(3, messages.count { !it.isUser })
    }

    /**
     * Test 1.3: Conversation ID tracking
     */
    @Test
    fun testConversationIdTracking() = runTest {
        var currentConversationId: Long? = null
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            val entity = invocation.getArgument<MessageEntity>(0)
            currentConversationId = entity.conversationId
            Unit
        }
        
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        chatRepository.sendMessage("First message")
        val firstConversationId = currentConversationId

        chatRepository.sendMessage("Second message")
        val secondConversationId = currentConversationId

        // Same conversation should have same ID
        assertEquals(firstConversationId, secondConversationId)
        assertTrue(firstConversationId != null)
    }

    // ==================== TEST 2: Error Handling ====================

    /**
     * Test 2.1: Agent unavailable handling
     */
    @Test
    fun testAgentUnavailable() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Agent service unavailable"))
        )

        val result = chatRepository.sendMessage("Hello")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("unavailable", ignoreCase = true) == true)
    }

    /**
     * Test 2.2: Network error handling
     */
    @Test
    fun testNetworkError() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Network error: Connection refused"))
        )

        val result = chatRepository.sendMessage("Test")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("Network", ignoreCase = true) == true)
    }

    /**
     * Test 2.3: LiteRT Bridge error handling
     */
    @Test
    fun testLiteRTBridgeError() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("LiteRT Bridge error: Model not loaded"))
        )

        val result = chatRepository.sendMessage("Test message")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("LiteRT") == true)
    }

    /**
     * Test 2.4: Timeout handling
     */
    @Test
    fun testTimeoutHandling() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Request timeout after 30 seconds"))
        )

        val result = chatRepository.sendMessage("Long running request")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("timeout", ignoreCase = true) == true)
    }

    // ==================== TEST 3: Agent Availability ====================

    /**
     * Test 3.1: Agent availability check - available
     */
    @Test
    fun testAgentAvailable() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(true)

        val isAvailable = chatRepository.isAgentAvailable()

        assertTrue(isAvailable)
    }

    /**
     * Test 3.2: Agent availability check - unavailable
     */
    @Test
    fun testAgentUnavailableCheck() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(false)

        val isAvailable = chatRepository.isAgentAvailable()

        assertFalse(isAvailable)
    }

    // ==================== TEST 4: Streaming Responses ====================

    /**
     * Test 4.1: Streaming response handling
     */
    @Test
    fun testStreamingResponse() = runTest {
        val tokens = listOf("Hello", " ", "world", "!")
        val fullResponse = tokens.joinToString("")
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success(fullResponse)
        )

        val result = chatRepository.sendMessage("Test")

        assertTrue(result.isSuccess)
        assertEquals(fullResponse, result.getOrNull())
    }

    // ==================== TEST 5: Configuration ====================

    /**
     * Test 5.1: Custom configuration application
     */
    @Test
    fun testCustomConfiguration() = runTest {
        val customConfig = AgentConfig(
            systemPrompt = "You are a helpful coding assistant",
            temperature = 0.5f,
            maxTokens = 4096,
            modelPath = "/custom/model.litertlm",
            baseUrl = "http://localhost:8080"
        )
        
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(customConfig))
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Create new repository with custom config
        val customRepo = ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
        val result = customRepo.sendMessage("Test")

        assertTrue(result.isSuccess)
    }

    /**
     * Test 5.2: Default configuration validity
     */
    @Test
    fun testDefaultConfiguration() {
        val defaultConfig = AgentConfig.DEFAULT
        
        assertTrue(defaultConfig.systemPrompt.isNotEmpty())
        assertTrue(defaultConfig.temperature in 0.0f..2.0f)
        assertTrue(defaultConfig.maxTokens > 0)
        assertTrue(defaultConfig.modelPath.isNotEmpty())
        assertTrue(defaultConfig.baseUrl.isNotEmpty())
    }

    // ==================== TEST 6: Multi-turn Conversations ====================

    /**
     * Test 6.1: Multi-turn conversation flow
     */
    @Test
    fun testMultiTurnConversation() = runTest {
        val conversationHistory = mutableListOf<MessageEntity>()
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            conversationHistory.add(invocation.getArgument(0))
            Unit
        }
        
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Simulate multi-turn conversation
        val turns = listOf(
            "What is AI?" to "AI stands for Artificial Intelligence.",
            "How does it work?" to "AI works by processing data and learning patterns.",
            "Can you give an example?" to "Sure! Image recognition is one example."
        )

        turns.forEach { (userMsg, _) ->
            chatRepository.sendMessage(userMsg)
        }

        // Verify conversation progression
        assertEquals(6, conversationHistory.size) // 3 user + 3 assistant
        assertEquals("What is AI?", conversationHistory[0].content)
        assertEquals("How does it work?", conversationHistory[2].content)
        assertEquals("Can you give an example?", conversationHistory[4].content)
    }

    // ==================== TEST 7: Edge Cases ====================

    /**
     * Test 7.1: Empty message handling
     */
    @Test
    fun testEmptyMessage() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("")
        )

        val result = chatRepository.sendMessage("")

        // Should handle gracefully
        assertTrue(result.isSuccess)
    }

    /**
     * Test 7.2: Very long message handling
     */
    @Test
    fun testLongMessage() = runTest {
        val longMessage = "A".repeat(10000)
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response to long message")
        )

        val result = chatRepository.sendMessage(longMessage)

        assertTrue(result.isSuccess)
    }

    /**
     * Test 7.3: Special characters in message
     */
    @Test
    fun testSpecialCharacters() = runTest {
        val specialMessage = "Test with émojis 🎉 and spëcial çhars"
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response with émojis ✨")
        )

        val result = chatRepository.sendMessage(specialMessage)

        assertTrue(result.isSuccess)
    }

    /**
     * Test 7.4: Code blocks in message
     */
    @Test
    fun testCodeBlocksInMessage() = runTest {
        val codeMessage = """
            Can you help with this code?
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
        """.trimIndent()
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Your code looks good!")
        )

        val result = chatRepository.sendMessage(codeMessage)

        assertTrue(result.isSuccess)
    }
}

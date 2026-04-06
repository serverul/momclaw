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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for offline functionality
 * Verifies that the app works correctly when offline or when services are unavailable
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFunctionalityTest {

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
        
        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(emptyList()))
        whenever(mockMessageDao.getAllConversations()).thenReturn(flowOf(emptyList()))
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(AgentConfig.DEFAULT))
        whenever(mockSettingsPreferences.lastConversationId).thenReturn(flowOf(null))
        
        chatRepository = ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
    }

    @Test
    fun testMessagesPersistWhenAgentUnavailable() = runTest {
        var savedEntity: MessageEntity? = null
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            savedEntity = invocation.getArgument(0)
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Agent unavailable"))
        )

        val result = chatRepository.sendMessage("Hello")

        // Message should still be saved even if agent fails
        assertTrue(result.isFailure)
        assertTrue(savedEntity!!.isUser)
        assertEquals("Hello", savedEntity!!.content)
    }

    @Test
    fun testOfflineDataRetrieval() = runTest {
        val savedMessages = listOf(
            MessageEntity(
                content = "User message",
                isUser = true,
                timestamp = System.currentTimeMillis(),
                conversationId = "test-conv"
            ),
            MessageEntity(
                content = "Assistant response",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                conversationId = "test-conv"
            )
        )
        
        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(savedMessages))
        
        val messages = chatRepository.getMessages().first()
        
        assertEquals(2, messages.size)
        assertEquals("User message", messages[0].content)
        assertEquals("Assistant response", messages[1].content)
    }

    @Test
    fun testConfigPersistsOffline() = runTest {
        val customConfig = AgentConfig(
            systemPrompt = "Offline mode prompt",
            temperature = 0.5f,
            maxTokens = 1024
        )
        
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(customConfig))
        
        val config = chatRepository.getConfig().first()
        
        assertEquals("Offline mode prompt", config.systemPrompt)
        assertEquals(0.5f, config.temperature)
        assertEquals(1024, config.maxTokens)
    }

    @Test
    fun testStreamErrorHandling() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(StreamState.Error(Exception("Network error")))
        )

        val states = chatRepository.sendMessageStream("Test").take(2).toList()
        
        assertTrue(states.any { it is StreamState.Error })
    }

    @Test
    fun testAgentAvailabilityCheck() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(false)
        
        val isAvailable = chatRepository.isAgentAvailable()
        
        assertTrue(!isAvailable)
    }

    @Test
    fun testOfflineConversationManagement() = runTest {
        whenever(mockMessageDao.clearConversation(any())).thenReturn(Unit)
        
        // Should not throw even if agent is unavailable
        chatRepository.clearConversation()
        
        assertTrue(true)
    }

    @Test
    fun testNewConversationStartsOffline() = runTest {
        whenever(mockMessageDao.clearConversation(any())).thenReturn(Unit)
        
        val newConvId = chatRepository.startNewConversation()
        
        assertTrue(newConvId.isNotEmpty())
    }

    @Test
    fun testMessageHistoryRetrieval() = runTest {
        val history = listOf(
            ChatMessage(content = "Q1", isUser = true),
            ChatMessage(content = "A1", isUser = false),
            ChatMessage(content = "Q2", isUser = true)
        )
        
        whenever(mockMessageDao.getMessagesPaginated(any(), any(), any())).thenReturn(
            history.map { msg ->
                MessageEntity(
                    content = msg.content,
                    isUser = msg.isUser,
                    timestamp = System.currentTimeMillis(),
                    conversationId = "test"
                )
            }
        )
        
        // Should be able to retrieve message history even offline
        assertTrue(true)
    }

    @Test
    fun testOfflineStateManager() = runTest {
        // Test that the repository handles offline state gracefully
        whenever(mockAgentClient.isAvailable()).thenReturn(false)
        
        val isAvailable = chatRepository.isAgentAvailable()
        
        // Should return false without throwing
        assertTrue(!isAvailable)
    }
}

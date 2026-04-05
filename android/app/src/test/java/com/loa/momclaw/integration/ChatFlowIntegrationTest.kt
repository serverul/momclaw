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
 * Integration tests for the complete UI -> Repository -> AgentClient flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatFlowIntegrationTest {

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
    fun testRepositorySavesUserMessage() = runTest {
        var savedEntity: MessageEntity? = null
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            savedEntity = invocation.getArgument(0)
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Test response")
        )

        val result = chatRepository.sendMessage("Hello, agent!")

        assertTrue(result.isSuccess)
        assertTrue(savedEntity!!.isUser)
        assertEquals("Hello, agent!", savedEntity!!.content)
    }

    @Test
    fun testRepositoryHandlesAgentError() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.getMessagesPaginated(any(), any(), any())).thenReturn(emptyList())
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Agent unavailable"))
        )

        val result = chatRepository.sendMessage("Hello!")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Agent unavailable") == true)
    }

    @Test
    fun testAgentAvailabilityCheck() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(true)
        val isAvailable = chatRepository.isAgentAvailable()
        assertTrue(isAvailable)
    }
}

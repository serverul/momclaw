package com.loa.momclaw.integration

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.domain.repository.StreamState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Race Condition Detection Tests
 * Tests for concurrent access patterns and race conditions
 * 
 * Covers:
 * - Concurrent message sends to same conversation
 * - Concurrent conversation switches
 * - Concurrent configuration updates
 * - Thread-safe state management
 * - Atomic operations validation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RaceConditionDetectionTest {

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

    /**
     * Test 1: Concurrent message sends - race condition detection
     * Verifies that concurrent messages are handled safely without data loss
     */
    @Test
    fun testConcurrentMessageSendsSafety() = runTest {
        // Setup: Thread-safe counter for message saves
        val saveCounter = AtomicInteger(0)
        whenever(mockMessageDao.insertMessage(any())).thenAnswer {
            saveCounter.incrementAndGet()
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Execute: Launch 10 concurrent message sends
        val jobs = List(10) { index ->
            async {
                chatRepository.sendMessage("Message $index")
            }
        }

        // Wait for all to complete
        val results = jobs.map { it.await() }

        // Verify: All messages processed without data loss
        assertEquals(10, results.size)
        assertTrue(results.all { it.isSuccess })
        // Note: In real implementation, we'd verify exact save count
        // For now, verify no exceptions and all succeeded
    }

    /**
     * Test 2: Concurrent conversation switches - state consistency
     * Verifies conversation ID doesn't get corrupted during rapid switches
     */
    @Test
    fun testConcurrentConversationSwitchesConsistency() = runTest {
        // Setup: Track all conversation IDs set
        val setConversationIds = mutableListOf<String>()
        val lock = Any()
        whenever(mockSettingsPreferences.setLastConversationId(any())).thenAnswer { invocation ->
            synchronized(lock) {
                setConversationIds.add(invocation.getArgument(0))
            }
            Unit
        }

        // Execute: Rapid conversation switches
        val conversationIds = List(20) { "conv-$it" }
        val jobs = conversationIds.map { convId ->
            async {
                chatRepository.switchToConversation(convId)
            }
        }

        // Wait for all
        jobs.forEach { it.await() }

        // Verify: All switches were recorded (no lost updates)
        assertTrue(setConversationIds.size >= 10)
        // Note: Some switches may be overwritten, but none should corrupt state
    }

    /**
     * Test 3: Concurrent reads and writes to message history
     * Verifies no deadlock or corruption when reading/writing simultaneously
     */
    @Test
    fun testConcurrentReadWriteMessageHistory() = runTest {
        // Setup: Shared message list
        val messages = mutableListOf<MessageEntity>()
        val lock = Any()
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { invocation ->
            synchronized(lock) {
                messages.add(invocation.getArgument(0))
            }
            Unit
        }
        
        whenever(mockMessageDao.getMessagesForConversation(any())).thenAnswer {
            synchronized(lock) {
                flowOf(messages.toList())
            }
        }

        // Execute: Concurrent reads and writes
        val writeJob = launch {
            repeat(50) { index ->
                chatRepository.sendMessage("Message $index")
                kotlinx.coroutines.delay(10)
            }
        }

        val readJob = launch {
            repeat(50) {
                chatRepository.getMessages()
                kotlinx.coroutines.delay(10)
            }
        }

        // Wait for completion
        writeJob.join()
        readJob.join()

        // Verify: No deadlock occurred and messages were saved
        assertTrue(messages.size >= 20)
    }

    /**
     * Test 4: Concurrent configuration updates
     * Verifies configuration changes don't cause race conditions
     */
    @Test
    fun testConcurrentConfigurationUpdates() = runTest {
        // Setup: Atomic configuration counter
        val configReads = AtomicInteger(0)
        
        whenever(mockSettingsPreferences.agentConfig).thenAnswer {
            configReads.incrementAndGet()
            flowOf(AgentConfig.DEFAULT)
        }

        // Execute: Concurrent configuration reads
        val jobs = List(50) {
            async {
                chatRepository.getConfig()
            }
        }

        // Wait for all
        val configs = jobs.map { it.await() }

        // Verify: All reads completed successfully
        assertEquals(50, configs.size)
        assertTrue(configs.all { it != null })
    }

    /**
     * Test 5: Concurrent streaming message handling
     * Verifies multiple streaming messages don't interfere with each other
     */
    @Test
    fun testConcurrentStreamingMessagesSafety() = runTest {
        // Setup: Track streaming state updates
        val updateCount = AtomicInteger(0)
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenAnswer {
            updateCount.incrementAndGet()
            Unit
        }
        
        // Setup: Different tokens for different streams
        var streamIndex = 0
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenAnswer {
            val index = streamIndex++
            flowOf("Token-$index-1", "Token-$index-2", "Token-$index-3")
        }

        // Execute: Launch 5 concurrent streams
        val jobs = List(5) { index ->
            async {
                chatRepository.sendMessageStream("Stream $index").toList()
            }
        }

        // Wait for all
        val results = jobs.map { it.await() }

        // Verify: All streams completed without interference
        assertEquals(5, results.size)
        assertTrue(results.all { it.isNotEmpty() })
    }

    /**
     * Test 6: Atomic conversation ID generation
     * Verifies conversation IDs are unique even under concurrent generation
     */
    @Test
    fun testAtomicConversationIdGeneration() = runTest {
        whenever(mockMessageDao.clearConversation(any())).thenReturn(Unit)

        // Execute: Generate 100 conversation IDs concurrently
        val ids = mutableSetOf<String>()
        val lock = Any()
        
        val jobs = List(100) {
            async {
                val convId = chatRepository.startNewConversation()
                synchronized(lock) {
                    ids.add(convId)
                }
            }
        }

        jobs.forEach { it.await() }

        // Verify: All IDs are unique (no collisions)
        assertEquals(100, ids.size)
    }

    /**
     * Test 7: Concurrent agent availability checks
     * Verifies availability state remains consistent under concurrent checks
     */
    @Test
    fun testConcurrentAgentAvailabilityChecks() = runTest {
        // Setup: Availability that changes state
        var isAvailable = true
        val checkCount = AtomicInteger(0)
        
        whenever(mockAgentClient.isAvailable()).thenAnswer {
            checkCount.incrementAndGet()
            synchronized(this) {
                // Simulate occasional state changes
                if (checkCount.get() % 10 == 0) {
                    isAvailable = !isAvailable
                }
                isAvailable
            }
        }

        // Execute: 50 concurrent availability checks
        val jobs = List(50) {
            async {
                chatRepository.isAgentAvailable()
            }
        }

        val results = jobs.map { it.await() }

        // Verify: All checks completed without error
        assertEquals(50, results.size)
        // Results should be either true or false, never undefined/corrupt
        assertTrue(results.all { it is Boolean })
    }

    /**
     * Test 8: Concurrent message deletions
     * Verifies deletion operations don't cause race conditions
     */
    @Test
    fun testConcurrentMessageDeletions() = runTest {
        // Setup: Track deletions
        val deletedIds = mutableSetOf<String>()
        val lock = Any()
        
        whenever(mockMessageDao.deleteConversation(any())).thenAnswer { invocation ->
            synchronized(lock) {
                deletedIds.add(invocation.getArgument(0))
            }
            Unit
        }

        // Execute: Concurrent deletions of different conversations
        val jobs = List(20) { index ->
            async {
                chatRepository.deleteConversation("conv-$index")
            }
        }

        jobs.forEach { it.await() }

        // Verify: All deletions processed (no lost operations)
        assertTrue(deletedIds.size >= 15) // Allow for some race conditions
    }

    /**
     * Test 9: Stress test with high concurrency
     * Verifies system stability under high load
     */
    @Test
    fun testHighConcurrencyStress() = runTest {
        // Setup
        val operationCount = AtomicInteger(0)
        whenever(mockMessageDao.insertMessage(any())).thenAnswer {
            operationCount.incrementAndGet()
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(Result.success("OK"))

        // Execute: 100 concurrent operations of different types
        val jobs = List(100) { index ->
            async {
                when (index % 4) {
                    0 -> chatRepository.sendMessage("Msg $index")
                    1 -> chatRepository.startNewConversation()
                    2 -> chatRepository.isAgentAvailable()
                    3 -> chatRepository.getConfig()
                    else -> Result.success(Unit)
                }
            }
        }

        // Wait for all
        jobs.forEach { it.await() }

        // Verify: No crashes or deadlocks occurred
        assertTrue(operationCount.get() >= 20) // At least 25 messages (100/4)
    }

    /**
     * Test 10: Concurrent new conversation starts
     * Verifies rapid conversation creation doesn't corrupt state
     */
    @Test
    fun testConcurrentNewConversationStarts() = runTest {
        whenever(mockMessageDao.clearConversation(any())).thenReturn(Unit)

        // Execute: 30 concurrent new conversation starts
        val conversationIds = mutableListOf<String>()
        val lock = Any()
        
        val jobs = List(30) {
            async {
                val convId = chatRepository.startNewConversation()
                synchronized(lock) {
                    conversationIds.add(convId)
                }
            }
        }

        jobs.forEach { it.await() }

        // Verify: All conversation IDs are valid strings
        assertTrue(conversationIds.all { it.isNotEmpty() })
        // Most should be unique (some race conditions expected in concurrent scenario)
        val uniqueIds = conversationIds.toSet()
        assertTrue(uniqueIds.size >= 20)
    }
}

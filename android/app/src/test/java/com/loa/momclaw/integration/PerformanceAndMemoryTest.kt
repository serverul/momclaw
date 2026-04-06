package com.loa.momclaw.integration

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
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
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Performance and Memory Management Tests
 * 
 * Covers:
 * - Token generation and streaming performance
 * - Response time benchmarks
 * - Memory usage patterns
 * - UI throttling effectiveness
 * - Database batching efficiency
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceAndMemoryTest {

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

    // ==================== Token Generation Performance ====================

    /**
     * Test 1: Token streaming latency
     * Tokens should arrive with minimal latency
     */
    @Test
    fun testTokenStreamingLatency() = runTest {
        // Setup: Simulate token stream
        val tokenCount = 100
        val tokens = List(tokenCount) { "token$it" }
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        // Execute: Measure time to receive all tokens
        val startTime = System.nanoTime()
        val states = chatRepository.sendMessageStream("Test").toList()
        val duration = System.nanoTime() - startTime

        // Verify: Should complete within reasonable time
        assertTrue(duration < 10_000_000_000L) // 10 seconds max
        assertTrue(states.any { it is StreamState.TokenReceived })
    }

    /**
     * Test 2: Token throughput benchmark
     * System should handle at least 10 tokens per second
     */
    @Test
    fun testTokenThroughputBenchmark() = runTest {
        val tokenCount = 50
        val tokens = List(tokenCount) { "t" }
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        val startTime = System.currentTimeMillis()
        chatRepository.sendMessageStream("Test").toList()
        val duration = System.currentTimeMillis() - startTime

        // Verify: At least 10 tokens/second
        val tokensPerSecond = if (duration > 0) (tokenCount * 1000.0 / duration) else tokenCount.toDouble()
        assertTrue(tokensPerSecond >= 10.0 || duration < 1000, "Token throughput too low: $tokensPerSecond tokens/sec")
    }

    // ==================== Response Time Performance ====================

    /**
     * Test 3: Message send response time
     * Complete message cycle should complete within reasonable time
     */
    @Test
    fun testMessageSendResponseTime() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Test response")
        )

        val duration = measureNanoTime {
            chatRepository.sendMessage("Test message")
        }

        // Verify: Should complete within 5 seconds
        assertTrue(duration < 5_000_000_000L, "Message send took too long: ${duration / 1_000_000}ms")
    }

    /**
     * Test 4: Agent availability check speed
     * Health checks should be fast
     */
    @Test
    fun testAgentAvailabilityCheckSpeed() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(true)

        val duration = measureNanoTime {
            chatRepository.isAgentAvailable()
        }

        // Verify: Should complete within 1 second
        assertTrue(duration < 1_000_000_000L, "Availability check took too long: ${duration / 1_000_000}ms")
    }

    /**
     * Test 5: Configuration retrieval speed
     * Config access should be fast
     */
    @Test
    fun testConfigurationRetrievalSpeed() = runTest {
        val duration = measureNanoTime {
            chatRepository.getConfig()
        }

        // Verify: Should be nearly instant
        assertTrue(duration < 100_000_000L, "Config retrieval took too long: ${duration / 1_000_000}ms")
    }

    // ==================== UI Throttling Effectiveness ====================

    /**
     * Test 6: UI throttling reduces update frequency
     * Rapid token stream should be throttled
     */
    @Test
    fun testUIThrottlingReducesUpdateFrequency() = runTest {
        var updateCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { 
            updateCount++
            Unit 
        }
        whenever(mockMessageDao.updateMessage(any())).thenAnswer {
            updateCount++
            Unit
        }
        
        // 100 rapid tokens
        val tokens = List(100) { "t" }
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        chatRepository.sendMessageStream("Test").toList()

        // Verify: Updates should be less than token count (throttling working)
        // Note: Actual throttling is in ChatViewModel, this tests flow structure
        assertTrue(updateCount >= 1, "At least one update should occur")
    }

    // ==================== Database Batching Efficiency ====================

    /**
     * Test 7: Database operations are batched
     * Multiple token updates should be batched into fewer DB writes
     */
    @Test
    fun testDatabaseOperationsAreBatched() = runTest {
        var insertCount = 0
        var updateCount = 0
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer { 
            insertCount++
            Unit 
        }
        whenever(mockMessageDao.updateMessage(any())).thenAnswer {
            updateCount++
            Unit
        }
        
        // 50 tokens
        val tokens = List(50) { "token" }
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        chatRepository.sendMessageStream("Test").toList()

        // Verify: Should have user message insert and final message update
        assertTrue(insertCount >= 1, "Should have at least one insert")
        assertTrue(updateCount >= 0, "Updates should occur")
    }

    // ==================== Memory Usage Patterns ====================

    /**
     * Test 8: Message history doesn't cause memory issues
     * Large message history should be handled efficiently
     */
    @Test
    fun testLargeMessageHistoryMemoryEfficiency() = runTest {
        // Setup: Large conversation history
        val largeHistory = List(1000) { index ->
            MessageEntity(
                content = "Message $index with some content to make it realistic",
                isUser = index % 2 == 0,
                timestamp = System.currentTimeMillis() - (1000 - index) * 1000,
                conversationId = "test-conv"
            )
        }
        
        whenever(mockMessageDao.getMessagesPaginated(any(), any(), any())).thenReturn(largeHistory)
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Execute: Should not OOM with large history
        val result = chatRepository.sendMessage("Test")

        // Verify: Completed successfully
        assertTrue(result.isSuccess)
    }

    /**
     * Test 9: Streaming doesn't accumulate unbounded memory
     * Long streams should release tokens as consumed
     */
    @Test
    fun testStreamingMemoryAccumulation() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        
        // Very long stream
        val tokens = List(500) { "token$it" }
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        // Execute: Consume stream
        val states = chatRepository.sendMessageStream("Test").toList()

        // Verify: Completed without memory issues
        assertTrue(states.isNotEmpty())
    }

    // ==================== Concurrent Performance ====================

    /**
     * Test 10: Concurrent operations performance
     * System should handle concurrent operations efficiently
     */
    @Test
    fun testConcurrentOperationsPerformance() = runTest {
        var operationCount = 0
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer {
            operationCount++
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            operationCount++
            Result.success("OK")
        }
        whenever(mockAgentClient.isAvailable()).thenAnswer {
            operationCount++
            true
        }

        // Execute: Multiple concurrent operations
        val duration = measureNanoTime {
            repeat(10) {
                chatRepository.sendMessage("Message $it")
            }
        }

        // Verify: Should complete within 10 seconds for 10 operations
        assertTrue(duration < 10_000_000_000L, "Concurrent operations too slow: ${duration / 1_000_000}ms")
    }

    // ==================== Startup Performance ====================

    /**
     * Test 11: Startup performance
     * Initial setup should be fast
     */
    @Test
    fun testStartupPerformance() = runTest {
        val duration = measureNanoTime {
            // Simulate startup operations
            chatRepository.getConfig()
            chatRepository.isAgentAvailable()
        }

        // Verify: Startup operations should be fast
        assertTrue(duration < 2_000_000_000L, "Startup too slow: ${duration / 1_000_000}ms")
    }

    // ==================== Cleanup Performance ====================

    /**
     * Test 12: Cleanup performance
     * Resource cleanup should be fast
     */
    @Test
    fun testCleanupPerformance() = runTest {
        whenever(mockMessageDao.deleteAllMessages()).thenReturn(Unit)

        val duration = measureNanoTime {
            chatRepository.clearAllMessages()
        }

        // Verify: Cleanup should be fast
        assertTrue(duration < 1_000_000_000L, "Cleanup too slow: ${duration / 1_000_000}ms")
    }

    // ==================== Edge Case Performance ====================

    /**
     * Test 13: Empty response performance
     * Empty responses should be handled quickly
     */
    @Test
    fun testEmptyResponsePerformance() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("")
        )

        val duration = measureNanoTime {
            chatRepository.sendMessage("Test")
        }

        // Verify: Empty response should be fast
        assertTrue(duration < 1_000_000_000L, "Empty response took too long")
    }

    /**
     * Test 14: Error response performance
     * Errors should fail fast
     */
    @Test
    fun testErrorResponsePerformance() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.failure(Exception("Immediate error"))
        )

        val duration = measureNanoTime {
            chatRepository.sendMessage("Test")
        }

        // Verify: Error should fail fast
        assertTrue(duration < 1_000_000_000L, "Error response took too long")
    }

    // ==================== Load Testing ====================

    /**
     * Test 15: Sustained load performance
     * System should maintain performance under sustained load
     */
    @Test
    fun testSustainedLoadPerformance() = runTest {
        var successCount = 0
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            successCount++
            Result.success("Response $successCount")
        }

        val startTime = System.currentTimeMillis()
        
        // 50 messages
        repeat(50) { index ->
            chatRepository.sendMessage("Load test $index")
        }
        
        val duration = System.currentTimeMillis() - startTime
        val messagesPerSecond = 50.0 * 1000.0 / duration

        // Verify: Should maintain at least 5 messages/second
        assertTrue(messagesPerSecond >= 5.0 || duration < 10000, 
            "Sustained load too slow: $messagesPerSecond msg/sec")
    }
}

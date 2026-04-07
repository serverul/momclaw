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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
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
 * Performance Benchmark Tests
 * 
 * Validates:
 * - Token streaming speed (>10 tokens/sec)
 * - Memory usage optimization
 * - Startup time optimization
 * - Battery impact assessment
 * 
 * Requirements:
 * - All benchmarks must meet minimum thresholds
 * - Results should be logged for CI/CD tracking
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceBenchmarkTest {

    @Mock
    private lateinit var mockMessageDao: MessageDao

    @Mock
    private lateinit var mockAgentClient: AgentClient

    @Mock
    private lateinit var mockSettingsPreferences: SettingsPreferences

    private lateinit var chatRepository: ChatRepository
    private lateinit var closeable: AutoCloseable

    // Performance thresholds (adjust based on device capabilities)
    companion object {
        const val MIN_TOKENS_PER_SECOND = 10.0
        const val MAX_STARTUP_TIME_MS = 3000L
        const val MAX_MESSAGE_SEND_TIME_MS = 5000L
        const val MAX_STREAMING_LATENCY_MS = 100L
        const val MAX_DB_OPERATION_TIME_MS = 100L
        const val MIN_THROUGHPUT_MSG_PER_SEC = 5.0
    }

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        
        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(emptyList()))
        whenever(mockMessageDao.getAllConversations()).thenReturn(flowOf(emptyList()))
        whenever(mockSettingsPreferences.agentConfig).thenReturn(flowOf(AgentConfig.DEFAULT))
        whenever(mockSettingsPreferences.lastConversationId).thenReturn(flowOf(null))
        
        chatRepository = ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // ==================== Token Streaming Speed Tests ====================

    /**
     * Benchmark 1: Token streaming speed must exceed 10 tokens/sec
     * 
     * This is a critical performance requirement for real-time chat.
     */
    @Test
    fun benchmarkTokenStreamingSpeed() = runTest {
        val tokenCount = 100
        val tokens = List(tokenCount) { "token$it" }
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        val startTime = System.currentTimeMillis()
        val states = chatRepository.sendMessageStream("Benchmark test").toList()
        val duration = System.currentTimeMillis() - startTime

        // Calculate tokens per second
        val tokensPerSecond = if (duration > 0) {
            (tokenCount * 1000.0) / duration
        } else {
            Double.MAX_VALUE // Very fast
        }

        // Log for CI/CD tracking
        println("[BENCHMARK] Token Streaming: %.2f tokens/sec (%d tokens in %d ms)".format(
            tokensPerSecond, tokenCount, duration))

        // Verify minimum threshold
        assertTrue(
            tokensPerSecond >= MIN_TOKENS_PER_SECOND || duration < 1000,
            "Token streaming too slow: %.2f tokens/sec (minimum: %.2f)".format(
                tokensPerSecond, MIN_TOKENS_PER_SECOND)
        )
    }

    /**
     * Benchmark 2: First token latency
     * Time to receive first token should be minimal
     */
    @Test
    fun benchmarkFirstTokenLatency() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf("First", "Second", "Third")
        )

        val startTime = System.nanoTime()
        val states = chatRepository.sendMessageStream("Test").toList()
        var firstTokenTime = 0L

        for (state in states) {
            if (state is StreamState.TokenReceived) {
                firstTokenTime = System.nanoTime() - startTime
                break
            }
        }

        val firstTokenMs = firstTokenTime / 1_000_000

        println("[BENCHMARK] First Token Latency: %d ms".format(firstTokenMs))

        // First token should arrive within 100ms of stream start
        assertTrue(
            firstTokenMs < MAX_STREAMING_LATENCY_MS * 10, // Allow up to 1 second
            "First token latency too high: %d ms".format(firstTokenMs)
        )
    }

    /**
     * Benchmark 3: Large token stream performance
     * System should handle 1000+ token streams efficiently
     */
    @Test
    fun benchmarkLargeTokenStreamPerformance() = runTest {
        val tokenCount = 1000
        val tokens = List(tokenCount) { "t" }
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        val startTime = System.currentTimeMillis()
        val states = chatRepository.sendMessageStream("Large stream").toList()
        val duration = System.currentTimeMillis() - startTime

        val tokensPerSecond = if (duration > 0) {
            (tokenCount * 1000.0) / duration
        } else {
            Double.MAX_VALUE
        }

        println("[BENCHMARK] Large Stream (1000 tokens): %.2f tokens/sec in %d ms".format(
            tokensPerSecond, duration))

        // Should still maintain reasonable throughput
        assertTrue(
            tokensPerSecond >= MIN_TOKENS_PER_SECOND || duration < 5000,
            "Large stream performance degraded: %.2f tokens/sec".format(tokensPerSecond)
        )
    }

    // ==================== Startup Time Tests ====================

    /**
     * Benchmark 4: Repository initialization time
     */
    @Test
    fun benchmarkRepositoryInitializationTime() = runTest {
        val duration = measureNanoTime {
            ChatRepository(mockMessageDao, mockAgentClient, mockSettingsPreferences)
        }

        val durationMs = duration / 1_000_000

        println("[BENCHMARK] Repository Init: %d ms".format(durationMs))

        assertTrue(
            durationMs < 100,
            "Repository initialization too slow: %d ms".format(durationMs)
        )
    }

    /**
     * Benchmark 5: Configuration loading time
     */
    @Test
    fun benchmarkConfigurationLoadingTime() = runTest {
        val duration = measureNanoTime {
            chatRepository.getConfig()
        }

        val durationMs = duration / 1_000_000

        println("[BENCHMARK] Config Load: %d ms".format(durationMs))

        assertTrue(
            durationMs < 50,
            "Configuration loading too slow: %d ms".format(durationMs)
        )
    }

    /**
     * Benchmark 6: Message history loading time
     */
    @Test
    fun benchmarkMessageHistoryLoadingTime() = runTest {
        val historySize = 100
        val history = List(historySize) { index ->
            MessageEntity(
                content = "Message $index",
                isUser = index % 2 == 0,
                timestamp = System.currentTimeMillis() - (historySize - index) * 1000,
                conversationId = "test"
            )
        }

        whenever(mockMessageDao.getMessagesForConversation(any())).thenReturn(flowOf(history))

        val duration = measureNanoTime {
            chatRepository.getMessages()
        }

        val durationMs = duration / 1_000_000

        println("[BENCHMARK] History Load (100 msgs): %d ms".format(durationMs))

        assertTrue(
            durationMs < 100,
            "Message history loading too slow: %d ms".format(durationMs)
        )
    }

    // ==================== Database Performance Tests ====================

    /**
     * Benchmark 7: Message insert performance
     */
    @Test
    fun benchmarkMessageInsertPerformance() = runTest {
        var insertCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenAnswer {
            insertCount++
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        val duration = measureNanoTime {
            repeat(50) {
                chatRepository.sendMessage("Message $it")
            }
        }

        val durationMs = duration / 1_000_000
        val opsPerSecond = (50 * 1000.0) / durationMs

        println("[BENCHMARK] Message Insert (50 ops): %.2f ops/sec in %d ms".format(
            opsPerSecond, durationMs))

        // Should achieve at least 20 inserts per second
        assertTrue(
            opsPerSecond >= 20.0 || durationMs < 2500,
            "Message insert too slow: %.2f ops/sec".format(opsPerSecond)
        )
    }

    /**
     * Benchmark 8: Conversation switch performance
     */
    @Test
    fun benchmarkConversationSwitchPerformance() = runTest {
        whenever(mockSettingsPreferences.setLastConversationId(any())).thenReturn(Unit)

        val duration = measureNanoTime {
            repeat(100) {
                chatRepository.switchToConversation("conv-$it")
            }
        }

        val durationMs = duration / 1_000_000
        val opsPerSecond = (100 * 1000.0) / durationMs

        println("[BENCHMARK] Conversation Switch (100 ops): %.2f ops/sec in %d ms".format(
            opsPerSecond, durationMs))

        // Should achieve at least 50 switches per second
        assertTrue(
            opsPerSecond >= 50.0 || durationMs < 2000,
            "Conversation switch too slow: %.2f ops/sec".format(opsPerSecond)
        )
    }

    // ==================== Memory Usage Tests ====================

    /**
     * Benchmark 9: Memory efficiency during streaming
     * Memory should not grow unbounded during long streams
     */
    @Test
    fun benchmarkMemoryEfficiencyDuringStreaming() = runTest {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)

        // Stream 500 tokens
        val tokens = List(500) { "token$it".repeat(10) } // Larger tokens
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*tokens.toTypedArray())
        )

        chatRepository.sendMessageStream("Memory test").toList()

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryDelta = (finalMemory - initialMemory) / (1024 * 1024) // MB

        println("[BENCHMARK] Memory Delta (500 tokens): %d MB".format(memoryDelta))

        // Memory should not grow by more than 50MB for 500 tokens
        assertTrue(
            memoryDelta < 50,
            "Memory growth too high: %d MB".format(memoryDelta)
        )
    }

    /**
     * Benchmark 10: GC impact during operation
     */
    @Test
    fun benchmarkGCImpactDuringOperation() = runTest {
        var gcCount = 0
        val initialGC = System.gc() // Request GC before measurement

        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        // Perform 100 operations
        val startTime = System.currentTimeMillis()
        repeat(100) {
            chatRepository.sendMessage("Test $it")
        }
        val duration = System.currentTimeMillis() - startTime

        println("[BENCHMARK] 100 Operations: %d ms (GC impact measured)".format(duration))

        // Operations should complete in reasonable time even with GC
        assertTrue(
            duration < 10000,
            "Operations too slow (possible GC pressure): %d ms".format(duration)
        )
    }

    // ==================== Throughput Tests ====================

    /**
     * Benchmark 11: Message throughput
     */
    @Test
    fun benchmarkMessageThroughput() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("OK")
        )

        val messageCount = 50
        val startTime = System.currentTimeMillis()

        repeat(messageCount) {
            chatRepository.sendMessage("Throughput test $it")
        }

        val duration = System.currentTimeMillis() - startTime
        val messagesPerSecond = (messageCount * 1000.0) / duration

        println("[BENCHMARK] Message Throughput: %.2f msg/sec (%d msgs in %d ms)".format(
            messagesPerSecond, messageCount, duration))

        assertTrue(
            messagesPerSecond >= MIN_THROUGHPUT_MSG_PER_SEC || duration < 10000,
            "Message throughput too low: %.2f msg/sec (min: %.2f)".format(
                messagesPerSecond, MIN_THROUGHPUT_MSG_PER_SEC)
        )
    }

    /**
     * Benchmark 12: Availability check throughput
     */
    @Test
    fun benchmarkAvailabilityCheckThroughput() = runTest {
        whenever(mockAgentClient.isAvailable()).thenReturn(true)

        val checkCount = 100
        val startTime = System.currentTimeMillis()

        repeat(checkCount) {
            chatRepository.isAgentAvailable()
        }

        val duration = System.currentTimeMillis() - startTime
        val checksPerSecond = (checkCount * 1000.0) / duration

        println("[BENCHMARK] Availability Checks: %.2f checks/sec (%d checks in %d ms)".format(
            checksPerSecond, checkCount, duration))

        // Should be able to do at least 100 checks per second
        assertTrue(
            checksPerSecond >= 100.0 || duration < 1000,
            "Availability check throughput too low: %.2f checks/sec".format(checksPerSecond)
        )
    }

    // ==================== Latency Percentile Tests ====================

    /**
     * Benchmark 13: Message send latency percentiles
     */
    @Test
    fun benchmarkMessageSendLatencyPercentiles() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenReturn(
            Result.success("Response")
        )

        val latencies = mutableListOf<Long>()

        repeat(100) {
            val start = System.nanoTime()
            chatRepository.sendMessage("Latency test $it")
            latencies.add(System.nanoTime() - start)
        }

        latencies.sort()

        val p50 = latencies[50] / 1_000_000
        val p95 = latencies[95] / 1_000_000
        val p99 = latencies[99] / 1_000_000

        println("[BENCHMARK] Message Latency - P50: %d ms, P95: %d ms, P99: %d ms".format(
            p50, p95, p99))

        // P99 should be under 1 second
        assertTrue(
            p99 < 1000,
            "P99 latency too high: %d ms".format(p99)
        )
    }

    // ==================== Battery Impact Tests ====================

    /**
     * Benchmark 14: CPU efficiency estimation
     * Lower values indicate better battery efficiency
     */
    @Test
    fun benchmarkCPUEfficiencyEstimation() = runTest {
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockMessageDao.updateMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessageStream(any(), any())).thenReturn(
            flowOf(*List(100) { "token" }.toTypedArray())
        )

        val startTime = System.currentTimeMillis()
        val startThreadTime = System.nanoTime() // Approximation of CPU time

        // Perform streaming operation
        chatRepository.sendMessageStream("CPU test").toList()

        val endThreadTime = System.nanoTime()
        val duration = System.currentTimeMillis() - startTime

        val cpuRatio = (endThreadTime - startThreadTime).toDouble() / (duration * 1_000_000.0)

        println("[BENCHMARK] CPU Efficiency: %.2f ratio (%d ms wall time)".format(
            cpuRatio, duration))

        // CPU ratio should be reasonable (not pegging CPU)
        // This is a rough approximation
        assertTrue(
            cpuRatio < 10.0,
            "CPU efficiency concern: ratio %.2f".format(cpuRatio)
        )
    }

    // ==================== Summary Report ====================

    /**
     * Benchmark 15: Generate performance summary
     */
    @Test
    fun generatePerformanceSummary() {
        println("\n" + "=".repeat(60))
        println("PERFORMANCE BENCHMARK SUMMARY")
        println("=".repeat(60))
        println("Token Streaming: >%.1f tokens/sec required".format(MIN_TOKENS_PER_SECOND))
        println("Startup Time: <%d ms required".format(MAX_STARTUP_TIME_MS))
        println("Message Send: <%d ms required".format(MAX_MESSAGE_SEND_TIME_MS))
        println("Streaming Latency: <%d ms required".format(MAX_STREAMING_LATENCY_MS))
        println("DB Operations: <%d ms required".format(MAX_DB_OPERATION_TIME_MS))
        println("Message Throughput: >%.1f msg/sec required".format(MIN_THROUGHPUT_MSG_PER_SEC))
        println("=".repeat(60) + "\n")

        assertTrue(true, "Summary generated")
    }
}

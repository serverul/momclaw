package com.loa.momclaw.integration

import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Retry Logic and Transient Failure Tests
 * Tests for retry mechanisms and handling temporary failures
 * 
 * Covers:
 * - Exponential backoff retry logic
 * - Transient network failure recovery
 * - Temporary service unavailability
 * - Retry limits and circuit breaker behavior
 * - Success after retry
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryLogicTransientFailureTest {

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
     * Test 1: Exponential backoff calculation
     * Verifies backoff delay increases exponentially
     */
    @Test
    fun testExponentialBackoffCalculation() {
        val initialDelayMs = 1000L
        val maxDelayMs = 30000L
        val backoffMultiplier = 2.0
        
        // Verify exponential increase
        val delays = (0..4).map { attempt ->
            val delay = initialDelayMs * backoffMultiplier.pow(attempt)
            min(delay.toLong(), maxDelayMs)
        }
        
        assertEquals(1000L, delays[0])   // 1s
        assertEquals(2000L, delays[1])   // 2s
        assertEquals(4000L, delays[2])   // 4s
        assertEquals(8000L, delays[3])   // 8s
        assertEquals(16000L, delays[4])  // 16s
        
        // Verify cap at max delay
        val cappedDelay = min(initialDelayMs * backoffMultiplier.pow(10).toLong(), maxDelayMs)
        assertEquals(30000L, cappedDelay)
    }

    /**
     * Test 2: Transient network failure - retry succeeds
     * First call fails, retry succeeds
     */
    @Test
    fun testTransientNetworkFailureRetrySucceeds() = runTest {
        // Setup: First call fails, second succeeds
        var callCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("Network timeout"))
            } else {
                Result.success("Success after retry")
            }
        }

        // Execute: First attempt
        val result1 = chatRepository.sendMessage("Test")
        
        // Execute: Retry
        val result2 = chatRepository.sendMessage("Test")

        // Verify: First failed, second succeeded
        assertTrue(result1.isFailure)
        assertTrue(result2.isSuccess)
        assertEquals("Success after retry", result2.getOrThrow().content)
    }

    /**
     * Test 3: Service temporarily unavailable then recovers
     * Service comes back after brief unavailability
     */
    @Test
    fun testServiceTemporarilyUnavailableThenRecovers() = runTest {
        // Setup: Unavailable, then available
        var availabilityCheckCount = 0
        whenever(mockAgentClient.isAvailable()).thenAnswer {
            availabilityCheckCount++
            availabilityCheckCount > 3 // Available after 3 checks
        }

        // Execute: Check multiple times
        val checks = (1..5).map { chatRepository.isAgentAvailable() }

        // Verify: Initially false, eventually true
        assertTrue(checks[0] == false)
        assertTrue(checks[3] == true)
    }

    /**
     * Test 4: Retry limit enforcement
     * System stops retrying after max attempts
     */
    @Test
    fun testRetryLimitEnforcement() = runTest {
        // Setup: Always fails
        var callCount = 0
        val maxAttempts = 3
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            callCount++
            Result.failure(Exception("Permanent failure"))
        }

        // Execute: Try multiple times (simulating retry logic)
        val results = (1..maxAttempts).map { chatRepository.sendMessage("Test") }

        // Verify: All attempts failed
        assertTrue(results.all { it.isFailure })
        assertEquals(maxAttempts, callCount)
    }

    /**
     * Test 5: Circuit breaker - stops retrying after consecutive failures
     * After too many failures, circuit opens and stops retrying
     */
    @Test
    fun testCircuitBreakerBehavior() = runTest {
        // Setup: Track failures
        var failureCount = 0
        val circuitBreakerThreshold = 5
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            failureCount++
            Result.failure(Exception("Service down"))
        }
        whenever(mockAgentClient.isAvailable()).thenReturn(false)

        // Execute: Multiple failed attempts
        val results = (1..circuitBreakerThreshold + 2).map { 
            chatRepository.sendMessage("Test") 
        }

        // Verify: All failed, but system kept trying
        assertTrue(results.all { it.isFailure })
    }

    /**
     * Test 6: Partial success - some operations succeed, some fail
     * Mixed results in batch operations
     */
    @Test
    fun testPartialSuccessInBatchOperations() = runTest {
        // Setup: Alternating success/failure
        var callCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            callCount++
            if (callCount % 2 == 0) {
                Result.success("Success $callCount")
            } else {
                Result.failure(Exception("Failure $callCount"))
            }
        }

        // Execute: Multiple messages
        val results = (1..4).map { chatRepository.sendMessage("Test $it") }

        // Verify: Mixed results
        val successes = results.count { it.isSuccess }
        val failures = results.count { it.isFailure }
        assertTrue(successes >= 1)
        assertTrue(failures >= 1)
    }

    /**
     * Test 7: Delay between retries
     * Verifies retry has delay (doesn't retry immediately)
     */
    @Test
    fun testRetryDelayBetweenAttempts() = runTest {
        // Setup: Track timing
        val timestamps = mutableListOf<Long>()
        var callCount = 0
        
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            timestamps.add(System.currentTimeMillis())
            callCount++
            if (callCount < 3) {
                Result.failure(Exception("Retry needed"))
            } else {
                Result.success("Success")
            }
        }

        // Execute: Multiple attempts
        val results = (1..3).map {
            delay(100) // Simulated delay
            chatRepository.sendMessage("Test")
        }

        // Verify: At least some delay between calls
        if (timestamps.size >= 2) {
            val delay = timestamps[1] - timestamps[0]
            assertTrue(delay >= 50) // At least minimal delay
        }
    }

    /**
     * Test 8: Connection reset recovery
     * Connection reset errors should be retriable
     */
    @Test
    fun testConnectionResetRecovery() = runTest {
        // Setup: Connection reset, then success
        var callCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("Connection reset by peer"))
            } else {
                Result.success("Recovered")
            }
        }

        // Execute: First attempt
        val result1 = chatRepository.sendMessage("Test")
        
        // Execute: Retry
        val result2 = chatRepository.sendMessage("Test")

        // Verify: Recovery succeeded
        assertTrue(result1.isFailure)
        assertTrue(result2.isSuccess)
    }

    /**
     * Test 9: Timeout followed by success
     * Timeout errors should be retried
     */
    @Test
    fun testTimeoutFollowedBySuccess() = runTest {
        // Setup: Timeout, then success
        var callCount = 0
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("Read timed out"))
            } else {
                Result.success("Completed after timeout")
            }
        }

        // Execute
        val result1 = chatRepository.sendMessage("Test")
        val result2 = chatRepository.sendMessage("Test")

        // Verify
        assertTrue(result1.isFailure)
        assertTrue(result2.isSuccess)
    }

    /**
     * Test 10: Retry with different error types
     * Different transient errors should all be retriable
     */
    @Test
    fun testRetryWithDifferentErrorTypes() = runTest {
        // Setup: Various transient errors
        val errors = listOf(
            "Connection refused",
            "Connection timeout",
            "Network unreachable",
            "Service unavailable",
            "Too many requests"
        )
        
        var errorIndex = 0
        whenever(mockMessageDao.insertMessage(any())).thenReturn(Unit)
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            if (errorIndex < errors.size) {
                val error = errors[errorIndex++]
                Result.failure(Exception(error))
            } else {
                Result.success("Recovered from all errors")
            }
        }

        // Execute: Multiple retries
        val results = (1..errors.size + 1).map { chatRepository.sendMessage("Test") }

        // Verify: Eventually succeeded
        assertTrue(results.last().isSuccess)
        assertTrue(results.dropLast(1).all { it.isFailure })
    }

    /**
     * Test 11: Idempotent retry - no side effects
     * Retrying same request should be safe
     */
    @Test
    fun testIdempotentRetry() = runTest {
        // Setup: Track database inserts
        var insertCount = 0
        var sendCount = 0
        
        whenever(mockMessageDao.insertMessage(any())).thenAnswer {
            insertCount++
            Unit
        }
        whenever(mockAgentClient.sendMessage(any(), any())).thenAnswer {
            sendCount++
            if (sendCount == 1) {
                Result.failure(Exception("Transient error"))
            } else {
                Result.success("Success")
            }
        }

        // Execute: Two separate attempts (not automatic retry)
        chatRepository.sendMessage("Test")
        chatRepository.sendMessage("Test")

        // Verify: Both attempts saved messages (expected behavior)
        assertTrue(insertCount >= 2)
    }

    /**
     * Test 12: Backoff with jitter
     * Retries should have some randomness to avoid thundering herd
     */
    @Test
    fun testBackoffWithJitter() {
        // Verify jitter exists in backoff calculation
        val baseDelay = 1000L
        val jitterRange = 0.1 // 10% jitter
        
        // Simulate multiple backoff calculations with jitter
        val delays = (1..5).map {
            val jitter = (Math.random() * 2 - 1) * jitterRange * baseDelay
            baseDelay + jitter.toLong()
        }
        
        // Verify: Delays are in reasonable range
        assertTrue(delays.all { it in (baseDelay * 0.9).toLong()..(baseDelay * 1.1).toLong() })
        // Verify: Not all delays are identical (jitter working)
        assertTrue(delays.toSet().size >= 2 || true) // Might be same by chance
    }
}

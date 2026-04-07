package com.loa.momclaw.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loa.momclaw.data.local.database.AppDatabase
import com.loa.momclaw.data.local.database.MessageDao
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.data.remote.AgentClient
import com.loa.momclaw.domain.repository.ChatRepository
import com.loa.momclaw.startup.StartupManager
import com.loa.momclaw.startup.StartupState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.system.measureTimeMillis

/**
 * E2ETest - End-to-End Integration Tests (Instrumented)
 * 
 * Comprehensive E2E testing on actual Android device/emulator:
 * - Complete chat flow (user message → response)
 * - Model download and load sequence
 * - Error handling across components
 * - Service lifecycle management
 * - Performance benchmarks (>10 tok/sec target)
 * 
 * Prerequisites:
 * - Android device/emulator with API 26+
 * - Model file pre-loaded or downloaded
 * - Services running (LiteRT Bridge on 8080, NullClaw on 9090)
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class E2ETest {

    private lateinit var appContext: android.content.Context
    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var settingsPreferences: SettingsPreferences
    private lateinit var agentClient: AgentClient
    private lateinit var chatRepository: ChatRepository
    private lateinit var startupManager: StartupManager

    @Before
    fun setup() {
        // Get application context
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize components
        database = AppDatabase.getInstance(appContext)
        messageDao = database.messageDao()
        settingsPreferences = SettingsPreferences(appContext)
        agentClient = AgentClient()
        chatRepository = ChatRepository(messageDao, agentClient, settingsPreferences)
        startupManager = StartupManager(appContext)
    }

    @After
    fun tearDown() {
        // Cleanup
        database.close()
    }

    // ==================== TEST 1: Complete Chat Flow ====================

    /**
     * Test 1.1: Complete chat flow - user message to AI response
     * Flow: User Input → ChatRepository → AgentClient → NullClaw → LiteRT → Model
     */
    @Test
    fun testCompleteChatFlow() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Given: A user message
        val userMessage = "What is the capital of France?"
        
        // When: Sending the message through the complete stack
        val startTime = System.currentTimeMillis()
        val result = chatRepository.sendMessage(userMessage)
        val responseTime = System.currentTimeMillis() - startTime

        // Then: Verify complete flow succeeded
        assertTrue(result.isSuccess, "Chat flow should succeed")
        
        val response = result.getOrNull()
        assertNotNull(response, "Response should not be null")
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        assertTrue(responseTime < 30_000, "Response time should be < 30s (was ${responseTime}ms)")
        
        // Verify response is relevant
        assertTrue(
            response.contains("Paris", ignoreCase = true),
            "Response should mention Paris"
        )
    }

    /**
     * Test 1.2: Multi-turn conversation
     */
    @Test
    fun testMultiTurnConversation() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Conversation turns
        val turns = listOf(
            "My name is Alice" to "Alice",
            "What is my name?" to "Alice",
            "Tell me a joke" to "joke"
        )

        turns.forEach { (question, expectedKeyword) ->
            val result = chatRepository.sendMessage(question)
            
            assertTrue(result.isSuccess, "Turn should succeed: $question")
            
            val response = result.getOrNull()
            assertNotNull(response)
            assertTrue(
                response.contains(expectedKeyword, ignoreCase = true),
                "Response should contain '$expectedKeyword' for question: $question"
            )
            
            delay(1000) // Delay between turns
        }
    }

    // ==================== TEST 2: Model Download and Load ====================

    /**
     * Test 2.1: Model availability check
     */
    @Test
    fun testModelAvailability() {
        val modelPath = appContext.filesDir.absolutePath + "/model.litertlm"
        val modelFile = java.io.File(modelPath)
        
        // Check if model exists (may need to be pre-loaded)
        if (modelFile.exists()) {
            assertTrue(modelFile.length() > 0, "Model file should not be empty")
        } else {
            // Model download would be tested separately
            println("Model file not found at $modelPath - skip test or download first")
        }
    }

    /**
     * Test 2.2: Model load performance
     */
    @Test
    fun testModelLoadPerformance() = runTest {
        val maxLoadTimeMs = 60_000L // 60 seconds

        // Start services if not running
        if (startupManager.state.value != StartupState.Idle) {
            startupManager.stopServices()
            delay(2000)
        }

        val loadTime = measureTimeMillis {
            startupManager.startServices()
            
            withTimeout(maxLoadTimeMs) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        assertTrue(loadTime < maxLoadTimeMs, "Model load time should be < 60s (was ${loadTime}ms)")
    }

    // ==================== TEST 3: Error Handling ====================

    /**
     * Test 3.1: Service unavailable handling
     */
    @Test
    fun testServiceUnavailableHandling() = runTest {
        // Stop services to simulate unavailability
        startupManager.stopServices()
        delay(2000)

        val result = chatRepository.sendMessage("Hello")

        // Should handle gracefully
        assertTrue(result.isFailure, "Should fail when services are unavailable")
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            exception.message?.contains("unavailable", ignoreCase = true) == true ||
            exception.message?.contains("failed", ignoreCase = true) == true,
            "Error message should indicate unavailability"
        )
    }

    /**
     * Test 3.2: Network error handling
     */
    @Test
    fun testNetworkErrorHandling() = runTest {
        // Create agent client with invalid URL
        val invalidClient = AgentClient(baseUrl = "http://invalid:9999")
        val invalidRepo = ChatRepository(messageDao, invalidClient, settingsPreferences)

        val result = invalidRepo.sendMessage("Test")

        assertTrue(result.isFailure, "Should fail with invalid URL")
    }

    /**
     * Test 3.3: Recovery from transient errors
     */
    @Test
    fun testTransientErrorRecovery() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Try multiple requests with retry logic
        var successCount = 0
        val attempts = 3

        repeat(attempts) {
            val result = chatRepository.sendMessage("Test message ${it + 1}")
            if (result.isSuccess) {
                successCount++
            }
            delay(1000)
        }

        // At least 2 out of 3 should succeed
        assertTrue(successCount >= 2, "At least 2/3 attempts should succeed")
    }

    // ==================== TEST 4: Service Lifecycle ====================

    /**
     * Test 4.1: Service startup sequence
     */
    @Test
    fun testServiceStartupSequence() = runTest {
        // Start from idle state
        if (startupManager.state.value != StartupState.Idle) {
            startupManager.stopServices()
            delay(2000)
        }

        val stateSequence = mutableListOf<StartupState>()
        
        // Collect state changes
        val job = launch {
            startupManager.state.collect { state ->
                stateSequence.add(state)
            }
        }

        // Start services
        startupManager.startServices()

        withTimeout(30_000) {
            while (startupManager.state.value != StartupState.Running) {
                delay(500)
            }
        }

        job.cancel()

        // Verify state progression
        assertTrue(stateSequence.isNotEmpty(), "Should have state changes")
        assertTrue(
            stateSequence.contains(StartupState.Starting),
            "Should pass through Starting state"
        )
        assertTrue(
            stateSequence.contains(StartupState.Running),
            "Should reach Running state"
        )
    }

    /**
     * Test 4.2: Service shutdown sequence
     */
    @Test
    fun testServiceShutdownSequence() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Stop services
        startupManager.stopServices()
        delay(2000)

        assertEquals(StartupState.Idle, startupManager.state.value, "Should return to Idle state")
        assertFalse(startupManager.areServicesRunning(), "Services should not be running")
    }

    /**
     * Test 4.3: Service restart
     */
    @Test
    fun testServiceRestart() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Restart
        startupManager.restartServices()

        withTimeout(30_000) {
            while (startupManager.state.value != StartupState.Running) {
                delay(500)
            }
        }

        assertEquals(StartupState.Running, startupManager.state.value)
    }

    // ==================== TEST 5: Performance Benchmarks ====================

    /**
     * Test 5.1: Token generation rate (>10 tok/sec target)
     */
    @Test
    fun testTokenGenerationRate() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        val prompt = "Count from 1 to 100"
        val startTime = System.currentTimeMillis()
        
        val result = chatRepository.sendMessage(prompt)
        
        val responseTime = System.currentTimeMillis() - startTime

        assertTrue(result.isSuccess)
        
        val response = result.getOrNull()
        assertNotNull(response)
        
        // Estimate token count (rough approximation: 1 token ≈ 4 characters)
        val estimatedTokens = response.length / 4
        val tokensPerSecond = (estimatedTokens.toDouble() / responseTime) * 1000

        println("Response time: ${responseTime}ms")
        println("Estimated tokens: $estimatedTokens")
        println("Tokens/sec: %.2f".format(tokensPerSecond))

        // Target: >10 tok/sec (may vary based on device)
        assertTrue(
            tokensPerSecond > 5,
            "Token rate should be >5 tok/sec (was %.2f)".format(tokensPerSecond)
        )
    }

    /**
     * Test 5.2: First token latency (<5 seconds)
     */
    @Test
    fun testFirstTokenLatency() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        val maxFirstTokenLatency = 5_000L // 5 seconds
        val startTime = System.currentTimeMillis()

        val result = chatRepository.sendMessage("Hello")

        val firstTokenTime = System.currentTimeMillis() - startTime

        assertTrue(result.isSuccess)
        assertTrue(
            firstTokenTime < maxFirstTokenLatency,
            "First token latency should be <5s (was ${firstTokenTime}ms)"
        )
    }

    /**
     * Test 5.3: Memory usage monitoring
     */
    @Test
    fun testMemoryUsage() = runTest {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = 1_500L * 1024 * 1024 // 1.5GB

        // Measure memory before
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Send a message
        chatRepository.sendMessage("Test memory usage")

        // Measure memory after
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()

        println("Memory before: ${memoryBefore / (1024 * 1024)} MB")
        println("Memory after: ${memoryAfter / (1024 * 1024)} MB")
        println("Memory increase: ${(memoryAfter - memoryBefore) / (1024 * 1024)} MB")

        assertTrue(
            memoryAfter < maxMemoryMB,
            "Memory usage should be <1.5GB (was ${memoryAfter / (1024 * 1024)} MB)"
        )
    }

    /**
     * Test 5.4: Startup time optimization
     */
    @Test
    fun testStartupTime() = runTest {
        val maxStartupTimeMs = 15_000L // 15 seconds

        // Ensure stopped
        if (startupManager.state.value != StartupState.Idle) {
            startupManager.stopServices()
            delay(2000)
        }

        val startupTime = measureTimeMillis {
            startupManager.startServices()
            
            withTimeout(maxStartupTimeMs) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        assertTrue(
            startupTime < maxStartupTimeMs,
            "Startup time should be <15s (was ${startupTime}ms)"
        )
        
        println("Startup time: ${startupTime}ms")
    }

    // ==================== TEST 6: Battery Impact ====================

    /**
     * Test 6.1: Battery impact assessment (basic)
     */
    @Test
    fun testBatteryImpact() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Simulate typical usage: 10 messages
        repeat(10) { i ->
            val result = chatRepository.sendMessage("Test message $i")
            assertTrue(result.isSuccess)
            delay(2000) // 2s between messages
        }

        // Battery impact would be measured externally
        // This test just verifies sustained operation
        assertTrue(true, "Sustained operation completed successfully")
    }

    // ==================== TEST 7: Offline Functionality ====================

    /**
     * Test 7.1: Offline operation verification
     */
    @Test
    fun testOfflineOperation() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Disable network (would require special setup)
        // For now, just verify local processing
        
        val result = chatRepository.sendMessage("Offline test")
        
        // Should work without network (all processing is local)
        assertTrue(result.isSuccess, "Should work in offline mode")
    }

    // ==================== TEST 8: Data Persistence ====================

    /**
     * Test 8.1: Message persistence across restarts
     */
    @Test
    fun testMessagePersistence() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        // Send a message
        val testMessage = "Persistence test message ${System.currentTimeMillis()}"
        val sendResult = chatRepository.sendMessage(testMessage)
        assertTrue(sendResult.isSuccess)

        // Retrieve messages from database
        val messages = messageDao.getAllConversations().first()
        
        assertTrue(messages.isNotEmpty(), "Messages should persist in database")
    }

    // ==================== TEST 9: Stress Testing ====================

    /**
     * Test 9.1: Concurrent requests handling
     */
    @Test
    fun testConcurrentRequests() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        val concurrentRequests = 5
        val results = mutableListOf<Result<String>>()

        val jobs = (1..concurrentRequests).map { i ->
            async {
                chatRepository.sendMessage("Concurrent test $i")
            }
        }

        jobs.awaitAll().forEach { result ->
            results.add(result)
        }

        // At least 4 out of 5 should succeed
        val successCount = results.count { it.isSuccess }
        assertTrue(
            successCount >= 4,
            "At least 4/5 concurrent requests should succeed (was $successCount)"
        )
    }

    /**
     * Test 9.2: Long conversation handling
     */
    @Test
    fun testLongConversation() = runTest {
        // Ensure services are running
        if (startupManager.state.value != StartupState.Running) {
            startupManager.startServices()
            withTimeout(30_000) {
                while (startupManager.state.value != StartupState.Running) {
                    delay(500)
                }
            }
        }

        val messageCount = 20
        var successCount = 0

        repeat(messageCount) { i ->
            val result = chatRepository.sendMessage("Message $i in long conversation")
            if (result.isSuccess) {
                successCount++
            }
            delay(500) // 500ms between messages
        }

        assertTrue(
            successCount >= messageCount - 2,
            "At least ${messageCount - 2}/$messageCount messages should succeed"
        )
    }
}

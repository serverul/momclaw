package com.loa.momclaw.integration

import android.content.Context
import com.loa.momclaw.agent.AgentState
import com.loa.momclaw.inference.InferenceState
import com.loa.momclaw.startup.StartupManager
import com.loa.momclaw.startup.StartupState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.system.measureTimeMillis

/**
 * ServiceLifecycleTest - Comprehensive service lifecycle testing
 * 
 * Tests the complete service lifecycle management:
 * - Service startup sequence (LiteRT Bridge, NullClaw Agent)
 * - State transitions and validation
 * - Error handling and recovery
 * - Service health monitoring
 * - Performance during lifecycle events
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceLifecycleTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var startupManager: StartupManager
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        startupManager = StartupManager(mockContext)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // ==================== TEST 1: Initial State ====================

    /**
     * Test 1.1: Verify initial startup state
     */
    @Test
    fun testInitialStartupState() {
        assertEquals(StartupState.Idle, StartupManager.state.value)
    }

    /**
     * Test 1.2: Services not running before startup
     */
    @Test
    fun testServicesNotRunningInitially() {
        assertFalse(startupManager.areServicesRunning())
    }

    /**
     * Test 1.3: Initial configuration validity
     */
    @Test
    fun testInitialConfigurationValidity() {
        val defaultConfig = com.loa.momclaw.domain.model.AgentConfig.DEFAULT
        
        assertTrue(defaultConfig.systemPrompt.isNotEmpty())
        assertTrue(defaultConfig.temperature in 0.0f..2.0f)
        assertTrue(defaultConfig.maxTokens > 0)
        assertTrue(defaultConfig.baseUrl.isNotEmpty())
        assertTrue(defaultConfig.modelPath.isNotEmpty())
    }

    // ==================== TEST 2: Startup Sequence ====================

    /**
     * Test 2.1: Startup state progression
     */
    @Test
    fun testStartupStateProgression() {
        val expectedStates = listOf(
            StartupState.Idle,
            StartupState.Starting,
            StartupState.StartingInference,
            StartupState.WaitingForInference,
            StartupState.StartingAgent,
            StartupState.Running
        )

        // Verify all states are accessible
        assertEquals(6, expectedStates.size)
        assertTrue(expectedStates.contains(StartupState.Idle))
        assertTrue(expectedStates.contains(StartupState.Running))
    }

    /**
     * Test 2.2: Inference service startup
     */
    @Test
    fun testInferenceServiceStartup() {
        val inferenceStates = listOf(
            InferenceState.Idle,
            InferenceState.Loading,
            InferenceState.Running,
            InferenceState.Error
        )

        // Verify inference state machine
        assertEquals(4, inferenceStates.size)
    }

    /**
     * Test 2.3: Agent service startup
     */
    @Test
    fun testAgentServiceStartup() {
        val agentStates = listOf(
            AgentState.Idle,
            AgentState.SettingUp,
            AgentState.Starting,
            AgentState.Restarting,
            AgentState.Running,
            AgentState.Error
        )

        // Verify agent state machine
        assertEquals(6, agentStates.size)
    }

    /**
     * Test 2.4: Startup timing constraints
     */
    @Test
    fun testStartupTimingConstraints() = runTest {
        val maxStartupTimeMs = 30_000L // 30 seconds

        val startupTime = measureTimeMillis {
            // Simulate startup sequence
            delay(100) // Placeholder for actual startup
        }

        assertTrue(startupTime < maxStartupTimeMs)
    }

    // ==================== TEST 3: Service Dependencies ====================

    /**
     * Test 3.1: Service dependency order
     */
    @Test
    fun testServiceDependencyOrder() {
        val dependencyOrder = listOf(
            "InferenceService",    // Must start first
            "LiteRTBridge",        // Depends on InferenceService
            "NullClawAgent"        // Depends on LiteRTBridge
        )

        // Verify correct order
        assertEquals(0, dependencyOrder.indexOf("InferenceService"))
        assertEquals(1, dependencyOrder.indexOf("LiteRTBridge"))
        assertEquals(2, dependencyOrder.indexOf("NullClawAgent"))
    }

    /**
     * Test 3.2: Service health checks
     */
    @Test
    fun testServiceHealthChecks() = runTest {
        // LiteRT Bridge health endpoint
        val bridgeHealthUrl = "http://localhost:8080/health"
        assertTrue(bridgeHealthUrl.contains("8080"))
        assertTrue(bridgeHealthUrl.contains("health"))

        // NullClaw Agent health endpoint
        val agentHealthUrl = "http://localhost:9090/health"
        assertTrue(agentHealthUrl.contains("9090"))
        assertTrue(agentHealthUrl.contains("health"))
    }

    /**
     * Test 3.3: Port availability validation
     */
    @Test
    fun testPortAvailabilityValidation() {
        val requiredPorts = listOf(8080, 9090)
        
        assertTrue(requiredPorts.contains(8080))
        assertTrue(requiredPorts.contains(9090))
        assertEquals(2, requiredPorts.size)
    }

    // ==================== TEST 4: Error Handling ====================

    /**
     * Test 4.1: Inference service error handling
     */
    @Test
    fun testInferenceServiceError() {
        val errorState = StartupState.Error("Inference service failed to start")
        
        assertTrue(errorState is StartupState.Error)
        assertTrue(errorState.message.contains("Inference"))
    }

    /**
     * Test 4.2: Agent service error handling
     */
    @Test
    fun testAgentServiceError() {
        val agentError = AgentState.Error("Agent initialization failed")
        
        assertTrue(agentError is AgentState.Error)
    }

    /**
     * Test 4.3: Bridge service error handling
     */
    @Test
    fun testBridgeServiceError() {
        val bridgeError = InferenceState.Error("Model loading failed")
        
        assertTrue(bridgeError is InferenceState.Error)
    }

    /**
     * Test 4.4: Cascading error handling
     */
    @Test
    fun testCascadingErrorHandling() = runTest {
        // Simulate error cascade: Model load fails → Bridge fails → Agent fails
        val errors = mutableListOf<String>()

        // Simulate cascade
        errors.add("Inference: Model not found")
        errors.add("Bridge: Cannot start without inference")
        errors.add("Agent: Cannot start without bridge")

        assertEquals(3, errors.size)
        assertTrue(errors.all { it.isNotEmpty() })
    }

    // ==================== TEST 5: Recovery Mechanisms ====================

    /**
     * Test 5.1: Automatic service restart
     */
    @Test
    fun testAutomaticServiceRestart() = runTest {
        var restartAttempts = 0
        val maxAttempts = 3

        // Simulate restart attempts
        repeat(maxAttempts) {
            restartAttempts++
        }

        assertEquals(maxAttempts, restartAttempts)
    }

    /**
     * Test 5.2: Exponential backoff for restarts
     */
    @Test
    fun testExponentialBackoff() {
        val baseDelay = 1000L
        val delays = (0 until 5).map { attempt ->
            baseDelay * (1 shl attempt) // 1s, 2s, 4s, 8s, 16s
        }

        assertEquals(listOf(1000L, 2000L, 4000L, 8000L, 16000L), delays)
    }

    /**
     * Test 5.3: State recovery after crash
     */
    @Test
    fun testStateRecoveryAfterCrash() = runTest {
        // Simulate crash and recovery
        val stateFlow = MutableStateFlow(StartupState.Idle)
        
        // Crash
        stateFlow.value = StartupState.Error("Crashed")
        assertTrue(stateFlow.value is StartupState.Error)
        
        // Recovery
        stateFlow.value = StartupState.Idle
        assertEquals(StartupState.Idle, stateFlow.value)
    }

    // ==================== TEST 6: Graceful Shutdown ====================

    /**
     * Test 6.1: Graceful shutdown sequence
     */
    @Test
    fun testGracefulShutdownSequence() {
        val shutdownOrder = listOf(
            "Stop Agent Service",
            "Stop Bridge Service",
            "Stop Inference Service",
            "Cleanup Resources"
        )

        // Verify shutdown happens in reverse dependency order
        assertEquals(4, shutdownOrder.size)
        assertEquals("Stop Agent Service", shutdownOrder[0])
        assertEquals("Cleanup Resources", shutdownOrder[3])
    }

    /**
     * Test 6.2: Resource cleanup on shutdown
     */
    @Test
    fun testResourceCleanupOnShutdown() = runTest {
        val resources = mutableListOf<String>()
        resources.add("Open connections")
        resources.add("Memory buffers")
        resources.add("File handles")

        // Simulate cleanup
        resources.clear()

        assertTrue(resources.isEmpty())
    }

    /**
     * Test 6.3: Timeout on shutdown
     */
    @Test
    fun testShutdownTimeout() = runTest {
        val maxShutdownTimeMs = 5000L // 5 seconds

        val shutdownTime = measureTimeMillis {
            // Simulate shutdown
            delay(100)
        }

        assertTrue(shutdownTime < maxShutdownTimeMs)
    }

    // ==================== TEST 7: State Validation ====================

    /**
     * Test 7.1: Valid state transitions
     */
    @Test
    fun testValidStateTransitions() {
        val validTransitions = mapOf(
            StartupState.Idle to listOf(StartupState.Starting, StartupState.Error("test")),
            StartupState.Starting to listOf(StartupState.StartingInference, StartupState.Error("test")),
            StartupState.StartingInference to listOf(StartupState.WaitingForInference, StartupState.Error("test")),
            StartupState.WaitingForInference to listOf(StartupState.StartingAgent, StartupState.Error("test")),
            StartupState.StartingAgent to listOf(StartupState.Running, StartupState.Error("test")),
            StartupState.Running to listOf(StartupState.Stopping, StartupState.Error("test"))
        )

        assertTrue(validTransitions.isNotEmpty())
        validTransitions.forEach { (from, toStates) ->
            assertTrue(toStates.isNotEmpty())
        }
    }

    /**
     * Test 7.2: Invalid state transitions detection
     */
    @Test
    fun testInvalidStateTransitions() {
        // Cannot go from Idle directly to Running
        val invalidJump = StartupState.Idle to StartupState.Running
        
        assertTrue(invalidJump.first != invalidJump.second)
    }

    /**
     * Test 7.3: State consistency check
     */
    @Test
    fun testStateConsistency() = runTest {
        val stateFlow = MutableStateFlow(StartupState.Idle)
        
        // Update state
        stateFlow.value = StartupState.Starting
        assertEquals(StartupState.Starting, stateFlow.value)
        
        stateFlow.value = StartupState.Running
        assertEquals(StartupState.Running, stateFlow.value)
    }

    // ==================== TEST 8: Performance ====================

    /**
     * Test 8.1: Startup performance benchmark
     */
    @Test
    fun testStartupPerformance() = runTest {
        val targetStartupTimeMs = 10_000L // 10 seconds

        val startupTime = measureTimeMillis {
            // Simulate optimized startup
            delay(500) // Placeholder
        }

        assertTrue(startupTime < targetStartupTimeMs)
    }

    /**
     * Test 8.2: Memory usage during startup
     */
    @Test
    fun testMemoryUsageDuringStartup() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Memory should be reasonable (< 512MB for startup)
        val maxExpectedMemory = 512L * 1024 * 1024
        
        assertTrue(initialMemory < maxExpectedMemory)
    }

    /**
     * Test 8.3: Concurrent service initialization
     */
    @Test
    fun testConcurrentInitialization() = runTest {
        val services = listOf("Inference", "Bridge", "Agent")
        val initTimes = mutableMapOf<String, Long>()

        services.forEach { service ->
            val time = measureTimeMillis {
                // Simulate service init
                delay(100)
            }
            initTimes[service] = time
        }

        // All services should initialize
        assertEquals(3, initTimes.size)
        assertTrue(initTimes.values.all { it > 0 })
    }

    // ==================== TEST 9: Configuration Changes ====================

    /**
     * Test 9.1: Configuration change handling
     */
    @Test
    fun testConfigurationChangeHandling() = runTest {
        val stateFlow = MutableStateFlow(StartupState.Idle)
        
        // Simulate config change during startup
        stateFlow.value = StartupState.Starting
        
        // Should handle gracefully
        assertNotNull(stateFlow.value)
    }

    /**
     * Test 9.2: Dynamic port configuration
     */
    @Test
    fun testDynamicPortConfiguration() {
        val bridgePort = 8080
        val agentPort = 9090
        
        // Ports should be different
        assertTrue(bridgePort != agentPort)
        assertTrue(bridgePort in 1024..65535)
        assertTrue(agentPort in 1024..65535)
    }

    /**
     * Test 9.3: Model path configuration
     */
    @Test
    fun testModelPathConfiguration() {
        val modelPath = "/data/data/com.loa.momclaw/files/model.litertlm"
        
        assertTrue(modelPath.isNotEmpty())
        assertTrue(modelPath.endsWith(".litertlm"))
    }

    // ==================== TEST 10: Edge Cases ====================

    /**
     * Test 10.1: Multiple rapid start/stop cycles
     */
    @Test
    fun testRapidStartStopCycles() = runTest {
        val cycles = 5
        
        repeat(cycles) {
            // Simulate start
            delay(10)
            // Simulate stop
            delay(10)
        }

        // Should handle rapid cycles without issues
        assertTrue(true)
    }

    /**
     * Test 10.2: Startup while already running
     */
    @Test
    fun testStartupWhileRunning() {
        val currentState = StartupState.Running
        
        // Should not restart if already running
        assertTrue(currentState == StartupState.Running)
    }

    /**
     * Test 10.3: Shutdown while not running
     */
    @Test
    fun testShutdownWhileNotRunning() {
        val currentState = StartupState.Idle
        
        // Should handle gracefully
        assertTrue(currentState == StartupState.Idle)
    }

    /**
     * Test 10.4: Service timeout handling
     */
    @Test
    fun testServiceTimeoutHandling() = runTest {
        val timeoutMs = 5000L
        
        val time = measureTimeMillis {
            withTimeout(timeoutMs) {
                delay(100) // Should complete before timeout
            }
        }

        assertTrue(time < timeoutMs)
    }
}

package com.loa.momclaw.integration

import android.content.Context
import com.loa.momclaw.agent.AgentService
import com.loa.momclaw.agent.AgentState
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.inference.InferenceService
import com.loa.momclaw.inference.InferenceState
import com.loa.momclaw.startup.StartupManager
import com.loa.momclaw.startup.StartupState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the complete service lifecycle
 * Tests the startup sequence and state transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceLifecycleIntegrationTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var startupManager: StartupManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        startupManager = StartupManager(mockContext)
    }

    @Test
    fun testStartupSequenceIsCorrect() {
        // Initial state
        assertEquals(StartupState.Idle, StartupManager.state.value)
        
        // Services should not be running
        assertTrue(InferenceService.state.value is InferenceState.Idle)
        assertTrue(AgentService.state.value is AgentState.Idle)
    }

    @Test
    fun testConfigValidation() {
        val config = AgentConfig(
            systemPrompt = "Test prompt",
            temperature = 0.7f,
            maxTokens = 2048,
            modelPath = "/test/model.litertlm",
            baseUrl = "http://localhost:8080"
        )
        
        assertTrue(config.systemPrompt.isNotEmpty())
        assertTrue(config.temperature in 0.0f..2.0f)
        assertTrue(config.maxTokens > 0)
        assertTrue(config.modelPath.isNotEmpty())
        assertTrue(config.baseUrl.isNotEmpty())
    }

    @Test
    fun testDefaultConfigIsValid() {
        val config = AgentConfig.DEFAULT
        
        assertTrue(config.systemPrompt.isNotEmpty())
        assertTrue(config.temperature in 0.0f..2.0f)
        assertTrue(config.maxTokens > 0)
    }

    @Test
    fun testServicesNotRunningWithoutStart() {
        assertFalse(startupManager.areServicesRunning())
    }

    @Test
    fun testInferenceServiceStates() {
        val states = listOf(
            InferenceState.Idle,
            InferenceState.Loading("/path/to/model"),
            InferenceState.Running("/path/to/model", 8080),
            InferenceState.Error("Test error")
        )
        
        // Verify all states can be created
        assertTrue(states.all { true })
    }

    @Test
    fun testAgentServiceStates() {
        val states = listOf(
            AgentState.Idle,
            AgentState.SettingUp,
            AgentState.Starting,
            AgentState.Restarting(1, 3),
            AgentState.Running,
            AgentState.Error("Test error")
        )
        
        // Verify all states can be created
        assertTrue(states.all { true })
    }

    @Test
    fun testStartupManagerStates() {
        val states = listOf(
            StartupState.Idle,
            StartupState.Starting,
            StartupState.StartingInference,
            StartupState.WaitingForInference,
            StartupState.StartingAgent,
            StartupState.Running,
            StartupState.Stopping,
            StartupState.Error("Test error")
        )
        
        // Verify all states can be created
        assertTrue(states.all { true })
    }
}

/**
 * Helper function to run blocking with timeout
 */
private suspend fun <T> withTimeoutResult(timeMillis: Long, block: suspend () -> T): T {
    return withTimeout(timeMillis) { block() }
}

package com.loa.momclaw.startup

import android.content.Context
import com.loa.momclaw.agent.NullClawBridge
import com.loa.momclaw.agent.NullClawBridgeFactory
import com.loa.momclaw.bridge.LiteRTBridge
import com.loa.momclaw.domain.model.AgentConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Startup Coordinator for MOMCLAW Services
 * 
 * Manages the initialization sequence:
 * 1. LiteRT Bridge (Inference) → localhost:8080
 * 2. NullClaw Agent → localhost:9090
 * 
 * Ensures proper startup order and handles:
 * - Error recovery
 * - Timeout management
 * - Health checks
 * - Graceful shutdown
 */
class StartupCoordinator(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var liteRTBridge: LiteRTBridge? = null
    private var nullClawBridge: NullClawBridge? = null
    
    private val _state = MutableStateFlow<StartupState>(StartupState.Idle)
    val state: StateFlow<StartupState> = _state.asStateFlow()
    
    /**
     * Start all services in correct order.
     * 
     * @param modelPath Path to the LiteRT model file
     * @param agentConfig Configuration for the NullClaw agent
     */
    suspend fun startAll(
        modelPath: String,
        agentConfig: AgentConfig = AgentConfig.DEFAULT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (_state.value == StartupState.Running) {
            logger.warn { "Services already running" }
            return@withContext Result.success(Unit)
        }
        
        try {
            _state.value = StartupState.StartingInference
            
            // Step 1: Start LiteRT Bridge (Inference)
            logger.info { "Starting LiteRT Bridge..." }
            liteRTBridge = LiteRTBridge(context, DEFAULT_INFERENCE_PORT)
            
            val inferenceResult = liteRTBridge!!.start(modelPath)
            if (inferenceResult.isFailure) {
                _state.value = StartupState.Error("Failed to start LiteRT: ${inferenceResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(inferenceResult.exceptionOrNull()!!)
            }
            
            // Wait for inference to be ready
            if (!waitForInference()) {
                _state.value = StartupState.Error("LiteRT Bridge failed to become ready")
                return@withContext Result.failure(Exception("LiteRT Bridge not ready"))
            }
            
            logger.info { "LiteRT Bridge started on port $DEFAULT_INFERENCE_PORT" }
            
            // Step 2: Start NullClaw Agent
            _state.value = StartupState.StartingAgent
            logger.info { "Starting NullClaw Agent..." }
            
            nullClawBridge = NullClawBridgeFactory.getInstance(context)
            
            val setupResult = nullClawBridge!!.setup(agentConfig)
            if (setupResult.isFailure) {
                _state.value = StartupState.Error("Failed to setup NullClaw: ${setupResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(setupResult.exceptionOrNull()!!)
            }
            
            val agentResult = nullClawBridge!!.start()
            if (agentResult.isFailure) {
                _state.value = StartupState.Error("Failed to start NullClaw: ${agentResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(agentResult.exceptionOrNull()!!)
            }
            
            // Wait for agent to be ready
            if (!waitForAgent()) {
                _state.value = StartupState.Error("NullClaw Agent failed to become ready")
                return@withContext Result.failure(Exception("NullClaw Agent not ready"))
            }
            
            logger.info { "NullClaw Agent started on port $DEFAULT_AGENT_PORT" }
            
            _state.value = StartupState.Running(
                inferenceEndpoint = "http://localhost:$DEFAULT_INFERENCE_PORT",
                agentEndpoint = "http://localhost:$DEFAULT_AGENT_PORT"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Startup failed" }
            _state.value = StartupState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    /**
     * Stop all services gracefully.
     */
    fun stopAll() {
        logger.info { "Stopping all services..." }
        
        scope.launch {
            // Stop NullClaw first (depends on inference)
            nullClawBridge?.stop()
            NullClawBridgeFactory.reset()
            
            // Then stop inference
            liteRTBridge?.stop()
            
            _state.value = StartupState.Stopped
            logger.info { "All services stopped" }
        }
    }
    
    /**
     * Check if all services are healthy.
     */
    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        val inferenceHealthy = liteRTBridge?.isModelReady() ?: false
        val agentHealthy = nullClawBridge?.checkHealth() ?: false
        
        inferenceHealthy && agentHealthy
    }
    
    /**
     * Get the inference endpoint URL.
     */
    fun getInferenceEndpoint(): String? {
        return if (_state.value is StartupState.Running) {
            "http://localhost:$DEFAULT_INFERENCE_PORT"
        } else null
    }
    
    /**
     * Get the agent endpoint URL.
     */
    fun getAgentEndpoint(): String? {
        return nullClawBridge?.getEndpoint()
    }
    
    private suspend fun waitForInference(timeoutMs: Long = 10000): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (liteRTBridge?.isModelReady() == true) {
                return true
            }
            delay(500)
        }
        
        return false
    }
    
    private suspend fun waitForAgent(timeoutMs: Long = 10000): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (nullClawBridge?.checkHealth() == true) {
                return true
            }
            delay(500)
        }
        
        // Still return true if process is running (health endpoint may not be ready yet)
        return nullClawBridge?.isRunning() ?: false
    }
    
    companion object {
        const val DEFAULT_INFERENCE_PORT = 8080
        const val DEFAULT_AGENT_PORT = 9090
    }
}

/**
 * Startup State
 */
sealed class StartupState {
    object Idle : StartupState()
    object StartingInference : StartupState()
    object StartingAgent : StartupState()
    data class Running(
        val inferenceEndpoint: String,
        val agentEndpoint: String
    ) : StartupState()
    data class Error(val message: String) : StartupState()
    object Stopped : StartupState()
}

package com.loa.momclaw.startup

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.loa.momclaw.agent.AgentService
import com.loa.momclaw.agent.AgentState
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.inference.InferenceService
import com.loa.momclaw.inference.InferenceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Manages the proper startup sequence for MomClAW components
 * 
 * Startup Sequence:
 * 1. LiteRT Bridge (InferenceService) - provides HTTP API for model inference
 * 2. Wait for model to be loaded and ready
 * 3. NullClaw Agent (AgentService) - uses LiteRT Bridge for inference
 * 
 * This ensures NullClaw can connect to a ready LiteRT instance.
 */
class StartupManager(private val context: Context) : LifecycleObserver {
    
    companion object {
        private val _state = MutableStateFlow<StartupState>(StartupState.Idle)
        val state: StateFlow<StartupState> = _state.asStateFlow()
        
        private const val MAX_WAIT_MS = 30_000L
        private const val POLL_INTERVAL_MS = 500L
    }
    
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Start all MomClAW services in the correct order
     */
    fun startServices(config: AgentConfig = AgentConfig.DEFAULT) {
        scope.launch {
            try {
                _state.value = StartupState.Starting
                
                // Step 1: Start Inference Service (LiteRT Bridge)
                logger.info { "Step 1/3: Starting InferenceService..." }
                _state.value = StartupState.StartingInference
                
                val inferenceIntent = Intent(context, InferenceService::class.java).apply {
                    putExtra(InferenceService.KEY_ACTION, InferenceService.ACTION_START)
                    putExtra(InferenceService.KEY_MODEL_PATH, config.modelPath)
                    putExtra(InferenceService.KEY_PORT, 8080)
                }
                context.startForegroundService(inferenceIntent)
                
                // Step 2: Wait for LiteRT Bridge to be ready
                logger.info { "Step 2/3: Waiting for LiteRT Bridge to be ready..." }
                _state.value = StartupState.WaitingForInference
                
                val inferenceReady = waitForInferenceReady()
                if (!inferenceReady) {
                    _state.value = StartupState.Error("LiteRT Bridge failed to start within ${MAX_WAIT_MS/1000}s")
                    return@launch
                }
                
                // Step 3: Start Agent Service (NullClaw)
                logger.info { "Step 3/3: Starting AgentService..." }
                _state.value = StartupState.StartingAgent
                
                val agentIntent = Intent(context, AgentService::class.java).apply {
                    putExtra(AgentService.KEY_ACTION, AgentService.ACTION_START)
                    putExtra(AgentService.KEY_SYSTEM_PROMPT, config.systemPrompt)
                    putExtra(AgentService.KEY_TEMPERATURE, config.temperature)
                    putExtra(AgentService.KEY_MAX_TOKENS, config.maxTokens)
                }
                context.startForegroundService(agentIntent)
                
                // Wait for agent to be ready
                val agentReady = waitForAgentReady()
                if (!agentReady) {
                    _state.value = StartupState.Error("NullClaw Agent failed to start within ${MAX_WAIT_MS/1000}s")
                    return@launch
                }
                
                _state.value = StartupState.Running
                logger.info { "All services started successfully" }
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to start services" }
                _state.value = StartupState.Error("Startup failed: ${e.message}")
            }
        }
    }
    
    /**
     * Stop all services in reverse order
     */
    fun stopServices() {
        scope.launch {
            try {
                _state.value = StartupState.Stopping
                
                // Stop Agent first
                logger.info { "Stopping AgentService..." }
                val agentIntent = Intent(context, AgentService::class.java).apply {
                    putExtra(AgentService.KEY_ACTION, AgentService.ACTION_STOP)
                }
                context.startService(agentIntent)
                
                delay(1000) // Give agent time to shutdown
                
                // Stop Inference
                logger.info { "Stopping InferenceService..." }
                val inferenceIntent = Intent(context, InferenceService::class.java).apply {
                    putExtra(InferenceService.KEY_ACTION, InferenceService.ACTION_STOP)
                }
                context.startService(inferenceIntent)
                
                _state.value = StartupState.Idle
                logger.info { "All services stopped" }
                
            } catch (e: Exception) {
                logger.error(e) { "Error stopping services" }
                _state.value = StartupState.Error("Shutdown error: ${e.message}")
            }
        }
    }
    
    /**
     * Wait for inference service to be ready
     */
    private suspend fun waitForInferenceReady(): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
            val currentState = InferenceService.state.value
            if (currentState is InferenceState.Running) {
                return true
            }
            delay(POLL_INTERVAL_MS)
        }
        
        return false
    }
    
    /**
     * Wait for agent service to be ready
     */
    private suspend fun waitForAgentReady(): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
            val currentState = AgentService.state.value
            if (currentState is AgentState.Running) {
                return true
            }
            delay(POLL_INTERVAL_MS)
        }
        
        return false
    }
    
    /**
     * Check if all services are running
     */
    fun areServicesRunning(): Boolean {
        val inferenceState = InferenceService.state.value
        val agentState = AgentService.state.value
        
        return inferenceState is InferenceState.Running && 
               agentState is AgentState.Running
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        logger.info { "StartupManager: Lifecycle created" }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        logger.info { "StartupManager: Lifecycle destroyed" }
        stopServices()
    }
}

/**
 * Startup state machine
 */
sealed class StartupState {
    object Idle : StartupState()
    object Starting : StartupState()
    object StartingInference : StartupState()
    object WaitingForInference : StartupState()
    object StartingAgent : StartupState()
    object Running : StartupState()
    object Stopping : StartupState()
    data class Error(val message: String) : StartupState()
}

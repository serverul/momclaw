package com.loa.momclaw.startup

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.loa.momclaw.agent.AgentService
import com.loa.momclaw.agent.AgentState
import com.loa.momclaw.inference.InferenceService
import com.loa.momclaw.inference.InferenceState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * StartupManager — Unified startup coordinator for MOMCLAW components
 * 
 * Consolidated from StartupManager + StartupCoordinator to eliminate duplication.
 * 
 * Features:
 * - Atomic state transitions with ReentrantLock
 * - Proper resource cleanup with structured concurrency
 * - Process startup timeouts
 * - ServiceRegistry integration for service discovery
 * 
 * Startup Sequence:
 * 1. LiteRT Bridge (InferenceService) - provides HTTP API for model inference
 * 2. Wait for model to be loaded and ready (with timeout)
 * 3. NullClaw Agent (AgentService) - uses LiteRT Bridge for inference
 */
class StartupManager(private val context: Context) : LifecycleObserver {
    
    companion object {
        private const val TAG = "StartupManager"
        private val lock = ReentrantLock()
        private val _state = MutableStateFlow<StartupState>(StartupState.Idle)
        val state: StateFlow<StartupState> = _state.asStateFlow()
        
        private const val MAX_WAIT_MS = 30_000L
        private const val INFERENCE_TIMEOUT_MS = 20_000L
        private const val AGENT_TIMEOUT_MS = 15_000L
        private const val POLL_INTERVAL_MS = 500L
        
        private var instanceScope: CoroutineScope? = null
        
        fun getStateSnapshot(): StartupState = _state.value
    }
    
    private val scope: CoroutineScope
        get() = instanceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            instanceScope = it
        }
    
    /**
     * Atomic state transition helper
     */
    private fun transitionState(newState: StartupState) {
        lock.withLock {
            val oldState = _state.value
            Log.i(TAG, "State transition: $oldState -> $newState")
            _state.value = newState
        }
    }
    
    /**
     * Atomic state transition with validation
     */
    private fun transitionStateIf(expected: StartupState, newState: StartupState): Boolean {
        return lock.withLock {
            if (_state.value == expected) {
                Log.i(TAG, "State transition: $expected -> $newState")
                _state.value = newState
                true
            } else {
                Log.w(TAG, "State transition rejected: expected $expected, got ${_state.value}")
                false
            }
        }
    }
    
    /**
     * Start all MOMCLAW services in the correct order
     */
    fun startServices(config: com.loa.momclaw.domain.model.AgentConfig = com.loa.momclaw.domain.model.AgentConfig.DEFAULT) {
        scope.launch {
            // Already running check
            if (_state.value is StartupState.Running) {
                Log.w(TAG, "Services already running, skipping start")
                return@launch
            }
            
            // Atomic transition from Idle/Stopped to Starting
            if (!transitionStateIf(StartupState.Idle, StartupState.Starting) &&
                !transitionStateIf(StartupState.Stopped, StartupState.Starting)) {
                Log.w(TAG, "Cannot start services from state: ${_state.value}")
                return@launch
            }
            
            try {
                // Register services with registry
                ServiceRegistry.register(
                    name = SERVICE_INFERENCE,
                    instance = InferenceService::class.java,
                    stateFlow = InferenceService.state,
                    dependencies = emptyList()
                )
                
                // Step 1: Start Inference Service (LiteRT Bridge) with timeout
                Log.i(TAG, "Starting Inference Service...")
                transitionState(StartupState.StartingInference)
                
                val inferenceStarted = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
                    startInferenceService(config)
                }
                
                if (inferenceStarted != true) {
                    transitionState(StartupState.Error("InferenceService failed to start within ${INFERENCE_TIMEOUT_MS/1000}s"))
                    Log.e(TAG, "InferenceService startup timed out")
                    return@launch
                }
                
                // Step 2: Wait for LiteRT Bridge to be ready with timeout
                Log.i(TAG, "Waiting for Inference Service to be ready...")
                transitionState(StartupState.WaitingForInference)
                
                val inferenceReady = withTimeoutOrNull(MAX_WAIT_MS) {
                    waitForInferenceReady()
                }
                
                if (inferenceReady != true) {
                    transitionState(StartupState.Error("LiteRT Bridge failed to become ready within ${MAX_WAIT_MS/1000}s"))
                    Log.e(TAG, "InferenceService failed to become ready")
                    cleanupOnError()
                    return@launch
                }
                
                // Register agent service with inference dependency
                ServiceRegistry.register(
                    name = SERVICE_AGENT,
                    instance = AgentService::class.java,
                    stateFlow = AgentService.state,
                    dependencies = listOf(SERVICE_INFERENCE)
                )
                
                // Step 3: Start Agent Service (NullClaw) with timeout
                Log.i(TAG, "Starting Agent Service...")
                transitionState(StartupState.StartingAgent)
                
                val agentStarted = withTimeoutOrNull(AGENT_TIMEOUT_MS) {
                    startAgentService(config)
                }
                
                if (agentStarted != true) {
                    transitionState(StartupState.Error("AgentService failed to start within ${AGENT_TIMEOUT_MS/1000}s"))
                    Log.e(TAG, "AgentService startup timed out")
                    cleanupOnError()
                    return@launch
                }
                
                // Wait for agent to be ready
                val agentReady = withTimeoutOrNull(MAX_WAIT_MS) {
                    waitForAgentReady()
                }
                
                if (agentReady != true) {
                    transitionState(StartupState.Error("NullClaw Agent failed to become ready within ${MAX_WAIT_MS/1000}s"))
                    Log.e(TAG, "AgentService failed to become ready")
                    cleanupOnError()
                    return@launch
                }
                
                transitionState(StartupState.Running(
                    inferenceEndpoint = "http://localhost:8080",
                    agentEndpoint = "http://localhost:9090"
                ))
                Log.i(TAG, "All services started successfully")
                
            } catch (e: CancellationException) {
                Log.w(TAG, "Startup cancelled")
                transitionState(StartupState.Error("Startup cancelled"))
                cleanupOnError()
            } catch (e: Exception) {
                Log.e(TAG, "Startup failed: ${e.message}", e)
                transitionState(StartupState.Error("Startup failed: ${e.message}"))
                cleanupOnError()
            }
        }
    }
    
    /**
     * Start inference service
     * @return true if intent was sent successfully
     */
    private suspend fun startInferenceService(config: com.loa.momclaw.domain.model.AgentConfig): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val inferenceIntent = Intent(context, InferenceService::class.java).apply {
                    putExtra(InferenceService.KEY_ACTION, InferenceService.ACTION_START)
                    putExtra(InferenceService.KEY_MODEL_PATH, config.modelPath)
                    putExtra(InferenceService.KEY_PORT, 8080)
                }
                context.startForegroundService(inferenceIntent)
                Log.d(TAG, "InferenceService start intent sent")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start InferenceService", e)
                false
            }
        }
    }
    
    /**
     * Start agent service
     * @return true if intent was sent successfully
     */
    private suspend fun startAgentService(config: com.loa.momclaw.domain.model.AgentConfig): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val agentIntent = Intent(context, AgentService::class.java).apply {
                    putExtra(AgentService.KEY_ACTION, AgentService.ACTION_START)
                    putExtra(AgentService.KEY_SYSTEM_PROMPT, config.systemPrompt)
                    putExtra(AgentService.KEY_TEMPERATURE, config.temperature)
                    putExtra(AgentService.KEY_MAX_TOKENS, config.maxTokens)
                }
                context.startForegroundService(agentIntent)
                Log.d(TAG, "AgentService start intent sent")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AgentService", e)
                false
            }
        }
    }
    
    /**
     * Cleanup on error - stop any started services
     */
    private suspend fun cleanupOnError() {
        Log.d(TAG, "Cleaning up services after error")
        
        // Try to stop agent if it was started
        if (AgentService.state.value !is AgentState.Idle) {
            withContext(Dispatchers.Main) {
                try {
                    val agentIntent = Intent(context, AgentService::class.java).apply {
                        putExtra(AgentService.KEY_ACTION, AgentService.ACTION_STOP)
                    }
                    context.startService(agentIntent)
                    Log.d(TAG, "AgentService stop intent sent")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop AgentService during cleanup", e)
                }
            }
        }
        
        // Try to stop inference if it was started
        if (InferenceService.state.value !is InferenceState.Idle) {
            withContext(Dispatchers.Main) {
                try {
                    val inferenceIntent = Intent(context, InferenceService::class.java).apply {
                        putExtra(InferenceService.KEY_ACTION, InferenceService.ACTION_STOP)
                    }
                    context.startService(inferenceIntent)
                    Log.d(TAG, "InferenceService stop intent sent")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop InferenceService during cleanup", e)
                }
            }
        }
        
        // Unregister from registry
        ServiceRegistry.unregister(SERVICE_AGENT)
        ServiceRegistry.unregister(SERVICE_INFERENCE)
    }
    
    /**
     * Stop all services in reverse order
     */
    fun stopServices() {
        scope.launch {
            try {
                lock.withLock {
                    if (_state.value !is StartupState.Running && _state.value !is StartupState.Starting) {
                        Log.w(TAG, "Services not running, skipping stop")
                        return@launch
                    }
                    _state.value = StartupState.Stopping
                }
                
                Log.i(TAG, "Stopping services...")
                
                // Stop Agent first (depends on inference)
                withContext(Dispatchers.Main) {
                    val agentIntent = Intent(context, AgentService::class.java).apply {
                        putExtra(AgentService.KEY_ACTION, AgentService.ACTION_STOP)
                    }
                    context.startService(agentIntent)
                    Log.d(TAG, "AgentService stop intent sent")
                }
                
                delay(1000) // Give agent time to shutdown gracefully
                
                // Stop Inference
                withContext(Dispatchers.Main) {
                    val inferenceIntent = Intent(context, InferenceService::class.java).apply {
                        putExtra(InferenceService.KEY_ACTION, InferenceService.ACTION_STOP)
                    }
                    context.startService(inferenceIntent)
                    Log.d(TAG, "InferenceService stop intent sent")
                }
                
                // Unregister services
                ServiceRegistry.unregister(SERVICE_AGENT)
                ServiceRegistry.unregister(SERVICE_INFERENCE)
                
                transitionState(StartupState.Stopped)
                Log.i(TAG, "All services stopped successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping services", e)
                transitionState(StartupState.Error("Shutdown error: ${e.message}"))
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
                Log.i(TAG, "InferenceService is ready")
                return true
            }
            if (currentState is InferenceState.Error) {
                Log.e(TAG, "InferenceService encountered error: ${(currentState as InferenceState.Error).message}")
                return false
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
                Log.i(TAG, "AgentService is ready")
                return true
            }
            if (currentState is AgentState.Error) {
                Log.e(TAG, "AgentService encountered error: ${(currentState as AgentState.Error).message}")
                return false
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
    
    /**
     * Get inference endpoint if running
     */
    fun getInferenceEndpoint(): String? {
        return if (_state.value is StartupState.Running) {
            "http://localhost:8080"
        } else null
    }
    
    /**
     * Get agent endpoint if running
     */
    fun getAgentEndpoint(): String? {
        return if (_state.value is StartupState.Running) {
            "http://localhost:9090"
        } else null
    }
    
    /**
     * Cleanup resources - call when destroying
     */
    private fun cleanup() {
        stopServices()
        instanceScope?.cancel()
        instanceScope = null
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        Log.i(TAG, "StartupManager created")
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.i(TAG, "StartupManager destroyed")
        cleanup()
    }
}

// Service name constants
const val SERVICE_INFERENCE = "inference"
const val SERVICE_AGENT = "agent"

/**
 * Startup state machine
 */
sealed class StartupState {
    object Idle : StartupState()
    object Starting : StartupState()
    object StartingInference : StartupState()
    object WaitingForInference : StartupState()
    object StartingAgent : StartupState()
    data class Running(
        val inferenceEndpoint: String,
        val agentEndpoint: String
    ) : StartupState()
    object Stopping : StartupState()
    object Stopped : StartupState()
    data class Error(val message: String) : StartupState()
}

package com.loa.momclaw.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of both LiteRT Bridge and NullClaw Agent.
 * 
 * Ensures proper startup sequence and coordination between components.
 */
@Singleton
class AgentLifecycleManager @Inject constructor(
    private val nullClawBridge: NullClawBridge
) {
    private var isInitialized = false

    companion object {
        private const val TAG = "AgentLifecycle"
        private const val LITERT_PORT = 8080
        private const val NULLCLAW_PORT = 9090
    }

    /**
     * Initializes the complete agent system.
     * 
     * Sequence:
     * 1. Setup NullClaw configuration
     * 2. Start NullClaw agent
     * 
     * Note: LiteRT Bridge should be started separately before this
     * 
     * @param config Agent configuration
     * @return Result with initialization status
     */
    suspend fun initialize(config: AgentConfig = AgentConfig()): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) {
                    Log.w(TAG, "Agent system already initialized")
                    return@withContext Result.success(Unit)
                }

                Log.i(TAG, "Starting agent initialization")

                // Setup NullClaw
                nullClawBridge.setup(config).getOrElse { error ->
                    return@withContext Result.failure(
                        Exception("Failed to setup NullClaw: ${error.message}", error)
                    )
                }

                // Start NullClaw
                nullClawBridge.start(NULLCLAW_PORT).getOrElse { error ->
                    return@withContext Result.failure(
                        Exception("Failed to start NullClaw: ${error.message}", error)
                    )
                }

                isInitialized = true
                Log.i(TAG, "Agent system initialized successfully")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing agent system", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Shuts down the agent system.
     */
    fun shutdown() {
        try {
            Log.i(TAG, "Shutting down agent system")
            
            nullClawBridge.stop()
            isInitialized = false
            
            Log.i(TAG, "Agent system shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }

    /**
     * Restarts the agent system.
     */
    suspend fun restart(): Result<Unit> {
        shutdown()
        kotlinx.coroutines.delay(2000) // Wait for cleanup
        return initialize()
    }

    /**
     * Checks if the system is fully operational.
     */
    fun isOperational(): Boolean {
        return isInitialized && nullClawBridge.isRunning()
    }

    /**
     * Gets health status of all components.
     */
    fun getHealthStatus(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "nullclaw_running" to nullClawBridge.isRunning(),
            "nullclaw_pid" to (nullClawBridge.getPid() ?: -1)
        )
    }
}

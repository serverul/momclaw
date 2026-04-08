package com.loa.momclaw

import android.app.Application
import android.util.Log
import com.loa.momclaw.agent.model.AgentConfig
import com.loa.momclaw.agent.AgentLifecycleManager
import com.loa.momclaw.bridge.LiteRTBridge
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MomClaw Application class.
 * 
 * Initializes the agent system and manages app-wide dependencies.
 */
import java.io.File

@HiltAndroidApp
class MomClawApp : Application() {

    @Inject
    lateinit var liteRTBridge: LiteRTBridge

    @Inject
    lateinit var agentLifecycleManager: AgentLifecycleManager

    private val appScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "MomClawApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MomClaw application starting")

        // Start agent services in background
        appScope.launch {
            initializeAgentSystem()
        }
    }

    /**
     * Initializes the complete agent system.
     * 
     * Sequence:
     * 1. Start LiteRT Bridge (if model is available)
     * 2. Initialize NullClaw Agent
     */
    private suspend fun initializeAgentSystem() {
        try {
            Log.i(TAG, "Initializing agent system")

            // Check if model exists
            val modelPath = getModelPath()
            if (modelPath.isNullOrEmpty()) {
                Log.w(TAG, "No model found. Agent system will start in degraded mode.")
                return
            }

            // Start LiteRT Bridge
            liteRTBridge.start(modelPath, 8080)
                .onSuccess {
                    Log.i(TAG, "LiteRT Bridge started successfully")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to start LiteRT Bridge", error)
                    return
                }

            // Start NullClaw Agent
            agentLifecycleManager.initialize(AgentConfig())
                .onSuccess {
                    Log.i(TAG, "Agent system initialized successfully")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to initialize agent system", error)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing agent system", e)
        }
    }

    /**
     * Gets the path to the LiteRT model file.
     * 
     * @return Model path if exists, null otherwise
     */
    private fun getModelPath(): String? {
        // Check internal storage for downloaded model
        val modelFile = File(filesDir, "models/gemma-4-E4B-it-litertlm.litertlm")
        
        return if (modelFile.exists()) {
            modelFile.absolutePath
        } else {
            // Check external storage (if app has permission)
            val externalModel = File(getExternalFilesDir(null), "models/gemma-4-E4B-it-litertlm.litertlm")
            if (externalModel.exists()) {
                externalModel.absolutePath
            } else {
                null
            }
        }
    }

    /**
     * Cleanup when app is terminated.
     */
    override fun onTerminate() {
        super.onTerminate()
        
        try {
            liteRTBridge.stop()
            agentLifecycleManager.shutdown()
            Log.i(TAG, "MomClaw application terminated")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

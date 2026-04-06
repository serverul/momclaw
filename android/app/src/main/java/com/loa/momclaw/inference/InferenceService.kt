package com.loa.momclaw.inference

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.loa.momclaw.MainActivity
import com.loa.momclaw.R
import com.loa.momclaw.bridge.LiteRTBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger

/**
 * InferenceService — Foreground service running LiteRT Bridge
 * 
 * Starts when: user opens chat, downloads a model, or manually enables inference.
 * Stops when: user stops it, app is killed, or model is unloaded.
 * 
 * IMPROVEMENTS:
 * - Atomic state transitions with ReentrantLock
 * - Process startup timeouts
 * - Proper resource cleanup for coroutines
 * - Structured concurrency
 * 
 * Responsibilities:
 * - Load/unload LiteRT models
 * - Start/stop the LiteRTBridge HTTP server (localhost:8080)
 * - Report inference status via StateFlow
 * - Maintain foreground notification so Android doesn't kill it
 */
class InferenceService : LifecycleService() {
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "momclaw_inference"
        const val CHANNEL_NAME = "MOMCLAW Inference"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_PORT = "port"
        const val KEY_ACTION = "action"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        
        // Timeout constants
        private const val STARTUP_TIMEOUT_MS = 20_000L
        private const val SHUTDOWN_TIMEOUT_MS = 5_000L
        
        private val stateLock = ReentrantLock()
        private val _state = MutableStateFlow<InferenceState>(InferenceState.Idle)
        val state: StateFlow<InferenceState> = _state.asStateFlow()
        
        fun getStateSnapshot(): InferenceState = stateLock.withLock { _state.value }
    }
    
    private var bridge: LiteRTBridge? = null
    private var inferenceScope: CoroutineScope? = null
    
    /**
     * Atomic state transition helper
     */
    private fun transitionState(newState: InferenceState) {
        stateLock.withLock {
            val oldState = _state.value
            logger.debug { "InferenceService state transition: $oldState -> $newState" }
            _state.value = newState
        }
    }
    
    /**
     * Atomic state transition with validation
     */
    private fun transitionStateIf(expected: InferenceState, newState: InferenceState): Boolean {
        return stateLock.withLock {
            if (_state.value == expected) {
                logger.debug { "InferenceService state transition: $expected -> $newState" }
                _state.value = newState
                true
            } else {
                logger.warn { "State transition rejected: expected $expected, got ${_state.value}" }
                false
            }
        }
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        inferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        logger.info { "InferenceService created" }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        logger.info { "InferenceService destroyed" }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val action = intent?.getStringExtra(KEY_ACTION) ?: ACTION_START
        
        when (action) {
            ACTION_START -> {
                val modelPath = intent?.getStringExtra(KEY_MODEL_PATH)
                    ?: "/data/data/com.loa.momclaw/files/models/model.litertlm"
                val port = intent?.getIntExtra(KEY_PORT, 8080) ?: 8080
                
                startForeground(NOTIFICATION_ID, buildNotification("Starting LiteRT Bridge..."))
                startInference(modelPath, port)
            }
            ACTION_STOP -> {
                stopInference()
            }
        }
        
        return START_STICKY
    }
    
    private fun startInference(modelPath: String, port: Int) {
        inferenceScope?.launch {
            // Already running check
            if (_state.value is InferenceState.Running) {
                logger.warn { "Inference already running" }
                return@launch
            }
            
            // Atomic transition
            if (!transitionStateIf(InferenceState.Idle, InferenceState.Loading(modelPath)) &&
                !(_state.value is InferenceState.Error)) {
                logger.error { "Cannot start inference from state: ${_state.value}" }
                return@launch
            }
            
            try {
                updateNotification("Loading model: ${File(modelPath).name}")
                
                bridge = LiteRTBridge(this@InferenceService, port)
                
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    transitionState(InferenceState.Error("Model file not found: $modelPath"))
                    updateNotification("Model not found")
                    cleanupOnError()
                    return@launch
                }
                
                // Start bridge with timeout
                val startResult = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                    try {
                        bridge?.start(modelPath)
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
                
                when {
                    startResult == null -> {
                        transitionState(InferenceState.Error("Startup timeout after ${STARTUP_TIMEOUT_MS/1000}s"))
                        updateNotification("Startup timeout")
                        cleanupOnError()
                    }
                    startResult.isFailure -> {
                        val error = startResult.exceptionOrNull()
                        transitionState(InferenceState.Error("Failed to load model: ${error?.message}"))
                        updateNotification("Failed to load model")
                        cleanupOnError()
                    }
                    else -> {
                        transitionState(InferenceState.Running(modelPath, port))
                        updateNotification("Running on localhost:$port")
                        logger.info { "InferenceService: LiteRT Bridge running on port $port" }
                    }
                }
                
            } catch (e: CancellationException) {
                logger.warn { "Inference startup cancelled" }
                transitionState(InferenceState.Error("Startup cancelled"))
                cleanupOnError()
            } catch (e: Exception) {
                transitionState(InferenceState.Error("Failed to start inference: ${e.message}"))
                updateNotification("Error: ${e.message}")
                logger.error(e) { "Failed to start InferenceService" }
                cleanupOnError()
            }
        }
    }
    
    private fun cleanupOnError() {
        try {
            bridge?.stop()
        } catch (e: Exception) {
            logger.warn { "Error during cleanup: ${e.message}" }
        }
        bridge = null
    }
    
    private fun stopInference() {
        inferenceScope?.launch {
            try {
                withTimeoutOrNull(SHUTDOWN_TIMEOUT_MS) {
                    bridge?.stop()
                }
                
                bridge = null
                transitionState(InferenceState.Idle)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                logger.error(e) { "Error stopping InferenceService" }
            }
        }
    }
    
    /**
     * Full cleanup - cancel all coroutines and release resources
     */
    private fun cleanup() {
        try {
            bridge?.stop()
        } catch (e: Exception) {
            logger.warn { "Error during cleanup: ${e.message}" }
        }
        bridge = null
        
        inferenceScope?.cancel()
        inferenceScope = null
    }
    
    private fun buildNotification(text: String): Notification {
        createNotificationChannel()
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MOMCLAW Inference")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "MOMCLAW LiteRT inference status"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

/**
 * InferenceService state machine
 */
sealed class InferenceState {
    object Idle : InferenceState()
    data class Loading(val modelPath: String) : InferenceState()
    data class Running(val modelPath: String, val port: Int) : InferenceState()
    object Stopping : InferenceState()
    data class Error(val message: String) : InferenceState()
}

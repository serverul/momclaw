package com.loa.momclaw.inference

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.loa.momclaw.MainActivity
import com.loa.momclaw.R
import com.loa.momclaw.bridge.LiteRTBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * InferenceService — Foreground service running LiteRT Bridge
 * 
 * Starts when: user opens chat, downloads a model, or manually enables inference.
 * Stops when: user stops it, app is killed, or model is unloaded.
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
        
        private val _state = MutableStateFlow(InferenceState.Idle)
        val state: StateFlow<InferenceState> = _state.asStateFlow()
    }
    
    private var bridge: LiteRTBridge? = null
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
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
        lifecycleScope.launch {
            try {
                _state.value = InferenceState.Loading(modelPath)
                updateNotification("Loading model: ${File(modelPath).name}")
                
                bridge = LiteRTBridge(this@InferenceService, port)
                
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    _state.value = InferenceState.Error("Model file not found: $modelPath")
                    updateNotification("Model not found")
                    return@launch
                }
                
                try {
                    bridge?.start(modelPath)
                    // start() throws on failure
                    _state.value = InferenceState.Running(modelPath, port)
                    updateNotification("Running on localhost:$port")
                    logger.info { "InferenceService: LiteRT Bridge running on port $port" }
                } catch (e: IllegalArgumentException) {
                    _state.value = InferenceState.Error("Failed to load model: ${e.message}")
                    updateNotification("Failed to load model")
                }
            } catch (e: Exception) {
                _state.value = InferenceState.Error("Failed to start inference: ${e.message}")
                updateNotification("Error: ${e.message}")
                logger.error(e) { "Failed to start InferenceService" }
            }
        }
    }
    
    private fun stopInference() {
        lifecycleScope.launch {
            try {
                bridge?.stop()
                bridge = null
                _state.value = InferenceState.Idle
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                logger.error(e) { "Error stopping InferenceService" }
            }
        }
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
    data class Error(val message: String) : InferenceState()
}
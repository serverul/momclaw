package com.loa.momclaw.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.loa.momclaw.MainActivity
import com.loa.momclaw.R
import com.loa.momclaw.agent.AgentConfig
import com.loa.momclaw.agent.AgentLifecycleManager
import com.loa.momclaw.bridge.LiteRTBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service that manages the agent lifecycle.
 * 
 * Keeps the agent system running even when the app is in the background.
 */
@AndroidEntryPoint
class AgentService : Service() {

    @Inject
    lateinit var liteRTBridge: LiteRTBridge

    @Inject
    lateinit var agentLifecycleManager: AgentLifecycleManager

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val binder = LocalBinder()

    companion object {
        private const val TAG = "AgentService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "agent_service_channel"
        private const val CHANNEL_NAME = "Agent Service"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AgentService = this@AgentService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Agent service created")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        startAgentSystem()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Agent service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Agent service destroyed")
        
        serviceScope.launch {
            stopAgentSystem()
        }
        
        serviceScope.cancel()
    }

    /**
     * Starts the agent system.
     */
    private fun startAgentSystem() {
        serviceScope.launch {
            try {
                val modelPath = getModelPath()
                if (modelPath.isNullOrEmpty()) {
                    Log.w(TAG, "No model found")
                    return@launch
                }

                // Start LiteRT Bridge
                liteRTBridge.start(modelPath, 8080)
                    .onSuccess {
                        Log.i(TAG, "LiteRT Bridge started in service")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to start LiteRT Bridge", error)
                    }

                // Start NullClaw Agent
                agentLifecycleManager.initialize(AgentConfig())
                    .onSuccess {
                        Log.i(TAG, "Agent system initialized in service")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to initialize agent system", error)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting agent system", e)
            }
        }
    }

    /**
     * Stops the agent system.
     */
    private suspend fun stopAgentSystem() {
        try {
            agentLifecycleManager.shutdown()
            liteRTBridge.stop()
            Log.i(TAG, "Agent system stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping agent system", e)
        }
    }

    /**
     * Gets the path to the LiteRT model file.
     */
    private fun getModelPath(): String? {
        val modelFile = getFileStreamPath("models/gemma-4-E4B-it-litertlm.litertlm")
        return if (modelFile.exists()) {
            modelFile.absolutePath
        } else {
            null
        }
    }

    /**
     * Creates the notification channel for the foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Agent service is running"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the notification for the foreground service.
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MomClaw Agent")
            .setContentText("Agent is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Checks if the agent is healthy.
     */
    fun isAgentHealthy(): Boolean {
        return agentLifecycleManager.isOperational()
    }

    /**
     * Restarts the agent system.
     */
    fun restartAgent() {
        serviceScope.launch {
            stopAgentSystem()
            delay(2000)
            startAgentSystem()
        }
    }
}

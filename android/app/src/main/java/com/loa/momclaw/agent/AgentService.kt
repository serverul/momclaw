package com.loa.momclaw.agent

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
package com.loa.momclaw.agent

import com.loa.momclaw.domain.model.AgentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import kotlin.math.min
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * AgentService — Foreground service managing the NullClaw agent process
 * 
 * Starts when: user opens app, agent is enabled in settings.
 * Stops when: user stops it, app is killed, or agent crashes.
 * 
 * Responsibilities:
 * - Extract and setup NullClaw binary from assets
 * - Start/stop the NullClaw agent process
 * - Monitor health of the agent process
 * - Auto-restart on crash (configurable)
 * - Report agent status via StateFlow
 */
class AgentService : LifecycleService() {
    
    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "momclaw_agent"
        const val CHANNEL_NAME = "MOMCLAW Agent"
        const val KEY_ACTION = "action"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_MAX_TOKENS = "max_tokens"
        
        private val _state = MutableStateFlow(AgentState.Idle)
        val state: StateFlow<AgentState> = _state.asStateFlow()
    }
    
    private var bridge: NullClawBridge? = null
    private var restartCount = 0
    private val maxRestarts = 3
    
    // Exponential backoff configuration
    private val initialDelayMs = 1000L
    private val maxDelayMs = 30000L
    private val backoffMultiplier = 2.0
    
    private fun calculateBackoffDelay(): Long {
        val delay = initialDelayMs * backoffMultiplier.pow(restartCount)
        return min(delay.toLong(), maxDelayMs)
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val action = intent?.getStringExtra(KEY_ACTION) ?: ACTION_START
        
        when (action) {
            ACTION_START -> {
                val systemPrompt = intent?.getStringExtra(KEY_SYSTEM_PROMPT)
                val temp = intent?.getFloatExtra(KEY_TEMPERATURE, 0.7f)
                val maxTokens = intent?.getIntExtra(KEY_MAX_TOKENS, 2048)
                
                startForeground(NOTIFICATION_ID, buildNotification("Starting NullClaw agent..."))
                startAgent(systemPrompt, temp, maxTokens)
            }
            ACTION_STOP -> {
                stopAgent()
            }
        }
        
        return START_STICKY
    }
    
    private fun startAgent(systemPrompt: String?, temperature: Float?, maxTokens: Int?) {
        lifecycleScope.launch {
            try {
                _state.value = AgentState.SettingUp
                
                bridge = NullClawBridge(this@AgentService)
                
                val config = AgentConfig(
                    systemPrompt = systemPrompt ?: "You are MomClaw, a helpful AI assistant running offline on this device.",
                    temperature = temperature ?: 0.7f,
                    maxTokens = maxTokens ?: 2048,
                    modelPrimary = "litert-bridge/gemma-4e4b",
                    baseUrl = "http://localhost:8080",
                    memoryBackend = "sqlite",
                    memoryPath = "/data/data/com.loa.momclaw/databases/agent.db"
                )
                
                updateNotification("Extracting binary...")
                val setupResult = withContext(Dispatchers.Default) {
                    bridge?.setup(config)
                }
                
                if (setupResult?.isFailure == true) {
                    _state.value = AgentState.Error("Setup failed: ${setupResult.exceptionOrNull()?.message}")
                    updateNotification("Setup failed")
                    return@launch
                }
                
                _state.value = AgentState.Starting
                
                updateNotification("Starting agent process...")
                val startResult = withContext(Dispatchers.Default) {
                    bridge?.start()
                }
                
                if (startResult?.isSuccess == true) {
                    _state.value = AgentState.Running
                    updateNotification("Agent running (PID: ${bridge?.getPid()})")
                    logger.info { "AgentService: NullClaw started successfully" }
                    restartCount = 0
                    startHealthMonitor()
                } else {
                    _state.value = AgentState.Error("Start failed: ${startResult?.exceptionOrNull()?.message}")
                    updateNotification("Agent failed to start")
                }
            } catch (e: Exception) {
                _state.value = AgentState.Error("Failed to start agent: ${e.message}")
                updateNotification("Error: ${e.message}")
                logger.error(e) { "Failed to start AgentService" }
            }
        }
    }
    
    /**
     * Health monitor with exponential backoff retry
     */
    private fun startHealthMonitor() {
        lifecycleScope.launch {
            while (_state.value is AgentState.Running) {
                delay(5000) // Check every 5 seconds
                
                val isRunning = bridge?.isRunning() ?: false
                if (!isRunning) {
                    if (restartCount < maxRestarts) {
                        restartCount++
                        val delayMs = calculateBackoffDelay()
                        
                        logger.warn { "Agent died, restarting in ${delayMs}ms (${restartCount}/${maxRestarts})..." }
                        _state.value = AgentState.Restarting(restartCount, maxRestarts)
                        updateNotification("Restarting agent in ${delayMs/1000}s (${restartCount}/$maxRestarts)...")
                        
                        // Exponential backoff delay
                        delay(delayMs)
                        
                        val startResult = withContext(Dispatchers.Default) {
                            bridge?.start()
                        }
                        
                        if (startResult?.isSuccess != true) {
                            _state.value = AgentState.Error("Agent crashed after $restartCount restarts")
                            updateNotification("Agent crashed")
                        } else {
                            // Reset backoff on successful restart
                            _state.value = AgentState.Running
                            updateNotification("Agent running (PID: ${bridge?.getPid()})")
                        }
                    } else {
                        _state.value = AgentState.Error("Agent crashed after $maxRestarts restart attempts")
                        updateNotification("Agent crashed permanently")
                        break
                    }
                }
            }
        }
    }
    
    private fun stopAgent() {
        lifecycleScope.launch {
            try {
                bridge?.stop()
                bridge = null
                _state.value = AgentState.Idle
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                logger.error(e) { "Error stopping AgentService" }
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
            .setContentTitle("MOMCLAW Agent")
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
            description = "MOMCLAW agent status"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

/**
 * AgentService state machine
 */
sealed class AgentState {
    object Idle : AgentState()
    object SettingUp : AgentState()
    object Starting : AgentState()
    data class Restarting(val current: Int, val max: Int) : AgentState()
    object Running : AgentState()
    data class Error(val message: String) : AgentState()
}
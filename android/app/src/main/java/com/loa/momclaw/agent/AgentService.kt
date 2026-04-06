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
import com.loa.momclaw.domain.model.AgentConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.math.pow

private val logger = KotlinLogging.logger

/**
 * AgentService — Foreground service managing the NullClaw agent process
 * 
 * Starts when: user opens app, agent is enabled in settings.
 * Stops when: user stops it, app is killed, or agent crashes.
 * 
 * IMPROVEMENTS:
 * - Atomic state transitions with ReentrantLock
 * - Proper resource cleanup for coroutines
 * - Process startup timeouts
 * - Structured concurrency
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
        
        // Timeout constants
        private const val STARTUP_TIMEOUT_MS = 15_000L
        private const val SHUTDOWN_TIMEOUT_MS = 5_000L
        private const val HEALTH_CHECK_INTERVAL_MS = 5_000L
        
        private val stateLock = ReentrantLock()
        private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
        val state: StateFlow<AgentState> = _state.asStateFlow()
        
        fun getStateSnapshot(): AgentState = stateLock.withLock { _state.value }
    }
    
    private var bridge: NullClawBridge? = null
    private var restartCount = 0
    private val maxRestarts = 3
    
    // Managed coroutine scopes
    private var healthMonitorJob: Job? = null
    private var serviceScope: CoroutineScope? = null
    
    // Exponential backoff configuration
    private val initialDelayMs = 1000L
    private val maxDelayMs = 30000L
    private val backoffMultiplier = 2.0
    
    private fun calculateBackoffDelay(): Long {
        val baseDelay = initialDelayMs * backoffMultiplier.pow(restartCount)
        val delay = min(baseDelay.toLong(), maxDelayMs)
        // Add jitter (±10%) to prevent thundering herd
        val jitter = (Math.random() * 0.2 - 0.1) * delay
        return max(100L, (delay + jitter.toLong()))
    }
    
    /**
     * Atomic state transition helper
     */
    private fun transitionState(newState: AgentState) {
        stateLock.withLock {
            val oldState = _state.value
            logger.debug { "AgentService state transition: $oldState -> $newState" }
            _state.value = newState
        }
    }
    
    /**
     * Atomic state transition with validation
     */
    private fun transitionStateIf(expected: AgentState, newState: AgentState): Boolean {
        return stateLock.withLock {
            if (_state.value == expected) {
                logger.debug { "AgentService state transition: $expected -> $newState" }
                _state.value = newState
                true
            } else {
                logger.warn { "State transition rejected: expected $expected, got ${_state.value}" }
                false
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        logger.info { "AgentService created" }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        logger.info { "AgentService destroyed" }
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
        serviceScope?.launch {
            // Already running check
            if (_state.value is AgentState.Running) {
                logger.warn { "Agent already running" }
                return@launch
            }
            
            // Atomic transition
            if (!transitionStateIf(AgentState.Idle, AgentState.SettingUp) &&
                !(_state.value is AgentState.Error)) {
                logger.error { "Cannot start agent from state: ${_state.value}" }
                return@launch
            }
            
            try {
                bridge = NullClawBridge(this@AgentService)
                
                val config = AgentConfig(
                    systemPrompt = systemPrompt ?: "You are MOMCLAW, a helpful AI assistant running offline on this device.",
                    temperature = temperature ?: 0.7f,
                    maxTokens = maxTokens ?: 2048,
                    modelPrimary = "litert-bridge/gemma-4e4b",
                    baseUrl = "http://localhost:8080",
                    memoryBackend = "sqlite",
                    memoryPath = "/data/data/com.loa.momclaw/databases/agent.db"
                )
                
                updateNotification("Extracting binary...")
                
                // Setup with timeout
                val setupResult = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                    bridge?.setup(config)
                }
                
                if (setupResult == null) {
                    transitionState(AgentState.Error("Setup timeout after ${STARTUP_TIMEOUT_MS/1000}s"))
                    updateNotification("Setup timeout")
                    return@launch
                }
                
                if (setupResult.isFailure) {
                    transitionState(AgentState.Error("Setup failed: ${setupResult.exceptionOrNull()?.message}"))
                    updateNotification("Setup failed")
                    return@launch
                }
                
                transitionState(AgentState.Starting)
                updateNotification("Starting agent process...")
                
                // Start with timeout
                val startResult = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                    bridge?.start()
                }
                
                if (startResult == null) {
                    transitionState(AgentState.Error("Start timeout after ${STARTUP_TIMEOUT_MS/1000}s"))
                    updateNotification("Start timeout")
                    cleanupOnError()
                    return@launch
                }
                
                if (startResult.isSuccess) {
                    transitionState(AgentState.Running)
                    updateNotification("Agent running (PID: ${bridge?.getPid()})")
                    logger.info { "AgentService: NullClaw started successfully" }
                    restartCount = 0
                    startHealthMonitor()
                } else {
                    transitionState(AgentState.Error("Start failed: ${startResult.exceptionOrNull()?.message}"))
                    updateNotification("Agent failed to start")
                    cleanupOnError()
                }
            } catch (e: CancellationException) {
                logger.warn { "Agent startup cancelled" }
                transitionState(AgentState.Error("Startup cancelled"))
                cleanupOnError()
            } catch (e: Exception) {
                transitionState(AgentState.Error("Failed to start agent: ${e.message}"))
                updateNotification("Error: ${e.message}")
                logger.error(e) { "Failed to start AgentService" }
                cleanupOnError()
            }
        }
    }
    
    /**
     * Health monitor with exponential backoff retry
     */
    private fun startHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = serviceScope?.launch {
            while (_state.value is AgentState.Running) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                
                val isRunning = bridge?.isRunning() ?: false
                if (!isRunning) {
                    if (restartCount < maxRestarts) {
                        restartCount++
                        val delayMs = calculateBackoffDelay()
                        
                        logger.warn { "Agent died, restarting in ${delayMs}ms (${restartCount}/${maxRestarts})..." }
                        transitionState(AgentState.Restarting(restartCount, maxRestarts))
                        updateNotification("Restarting agent in ${delayMs/1000}s (${restartCount}/$maxRestarts)...")
                        
                        // Exponential backoff delay
                        delay(delayMs)
                        
                        val startResult = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                            bridge?.start()
                        }
                        
                        if (startResult?.isSuccess != true) {
                            transitionState(AgentState.Error("Agent crashed after $restartCount restarts"))
                            updateNotification("Agent crashed")
                            cleanupOnError()
                            break
                        } else {
                            // Reset backoff on successful restart
                            transitionState(AgentState.Running)
                            updateNotification("Agent running (PID: ${bridge?.getPid()})")
                        }
                    } else {
                        transitionState(AgentState.Error("Agent crashed after $maxRestarts restart attempts"))
                        updateNotification("Agent crashed permanently")
                        cleanupOnError()
                        break
                    }
                }
            }
        }
    }
    
    private fun cleanupOnError() {
        bridge?.cleanup()
        bridge = null
    }
    
    private fun stopAgent() {
        serviceScope?.launch {
            try {
                healthMonitorJob?.cancel()
                healthMonitorJob = null
                
                withTimeoutOrNull(SHUTDOWN_TIMEOUT_MS) {
                    bridge?.stop()
                }
                
                bridge?.cleanup()
                bridge = null
                restartCount = 0
                transitionState(AgentState.Idle)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                logger.error(e) { "Error stopping AgentService" }
            }
        }
    }
    
    /**
     * Full cleanup - cancel all coroutines and release resources
     */
    private fun cleanup() {
        healthMonitorJob?.cancel()
        healthMonitorJob = null
        
        bridge?.cleanup()
        bridge = null
        
        serviceScope?.cancel()
        serviceScope = null
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

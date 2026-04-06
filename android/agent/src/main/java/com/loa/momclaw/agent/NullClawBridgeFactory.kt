package com.loa.momclaw.agent

import android.content.Context
import com.loa.momclaw.agent.config.ConfigurationManager
import com.loa.momclaw.agent.model.AgentConfig
import com.loa.momclaw.agent.monitoring.AgentMonitor
import com.loa.momclaw.agent.monitoring.ProcessLifecycleListener
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger

/**
 * Singleton factory for managing NullClaw Bridge instances.
 * 
 * Features:
 * - Thread-safe singleton pattern
 * - Proper resource cleanup on reset
 * - Atomic state transitions
 * - Lifecycle management
 * - Configuration management
 * - Health monitoring integration
 */
object NullClawBridgeFactory {
    
    @Volatile
    private var instance: NullClawBridge? = null
    
    private val mutex = Mutex()
    private val stateLock = ReentrantLock()
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var configManager: ConfigurationManager? = null
    
    @Volatile
    private var monitor: AgentMonitor? = null
    
    /**
     * Get or create the NullClaw Bridge instance.
     * Thread-safe singleton pattern.
     */
    suspend fun getInstance(context: Context): NullClawBridge {
        return instance ?: mutex.withLock {
            instance ?: NullClawBridge(context.applicationContext).also {
                instance = it
                isInitialized = true
                configManager = ConfigurationManager(context.applicationContext)
                monitor = AgentMonitor(context.applicationContext)
                logger.info { "NullClawBridge instance created" }
            }
        }
    }
    
    /**
     * Get existing instance or null if not created.
     */
    fun getInstanceOrNull(): NullClawBridge? = stateLock.withLock { instance }
    
    /**
     * Reset the factory (for testing or cleanup).
     * Stops any running instance and clears the singleton.
     */
    suspend fun reset() {
        mutex.withLock {
            instance?.let { bridge ->
                logger.info { "Stopping NullClawBridge instance..." }
                bridge.cleanup()  // Use full cleanup instead of just stop()
            }
            instance = null
            isInitialized = false
            configManager = null
            monitor = null
            logger.info { "NullClawBridgeFactory reset" }
        }
    }
    
    /**
     * Check if a bridge instance exists and is running.
     */
    fun isRunning(): Boolean {
        return stateLock.withLock {
            instance?.isRunning() ?: false
        }
    }
    
    /**
     * Check if factory has been initialized.
     */
    fun isInitialized(): Boolean = stateLock.withLock { isInitialized }
    
    /**
     * Stop the current instance without clearing it.
     */
    fun stopInstance() {
        stateLock.withLock {
            instance?.stop()
        }
    }
    
    /**
     * Perform full cleanup of the current instance.
     */
    fun cleanupInstance() {
        stateLock.withLock {
            instance?.cleanup()
            instance = null
            isInitialized = false
        }
    }
    
    /**
     * Get health status of the running agent.
     */
    suspend fun getHealthStatus(): AgentMonitor.AgentHealth? {
        val bridge = instance ?: return null
        return try {
            bridge.getHealthStatus()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get health status" }
            null
        }
    }
    
    /**
     * Get diagnostics information.
     */
    fun getDiagnostics(): AgentMonitor.Diagnostics? {
        return monitor?.getDiagnostics()
    }
    
    /**
     * Get configuration manager.
     */
    fun getConfigurationManager(): ConfigurationManager? = configManager
    
    /**
     * Update configuration.
     */
    suspend fun updateConfiguration(
        systemPrompt: String? = null,
        temperature: Float? = null,
        maxTokens: Int? = null
    ): Result<AgentConfig> {
        val manager = configManager ?: return Result.failure(
            IllegalStateException("Configuration manager not initialized")
        )
        
        return try {
            val config = manager.updateConfig(
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = maxTokens
            )
            Result.success(config)
        } catch (e: Exception) {
            logger.error(e) { "Failed to update configuration" }
            Result.failure(e)
        }
    }
    
    /**
     * Add lifecycle listener to the bridge.
     */
    fun addLifecycleListener(listener: ProcessLifecycleListener) {
        instance?.addLifecycleListener(listener)
    }
    
    /**
     * Remove lifecycle listener from the bridge.
     */
    fun removeLifecycleListener(listener: ProcessLifecycleListener) {
        instance?.removeLifecycleListener(listener)
    }
}

/**
 * Dependency Injection module for NullClaw Bridge.
 * Use with Hilt or manual DI.
 */
object NullClawBridgeModule {
    
    /**
     * Provide NullClaw Bridge instance.
     */
    fun provideNullClawBridge(context: Context): NullClawBridge {
        return NullClawBridge(context.applicationContext)
    }
    
    /**
     * Provide ConfigGenerator instance.
     */
    fun provideConfigGenerator(): ConfigGenerator = ConfigGenerator
    
    /**
     * Provide default AgentConfig.
     */
    fun provideDefaultAgentConfig(): AgentConfig = AgentConfig.DEFAULT
    
    /**
     * Provide ConfigurationManager.
     */
    fun provideConfigurationManager(context: Context): ConfigurationManager {
        return ConfigurationManager(context.applicationContext)
    }
    
    /**
     * Provide AgentMonitor.
     */
    fun provideAgentMonitor(context: Context): AgentMonitor {
        return AgentMonitor(context.applicationContext)
    }
}

/**
 * Extension to create pre-configured bridge.
 */
suspend fun NullClawBridge.setupWithDefaults(
    systemPrompt: String? = null,
    temperature: Float? = null,
    maxTokens: Int? = null
): Result<Unit> {
    val config = AgentConfig(
        systemPrompt = systemPrompt ?: AgentConfig.DEFAULT.systemPrompt,
        temperature = temperature ?: AgentConfig.DEFAULT.temperature,
        maxTokens = maxTokens ?: AgentConfig.DEFAULT.maxTokens
    )
    return setup(config)
}

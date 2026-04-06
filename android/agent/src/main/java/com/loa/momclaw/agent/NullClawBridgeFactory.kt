package com.loa.momclaw.agent

import android.content.Context
import com.loa.momclaw.agent.model.AgentConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger {}

/**
 * Singleton factory for managing NullClaw Bridge instances.
 * 
 * Features:
 * - Thread-safe singleton pattern
 * - Proper resource cleanup on reset
 * - Atomic state transitions
 * - Lifecycle management
 */
object NullClawBridgeFactory {
    
    @Volatile
    private var instance: NullClawBridge? = null
    
    private val mutex = Mutex()
    private val stateLock = ReentrantLock()
    
    @Volatile
    private var isInitialized = false
    
    /**
     * Get or create the NullClaw Bridge instance.
     * Thread-safe singleton pattern.
     */
    suspend fun getInstance(context: Context): NullClawBridge {
        return instance ?: mutex.withLock {
            instance ?: NullClawBridge(context.applicationContext).also {
                instance = it
                isInitialized = true
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
    fun provideConfigGenerator(): ConfigGenerator.Type = ConfigGenerator
    
    /**
     * Provide default AgentConfig.
     */
    fun provideDefaultAgentConfig(): AgentConfig = AgentConfig.DEFAULT
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

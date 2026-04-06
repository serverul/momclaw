package com.loa.momclaw.agent

import android.content.Context
import com.loa.momclaw.agent.model.AgentConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Singleton factory for managing NullClaw Bridge instances.
 * 
 * Ensures only one bridge instance exists at a time,
 * handles lazy initialization, and provides thread-safe access.
 */
object NullClawBridgeFactory {
    
    @Volatile
    private var instance: NullClawBridge? = null
    
    private val mutex = Mutex()
    
    /**
     * Get or create the NullClaw Bridge instance.
     * Thread-safe singleton pattern.
     */
    suspend fun getInstance(context: Context): NullClawBridge {
        return instance ?: mutex.withLock {
            instance ?: NullClawBridge(context.applicationContext).also {
                instance = it
                logger.info { "NullClawBridge instance created" }
            }
        }
    }
    
    /**
     * Get existing instance or null if not created.
     */
    fun getInstanceOrNull(): NullClawBridge? = instance
    
    /**
     * Reset the factory (for testing).
     * Stops any running instance and clears the singleton.
     */
    suspend fun reset() {
        mutex.withLock {
            instance?.stop()
            instance = null
            logger.info { "NullClawBridgeFactory reset" }
        }
    }
    
    /**
     * Check if a bridge instance exists and is running.
     */
    fun isRunning(): Boolean {
        return instance?.isRunning() ?: false
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

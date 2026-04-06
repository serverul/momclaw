package com.loa.momclaw.agent.service

/**
 * Service Interface for Agent Operations
 * 
 * Defines the contract for agent service implementations.
 * Used for dependency injection and testing.
 */
interface AgentServiceInterface {
    
    /**
     * Current state of the agent service
     */
    val state: AgentServiceState
    
    /**
     * Start the agent with the given configuration
     */
    suspend fun start(config: AgentConfig): Result<Unit>
    
    /**
     * Stop the agent
     */
    suspend fun stop()
    
    /**
     * Check if agent is healthy and responsive
     */
    suspend fun isHealthy(): Boolean
    
    /**
     * Get the HTTP endpoint for the agent
     */
    fun getEndpoint(): String
}

/**
 * Agent Service State
 */
sealed class AgentServiceState {
    object Idle : AgentServiceState()
    object SettingUp : AgentServiceState()
    object Starting : AgentServiceState()
    object Running : AgentServiceState()
    data class Restarting(val attempt: Int, val maxAttempts: Int) : AgentServiceState()
    data class Error(val message: String, val throwable: Throwable? = null) : AgentServiceState()
    object Stopped : AgentServiceState()
}

/**
 * Agent Configuration for Service Layer
 */
data class AgentConfig(
    val systemPrompt: String = "You are a helpful AI assistant running on-device.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "litert-bridge/gemma-4e4b",
    val baseUrl: String = "http://localhost:8080",
    val memoryBackend: String = "sqlite",
    val memoryPath: String = "",
    val autoRestart: Boolean = true,
    val maxRestartAttempts: Int = 3
)

package com.loa.momclaw.agent.service

import com.loa.momclaw.domain.model.AgentConfig as DomainAgentConfig

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
    suspend fun start(config: DomainAgentConfig): Result<Unit>
    
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
 * Extended configuration with service-specific options.
 */
data class AgentServiceConfig(
    val agentConfig: DomainAgentConfig = DomainAgentConfig.DEFAULT,
    val autoRestart: Boolean = true,
    val maxRestartAttempts: Int = 3
) {
    companion object {
        val DEFAULT = AgentServiceConfig()
    }
}

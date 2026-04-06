package com.loa.momclaw.agent.model

/**
 * Type alias for backward compatibility.
 * 
 * DEPRECATED: Import AgentConfig directly from com.loa.momclaw.domain.model
 * This alias will be removed in a future version.
 * 
 * The canonical AgentConfig is in the app module's domain/model package.
 */
@Deprecated(
    message = "Import AgentConfig from com.loa.momclaw.domain.model instead",
    replaceWith = ReplaceWith("com.loa.momclaw.domain.model.AgentConfig")
)
typealias AgentConfig = com.loa.momclaw.domain.model.AgentConfig

/**
 * Extension function to convert domain AgentConfig to agent module format.
 * Provided for migration compatibility.
 */
@Deprecated(
    message = "No longer needed - use domain AgentConfig directly",
    level = DeprecationLevel.WARNING
)
fun com.loa.momclaw.domain.model.AgentConfig.toAgentModel(): com.loa.momclaw.domain.model.AgentConfig = this

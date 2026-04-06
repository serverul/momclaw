package com.loa.momclaw.domain.model

/**
 * Typealias to the canonical AgentConfig in the agent module.
 * 
 * The agent module owns the AgentConfig definition.
 * App module re-exports it here for convenient access from domain layer.
 */
typealias AgentConfig = com.loa.momclaw.agent.model.AgentConfig

/**
 * Convenience access to AgentConfig.DEFAULT via domain namespace.
 */
val AgentConfigDefault = com.loa.momclaw.agent.model.AgentConfig.DEFAULT

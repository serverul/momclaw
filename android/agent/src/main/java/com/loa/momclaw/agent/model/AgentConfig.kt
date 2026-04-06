package com.loa.momclaw.agent.model

/**
 * Agent configuration model.
 * 
 * Single source of truth for agent configuration.
 * The app module's domain.model.AgentConfig should import from here
 * or use a typealias to avoid duplication.
 */
data class AgentConfig(
    val systemPrompt: String = "You are a helpful AI assistant running on-device. You are concise, helpful, and prioritize the user's needs.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "litert-bridge/gemma-4e4b",
    val modelPath: String = "/data/data/com.loa.momclaw/files/models/gemma-3-E4B-it.litertlm",
    val baseUrl: String = "http://localhost:8080",
    val memoryBackend: String = "sqlite",
    val memoryPath: String = "/data/data/com.loa.momclaw/databases/agent.db"
) {
    companion object {
        val DEFAULT = AgentConfig()
    }
}

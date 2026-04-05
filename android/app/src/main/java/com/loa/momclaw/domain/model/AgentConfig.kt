package com.loa.momclaw.domain.model

/**
 * Domain model for agent configuration
 * Used across the app for settings and agent communication
 */
data class AgentConfig(
    val systemPrompt: String = "You are a helpful AI assistant running on-device. You are concise, helpful, and prioritize the user's needs.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "litert-bridge/gemma-4e4b",
    val baseUrl: String = "http://localhost:8080",
    val memoryBackend: String = "sqlite",
    val memoryPath: String = "/data/data/com.loa.momclaw/databases/agent.db"
) {
    companion object {
        /**
         * Default configuration
         */
        val DEFAULT = AgentConfig()
    }
}

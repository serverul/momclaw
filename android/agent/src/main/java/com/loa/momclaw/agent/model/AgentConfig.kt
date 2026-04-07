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
    val modelPrimary: String = "litert-bridge/gemma-3-e4b",
    val modelPath: String = "/data/data/com.loa.momclaw/files/models/gemma-3-E4B-it.litertlm",
    val baseUrl: String = "http://localhost:8080",
    val memoryBackend: String = "sqlite",
    val memoryPath: String = "/data/data/com.loa.momclaw/databases/agent.db"
) {
    companion object {
        val DEFAULT = AgentConfig()
    }
    
    /**
     * Converts the agent config to NullClaw JSON configuration format.
     */
    fun toJson(): String {
        val config = mapOf(
            "agents" to mapOf(
                "defaults" to mapOf(
                    "model" to mapOf("primary" to modelPrimary),
                    "system_prompt" to systemPrompt
                )
            ),
            "models" to mapOf(
                "providers" to mapOf(
                    "litert-bridge" to mapOf(
                        "type" to "custom",
                        "base_url" to baseUrl
                    )
                )
            ),
            "memory" to mapOf(
                "backend" to memoryBackend,
                "path" to memoryPath
            ),
            "tools" to mapOf(
                "enabled" to listOf("shell", "file_read", "file_write")
            ),
            "channels" to mapOf(
                "cli" to mapOf("enabled" to false),
                "http" to mapOf(
                    "enabled" to true,
                    "port" to 9090
                )
            ),
            "inference" to mapOf(
                "temperature" to temperature,
                "max_tokens" to maxTokens
            )
        )

        return kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(config)
    }
}

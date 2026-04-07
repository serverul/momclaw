package com.loa.momclaw.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Configuration for the NullClaw agent.
 */
@Serializable
data class AgentConfig(
    val systemPrompt: String = "You are MOMCLAW, a helpful AI assistant running offline on this device.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val providerBaseUrl: String = "http://localhost:8080",
    val databasePath: String = "",
    val enabledTools: List<String> = listOf("shell", "file_read", "file_write")
) {
    /**
     * Converts the agent config to NullClaw JSON configuration format.
     */
    fun toJson(): String {
        val config = mapOf(
            "agents" to mapOf(
                "defaults" to mapOf(
                    "model" to mapOf("primary" to "litert-bridge/gemma-4e4b"),
                    "system_prompt" to systemPrompt
                )
            ),
            "models" to mapOf(
                "providers" to mapOf(
                    "litert-bridge" to mapOf(
                        "type" to "custom",
                        "base_url" to providerBaseUrl
                    )
                )
            ),
            "memory" to mapOf(
                "backend" to "sqlite",
                "path" to databasePath
            ),
            "tools" to mapOf(
                "enabled" to enabledTools
            ),
            "channels" to mapOf(
                "cli" to mapOf("enabled" to false),
                "http" to mapOf(
                    "enabled" to true,
                    "port" to 9090
                )
            )
        )

        return Json { prettyPrint = true }.encodeToString(config)
    }
}

/**
 * Agent configuration builder for fluent API.
 */
class AgentConfigBuilder {
    private var systemPrompt: String = "You are MOMCLAW, a helpful AI assistant running offline on this device."
    private var temperature: Float = 0.7f
    private var maxTokens: Int = 2048
    private var providerBaseUrl: String = "http://localhost:8080"
    private var databasePath: String = ""
    private var enabledTools: List<String> = listOf("shell", "file_read", "file_write")

    fun systemPrompt(prompt: String) = apply { this.systemPrompt = prompt }
    fun temperature(temp: Float) = apply { this.temperature = temp }
    fun maxTokens(tokens: Int) = apply { this.maxTokens = tokens }
    fun providerBaseUrl(url: String) = apply { this.providerBaseUrl = url }
    fun databasePath(path: String) = apply { this.databasePath = path }
    fun enabledTools(tools: List<String>) = apply { this.enabledTools = tools }

    fun build(): AgentConfig = AgentConfig(
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        providerBaseUrl = providerBaseUrl,
        databasePath = databasePath,
        enabledTools = enabledTools
    )
}

/**
 * Extension function to create a config builder.
 */
fun agentConfig(block: AgentConfigBuilder.() -> Unit): AgentConfig {
    return AgentConfigBuilder().apply(block).build()
}

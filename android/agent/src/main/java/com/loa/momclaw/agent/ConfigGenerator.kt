package com.loa.momclaw.agent

import com.loa.momclaw.agent.model.AgentConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Configuration Generator for NullClaw Agent
 * 
 * Generates JSON configuration files compatible with NullClaw's config format.
 * Supports all NullClaw configuration options including:
 * - Model providers (custom HTTP, OpenAI-compatible)
 * - Memory backends (SQLite)
 * - Tools (shell, file operations)
 * - Gateway settings (local binding, port)
 * - Inference parameters
 * - Channel configurations
 */
object ConfigGenerator {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Generate a complete NullClaw configuration from AgentConfig.
     */
    fun generate(config: AgentConfig): String {
        val nullClawConfig = NullClawConfig(
            agents = AgentsConfig(
                defaults = AgentDefaults(
                    model = ModelConfig(primary = config.modelPrimary),
                    systemPrompt = config.systemPrompt
                )
            ),
            models = ModelsConfig(
                providers = mapOf(
                    "litert-bridge" to ProviderConfig(
                        type = "custom",
                        baseUrl = config.baseUrl,
                        apiFormat = "openai"
                    )
                )
            ),
            memory = MemoryConfig(
                backend = config.memoryBackend,
                path = config.memoryPath
            ),
            tools = ToolsConfig(
                enabled = listOf("shell", "file_read", "file_write"),
                shell = ShellConfig(
                    allowedCommands = listOf("ls", "cat", "echo", "pwd", "date", "grep", "head", "tail"),
                    timeoutMs = 5000
                )
            ),
            gateway = GatewayConfig(
                mode = "local",
                bind = "loopback",
                port = 9090
            ),
            inference = InferenceConfig(
                temperature = config.temperature.toDouble(),
                maxTokens = config.maxTokens,
                topP = 0.95,
                topK = 40
            )
        )
        
        return json.encodeToString(nullClawConfig)
    }
    
    /**
     * Generate a minimal configuration for LiteRT-only mode (no tools).
     */
    fun generateMinimal(config: AgentConfig): String {
        val nullClawConfig = NullClawConfig(
            agents = AgentsConfig(
                defaults = AgentDefaults(
                    model = ModelConfig(primary = config.modelPrimary),
                    systemPrompt = config.systemPrompt
                )
            ),
            models = ModelsConfig(
                providers = mapOf(
                    "litert-bridge" to ProviderConfig(
                        type = "custom",
                        baseUrl = config.baseUrl,
                        apiFormat = "openai"
                    )
                )
            ),
            memory = null,  // No persistent memory in minimal mode
            tools = null,   // No tools in minimal mode
            gateway = GatewayConfig(
                mode = "local",
                bind = "loopback",
                port = 9090
            ),
            inference = InferenceConfig(
                temperature = config.temperature.toDouble(),
                maxTokens = config.maxTokens
            )
        )
        
        return json.encodeToString(nullClawConfig)
    }
    
    /**
     * Generate configuration with custom tools.
     */
    fun generateWithTools(
        config: AgentConfig,
        enabledTools: List<String>,
        allowedShellCommands: List<String> = emptyList()
    ): String {
        val nullClawConfig = NullClawConfig(
            agents = AgentsConfig(
                defaults = AgentDefaults(
                    model = ModelConfig(primary = config.modelPrimary),
                    systemPrompt = config.systemPrompt
                )
            ),
            models = ModelsConfig(
                providers = mapOf(
                    "litert-bridge" to ProviderConfig(
                        type = "custom",
                        baseUrl = config.baseUrl,
                        apiFormat = "openai"
                    )
                )
            ),
            memory = MemoryConfig(
                backend = config.memoryBackend,
                path = config.memoryPath
            ),
            tools = ToolsConfig(
                enabled = enabledTools,
                shell = if ("shell" in enabledTools) {
                    ShellConfig(
                        allowedCommands = allowedShellCommands.ifEmpty {
                            listOf("ls", "cat", "echo", "pwd", "date")
                        },
                        timeoutMs = 5000
                    )
                } else null
            ),
            gateway = GatewayConfig(
                mode = "local",
                bind = "loopback",
                port = 9090
            ),
            inference = InferenceConfig(
                temperature = config.temperature.toDouble(),
                maxTokens = config.maxTokens
            )
        )
        
        return json.encodeToString(nullClawConfig)
    }
    
    /**
     * Generate configuration for channel integration (Telegram, Discord).
     * Note: Channels are post-MVP feature.
     */
    fun generateWithChannels(
        config: AgentConfig,
        channels: Map<String, ChannelConfig>
    ): String {
        val baseConfig = generate(config)
        val baseObj = json.decodeFromString<NullClawConfig>(baseConfig)
        
        return json.encodeToString(baseObj.copy(
            channels = channels
        ))
    }
}

// ==================== Data Classes ====================

@Serializable
data class NullClawConfig(
    val agents: AgentsConfig,
    val models: ModelsConfig,
    val memory: MemoryConfig? = null,
    val tools: ToolsConfig? = null,
    val gateway: GatewayConfig = GatewayConfig(),
    val inference: InferenceConfig = InferenceConfig(),
    val channels: Map<String, ChannelConfig>? = null
)

@Serializable
data class AgentsConfig(
    val defaults: AgentDefaults
)

@Serializable
data class AgentDefaults(
    val model: ModelConfig,
    val systemPrompt: String
)

@Serializable
data class ModelConfig(
    val primary: String
)

@Serializable
data class ModelsConfig(
    val providers: Map<String, ProviderConfig>
)

@Serializable
data class ProviderConfig(
    val type: String,
    val baseUrl: String,
    val apiFormat: String = "openai"
)

@Serializable
data class MemoryConfig(
    val backend: String = "sqlite",
    val path: String
)

@Serializable
data class ToolsConfig(
    val enabled: List<String>,
    val shell: ShellConfig? = null
)

@Serializable
data class ShellConfig(
    val allowedCommands: List<String>,
    val timeoutMs: Int = 5000
)

@Serializable
data class GatewayConfig(
    val mode: String = "local",
    val bind: String = "loopback",
    val port: Int = 9090
)

@Serializable
data class InferenceConfig(
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val topP: Double = 0.95,
    val topK: Int = 40
)

@Serializable
data class ChannelConfig(
    val enabled: Boolean = true,
    val token: String? = null,
    val options: Map<String, String>? = null
)

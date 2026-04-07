package com.loa.momclaw.agent

/**
 * Agent Configuration
 * 
 * DEPRECATED: Use com.loa.momclaw.agent.model.AgentConfig instead.
 * This typealias is kept for backward compatibility.
 */
@Deprecated(
    message = "Use com.loa.momclaw.agent.model.AgentConfig instead",
    replaceWith = ReplaceWith(
        expression = "com.loa.momclaw.agent.model.AgentConfig",
        imports = ["com.loa.momclaw.agent.model.AgentConfig"]
    )
)
typealias AgentConfig = com.loa.momclaw.agent.model.AgentConfig

/**
 * Agent configuration builder for fluent API.
 * 
 * DEPRECATED: Use com.loa.momclaw.agent.model.AgentConfig directly.
 */
@Deprecated(
    message = "Use com.loa.momclaw.agent.model.AgentConfig directly"
)
class AgentConfigBuilder {
    private var systemPrompt: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.systemPrompt
    private var temperature: Float = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.temperature
    private var maxTokens: Int = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.maxTokens
    private var modelPrimary: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.modelPrimary
    private var modelPath: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.modelPath
    private var baseUrl: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.baseUrl
    private var memoryBackend: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.memoryBackend
    private var memoryPath: String = com.loa.momclaw.agent.model.AgentConfig.DEFAULT.memoryPath

    fun systemPrompt(prompt: String) = apply { this.systemPrompt = prompt }
    fun temperature(temp: Float) = apply { this.temperature = temp }
    fun maxTokens(tokens: Int) = apply { this.maxTokens = tokens }
    fun modelPrimary(model: String) = apply { this.modelPrimary = model }
    fun modelPath(path: String) = apply { this.modelPath = path }
    fun baseUrl(url: String) = apply { this.baseUrl = url }
    fun memoryBackend(backend: String) = apply { this.memoryBackend = backend }
    fun memoryPath(path: String) = apply { this.memoryPath = path }

    fun build(): com.loa.momclaw.agent.model.AgentConfig = com.loa.momclaw.agent.model.AgentConfig(
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        modelPrimary = modelPrimary,
        modelPath = modelPath,
        baseUrl = baseUrl,
        memoryBackend = memoryBackend,
        memoryPath = memoryPath
    )
}

/**
 * Extension function to create a config builder.
 * 
 * DEPRECATED: Use com.loa.momclaw.agent.model.AgentConfig directly.
 */
@Deprecated(
    message = "Use com.loa.momclaw.agent.model.AgentConfig directly"
)
fun agentConfig(block: AgentConfigBuilder.() -> Unit): com.loa.momclaw.agent.model.AgentConfig {
    return AgentConfigBuilder().apply(block).build()
}

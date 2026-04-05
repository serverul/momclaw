package com.loa.momclaw.agent

/**
 * Configuration data class for the NullClaw agent
 */
data class AgentConfig(
    val systemPrompt: String = "You are a helpful AI assistant running on-device.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "litert-bridge/gemma-4e4b",
    val baseUrl: String = "http://localhost:8080",
    val memoryBackend: String = "sqlite",
    val memoryPath: String = "/data/data/com.loa.momclaw/databases/agent.db"
) {
    /**
     * Generate JSON config for NullClaw binary
     */
    fun toJsonConfig(): String {
        return """
        {
          "agents": {
            "defaults": {
              "model": {
                "primary": "$modelPrimary"
              },
              "system_prompt": ${escapeJson(systemPrompt)},
              "temperature": $temperature,
              "max_tokens": $maxTokens
            }
          },
          "models": {
            "providers": {
              "litert-bridge": {
                "type": "custom",
                "base_url": "$baseUrl"
              }
            }
          },
          "memory": {
            "backend": "$memoryBackend",
            "path": "$memoryPath"
          }
        }
        """.trimIndent()
    }

    private fun escapeJson(str: String): String {
        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
}

package com.loa.momclaw.agent

import com.loa.momclaw.domain.model.AgentConfig
import org.json.JSONObject
import java.io.File

/**
 * Generates configuration files for NullClaw agent
 */
object ConfigGenerator {
    
    /**
     * Generate a config file from AgentConfig
     * @param config The agent configuration
     * @param outputFile The file to write the config to
     * @return Result containing the file path or error
     */
    fun generateConfigFile(config: AgentConfig, outputFile: File): Result<File> {
        return try {
            val jsonContent = buildJsonConfig(config)
            
            // Validate JSON is well-formed
            JSONObject(jsonContent) // This will throw if invalid
            
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(jsonContent)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(ConfigGenerationException("Failed to generate config: ${e.message}", e))
        }
    }
    
    /**
     * Build JSON config string from AgentConfig
     */
    private fun buildJsonConfig(config: AgentConfig): String {
        return """
        {
          "agents": {
            "defaults": {
              "model": {
                "primary": "${escapeJson(config.modelPrimary)}"
              },
              "system_prompt": ${escapeJson(config.systemPrompt)},
              "temperature": ${config.temperature},
              "max_tokens": ${config.maxTokens}
            }
          },
          "models": {
            "providers": {
              "litert-bridge": {
                "type": "custom",
                "base_url": "${escapeJson(config.baseUrl)}"
              }
            }
          },
          "memory": {
            "backend": "${escapeJson(config.memoryBackend)}",
            "path": "${escapeJson(config.memoryPath)}"
          }
        }
        """.trimIndent()
    }
    
    /**
     * Generate a minimal default config
     */
    fun generateDefaultConfig(outputFile: File): Result<File> {
        return generateConfigFile(AgentConfig.DEFAULT, outputFile)
    }
    
    /**
     * Parse existing config file to AgentConfig
     */
    fun parseConfigFile(configFile: File): Result<AgentConfig> {
        return try {
            val json = JSONObject(configFile.readText())
            val agents = json.getJSONObject("agents").getJSONObject("defaults")
            val model = agents.getJSONObject("model")
            val memory = json.getJSONObject("memory")
            val providers = json.getJSONObject("models").getJSONObject("providers")
                .getJSONObject("litert-bridge")
            
            Result.success(
                AgentConfig(
                    systemPrompt = agents.getString("system_prompt"),
                    temperature = agents.getDouble("temperature").toFloat(),
                    maxTokens = agents.getInt("max_tokens"),
                    modelPrimary = model.getString("primary"),
                    baseUrl = providers.getString("base_url"),
                    memoryBackend = memory.getString("backend"),
                    memoryPath = memory.getString("path")
                )
            )
        } catch (e: Exception) {
            Result.failure(ConfigGenerationException("Failed to parse config: ${e.message}", e))
        }
    }
    
    /**
     * Merge partial config updates with existing config
     */
    fun mergeConfig(
        existing: AgentConfig,
        systemPrompt: String? = null,
        temperature: Float? = null,
        maxTokens: Int? = null,
        modelPrimary: String? = null,
        baseUrl: String? = null
    ): AgentConfig {
        return existing.copy(
            systemPrompt = systemPrompt ?: existing.systemPrompt,
            temperature = temperature ?: existing.temperature,
            maxTokens = maxTokens ?: existing.maxTokens,
            modelPrimary = modelPrimary ?: existing.modelPrimary,
            baseUrl = baseUrl ?: existing.baseUrl
        )
    }
}

class ConfigGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Escape string for JSON
 */
private fun escapeJson(str: String): String {
    return "\"" + str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""
}

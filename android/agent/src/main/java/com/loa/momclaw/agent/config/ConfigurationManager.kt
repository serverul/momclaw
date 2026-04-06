package com.loa.momclaw.agent.config

import android.content.Context
import com.loa.momclaw.agent.model.AgentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Configuration Manager for NullClaw Agent
 * 
 * Handles:
 * - Loading/saving configuration from files
 * - Default configuration generation
 * - Configuration validation
 * - Environment-specific settings
 */
class ConfigurationManager(private val context: Context) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    
    private val configFile: File
        get() = File(context.filesDir, "config/agent-config.json")
    
    private val modelConfigFile: File
        get() = File(context.filesDir, "config/model-config.json")
    
    /**
     * Load configuration from file or create default
     */
    suspend fun loadConfig(): AgentConfig = withContext(Dispatchers.IO) {
        try {
            val file = configFile
            if (file.exists()) {
                logger.info { "Loading config from: ${file.absolutePath}" }
                val content = file.readText()
                json.decodeFromString<AgentConfig>(content)
            } else {
                logger.info { "Config file not found, creating default" }
                val default = AgentConfig.DEFAULT
                saveConfig(default)
                default
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load config, using defaults" }
            AgentConfig.DEFAULT
        }
    }
    
    /**
     * Save configuration to file
     */
    suspend fun saveConfig(config: AgentConfig) = withContext(Dispatchers.IO) {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(config))
            logger.info { "Config saved to: ${configFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save config" }
            throw ConfigException("Failed to save configuration", e)
        }
    }
    
    /**
     * Update specific config fields
     */
    suspend fun updateConfig(
        systemPrompt: String? = null,
        temperature: Float? = null,
        maxTokens: Int? = null,
        modelPrimary: String? = null,
        modelPath: String? = null,
        baseUrl: String? = null
    ): AgentConfig {
        val current = loadConfig()
        val updated = current.copy(
            systemPrompt = systemPrompt ?: current.systemPrompt,
            temperature = temperature ?: current.temperature,
            maxTokens = maxTokens ?: current.maxTokens,
            modelPrimary = modelPrimary ?: current.modelPrimary,
            modelPath = modelPath ?: current.modelPath,
            baseUrl = baseUrl ?: current.baseUrl
        )
        saveConfig(updated)
        return updated
    }
    
    /**
     * Validate configuration
     */
    fun validateConfig(config: AgentConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate temperature
        if (config.temperature < 0f || config.temperature > 2f) {
            errors.add("Temperature must be between 0 and 2, got ${config.temperature}")
        }
        
        // Validate maxTokens
        if (config.maxTokens < 1 || config.maxTokens > 8192) {
            errors.add("maxTokens must be between 1 and 8192, got ${config.maxTokens}")
        }
        
        // Validate model path exists
        if (!File(config.modelPath).exists()) {
            errors.add("Model path does not exist: ${config.modelPath}")
        }
        
        // Validate base URL
        if (!config.baseUrl.startsWith("http://") && !config.baseUrl.startsWith("https://")) {
            errors.add("Base URL must start with http:// or https://")
        }
        
        // Validate system prompt
        if (config.systemPrompt.isBlank()) {
            errors.add("System prompt cannot be empty")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Reset configuration to defaults
     */
    suspend fun resetToDefaults(): AgentConfig {
        val default = AgentConfig.DEFAULT
        saveConfig(default)
        return default
    }
    
    /**
     * Export configuration to JSON string
     */
    fun exportConfig(config: AgentConfig): String {
        return json.encodeToString(config)
    }
    
    /**
     * Import configuration from JSON string
     */
    suspend fun importConfig(jsonString: String): AgentConfig {
        return try {
            val config = json.decodeFromString<AgentConfig>(jsonString)
            val validation = validateConfig(config)
            if (!validation.isValid) {
                throw ConfigException("Invalid configuration: ${validation.errors.joinToString()}")
            }
            saveConfig(config)
            config
        } catch (e: Exception) {
            throw ConfigException("Failed to import configuration", e)
        }
    }
    
    /**
     * Get model-specific configuration
     */
    fun getModelConfig(modelPath: String): ModelConfig {
        val modelFile = File(modelPath)
        val modelName = modelFile.nameWithoutExtension
        
        return ModelConfig(
            name = modelName,
            path = modelPath,
            type = when {
                modelName.contains("gemma", ignoreCase = true) -> ModelType.GEMMA
                modelName.contains("llama", ignoreCase = true) -> ModelType.LLAMA
                else -> ModelType.UNKNOWN
            },
            contextLength = 8192, // Default for Gemma 4E4B
            quantization = if (modelPath.contains("int8")) Quantization.INT8 else Quantization.FP16
        )
    }
    
    /**
     * Generate NullClaw configuration JSON
     */
    fun generateNullClawConfig(config: AgentConfig): String {
        return """
        {
          "agents": {
            "defaults": {
              "model": {
                "primary": "${config.modelPrimary}"
              },
              "system_prompt": ${escapeJson(config.systemPrompt)},
              "thinking": "low"
            }
          },
          "models": {
            "providers": {
              "litert-bridge": {
                "type": "custom",
                "base_url": "${config.baseUrl}",
                "api_format": "openai",
                "timeout_ms": 60000
              }
            }
          },
          "memory": {
            "backend": "${config.memoryBackend}",
            "path": "${config.memoryPath}"
          },
          "tools": {
            "enabled": ["shell", "file_read", "file_write"],
            "shell": {
              "allowed_commands": ["ls", "cat", "echo", "pwd", "date", "grep"],
              "timeout_ms": 5000
            }
          },
          "gateway": {
            "mode": "local",
            "bind": "loopback",
            "port": 9090,
            "auth": "token"
          },
          "inference": {
            "temperature": ${config.temperature},
            "max_tokens": ${config.maxTokens},
            "top_p": 0.95,
            "top_k": 40
          },
          "logging": {
            "level": "info",
            "format": "json"
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
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}

data class ModelConfig(
    val name: String,
    val path: String,
    val type: ModelType,
    val contextLength: Int,
    val quantization: Quantization
)

enum class ModelType {
    GEMMA, LLAMA, UNKNOWN
}

enum class Quantization {
    FP16, INT8, INT4
}

class ConfigException(message: String, cause: Throwable? = null) : Exception(message, cause)

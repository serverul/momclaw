package com.loa.momclaw.bridge

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Model Fallback Manager
 * 
 * Provides graceful degradation when LiteRT models are not available:
 * - Primary: LiteRT on-device model (Gemma 4 E4B IT)
 * - Fallback 1: Simulated responses (echo mode)
 * - Fallback 2: Error responses with helpful guidance
 * 
 * This ensures the app remains functional even without model files,
 * allowing users to test the UI and integration while downloading models.
 */
class ModelFallbackManager(
    private val context: Context,
    private val engine: LlmEngineWrapper
) {
    
    private val modelLoader = ModelLoader(context)
    
    /**
     * Check model availability and return fallback status
     */
    suspend fun checkModelStatus(modelPath: String): ModelStatus = withContext(Dispatchers.IO) {
        // Check if model file exists
        val file = File(modelPath)
        if (!file.exists()) {
            return@withContext ModelStatus.NotFound(
                path = modelPath,
                suggestion = "Download model from HuggingFace: litert-community/gemma-4-E4B-it-litertlm"
            )
        }
        
        // Check file size (Gemma 4 E4B is ~3.9GB)
        val sizeGB = file.length() / (1024.0 * 1024.0 * 1024.0)
        if (sizeGB < 0.5) {
            return@withContext ModelStatus.Corrupted(
                path = modelPath,
                sizeGB = sizeGB,
                expectedGB = 3.5,
                suggestion = "Model file appears incomplete. Re-download from HuggingFace."
            )
        }
        
        // Verify model can be loaded
        val loadResult = modelLoader.verifyModel(modelPath)
        return@withContext when (loadResult) {
            is ModelLoader.LoadResult.Success -> ModelStatus.Available(
                path = modelPath,
                sizeGB = sizeGB,
                info = loadResult.info
            )
            is ModelLoader.LoadResult.Error -> ModelStatus.Invalid(
                path = modelPath,
                error = loadResult.message,
                suggestion = loadResult.cause?.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Attempt to load model with fallback to simulation mode
     */
    suspend fun loadWithFallback(
        modelPath: String,
        enableSimulation: Boolean = true
    ): LoadResult = withContext(Dispatchers.IO) {
        val status = checkModelStatus(modelPath)
        
        when (status) {
            is ModelStatus.Available -> {
                // Try to load real model
                val loaded = engine.loadModel(modelPath)
                if (loaded.isSuccess) {
                    LoadResult.Success(
                        mode = InferenceMode.LITERT,
                        modelName = status.info.name,
                        message = "Model loaded successfully"
                    )
                } else {
                    // Engine failed to load - use fallback if enabled
                    if (enableSimulation) {
                        LoadResult.Success(
                            mode = InferenceMode.SIMULATION,
                            modelName = "simulation-mode",
                            message = "LiteRT engine unavailable. Using simulation mode for testing."
                        )
                    } else {
                        LoadResult.Failure(
                            error = "Model engine failed to initialize",
                            suggestion = "Check device compatibility (requires ARM64, API 28+)"
                        )
                    }
                }
            }
            
            is ModelStatus.NotFound -> {
                if (enableSimulation) {
                    LoadResult.Success(
                        mode = InferenceMode.SIMULATION,
                        modelName = "simulation-mode",
                        message = "Model not found. Using simulation mode. ${status.suggestion}"
                    )
                } else {
                    LoadResult.Failure(
                        error = "Model file not found: ${status.path}",
                        suggestion = status.suggestion
                    )
                }
            }
            
            is ModelStatus.Corrupted -> {
                if (enableSimulation) {
                    LoadResult.Success(
                        mode = InferenceMode.SIMULATION,
                        modelName = "simulation-mode",
                        message = "Model file corrupted (${status.sizeGB}GB vs expected ${status.expectedGB}GB). ${status.suggestion}"
                    )
                } else {
                    LoadResult.Failure(
                        error = "Model file corrupted",
                        suggestion = status.suggestion
                    )
                }
            }
            
            is ModelStatus.Invalid -> {
                if (enableSimulation) {
                    LoadResult.Success(
                        mode = InferenceMode.SIMULATION,
                        modelName = "simulation-mode",
                        message = "Invalid model: ${status.error}. ${status.suggestion}"
                    )
                } else {
                    LoadResult.Failure(
                        error = status.error,
                        suggestion = status.suggestion
                    )
                }
            }
        }
    }
    
    /**
     * Generate response with fallback to simulation
     */
    suspend fun generateWithFallback(
        request: LiteRTRequest,
        mode: InferenceMode
    ): LiteRTResponseChunk {
        return when (mode) {
            InferenceMode.LITERT -> {
                try {
                    val result = engine.generateComplete(
                        prompt = request.prompt,
                        temperature = request.temperature,
                        maxTokens = request.maxTokens
                    )
                    result.fold(
                        onSuccess = { text -> LiteRTResponseChunk(text = text, isComplete = true, tokensGenerated = 0) },
                        onFailure = { e -> simulateResponse(request, "Error: ${e.message}") }
                    )
                } catch (e: Exception) {
                    // Fall back to simulation on error
                    simulateResponse(request, "Error: ${e.message}")
                }
            }
            InferenceMode.SIMULATION -> {
                simulateResponse(request, null)
            }
        }
    }
    
    /**
     * Generate streaming response with fallback to simulation
     */
    fun generateStreamingWithFallback(
        request: LiteRTRequest,
        mode: InferenceMode
    ) = kotlinx.coroutines.flow.flow {
        when (mode) {
            InferenceMode.LITERT -> {
                try {
                    engine.generate(
                        prompt = request.prompt,
                        temperature = request.temperature,
                        maxTokens = request.maxTokens
                    ).collect { token ->
                        emit(LiteRTResponseChunk(
                            text = token,
                            isComplete = false,
                            tokensGenerated = 1
                        ))
                    }
                    // Emit final completion marker
                    emit(LiteRTResponseChunk(
                        text = "",
                        isComplete = true,
                        tokensGenerated = 0
                    ))
                } catch (e: Exception) {
                    // Fall back to simulation on error
                    emit(LiteRTResponseChunk(
                        text = "⚠️ LiteRT Error: ${e.message}\n\nSimulation mode activated.",
                        isComplete = false,
                        tokensGenerated = 0
                    ))
                    simulateStreamingResponse(request).collect { chunk ->
                        emit(chunk)
                    }
                }
            }
            InferenceMode.SIMULATION -> {
                simulateStreamingResponse(request).collect { chunk ->
                    emit(chunk)
                }
            }
        }
    }
    
    /**
     * Simulate a response for testing without model
     */
    private fun simulateResponse(
        request: LiteRTRequest,
        errorPrefix: String?
    ): LiteRTResponseChunk {
        val prefix = errorPrefix?.let { "$it\n\n" } ?: ""
        val response = buildString {
            append(prefix)
            append("🤖 **Simulation Mode**\n\n")
            append("I received your prompt:\n```\n")
            append(request.prompt.take(200))
            if (request.prompt.length > 200) append("...")
            append("\n```\n\n")
            append("**Model Status:** Not loaded\n")
            append("**To enable real inference:**\n")
            append("1. Download Gemma 4 E4B IT model:\n")
            append("   `litert-community/gemma-4-E4B-it-litertlm`\n")
            append("2. Place at: `/data/data/com.loa.momclaw/files/models/`\n")
            append("3. Restart the app\n\n")
            append("This simulation mode allows you to test the UI and API integration.")
        }
        
        return LiteRTResponseChunk(
            text = response,
            isComplete = true,
            tokensGenerated = response.split(" ").size
        )
    }
    
    /**
     * Simulate streaming response for testing
     */
    private fun simulateStreamingResponse(request: LiteRTRequest) = kotlinx.coroutines.flow.flow {
        val words = listOf(
            "🤖", " **Simulation", " Mode**\n\n",
            "I", " received", " your", " prompt", ".\n",
            "This", " is", " a", " simulated", " response",
            " for", " testing", " purposes", ".\n\n",
            "To", " enable", " real", " inference", ",",
            " download", " the", " Gemma", " model", "."
        )
        
        words.forEach { word ->
            kotlinx.coroutines.delay(50) // Simulate generation speed
            emit(LiteRTResponseChunk(
                text = word,
                isComplete = false,
                tokensGenerated = 1
            ))
        }
        
        emit(LiteRTResponseChunk(
            text = "",
            isComplete = true,
            tokensGenerated = words.size
        ))
    }
}


// ==================== Data Classes ====================

/**
 * Model availability status
 */
sealed class ModelStatus {
    data class Available(
        val path: String,
        val sizeGB: Double,
        val info: ModelLoader.ModelInfo
    ) : ModelStatus()
    
    data class NotFound(
        val path: String,
        val suggestion: String
    ) : ModelStatus()
    
    data class Corrupted(
        val path: String,
        val sizeGB: Double,
        val expectedGB: Double,
        val suggestion: String
    ) : ModelStatus()
    
    data class Invalid(
        val path: String,
        val error: String,
        val suggestion: String
    ) : ModelStatus()
}

/**
 * Result of model loading attempt
 */
sealed class LoadResult {
    data class Success(
        val mode: InferenceMode,
        val modelName: String,
        val message: String
    ) : LoadResult()
    
    data class Failure(
        val error: String,
        val suggestion: String
    ) : LoadResult()
}

/**
 * Inference mode
 */
enum class InferenceMode {
    LITERT,       // Real on-device inference
    SIMULATION    // Simulated responses for testing
}

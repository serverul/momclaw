package com.loa.momclaw.bridge

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.LlmEngine
import com.google.ai.edge.litertlm.LlmGenerationSettings
import com.google.ai.edge.litertlm.LlmSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper for LiteRT-LM inference engine using TensorFlow Lite backend.
 * 
 * This implementation uses the real TensorFlow Lite-based LlmEngine
 * for actual on-device inference.
 * 
 * Architecture:
 * - LlmEngine: Singleton managing TensorFlow Lite interpreter
 * - LlmSession: Per-request session for inference
 * - LlmStream: Callback interface for streaming output
 */
class LlmEngineWrapper(private val context: Context) {

    private var engine: LlmEngine? = null
    private var session: LlmSession? = null
    private var isModelLoaded = false
    private var modelPath: String? = null
    private var generationSettings: LlmGenerationSettings = LlmGenerationSettings.DEFAULT

    /**
     * Loads a LiteRT model from the given path.
     * 
     * Supports both .tflite and .litertlm model files.
     * 
     * @param path Absolute path to model file
     * @return Result.success if loaded successfully, Result.failure otherwise
     */
    suspend fun loadModel(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(path)
                
                if (!modelFile.exists()) {
                    return@withContext Result.failure(
                        Exception("Model file not found: $path")
                    )
                }

                // Create model object
                val model = LlmEngine.Model(modelFile)
                
                // Initialize engine
                engine = LlmEngine.getInstance(context)
                
                // Load model into engine
                val loadResult = engine!!.loadModel(model, generationSettings)
                
                if (loadResult.isFailure) {
                    return@withContext Result.failure(
                        loadResult.exceptionOrNull() 
                            ?: Exception("Failed to load model")
                    )
                }
                
                // Create session
                session = LlmSession.create(context)
                val sessionResult = session!!.loadModel(model, generationSettings)
                
                if (sessionResult.isFailure) {
                    return@withContext Result.failure(
                        sessionResult.exceptionOrNull() 
                            ?: Exception("Failed to initialize session")
                    )
                }
                
                modelPath = path
                isModelLoaded = true
                
                Log.i("LlmEngineWrapper", "Model loaded successfully: ${model.name}")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e("LlmEngineWrapper", "Failed to load model: ${e.message}", e)
                Result.failure(Exception("Failed to load model: ${e.message}", e))
            }
        }
    }

    /**
     * Generates text tokens as a Flow for streaming responses.
     * 
     * Uses the TensorFlow Lite interpreter for actual inference.
     * 
     * @param prompt Formatted input prompt
     * @param temperature Sampling temperature (0.0-2.0)
     * @param maxTokens Maximum tokens to generate
     * @return Flow of generated tokens
     */
    fun generate(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<String> {
        if (!isModelLoaded || session == null) {
            throw IllegalStateException("Model not loaded. Call loadModel() first.")
        }

        // Use real TensorFlow Lite inference with Flow
        return session!!.generateFlow(
            prompt = prompt,
            temperature = temperature,
            maxTokens = maxTokens
        ).flowOn(Dispatchers.Default)
    }

    /**
     * Generates a complete response (non-streaming).
     * 
     * Collects all tokens from the streaming flow and returns as single string.
     * 
     * @param prompt Formatted input prompt
     * @param temperature Sampling temperature
     * @param maxTokens Maximum tokens to generate
     * @return Complete generated text
     */
    suspend fun generateComplete(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Result<String> {
        return try {
            val response = StringBuilder()
            
            generate(prompt, temperature, maxTokens)
                .catch { e ->
                    Log.e("LlmEngineWrapper", "Error during generation", e)
                    throw e
                }
                .collect { token ->
                    response.append(token)
                }
            
            Result.success(response.toString())
        } catch (e: Exception) {
            Log.e("LlmEngineWrapper", "Failed to generate response", e)
            Result.failure(e)
        }
    }

    /**
     * Update generation settings
     */
    fun updateSettings(
        temperature: Float? = null,
        topK: Int? = null,
        topP: Float? = null,
        maxTokens: Int? = null
    ) {
        val builder = LlmGenerationSettings.builder()
        
        temperature?.let { builder.setTemperature(it) }
        topK?.let { builder.setTopK(it) }
        topP?.let { builder.setTopP(it) }
        maxTokens?.let { builder.setMaxTokens(it) }
        
        generationSettings = builder.build()
        
        Log.d("LlmEngineWrapper", "Updated generation settings: temp=$temperature, topK=$topK, topP=$topP, maxTokens=$maxTokens")
    }

    /**
     * Checks if a model is currently loaded.
     */
    fun isLoaded(): Boolean = isModelLoaded && session != null

    /**
     * Gets the currently loaded model path.
     */
    fun getLoadedModelPath(): String? = modelPath

    /**
     * Releases model resources.
     */
    fun close() {
        try {
            session?.close()
            session = null
            
            engine?.close()
            engine = null
            
            isModelLoaded = false
            modelPath = null
            
            Log.i("LlmEngineWrapper", "Engine resources released")
        } catch (e: Exception) {
            Log.e("LlmEngineWrapper", "Error closing engine", e)
        }
    }

    companion object {
        private const val TAG = "LlmEngineWrapper"
    }
}

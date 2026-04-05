package com.loa.momclaw.bridge

import android.content.Context
import com.google.ai.edge.litertlm.LlmCallback
import com.google.ai.edge.litertlm.LlmEngine
import com.google.ai.edge.litertlm.LlmGenerationSettings
import com.google.ai.edge.litertlm.LlmSession
import com.google.ai.edge.litertlm.LlmStream
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}

/**
 * LiteRT LLM Engine Wrapper
 * 
 * Uses Google AI Edge LiteRT-LM SDK for on-device inference.
 * 
 * SDK: com.google.ai.edge:litert-lm:1.0.0
 * GitHub: https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/llm
 * 
 * Model must be downloaded from HuggingFace:
 *   litert-community/gemma-3-E4B-it-litertlm -> gemma-3-E4B-it.litertlm
 * 
 * Usage:
 *   val wrapper = LlmEngineWrapper(context)
 *   wrapper.loadModel("/data/data/com.loa.momclaw/models/gemma-3-E4B-it.litertlm")
 *   val result = wrapper.generate("Hello")
 *   // or streaming:
 *   wrapper.generateStreaming("Hello").collect { chunk -> ... }
 */
class LlmEngineWrapper(
    private val context: Context
) {
    private var session: LlmSession? = null
    private var modelName: String? = null
    private var modelPath: String? = null
    
    /**
     * Load the LiteRT model from file path.
     * Supports .litertlm files (Gemma 3 E4B instruction-tuned).
     */
    suspend fun loadModel(path: String): Boolean {
        return try {
            logger.info { "Loading LiteRT model from: $path" }
            
            val file = java.io.File(path)
            if (!file.exists()) {
                logger.error { "Model file not found: $path" }
                return false
            }
            
            modelPath = path
            modelName = file.nameWithoutExtension
            
            // Load model into LiteRT session
            session = LlmSession.create(context).apply {
                val model = LlmEngine.Model(file)
                val settings = LlmGenerationSettings.builder()
                    .setTopK(40)
                    .setTopP(0.95f)
                    .setTemperature(0.7f)
                    .setRandomSeed(42)
                    .build()
                loadModel(model, settings)
            }
            
            logger.info { "LiteRT model loaded: $modelName" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to load LiteRT model: ${e.message}" }
            session = null
            modelPath = null
            modelName = null
            false
        }
    }
    
    fun isReady(): Boolean = session != null
    
    fun getModelInfo(): Map<String, Any?> = mapOf(
        "name" to modelName,
        "path" to modelPath,
        "loaded" to (session != null),
        "type" to "LiteRT-LM",
        // Query token info from LiteRT once loaded
        "tokenCount" to getTokenCount()
    )
    
    private fun getTokenCount(): Int = try {
        if (session != null) 4096 else 0  // LiteRT doesn't expose this directly yet
    } catch (_: Exception) { 0 }
    
    /**
     * Format OpenAI-style messages into a prompt for LiteRT.
     * Uses conversational prompt format for instruction-tuned models.
     */
    fun formatPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (message in messages) {
            when (message.role.lowercase()) {
                "system", "user" -> {
                    sb.append("user: ")
                    sb.appendLine(message.content)
                }
                "assistant" -> {
                    sb.append("model: ")
                    sb.appendLine(message.content)
                }
            }
        }
        sb.append("model: ")
        return sb.toString()
    }
    
    /**
     * Generate a non-streaming response.
     */
    suspend fun generate(request: LiteRTRequest): LiteRTResponseChunk {
        val currentSession = session ?: throw IllegalStateException("Model not loaded")
        
        return suspendCancellableCoroutine { continuation ->
            try {
                currentSession.generateAsync(request.prompt, object : LlmCallback() {
                    override fun onResult(result: String?) {
                        continuation.resume(
                            LiteRTResponseChunk(
                                text = result ?: "",
                                isComplete = true,
                                tokensGenerated = result?.split(" ")?.size ?: 0
                            )
                        )
                    }
                    
                    override fun onError(error: Throwable?) {
                        continuation.resumeWithException(
                            error ?: RuntimeException("LiteRT generation failed")
                        )
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Generate a streaming response using LiteRT streaming API.
     */
    fun generateStreaming(request: LiteRTRequest): Flow<LiteRTResponseChunk> = callbackFlow {
        val currentSession = session ?: run {
            trySendBlocking(LiteRTResponseChunk("Error: Model not loaded", true, 0))
            close()
            return@callbackFlow
        }
        
        try {
            val stream = currentSession.generateStream(
                request.prompt,
                object : LlmStream() {
                    override fun onResult(result: String?) {
                        trySendBlocking(
                            LiteRTResponseChunk(
                                text = result ?: "",
                                isComplete = false,
                                tokensGenerated = 0
                            )
                        )
                    }

                    override fun onComplete() {
                        trySendBlocking(
                            LiteRTResponseChunk(
                                text = "",
                                isComplete = true,
                                tokensGenerated = 0
                            )
                        )
                        close()
                    }
                    
                    override fun onError(error: Throwable?) {
                        close(error ?: RuntimeException("LiteRT streaming failed"))
                    }
                }
            )
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            // Cleanup if needed
        }
    }
    
    /**
     * Close the session and release resources.
     */
    fun close() {
        logger.info { "Closing LiteRT engine" }
        try {
            session?.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing LiteRT session" }
        }
        session = null
        modelPath = null
        modelName = null
    }
}
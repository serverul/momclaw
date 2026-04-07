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
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * LiteRT LLM Engine Wrapper
 * 
 * Uses Google AI Edge LiteRT-LM SDK for on-device inference.
 * Thread-safe: All operations protected by read-write lock.
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
    private companion object {
        private val TAG = "LlmEngineWrapper"
    }
    
    private val sessionRef = AtomicReference<LlmSession?>(null)
    @Volatile private var modelName: String? = null
    @Volatile private var modelPath: String? = null
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Load the LiteRT model from file path.
     * Supports .litertlm files (Gemma 3 E4B instruction-tuned).
     * Thread-safe: Acquires write lock during load.
     */
    suspend fun loadModel(path: String): Boolean {
        return lock.write {
            try {
                val file = java.io.File(path)
                if (!file.exists()) {
                    logger.error { "Model file not found: $path" }
                    return@write false
                }
                
                val fileSizeMB = file.length() / (1024 * 1024)
                logger.info { "Loading LiteRT model from $path (${fileSizeMB}MB)" }
                
                // Close existing session if any
                sessionRef.get()?.close()
                
                modelPath = path
                modelName = file.nameWithoutExtension
                
                val newSession = LlmSession.create(context).apply {
                    val model = LlmEngine.Model(file)
                    val settings = LlmGenerationSettings.builder()
                        .setTopK(40)
                        .setTopP(0.95f)
                        .setTemperature(0.7f)
                        .setRandomSeed(42)
                        .build()
                    loadModel(model, settings)
                }
                
                sessionRef.set(newSession)
                logger.info { "LiteRT model loaded: $modelName" }
                true
            } catch (e: Exception) {
                logger.error(e) { "Failed to load LiteRT model from $path" }
                sessionRef.set(null)
                modelPath = null
                modelName = null
                false
            }
        }
    }
    
    fun isReady(): Boolean = lock.read { sessionRef.get() != null }
    
    fun getModelInfo(): Map<String, Any?> = lock.read {
        mapOf(
            "name" to modelName,
            "path" to modelPath,
            "loaded" to (sessionRef.get() != null),
            "type" to "LiteRT-LM",
            "tokenCount" to getTokenCount()
        )
    }
    
    private fun getTokenCount(): Int = try {
        if (sessionRef.get() != null) 4096 else 0  // LiteRT doesn't expose this directly yet
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
     * Thread-safe: Acquires read lock to access session.
     */
    suspend fun generate(request: LiteRTRequest): LiteRTResponseChunk {
        val currentSession = lock.read { sessionRef.get() }
            ?: throw IllegalStateException("Model not loaded")
        
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
     * Thread-safe: Acquires read lock to access session.
     */
    fun generateStreaming(request: LiteRTRequest): Flow<LiteRTResponseChunk> = callbackFlow {
        val currentSession = lock.read { sessionRef.get() }
        if (currentSession == null) {
            trySendBlocking(LiteRTResponseChunk("Error: Model not loaded", true, 0))
            close()
            return@callbackFlow
        }
        
        try {
            currentSession.generateStream(
                request.prompt,
                object : LlmStream() {
                    override fun onResult(result: String?) {
                        if (!isClosedForSend) {
                            trySendBlocking(
                                LiteRTResponseChunk(
                                    text = result ?: "",
                                    isComplete = false,
                                    tokensGenerated = 0
                                )
                            )
                        }
                    }

                    override fun onComplete() {
                        if (!isClosedForSend) {
                            trySendBlocking(
                                LiteRTResponseChunk(
                                    text = "",
                                    isComplete = true,
                                    tokensGenerated = 0
                                )
                            )
                        }
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
            // Cleanup if needed - channel automatically closed
        }
    }
    
    /**
     * Close the session and release resources.
     * Thread-safe: Acquires write lock during cleanup.
     */
    fun close() {
        lock.write {
            logger.info { "Closing LLM engine, model=$modelName" }
            try {
                sessionRef.getAndSet(null)?.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing LLM session" }
            }
            modelPath = null
            modelName = null
        }
    }
    
    /**
     * Cleanup on garbage collection
     */
    protected fun finalize() {
        close()
    }
}
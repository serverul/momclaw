// STUB: com.google.ai.edge.litertlm.LlmSession
// TODO: Replace with real Google AI Edge LiteRT-LM SDK when published
package com.google.ai.edge.litertlm

import android.content.Context

/**
 * LiteRT session for model inference.
 * Stub implementation for build-time compilation.
 */
class LlmSession private constructor(private val context: Context) {
    
    companion object {
        fun create(context: Context): LlmSession = LlmSession(context)
    }
    
    /**
     * Load model into this session
     */
    fun loadModel(model: LlmEngine.Model, settings: LlmGenerationSettings) {
        // Stub - no-op
    }
    
    /**
     * Async generation with callback
     */
    fun generateAsync(prompt: String, callback: LlmCallback) {
        // Stub - simulate async response
        callback.onResult("This is a stub response. Connect the real LiteRT-LM SDK for actual inference.")
    }
    
    /**
     * Streaming generation with LlmStream callback
     */
    fun generateStream(prompt: String, stream: LlmStream) {
        // Stub - simulate streaming response
        stream.onResult("Stub ")
        stream.onResult("streaming ")
        stream.onResult("response.\n\n")
        stream.onComplete()
    }
    
    /**
     * Close the session
     */
    fun close() {
        // Stub - no-op
    }
}

// STUB: com.google.ai.edge.litertlm.LlmSession
// IMPORTANT: This is a placeholder stub for build-time compilation.
// The actual Google AI Edge LiteRT-LM SDK is not yet publicly available.
//
// Integration options:
// 1. Wait for official Google SDK release: https://ai.google.dev/edge/litert
// 2. Use ML Kit on-device translation/text APIs as alternative
// 3. Implement custom TensorFlow Lite model loading
//
// This stub provides build compatibility only. Runtime will return
// placeholder responses if actual inference is attempted.
package com.google.ai.edge.litertlm

import android.content.Context
import android.util.Log

/**
 * LiteRT session for model inference.
 * 
 * STUB IMPLEMENTATION - LIMITED FUNCTIONALITY
 * 
 * This class exists for build-time compilation only. Actual inference
 * requires the real LiteRT-LM SDK from Google AI Edge.
 * 
 * The stub provides simulated responses for testing UI without the SDK.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
class LlmSession private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "LlmSession"
        
        fun create(context: Context): LlmSession {
            Log.w(TAG, "Creating stub LlmSession - actual inference not available")
            return LlmSession(context)
        }
    }
    
    /**
     * Load model into this session
     */
    fun loadModel(model: LlmEngine.Model, settings: LlmGenerationSettings) {
        Log.i(TAG, "Stub: Model loaded (simulated): ${model.name}")
        Log.w(TAG, "Install LiteRT-LM SDK for actual model inference")
    }
    
    /**
     * Async generation with callback
     * 
     * STUB: Returns a placeholder message instead of actual inference.
     */
    fun generateAsync(prompt: String, callback: LlmCallback) {
        Log.d(TAG, "Stub generateAsync called with prompt length: ${prompt.length}")
        Log.w(TAG, "Returning stub response - install LiteRT-LM SDK for actual inference")
        callback.onResult(
            "[STUB RESPONSE] This is a placeholder from the LiteRT-LM stub. " +
            "For actual AI responses, integrate the real LiteRT-LM SDK. " +
            "See: https://ai.google.dev/edge/litert\n\n" +
            "Your prompt was: ${prompt.take(100)}..."
        )
    }
    
    /**
     * Streaming generation with LlmStream callback
     * 
     * STUB: Simulates streaming with placeholder tokens.
     */
    fun generateStream(prompt: String, stream: LlmStream) {
        Log.d(TAG, "Stub generateStream called with prompt length: ${prompt.length}")
        Log.w(TAG, "Streaming stub response - install LiteRT-LM SDK for actual inference")
        
        val stubResponse = listOf(
            "[STUB] ",
            "This ",
            "is ",
            "a ",
            "placeholder ",
            "streaming ",
            "response.\n\n",
            "Install ",
            "LiteRT-LM ",
            "SDK ",
            "for ",
            "actual ",
            "inference."
        )
        
        stubResponse.forEach { token ->
            stream.onResult(token)
            Thread.sleep(50) // Simulate streaming delay
        }
        stream.onComplete()
    }
    
    /**
     * Close the session
     */
    fun close() {
        Log.d(TAG, "Stub session closed")
    }
}

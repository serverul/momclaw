// STUB: com.google.ai.edge.litertlm.LlmCallback
// IMPORTANT: This is a placeholder stub for build-time compilation.
// The actual Google AI Edge LiteRT-LM SDK is not yet publicly available.
//
// Expected artifact: com.google.ai.edge:litert-lm:1.0.0
// See: https://ai.google.dev/edge/litert-lm/overview
//
// This stub provides build compatibility only.
package com.google.ai.edge.litertlm

/**
 * Callback interface for LiteRT async generation.
 * 
 * STUB IMPLEMENTATION
 * 
 * This class exists for build-time compilation only. Actual inference
 * requires the real LiteRT-LM SDK from Google AI Edge.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
abstract class LlmCallback {
    /**
     * Called when generation completes successfully.
     * @param result The generated text, or null if an error occurred.
     */
    abstract fun onResult(result: String?)
    
    /**
     * Called when an error occurs during generation.
     * @param error The error that occurred.
     */
    abstract fun onError(error: Throwable?)
    
    /**
     * Called for partial results during streaming generation.
     * @param partial The partial text generated so far.
     */
    open fun onPartialResult(partial: String?) {}
}

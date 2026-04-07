// Callback interface for LiteRT async generation
package com.google.ai.edge.litertlm

/**
 * Callback interface for LiteRT async generation.
 * 
 * Used for non-streaming generation or when Flow-based streaming
 * is not desired.
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

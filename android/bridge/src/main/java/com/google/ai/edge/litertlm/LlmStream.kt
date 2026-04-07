// STUB: com.google.ai.edge.litertlm.LlmStream
// IMPORTANT: This is a placeholder stub for build-time compilation.
// The actual Google AI Edge LiteRT-LM SDK is not yet publicly available.
//
// Integration options:
// 1. Wait for official Google SDK release: https://ai.google.dev/edge/litert
// 2. Use ML Kit on-device APIs as alternative
// 3. Implement custom TensorFlow Lite model loading
//
// This stub provides build compatibility only.
package com.google.ai.edge.litertlm

/**
 * Streaming callback interface for LiteRT generation.
 * 
 * STUB IMPLEMENTATION
 * 
 * This class exists for build-time compilation only. Actual streaming
 * inference requires the real LiteRT-LM SDK from Google AI Edge.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
abstract class LlmStream {
    /**
     * Called for each partial result token/chunk.
     * Override this method to receive streaming tokens.
     * 
     * @param result The partial text token or chunk.
     */
    open fun onResult(result: String?) {}
    
    /**
     * Called when streaming generation is complete.
     * Override this method to handle completion.
     */
    open fun onComplete() {}
    
    /**
     * Called when an error occurs during streaming.
     * Override this method to handle errors.
     * 
     * @param error The error that occurred.
     */
    open fun onError(error: Throwable?) {}
}

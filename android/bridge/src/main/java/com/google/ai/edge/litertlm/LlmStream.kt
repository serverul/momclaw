// STUB: com.google.ai.edge.litertlm.LlmStream
// TODO: Replace with real Google AI Edge LiteRT-LM SDK when published
package com.google.ai.edge.litertlm

/**
 * Streaming callback interface for LiteRT generation.
 * Stub implementation for build-time compilation.
 */
abstract class LlmStream {
    /**
     * Called for each partial result token/chunk
     */
    open fun onResult(result: String?) {}
    
    /**
     * Called when streaming is complete
     */
    open fun onComplete() {}
    
    /**
     * Called on error
     */
    open fun onError(error: Throwable?) {}
}

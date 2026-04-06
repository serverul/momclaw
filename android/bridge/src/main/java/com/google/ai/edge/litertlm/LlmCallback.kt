// STUB: com.google.ai.edge.litertlm.LlmCallback
// TODO: Replace with real Google AI Edge LiteRT-LM SDK when published
// Expected artifact: com.google.ai.edge:litert-lm:1.0.0
// See: https://ai.google.dev/edge/litert-lm/overview
package com.google.ai.edge.litertlm

/**
 * Callback interface for LiteRT async generation.
 * Stub implementation for build-time compilation.
 */
abstract class LlmCallback {
    abstract fun onResult(result: String?)
    abstract fun onError(error: Throwable?)
    open fun onPartialResult(partial: String?) {}
}

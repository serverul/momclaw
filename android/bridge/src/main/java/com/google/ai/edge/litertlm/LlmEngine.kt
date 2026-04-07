// STUB: com.google.ai.edge.litertlm.LlmEngine
// IMPORTANT: This is a placeholder stub for build-time compilation.
// The actual Google AI Edge LiteRT-LM SDK is not yet publicly available.
// 
// Integration options:
// 1. Wait for official Google SDK release: https://ai.google.dev/edge/litert
// 2. Use ML Kit on-device translation/text APIs as alternative
// 3. Implement custom TensorFlow Lite model loading
//
// This stub provides build compatibility only. Runtime will throw
// UnsupportedOperationException if actual inference is attempted.
package com.google.ai.edge.litertlm

import android.content.Context
import android.util.Log
import java.io.File

/**
 * LiteRT Engine for loading and managing LLM models.
 * 
 * STUB IMPLEMENTATION - NOT FUNCTIONAL
 * 
 * This class exists for build-time compilation only. Actual inference
 * requires the real LiteRT-LM SDK from Google AI Edge.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
class LlmEngine private constructor() {
    
    companion object {
        private const val TAG = "LlmEngine"
        
        fun getInstance(context: Context): LlmEngine {
            Log.w(TAG, "Using stub LlmEngine - actual inference not available")
            return LlmEngine()
        }
    }
    
    /**
     * Model representation wrapping a .litertlm file
     */
    class Model(file: File) {
        val path: String = file.absolutePath
        val name: String = file.nameWithoutExtension
    }
    
    fun loadModel(model: Model, settings: LlmGenerationSettings) {
        Log.w(TAG, "Stub loadModel called for: ${model.name}")
        Log.w(TAG, "Install LiteRT-LM SDK for actual model loading")
    }
    
    fun close() {
        Log.d(TAG, "Stub close called")
    }
}

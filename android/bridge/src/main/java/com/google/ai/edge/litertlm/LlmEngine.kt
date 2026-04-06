// STUB: com.google.ai.edge.litertlm.LlmEngine
// TODO: Replace with real Google AI Edge LiteRT-LM SDK when published
package com.google.ai.edge.litertlm

import android.content.Context
import java.io.File

/**
 * LiteRT Engine for loading and managing LLM models.
 * Stub implementation for build-time compilation.
 */
class LlmEngine private constructor() {
    
    companion object {
        fun getInstance(context: Context): LlmEngine = LlmEngine()
    }
    
    /**
     * Model representation wrapping a .litertlm file
     */
    class Model(file: File) {
        val path: String = file.absolutePath
    }
    
    fun loadModel(model: Model, settings: LlmGenerationSettings): Unit {}
    fun close(): Unit {}
}

// STUB: com.google.ai.edge.litertlm.LlmGenerationSettings
// TODO: Replace with real Google AI Edge LiteRT-LM SDK when published
package com.google.ai.edge.litertlm

/**
 * Generation settings for LiteRT models.
 * Stub implementation for build-time compilation.
 */
class LlmGenerationSettings private constructor(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val randomSeed: Long = 0
) {
    
    companion object {
        fun builder() = Builder()
    }
    
    class Builder {
        private var topK = 40
        private var topP = 0.95f
        private var temperature = 0.7f
        private var randomSeed = 0L
        
        fun setTopK(topK: Int) = apply { this.topK = topK }
        fun setTopP(topP: Float) = apply { this.topP = topP }
        fun setTemperature(temperature: Float) = apply { this.temperature = temperature }
        fun setRandomSeed(randomSeed: Long) = apply { this.randomSeed = randomSeed }
        fun build() = LlmGenerationSettings(topK, topP, temperature, randomSeed)
    }
}

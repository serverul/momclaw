// Generation settings for LiteRT models
package com.google.ai.edge.litertlm

/**
 * Generation settings for LiteRT models.
 * 
 * Controls sampling parameters for text generation including
 * temperature, top-k, top-p, and max tokens.
 * 
 * Default values are optimized for Gemma-style models.
 * 
 * @see <a href="https://ai.google.dev/edge/litert">Google AI Edge LiteRT</a>
 */
class LlmGenerationSettings private constructor(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val randomSeed: Long = 0,
    val maxTokens: Int = 2048
) {
    
    companion object {
        /**
         * Create a builder for generation settings.
         */
        fun builder() = Builder()
        
        /**
         * Default settings optimized for general text generation.
         */
        val DEFAULT = builder()
            .setTopK(40)
            .setTopP(0.95f)
            .setTemperature(0.7f)
            .setMaxTokens(2048)
            .build()
        
        /**
         * Settings optimized for creative writing.
         */
        val CREATIVE = builder()
            .setTopK(80)
            .setTopP(0.95f)
            .setTemperature(1.0f)
            .setMaxTokens(2048)
            .build()
        
        /**
         * Settings optimized for factual/deterministic output.
         */
        val PRECISE = builder()
            .setTopK(10)
            .setTopP(0.9f)
            .setTemperature(0.3f)
            .setMaxTokens(2048)
            .build()
    }
    
    /**
     * Builder for LlmGenerationSettings.
     */
    class Builder {
        private var topK = 40
        private var topP = 0.95f
        private var temperature = 0.7f
        private var randomSeed = 0L
        private var maxTokens = 2048
        
        /**
         * Set top-k sampling parameter.
         * Limits sampling to top k most likely tokens.
         * @param topK Number of top tokens (default: 40)
         */
        fun setTopK(topK: Int) = apply { 
            require(topK > 0) { "TopK must be positive" }
            this.topK = topK 
        }
        
        /**
         * Set top-p (nucleus) sampling parameter.
         * Limits sampling to tokens with cumulative probability >= top_p.
         * @param topP Cumulative probability threshold (default: 0.95, range: 0.0-1.0)
         */
        fun setTopP(topP: Float) = apply { 
            require(topP in 0.0f..1.0f) { "TopP must be between 0.0 and 1.0" }
            this.topP = topP 
        }
        
        /**
         * Set sampling temperature.
         * Higher values produce more random output.
         * @param temperature Sampling temperature (default: 0.7, range: 0.0-2.0)
         */
        fun setTemperature(temperature: Float) = apply { 
            require(temperature >= 0.0f) { "Temperature must be non-negative" }
            this.temperature = temperature 
        }
        
        /**
         * Set random seed for reproducible outputs.
         * @param randomSeed Random seed (default: 0 = random)
         */
        fun setRandomSeed(randomSeed: Long) = apply { this.randomSeed = randomSeed }
        
        /**
         * Set maximum tokens to generate.
         * @param maxTokens Maximum output tokens (default: 2048)
         */
        fun setMaxTokens(maxTokens: Int) = apply { 
            require(maxTokens > 0) { "MaxTokens must be positive" }
            this.maxTokens = maxTokens 
        }
        
        /**
         * Build the generation settings.
         */
        fun build() = LlmGenerationSettings(topK, topP, temperature, randomSeed, maxTokens)
    }
    
    override fun toString(): String {
        return "LlmGenerationSettings(topK=$topK, topP=$topP, temperature=$temperature, " +
               "randomSeed=$randomSeed, maxTokens=$maxTokens)"
    }
}

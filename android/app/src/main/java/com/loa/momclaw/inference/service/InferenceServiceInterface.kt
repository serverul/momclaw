package com.loa.momclaw.inference.service

/**
 * Service Interface for Inference Operations
 * 
 * Defines the contract for inference service implementations.
 * Used for dependency injection and testing.
 */
interface InferenceServiceInterface {
    
    /**
     * Current state of the inference service
     */
    val state: InferenceServiceState
    
    /**
     * Load a model from the given path
     */
    suspend fun loadModel(modelPath: String): Result<Unit>
    
    /**
     * Unload the current model
     */
    suspend fun unloadModel()
    
    /**
     * Start the HTTP server on the given port
     */
    suspend fun startServer(port: Int = 8080): Result<Unit>
    
    /**
     * Stop the HTTP server
     */
    suspend fun stopServer()
    
    /**
     * Check if model is loaded and ready
     */
    fun isModelReady(): Boolean
    
    /**
     * Get the HTTP endpoint for the inference server
     */
    fun getEndpoint(): String
    
    /**
     * Get information about the loaded model
     */
    fun getModelInfo(): ModelInfo?
}

/**
 * Inference Service State
 */
sealed class InferenceServiceState {
    object Idle : InferenceServiceState()
    data class Loading(val modelPath: String) : InferenceServiceState()
    data class Running(val modelPath: String, val port: Int) : InferenceServiceState()
    data class Error(val message: String, val throwable: Throwable? = null) : InferenceServiceState()
    object Stopped : InferenceServiceState()
}

/**
 * Model Information
 */
data class ModelInfo(
    val name: String,
    val path: String,
    val loaded: Boolean,
    val type: String = "LiteRT-LM",
    val sizeBytes: Long = 0,
    val contextLength: Int = 4096,
    val tokenCount: Int = 0
)

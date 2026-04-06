package com.loa.momclaw.bridge

import kotlinx.serialization.Serializable

/**
 * Standardized error types for LiteRT Bridge
 * 
 * Provides consistent error handling across the bridge with:
 * - Structured error codes
 * - User-friendly messages
 * - Debug information
 */
sealed class BridgeError(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Model-related errors
     */
    sealed class ModelError(
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap(),
        cause: Throwable? = null
    ) : BridgeError("MODEL_$code", message, details, cause) {
        
        class NotFound(path: String) : ModelError(
            "NOT_FOUND",
            "Model file not found",
            mapOf("path" to path)
        )
        
        class LoadFailed(path: String, reason: String, cause: Throwable? = null) : ModelError(
            "LOAD_FAILED",
            "Failed to load model: $reason",
            mapOf("path" to path, "reason" to reason),
            cause
        )
        
        class NotReady : ModelError(
            "NOT_READY",
            "Model not loaded. Call loadModel() first."
        )
        
        class InvalidFormat(path: String, expected: String) : ModelError(
            "INVALID_FORMAT",
            "Invalid model format. Expected: $expected",
            mapOf("path" to path, "expected" to expected)
        )
        
        class InsufficientMemory(required: Long, available: Long) : ModelError(
            "INSUFFICIENT_MEMORY",
            "Not enough memory to load model",
            mapOf("requiredMB" to required / (1024 * 1024), "availableMB" to available / (1024 * 1024))
        )
    }
    
    /**
     * Inference errors
     */
    sealed class InferenceError(
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap(),
        cause: Throwable? = null
    ) : BridgeError("INFERENCE_$code", message, details, cause) {
        
        class GenerationFailed(reason: String, cause: Throwable? = null) : InferenceError(
            "GENERATION_FAILED",
            "Generation failed: $reason",
            mapOf("reason" to reason),
            cause
        )
        
        class Timeout(timeoutMs: Long) : InferenceError(
            "TIMEOUT",
            "Generation timed out after ${timeoutMs}ms",
            mapOf("timeoutMs" to timeoutMs)
        )
        
        class TokenLimitExceeded(limit: Int, requested: Int) : InferenceError(
            "TOKEN_LIMIT",
            "Token limit exceeded",
            mapOf("limit" to limit, "requested" to requested)
        )
        
        class StreamingError(cause: Throwable? = null) : InferenceError(
            "STREAMING_ERROR",
            "Streaming generation failed",
            cause = cause
        )
    }
    
    /**
     * Server errors
     */
    sealed class ServerError(
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap(),
        cause: Throwable? = null
    ) : BridgeError("SERVER_$code", message, details, cause) {
        
        class StartupFailed(port: Int, cause: Throwable? = null) : ServerError(
            "STARTUP_FAILED",
            "Failed to start server on port $port",
            mapOf("port" to port),
            cause
        )
        
        class AlreadyRunning(port: Int) : ServerError(
            "ALREADY_RUNNING",
            "Server already running on port $port",
            mapOf("port" to port)
        )
        
        class BindFailed(port: Int, cause: Throwable? = null) : ServerError(
            "BIND_FAILED",
            "Failed to bind to port $port",
            mapOf("port" to port),
            cause
        )
    }
    
    /**
     * Request validation errors
     */
    sealed class ValidationError(
        code: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) : BridgeError("VALIDATION_$code", message, details) {
        
        class MissingField(field: String) : ValidationError(
            "MISSING_FIELD",
            "Required field missing: $field",
            mapOf("field" to field)
        )
        
        class InvalidValue(field: String, value: Any?, reason: String) : ValidationError(
            "INVALID_VALUE",
            "Invalid value for '$field': $reason",
            mapOf("field" to field, "value" to value, "reason" to reason)
        )
        
        class EmptyMessages : ValidationError(
            "EMPTY_MESSAGES",
            "Messages array cannot be empty"
        )
    }
}

/**
 * JSON-serializable error response for API
 */
@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Convert BridgeError to API response
 */
fun BridgeError.toResponse(): ErrorResponse = ErrorResponse(
    error = ErrorDetail(
        code = this.code,
        message = this.message,
        details = this.details.mapValues { it.value?.toString() ?: "" }
    )
)

/**
 * Result wrapper for operations that can fail
 */
sealed class OperationResult<out T> {
    data class Success<T>(val value: T) : OperationResult<T>()
    data class Failure(val error: BridgeError) : OperationResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): OperationResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> OperationResult<R>): OperationResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): OperationResult<T> {
        if (this is Success) action(value)
        return this
    }
    
    inline fun onFailure(action: (BridgeError) -> Unit): OperationResult<T> {
        if (this is Failure) action(error)
        return this
    }
    
    fun getOrNull(): T? = (this as? Success)?.value
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error
    }
}

/**
 * Extension to wrap exceptions in OperationResult
 */
inline fun <T> runCatchingBridge(block: () -> T): OperationResult<T> {
    return try {
        OperationResult.Success(block())
    } catch (e: BridgeError) {
        OperationResult.Failure(e)
    } catch (e: Exception) {
        OperationResult.Failure(BridgeError.InferenceError.GenerationFailed(e.message ?: "Unknown error", e))
    }
}

package com.loa.momclaw.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data classes for OpenAI-compatible chat API requests and responses.
 * 
 * NOTE: Request types are in ChatRequest.kt
 * This file contains only response types to avoid redeclarations.
 */

// ==================== Response Models ====================

/**
 * Chat completion response (OpenAI-compatible)
 */
@Serializable
data class ChatCompletionResponse(
    val id: String,
    @SerialName("object")
    val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage? = null
)

/**
 * Token usage statistics
 */
@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

/**
 * Legacy aliases for backward compatibility
 */
typealias ChatResponse = ChatCompletionResponse
typealias MessageDto = ChatMessage
typealias Choice = ChatChoice
typealias Delta = ChatDelta

// ==================== Streaming Models ====================

/**
 * Streaming chat completion chunk
 */
@Serializable
data class ChatCompletionChunk(
    val id: String,
    @SerialName("object")
    val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<StreamingChoice>
)

@Serializable
data class StreamingChoice(
    val index: Int = 0,
    val delta: ChatDelta,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

// ==================== Error Models ====================

/**
 * API error response
 */
@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null
)

// ==================== SSE Formatter ====================

/**
 * SSE (Server-Sent Events) formatter for streaming responses
 */
object SSEFormatter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = false
    }

    /**
     * Format a streaming token as SSE event
     */
    fun formatStreamEvent(token: String, model: String): String {
        val delta = ChatDelta(content = token)
        val choice = StreamingChoice(delta = delta)
        val response = ChatCompletionChunk(
            id = "chatcmpl-${System.currentTimeMillis()}",
            created = System.currentTimeMillis() / 1000,
            model = model,
            choices = listOf(choice)
        )
        return buildString {
            append("data: ")
            append(json.encodeToString(ChatCompletionChunk.serializer(), response))
            append("\n\n")
        }
    }

    /**
     * Format [DONE] marker for stream end
     */
    fun formatDoneEvent(): String {
        return "data: [DONE]\n\n"
    }

    /**
     * Format error event
     */
    fun formatErrorEvent(error: String): String {
        return buildString {
            append("data: ")
            append(json.encodeToString(ErrorResponse.serializer(), ErrorResponse(
                ErrorDetail(message = error, type = "inference_error")
            )))
            append("\n\n")
        }
    }

    /**
     * Format complete response (non-streaming)
     */
    fun formatCompleteResponse(response: ChatCompletionResponse): String {
        return json.encodeToString(ChatCompletionResponse.serializer(), response)
    }
}

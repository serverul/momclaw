package com.loa.momclaw.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible chat completion request
 */
@Serializable
data class ChatCompletionRequest(
    val model: String = "gemma-4e4b",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("top_p")
    val topP: Double = 0.9,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double = 0.0,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double = 0.0
)

@Serializable
data class ChatMessage(
    val role: String,  // system, user, assistant
    val content: String
)

/**
 * OpenAI-compatible chat completion response
 */
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    val delta: ChatDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ChatDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/**
 * SSE event wrapper
 */
@Serializable
data class SSEEvent(
    val data: String,
    val event: String? = null,
    val id: String? = null,
    val retry: Int? = null
)

/**
 * LiteRT internal request format
 */
data class LiteRTRequest(
    val prompt: String,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val stopTokens: List<String> = emptyList()
)

/**
 * LiteRT response chunk
 */
data class LiteRTResponseChunk(
    val text: String,
    val isComplete: Boolean = false,
    val tokensGenerated: Int = 0
)

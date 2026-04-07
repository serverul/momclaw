package com.loa.momclaw.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data classes for OpenAI-compatible chat API requests and responses.
 */
@Serializable
data class ChatRequest(
    val model: String = "gemma-4-e4b",
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2048,
    val top_p: Float = 1.0f,
    val frequency_penalty: Float = 0.0f,
    val presence_penalty: Float = 0.0f
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val delta: Delta? = null,
    val message: MessageDto? = null,
    val finish_reason: String? = null
)

@Serializable
data class Delta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

/**
 * Type alias for compatibility - points to ChatResponse
 */
typealias ChatCompletionResponse = ChatResponse

/**
 * SSE (Server-Sent Events) parser and formatter.
 */
object SSEFormatter {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    fun formatStreamEvent(token: String, model: String): String {
        val delta = Delta(content = token)
        val choice = Choice(delta = delta)
        val response = ChatResponse(
            id = "chatcmpl-${System.currentTimeMillis()}",
            created = System.currentTimeMillis() / 1000,
            model = model,
            choices = listOf(choice)
        )
        val responseJson = json.encodeToString(response)
        return "data: $responseJson\n\n"
    }

    fun formatDoneEvent(): String {
        return "data: [DONE]\n\n"
    }

    fun formatErrorEvent(error: String): String {
        return "data: {\"error\": \"$error\"}\n\n"
    }
}

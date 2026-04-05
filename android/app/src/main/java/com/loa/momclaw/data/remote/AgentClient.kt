package com.loa.momclaw.data.remote

import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/**
 * HTTP client for communicating with the NullClaw agent
 * Supports both regular and streaming responses
 */
class AgentClient(
    private val config: AgentConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Send a message and receive a streaming response
     */
    fun sendMessageStream(message: String, conversationHistory: List<ChatMessage> = emptyList()): Flow<String> = callbackFlow {
        val request = buildChatRequest(message, conversationHistory, stream = true)

        val eventSourceFactory = EventSources.createFactory(httpClient)
        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                // Parse SSE data for token
                val token = parseStreamToken(data)
                if (token != null) {
                    trySend(token)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                close(t ?: Exception("Stream failed: ${response?.message}"))
            }
        }

        val eventSource = eventSourceFactory.newEventSource(request, eventSourceListener)

        awaitClose {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Send a message and receive a complete response (non-streaming)
     */
    suspend fun sendMessage(message: String, conversationHistory: List<ChatMessage> = emptyList()): Result<String> {
        return try {
            val request = buildChatRequest(message, conversationHistory, stream = false)
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
                Result.success(chatResponse.content)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if the agent is available
     */
    suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/health")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get list of available models
     */
    suspend fun getAvailableModels(): Result<List<ModelInfo>> {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/v1/models")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                val modelsResponse = json.decodeFromString<ModelsResponse>(responseBody)
                Result.success(modelsResponse.data)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load a specific model
     */
    suspend fun loadModel(modelId: String): Result<Boolean> {
        return try {
            val requestBody = json.encodeToString(LoadModelRequest(modelId))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${config.baseUrl}/v1/models/load")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildChatRequest(message: String, history: List<ChatMessage>, stream: Boolean): Request {
        val messages = history.map { msg ->
            ChatRequestMessage(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.content
            )
        } + ChatRequestMessage(role = "user", content = message)

        val chatRequest = ChatRequest(
            model = config.modelPrimary,
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = stream
        )

        val requestBody = json.encodeToString(chatRequest)
            .toRequestBody("application/json".toMediaType())

        return Request.Builder()
            .url("${config.baseUrl}/v1/chat/completions")
            .post(requestBody)
            .header("Accept", if (stream) "text/event-stream" else "application/json")
            .build()
    }

    private fun parseStreamToken(data: String): String? {
        if (data.isBlank() || data == "[DONE]") return null

        return try {
            val chunk = json.decodeFromString<StreamChunk>(data)
            chunk.choices.firstOrNull()?.delta?.content
        } catch (e: Exception) {
            null
        }
    }
}

// Data classes for API requests/responses

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatRequestMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

@Serializable
data class ChatRequestMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val content: String,
    val model: String? = null,
    val usage: UsageInfo? = null
)

@Serializable
data class StreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta,
    val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String? = null,
    val size: Long? = null,
    val downloaded: Boolean = false,
    val loaded: Boolean = false
)

@Serializable
data class LoadModelRequest(
    val modelId: String
)

@Serializable
data class UsageInfo(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

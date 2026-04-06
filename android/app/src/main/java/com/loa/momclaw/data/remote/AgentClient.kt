package com.loa.momclaw.data.remote

import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import com.loa.momclaw.util.MomClawLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
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
 * Supports both regular and streaming responses with error recovery
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
        .pingInterval(15, TimeUnit.SECONDS)  // Keep-alive for SSE
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val HEALTH_CHECK_TIMEOUT_MS = 5000L
    }

    /**
     * Send a message and receive a streaming response with automatic retry
     */
    fun sendMessageStream(
        message: String, 
        conversationHistory: List<ChatMessage> = emptyList()
    ): Flow<String> = callbackFlow {
        val request = buildChatRequest(message, conversationHistory, stream = true)

        val eventSourceFactory = EventSources.createFactory(httpClient)
        var retryCount = 0
        
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
                MomClawLogger.d("SSE stream closed normally")
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val errorMsg = when {
                    t != null -> "Stream error: ${t.message}"
                    response != null -> "HTTP ${response.code}: ${response.message}"
                    else -> "Unknown stream error"
                }
                
                MomClawLogger.e(errorMsg, t)
                
                // Don't retry on client errors (4xx)
                val isClientError = response?.code?.let { it in 400..499 } ?: false
                
                if (!isClientError && retryCount < MAX_RETRIES) {
                    retryCount++
                    MomClawLogger.d("Retrying SSE connection (attempt $retryCount/$MAX_RETRIES)")
                    // Will be handled by retryWhen
                } else {
                    close(t ?: Exception(errorMsg))
                }
            }
            
            override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                MomClawLogger.d("SSE connection opened")
                retryCount = 0  // Reset retry count on successful connection
            }
        }

        val eventSource = eventSourceFactory.newEventSource(request, eventSourceListener)

        awaitClose {
            MomClawLogger.d("Cancelling SSE event source")
            eventSource.cancel()
        }
    }
    .retryWhen { cause, attempt ->
        if (attempt < MAX_RETRIES && !isCancellationError(cause)) {
            MomClawLogger.d("Retrying stream after error: ${cause.message}")
            delay(RETRY_DELAY_MS * (attempt + 1))  // Exponential backoff
            true
        } else {
            false
        }
    }
    .flowOn(Dispatchers.IO)

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
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                MomClawLogger.e(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            MomClawLogger.e("Failed to send message", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the agent is available with timeout
     */
    suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/health")
                .get()
                .build()
            
            val response = httpClient.newBuilder()
                .callTimeout(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
                .newCall(request)
                .execute()
            
            val isHealthy = response.isSuccessful
            if (isHealthy) {
                MomClawLogger.d("Agent health check passed")
            } else {
                MomClawLogger.w("Agent health check failed: HTTP ${response.code}")
            }
            isHealthy
        } catch (e: Exception) {
            MomClawLogger.w("Agent health check failed: ${e.message}")
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
                MomClawLogger.d("Loaded ${modelsResponse.data.size} models")
                Result.success(modelsResponse.data)
            } else {
                // Return empty list instead of failure for graceful degradation
                MomClawLogger.w("Failed to load models: HTTP ${response.code}")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            MomClawLogger.e("Failed to get available models", e)
            Result.failure(e)
        }
    }

    /**
     * Load a specific model
     */
    suspend fun loadModel(modelId: String): Result<Boolean> {
        return try {
            MomClawLogger.d("Loading model: $modelId")
            
            val requestBody = json.encodeToString(LoadModelRequest(modelId))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${config.baseUrl}/v1/models/load")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                MomClawLogger.i("Model loaded successfully: $modelId")
            } else {
                MomClawLogger.e("Failed to load model: HTTP ${response.code}")
            }
            
            Result.success(success)
        } catch (e: Exception) {
            MomClawLogger.e("Failed to load model: $modelId", e)
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
            .header("Cache-Control", "no-cache")
            .build()
    }

    private fun parseStreamToken(data: String): String? {
        if (data.isBlank() || data == "[DONE]") return null

        return try {
            val chunk = json.decodeFromString<StreamChunk>(data)
            chunk.choices.firstOrNull()?.delta?.content
        } catch (e: Exception) {
            // Log parsing errors but don't fail the stream
            MomClawLogger.w("Failed to parse stream token: ${e.message}")
            null
        }
    }
    
    private fun isCancellationError(cause: Throwable): Boolean {
        return cause is java.net.SocketException ||
               cause.message?.contains("cancel", ignoreCase = true) == true
    }
    
    /**
     * Release resources and cleanup HTTP client
     * IMPORTANT: Call this when the client is no longer needed
     */
    fun close() {
        MomClawLogger.d("Closing AgentClient and releasing resources")
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
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

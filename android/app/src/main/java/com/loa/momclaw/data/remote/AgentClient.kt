package com.loa.momclaw.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.loa.momclaw.config.NetworkConfig

/**
 * Data class for chat request to NullClaw.
 */
@Serializable
data class ChatRequestDto(
    val message: String,
    val conversation_id: Long? = null
)

/**
 * Data class for chat response from NullClaw.
 */
@Serializable
data class ChatResponseDto(
    val response: String,
    val token: String? = null,
    val done: Boolean = false
)

/**
 * HTTP client for communicating with NullClaw agent.
 * 
 * Provides methods for:
 * - Sending chat messages (streaming)
 * - Health checks
 * - Configuration updates
 */
class AgentClient(
    private val baseUrl: String = NetworkConfig.AGENT_URL
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "AgentClient"
        private const val CHAT_ENDPOINT = "/chat"
        private const val HEALTH_ENDPOINT = "/health"
    }

    /**
     * Sends a chat message and returns streaming tokens.
     * 
     * @param messages List of message DTOs
     * @return Flow of tokens
     */
    fun chat(messages: List<MessageDto>): Flow<String> = flow {
        val lastMessage = messages.lastOrNull { it.role == "user" }?.content
            ?: throw IllegalArgumentException("No user message found")

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.encodeToString(ChatRequestDto.serializer(), ChatRequestDto(lastMessage))
        )

        val request = Request.Builder()
            .url("$baseUrl$CHAT_ENDPOINT")
            .post(requestBody)
            .build()

        val responseFlow = MutableSharedFlow<String>()

        // Use SSE for streaming
        val eventSourceFactory = EventSources.createFactory(client)
        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    if (data == "[DONE]") {
                        responseFlow.close()
                        return
                    }

                    // Parse SSE data - assuming format similar to OpenAI
                    val response = json.decodeFromString(ChatResponseDto.serializer(), data)
                    response.token?.let { token ->
                        responseFlow.tryEmit(token)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SSE event", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                responseFlow.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE connection failed", t)
                responseFlow.close()
            }
        }

        eventSourceFactory.newEventSource(request, eventSourceListener)

        // Collect and emit tokens
        responseFlow.collect { token ->
            emit(token)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sends a simple non-streaming chat message.
     * 
     * @param message User message
     * @return Complete response
     */
    suspend fun chatSimple(message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                json.encodeToString(ChatRequestDto.serializer(), ChatRequestDto(message))
            )

            val request = Request.Builder()
                .url("$baseUrl$CHAT_ENDPOINT")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Request failed: ${response.code}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response"))

            val chatResponse = json.decodeFromString(ChatResponseDto.serializer(), responseBody)
            Result.success(chatResponse.response)
        } catch (e: Exception) {
            Log.e(TAG, "Chat request failed", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the agent is healthy.
     */
    suspend fun healthCheck(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl$HEALTH_ENDPOINT")
                .get()
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the agent is reachable.
     */
    suspend fun isReachable(): Boolean {
        return healthCheck().getOrDefault(false)
    }

    /**
     * Cleans up resources.
     */
    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

/**
 * Simple message DTO for agent communication.
 */
@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

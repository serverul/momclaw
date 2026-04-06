package com.loa.momclaw.bridge

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Server-Sent Events writer for streaming responses
 */
class SSEWriter(
    private val call: ApplicationCall,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun writeStream(
        flow: Flow<ChatCompletionResponse>,
        onEvent: suspend (ChatCompletionResponse) -> Unit = {}
    ) {
        call.response.headers.apply {
            append(HttpHeaders.ContentType, "text/event-stream")
            append(HttpHeaders.CacheControl, "no-cache")
            append(HttpHeaders.Connection, "keep-alive")
            append("X-Accel-Buffering", "no")
        }
        
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            flow.collect { response ->
                onEvent(response)
                val eventData = json.encodeToString(response)
                write("data: $eventData\n\n")
                flush()
            }
            
            // Send [DONE] marker
            write("data: [DONE]\n\n")
            flush()
        }
    }
    
    private fun writeEvent(response: ChatCompletionResponse) {
        val eventData = json.encodeToString(response)
        write("data: $eventData\n\n")
    }
    
    companion object {
        /**
         * Generate a unique response ID
         */
        fun generateId(): String = "chatcmpl-${UUID.randomUUID().toString().take(24)}"
        
        /**
         * Get current Unix timestamp
         */
        fun currentTimestamp(): Long = System.currentTimeMillis() / 1000
    }
}

/**
 * Extension to create SSE responses from Flow
 */
suspend fun ApplicationCall.respondSSE(
    flow: Flow<ChatCompletionResponse>,
    json: Json = Json { ignoreUnknownKeys = true }
) {
    SSEWriter(this, json).writeStream(flow)
}

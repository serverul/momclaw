package com.loa.momclaw.bridge

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Writer
import java.util.UUID

/**
 * Server-Sent Events writer for streaming responses
 * 
 * Supports Ktor 2.x which doesn't have native SSE plugin.
 * Uses respondTextWriter with text/event-stream content type.
 */
class SSEWriter(
    private val call: ApplicationCall,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    /**
     * Write SSE stream from Flow of ChatCompletionResponse
     */
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
                writeEvent(response)
                flush()
            }
            
            // Send [DONE] marker
            write("data: [DONE]\n\n")
            flush()
        }
    }
    
    /**
     * Write a single SSE event
     */
    private fun Writer.writeEvent(response: ChatCompletionResponse) {
        val eventData = json.encodeToString(response)
        write("data: $eventData\n\n")
    }
    
    /**
     * Write a raw SSE line
     */
    fun Writer.writeSSELine(data: String) {
        write("data: $data\n\n")
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
        
        /**
         * SSE content type constant
         */
        val SSE_CONTENT_TYPE = ContentType.Text.EventStream
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

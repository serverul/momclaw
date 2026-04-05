package com.loa.momclaw.bridge

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiteRTBridgeTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        assertTrue(body.contains("healthy"))
    }
    
    @Test
    fun testModelsEndpoint() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/v1/models")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        assertTrue(body.contains("gemma-4e4b"))
    }
    
    @Test
    fun testNonStreamingChatCompletion() = testApplication {
        application {
            module()
        }
        
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(json)
            }
        }
        
        val request = ChatCompletionRequest(
            model = "gemma-4e4b",
            messages = listOf(
                ChatMessage(role = "user", content = "Hello!")
            ),
            stream = false
        )
        
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val completion = json.decodeFromString(ChatCompletionResponse.serializer(), response.bodyAsText())
        assertEquals("gemma-4e4b", completion.model)
        assertTrue(completion.choices.isNotEmpty())
        assertEquals("assistant", completion.choices[0].message?.role)
    }
}

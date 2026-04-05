package com.loa.momclaw.bridge

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mock LlmEngineWrapper for testing without actual LiteRT model
 */
class MockLlmEngineWrapper : LlmEngineWrapper(null) {
    private var isModelLoaded = false
    
    override suspend fun loadModel(path: String): Boolean {
        isModelLoaded = true
        return true
    }
    
    override fun isReady(): Boolean = isModelLoaded
    
    override fun getModelInfo(): Map<String, Any?> = mapOf(
        "name" to "mock-gemma-4e4b",
        "path" to "/mock/path/model.litertlm",
        "loaded" to isModelLoaded,
        "type" to "Mock-LiteRT-LM",
        "tokenCount" to 4096
    )
    
    override fun formatPrompt(messages: List<ChatMessage>): String {
        return messages.joinToString("\n") { "${it.role}: ${it.content}" }
    }
    
    override suspend fun generate(request: LiteRTRequest): LiteRTResponseChunk {
        return LiteRTResponseChunk(
            text = "Mock response for: ${request.prompt.take(50)}...",
            isComplete = true,
            tokensGenerated = 10
        )
    }
    
    override fun generateStreaming(request: LiteRTRequest): Flow<LiteRTResponseChunk> = flow {
        val words = listOf("Mock", " streaming", " response", " for", " test")
        words.forEach { word ->
            emit(LiteRTResponseChunk(text = word, isComplete = false, tokensGenerated = 1))
            kotlinx.coroutines.delay(100)
        }
        emit(LiteRTResponseChunk(text = "", isComplete = true, tokensGenerated = words.size))
    }
    
    override fun close() {
        isModelLoaded = false
    }
}

class LiteRTBridgeTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val mockEngine = MockLlmEngineWrapper()
    
    /**
     * Test health endpoint returns healthy status
     */
    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            moduleInner(mockEngine, json)
        }
        
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        assertTrue(body.contains("healthy"))
    }
    
    /**
     * Test models endpoint returns model info
     */
    @Test
    fun testModelsEndpoint() = testApplication {
        application {
            moduleInner(mockEngine, json)
        }
        
        val response = client.get("/v1/models")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        assertTrue(body.contains("mock-gemma-4e4b"))
    }
    
    /**
     * Test non-streaming chat completion
     */
    @Test
    fun testNonStreamingChatCompletion() = testApplication {
        application {
            moduleInner(mockEngine, json)
        }
        
        val client = createClient {
            install(ContentNegotiation) {
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
        
        val body = response.bodyAsText()
        assertTrue(body.contains("Mock response"))
    }
    
    /**
     * Test streaming chat completion
     */
    @Test
    fun testStreamingChatCompletion() = testApplication {
        application {
            moduleInner(mockEngine, json)
        }
        
        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val request = ChatCompletionRequest(
            model = "gemma-4e4b",
            messages = listOf(
                ChatMessage(role = "user", content = "Hello!")
            ),
            stream = true
        )
        
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val body = response.bodyAsText()
        assertTrue(body.contains("data:"))
    }
    
    /**
     * Test completions endpoint returns not implemented
     */
    @Test
    fun testCompletionsNotImplemented() = testApplication {
        application {
            moduleInner(mockEngine, json)
        }
        
        val response = client.post("/v1/completions") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        
        assertEquals(HttpStatusCode.NotImplemented, response.status)
    }
    
    /**
     * Test service unavailable when model not loaded
     */
    @Test
    fun testServiceUnavailableWhenModelNotLoaded() = testApplication {
        val unloadedMock = MockLlmEngineWrapper()
        // Don't load model - isReady should return false
        
        application {
            moduleInner(unloadedMock, json)
        }
        
        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val request = ChatCompletionRequest(
            model = "gemma-4e4b",
            messages = listOf(ChatMessage(role = "user", content = "Hello!")),
            stream = false
        )
        
        val response = client.post("/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("Model not loaded"))
    }
}

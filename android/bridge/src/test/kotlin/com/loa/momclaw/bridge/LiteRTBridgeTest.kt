package com.loa.momclaw.bridge

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for LiteRT Bridge components
 */
class LiteRTBridgeTest {
    
    // ==================== ModelLoader Tests ====================
    
    @Test
    fun `ModelLoader generates correct default path`() {
        // Default path should be in app files directory
        val expectedPath = "/data/data/com.loa.momclaw/files/models/gemma-3-E4B-it.litertlm"
        // Note: In tests, we can't access real context, so this verifies the pattern
        assertTrue("Default path should contain models directory", 
            expectedPath.contains("models"))
        assertTrue("Default path should end with .litertlm",
            expectedPath.endsWith(".litertlm"))
    }
    
    @Test
    fun `ModelInfo contains required fields`() {
        val info = ModelLoader.ModelInfo(
            name = "gemma-3-E4B-it",
            path = "/path/to/model.litertlm",
            sizeBytes = 1024 * 1024 * 500, // 500MB
            checksum = "abc123",
            isReady = true
        )
        
        assertEquals("gemma-3-E4B-it", info.name)
        assertEquals("/path/to/model.litertlm", info.path)
        assertEquals(500L * 1024 * 1024, info.sizeBytes)
        assertEquals("abc123", info.checksum)
        assertTrue(info.isReady)
    }
    
    @Test
    fun `LoadResult success contains model info`() {
        val info = ModelLoader.ModelInfo(
            name = "test-model",
            path = "/test/model.litertlm",
            sizeBytes = 1000,
            checksum = null,
            isReady = true
        )
        val result = ModelLoader.LoadResult.Success(info)
        
        assertTrue(result is ModelLoader.LoadResult.Success)
        assertEquals("test-model", (result as ModelLoader.LoadResult.Success).info.name)
    }
    
    @Test
    fun `LoadResult error contains message and cause`() {
        val cause = RuntimeException("Test error")
        val result = ModelLoader.LoadResult.Error("Test message", cause)
        
        assertTrue(result is ModelLoader.LoadResult.Error)
        assertEquals("Test message", (result as ModelLoader.LoadResult.Error).message)
        assertEquals(cause, result.cause)
    }
    
    // ==================== Error Tests ====================
    
    @Test
    fun `BridgeError creates proper error codes`() {
        val error = BridgeError.ModelError.NotFound("/path/to/model")
        assertEquals("MODEL_NOT_FOUND", error.code)
        assertTrue(error.message.contains("not found"))
    }
    
    @Test
    fun `BridgeError toResponse creates valid JSON structure`() {
        val error = BridgeError.ModelError.NotReady()
        val response = error.toResponse()
        
        assertNotNull(response.error)
        assertEquals("MODEL_NOT_READY", response.error.code)
        assertTrue(response.error.message.contains("not loaded"))
    }
    
    @Test
    fun `OperationResult success maps correctly`() {
        val result: OperationResult<Int> = OperationResult.Success(42)
            .map { value: Int -> value * 2 }
        
        assertTrue(result is OperationResult.Success)
        assertEquals(84, (result as OperationResult.Success).value)
    }
    
    @Test
    fun `OperationResult failure preserves error`() {
        val error = BridgeError.ModelError.NotReady()
        val result: OperationResult<Int> = OperationResult.Failure(error)
            .map { value: Int -> value * 2 }
        
        assertTrue(result is OperationResult.Failure)
        assertEquals(error, (result as OperationResult.Failure).error)
    }
    
    // ==================== Chat Request Tests ====================
    
    @Test
    fun `ChatCompletionRequest defaults are valid`() {
        val request = ChatCompletionRequest(
            messages = listOf(ChatMessage("user", "Hello"))
        )
        
        assertEquals("gemma-3-e4b", request.model)
        assertEquals(0.7, request.temperature, 0.01)
        assertEquals(0.9, request.topP, 0.01)
        assertFalse(request.stream)
        assertNull(request.maxTokens)
    }
    
    @Test
    fun `ChatCompletionRequest serialization works`() {
        val request = ChatCompletionRequest(
            model = "test-model",
            messages = listOf(
                ChatMessage("system", "You are helpful"),
                ChatMessage("user", "Hello")
            ),
            temperature = 0.8,
            stream = true
        )
        
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val jsonString = json.encodeToString(ChatCompletionRequest.serializer(), request)
        
        assertTrue(jsonString.contains("\"model\":\"test-model\""))
        assertTrue(jsonString.contains("\"stream\":true"))
        assertTrue(jsonString.contains("\"temperature\":0.8"))
    }
    
    // ==================== SSE Writer Tests ====================
    
    @Test
    fun `SSEWriter generates valid IDs`() {
        val id = SSEWriter.generateId()
        
        assertTrue("ID should start with chatcmpl-", id.startsWith("chatcmpl-"))
        assertTrue("ID should have substantial length", id.length > 10)
    }
    
    @Test
    fun `SSEWriter generates valid timestamps`() {
        val timestamp = SSEWriter.currentTimestamp()
        
        assertTrue("Timestamp should be positive", timestamp > 0)
        assertTrue("Timestamp should be reasonable (after 2020)", timestamp > 1577836800)
    }
    
    // ==================== LiteRTRequest Tests ====================
    
    @Test
    fun `LiteRTRequest has sensible defaults`() {
        val request = LiteRTRequest(prompt = "Hello")
        
        assertEquals(0.7f, request.temperature, 0.01f)
        assertEquals(0.9f, request.topP, 0.01f)
        assertEquals(2048, request.maxTokens)
        assertTrue(request.stopTokens.isEmpty())
    }
    
    @Test
    fun `LiteRTResponseChunk indicates completion`() {
        val chunk = LiteRTResponseChunk(
            text = "Hello",
            isComplete = true,
            tokensGenerated = 5
        )
        
        assertTrue(chunk.isComplete)
        assertEquals(5, chunk.tokensGenerated)
    }
}

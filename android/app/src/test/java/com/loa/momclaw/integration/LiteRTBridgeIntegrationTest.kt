package com.loa.momclaw.integration

import com.loa.momclaw.bridge.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for LiteRT Bridge data models
 */
class LiteRTBridgeIntegrationTest {

    @Test
    fun testLiteRTRequestFormat() {
        val request = LiteRTRequest(
            prompt = "Hello, world!",
            temperature = 0.7f,
            topP = 0.95f,
            maxTokens = 1024,
            stopTokens = listOf("END", "STOP")
        )
        
        assertEquals("Hello, world!", request.prompt)
        assertEquals(0.7f, request.temperature)
        assertEquals(1024, request.maxTokens)
        assertEquals(2, request.stopTokens.size)
    }

    @Test
    fun testLiteRTResponseChunkComplete() {
        val chunk = LiteRTResponseChunk(
            text = "Response text",
            isComplete = true,
            tokensGenerated = 5
        )
        
        assertTrue(chunk.isComplete)
        assertEquals(5, chunk.tokensGenerated)
    }

    @Test
    fun testChatCompletionRequestStructure() {
        val request = ChatCompletionRequest(
            model = "gemma-4e4b",
            messages = listOf(
                ChatMessage(role = "user", content = "Hello"),
                ChatMessage(role = "assistant", content = "Hi!")
            ),
            temperature = 0.8,
            stream = true
        )
        
        assertEquals("gemma-4e4b", request.model)
        assertEquals(2, request.messages.size)
        assertTrue(request.stream)
    }
}

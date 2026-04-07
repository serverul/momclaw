package com.loa.momclaw.test.fixtures

import com.loa.momclaw.data.local.database.MessageEntity
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.domain.model.ChatMessage
import java.util.UUID

/**
 * Test data fixtures for MomClAW tests
 * Provides pre-built test data for consistent testing across all test classes
 */
object TestFixtures {

    // ==================== Agent Config Fixtures ====================

    val DEFAULT_CONFIG = AgentConfig.DEFAULT

    val CUSTOM_CONFIG = AgentConfig(
        systemPrompt = "Test system prompt for unit testing",
        temperature = 0.7f,
        maxTokens = 2048,
        modelPath = "/test/models/gemma-test.litertlm",
        baseUrl = "http://localhost:8080"
    )

    val LOW_TEMPERATURE_CONFIG = AgentConfig(
        systemPrompt = "Precise mode",
        temperature = 0.1f,
        maxTokens = 4096,
        modelPath = "/test/models/gemma-test.litertlm",
        baseUrl = "http://localhost:8080"
    )

    val HIGH_TEMPERATURE_CONFIG = AgentConfig(
        systemPrompt = "Creative mode",
        temperature = 1.5f,
        maxTokens = 2048,
        modelPath = "/test/models/gemma-test.litertlm",
        baseUrl = "http://localhost:8080"
    )

    // ==================== Chat Message Fixtures ====================

    fun createUserMessage(content: String = "Test user message") = ChatMessage(
        content = content,
        isUser = true,
        isStreaming = false,
        isComplete = true
    )

    fun createAssistantMessage(content: String = "Test assistant response") = ChatMessage(
        content = content,
        isUser = false,
        isStreaming = false,
        isComplete = true
    )

    fun createStreamingMessage(content: String = "") = ChatMessage(
        content = content,
        isUser = false,
        isStreaming = true,
        isComplete = false
    )

    val SIMPLE_CONVERSATION = listOf(
        createUserMessage("Hello"),
        createAssistantMessage("Hi there! How can I help you?"),
        createUserMessage("What is 2+2?"),
        createAssistantMessage("2+2 equals 4.")
    )

    val LONG_CONVERSATION = (1..50).flatMap { i ->
        listOf(
            createUserMessage("Question $i"),
            createAssistantMessage("Answer $i with some detailed response content")
        )
    }

    // ==================== Message Entity Fixtures ====================

    fun createMessageEntity(
        content: String = "Test message",
        isUser: Boolean = true,
        conversationId: String = TEST_CONVERSATION_ID
    ) = MessageEntity(
        content = content,
        isUser = isUser,
        timestamp = System.currentTimeMillis(),
        conversationId = conversationId
    )

    val TEST_CONVERSATION_ID = "test-conv-${UUID.randomUUID()}"

    fun createConversationEntities(count: Int, conversationId: String = TEST_CONVERSATION_ID): List<MessageEntity> {
        return (1..count).map { i ->
            createMessageEntity(
                content = "Message $i",
                isUser = i % 2 != 0,
                conversationId = conversationId
            )
        }
    }

    // ==================== Token Stream Fixtures ====================

    val SHORT_TOKEN_STREAM = listOf("Hello", "!", " How", " are", " you", "?")
    val MEDIUM_TOKEN_STREAM = (1..50).map { "token$it" }
    val LONG_TOKEN_STREAM = (1..500).map { "token$it" }

    // ==================== Error Fixtures ====================

    val NETWORK_ERROR = Exception("Network error: Connection refused")
    val TIMEOUT_ERROR = Exception("Timeout: Connection timed out after 30000ms")
    val MODEL_NOT_FOUND_ERROR = Exception("Model not found: /path/to/model.litertlm")
    val OOM_ERROR = Exception("OutOfMemoryError: Failed to allocate model memory")
    val SERVICE_UNAVAILABLE_ERROR = Exception("HTTP 503: Service Unavailable")
    val CRASH_ERROR = Exception("Process died unexpectedly (exit code 137)")
    val DISK_FULL_ERROR = Exception("IOException: No space left on device")
    val CORRUPT_MODEL_ERROR = Exception("Invalid model format: corrupted header")

    // ==================== Performance Thresholds ====================

    object Thresholds {
        const val MIN_TOKENS_PER_SECOND = 10.0
        const val MAX_STARTUP_TIME_MS = 3000L
        const val MAX_MESSAGE_SEND_MS = 5000L
        const val MAX_STREAMING_LATENCY_MS = 100L
        const val MIN_THROUGHPUT_MSG_PER_SEC = 5.0
        const val MAX_DB_OP_MS = 100L
    }

    // ==================== Startup Check Fixtures ====================

    val ALL_24_STARTUP_CHECKS = listOf(
        // Inference Service (8)
        "inference_process_started",
        "inference_http_endpoint_ready",
        "inference_model_loaded",
        "inference_memory_allocated",
        "inference_health_endpoint_responding",
        "inference_chat_endpoint_responding",
        "inference_streaming_working",
        "inference_metrics_available",
        // Agent Service (8)
        "agent_process_started",
        "agent_http_endpoint_ready",
        "agent_config_loaded",
        "agent_inference_connection_established",
        "agent_health_endpoint_responding",
        "agent_chat_endpoint_responding",
        "agent_streaming_working",
        "agent_tools_available",
        // Integration (8)
        "database_accessible",
        "preferences_accessible",
        "ui_initialized",
        "navigation_working",
        "message_persistence_working",
        "settings_persistence_working",
        "error_handling_working",
        "logging_working"
    )
}

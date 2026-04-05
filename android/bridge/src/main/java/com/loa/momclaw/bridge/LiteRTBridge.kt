package com.loa.momclaw.bridge

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * LiteRT Bridge Server
 * 
 * Provides an OpenAI-compatible HTTP API for on-device inference using LiteRT.
 * This allows NullClaw or any OpenAI-compatible client to use local models.
 * 
 * Current implementation is a MOCK - replace LlmEngineWrapper calls with actual
 * LiteRT SDK when available.
 */
class LiteRTBridge(
    private val port: Int = 8080,
    private val modelPath: String = "gemma-4e4b.litertlm"
) {
    private lateinit var server: ApplicationEngine
    private val llmEngine = LlmEngineWrapper(modelPath)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * Start the HTTP server
     */
    suspend fun start() {
        logger.info { "Starting LiteRT Bridge on port $port" }
        
        // Load the model first
        llmEngine.loadModel()
        
        server = embeddedServer(Netty, port = port, module = Application::module)
            .start(wait = false)
        
        logger.info { "LiteRT Bridge started at http://localhost:$port" }
    }
    
    /**
     * Stop the server
     */
    fun stop() {
        logger.info { "Stopping LiteRT Bridge" }
        llmEngine.close()
        if (::server.isInitialized) {
            server.stop(1000, 2000)
        }
    }
}

/**
 * Ktor Application module
 */
fun Application.module() {
    val llmEngine = LlmEngineWrapper()
    val json = Json { ignoreUnknownKeys = true }
    
    // Load model on startup
    application.launch {
        llmEngine.loadModel()
    }
    
    // Install plugins
    install(ContentNegotiation) {
        json(json)
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    
    install(SSE)
    
    // Routing
    routing {
        // Health check
        get("/health") {
            call.respond(mapOf(
                "status" to "healthy",
                "model_loaded" to llmEngine.isReady(),
                "model" to llmEngine.getModelInfo()
            ))
        }
        
        // Model info
        get("/v1/models") {
            call.respond(mapOf(
                "object" to "list",
                "data" to listOf(
                    mapOf(
                        "id" to "gemma-4e4b",
                        "object" to "model",
                        "created" to System.currentTimeMillis() / 1000,
                        "owned_by" to "google"
                    )
                )
            ))
        }
        
        // Chat completions endpoint
        post("/v1/chat/completions") {
            val request = call.receive<ChatCompletionRequest>()
            logger.info { "Received chat request: ${request.messages.size} messages, stream=${request.stream}" }
            
            if (!llmEngine.isReady()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "Model not loaded")
                )
                return@post
            }
            
            // Convert to LiteRT format
            val prompt = llmEngine.formatPrompt(request.messages)
            val litertRequest = LiteRTRequest(
                prompt = prompt,
                temperature = request.temperature.toFloat(),
                topP = request.topP.toFloat(),
                maxTokens = request.maxTokens ?: 2048,
                stopTokens = request.stop ?: emptyList()
            )
            
            if (request.stream) {
                // Streaming response using SSE
                sse {
                    val responseId = SSEWriter.generateId()
                    val created = SSEWriter.currentTimestamp()
                    var tokensGenerated = 0
                    
                    llmEngine.generateStreaming(litertRequest).collect { chunk ->
                        tokensGenerated = chunk.tokensGenerated
                        
                        val response = ChatCompletionResponse(
                            id = responseId,
                            created = created,
                            model = "gemma-4e4b",
                            choices = listOf(
                                ChatChoice(
                                    index = 0,
                                    delta = ChatDelta(
                                        role = if (tokensGenerated == 1) "assistant" else null,
                                        content = chunk.text
                                    ),
                                    finishReason = if (chunk.isComplete) "stop" else null
                                )
                            )
                        )
                        
                        send(data = json.encodeToString(ChatCompletionResponse.serializer(), response))
                        
                        if (chunk.isComplete) {
                            close()
                        }
                    }
                }
            } else {
                // Non-streaming response
                val response = llmEngine.generate(litertRequest)
                val responseId = SSEWriter.generateId()
                val created = SSEWriter.currentTimestamp()
                
                call.respond(ChatCompletionResponse(
                    id = responseId,
                    created = created,
                    model = "gemma-4e4b",
                    choices = listOf(
                        ChatChoice(
                            index = 0,
                            message = ChatMessage(
                                role = "assistant",
                                content = response.text
                            ),
                            finishReason = "stop"
                        )
                    ),
                    usage = Usage(
                        promptTokens = 50,  // MOCK
                        completionTokens = response.tokensGenerated,
                        totalTokens = 50 + response.tokensGenerated
                    )
                ))
            }
        }
        
        // Completions endpoint (for compatibility)
        post("/v1/completions") {
            call.respond(
                HttpStatusCode.NotImplemented,
                mapOf("error" to "Use /v1/chat/completions instead")
            )
        }
    }
}

/**
 * Main entry point
 */
suspend fun main() {
    val bridge = LiteRTBridge(port = 8080)
    
    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        bridge.stop()
    })
    
    bridge.start()
    
    // Keep running
    Thread.currentThread().join()
}

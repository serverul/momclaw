package com.loa.momclaw.bridge

import android.content.Context
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * LiteRT Bridge Server
 * 
 * Provides an OpenAI-compatible HTTP API for on-device inference using LiteRT.
 * This allows NullClaw or any OpenAI-compatible client to use local models.
 * 
 * Architecture:
 *   NullClaw → HTTP POST /v1/chat/completions → LiteRTBridge → LiteRT-LM → SSE Response
 * 
 * Model path is configured at startup via loadModel().
 */
class LiteRTBridge(
    private val context: Context,
    private val port: Int = 8080
) {
    private lateinit var server: ApplicationEngine
    private val engine = LlmEngineWrapper(context)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * Load model and start the HTTP server
     */
    suspend fun start(modelPath: String) {
        val loaded = engine.loadModel(modelPath)
        if (!loaded) {
            throw IllegalArgumentException("Failed to load model from: $modelPath")
        }
        
        logger.info { "Starting LiteRT Bridge on port $port" }
        
        server = embeddedServer(Netty, port = port, module = {
            moduleInner(engine, json)
        }).start(wait = false)
        
        logger.info { "LiteRT Bridge started at http://localhost:$port" }
    }
    
    /**
     * Start without model (model must be loaded separately)
     */
    suspend fun startServer() {
        logger.info { "Starting LiteRT Bridge on port $port (no model loaded)" }
        
        server = embeddedServer(Netty, port = port, module = {
            moduleInner(engine, json)
        }).start(wait = false)
        
        logger.info { "LiteRT Bridge started at http://localhost:$port" }
    }

    /**
     * Load or reload a model after server start
     */
    suspend fun loadModel(modelPath: String): Boolean {
        return engine.loadModel(modelPath)
    }
    
    /**
     * Stop the server and release model resources
     */
    fun stop() {
        logger.info { "Stopping LiteRT Bridge" }
        engine.close()
        if (::server.isInitialized) {
            server.stop(1000, 2000)
        }
    }
    
    fun isModelReady(): Boolean = engine.isReady()
}

/**
 * Ktor Application module — uses real LiteRT engine
 */
fun Application.moduleInner(
    llmEngine: LlmEngineWrapper,
    json: Json
) {
    install(ContentNegotiation) {
        json(json)
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    
    install(SSE)
    
    routing {
        // Health check
        get("/health") {
            call.respond(mapOf(
                "status" to "healthy",
                "model_loaded" to llmEngine.isReady(),
                "model" to llmEngine.getModelInfo()
            ))
        }
        
        // Model info (OpenAI-compatible)
        get("/v1/models") {
            val modelInfo = llmEngine.getModelInfo()
            call.respond(mapOf(
                "object" to "list",
                "data" to listOf(
                    mapOf(
                        "id" to (modelInfo["name"] ?: "unknown"),
                        "object" to "model",
                        "created" to System.currentTimeMillis() / 1000,
                        "owned_by" to "google",
                        "path" to (modelInfo["path"] ?: ""),
                        "loaded" to (modelInfo["loaded"] ?: false)
                    )
                )
            ))
        }
        
        // Chat completions endpoint (OpenAI-compatible)
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
            
            val prompt = llmEngine.formatPrompt(request.messages)
            val litertRequest = LiteRTRequest(
                prompt = prompt,
                temperature = request.temperature.toFloat().coerceIn(0.0f, 2.0f),
                topP = request.topP.toFloat().coerceIn(0.0f, 1.0f),
                maxTokens = request.maxTokens ?: 2048,
                stopTokens = request.stop ?: emptyList()
            )
            
            if (request.stream) {
                // Streaming response via SSE
                sse {
                    val responseId = SSEWriter.generateId()
                    val created = SSEWriter.currentTimestamp()
                    var tokensGenerated = 0
                    
                    llmEngine.generateStreaming(litertRequest).collect { chunk ->
                        tokensGenerated++
                        
                        val response = ChatCompletionResponse(
                            id = responseId,
                            created = created,
                            model = llmEngine.getModelInfo()["name"] as? String ?: "litert",
                            choices = listOf(
                                ChatChoice(
                                    index = 0,
                                    delta = ChatDelta(
                                        role = if (tokensGenerated == 1) "assistant" else null,
                                        content = chunk.text
                                    ),
                                    finishReason = if (chunk.isComplete) "stop" else null
                                )
                            ),
                            usage = if (chunk.isComplete) Usage(
                                promptTokens = 0,
                                completionTokens = tokensGenerated,
                                totalTokens = tokensGenerated
                            ) else null
                        )
                        
                        send(
                            data = json.encodeToString(
                                ChatCompletionResponse.serializer(),
                                response
                            )
                        )
                        
                        if (chunk.isComplete) {
                            close()
                        }
                    }
                }
            } else {
                // Non-streaming response
                val response = runBlocking { llmEngine.generate(litertRequest) }
                val responseId = SSEWriter.generateId()
                val created = SSEWriter.currentTimestamp()
                
                call.respond(ChatCompletionResponse(
                    id = responseId,
                    created = created,
                    model = llmEngine.getModelInfo()["name"] as? String ?: "litert",
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
                        promptTokens = 0,
                        completionTokens = response.tokensGenerated,
                        totalTokens = response.tokensGenerated
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
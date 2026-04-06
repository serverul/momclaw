package com.loa.momclaw.bridge

import android.content.Context
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger

/**
 * LiteRT Bridge Server
 * 
 * Provides an OpenAI-compatible HTTP API for on-device inference using LiteRT.
 * This allows NullClaw or any OpenAI-compatible client to use local models.
 * 
 * Architecture:
 *   NullClaw → HTTP POST /v1/chat/completions → LiteRTBridge → LiteRT-LM → SSE Response
 * 
 * Features:
 *   - Model loading from device storage
 *   - Streaming and non-streaming responses
 *   - Health monitoring
 *   - Error handling with detailed messages
 *   - Process lifecycle management
 * 
 * Model: Gemma 3 E4B IT (litert-community/gemma-3-E4B-it-litertlm)
 */
class LiteRTBridge(
    private val context: Context,
    private val port: Int = 8080
) {
    private var server: ApplicationEngine? = null
    private val engine = LlmEngineWrapper(context)
    private val modelLoader = ModelLoader(context)
    private val healthMonitor = HealthMonitor(context)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    
    @Volatile private var isServerRunning = false
    @Volatile private var modelLoadTime: Long? = null
    
    /**
     * Load model and start the HTTP server
     */
    suspend fun start(modelPath: String): Result<Unit> {
        return try {
            // Verify model first
            val modelResult = modelLoader.verifyModel(modelPath)
            if (modelResult is ModelLoader.LoadResult.Error) {
                return Result.failure(
                    BridgeError.ModelError.LoadFailed(
                        modelPath, 
                        modelResult.message,
                        modelResult.cause
                    )
                )
            }
            
            val modelInfo = (modelResult as ModelLoader.LoadResult.Success).info
            
            // Check memory
            val requiredMemoryMB = modelInfo.sizeBytes / (1024 * 1024) * 2 // 2x safety margin
            if (!healthMonitor.canLoadModel(requiredMemoryMB)) {
                return Result.failure(
                    BridgeError.ModelError.InsufficientMemory(
                        requiredMemoryMB * 1024 * 1024,
                        healthMonitor.getMemoryInfo().availableMB * 1024 * 1024
                    )
                )
            }
            
            // Load model
            val loadStart = System.currentTimeMillis()
            val loaded = engine.loadModel(modelPath)
            modelLoadTime = System.currentTimeMillis() - loadStart
            
            if (!loaded) {
                return Result.failure(
                    BridgeError.ModelError.LoadFailed(modelPath, "LiteRT failed to load model")
                )
            }
            
            logger.info { "Model loaded: ${modelInfo.name} (${modelInfo.sizeBytes / (1024 * 1024)}MB) in ${modelLoadTime}ms" }
            
            // Start server
            startServer()
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start LiteRT Bridge" }
            Result.failure(e)
        }
    }
    
    /**
     * Start server without model (model must be loaded separately)
     */
    suspend fun startServer() {
        if (isServerRunning) {
            logger.warn { "Server already running on port $port" }
            return
        }
        
        logger.info { "Starting LiteRT Bridge on port $port" }
        
        server = embeddedServer(Netty, port = port, module = {
            install(StatusPages) {
                exception<BridgeError> { call, error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        error.toResponse()
                    )
                }
                exception<Exception> { call, error ->
                    logger.error(error) { "Unhandled exception" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(
                            ErrorDetail("INTERNAL_ERROR", error.message ?: "Unknown error")
                        )
                    )
                }
            }
            moduleInner(engine, json, healthMonitor)
        }).start(wait = false)
        
        isServerRunning = true
        healthMonitor.recordStart(port)
        
        logger.info { "LiteRT Bridge started at http://localhost:$port" }
    }

    /**
     * Load or reload a model after server start
     */
    suspend fun loadModel(modelPath: String): Boolean {
        val loadStart = System.currentTimeMillis()
        val result = engine.loadModel(modelPath)
        modelLoadTime = System.currentTimeMillis() - loadStart
        
        if (result) {
            logger.info { "Model loaded from $modelPath in ${modelLoadTime}ms" }
        } else {
            logger.error { "Failed to load model from $modelPath" }
        }
        
        return result
    }
    
    /**
     * Stop the server and release model resources
     */
    fun stop() {
        logger.info { "Stopping LiteRT Bridge" }
        
        engine.close()
        server?.stop(1000, 2000)
        server = null
        isServerRunning = false
        healthMonitor.recordStop()
        
        logger.info { "LiteRT Bridge stopped" }
    }
    
    /**
     * Check if model is ready for inference
     */
    fun isModelReady(): Boolean = engine.isReady()
    
    /**
     * Check if server is running
     */
    fun isServerRunning(): Boolean = isServerRunning
    
    /**
     * Get health status
     */
    suspend fun getHealthStatus(): HealthMonitor.HealthStatus {
        val modelInfo = engine.getModelInfo()
        return healthMonitor.checkHealth(
            serverRunning = isServerRunning,
            port = if (isServerRunning) port else null,
            modelLoaded = modelInfo["loaded"] as? Boolean ?: false,
            modelName = modelInfo["name"] as? String,
            modelPath = modelInfo["path"] as? String,
            modelLoadTime = modelLoadTime
        )
    }
    
    /**
     * Get model loader for external model management
     */
    fun getModelLoader(): ModelLoader = modelLoader
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stop()
    }
}

/**
 * Ktor Application module — uses real LiteRT engine
 */
fun Application.moduleInner(
    llmEngine: LlmEngineWrapper,
    json: Json,
    healthMonitor: HealthMonitor
) {
    install(ContentNegotiation) {
        json(json)
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.CacheControl)
        allowHeader(HttpHeaders.Accept)
    }
    
    routing {
        // Health check endpoint
        get("/health") {
            healthMonitor.recordRequest()
            val modelInfo = llmEngine.getModelInfo()
            
            val health = healthMonitor.checkHealth(
                serverRunning = true,
                port = 8080,
                modelLoaded = modelInfo["loaded"] as? Boolean ?: false,
                modelName = modelInfo["name"] as? String,
                modelPath = modelInfo["path"] as? String,
                modelLoadTime = null
            )
            
            call.respond(health.toResponse())
        }
        
        // Detailed health check
        get("/health/details") {
            healthMonitor.recordRequest()
            val health = runBlocking { 
                healthMonitor.checkHealth(
                    serverRunning = true,
                    port = 8080,
                    modelLoaded = llmEngine.isReady(),
                    modelName = llmEngine.getModelInfo()["name"] as? String,
                    modelPath = llmEngine.getModelInfo()["path"] as? String,
                    modelLoadTime = null
                )
            }
            call.respond(health.toResponse())
        }
        
        // Model info (OpenAI-compatible)
        get("/v1/models") {
            healthMonitor.recordRequest()
            val modelInfo = llmEngine.getModelInfo()
            call.respond(mapOf(
                "object" to "list",
                "data" to listOf(
                    mapOf(
                        "id" to (modelInfo["name"] ?: "gemma-4e4b"),
                        "object" to "model",
                        "created" to System.currentTimeMillis() / 1000,
                        "owned_by" to "google",
                        "path" to (modelInfo["path"] ?: ""),
                        "loaded" to (modelInfo["loaded"] ?: false),
                        "type" to (modelInfo["type"] ?: "LiteRT-LM")
                    )
                )
            ))
        }
        
        // Load model endpoint
        post("/v1/models/load") {
            val request = call.receive<ModelLoadRequest>()
            healthMonitor.recordRequest()
            
            val result = llmEngine.loadModel(request.path)
            if (result) {
                call.respond(mapOf(
                    "status" to "loaded",
                    "path" to request.path,
                    "model" to llmEngine.getModelInfo()
                ))
            } else {
                healthMonitor.recordError()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(ErrorDetail("MODEL_LOAD_FAILED", "Failed to load model from ${request.path}"))
                )
            }
        }
        
        // Unload model endpoint
        post("/v1/models/unload") {
            healthMonitor.recordRequest()
            llmEngine.close()
            call.respond(mapOf("status" to "unloaded"))
        }
        
        // Chat completions endpoint (OpenAI-compatible)
        post("/v1/chat/completions") {
            val request = call.receive<ChatCompletionRequest>()
            healthMonitor.recordRequest()
            
            logger.info { "Chat request: ${request.messages.size} messages, stream=${request.stream}" }
            
            // Validate request
            if (request.messages.isEmpty()) {
                healthMonitor.recordError()
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(ErrorDetail("INVALID_REQUEST", "Messages cannot be empty"))
                )
                return@post
            }
            
            if (!llmEngine.isReady()) {
                healthMonitor.recordError()
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ErrorResponse(ErrorDetail("MODEL_NOT_READY", "Model not loaded. Use POST /v1/models/load first."))
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
            
            try {
                if (request.stream) {
                    // Streaming response via SSE
                    val responseId = SSEWriter.generateId()
                    val created = SSEWriter.currentTimestamp()
                    var tokensGenerated = 0
                    
                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        llmEngine.generateStreaming(litertRequest).collect { chunk ->
                            tokensGenerated++
                            
                            val response = ChatCompletionResponse(
                                id = responseId,
                                created = created,
                                model = llmEngine.getModelInfo()["name"] as? String ?: "gemma-4e4b",
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
                            
                            write("data: ${json.encodeToString(ChatCompletionResponse.serializer(), response)}\n\n")
                            flush()
                            
                            if (chunk.isComplete) {
                                write("data: [DONE]\n\n")
                                flush()
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
                        model = llmEngine.getModelInfo()["name"] as? String ?: "gemma-4e4b",
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
            } catch (e: Exception) {
                healthMonitor.recordError()
                logger.error(e) { "Generation failed" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(ErrorDetail("GENERATION_FAILED", e.message ?: "Generation failed"))
                )
            }
        }
        
        // Completions endpoint (for compatibility)
        post("/v1/completions") {
            call.respond(
                HttpStatusCode.NotImplemented,
                ErrorResponse(ErrorDetail("NOT_IMPLEMENTED", "Use /v1/chat/completions instead"))
            )
        }
        
        // Metrics endpoint
        get("/metrics") {
            val modelInfo = llmEngine.getModelInfo()
            val memoryInfo = healthMonitor.getMemoryInfo()
            
            call.respond(mapOf(
                "model" to mapOf(
                    "loaded" to modelInfo["loaded"],
                    "name" to modelInfo["name"],
                    "type" to modelInfo["type"]
                ),
                "memory" to mapOf(
                    "available_mb" to memoryInfo.availableMB,
                    "total_mb" to memoryInfo.totalMB,
                    "low_memory" to memoryInfo.lowMemory
                )
            ))
        }
    }
}

/**
 * Request for loading a model
 */
@kotlinx.serialization.Serializable
data class ModelLoadRequest(
    val path: String
)

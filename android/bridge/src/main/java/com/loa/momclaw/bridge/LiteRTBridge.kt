package com.loa.momclaw.bridge

import android.content.Context
import android.util.Log
import com.loa.momclaw.util.MomClawLogger
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiteRT Bridge - HTTP server that exposes LiteRT-LM inference via OpenAI-compatible API.
 * 
 * Runs on localhost:8080 and provides:
 * - POST /v1/chat/completions - Chat completion endpoint (streaming)
 * - GET /health - Health check endpoint
 * - GET /v1/models - List available models
 */
@Singleton
class LiteRTBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var server: ApplicationEngine? = null
    private var llmEngine: LlmEngineWrapper = LlmEngineWrapper(context)
    private var currentModel: String? = null
    private var isRunning = false
    private var serverPort: Int = DEFAULT_PORT

    companion object {
        private const val TAG = "LiteRTBridge"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_MODEL_NAME = "gemma-4-e4b"
    }
    
    private val logger = MomClawLogger
    
    /**
     * Secondary constructor for manual instantiation (non-Hilt usage).
     * Used by InferenceService which doesn't use Hilt injection.
     */
    constructor(context: Context) : this(context)

    /**
     * Starts the LiteRT Bridge server.
     * 
     * @param modelPath Path to .litertlm model file
     * @param port Port to listen on (default 8080)
     * @return Result.success if started successfully
     */
    suspend fun start(modelPath: String, port: Int = DEFAULT_PORT): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Load model first
                llmEngine.loadModel(modelPath).getOrElse { error ->
                    return@withContext Result.failure(
                        Exception("Failed to load model: ${error.message}", error)
                    )
                }

                currentModel = DEFAULT_MODEL_NAME

                // Start Ktor server
                server = embeddedServer(Netty, port = port) {
                    install(ContentNegotiation) {
                        json()
                    }

                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.ContentType)
                        allowHeader(HttpHeaders.Authorization)
                    }

                    routing {
                        route("/v1") {
                            // Chat completion endpoint
                            post("/chat/completions") {
                                handleChatCompletion(call)
                            }

                            // Models endpoint
                            get("/models") {
                                handleListModels(call)
                            }
                        }

                        // Health check
                        get("/health") {
                            call.respond(mapOf(
                                "status" to "ok",
                                "model_loaded" to (currentModel != null),
                                "model" to (currentModel ?: "none")
                            ))
                        }
                    }
                }.start(wait = false)

                isRunning = true
                logger.i(TAG, "LiteRT Bridge started on port $port with model: $currentModel")
                
                Result.success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to start LiteRT Bridge", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Handles chat completion requests with streaming responses.
     */
    private suspend fun handleChatCompletion(call: ApplicationCall) {
        try {
            val request = call.receive<ChatRequest>()
            
            if (!llmEngine.isLoaded()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "Model not loaded")
                )
                return
            }

            // Format prompt from messages
            val prompt = PromptFormatter.formatPrompt(request.messages)

            // Set up streaming response (no caching)
            call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
            call.response.header(HttpHeaders.Connection, "keep-alive")
            
            call.respondTextWriter(ContentType.Text.EventStream) {
                // Stream tokens
                llmEngine.generate(
                    prompt = prompt,
                    temperature = request.temperature,
                    maxTokens = request.max_tokens
                ).catch { e ->
                    logger.e(TAG, "Error during generation", e)
                    write(SSEFormatter.formatErrorEvent(e.message ?: "Unknown error"))
                }.collect { token ->
                    write(SSEFormatter.formatStreamEvent(token, request.model))
                    flush()
                }

                // Send done event
                write(SSEFormatter.formatDoneEvent())
                flush()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error handling chat completion", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Internal server error"))
            )
        }
    }

    /**
     * Handles list models requests.
     */
    private suspend fun handleListModels(call: ApplicationCall) {
        val models = listOfNotNull(
            currentModel?.let { modelName ->
                mapOf(
                    "id" to modelName,
                    "object" to "model",
                    "created" to System.currentTimeMillis() / 1000,
                    "owned_by" to "local"
                )
            }
        )

        call.respond(mapOf(
            "object" to "list",
            "data" to models
        ))
    }

    /**
     * Stops the LiteRT Bridge server.
     */
    fun stop() {
        try {
            server?.stop(1000, 2000)
            server = null
            
            llmEngine.close()
            currentModel = null
            isRunning = false
            
            logger.i(TAG, "LiteRT Bridge stopped")
        } catch (e: Exception) {
            logger.e(TAG, "Error stopping LiteRT Bridge", e)
        }
    }

    /**
     * Checks if the server is currently running.
     */
    fun isRunning(): Boolean = isRunning && server?.application != null

    /**
     * Gets the currently loaded model name.
     */
    fun getCurrentModel(): String? = currentModel
}

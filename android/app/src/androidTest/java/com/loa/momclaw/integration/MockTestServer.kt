package com.loa.momclaw.integration

import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer

/**
 * Lightweight mock HTTP server for integration tests.
 * Simulates LiteRT Bridge and NullClaw Agent endpoints.
 */
class MockTestServer(private val port: Int) {

    private var server: HttpServer? = null
    private val handlers = mutableMapOf<String, (com.sun.net.httpserver.HttpExchange) -> Unit>()
    private var isRunning = false

    fun start(): MockTestServer {
        server = HttpServer.create(InetSocketAddress("localhost", port), 0).apply {
            createContext("/health") { exchange ->
                sendJson(exchange, 200, """{"status":"ok","uptime":5000}""")
            }
            createContext("/v1/models") { exchange ->
                sendJson(exchange, 200, """{"object":"list","data":[{"id":"gemma-4e4b","object":"model","loaded":true}]}""")
            }
            createContext("/v1/chat/completions") { exchange ->
                if (exchange.requestMethod == "POST") {
                    val body = exchange.requestBody.bufferedReader().readText()
                    if (body.contains("\"stream\":true") || body.contains("\"stream\": true")) {
                        handleSSE(exchange)
                    } else {
                        handleNonStreaming(exchange)
                    }
                } else {
                    sendJson(exchange, 405, """{"error":"Method not allowed"}""")
                }
            }
            createContext("/v1/models/load") { exchange ->
                sendJson(exchange, 200, """{"status":"loaded"}""")
            }
            createContext("/v1/models/unload") { exchange ->
                sendJson(exchange, 200, """{"status":"unloaded"}""")
            }
            createContext("/metrics") { exchange ->
                sendJson(exchange, 200, """{"model":{"loaded":true},"memory":{"available_mb":2048}}""")
            }
            start()
        }
        isRunning = true
        return this
    }

    private fun handleNonStreaming(exchange: com.sun.net.httpserver.HttpExchange) {
        val response = """
        {
            "id": "chatcmpl-test",
            "created": ${System.currentTimeMillis() / 1000},
            "model": "gemma-4e4b",
            "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "Hello! I am a mock response."},
                "finish_reason": "stop"
            }],
            "usage": {"prompt_tokens": 5, "completion_tokens": 8, "total_tokens": 13}
        }
        """.trimIndent()
        sendJson(exchange, 200, response)
    }

    private fun handleSSE(exchange: com.sun.net.httpserver.HttpExchange) {
        exchange.sendResponseHeaders(200, 0)
        val writer = OutputStreamWriter(exchange.responseBody)

        val tokens = listOf("Hello", " there", "!", " How", " can", " I", " help", "?")
        val id = "chatcmpl-test-sse"
        val created = System.currentTimeMillis() / 1000

        tokens.forEachIndexed { index, token ->
            val chunk = """
            {"id":"$id","created":$created,"model":"gemma-4e4b","choices":[{"index":0,"delta":{"${if (index == 0) "role\":\"assistant\",\"content" else "content"}":"$token"},"finish_reason":null}]}
            """.trimIndent()
            writer.write("data: $chunk\n\n")
            writer.flush()
            Thread.sleep(10)
        }

        writer.write("data: {\"id\":\"$id\",\"created\":$created,\"model\":\"gemma-4e4b\",\"choices\":[{\"index\":0,\"delta\":{\"content\":null},\"finish_reason\":\"stop\"}]}\n\n")
        writer.flush()
        writer.write("data: [DONE]\n\n")
        writer.flush()
        exchange.responseBody.close()
    }

    private fun sendJson(exchange: com.sun.net.httpserver.HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.close()
    }

    fun stop() {
        server?.stop(0)
        server = null
        isRunning = false
    }

    fun isRunning(): Boolean = isRunning

    companion object {
        const val LITERT_PORT = 18080
        const val NULLCLAW_PORT = 19090
    }
}

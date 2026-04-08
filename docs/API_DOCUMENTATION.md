# MOMCLAW API Documentation

**Version**: 1.0.0  
**Base URLs**: 
- LiteRT Bridge: `http://localhost:8080/v1`
- NullClaw Agent: `http://localhost:9090`

---

## 📖 Table of Contents

- [Overview](#overview)
- [LiteRT Bridge API](#litert-bridge-api)
  - [Chat Completions](#chat-completions)
  - [Streaming Responses](#streaming-responses)
  - [Models](#models)
  - [Health Check](#health-check)
- [NullClaw Agent API](#nullclaw-agent-api)
  - [Agent Chat](#agent-chat)
  - [Tool Execution](#tool-execution)
  - [Memory Management](#memory-management)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Examples](#examples)

---

## Overview

MOMCLAW provides two HTTP APIs:

1. **LiteRT Bridge API** - OpenAI-compatible API for model inference
2. **NullClaw Agent API** - Agent-specific API for tool execution and memory

Both APIs run locally on the Android device and require no external network access.

---

## LiteRT Bridge API

### Base URL
```
http://localhost:8080/v1
```

### Authentication
No authentication required (local only).

### Content Type
All requests and responses use `application/json`.

---

### Chat Completions

#### `POST /v1/chat/completions`

Generate a chat completion using the loaded model.

**Request Body:**

```json
{
  "model": "gemma-4-e4b-it",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant."
    },
    {
      "role": "user",
      "content": "Hello, how are you?"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 2048,
  "stream": false,
  "top_p": 0.95,
  "top_k": 40,
  "repeat_penalty": 1.1
}
```

**Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `model` | string | No | `gemma-4-e4b-it` | Model to use (currently only one model supported) |
| `messages` | array | Yes | - | Array of message objects |
| `temperature` | float | No | `0.7` | Sampling temperature (0.0-2.0) |
| `max_tokens` | integer | No | `2048` | Maximum tokens to generate |
| `stream` | boolean | No | `false` | Enable streaming responses |
| `top_p` | float | No | `0.95` | Nucleus sampling parameter |
| `top_k` | integer | No | `40` | Top-k sampling parameter |
| `repeat_penalty` | float | No | `1.1` | Repetition penalty (1.0-2.0) |

**Message Object:**

```json
{
  "role": "system" | "user" | "assistant",
  "content": "string"
}
```

**Response (Non-streaming):**

```json
{
  "id": "chatcmpl-1234567890",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "gemma-4-e4b-it",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! I'm doing well, thank you for asking!"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 25,
    "completion_tokens": 15,
    "total_tokens": 40
  }
}
```

**Response (Streaming):**

When `stream: true`, the response is sent as Server-Sent Events (SSE):

```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gemma-4-e4b-it","choices":[{"index":0,"delta":{"role":"assistant"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gemma-4-e4b-it","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gemma-4-e4b-it","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1234567890,"model":"gemma-4-e4b-it","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

---

### Streaming Responses

#### Event Types

- `chat.completion.chunk` - Streaming chunk with delta content
- `[DONE]` - End of stream marker

#### Delta Object

```json
{
  "role": "assistant",
  "content": "partial text"
}
```

---

### Models

#### `GET /v1/models`

List available models.

**Response:**

```json
{
  "object": "list",
  "data": [
    {
      "id": "gemma-4-e4b-it",
      "object": "model",
      "created": 1234567890,
      "owned_by": "google",
      "permission": [],
      "root": "gemma-4-e4b-it",
      "parent": null
    }
  ]
}
```

#### `GET /v1/models/{model}`

Get details about a specific model.

**Response:**

```json
{
  "id": "gemma-4-e4b-it",
  "object": "model",
  "created": 1234567890,
  "owned_by": "google",
  "permission": [],
  "root": "gemma-4-e4b-it",
  "parent": null,
  "meta": {
    "quantization": "Q4_K_M",
    "context_length": 8192,
    "file_size": "2.5GB"
  }
}
```

---

### Health Check

#### `GET /v1/health`

Check if the bridge is running and model is loaded.

**Response:**

```json
{
  "status": "healthy",
  "model_loaded": true,
  "model": "gemma-4-e4b-it",
  "uptime_seconds": 3600,
  "requests_served": 42,
  "memory_usage_mb": 2048
}
```

---

## NullClaw Agent API

### Base URL
```
http://localhost:9090
```

### Authentication
No authentication required (local only).

---

### Agent Chat

#### `POST /agent/chat`

Send a message to the agent and get a response with tool execution.

**Request Body:**

```json
{
  "message": "What's the weather in Bucharest?",
  "conversation_id": "optional-conversation-id",
  "enable_tools": true,
  "max_iterations": 5
}
```

**Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `message` | string | Yes | - | User message |
| `conversation_id` | string | No | auto-generated | Conversation ID for memory |
| `enable_tools` | boolean | No | `true` | Enable tool execution |
| `max_iterations` | integer | No | `5` | Maximum tool iterations |

**Response:**

```json
{
  "conversation_id": "conv-123456",
  "response": "I checked the weather for Bucharest...",
  "tool_calls": [
    {
      "tool": "web_search",
      "input": {
        "query": "weather Bucharest today"
      },
      "output": "Partly cloudy, 22°C",
      "success": true
    }
  ],
  "memory_updated": true,
  "timestamp": "2026-04-06T12:00:00Z"
}
```

---

### Tool Execution

#### `POST /agent/tool`

Execute a specific tool directly.

**Request Body:**

```json
{
  "tool": "shell",
  "input": {
    "command": "ls -la /sdcard/"
  }
}
```

**Response:**

```json
{
  "success": true,
  "output": "total 48\ndrwxrwx--x 4...",
  "error": null,
  "execution_time_ms": 42
}
```

#### Available Tools

| Tool | Description | Input Schema |
|------|-------------|--------------|
| `shell` | Execute shell commands | `{"command": "string"}` |
| `file_read` | Read file contents | `{"path": "string"}` |
| `file_write` | Write to file | `{"path": "string", "content": "string"}` |
| `web_search` | Search the web (requires internet) | `{"query": "string"}` |
| `memory_query` | Query agent memory | `{"query": "string"}` |

---

### Memory Management

#### `GET /agent/memory`

Get agent memory for current conversation.

**Query Parameters:**
- `conversation_id` (optional): Specific conversation ID

**Response:**

```json
{
  "conversation_id": "conv-123456",
  "messages": [
    {
      "role": "user",
      "content": "Hello",
      "timestamp": "2026-04-06T12:00:00Z"
    },
    {
      "role": "assistant",
      "content": "Hi! How can I help?",
      "timestamp": "2026-04-06T12:00:02Z"
    }
  ],
  "total_messages": 2,
  "created_at": "2026-04-06T12:00:00Z"
}
```

#### `DELETE /agent/memory`

Clear agent memory for a conversation.

**Query Parameters:**
- `conversation_id` (required): Conversation ID to clear

**Response:**

```json
{
  "success": true,
  "message": "Memory cleared for conversation conv-123456"
}
```

---

## Error Handling

All errors follow this format:

```json
{
  "error": {
    "type": "invalid_request_error",
    "message": "Invalid parameter: temperature must be between 0 and 2",
    "code": "invalid_parameter"
  }
}
```

### Error Types

| Type | Code | HTTP Status | Description |
|------|------|-------------|-------------|
| `invalid_request_error` | `invalid_parameter` | 400 | Invalid request parameter |
| `model_error` | `model_not_loaded` | 503 | Model not loaded |
| `rate_limit_error` | `rate_limit_exceeded` | 429 | Rate limit exceeded |
| `internal_error` | `internal_error` | 500 | Internal server error |

---

## Rate Limiting

**Note**: Since the API runs locally, there are no traditional rate limits. However:

- **Memory**: Limited by available RAM (model uses ~2.5GB)
- **Storage**: Conversations stored in SQLite database
- **Performance**: Inference speed depends on device capabilities

---

## Examples

### Python Example (Requests)

```python
import requests
import json

# Chat completion
response = requests.post(
    "http://localhost:8080/v1/chat/completions",
    json={
        "model": "gemma-4-e4b-it",
        "messages": [
            {"role": "user", "content": "Tell me a joke"}
        ],
        "temperature": 0.7,
        "max_tokens": 100
    }
)

result = response.json()
print(result["choices"][0]["message"]["content"])
```

### Kotlin Example (Ktor Client)

```kotlin
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String = "gemma-4-e4b-it",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

suspend fun chat(message: String): String {
    val client = HttpClient()
    
    val response = client.post("http://localhost:8080/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        setBody(ChatRequest(
            messages = listOf(Message("user", message))
        ))
    }
    
    return response.bodyAsText()
}
```

### cURL Example

```bash
# Chat completion
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma-4-e4b-it",
    "messages": [
      {"role": "user", "content": "What is 2+2?"}
    ],
    "temperature": 0.7,
    "max_tokens": 50
  }'

# Streaming response
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma-4-e4b-it",
    "messages": [
      {"role": "user", "content": "Tell me a story"}
    ],
    "stream": true
  }'

# Health check
curl http://localhost:8080/v1/health
```

---

## Testing

### Postman Collection

Import the Postman collection for testing:

```bash
# Download collection
curl -O https://github.com/serverul/MOMCLAW/raw/main/postman/MOMCLAW_API.postman_collection.json
```

### Automated Tests

Run API tests:

```bash
# From project root
./scripts/run-api-tests.sh
```

---

## Support

- **Issues**: [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)
- **Documentation**: [DOCUMENTATION.md](DOCUMENTATION.md)
- **Discussions**: [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions)

---

**Last Updated**: 2026-04-06  
**API Version**: 1.0.0

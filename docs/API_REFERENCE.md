# MomClAW API Reference

**Version**: 1.0.0  
**Last Updated**: 2026-04-07

---

## Overview

MomClAW exposes two HTTP APIs for communication between components:

1. **LiteRT Bridge API** - OpenAI-compatible chat completions
2. **NullClaw Agent API** - Agent orchestration and tool execution

---

## LiteRT Bridge API

**Base URL**: `http://localhost:8080/v1`

OpenAI-compatible API for LLM inference using LiteRT-LM.

### Endpoints

#### POST /chat/completions

Generate chat completions with streaming support.

**Request:**
```json
{
  "model": "gemma-3-E4B-it",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 2048,
  "stream": true
}
```

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| model | string | required | Model identifier |
| messages | array | required | Conversation history |
| temperature | float | 0.7 | Sampling temperature (0.0-2.0) |
| max_tokens | int | 2048 | Maximum tokens to generate |
| top_p | float | 0.9 | Nucleus sampling parameter |
| top_k | int | 40 | Top-k sampling parameter |
| stream | bool | false | Enable streaming responses |
| stop | array | null | Stop sequences |

**Response (non-streaming):**
```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1712500000,
  "model": "gemma-3-E4B-it",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "Hello! How can I help you today?"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 10,
    "total_tokens": 25
  }
}
```

**Response (streaming):**
```
data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"Hello"}}]}

data: {"id":"chatcmpl-xxx","choices":[{"delta":{"content":"!"}}]}

data: [DONE]
```

---

#### GET /models

List available models.

**Response:**
```json
{
  "object": "list",
  "data": [
    {
      "id": "gemma-3-E4B-it",
      "object": "model",
      "created": 1712500000,
      "owned_by": "google"
    }
  ]
}
```

---

#### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "version": "1.0.0"
}
```

---

## NullClaw Agent API

**Base URL**: `http://localhost:9090`

Agent orchestration API for tool execution and conversation management.

### Endpoints

#### POST /chat

Send a message to the agent with automatic tool execution.

**Request:**
```json
{
  "message": "What files are in /sdcard/Downloads?",
  "session_id": "default",
  "context": {
    "user_id": "user123"
  }
}
```

**Response:**
```json
{
  "response": "I found 5 files in /sdcard/Downloads:\n- document.pdf\n- image.jpg\n...",
  "tools_used": ["shell"],
  "session_id": "default",
  "timestamp": "2026-04-07T12:00:00Z"
}
```

---

#### POST /tool

Execute a specific tool directly.

**Request:**
```json
{
  "tool": "shell",
  "action": "list_directory",
  "params": {
    "path": "/sdcard/Downloads"
  }
}
```

**Response:**
```json
{
  "success": true,
  "result": {
    "files": ["document.pdf", "image.jpg"],
    "total": 2
  },
  "error": null
}
```

---

#### GET /tools

List available tools.

**Response:**
```json
{
  "tools": [
    {
      "name": "shell",
      "description": "Execute shell commands",
      "actions": ["execute", "list_directory", "read_file", "write_file"]
    },
    {
      "name": "web_search",
      "description": "Search the web for information",
      "actions": ["search", "fetch"]
    }
  ]
}
```

---

#### GET /status

Get agent status.

**Response:**
```json
{
  "status": "running",
  "uptime_seconds": 3600,
  "sessions_active": 1,
  "model": "gemma-3-E4B-it",
  "memory_usage_mb": 450
}
```

---

## Available Tools

### Shell Tool

Execute shell commands on the device.

| Action | Parameters | Description |
|--------|------------|-------------|
| execute | `command`: string | Execute arbitrary command |
| list_directory | `path`: string | List directory contents |
| read_file | `path`: string | Read file contents |
| write_file | `path`: string, `content`: string | Write to file |
| delete | `path`: string | Delete file or directory |

### File Tool

File system operations.

| Action | Parameters | Description |
|--------|------------|-------------|
| exists | `path`: string | Check if file exists |
| info | `path`: string | Get file metadata |
| copy | `src`: string, `dst`: string | Copy file |
| move | `src`: string, `dst`: string | Move file |
| mkdir | `path`: string | Create directory |

### Web Search Tool

Search the web for information (requires internet).

| Action | Parameters | Description |
|--------|------------|-------------|
| search | `query`: string, `limit`: int | Search the web |
| fetch | `url`: string | Fetch URL content |

---

## Error Handling

All APIs return standard HTTP status codes:

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid parameters |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Server Error |
| 503 | Service Unavailable - Model not loaded |

**Error Response Format:**
```json
{
  "error": {
    "type": "invalid_request_error",
    "message": "Missing required parameter: messages",
    "code": "missing_parameter"
  }
}
```

---

## Rate Limiting

The LiteRT Bridge has built-in rate limiting:

- **Requests per minute**: 60
- **Tokens per minute**: 100,000
- **Concurrent requests**: 4

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
X-RateLimit-Reset: 1712500600
```

---

## Android Integration

### Kotlin Client Example

```kotlin
class AgentClient @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun sendMessage(message: String): String {
        return httpClient.post("http://localhost:9090/chat") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "message" to message,
                "session_id" to "default"
            ))
        }.body<ChatResponse>().response
    }
    
    suspend fun executeTool(
        tool: String,
        action: String,
        params: Map<String, Any>
    ): ToolResult {
        return httpClient.post("http://localhost:9090/tool") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "tool" to tool,
                "action" to action,
                "params" to params
            ))
        }.body()
    }
}
```

### Streaming Example

```kotlin
fun streamCompletion(
    messages: List<Message>,
    onToken: (String) -> Unit
): Flow<String> = flow {
    httpClient.preparePost("http://localhost:8080/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        setBody(mapOf(
            "model" to "gemma-3-E4B-it",
            "messages" to messages,
            "stream" to true
        ))
    }.execute { response ->
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ")
                if (data == "[DONE]") break
                val chunk = Json.decodeFromString<StreamChunk>(data)
                chunk.choices.firstOrNull()?.delta?.content?.let { token ->
                    emit(token)
                    onToken(token)
                }
            }
        }
    }
}
```

---

## WebSocket Support (Planned)

Future versions will support WebSocket connections for real-time communication:

```
ws://localhost:8080/v1/chat/stream
ws://localhost:9090/agent/ws
```

---

## Security Considerations

1. **Localhost Only**: APIs bind to localhost only
2. **No Authentication**: No auth required (local only)
3. **Permission-Based**: Tool execution respects Android permissions
4. **Sandboxed**: File access limited to app's sandbox

---

## Performance Guidelines

### Model Loading

```kotlin
// Preload model for faster first response
suspend fun preloadModel() {
    httpClient.get("http://localhost:8080/v1/models")
}
```

### Memory Management

- Model uses ~2.5GB RAM when loaded
- Unload model when not in use
- Monitor memory with `/status` endpoint

### Batch Requests

For multiple requests, use a single session:

```kotlin
// Use session_id to maintain context
val sessionId = UUID.randomUUID().toString()

requests.forEach { message ->
    client.postMessage(message, sessionId)
}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-04-06 | Initial API release |

---

**Documentation Maintainer**: MomClAW Team  
**Contact**: support@momclaw.app

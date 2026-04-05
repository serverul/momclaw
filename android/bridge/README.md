# LiteRT Bridge Module

Mock implementation of the LiteRT Bridge for MOMCLAW Android app.

## Purpose

Provides an OpenAI-compatible HTTP API for on-device inference using Google AI Edge LiteRT.

## Current Status

**MOCK IMPLEMENTATION** - The actual LiteRT SDK is not yet available. This implementation:
- Accepts OpenAI-format requests at `/v1/chat/completions`
- Returns mock streaming/non-streaming responses
- Has correct API shape for NullClaw integration

## Structure

```
bridge/
├── build.gradle.kts          # Dependencies (Ktor, Kotlinx, logging)
└── src/main/java/com/loa/momclaw/bridge/
    ├── LiteRTBridge.kt       # Main HTTP server, routing
    ├── LlmEngineWrapper.kt   # LiteRT wrapper (currently mock)
    ├── ChatRequest.kt        # Data classes (OpenAI-compatible)
    └── SSEWriter.kt          # Server-Sent Events streaming
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check & model status |
| `/v1/models` | GET | List available models |
| `/v1/chat/completions` | POST | OpenAI-compatible chat completions |

## Running

```bash
./gradlew run
```

Server starts at `http://localhost:8080`

## Usage Example

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma-4e4b",
    "messages": [{"role": "user", "content": "Hello!"}],
    "stream": false
  }'
```

## When LiteRT SDK is Available

Replace `LlmEngineWrapper` mock methods with actual LiteRT calls:

```kotlin
// Expected LiteRT usage (pseudocode)
val engine = LiteRTEngine.loadModel("gemma-4e4b.litertlm")
engine.generateStreaming(prompt) { chunk -> emit(chunk) }
```

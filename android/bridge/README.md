# MOMCLAW Integration Guide

## Quick Start

1. **Download Model**: 
   ```bash
   huggingface-cli download litert-community/gemma-4-E4B-it-litertlm
   ```

2. **Place in app assets**: 
   Copy `gemma-4-E4B-it.litertlm` to:
 
 `app/src/main/assets/gemma-4-E4B-it.litertlm`
   ```

3. **Initialize Bridge**:
   ```kotlin
   // In your Application class or DI module:
   val bridge = LiteRTBridge(context)
   bridge.start(modelPath)
   
   // Or use factory
   val bridge = NullClawBridgeFactory.getInstance(context)
   val config = AgentConfig(
       modelPath = context.filesDir.resolve("models/gemma-4-E4B-it.litertlm").absolutePath
   )
   bridge.setup(config)
   bridge.start()
   ```

4. **Check Health**:
   ```kotlin
   // Quick check
   val isReady = bridge.isModelReady()
   val isRunning = bridge.isRunning()
   
   // Full health check
   val health = bridge.getHealthStatus()
   println("Status: ${health.status}") // HEALTHY, DEGRADED, UNHEALTHY
   println("Model: ${health.model}")
   
   // NullClaw health
   val agentHealth = nullClawBridge.checkHealth()
   ```

5. **Use the Agent**:
   ```kotlin
   // In your ViewModel or Activity:
       val bridge = NullClawBridgeFactory.getInstance(application)
       
       // Send message
       val response = bridgeClient.post("http://localhost:9090/v1/chat/completions") {
           "messages": [
               {"role": "system", "content": "You are a helpful assistant."},
               {"role": "user", "content": "Hello!"}
           ]
       }
       
       // Handle response
       println(response.body)
  }
   ```

## Configuration
### AgentConfig Options
```kotlin
val config = AgentConfig(
    systemPrompt = "You are a helpful AI assistant running on-device.",
    temperature = 0.7f,  // 0.0-2.0
    maxTokens = 2048,    // 4096 max
    modelPrimary = "litert-bridge/gemma-4e4b",
    modelPath = "/data/data/com.loa.momclaw/files/models/gemma-4-E4B-it.litertlm",
    baseUrl = "http://localhost:8080",
    memoryBackend = "sqlite",
    memoryPath = "/data/data/com.loa.momclaw/databases/agent.db"
)
```

### LiteRT Request
```kotlin
data class LiteRTRequest(
    val prompt: String,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val stopTokens: List<String> = emptyList()
)
```

## Model Management
### ModelLoader
```kotlin
val loader = ModelLoader(context)
val modelPath = loader.getDefaultModelPath()
val result = loader.verifyModel(modelPath)

when (result is ModelLoader.LoadResult.Success) {
    println("Model loaded: ${result.info.name}")
    // Bridge ready for inference
}
 when (result is ModelLoader.LoadResult.Error) {
    // Download model from HuggingFace
    loader.downloadFromHuggingFace()
}
```

### Storage Info
```kotlin
val storageInfo = loader.getStorageInfo()
println("Models dir: ${storageInfo.modelsDirectory}")
println("Available: ${storageInfo.availableMB} MB")
```

## Error Handling
All errors are returned as `BridgeError` with structured error codes:
 messages.

### BridgeError Types
- `ModelError`: Model-related issues (not found, load failed, not ready)
- `InferenceError`: Generation failures (timeout, token limit exceeded)
- `ValidationError`: Invalid requests (missing fields, empty messages)
- `ServerError`: Server issues (startup failed, bind failed)

```

### Example Usage
```kotlin
try {
    val response = bridgeClient.post("http://localhost:8080/v1/chat/completions") { ... }
} catch (e: BridgeError) {
    when (e is BridgeError.ModelError.NotReady) {
    // Handle model not loaded error
    println("Please load a model first")
    }
}
```

## Health Monitoring
- Server uptime tracking
- Model load status
- Memory usage
- Request metrics (total, errors, error rate)
- Disk space monitoring

- Low memory detection

### Health Endpoints
- `GET /health`: Quick health check
  `GET /health/details`: Full diagnostics
- `GET /metrics`: Performance metrics

- `GET /v1/models`: Model info (OpenAI-compatible)

## Logging
- Structured logging with [prefix] tags:
  - `[LiteRT]`: LiteRT Bridge messages
  - `[NullClaw]`: NullClaw agent messages
  - `[Config]`: Configuration changes
- `[Health]`: Health check results

- `[Error]`: Errors and warnings
  - `[Metrics]`: Performance metrics

## Troubleshooting
### Model Won't Load
1. Check if model file exists: `adb shell ls -la /data/data/com.loa.momclaw/files/models/`
2. Check model size (should be >100MB for Gemma 4E4B)
3. Verify model format (.litertlm extension)
4. Check logs for error messages
5. Verify configuration is correct

6. Try restarting the app

### Agent Won't Start
1. Check if binary was extracted from assets
2. Check process is running: `adb shell ps | grep nullclaw`
3. Check port 9090 is listening: `adb shell netstat -tulpn | grep 9090`
4. Verify configuration was generated
5. Check logs for error messages
6. Try restarting the app

7. If stub mode, verify stub is functioning

   - Check if HTTP server starts on mock

### Connection Issues
1. Verify LiteRT Bridge is running (port 8080)
2. Verify NullClaw can connect (port 9090)
3. Test health endpoints manually:
 `curl http://localhost:8080/health` and `curl http://localhost:9090/health`

### Performance Tips
1. Use streaming for long responses
2. Adjust `maxTokens` based on needs
3. Lower `temperature` for more deterministic responses
4. Close unused resources when done
5. Monitor memory usage during heavy operations
6. Use background threads for time-consuming operations

7. Consider batching multiple requests if needed
8. Clear model and memory periodically

9. Check health before critical operations
10. Handle errors gracefully

11. Provide user feedback for loading/inference states
12. Log errors for debugging

13. Use timeouts for external operations

14. Implement retry logic for appropriate

15. Add circuit breakers for possible
16. Test edge cases thoroughly before deployment

# NullClaw Agent Module

Android library module that wraps the NullClaw binary for on-device AI agent execution.

## Structure

```
agent/
├── build.gradle.kts          # Module build configuration
├── src/main/
│   ├── java/com/loa/momclaw/agent/
│   │   ├── NullClawBridge.kt    # Binary process lifecycle management
│   │   ├── ConfigGenerator.kt   # JSON config file generation
│   │   └── AgentConfig.kt       # Configuration data class
│   └── assets/
│       └── nullclaw             # Placeholder binary (ARM64 binary TBD)
```

## Usage

```kotlin
val config = AgentConfig(
    systemPrompt = "You are a helpful assistant.",
    temperature = 0.7f,
    maxTokens = 2048
)

val bridge = NullClawBridge(context)
bridge.setup(config)
bridge.start()

// Later...
bridge.stop()
```

## Key Classes

### NullClawBridge
Manages the NullClaw binary lifecycle:
- Extracts binary from assets to internal storage
- Sets executable permissions
- Starts process with config
- Monitors process health
- Graceful shutdown

### AgentConfig
Data class holding configuration:
- `systemPrompt` - Agent's system prompt
- `temperature` - Response randomness (0.0-1.0)
- `maxTokens` - Max response length
- `modelPrimary` - LiteRT model identifier
- `baseUrl` - Local inference server URL
- `memoryBackend` - Storage backend (sqlite)
- `memoryPath` - Database path

### ConfigGenerator
Utilities for config file management:
- `generateConfigFile()` - Write JSON config
- `parseConfigFile()` - Read JSON config
- `mergeConfig()` - Update partial config

## Binary Placeholder

The `assets/nullclaw` file is a placeholder shell script. Replace with the compiled ARM64 binary for production builds.

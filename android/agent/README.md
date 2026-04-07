# NullClaw Agent Module

Android library module that wraps the NullClaw binary for on-device AI agent execution.

## Structure

```
agent/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/loa/momclaw/agent/
│   │   ├── NullClawBridge.kt          # Binary process lifecycle management
│   │   ├── NullClawBridgeFactory.kt   # Thread-safe singleton factory
│   │   ├── AgentConfig.kt             # Root-level config data class
│   │   ├── AgentLifecycleManager.kt   # Lifecycle-aware management
│   │   ├── ConfigGenerator.kt         # JSON config file generation
│   │   ├── config/
│   │   │   └── ConfigurationManager.kt # Load/save/validate config
│   │   ├── model/
│   │   │   └── AgentConfig.kt         # Detailed config with defaults
│   │   └── monitoring/
│   │       └── AgentMonitor.kt        # Health & diagnostics
│   └── assets/
│       └── nullclaw                   # ARM64 binary (3.5MB)
└── src/test/java/com/loa/momclaw/agent/
    ├── NullClawAgentTest.kt
    ├── NullClawBridgeTest.kt
    └── NullClawAgentIntegrationTest.kt
```

## Build

```bash
# Requires JDK 17 + Android SDK API 34
./gradlew :agent:assembleDebug        # Debug AAR
./gradlew :agent:assembleRelease      # Release AAR
./gradlew :agent:test                 # Unit tests
```

Output: `agent/build/outputs/aar/agent-debug.aar`

## Usage

```kotlin
// Via factory (recommended)
val bridge = NullClawBridgeFactory.getInstance(context)
val config = AgentConfig(
    systemPrompt = "You are a helpful assistant.",
    temperature = 0.7f,
    maxTokens = 2048,
    modelPath = context.filesDir.resolve("models/gemma-3-E4B-it.litertlm").absolutePath,
    baseUrl = "http://localhost:8080"
)
bridge.setup(config)
bridge.start()  // Starts on port 9090

// Health check
val health = bridge.getHealthStatus()

// Cleanup
NullClawBridgeFactory.reset()
```

## Key Classes

### NullClawBridge
- Copies binary from assets → internal storage
- Generates `nullclaw-config.json`
- Starts process with `--config` + `gateway --port 9090`
- Monitors process health, reads stdout
- Graceful shutdown with force-kill fallback (5s timeout)
- Thread-safe state (ReentrantLock + AtomicReference)

### NullClawBridgeFactory
- Thread-safe singleton via Mutex
- Configuration management integration
- Lifecycle listener management
- Health/diagnostics aggregation

### AgentConfig
- `systemPrompt`, `temperature`, `maxTokens`
- `modelPrimary`, `modelPath`, `baseUrl`
- `memoryBackend` (sqlite), `memoryPath`

### AgentMonitor
- Tracks start/stop/error events
- Reports health status (HEALTHY/DEGRADED/UNHEALTHY)
- Bridge connection check (localhost:8080)

## Architecture

```
App → NullClawBridgeFactory → NullClawBridge → nullclaw binary (port 9090)
                                                      ↓ HTTP
                                               LiteRT Bridge (port 8080)
```

## Binary

The `assets/nullclaw` is a compiled Zig binary (3.5MB, ARM64). For other architectures or updates, cross-compile from the NullClaw source:

```bash
zig build -Dtarget=aarch64-linux-android -Doptimize=ReleaseSmall
cp zig-out/bin/nullclaw agent/src/main/assets/nullclaw
```

## Dependencies

- Kotlin coroutines, kotlinx-serialization
- Hilt (DI)
- AndroidX Lifecycle
- kotlin-logging

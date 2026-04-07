# MomClaw Android App

Offline AI Agent powered by Gemma 4E4B running locally on Android.

## Architecture

MomClaw Android follows a **Hybrid Architecture** with three main modules:

### 1. **App Module** (`app/`)
- UI layer with Jetpack Compose (Material3 design)
- MVVM architecture with ViewModels
- Domain layer with use cases
- Data layer with Room database and DataStore
- Hilt dependency injection

### 2. **Bridge Module** (`bridge/`)
- LiteRT-LM HTTP server (Ktor)
- OpenAI-compatible API on localhost:8080
- Model inference and streaming responses
- Gemma 4E4B model integration

### 3. **Agent Module** (`agent/`)
- NullClaw agent binary wrapper
- Configuration management
- Process lifecycle management
- SQLite-based memory system

## Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.02.00) + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room 2.6.1
- **Preferences**: DataStore 1.0.0
- **Networking**: OkHttp 4.12.0 + Ktor 2.3.7
- **Serialization**: Kotlinx Serialization 1.6.2
- **Async**: Kotlinx Coroutines 1.7.3

## Project Structure

```
android/
├── app/                      # Main application
│   ├── src/main/
│   │   ├── java/com/loa/momclaw/
│   │   │   ├── ui/           # UI layer (Compose screens)
│   │   │   ├── domain/       # Business logic
│   │   │   ├── data/         # Data layer (Room, DataStore)
│   │   │   ├── di/           # Dependency injection
│   │   │   └── service/      # Background services
│   │   └── res/              # Android resources
│   └── build.gradle.kts
│
├── bridge/                   # LiteRT Bridge module
│   ├── src/main/java/com/loa/momclaw/bridge/
│   │   ├── LiteRTBridge.kt   # HTTP server
│   │   ├── LlmEngineWrapper.kt
│   │   └── ChatModels.kt     # Data models
│   └── build.gradle.kts
│
├── agent/                    # NullClaw Agent module
│   ├── src/main/
│   │   ├── java/com/loa/momclaw/agent/
│   │   │   ├── NullClawBridge.kt
│   │   │   ├── AgentConfig.kt
│   │   │   └── AgentLifecycleManager.kt
│   │   └── assets/
│   │       └── nullclaw      # ARM64 binary
│   └── build.gradle.kts
│
├── build.gradle.kts          # Root build config
├── settings.gradle.kts
└── gradle.properties
```

## Building

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- Gradle 8.4

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

## Configuration

### Minimum Requirements

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34

### Build Variants

- **debug**: Development build with debugging enabled
- **release**: Production build with ProGuard/R8 optimization

## Modules

### App Module Dependencies

```kotlin
dependencies {
    implementation(project(":bridge"))
    implementation(project(":agent"))
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    
    // Architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.50")
}
```

### Bridge Module Dependencies

```kotlin
dependencies {
    // LiteRT-LM (placeholder - requires Google AI Edge SDK)
    // implementation("com.google.ai.edge:litert-lm:1.0.0")
    
    // Ktor server
    implementation("io.ktor:ktor-server-netty:2.3.7")
}
```

## Key Features

### Chat Screen
- Real-time streaming responses
- Message history persistence
- Clean conversation UI with Material3 design
- Input validation and error handling

### Models Screen
- Model download from HuggingFace
- Model loading and unloading
- Storage management
- Progress indicators

### Settings Screen
- System prompt customization
- Temperature and token limits
- Dark mode toggle
- Auto-save preferences

## Data Flow

```
User Input → ChatViewModel → AgentClient → NullClaw (9090)
                                              ↓
                                         LiteRT Bridge (8080)
                                              ↓
                                         Gemma 4E4B Model
                                              ↓
SSE Stream ← AgentClient ← NullClaw Response ← Stream Tokens
     ↓
ChatViewModel → UI Update
```

## Security

- ProGuard rules for code obfuscation
- Network security config for localhost communication
- File provider for secure file sharing
- No hardcoded credentials

## Performance

- Lazy column for message list
- Flow-based reactive updates
- Coroutine-based async operations
- Efficient Room database queries

## Testing

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# Test coverage
./gradlew jacocoTestReport
```

## Known Limitations

1. **LiteRT-LM SDK**: Currently a placeholder - requires Google AI Edge integration
2. **NullClaw Binary**: Must be compiled separately for ARM64 Android
3. **Model Download**: Not implemented - requires HuggingFace API integration
4. **Background Service**: May be killed by aggressive battery optimization

## Future Enhancements

- [ ] Implement actual LiteRT-LM integration
- [ ] Add model download from HuggingFace
- [ ] Implement tool calls (shell, file operations)
- [ ] Add conversation export/import
- [ ] Implement multi-turn context management
- [ ] Add voice input/output
- [ ] Support multiple models simultaneously

## License

[Add your license here]

## Contributing

[Add contributing guidelines here]

## Credits

- **Gemma 4E4B** by Google
- **LiteRT-LM** by Google AI Edge
- **NullClaw** Agent Framework
- **Jetpack Compose** by Google

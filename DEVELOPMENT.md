# MomClaw Developer Guide

## Project Overview

MOMCLAW (Mobile Offline Model Agent CLAW) is an AI-powered chat application for Android that combines:

- **Chat UI** - Clean Material 3 Compose interface
- **Agent Integration** - NullClaw agent via local HTTP
- **Model Management** - Download and manage LLM models
- **Settings** - Configure agent behavior

## Project Structure

```
momclaw/
├── android/                        # Android multi-module project
│   ├── app/                        # Main application module
│   │   ├── src/main/
│   │   │   ├── java/com/loa/momclaw/
│   │   │   │   ├── MOMCLAWApplication.kt  # Application + Hilt DI
│   │   │   │   ├── MainActivity.kt        # Entry point Activity
│   │   │   │   ├── ChatRoute.kt           # Chat screen Compose
│   │   │   │   ├── ModelsRoute.kt         # Models screen Compose
│   │   │   │   ├── SettingsRoute.kt       # Settings screen Compose
│   │   │   │   ├── data/                  # Data layer
│   │   │   │   ├── domain/                # Domain layer
│   │   │   │   └── ui/                    # UI components
│   │   │   ├── res/                       # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── bridge/                     # LiteRT Bridge (Ktor server)
│   │   └── build.gradle.kts
│   ├── agent/                      # NullClaw agent integration
│   │   └── build.gradle.kts
│   ├── build.gradle.kts            # Top-level build config
│   ├── settings.gradle.kts         # Project settings
│   └── gradle.properties           # Build configuration
├── native/                         # Native components (future)
├── .github/workflows/              # CI/CD pipelines
├── BUILD.md                        # Build instructions
├── DEVELOPMENT.md                  # This file
├── SPEC.md                         # Technical specification
└── README.md                       # Project overview
```

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────┐
│           Presentation Layer        │
│  • Compose UI (Chat, Models, Settings)│
│  • ViewModels (State management)    │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│            Domain Layer             │
│  • Use Cases                        │
│  • Repositories (ChatRepository)    │
│  • Domain Models (ChatMessage)      │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│             Data Layer              │
│  • AgentClient (HTTP to NullClaw)   │
│  • Room Database (Message storage)  │
│  • DataStore (Settings/preferences) │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│           External Services         │
│  • NullClaw Agent (localhost:9090)  │
│  • LiteRT Bridge (localhost:8080)   │
│  • LiteRT-LM (Gemma model)          │
└─────────────────────────────────────┘
```

### Dependency Injection

MOMCLAW uses **Hilt** for dependency injection:

- **AppModule** (in `MOMCLAWApplication.kt`) provides:
  - `MOMCLAWDatabase` (Room database)
  - `MessageDao` (database access)
  - `SettingsPreferences` (persistent settings)
  - `AgentConfig` (agent configuration)
  - `AgentClient` (HTTP client for NullClaw)
  - `ChatRepository` (chat operations)

## Development Setup

### Prerequisites

1. **Android Studio Hedgehog** (2023.1.1) or newer
2. **JDK 17** (bundled with Android Studio)
3. **Android SDK 35** with build tools
4. **Kotlin 2.0.21** (specified in top-level build.gradle.kts)

### Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/serverul/momclaw.git
   cd momclaw
   ```

2. Open in Android Studio:
   - File → Open → Select `momclaw/android` directory
   - Android Studio will sync Gradle automatically

3. Run the app:
   - Select a device/emulator (minSdk 28)
   - Click **Run** (▶️) or press `Shift+F10`

### Command Line Development

```bash
# Navigate to android directory
cd momclaw/android

# Sync and build
./gradlew assembleDebug

# Run on connected device
./gradlew installDebug

# Run tests
./gradlew testDebugUnitTest
```

## Key Components

### 1. Chat System

**Files:**
- `ChatRoute.kt` - Chat UI screen
- `ChatViewModel.kt` - Chat state management
- `ChatRepository.kt` - Chat operations (send, load, save)
- `ChatMessage.kt` - Message data model
- `AgentClient.kt` - HTTP client for NullClaw communication

**Flow:**
1. User types message in `ChatRoute`
2. `ChatViewModel` receives input, calls `ChatRepository.sendMessage()`
3. Repository saves to Room database, sends to `AgentClient`
4. `AgentClient` POSTs to NullClaw (`localhost:9090`)
5. SSE response streamed back, saved to database
6. UI updates with streaming response

### 2. Models Management

**Files:**
- `ModelsRoute.kt` - Models UI
- `ModelsViewModel.kt` - Models state management

**Features:**
- List available models
- Download models from HuggingFace
- Load/unload models for inference
- Delete cached models

### 3. Settings

**Files:**
- `SettingsRoute.kt` - Settings UI
- `SettingsViewModel.kt` - Settings state management
- `SettingsPreferences.kt` - DataStore preferences

**Configurable:**
- Agent URL
- System prompt
- Temperature
- Max tokens
- Dark theme toggle
- Streaming preference
- Background agent toggle
- Notifications

## Data Persistence

### Room Database

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    val id: String,
    val content: String,
    val role: String,  // user | agent | system
    val timestamp: Long,
    val conversationId: String
)
```

### DataStore Preferences

Used for app settings (theme, streaming, etc.):
```kotlin
class SettingsPreferences(context: Context) {
    // Persistent key-value store
}
```

## Networking

### AgentClient

Connects to NullClaw agent via HTTP:
- Default: `http://localhost:9090`
- POST `/chat` for chat messages
- SSE streaming for responses

### LiteRT Bridge

Ktor server bridge between NullClaw and LiteRT-LM:
- Runs on `localhost:8080`
- OpenAI-compatible API (`/v1/chat/completions`)
- SSE streaming responses

## Theming

### Material 3 Theme

Defined in `ui/theme/`:
- `Color.kt` - Primary, secondary, tertiary color palettes
- `Theme.kt` - Light/dark theme setup
- `Type.kt` - Typography scales

### Dynamic Colors

The app supports Material You dynamic colors on Android 12+.

## Testing Strategy

### Unit Tests

- ViewModel tests with `kotlinx.coroutines.test`
- Repository tests with mock dependencies
- Database DAO tests with Room's in-memory database

### Instrumented Tests

- UI tests with Compose Test
- Integration tests on real device/emulator

### Running Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires device)
./gradlew connectedAndroidTest
```

## CI/CD Pipelines

### GitHub Actions

Located in `.github/workflows/`:

1. **ci.yml** - On push/PR to main/develop:
   - Build debug APK
   - Run unit tests
   - Run lint checks
   - Upload artifacts

2. **android-build.yml** - Extended pipeline:
   - Matrix builds (API 28/35, debug/release)
   - Instrumented tests on emulator
   - Detekt static analysis
   - Release AAB generation

3. **release.yml** - Automated releases:
   - Triggered by tags
   - Builds signed release AAB
   - Creates GitHub release with artifacts

## Best Practices

### Code Style

- Follow **Kotlin Official Style Guide**
- Use **detekt** for static analysis
- 4-space indentation
- Max line length: 120 characters

### Git Workflow

- `main` - Stable releases
- `develop` - Integration branch
- Feature branches: `feature/description`
- PRs required for main/develop

### Naming Conventions

- **Files**: PascalCase (`ChatRoute.kt`)
- **Functions/Variables**: camelCase (`sendMessage`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_TOKENS`)
- **Resources**: snake_case (`ic_chat_send.xml`)

### Composition Over Inheritance

Prefer Compose functions over XML layouts; use sealed classes for state.

## Common Tasks

### Add New Screen

1. Create screen Composable in `ui/`
2. Add route in `NavGraph.kt`
3. Create ViewModel in `ui/`
4. Update `MainActivity` navigation

### Add Database Entity

1. Create entity in `data/local/database/`
2. Add DAO interface
3. Update `MOMCLAWDatabase.kt`
4. Add migration if needed

### Add API Endpoint

1. Update `AgentClient.kt` with new endpoint
2. Create data class for request/response
3. Update ViewModel to consume new API

## Troubleshooting

### Gradle Sync Failed

- Check `gradle.properties` for correct JVM args
- Verify JDK 17 is selected in Android Studio
- Run `./gradlew --refresh-dependencies`

### App Crashes on Launch

- Check Logcat for stack traces
- Verify NullClaw agent is running at expected URL
- Check network permissions in `AndroidManifest.xml`

### Build Errors

- Clean and rebuild: `./gradlew clean assembleDebug`
- Invalidate caches in Android Studio: File → Invalidate Caches

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a Pull Request
5. Address review comments

## License

See [LICENSE](LICENSE) file.
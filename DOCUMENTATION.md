# MomClaw Documentation

**Mobile Offline Model Agent — AI Agent 100% offline pe Android**

> Versiune: 1.0.0  
> Ultima actualizare: 2026-04-06

---

## 📖 Cuprins

- [Prezentare Generală](#prezentare-generală)
- [Arhitectură](#arhitectură)
- [Instalare și Setup](#instalare-și-setup)
- [Deployment](#deployment)
- [API Documentation](#api-documentation)
- [Troubleshooting](#troubleshooting)
- [Dezvoltare](#dezvoltare)
- [Testare](#testare)
- [CI/CD](#cicd)

---

## Prezentare Generală

### Ce este MomClaw?

MomClaw este un agent AI complet autonom care rulează pe Android — zero cloud, zero tracking, 100% offline.

### Caracteristici Principale

- 🧠 **Conversații inteligente cu reasoning** - Gemma 3 E4B-it model
- 🔧 **Tool calls native** - shell, file ops, web search
- 💾 **Memorie persistentă** - SQLite database
- 📱 **Canale externe** - Telegram, Discord (când e online)
- 🔄 **Sync cu instanță OpenClaw** - mainframe integration
- 🔒 **Privacy-first** - toate datele rămân pe device

### Tech Stack

| Componentă | Tehnologie |
|-----------|------------|
| Android App | Kotlin + Jetpack Compose |
| Inference | LiteRT-LM (Google AI Edge) |
| Bridge | Ktor Server (Netty) |
| Agent Logic | NullClaw (Zig) |
| Model | Gemma 3 E4B-it (Q4_K_M) |
| Memory | Room Database (SQLite) |
| DI | Hilt (Dagger) |
| Build | Gradle 8.9+ + CMake + Zig |

---

## Arhitectură

### High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                  MOMCLAW Android App                │
├────────────────────────────────┬────────────────────┤
│  UI (Kotlin + Compose)         │  Bridge Module     │
│  • Chat interface              │  (Ktor Server)     │
│  • Model management            │  • HTTP API        │
│  • Settings & config           │  • LiteRT bridge   │
├────────────────────────────────┼────────────────────┤
│  Agent Module                  │  Native Layer      │
│  • NullClaw integration        │  (C++ + Zig)       │
│  • Tool execution              │  • llama.cpp       │
│  • Memory management           │  • Optimizations   │
├────────────────────────────────┴────────────────────┤
│           Android Foreground Service                │
│  • Background agent                                 │
│  • Telegram/Discord channels                        │
│  • OpenClaw sync                                    │
└─────────────────────────────────────────────────────┘
```

### Module Architecture

```
momclaw/
├── app/                    # Main application
│   ├── ui/                 # Compose UI screens
│   ├── data/               # Data layer (Room, DataStore)
│   ├── domain/             # Business logic
│   └── di/                 # Hilt modules
├── bridge/                 # LiteRT HTTP bridge
│   ├── server/             # Ktor server
│   ├── inference/          # LiteRT-LM integration
│   └── api/                # OpenAI-compatible API
└── agent/                  # NullClaw agent
    ├── core/               # Agent logic
    ├── tools/              # Tool implementations
    └── memory/             # Memory management
```

### Data Flow

```
User Input
    ↓
ChatViewModel
    ↓
ChatRepository
    ↓
AgentClient (HTTP POST)
    ↓
NullClaw Agent (localhost:9090)
    ↓
LiteRT Bridge (localhost:8080)
    ↓
LiteRT-LM Inference (Gemma model)
    ↓
SSE Stream Response
    ↓
ChatRepository → Room Database
    ↓
UI Update
```

---

## Instalare și Setup

### Prerequisites

**Obligatoriu:**
- JDK 17 (nu 11 sau 21)
- Android SDK API 35 (Android 15)
- Android NDK r25c+ (pentru native components)
- Android Studio Hedgehog (2023.1.1) sau mai nou
- Git

**Recomandat:**
- 8GB+ RAM
- SSD pentru build-uri mai rapide
- Linux/macOS pentru compatibilitate optimă

### Clonare și Build

```bash
# 1. Clone repository
git clone https://github.com/serverul/momclaw.git
cd momclaw

# 2. Setup Gradle wrapper (Unix/Linux/macOS)
chmod +x android/gradlew

# 3. Debug build (pentru dezvoltare)
./android/gradlew assembleDebug

# 4. Release build (pentru distribuție)
./android/gradlew assembleRelease

# 5. Build AAB pentru Google Play
./android/gradlew bundleRelease
```

### Android Studio Setup

1. Deschide Android Studio
2. **File → Open** → Selectează folderul `momclaw/android`
3. Așteaptă Gradle sync (prima dată durează 5-10 min)
4. Selectează un device/emulator (API 28+)
5. Apasă **Run** (▶️) sau `Shift+F10`

### Model Download

```bash
# Download Gemma 3 E4B-it model (~2.5GB)
./scripts/download-model.sh ./models

# Copiază pe device
adb push models/gemma-3-E4B-it.litertlm \
    /sdcard/Android/data/com.loa.momclaw/files/models/
```

---

## Deployment

### Build Types

| Build Type | Descriere | Minify | Debuggable |
|-----------|-----------|---------|------------|
| **debug** | Dezvoltare și testare | ❌ | ✅ |
| **release** | Producție și distribuție | ✅ | ❌ |

### Signing Release Builds

#### 1. Generare Keystore

```bash
keytool -genkey -v \
    -keystore momclaw-release-key.jks \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -alias momclaw
```

**Important:**
- Păstrează keystore-ul în siguranță (backup!)
- Nu commit-ui keystore-ul în repo
- Setează parole puternice

#### 2. Configurare key.properties

Creează `android/key.properties`:

```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=momclaw
storeFile=../momclaw-release-key.jks
```

**⚠️ NU commite acest fișier!** Adaugă în `.gitignore`:

```gitignore
# Signing
key.properties
*.jks
*.keystore
```

#### 3. Build Release

```bash
# Build signed APK
./android/gradlew assembleRelease

# Build signed AAB (pentru Google Play)
./android/gradlew bundleRelease

# Output locations:
# APK: android/app/build/outputs/apk/release/app-release.apk
# AAB: android/app/build/outputs/bundle/release/app-release.aab
```

### Distribution

#### Google Play Store

1. Build AAB signed: `./android/gradlew bundleRelease`
2. Upload în Google Play Console
3. Completează metadata (descriere, screenshots, etc.)
4. Submit pentru review

#### APK Direct Distribution

```bash
# Build signed APK
./android/gradlew assembleRelease

# Verifică semnătura
jarsigner -verify -verbose -certs \
    android/app/build/outputs/apk/release/app-release.apk

# Distribuie APK-ul
```

#### F-Droid

MomClaw poate fi publicat pe F-Droid:

1. Asigură-te că toate dependențele sunt FOSS
2. Creează `metadata/com.loa.momclaw.yml`
3. Submit merge request la fdroiddata

---

## API Documentation

### LiteRT Bridge API

Bridge-ul oferă un API compatibil OpenAI pe `localhost:8080`.

#### Base URL

```
http://localhost:8080/v1
```

#### Endpoints

##### POST /v1/chat/completions

**Request:**

```json
{
  "model": "gemma-3-e4b-it",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 2048,
  "stream": true
}
```

**Response (non-streaming):**

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1712345678,
  "model": "gemma-3-e4b-it",
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
    "completion_tokens": 8,
    "total_tokens": 23
  }
}
```

**Response (streaming):**

```
data: {"choices":[{"delta":{"content":"Hello"}}]}

data: {"choices":[{"delta":{"content":"! How"}}]}

data: [DONE]
```

##### GET /v1/models

**Response:**

```json
{
  "object": "list",
  "data": [
    {
      "id": "gemma-3-e4b-it",
      "object": "model",
      "created": 1712345678,
      "owned_by": "google"
    }
  ]
}
```

##### GET /health

**Response:**

```json
{
  "status": "ok",
  "model": "gemma-3-e4b-it",
  "uptime_seconds": 3600
}
```

### NullClaw Agent API

Agentul NullClaw rulează pe `localhost:9090`.

#### POST /chat

**Request:**

```json
{
  "message": "Hello, agent!",
  "conversation_id": "conv-123",
  "context": {
    "memory": true,
    "tools": ["shell", "file", "web"]
  }
}
```

**Response (SSE stream):**

```
event: token
data: {"content": "Hello"}

event: token
data: {"content": "! I'm"}

event: done
data: {"status": "complete", "tokens_used": 25}
```

#### POST /tool

**Request:**

```json
{
  "tool": "shell",
  "args": {
    "command": "ls -la"
  }
}
```

**Response:**

```json
{
  "output": "total 48\ndrwxr-xr-x  2 user user 4096 Apr  5 23:00 .",
  "exit_code": 0
}
```

---

## Troubleshooting

### Build Issues

#### Gradle Sync Failed

**Simptom:** Android Studio nu poate sincroniza Gradle

**Soluții:**

1. **Verifică JDK:**
   ```bash
   java -version
   # Trebuie să fie 17.x
   ```

2. **Curăță cache:**
   ```bash
   ./android/gradlew clean --no-daemon
   rm -rf ~/.gradle/caches/
   ```

3. **Refresh dependencies:**
   ```bash
   ./android/gradlew --refresh-dependencies
   ```

#### OutOfMemoryError

**Simptom:** `java.lang.OutOfMemoryError: Java heap space`

**Soluție:** Mărește heap în `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
```

#### NDK Not Found

**Simptom:** `No version of NDK matched the requested version`

**Soluție:**

```bash
# Install NDK via Android Studio
# Tools → SDK Manager → SDK Tools → NDK (Side by side)

# Sau via command line:
sdkmanager "ndk;25.2.9519653"
```

### Runtime Issues

#### App Crashes on Launch

**Verifică:**

1. **NullClaw agent running:**
   ```bash
   adb logcat | grep "NullClaw"
   ```

2. **Model loaded:**
   ```bash
   adb shell "ls -la /sdcard/Android/data/com.loa.momclaw/files/models/"
   ```

3. **Permissions în manifest:**
   - `INTERNET`
   - `FOREGROUND_SERVICE`
   - `POST_NOTIFICATIONS`

#### Model Loading Failed

**Simptom:** `Failed to load model: gemma-3-e4b-it.litertlm`

**Soluții:**

1. **Verifică integritate:**
   ```bash
   # Download din nou
   ./scripts/download-model.sh ./models --force
   
   # Verifică dimensiunea (~2.5GB)
   ls -lh models/
   ```

2. **Storage suficient:**
   ```bash
   adb shell df -h /sdcard
   # Necesită 3GB+ free space
   ```

3. **Memory:**
   - Minim 4GB RAM pe device
   - Închide alte app-uri

#### Agent Not Responding

**Simptom:** Chat-ul nu răspunde, timeout errors

**Debug:**

1. **Verifică procesele:**
   ```bash
   adb shell ps -A | grep momclaw
   adb shell netstat -tulpn | grep 9090
   ```

2. **Verifică log-urile:**
   ```bash
   adb logcat -s MomClaw:* NullClaw:* LiteRT:*
   ```

3. **Restart agent:**
   ```kotlin
   // In SettingsRoute.kt
   viewModel.restartAgent()
   ```

### Network Issues

#### Can't Connect to Agent

**Simptom:** `Connection refused: localhost:9090`

**Soluții:**

1. **Verifică port:**
   ```bash
   adb shell netstat -tulpn | grep 9090
   ```

2. **Firewall:**
   - Android permite loopback by default
   - Verifică dacă VPN-ul blochează

3. **Service running:**
   ```bash
   adb shell dumpsys activity services com.loa.momclaw
   ```

### Performance Issues

#### Slow Inference

**Simptom:** Răspunsuri lente (>10 secunde)

**Optimizări:**

1. **Reducă max_tokens:**
   ```kotlin
   settings.maxTokens = 512
   ```

2. **Reducă temperatura:**
   ```kotlin
   settings.temperature = 0.5
   ```

3. **Close other apps:**
   - Liberează RAM
   - Disable background apps

#### High Battery Drain

**Simptom:** Bateria se consumă rapid

**Soluții:**

1. **Reduce usage:**
   ```kotlin
   settings.backgroundAgent = false
   ```

2. **Optimize model:**
   - Use Q4_K_M quantization
   - Avoid full-precision models

3. **Batch requests:**
   - Group messages
   - Reduce API calls

---

## Dezvoltare

### Code Style

- **Kotlin Official Style Guide**
- 4-space indentation
- Max line length: 120 characters
- Use detekt for static analysis

### Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/loa/momclaw/
│   │   │   ├── MOMCLAWApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/              # Compose screens
│   │   │   ├── data/            # Data layer
│   │   │   ├── domain/          # Business logic
│   │   │   └── di/              # Hilt modules
│   │   ├── res/                 # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── bridge/
│   ├── src/main/java/com/loa/momclaw/bridge/
│   │   ├── server/              # Ktor server
│   │   ├── inference/           # LiteRT-LM
│   │   └── api/                 # API endpoints
│   └── build.gradle.kts
├── agent/
│   ├── src/main/java/com/loa/momclaw/agent/
│   │   ├── core/                # Agent core
│   │   ├── tools/               # Tool implementations
│   │   └── memory/              # Memory management
│   └── build.gradle.kts
├── build.gradle.kts             # Top-level
├── settings.gradle.kts          # Project settings
└── gradle.properties            # Build config
```

### Naming Conventions

| Tip | Convenție | Exemplu |
|-----|-----------|---------|
| Files | PascalCase | `ChatRoute.kt` |
| Functions | camelCase | `sendMessage()` |
| Variables | camelCase | `messageContent` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_TOKENS` |
| Resources | snake_case | `ic_chat_send.xml` |

### Git Workflow

```
main         → Stable releases
develop      → Integration branch
feature/*    → Feature branches
hotfix/*     → Emergency fixes
```

### Commit Conventions

```
feat: add new chat feature
fix: resolve memory leak in agent
docs: update API documentation
test: add unit tests for repository
refactor: simplify bridge server
chore: update dependencies
```

---

## Testare

### Unit Tests

```bash
# Rulează toate unit tests
./android/gradlew testDebugUnitTest

# Rulează cu coverage
./android/gradlew testDebugUnitTestCoverage

# Raport coverage
open android/app/build/reports/coverage/test/debug/index.html
```

### Instrumented Tests

```bash
# Pe device/emulator
./android/gradlew connectedAndroidTest

# Specific module
./android/gradlew :app:connectedAndroidTest
```

### UI Tests (Compose)

```kotlin
@Test
fun chatScreen_displaysMessages() {
    composeTestRule.setContent {
        ChatRoute(viewModel = viewModel)
    }
    
    composeTestRule
        .onNodeWithText("Hello")
        .assertIsDisplayed()
}
```

### Test Scripts

**scripts/run-tests.sh:**

```bash
#!/usr/bin/env bash
set -e

echo "=== Running Unit Tests ==="
./android/gradlew testDebugUnitTest

echo "=== Running Lint ==="
./android/gradlew lintDebug

echo "=== Running Detekt ==="
./android/gradlew detekt

echo "=== All tests passed! ==="
```

---

## CI/CD

### GitHub Actions Workflows

#### ci.yml - Continuous Integration

Trigger: Push/PR pe `main` sau `develop`

**Steps:**
1. Checkout code
2. Setup JDK 17
3. Build debug APK
4. Run unit tests
5. Run lint
6. Upload artifacts

#### android-build.yml - Full Pipeline

Trigger: Push/PR pe `main` sau `develop`

**Jobs:**
- **build**: Matrix build (API 28/35, debug/release)
- **test**: Unit + instrumented tests
- **lint**: Detekt + Android lint
- **release**: Build signed AAB (doar pe main)

#### release.yml - Automated Releases

Trigger: Tag push `v*`

**Steps:**
1. Build signed release AAB
2. Create GitHub release
3. Upload AAB/APK
4. Generate release notes

### CI Best Practices

1. **Cache Gradle:**
   ```yaml
   - uses: actions/cache@v3
     with:
       path: ~/.gradle/caches
       key: gradle-${{ hashFiles('**/*.gradle.kts') }}
   ```

2. **Parallel jobs:**
   - Run tests in parallel
   - Split lint and build

3. **Artifact retention:**
   - Keep APKs for 30 days
   - Keep release builds indefinitely

### Manual Deployment

```bash
# Build release
./android/gradlew bundleRelease

# Upload to Google Play Console
# (via web UI or fastlane)

# Create Git tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# GitHub Actions va crea release automat
```

---

## Suport

### Documentație Adițională

- [BUILD.md](BUILD.md) - Build instructions detaliate
- [DEVELOPMENT.md](DEVELOPMENT.md) - Developer guide
- [SPEC.md](SPEC.md) - Specificații tehnice
- [MOMCLAW-PLAN.md](MOMCLAW-PLAN.md) - Roadmap

### Contact

- **GitHub Issues:** [momclaw/issues](https://github.com/serverul/momclaw/issues)
- **Discussions:** [momclaw/discussions](https://github.com/serverul/momclaw/discussions)
- **Email:** support@momclaw.app

### Contribuții

Vezi [CONTRIBUTING.md](CONTRIBUTING.md) pentru ghidul de contribuție.

---

**Built with ❤️ by LinuxOnAsteroids**

*Licensed under Apache License 2.0*

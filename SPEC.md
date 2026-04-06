# MOMCLAW — Specificație Tehnică (Hybrid Architecture)

> **Versiune:** 1.0.0-mvp  
> **Creat:** 2026-04-05  
> **Status:** Implementation Ready  
> **Package:** `com.loa.MOMCLAW`  
> **Repo:** https://github.com/serverul/MOMCLAW

---

## 📋 Cuprins

1. [Obiectiv](#obiectiv)
2. [Arhitectură Hybrid](#arhitectură-hybrid)
3. [Componente Tehnice](#componente-tehnice)
4. [Structura Proiect](#structura-proiect)
5. [Implementare Pas cu Pas](#implementare-pas-cu-pas)
6. [Testing](#testing)
7. [Acceptance Criteria](#acceptance-criteria)

---

## 🎯 Obiectiv

**MOMCLAW** = Agent AI 100% offline pe Android

**Ce face:**
- Agent AI complet (NullClaw) cu tools + memory
- Model Gemma 4E4B local (LiteRT-LM)
- Conversații fără internet
- UI chat simplu și rapid
- Extensibil cu canale (Telegram, Discord) post-MVP

**Stack:**
- **NullClaw** (Zig) — agent logic, tools, memory
- **LiteRT-LM** (Google) — inference Gemma 4
- **LiteRT Bridge** (Kotlin) — HTTP API între NullClaw și LiteRT
- **Android** (Kotlin + Compose) — UI + app logic

---

## 🏗️ Arhitectură Hybrid

### Overview

```
┌─────────────────────────────────────────────┐
│            MOMCLAW Android App              │
├─────────────────────────────────────────────┤
│  UI Layer (Kotlin + Compose)                │
│  • ChatScreen (messages list + input)       │
│  • SettingsScreen (temperature, prompt)     │
│  • ModelsScreen (download/switch models)    │
└─────────────────────────────────────────────┘
         │
         │ HTTP localhost:9090
         ▼
┌─────────────────────────────────────────────┐
│  NullClaw Agent (Zig binary, ARM64)         │
│  ┌───────────────────────────────────────┐  │
│  │  Agent Logic                          │  │
│  │  • Conversation management            │  │
│  │  • Tool dispatch (shell, files)       │  │
│  │  • Memory (SQLite)                    │  │
│  │  • System prompts                     │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │  Provider: custom HTTP                │  │
│  │  • base_url: http://localhost:8080    │  │
│  │  • OpenAI-compatible API              │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
         │
         │ HTTP POST /v1/chat/completions
         ▼
┌─────────────────────────────────────────────┐
│  LiteRT Bridge (Kotlin HTTP Server)         │
│  • Ktor server pe localhost:8080            │
│  • Endpoint: POST /v1/chat/completions      │
│  • Input: OpenAI chat format                │
│  • Output: SSE stream (data: {...})         │
│  • Loads Gemma 4E4B .litertlm model         │
└─────────────────────────────────────────────┘
         │
         │ LiteRT-LM API
         ▼
┌─────────────────────────────────────────────┐
│  LiteRT-LM Framework (Google)               │
│  • gemma-4-E4B-it-litertlm.litertlm         │
│  • Size: 3.65 GB                            │
│  • GPU acceleration (când e disponibil)     │
│  • Context: până la 32K tokens              │
│  • Decode: ~17 tok/sec (CPU mid-range)      │
└─────────────────────────────────────────────┘
```

### Flux Date (Chat Request)

```
1. User → ChatScreen: "Hello"
2. ChatViewModel → AgentClient: send("Hello")
3. AgentClient → NullClaw (localhost:9090): POST /chat
4. NullClaw:
   a. Load conversation from SQLite
   b. Apply system prompt
   c. Check for tool calls
   d. POST → LiteRT Bridge (localhost:8080/v1/chat/completions)
5. LiteRT Bridge:
   a. Convert OpenAI format → LiteRT format
   b. Load/verify model în memorie
   c. Call LiteRT-LM inference
   d. Stream tokens ca SSE
6. NullClaw:
   a. Receive SSE stream
   b. Forward to AgentClient
   c. Save response to SQLite
7. AgentClient:
   a. Parse SSE stream
   b. Emit tokens one by one
8. ChatViewModel:
   a. Update state cu fiecare token
   b. UI render (streaming effect)
```

---

## 🔧 Componente Tehnice

### 1. LiteRT Bridge (Kotlin HTTP Server)

**Tech Stack:**
- Kotlin + Ktor server
- LiteRT-LM SDK (Google)
- Coroutines pentru streaming

**Responsabilități:**
- Load model `.litertlm` la startup
- Expune endpoint OpenAI-compatible
- Convertește request format
- Streaming SSE responses

**API:**

```http
POST /v1/chat/completions
Content-Type: application/json

{
  "model": "gemma-4e4b",
  "messages": [
    {"role": "system", "content": "You are helpful."},
    {"role": "user", "content": "Hello"}
  ],
  "stream": true,
  "temperature": 0.7,
  "max_tokens": 2048
}
```

**Response (SSE):**
```
data: {"choices":[{"delta":{"content":"Hi"}}]}
data: {"choices":[{"delta":{"content":" there"}}]}
data: {"choices":[{"delta":{"content":"!"}}]}
data: [DONE]
```

**Cod exemplu:**

```kotlin
// LiteRTBridge.kt
class LiteRTBridge(private val context: Context) {
    
    private val engine = LlmEngine.getInstance(context)
    private var session: LlmSession? = null
    
    suspend fun start(modelPath: String, port: Int = 8080) {
        // Load model
        engine.loadModel(modelPath)
        session = engine.createSession()
        
        // Start Ktor server
        embeddedServer(Netty, port = port) {
            routing {
                post("/v1/chat/completions") {
                    handleChatCompletion(call)
                }
            }
        }.start(wait = true)
    }
    
    private suspend fun handleChatCompletion(call: ApplicationCall) {
        val request = call.receive<ChatRequest>()
        
        call.response.cacheControl(CacheControl.NoCache)
        call.respondTextWriter(ContentType.Text.EventStream) {
            session?.generate(
                prompt = formatPrompt(request.messages),
                temperature = request.temperature,
                maxTokens = request.maxTokens
            )?.collect { token ->
                write("data: ${formatSSE(token)}\n\n")
                flush()
            }
            write("data: [DONE]\n\n")
        }
    }
}
```

### 2. NullClaw Agent (Zig Binary)

**Tech Stack:**
- NullClaw compilat pentru ARM64 Android
- Config cu provider custom HTTP
- SQLite pentru memory

**Config:**

```json
{
  "agents": {
    "defaults": {
      "model": {
        "primary": "litert-bridge/gemma-4e4b"
      },
      "system_prompt": "You are MOMCLAW, a helpful AI assistant running offline on this device."
    }
  },
  "models": {
    "providers": {
      "litert-bridge": {
        "type": "custom",
        "base_url": "http://localhost:8080"
      }
    }
  },
  "memory": {
    "backend": "sqlite",
    "path": "/data/data/com.loa.MOMCLAW/databases/agent.db"
  },
  "tools": {
    "enabled": ["shell", "file_read", "file_write"]
  },
  "channels": {
    "cli": {
      "enabled": true
    }
  }
}
```

**Nota:** NullClaw are deja suport pentru provideri custom HTTP. Doar setăm `base_url` la localhost.

### 3. Android Application

**Tech Stack:**
- Kotlin 1.9+
- Jetpack Compose (BOM 2024.02.00)
- Room Database (pentru conversații UI)
- DataStore (preferences)
- OkHttp (HTTP client către NullClaw)
- WorkManager (pentru servicii background)

**Architecture:** MVVM + Clean Architecture

**Modules:**
- `app` — UI + business logic
- `bridge` — LiteRT HTTP server
- `agent` — NullClaw binary wrapper

---

## 📂 Structura Proiect

```
MOMCLAW/
├── android/
│   ├── app/                              # Main Android app
│   │   ├── src/main/
│   │   │   ├── java/com/loa/momclaw/
│   │   │   │   ├── MomClawApp.kt         # Application class
│   │   │   │   ├── MainActivity.kt       # Single Activity
│   │   │   │   │
│   │   │   │   ├── ui/                   # UI Layer
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   │   ├── ChatViewModel.kt
│   │   │   │   │   │   └── ChatState.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── ModelsScreen.kt
│   │   │   │   │   │   └── ModelsViewModel.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   └── navigation/
│   │   │   │   │       └── NavGraph.kt
│   │   │   │   │
│   │   │   │   ├── domain/               # Business Logic
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Message.kt
│   │   │   │   │   │   └── AgentConfig.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── AgentRepository.kt
│   │   │   │   │
│   │   │   │   ├── data/                 # Data Layer
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── database/
│   │   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   │   └── MessageDao.kt
│   │   │   │   │   │   └── preferences/
│   │   │   │   │   │       └── SettingsPreferences.kt
│   │   │   │   │   │
│   │   │   │   │   └── remote/
│   │   │   │   │       ├── AgentClient.kt       # NullClaw client
│   │   │   │   │       └── SSEParser.kt
│   │   │   │   │
│   │   │   │   └── service/              # Background Services
│   │   │   │       └── AgentService.kt   # Foreground service
│   │   │   │
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── build.gradle.kts
│   │
│   ├── bridge/                           # LiteRT HTTP Bridge
│   │   ├── src/main/
│   │   │   └── java/com/loa/momclaw/bridge/
│   │   │       ├── LiteRTBridge.kt       # Main bridge
│   │   │       ├── LlmEngineWrapper.kt   # LiteRT wrapper
│   │   │       ├── ChatRequest.kt        # Data classes
│   │   │       └── SSEWriter.kt          # SSE formatter
│   │   │
│   │   └── build.gradle.kts
│   │
│   ├── agent/                            # NullClaw Wrapper
│   │   ├── src/main/
│   │   │   ├── java/com/loa/momclaw/agent/
│   │   │   │   ├── NullClawBridge.kt     # Binary wrapper
│   │   │   │   └── ConfigGenerator.kt    # Config builder
│   │   │   │
│   │   │   └── assets/
│   │   │       └── nullclaw              # ARM64 binary
│   │   │
│   │   └── build.gradle.kts
│   │
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── native/
│   └── nullclaw/                         # Git submodule
│
├── models/
│   └── download-model.sh                 # Script pentru HF
│
├── docs/
│   ├── BUILD.md                          # Build instructions
│   ├── DEVELOPMENT.md                    # Dev guide
│   └── ARCHITECTURE.md                   # Architecture details
│
├── .github/
│   └── workflows/
│       └── android.yml                   # CI/CD
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🛠️ Implementare Pas cu Pas

### PHASE 1: Setup + Infrastructure (Săptămâna 1-2)

#### Step 1.1: Setup GitHub Repo

```bash
# Clone repo (already exists)
git clone https://github.com/serverul/MOMCLAW.git
cd MOMCLAW

# Add submodules
git submodule add https://github.com/nullclaw/nullclaw.git native/nullclaw

# Create structure
mkdir -p android/{app,bridge,agent}
mkdir -p models docs .github/workflows
```

#### Step 1.2: Android Project Setup

**settings.gradle.kts:**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MOMCLAW"
include(":app")
include(":bridge")
include(":agent")
```

**build.gradle.kts (root):**

```kotlin
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

**gradle.properties:**

```properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

#### Step 1.3: Build NullClaw pentru Android

```bash
cd native/nullclaw

# Cross-compile pentru ARM64 Android
zig build -Dtarget=aarch64-linux-android -Doptimize=ReleaseSmall

# Output: zig-out/bin/nullclaw
# Copy to android/agent/src/main/assets/
cp zig-out/bin/nullclaw ../../android/agent/src/main/assets/
```

---

### PHASE 2: LiteRT Bridge (Săptămâna 3)

#### Step 2.1: LiteRT Dependency

**android/bridge/build.gradle.kts:**

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.loa.MOMCLAW.bridge"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // LiteRT-LM (Google AI Edge)
    implementation("com.google.ai.edge:litert-lm:1.0.0")
    
    // Ktor server
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:serialization-kotlinx-json:2.3.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Log
    implementation("androidx.core:core-ktx:1.12.0")
}
```

#### Step 2.2: LiteRT Bridge Implementation

**android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt:**

```kotlin
package com.loa.MOMCLAW.bridge

import android.content.Context
import com.google.ai.edge.litert.LlmEngine
import com.google.ai.edge.litert.LlmSession
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2048
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

class LiteRTBridge(private val context: Context) {
    
    private var engine: LlmEngine? = null
    private var session: LlmSession? = null
    private var server: ApplicationEngine? = null
    
    suspend fun start(modelPath: String, port: Int = 8080): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize LiteRT engine
                engine = LlmEngine.getInstance(context).apply {
                    loadModel(modelPath)
                }
                session = engine?.createSession()
                
                // Start HTTP server
                server = embeddedServer(Netty, port = port) {
                    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                        register(io.ktor.http.ContentType.Application.Json, 
                                 io.ktor.serialization.kotlinx.json.JsonConverter(Json))
                    }
                    
                    routing {
                        post("/v1/chat/completions") {
                            handleChatCompletion(call)
                        }
                        
                        get("/health") {
                            call.respond(mapOf("status" to "ok"))
                        }
                    }
                }.start(wait = false)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun handleChatCompletion(call: ApplicationCall) {
        val request = call.receive<ChatRequest>()
        
        // Format prompt din messages
        val prompt = formatPrompt(request.messages)
        
        // Streaming response
        call.response.cacheControl(io.ktor.http.CacheControl.NoCache)
        call.respondTextWriter(io.ktor.http.ContentType.Text.EventStream) {
            session?.generate(
                prompt = prompt,
                temperature = request.temperature,
                maxTokens = request.max_tokens
            )?.collect { token ->
                val sseEvent = """data: {"choices":[{"delta":{"content":"$token"}}]}"""
                write("$sseEvent\n\n")
                flush()
            }
            write("data: [DONE]\n\n")
        }
    }
    
    private fun formatPrompt(messages: List<Message>): String {
        val builder = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                "system" -> builder.append("<|system|>\n${msg.content}\n")
                "user" -> builder.append("<|user|>\n${msg.content}\n")
                "assistant" -> builder.append("<|assistant|)\n${msg.content}\n")
            }
        }
        builder.append("<|assistant|)\n")
        return builder.toString()
    }
    
    fun stop() {
        server?.stop(0, 0)
        session?.close()
        engine = null
    }
}
```

---

### PHASE 3: NullClaw Agent Wrapper (Săptămâna 3-4)

#### Step 3.1: NullClaw Bridge

**android/agent/src/main/java/com/loa/momclaw/agent/NullClawBridge.kt:**

```kotlin
package com.loa.MOMCLAW.agent

import android.content.Context
import java.io.File
import java.io.IOException

class NullClawBridge(private val context: Context) {
    
    private var process: Process? = null
    private var configPath: String? = null
    
    fun setup(config: AgentConfig): Result<String> {
        return try {
            // Copy binary from assets
            val binaryFile = copyBinaryToStorage()
            binaryFile.setExecutable(true)
            
            // Generate config file
            val configFile = generateConfig(config)
            configPath = configFile.absolutePath
            
            Result.success("Setup complete")
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    fun start(): Result<Unit> {
        return try {
            val binaryPath = File(context.filesDir, "nullclaw").absolutePath
            
            val processBuilder = ProcessBuilder(
                binaryPath,
                "--config", configPath!!,
                "gateway",
                "--port", "9090"
            ).apply {
                redirectErrorStream(true)
                directory(context.filesDir)
            }
            
            process = processBuilder.start()
            
            // Wait for startup
            Thread.sleep(2000)
            
            if (process?.isAlive == true) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to start NullClaw"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun stop() {
        process?.destroy()
        process = null
    }
    
    fun isRunning(): Boolean = process?.isAlive == true
    
    private fun copyBinaryToStorage(): File {
        val outputFile = File(context.filesDir, "nullclaw")
        if (!outputFile.exists()) {
            context.assets.open("nullclaw").use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outputFile
    }
    
    private fun generateConfig(config: AgentConfig): File {
        val configFile = File(context.filesDir, "nullclaw-config.json")
        configFile.writeText(config.toJson())
        return configFile
    }
}

data class AgentConfig(
    val systemPrompt: String = "You are MOMCLAW, a helpful AI assistant running offline.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048
) {
    fun toJson(): String = """
    {
      "agents": {
        "defaults": {
          "model": {"primary": "litert-bridge/gemma-4e4b"},
          "system_prompt": "$systemPrompt"
        }
      },
      "models": {
        "providers": {
          "litert-bridge": {
            "type": "custom",
            "base_url": "http://localhost:8080"
          }
        }
      },
      "memory": {
        "backend": "sqlite",
        "path": "/data/data/com.loa.MOMCLAW/databases/agent.db"
      },
      "tools": {
        "enabled": ["shell", "file_read", "file_write"]
      }
    }
    """.trimIndent()
}
```

---

### PHASE 4: Android App UI (Săptămâna 4-5)

#### Step 4.1: App Dependencies

**android/app/build.gradle.kts:**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.loa.MOMCLAW"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.loa.MOMCLAW"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(project(":bridge"))
    implementation(project(":agent"))
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

#### Step 4.2: Chat ViewModel

**android/app/src/main/java/com/loa/momclaw/ui/chat/ChatViewModel.kt:**

```kotlin
package com.loa.MOMCLAW.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.MOMCLAW.data.remote.AgentClient
import com.loa.MOMCLAW.data.remote.MessageDto
import com.loa.MOMCLAW.domain.model.Message
import com.loa.MOMCLAW.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val currentResponse: String = "",
    val error: String? = null
)

sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class InputChanged(val text: String) : ChatEvent()
    object ClearConversation : ChatEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val agentClient: AgentClient
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    init {
        loadConversation()
    }
    
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendMessage -> sendMessage(event.text)
            is ChatEvent.InputChanged -> updateInput(event.text)
            is ChatEvent.ClearConversation -> clearConversation()
        }
    }
    
    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            // Add user message
            val userMessage = Message(
                role = "user",
                content = text,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(userMessage)
            
            _state.update { it.copy(
                inputText = "",
                isStreaming = true,
                messages = _state.value.messages + userMessage
            )}
            
            // Stream response
            val responseBuilder = StringBuilder()
            
            agentClient.chat(
                messages = _state.value.messages.map { 
                    MessageDto(it.role, it.content) 
                } + MessageDto("user", text)
            ).catch { e ->
                _state.update { it.copy(
                    error = "Error: ${e.message}",
                    isStreaming = false
                )}
            }.collect { token ->
                responseBuilder.append(token)
                _state.update { it.copy(
                    currentResponse = responseBuilder.toString()
                )}
            }
            
            // Save assistant message
            val assistantMessage = Message(
                role = "assistant",
                content = responseBuilder.toString(),
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(assistantMessage)
            
            _state.update { it.copy(
                messages = _state.value.messages + assistantMessage,
                currentResponse = "",
                isStreaming = false
            )}
        }
    }
    
    private fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }
    
    private fun clearConversation() {
        viewModelScope.launch {
            chatRepository.clearCurrentConversation()
            _state.update { ChatState() }
        }
    }
    
    private fun loadConversation() {
        viewModelScope.launch {
            chatRepository.getCurrentConversation()
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
        }
    }
}
```

#### Step 4.3: Chat Screen (Compose)

**android/app/src/main/java/com/loa/momclaw/ui/chat/ChatScreen.kt:**

```kotlin
package com.loa.MOMCLAW.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ChatEvent.ClearConversation) }) {
                        Icon(Icons.Default.Delete, "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = false
            ) {
                items(state.messages) { message ->
                    MessageBubble(
                        message = message,
                        isUser = message.role == "user"
                    )
                }
                
                // Streaming response
                if (state.isStreaming && state.currentResponse.isNotEmpty()) {
                    item {
                        MessageBubble(
                            message = Message(
                                role = "assistant",
                                content = state.currentResponse,
                                timestamp = System.currentTimeMillis()
                            ),
                            isUser = false
                        )
                    }
                }
            }
            
            // Error message
            state.error?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Input Area
            ChatInput(
                text = state.inputText,
                enabled = !state.isStreaming,
                onTextChange = { onEvent(ChatEvent.InputChanged(it)) },
                onSend = { onEvent(ChatEvent.SendMessage(state.inputText)) }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isUser: Boolean
) {
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val color = if (isUser) 
        MaterialTheme.colorScheme.primaryContainer
    else 
        MaterialTheme.colorScheme.surfaceVariant
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

---

### PHASE 5: Model Download (Săptămâna 5)

#### Step 5.1: HuggingFace Download

**models/download-model.sh:**

```bash
#!/bin/bash

# Download Gemma 4E4B LiteRT model
MODEL_NAME="gemma-4-E4B-it-litertlm.litertlm"
MODEL_URL="https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/$MODEL_NAME"

echo "Downloading Gemma 4E4B LiteRT model..."
echo "Size: ~3.65 GB"
echo "URL: $MODEL_URL"

wget -c "$MODEL_URL" -O "$MODEL_NAME"

echo "Download complete: $MODEL_NAME"
echo "Copy to device: adb push $MODEL_NAME /sdcard/MOMCLAW/models/"
```

#### Step 5.2: Models Screen (UI pentru download)

**android/app/src/main/java/com/loa/momclaw/ui/models/ModelsScreen.kt:**

```kotlin
package com.loa.MOMCLAW.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Model(
    val id: String,
    val name: String,
    val size: String,
    val downloaded: Boolean,
    val loaded: Boolean
)

@Composable
fun ModelsScreen(
    models: List<Model>,
    onDownloadModel: (String) -> Unit,
    onLoadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(models) { model ->
            ModelCard(
                model = model,
                onDownload = { onDownloadModel(model.id) },
                onLoad = { onLoadModel(model.id) },
                onDelete = { onDeleteModel(model.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ModelCard(
    model: Model,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "Size: ${model.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    model.loaded -> {
                        AssistChip(
                            onClick = { },
                            label = { Text("Loaded ✓") },
                            enabled = false
                        )
                    }
                    model.downloaded -> {
                        Button(onClick = onLoad) {
                            Text("Load")
                        }
                        OutlinedButton(onClick = onDelete) {
                            Text("Delete")
                        }
                    }
                    else -> {
                        Button(onClick = onDownload) {
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}
```

---

### PHASE 6: Integration + Testing (Săptămâna 6)

#### Step 6.1: Startup Sequence

**android/app/src/main/java/com/loa/momclaw/MomClawApp.kt:**

```kotlin
package com.loa.MOMCLAW

import android.app.Application
import com.loa.MOMCLAW.agent.NullClawBridge
import com.loa.MOMCLAW.bridge.LiteRTBridge
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MomClawApp : Application() {
    
    @Inject
    lateinit var liteRTBridge: LiteRTBridge
    
    @Inject
    lateinit var nullClawBridge: NullClawBridge
    
    private val appScope = CoroutineScope(Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Start services in background
        appScope.launch {
            startServices()
        }
    }
    
    private suspend fun startServices() {
        // 1. Start LiteRT Bridge (port 8080)
        val modelPath = getModelPath()
        liteRTBridge.start(modelPath, 8080)
            .onFailure { e ->
                // Log error, notify user
            }
        
        // 2. Start NullClaw Agent (port 9090)
        nullClawBridge.setup(AgentConfig())
        nullClawBridge.start()
            .onFailure { e ->
                // Log error, notify user
            }
    }
    
    private fun getModelPath(): String {
        // Check if model exists in internal storage
        val modelFile = File(filesDir, "models/gemma-4-E4B-it-litertlm.litertlm")
        return if (modelFile.exists()) {
            modelFile.absolutePath
        } else {
            // Return placeholder or trigger download
            ""
        }
    }
}
```

#### Step 6.2: Testing Checklist

**Manual Testing:**

1. ✅ App starts without crash
2. ✅ LiteRT Bridge starts on port 8080
3. ✅ NullClaw Agent starts on port 9090
4. ✅ Model loads successfully
5. ✅ Chat sends message
6. ✅ Response streams token by token
7. ✅ Conversation persists în SQLite
8. ✅ Settings save/load works
9. ✅ Model download from HF works
10. ✅ Offline mode (airplane mode) works

**Automated Testing:**

- Unit tests pentru ViewModels
- Integration tests pentru bridges
- UI tests pentru screens

---

## ✅ Acceptance Criteria (v1.0.0)

### Must Have

- [ ] Chat UI funcționează offline
- [ ] Model Gemma 4E4B se descarcă din HuggingFace
- [ ] Modelul se încarcă în LiteRT
- [ ] NullClaw pornește și se conectează la LiteRT Bridge
- [ ] Streaming responses vizibile în UI
- [ ] Istoric conversații salvat în SQLite
- [ ] Settings se salvează corect
- [ ] Nu crash-uiește pe ARM64 devices
- [ ] APK < 100MB (fără model)
- [ ] Token rate > 10 tok/sec

### Should Have

- [ ] Dark/Light theme
- [ ] Clear conversation button
- [ ] Model switch în settings
- [ ] Error messages user-friendly
- [ ] Loading states clare

### Won't Have (v1)

- Background service persistent
- Telegram/Discord channels
- Tool calls (complex)
- Cloud sync
- Multimodal input
- Multiple models loaded

---

## 📝 Note pentru Implementare

### Workflow

1. **Un pas la un timp** — nu sări la următorul fără să testezi
2. **Commit des** — pași mici, mesaje clare
3. **Testează pe device real** — emulatorul nu e suficient
4. **Loghează erorile** — folosește `adb logcat | grep MOMCLAW`
5. **Întreabă când te blochezi** — nu ghici

### Dacă te blochezi

1. Verifică logs
2. Testează componentele izolat
3. Simplifică — ia cel mai simplu path
4. Întreabă
5. Consultă docs (LiteRT, NullClaw, Android)

---

## 🔗 Resurse

### Frameworks & Libraries
- [LiteRT-LM](https://ai.google.dev/edge/litert-lm/overview) — Google AI Edge
- [NullClaw](https://github.com/nullclaw/nullclaw) — Agent framework
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — UI
- [Ktor](https://ktor.io/) — HTTP server

### Models
- [Gemma 4E4B LiteRT](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm)

### Documentation
- [Android NDK](https://developer.android.com/ndk/guides)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
- [Room Database](https://developer.android.com/training/data-storage/room)

---

**Acesta este spec-ul FINAL pentru MOMCLAW v1.0.0-mvp (Hybrid Architecture).**

---

*Spec creat — 5 Apr 2026*
*Architecture: LiteRT-LM + NullClaw Hybrid*

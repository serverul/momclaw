<p align="center">
  <img src="assets/icon.png" alt="MomClaw Logo" width="120" height="120">
</p>

<h1 align="center">MomClaw 🐾</h1>

<p align="center">
  <strong>Mobile Offline Model Agent — AI Agent 100% offline pe Android</strong>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-documentation">Documentation</a> •
  <a href="#-contributing">Contributing</a>
</p>

<p align="center">
  <a href="https://github.com/serverul/momclaw/releases">
    <img src="https://img.shields.io/github/v/release/serverul/momclaw?include_prereleases" alt="Release">
  </a>
  <a href="https://github.com/serverul/momclaw/actions">
    <img src="https://github.com/serverul/momclaw/workflows/CI/badge.svg" alt="CI Status">
  </a>
  <a href="https://opensource.org/licenses/Apache-2.0">
    <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License">
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg" alt="Kotlin">
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-brightgreen.svg" alt="Compose">
  </a>
</p>

---

## 🚀 Features

Un agent AI complet autonom care rulează pe telefon — zero cloud, zero tracking, 100% offline.

| Feature | Descriere |
|---------|-----------|
| 🧠 **Conversații inteligente** | Reasoning avansat cu Gemma 3 E4B-it |
| 🔧 **Tool calls native** | Shell, file operations, web search |
| 💾 **Memorie persistentă** | SQLite database cu conversații complete |
| 📱 **Canale externe** | Telegram și Discord integration (online mode) |
| 🔄 **OpenClaw Sync** | Sincronizare cu instanță principală OpenClaw |
| 🔒 **Privacy-first** | Toate datele rămân pe device |
| ⚡ **Performance** | Optimizat pentru mobile cu LiteRT-LM |
| 🌙 **Dark theme** | Material You design cu dynamic colors |

### Tech Stack

| Componentă | Tehnologie |
|------------|------------|
| Android App | Kotlin 2.0 + Jetpack Compose |
| Inference | LiteRT-LM (Google AI Edge) |
| Bridge | Ktor Server (Netty) |
| Agent | NullClaw (Zig) |
| Model | Gemma 3 E4B-it (Q4_K_M) |
| Memory | Room Database (SQLite) |
| DI | Hilt (Dagger) |
| Build | Gradle 8.9+ + CMake + Zig |

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/chat.png" width="200" alt="Chat Screen">
  <img src="assets/screenshots/models.png" width="200" alt="Models Screen">
  <img src="assets/screenshots/settings.png" width="200" alt="Settings Screen">
</p>

---

## 🏃 Quick Start

### Prerequisites

- **JDK 17** (obligatoriu)
- **Android SDK API 35** (Android 15)
- **Android NDK r25c+**
- **Android Studio Hedgehog** sau mai nou
- **Git**

### Build și Run

```bash
# 1. Clone repository
git clone https://github.com/serverul/momclaw.git
cd momclaw

# 2. Build debug APK
chmod +x android/gradlew
./android/gradlew assembleDebug

# 3. Install pe device
adb install android/app/build/outputs/apk/debug/app-debug.apk

# 4. Download model (~2.5GB)
./scripts/download-model.sh ./models
adb push models/gemma-3-E4B-it.litertlm \
    /sdcard/Android/data/com.loa.momclaw/files/models/
```

### Android Studio

1. **File → Open** → Selectează `momclaw/android`
2. Așteaptă Gradle sync (5-10 min prima dată)
3. Selectează device/emulator (API 28+)
4. Apasă **Run** (▶️) sau `Shift+F10`

---

## 📖 Documentation

| Documentație | Descriere |
|-------------|-----------|
| [DOCUMENTATION.md](DOCUMENTATION.md) | **Documentație completă** - setup, deployment, API, troubleshooting |
| [BUILD.md](BUILD.md) | Build instructions detaliate |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Developer guide și arhitectură |
| [SPEC.md](SPEC.md) | Specificații tehnice complete |
| [MOMCLAW-PLAN.md](MOMCLAW-PLAN.md) | Roadmap și planuri viitoare |

### API Documentation

**LiteRT Bridge API** (OpenAI-compatible):
- Base URL: `http://localhost:8080/v1`
- Endpoints: `/chat/completions`, `/models`, `/health`
- Full docs: [DOCUMENTATION.md#api-documentation](DOCUMENTATION.md#api-documentation)

**NullClaw Agent API**:
- Base URL: `http://localhost:9090`
- Endpoints: `/chat`, `/tool`
- Full docs: [DOCUMENTATION.md#nullclaw-agent-api](DOCUMENTATION.md#nullclaw-agent-api)

---

## 🏗️ Arhitectură

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

Vezi [DEVELOPMENT.md](DEVELOPMENT.md) pentru detalii complete.

---

## 🧪 Testare

```bash
# Unit tests
./android/gradlew testDebugUnitTest

# Instrumented tests (pe device)
./android/gradlew connectedAndroidTest

# Lint + Detekt
./android/gradlew lint detekt

# Coverage report
./android/gradlew testDebugUnitTestCoverage
```

---

## 🚀 Deployment

### Release Builds

```bash
# 1. Generează keystore (prima dată)
keytool -genkey -v -keystore momclaw-release-key.jks \
    -keyalg RSA -keysize 2048 -validity 10000 -alias momclaw

# 2. Creează key.properties
cat > android/key.properties << EOF
storePassword=YOUR_PASSWORD
keyPassword=YOUR_PASSWORD
keyAlias=momclaw
storeFile=../momclaw-release-key.jks
EOF

# 3. Build release APK
./android/gradlew assembleRelease

# 4. Build release AAB (Google Play)
./android/gradlew bundleRelease

# Output:
# APK: android/app/build/outputs/apk/release/app-release.apk
# AAB: android/app/build/outputs/bundle/release/app-release.aab
```

Vezi [DOCUMENTATION.md#deployment](DOCUMENTATION.md#deployment) pentru detalii complete.

---

## 🤝 Contributing

Contribuțiile sunt binevenite! Vezi [CONTRIBUTING.md](CONTRIBUTING.md) pentru ghidul complet.

### Proces

1. Fork repository
2. Creează feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push branch (`git push origin feature/amazing-feature`)
5. Deschide Pull Request

### Code Style

- Kotlin Official Style Guide
- 4-space indentation
- Max line length: 120 characters
- Run `./android/gradlew detekt` înainte de commit

---

## 📝 License

Acest proiect este licențiat sub Apache License 2.0 - vezi [LICENSE](LICENSE) pentru detalii.

---

## 🙏 Acknowledgments

- [NullClaw](https://github.com/nullclaw/nullclaw) - Agent implementation
- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Inference engine
- [PrivateAIEdgeGallery](https://github.com/serverul/PrivateAIEdgeGallery) - Inspiration
- [Google AI Edge](https://ai.google.dev/edge) - LiteRT-LM
- [Gemma](https://ai.google.dev/gemma) - Language model

---

## 📊 Status

| Feature | Status |
|---------|--------|
| Chat UI | ✅ Done |
| Model Management | ✅ Done |
| Settings | ✅ Done |
| LiteRT Integration | ✅ Done |
| NullClaw Agent | 🚧 In Progress |
| Telegram Channel | 📋 Planned |
| Discord Channel | 📋 Planned |
| OpenClaw Sync | 📋 Planned |

---

## 📞 Contact & Support

- **GitHub Issues:** [momclaw/issues](https://github.com/serverul/momclaw/issues)
- **Discussions:** [momclaw/discussions](https://github.com/serverul/momclaw/discussions)
- **Email:** momclaw@example.com (TODO)

---

<p align="center">
  <strong>Built with ❤️ by <a href="https://github.com/serverul">LinuxOnAsteroids</a></strong>
</p>

<p align="center">
  <sub>Inspirat de NullClaw + llama.cpp + PrivateAIEdgeGallery</sub>
</p>

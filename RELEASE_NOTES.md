# MomClAW v1.0.0 Release Notes

**Release Date**: 2026-04-06  
**Codename**: "Prime Claw"

---

## 🎉 First Stable Release

MomClAW v1.0.0 marks the first stable release of the Mobile Offline Model Agent - a fully autonomous AI assistant running 100% offline on Android devices.

---

## 🚀 Key Features

### Core Capabilities

- ✅ **100% Offline AI** - Gemma 3 E4B-it runs entirely on-device
- ✅ **Privacy-First Design** - No cloud, no tracking, no data leaves device
- ✅ **Intelligent Conversations** - Advanced reasoning with tool execution
- ✅ **Persistent Memory** - SQLite-based conversation history
- ✅ **Material You UI** - Modern Android design with dynamic colors

### Technical Stack

- **Android**: Kotlin 2.0.21 + Jetpack Compose BOM 2024.10.01
- **Inference**: LiteRT-LM (Google AI Edge)
- **Model**: Gemma 3 E4B-it Q4_K_M (~2.5GB)
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room (SQLite)

### Performance

- **Startup**: < 5 seconds on modern devices
- **Inference**: 15-30 tokens/sec on Snapdragon 8 Gen 2
- **Memory**: ~4GB RAM required
- **Storage**: 3GB+ free space for model

---

## 📱 Supported Devices

- **Minimum**: Android 9.0 (API 28)
- **Target**: Android 15 (API 35)
- **Architecture**: ARM64-v8a (recommended), ARMv7, x86_64
- **RAM**: 4GB minimum, 6GB+ recommended

---

## 🔧 Installation

### Google Play Store (Recommended)
1. Search for "MomClAW"
2. Install (~60MB download)
3. Open app, download model (~2.5GB)

### Manual APK
1. Download APK from [GitHub Releases](https://github.com/serverul/MOMCLAW/releases)
2. Install APK
3. Download model from in-app prompt

### Model Download
- **Size**: ~2.5GB
- **Source**: Hugging Face (verified)
- **Checksum**: SHA256 verified on download

---

## 🎯 What's Included

### Features

- ✅ Chat interface with conversation history
- ✅ Model management (download, load, unload)
- ✅ Settings (temperature, max tokens, theme)
- ✅ Dark theme + Material You dynamic colors
- ✅ Foreground service for background inference
- ✅ Room database for persistent storage

### Security

- ✅ Signed release builds
- ✅ ProGuard/R8 code shrinking + obfuscation
- ✅ Network Security Configuration (HTTPS only)
- ✅ No telemetry or analytics
- ✅ Local-only data storage

### CI/CD

- ✅ GitHub Actions workflows for build/test/deploy
- ✅ CodeQL security scanning
- ✅ Dependabot for dependency updates
- ✅ Automated release creation

---

## 🐛 Known Issues

1. **First model load is slow** (30-60 seconds) - Subsequent loads are faster
2. **Large model size** (~2.5GB) - Requires Wi-Fi for initial download
3. **Battery usage** - Inference is CPU/GPU intensive
4. **Memory pressure** - May be killed by system on low-RAM devices

---

## 📋 Coming in v1.1.0

- 🔄 **NullClaw Agent** - Autonomous tool execution
- 💬 **Telegram Channel** - External messaging integration
- 🎮 **Discord Channel** - Discord bot integration
- 🔄 **OpenClaw Sync** - Synchronization with main OpenClaw instance
- 📊 **Usage Statistics** - Local-only metrics dashboard

---

## 🛡️ Security

This release includes:

- Code signing with RSA 2048-bit key
- ProGuard/R8 obfuscation
- No hardcoded secrets
- HTTPS-only network connections
- Local-only data storage

See [SECURITY.md](SECURITY.md) for full security policy.

---

## 📚 Documentation

- [README.md](README.md) - Overview and quick start
- [USER_GUIDE.md](USER_GUIDE.md) - Complete user documentation
- [DOCUMENTATION.md](DOCUMENTATION.md) - Technical documentation
- [DEVELOPMENT.md](DEVELOPMENT.md) - Developer guide

---

## 🙏 Credits

- [Google AI Edge](https://ai.google.dev/edge) - LiteRT-LM inference
- [Gemma](https://ai.google.dev/gemma) - Language model
- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Inference engine
- [NullClaw](https://github.com/nullclaw/nullclaw) - Agent architecture

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)
- **Discussions**: [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions)
- **Email**: support@momclaw.app

---

**Built with ❤️ by LinuxOnAsteroids**

*Inspirat de NullClaw + llama.cpp + PrivateAIEdgeGallery*

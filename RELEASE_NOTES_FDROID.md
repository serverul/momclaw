# MomClAW v1.0.0 - F-Droid Release Notes

## Package Name
com.loa.momclaw

## Summary (79 chars max)
Privacy-first AI chatbot with offline inference using Gemma 3. No cloud, no tracking.

## Description

MomClAW is a fully offline AI assistant that respects your privacy. Using the Gemma 3 E4B-it model, it provides intelligent responses without sending any data to external servers.

### Key Features

**🔒 Privacy First**
- Completely offline operation after initial model download
- No cloud services or external APIs
- All data stored locally on device
- No account required
- No tracking or analytics

**💬 AI Chat**
- Real-time streaming chat responses
- Powered by Gemma 3 E4B-it model
- Contextual conversation memory
- Customizable AI parameters (temperature, max tokens)

**🛠️ Tool Execution**
- Shell command execution
- File operations (read, write, edit)
- Web tools (fetch, search)
- Extensible tool framework

**🎨 Modern UI**
- Material 3 design with dynamic theming
- Dark theme support
- Smooth animations and transitions
- Responsive layout for all screen sizes

**📱 Model Management**
- Download models on-demand
- Load/unload models to save memory
- Multiple model support planned

### Technical Details

**Architecture**
- Clean Architecture with MVVM pattern
- Multi-module structure (app, bridge, agent)
- Repository pattern for data access
- Kotlin coroutines and Flow

**Tech Stack**
- Kotlin 2.0.21
- Jetpack Compose BOM 2024.10.01
- Hilt 2.52 for dependency injection
- Room 2.6.1 for local database
- DataStore 1.1.1 for preferences
- Ktor 2.3.8 for HTTP server
- OkHttp 4.12.0 for networking
- LiteRT-LM 1.0.0 for inference
- Gradle 8.9+ with Kotlin DSL

**Build Requirements**
- Android SDK (API 34)
- JDK 17
- 4GB+ RAM for compilation
- NDK is NOT required (no native code in this version)

**Supported Architectures**
- arm64-v8a (primary)
- armeabi-v7a (optional)
- x86_64 (for emulators/testing)

### Requirements

**Minimum**
- Android 8.0+ (API 26+)
- 2GB RAM
- 3GB free storage (2GB for model + space for app)

**Recommended**
- Android 10+ (API 29+)
- 4GB+ RAM
- 4GB+ free storage

### What's New in v1.0.0

**🎉 Initial Release**

This is the first public release of MomClAW, featuring:

- Complete chat interface with Material 3 design
- Offline AI inference using Gemma 3 E4B-it
- Tool execution framework (shell, file, web)
- Persistent conversation memory with SQLite
- Model management system
- Customizable AI parameters
- HTTP bridge for external integrations
- Dark theme and dynamic colors
- Foreground service for background operation

### Known Limitations

1. **Initial Model Download**
   - Requires internet connection on first run
   - Model size: ~2GB
   - Download progress shown in UI

2. **Performance**
   - First inference may be slow (model loading)
   - Battery usage during inference is significant
   - Devices with <4GB RAM may experience lag

3. **Storage**
   - Model is stored in app private directory
   - Cannot be moved to SD card (Android security)
   - Uninstalling app deletes model (must redownload)

4. **Network**
   - Tool execution features may require internet
   - Web fetch/search tools need connectivity
   - Bridge API accessible on localhost only

### Free Software

**License:** GPL-3.0

**Source Code:** https://github.com/serverul/momclaw

**No Proprietary Dependencies:**
- All libraries are open source
- No Google Play Services required
- No proprietary inference engines
- Fully reproducible builds

**Contributing:**
- Issues: https://github.com/serverul/momclaw/issues
- Pull Requests: Welcome!
- Discussions: https://github.com/serverul/momclaw/discussions

### Permissions

**Required:**
- `INTERNET` - For initial model download and web tools
- `FOREGROUND_SERVICE` - For background agent operation
- `POST_NOTIFICATIONS` - For foreground service notification (Android 13+)

**Not Required:**
- No access to contacts, location, SMS, or phone state
- No background data collection
- No analytics or tracking

### Building from Source

```bash
# Clone repository
git clone https://github.com/serverul/momclaw.git
cd momclaw

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

### Security & Privacy

**Data Handling:**
- All chat history stored in local SQLite database
- No data transmitted to external servers
- No telemetry or crash reporting (yet)
- No third-party SDKs for analytics

**Model Safety:**
- Gemma 3 model downloaded from official sources
- Model integrity verified via checksums
- Model stored in app private directory (protected by Android sandbox)

**Network Security:**
- HTTP bridge binds to localhost only (127.0.0.1)
- No external ports exposed
- CORS disabled by default
- API key authentication optional

### Roadmap

**v1.1.0 (Planned)**
- Telegram channel integration
- Discord channel integration
- Enhanced memory management
- Multiple model support

**v1.2.0 (Planned)**
- OpenClaw sync functionality
- Conversation export/import
- Custom system prompts
- Advanced tool configuration

**v2.0.0 (Future)**
- Multi-agent support
- Plugin system
- Cloud backup (optional, disabled by default)
- Web interface

### Support

**Documentation:**
- README.md - Quick start guide
- BUILD.md - Build instructions
- DEVELOPMENT.md - Developer guide
- SPEC.md - Technical specifications

**Community:**
- GitHub Issues: Bug reports and feature requests
- GitHub Discussions: Questions and general discussion

---

**Version Code:** 1  
**Version Name:** 1.0.0  
**MinSDK:** 26 (Android 8.0)  
**TargetSDK:** 34 (Android 14)  
**Release Date:** 2026-04-05

**Maintainer:** serverul  
**License:** GPL-3.0-or-later  
**Website:** https://github.com/serverul/momclaw

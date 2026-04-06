# Changelog

All notable changes to MOMCLAW will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **BUILD-DEPLOYMENT-GUIDE.md** - Comprehensive all-in-one build and deployment reference
- **PRODUCTION-CHECKLIST.md** - Single-source checklist for production releases
- **PRIVACY_POLICY.md** - Complete privacy policy for store submission
- **Screenshots documentation** - Guide for capturing store screenshots
- **Fastlane screenshots structure** - Directory structure for all screen sizes
- **DEPLOYMENT.md** - Comprehensive deployment guide for Google Play Store and F-Droid
- **Fastlane configuration** - Automated Google Play deployment with multiple tracks
- **CI/CD automation scripts**:
  - `scripts/ci-build.sh` - Main automation script for all operations
  - `scripts/build-release.sh` - Build release APK + AAB
  - `scripts/build-fdroid.sh` - Build F-Droid compatible APK
  - `scripts/validate-build.sh` - Pre-release validation
  - `scripts/run-tests.sh` - Run all test suites
- **Google Play workflows**:
  - `.github/workflows/google-play-deploy.yml` - Deploy to any track (internal/alpha/beta/production)
  - `.github/workflows/fdroid-build.yml` - Build for F-Droid repository
- **Store metadata**:
  - Title, short/full descriptions
  - Changelogs per version
  - Screenshots directory structure
- Comprehensive DOCUMENTATION.md with setup, deployment, API docs, and troubleshooting
- CONTRIBUTING.md guide for contributors
- CI/CD workflows (release.yml, security.yml)
- Optimized gradle.properties with performance settings
- Consumer ProGuard rules for all modules
- Enhanced ProGuard rules for app module
- key.properties.example for signing configuration

### Changed
- Updated README.md with badges, screenshots placeholders, and better structure
- Improved build.gradle.kts for all modules with signing config and optimizations
- Enhanced app/build.gradle.kts with comprehensive dependencies and build features
- Better deployment documentation and automation

### Fixed
- Signing configuration for release builds
- Module dependency versions alignment

## [1.0.0] - 2026-04-06

### Added
- Initial release
- Chat UI with Jetpack Compose and Material 3
- Model management screen
- Settings screen with preferences
- LiteRT-LM integration for on-device inference
- NullClaw agent integration
- Ktor-based HTTP bridge (OpenAI-compatible API)
- Room database for message persistence
- DataStore for app preferences
- Hilt dependency injection
- Navigation component
- Foreground service for background agent
- Gemma 3 E4B-it model support

### Features
- 💬 Real-time chat with streaming responses
- 🧠 Offline AI inference (no cloud required)
- 🔧 Tool execution (shell, file, web)
- 💾 Persistent memory (SQLite)
- 📱 Material You design with dynamic colors
- 🌙 Dark theme support
- 📊 Model management (download, load, unload)
- ⚙️ Customizable settings (temperature, max tokens, etc.)
- 🔒 Privacy-first (all data stays on device)

### Architecture
- Multi-module structure (app, bridge, agent)
- Clean architecture (data/domain/ui layers)
- MVVM with ViewModels
- Repository pattern
- Kotlin coroutines and Flow
- Jetpack Compose for UI

### Tech Stack
- Kotlin 2.0.21
- Jetpack Compose BOM 2024.10.01
- Hilt 2.52
- Room 2.6.1
- DataStore 1.1.1
- Ktor 2.3.8
- OkHttp 4.12.0
- LiteRT-LM 1.0.0
- Gradle 8.9+

### Documentation
- README.md with quick start guide
- BUILD.md with detailed build instructions
- DEVELOPMENT.md with developer guide
- SPEC.md with technical specifications
- MOMCLAW-PLAN.md with roadmap

### CI/CD
- GitHub Actions workflow for CI (ci.yml)
- Extended Android build pipeline (android-build.yml)

---

## Version History

| Version | Release Date | Highlights |
|---------|--------------|------------|
| 1.0.0 | 2026-04-05 | Initial release |

---

## Roadmap

### v1.1.0 (Planned)
- [ ] Telegram channel integration
- [ ] Discord channel integration
- [ ] Enhanced memory management
- [ ] Multiple model support

### v1.2.0 (Planned)
- [ ] OpenClaw sync functionality
- [ ] Conversation export/import
- [ ] Custom system prompts
- [ ] Advanced tool configuration

### v2.0.0 (Future)
- [ ] Multi-agent support
- [ ] Plugin system
- [ ] Cloud backup (optional)
- [ ] Web interface

---

[Unreleased]: https://github.com/serverul/MOMCLAW/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/serverul/MOMCLAW/releases/tag/v1.0.0

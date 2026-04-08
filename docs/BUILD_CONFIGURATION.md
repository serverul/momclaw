# MOMCLAW Build Configuration Guide

Complete guide for configuring, building, and deploying MOMCLAW Android application.

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## Table of Contents

- [Build Overview](#build-overview)
- [Prerequisites](#prerequisites)
- [Signing Configuration](#signing-configuration)
- [Build Variants](#build-variants)
- [APK Splits Configuration](#apk-splits-configuration)
- [ProGuard Configuration](#proguard-configuration)
- [Version Management](#version-management)
- [Dependency Management](#dependency-management)
- [Security Scanning](#security-scanning)
- [CI/CD Pipeline](#cicd-pipeline)
- [Release Process](#release-process)
- [Troubleshooting](#troubleshooting)

---

## Build Overview

MOMCLAW uses Gradle with Kotlin DSL for build configuration. The project consists of:

```
android/
├── app/           # Main application module
├── bridge/        # Ktor server + LiteRT bridge
├── agent/         # NullClaw agent integration
├── build.gradle.kts
└── settings.gradle.kts
```

### Build Outputs

| Build Type | Output | Size | Purpose |
|------------|--------|------|---------|
| Debug | `app-debug.apk` | ~150MB | Development & Testing |
| Release | `app-release.apk` | ~80MB | Production Distribution |
| Release AAB | `app-release.aab` | ~70MB | Google Play Store |

### APK Splits (Optimized)

For reduced download size, APK splits are configured per ABI:

| ABI | Devices | Split APK Size |
|-----|---------|----------------|
| `arm64-v8a` | Modern devices (64-bit) | ~60MB |
| `armeabi-v7a` | Legacy devices (32-bit) | ~55MB |
| `x86_64` | Emulators, Chromebooks | ~65MB |
| Universal | All devices | ~90MB |

---

## Prerequisites

### Required Tools

```bash
# Check versions
java -version          # JDK 17+
./android/gradlew --version  # Gradle 8.9+

# Android SDK
# API 35 (Android 15)
# Build Tools 35.0.0
# NDK r25c+
# CMake 3.22.1
```

### Environment Variables

```bash
# ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

### Initialize Project

```bash
# Clone and setup
git clone https://github.com/serverul/MOMCLAW.git
cd MOMCLAW
chmod +x scripts/*.sh
./scripts/setup.sh
```

---

## Signing Configuration

### Generate Keystore

```bash
# Generate release keystore
./scripts/ci-build.sh keystore:generate

# Or manually
keytool -genkeypair \
  -alias MOMCLAW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore MOMCLAW-release-key.jks \
  -storetype PKCS12

# Output:
# MOMCLAW-release-key.jks (keep this secure!)
```

### Configure Signing

Create `android/key.properties` (DO NOT commit this file):

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=MOMCLAW
storeFile=../MOMCLAW-release-key.jks
```

### GitHub Secrets (CI/CD)

For automated builds, configure GitHub secrets:

```bash
# Required secrets (Settings → Secrets → Actions)
KEYSTORE_BASE64         # base64 encoded keystore file
STORE_PASSWORD          # keystore password
KEY_PASSWORD            # key password
KEY_ALIAS               # key alias (MOMCLAW)

# Encode keystore
base64 -w 0 MOMCLAW-release-key.jks > keystore_base64.txt
# Copy content to KEYSTORE_BASE64 secret
```

See [`.github/SECRETS_SETUP.md`](.github/SECRETS_SETUP.md) for detailed instructions.

---

## Build Variants

### Debug Build

```bash
# Quick debug build
./android/gradlew assembleDebug

# Output: android/app/build/outputs/apk/debug/app-debug.apk
```

**Debug Features**:
- Application ID suffix: `.debug`
- Version name suffix: `-DEBUG`
- Debugging enabled
- No ProGuard
- Larger APK size

### Release Build

```bash
# Release build (requires signing config)
./android/gradlew assembleRelease

# Output: android/app/build/outputs/apk/release/app-release.apk
```

**Release Features**:
- ProGuard enabled
- Resource shrinking
- Optimized APK
- Signed with release key
- Production-ready

### Build AAB (Google Play)

```bash
# Android App Bundle
./android/gradlew bundleRelease

# Output: android/app/build/outputs/bundle/release/app-release.aab
```

---

## APK Splits Configuration

APK splits are configured in `android/app/build.gradle.kts` to reduce download size:

```kotlin
android {
    // ... existing config ...
    
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}
```

### Build Split APKs

```bash
# Build splits
./android/gradlew assembleRelease

# Outputs:
# app-arm64-v8a-release.apk    (~60MB)
# app-armeabi-v7a-release.apk  (~55MB)
# app-x86_64-release.apk       (~65MB)
# app-universal-release.apk    (~90MB)
```

### Version Code Suffixes

Each split gets a unique version code suffix:

| ABI | Suffix | Example (base: 100) |
|-----|--------|---------------------|
| `arm64-v8a` | 1 | 1001 |
| `armeabi-v7a` | 2 | 1002 |
| `x86` | 3 | 1003 |
| `x86_64` | 4 | 1004 |
| Universal | 0 | 1000 |

---

## ProGuard Configuration

### Overview

MOMCLAW uses ProGuard for:
- Code shrinking (removes unused code)
- Resource shrinking (removes unused resources)
- Code obfuscation (optional)
- Optimization (7 passes)

### Configuration Files

```
android/app/
├── proguard-rules.pro          # Custom rules
└── proguard/
    ├── project-rules.pro       # App-specific rules
    ├── androidx-rules.pro      # AndroidX libraries
    ├── kotlin-rules.pro        # Kotlin + Coroutines
    ├── hilt-rules.pro          # Dependency injection
    ├── litert-rules.pro        # LiteRT-LM + TensorFlow
    └── optimization-rules.pro  # Optimization settings
```

### Build with ProGuard

```bash
# Release build with ProGuard
./android/gradlew assembleRelease

# Check ProGuard output
ls android/app/build/outputs/mapping/release/
```

### ProGuard Outputs

| File | Purpose |
|------|---------|
| `mapping.txt` | Maps obfuscated to original names |
| `configuration.txt` | Effective configuration |
| `seeds.txt` | Kept classes/members |
| `usage.txt` | Removed code |
| `resources.txt` | Resource optimization log |

### Debug ProGuard Issues

```bash
# Disable shrinking for debugging
# In build.gradle.kts:
isMinifyEnabled = false
isShrinkResources = false

# Or keep specific classes
# In proguard-rules.pro:
-keep class com.example.MyClass { *; }
```

### Retrace Stack Traces

```bash
# Retrace obfuscated stack trace
retrace \
  android/app/build/outputs/mapping/release/mapping.txt \
  crash-stacktrace.txt
```

---

## Version Management

### Using Version Manager Script

```bash
# Show current version
./scripts/version-manager.sh current

# Set specific version
./scripts/version-manager.sh set 1.2.0

# Bump version
./scripts/version-manager.sh bump minor    # 1.0.0 → 1.1.0
./scripts/version-manager.sh bump major    # 1.0.0 → 2.0.0
./scripts/version-manager.sh bump patch    # 1.0.0 → 1.0.1

# Prepare release (updates all references + git tag)
./scripts/version-manager.sh release 1.0.0
```

### Version Format

MOMCLAW follows [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE]

Examples:
  1.0.0          - Stable release
  1.0.1          - Patch release (bug fixes)
  1.1.0          - Minor release (new features)
  2.0.0          - Major release (breaking changes)
  1.0.0-beta.1   - Beta release
  1.0.0-rc.1     - Release candidate
  1.0.0-SNAPSHOT - Development snapshot
```

### Version Code Strategy

Version code is calculated to support multi-APK:

```
VERSION_CODE = MAJOR * 10000 + MINOR * 100 + PATCH

Examples:
  1.0.0 → 10000
  1.0.1 → 10001
  1.1.0 → 10100
  2.0.0 → 20000
```

### Automated Version Updates

The version manager updates:
- `android/app/build.gradle.kts` (versionName, versionCode)
- `README.md` (version badge)
- `CHANGELOG.md` (adds new version header)
- `DOCUMENTATION-INDEX.md` (version info)
- Creates `version.json` for build metadata
- Creates git tag

---

## Dependency Management

### Gradle Dependencies

All dependencies are declared in `android/app/build.gradle.kts`:

```kotlin
dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    
    // ... etc
}
```

### Update Dependencies

```bash
# Check for updates
./android/gradlew dependencyUpdates

# Update specific dependency
# Edit build.gradle.kts, then:
./android/gradlew clean build
```

### Dependency Security

```bash
# Run security scan
./android/gradlew dependencyCheckAnalyze

# Check for vulnerabilities
# Reports: android/app/build/reports/dependency-check/
```

### Dependency Constraints

Use version catalogs for consistent versions:

```kotlin
// gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.10.01"
hilt = "2.52"

[libraries]
androidx-core = { module = "androidx.core:core-ktx", version.ref = "core" }
```

---

## Security Scanning

### Automated Scans

GitHub Actions runs security scans automatically:

1. **Dependency Check** - CVE vulnerabilities
2. **CodeQL** - Code security analysis
3. **Secrets Scan** - TruffleHog + Gitleaks
4. **Security Lint** - Android security checks

### Run Security Scans Locally

```bash
# Dependency vulnerability scan
./android/gradlew dependencyCheckAnalyze

# Android lint (security checks)
./android/gradlew lint -Pandroid.lint.checks=Security

# CodeQL (requires setup)
# See: https://github.com/github/codeql

# Secrets scan
trufflehog git file://. --only-verified
gitleaks detect --source . --verbose
```

### Security Reports

All security scan reports are uploaded to GitHub Actions artifacts:
- `dependency-check-report`
- `codeql-results-java-kotlin`
- `security-lint-report`

---

## CI/CD Pipeline

### Workflow Overview

```
┌─────────────┐
│   Push/PR   │
└──────┬──────┘
       │
       ├─────► Build (debug/release)
       │
       ├─────► Test (unit + instrumented)
       │
       ├─────► Lint (detekt + Android lint)
       │
       └─────► Security Scan
               │
               ▼
         ┌─────┴─────┐
         │  Release? │
         └─────┬─────┘
               │ YES
               ▼
         ┌──────────────┐
         │ Build Release│
         │ Sign APK/AAB │
         └──────┬───────┘
                │
                ├─────► GitHub Release
                │
                ├─────► Google Play (alpha/beta/prod)
                │
                └─────► F-Droid (optional)
```

### GitHub Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `android-build.yml` | Push to main/develop | Build, test, lint |
| `release.yml` | Tag push `v*` | Build and publish release |
| `security.yml` | Push + weekly | Security scans |
| `google-play-deploy.yml` | Manual | Deploy to Play Store |
| `fdroid-build.yml` | Manual | Build for F-Droid |

### Trigger Builds

```bash
# Automatic: Push to main/develop
git push origin main

# Manual release: Create tag
./scripts/version-manager.sh release 1.0.0
git push --tags

# Manual workflow dispatch
# Go to Actions → Select workflow → Run workflow
```

### Build Commands

```bash
# Using main build script
./scripts/ci-build.sh build:debug
./scripts/ci-build.sh build:release 1.0.0
./scripts/ci-build.sh deploy:internal    # Play Store internal test
./scripts/ci-build.sh deploy:alpha       # Play Store alpha
./scripts/ci-build.sh deploy:beta        # Play Store beta
./scripts/ci-build.sh deploy:production  # Play Store production
./scripts/ci-build.sh deploy:github 1.0.0
./scripts/ci-build.sh build:fdroid 1.0.0

# Using Makefile
make debug
make release VERSION=1.0.0
make aab
make test
make lint
```

---

## Release Process

### Pre-Release Checklist

1. **Code Quality**
   - [ ] All tests passing
   - [ ] Lint clean
   - [ ] No security vulnerabilities
   - [ ] Code reviewed

2. **Version Updates**
   - [ ] Update version in `build.gradle.kts`
   - [ ] Update `CHANGELOG.md`
   - [ ] Update documentation if needed
   - [ ] Generate version info

3. **Assets**
   - [ ] Screenshots updated
   - [ ] App icons verified
   - [ ] Graphics assets ready

4. **Configuration**
   - [ ] Signing config ready
   - [ ] ProGuard rules tested
   - [ ] Release notes prepared

5. **Testing**
   - [ ] Tested on multiple devices
   - [ ] Tested upgrade path
   - [ ] Performance verified
   - [ ] Memory usage acceptable

### Release Steps

```bash
# 1. Prepare release
./scripts/version-manager.sh release 1.0.0

# 2. Review changes
git log
git diff HEAD~1

# 3. Push to trigger CI/CD
git push && git push --tags

# 4. Monitor build
# Go to: https://github.com/serverul/MOMCLAW/actions

# 5. Verify release
# Download APK from GitHub Releases
# Test on device

# 6. Deploy to Play Store (if needed)
./scripts/ci-build.sh deploy:internal
# or: deploy:alpha, deploy:beta, deploy:production

# 7. Update F-Droid (if needed)
# Submit merge request to fdroiddata repository
```

### Post-Release

```bash
# 1. Bump to next snapshot
./scripts/version-manager.sh snapshot

# 2. Update documentation
# - README.md
# - CHANGELOG.md (add Unreleased section)
# - Any affected docs

# 3. Announce release
# - GitHub Discussions
# - Social media
# - Discord/Telegram channels

# 4. Monitor
# - Crash reports
# - User feedback
# - Download metrics
```

---

## Troubleshooting

### Common Build Issues

#### Gradle Sync Failed

```bash
# Clean gradle cache
rm -rf ~/.gradle/caches/
rm -rf android/.gradle/
./android/gradlew clean

# Invalidate Android Studio caches
# File → Invalidate Caches / Restart
```

#### Signing Failed

```bash
# Verify keystore
keytool -list -keystore MOMCLAW-release-key.jks

# Check key.properties
cat android/key.properties

# Verify paths are correct
# storeFile should be relative to android/ directory
```

#### ProGuard Errors

```bash
# Check which class is being removed
cat android/app/build/outputs/mapping/release/usage.txt | grep "ClassName"

# Add keep rule
echo "-keep class com.example.ClassName { *; }" >> android/app/proguard-rules.pro

# Or disable ProGuard temporarily for debugging
# In build.gradle.kts: isMinifyEnabled = false
```

#### NDK Build Failed

```bash
# Check NDK version
cat android/local.properties | grep ndk

# Install correct NDK version
sdkmanager "ndk;25.2.9519653"

# Clean native builds
./android/gradlew clean
rm -rf android/app/.cxx/
rm -rf android/app/.externalNativeBuild/
```

#### Out of Memory

```bash
# Increase Gradle memory
# Edit android/gradle.properties:
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Or use daemon with more memory
./android/gradlew assembleRelease --no-daemon -Dorg.gradle.jvmargs="-Xmx4g"
```

### Debug Build Issues

```bash
# Enable verbose logging
./android/gradlew assembleDebug --info --stacktrace

# Check build scan
./android/gradlew assembleDebug --scan

# View detailed error
./android/gradlew assembleDebug --stacktrace 2>&1 | less
```

### Test Failures

```bash
# Run specific test
./android/gradlew test --tests "com.loa.momclaw.MyTest"

# Run with logging
./android/gradlew test --info --stacktrace

# Generate coverage report
./android/gradlew testDebugUnitTestCoverage
```

### CI/CD Issues

```bash
# Check workflow logs
# Go to Actions → Select failed run → View logs

# Download artifacts
# Scroll to bottom of workflow run → Artifacts

# Re-run failed job
# Click "Re-run jobs" in workflow run

# Debug locally with same setup
# Check .github/workflows/*.yml for exact commands
```

---

## Additional Resources

### Documentation

- [README.md](README.md) - Project overview
- [DEVELOPMENT.md](DEVELOPMENT.md) - Developer guide
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide
- [TESTING.md](TESTING.md) - Testing guide
- [PRODUCTION-CHECKLIST.md](PRODUCTION-CHECKLIST.md) - Production checklist

### External Links

- [Gradle Documentation](https://docs.gradle.org/)
- [Android Build Guide](https://developer.android.com/build)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Google Play Console](https://play.google.com/console)
- [F-Droid Manual](https://f-droid.org/en/docs/)

### Support

- **GitHub Issues**: [MOMCLAW/issues](https://github.com/serverul/MOMCLAW/issues)
- **Discussions**: [MOMCLAW/discussions](https://github.com/serverul/MOMCLAW/discussions)
- **Email**: support@MOMCLAW.app

---

**This document is maintained alongside the codebase. Last updated: 2026-04-06**

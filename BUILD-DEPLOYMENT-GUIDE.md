# MomClaw Build & Deployment Guide

**Complete guide for building, testing, and deploying MomClaw**

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Building the App](#building-the-app)
4. [Testing](#testing)
5. [Signing Configuration](#signing-configuration)
6. [Deployment Overview](#deployment-overview)
7. [Google Play Store Deployment](#google-play-store-deployment)
8. [F-Droid Deployment](#f-droid-deployment)
9. [GitHub Releases](#github-releases)
10. [CI/CD Automation](#cicd-automation)
11. [Troubleshooting](#troubleshooting)

---

## 🛠️ Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| JDK | 17+ | Kotlin/Gradle compilation |
| Android SDK | API 35 | Android build tools |
| Android NDK | r25c+ | Native code compilation |
| Git | 2.0+ | Version control |
| Ruby | 3.2+ | Fastlane (optional) |
| GPG | 2.0+ | F-Droid signing (optional) |

### Required Accounts

| Platform | Purpose | Cost |
|----------|---------|------|
| Google Play Developer | Play Store deployment | $25 one-time |
| GitHub | Source hosting, releases | Free |
| F-Droid | Alternative distribution | Free |

### System Requirements

- **OS**: Linux, macOS, or Windows with WSL2
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 20GB free space (for builds, dependencies, and model)

---

## 🔧 Environment Setup

### 1. Install JDK 17

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-17-jdk-headless

# macOS
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # Linux
export JAVA_HOME=/usr/local/opt/openjdk@17            # macOS
```

### 2. Install Android SDK

```bash
# Download commandlinetools
# https://developer.android.com/studio#command-line-tools-only

# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Install required components
sdkmanager "platforms;android-35" "build-tools;35.0.0" "ndk;25.2.9519653"
```

### 3. Clone Repository

```bash
git clone https://github.com/serverul/momclaw.git
cd momclaw
```

### 4. Initial Setup

```bash
# Run setup script (validates environment)
./scripts/setup.sh

# Or manually verify
java -version           # Should show 17+
./android/gradlew --version  # Should show 8.9+
```

---

## 🏗️ Building the App

### Quick Commands

```bash
# Debug APK (fastest)
make build
# OR
./scripts/ci-build.sh build:debug

# Release APK
make release VERSION=1.0.0
# OR
./scripts/ci-build.sh build:release 1.0.0

# Release AAB (for Google Play)
make aab
# OR
./android/gradlew bundleRelease
```

### Build Outputs

| Build Type | Output Path |
|------------|-------------|
| Debug APK | `android/app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `momclaw-VERSION.apk` |
| Release AAB | `momclaw-VERSION.aab` |
| F-Droid APK | `momclaw-VERSION-fdroid.apk` |

### Build Variants

```bash
# Debug (default)
./android/gradlew assembleDebug

# Release (requires signing config)
./android/gradlew assembleRelease

# Both APK and AAB
./android/gradlew assembleRelease bundleRelease
```

### Clean Build

```bash
make clean
./android/gradlew clean
```

---

## 🧪 Testing

### Run All Tests

```bash
make test
# OR
./scripts/ci-build.sh test:all
```

### Individual Test Commands

```bash
# Unit tests only
make test-unit
./android/gradlew testDebugUnitTest

# Instrumented tests (requires connected device/emulator)
make test-instrumented
./android/gradlew connectedAndroidTest

# Lint checks
make lint
./android/gradlew lintDebug

# Kotlin static analysis
make detekt
./android/gradlew detekt

# Test coverage report
./scripts/ci-build.sh test:coverage
./android/gradlew testDebugUnitTestCoverage
```

### Test Results

| Output | Location |
|--------|----------|
| Unit test reports | `android/app/build/reports/tests/` |
| Coverage reports | `android/app/build/reports/coverage/` |
| Lint reports | `android/app/build/reports/lint-results.html` |
| Detekt reports | `android/app/build/reports/detekt/` |

### Validation

```bash
# Full validation (lint + tests)
make validate
./scripts/ci-build.sh validate
```

---

## 🔐 Signing Configuration

### Generate Keystore

```bash
# Using make
make keystore

# Using script
./scripts/ci-build.sh keystore:generate

# Manual
keytool -genkeypair -v \
  -alias momclaw \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore momclaw-release-key.jks \
  -storepass YOUR_PASSWORD \
  -keypass YOUR_PASSWORD
```

### Create key.properties

```bash
cat > android/key.properties << EOF
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=momclaw
storeFile=../momclaw-release-key.jks
EOF

# IMPORTANT: Add to .gitignore (already done)
echo "android/key.properties" >> .gitignore
echo "momclaw-release-key.jks" >> .gitignore
```

### Configure GitHub Secrets

See [.github/SECRETS_SETUP.md](.github/SECRETS_SETUP.md) for detailed instructions.

**Required secrets:**
- `KEYSTORE_BASE64` - Base64-encoded keystore file
- `STORE_PASSWORD` - Keystore password
- `KEY_PASSWORD` - Key password
- `KEY_ALIAS` - Key alias (usually "momclaw")

**Optional secrets:**
- `GOOGLE_PLAY_SERVICE_ACCOUNT` - For Play Store deployment
- `GPG_PRIVATE_KEY` - For F-Droid signing
- `DISCORD_WEBHOOK_ID/TOKEN` - For notifications

---

## 🚀 Deployment Overview

### Deployment Paths

```
┌─────────────────────────────────────────────────────────────┐
│                    MomClaw Deployment                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ Google Play  │    │ GitHub       │    │ F-Droid      │  │
│  │ Store        │    │ Releases     │    │              │  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘  │
│         │                   │                   │           │
│         ▼                   ▼                   ▼           │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ Internal     │    │ APK + AAB    │    │ Unsigned APK │  │
│  │ Alpha        │    │ + Changelog  │    │ + GPG Sign   │  │
│  │ Beta         │    │              │    │ + SHA256     │  │
│  │ Production   │    │              │    │              │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Deployment Targets

| Target | Format | Use Case |
|--------|--------|----------|
| Google Play Internal | AAB | Internal testing |
| Google Play Alpha | AAB | Early adopters |
| Google Play Beta | AAB | Public beta |
| Google Play Production | AAB | Public release |
| GitHub Releases | APK + AAB | Direct download |
| F-Droid | Unsigned APK | Alternative store |

---

## 📱 Google Play Store Deployment

### Prerequisites

- [ ] Google Play Developer Account ($25)
- [ ] App created in Play Console
- [ ] Service Account JSON key
- [ ] Signing keystore
- [ ] Store assets (screenshots, graphics, descriptions)

### Deployment Steps

#### Option 1: Using Makefile

```bash
# Deploy to Internal Testing
make deploy-internal

# Deploy to Alpha
make deploy-alpha

# Deploy to Beta
make deploy-beta

# Deploy to Production
make deploy-production
```

#### Option 2: Using CI Script

```bash
./scripts/ci-build.sh deploy:internal
./scripts/ci-build.sh deploy:alpha
./scripts/ci-build.sh deploy:beta
./scripts/ci-build.sh deploy:production
```

#### Option 3: Using Fastlane Directly

```bash
cd android
fastlane internal     # Internal testing
fastlane alpha        # Alpha track
fastlane beta         # Beta track
fastlane production   # Production
```

#### Option 4: Using GitHub Actions

1. Go to **Actions** → **Deploy to Google Play**
2. Click **Run workflow**
3. Select track (internal/alpha/beta/production)
4. Enter version
5. Click **Run workflow**

### Promotion Between Tracks

```bash
# Promote Internal → Alpha
make fastlane-promote_internal_to_alpha

# Promote Alpha → Beta
make fastlane-promote_alpha_to_beta

# Promote Beta → Production
make fastlane-promote_beta_to_production
```

### Store Listing Updates

```bash
# Download existing metadata
cd android && fastlane download_metadata

# Edit metadata in android/fastlane/metadata/android/en-US/

# Upload updated metadata
make fastlane-update_metadata
```

---

## 🤖 F-Droid Deployment

### Prerequisites

- [ ] GPG key for signing
- [ ] GPG key published to keyserver
- [ ] F-Droid metadata YAML prepared

### Build F-Droid APK

```bash
# Using make
make build-fdroid VERSION=1.0.0

# Using script
./scripts/ci-build.sh build:fdroid 1.0.0

# Using GitHub Actions
# Go to Actions → F-Droid Build → Run workflow
```

### Output Files

| File | Purpose |
|------|---------|
| `momclaw-VERSION-fdroid.apk` | Unsigned APK |
| `momclaw-VERSION-fdroid.apk.asc` | GPG signature |
| `momclaw-VERSION-fdroid.apk.sha256` | SHA256 checksum |

### F-Droid Metadata

The F-Droid build workflow automatically generates metadata in `fdroid-metadata/com.loa.momclaw.yml`.

### Submit to F-Droid

#### Option 1: fdroiddata Repository

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata)
2. Add metadata to `metadata/com.loa.momclaw.yml`
3. Submit merge request

#### Option 2: Self-Hosted Repository

1. Set up F-Droid server
2. Add APK and metadata
3. Publish repository URL

---

## 📦 GitHub Releases

### Create Release

```bash
# Using make
make deploy-github VERSION=1.0.0

# Using script
./scripts/ci-build.sh deploy:github 1.0.0

# Using GitHub Actions (automatic on tag push)
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### Release Contents

- **APK**: `momclaw-VERSION.apk` (signed)
- **AAB**: `momclaw-VERSION.aab` (for Play Store)
- **Changelog**: From CHANGELOG.md
- **Release Notes**: Auto-generated

### Manual GitHub Release

```bash
# Build artifacts
./android/gradlew assembleRelease bundleRelease

# Create release
gh release create v1.0.0 \
  momclaw-1.0.0.apk \
  momclaw-1.0.0.aab \
  --title "MomClaw v1.0.0" \
  --notes-file CHANGELOG.md
```

---

## 🔄 CI/CD Automation

### Available Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Push/PR to main/develop | Build, test, lint |
| `android-build.yml` | Push/PR | Multi-API builds |
| `release.yml` | Tag push (v*) | Release automation |
| `google-play-deploy.yml` | Manual | Play Store deployment |
| `fdroid-build.yml` | Tag (v*-fdroid) or manual | F-Droid builds |
| `security.yml` | Weekly + PR | Security scanning |

### Workflow Commands

```bash
# Trigger CI manually (simulated)
git push origin main

# Trigger release
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Trigger F-Droid build
git tag -a v1.0.0-fdroid -m "F-Droid v1.0.0"
git push origin v1.0.0-fdroid
```

### Dependabot

Automated dependency updates are configured in `.github/dependabot.yml`:
- Gradle dependencies: Weekly
- GitHub Actions: Weekly

---

## 🔧 Troubleshooting

### Common Build Issues

#### JDK Version Mismatch

```
Error: Unsupported class file major version 61
```

**Solution**: Install JDK 17 and set JAVA_HOME
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

#### Android SDK Not Found

```
Error: SDK location not found
```

**Solution**: Set ANDROID_HOME
```bash
export ANDROID_HOME=$HOME/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > android/local.properties
```

#### Keystore Not Found

```
Error: Keystore not found
```

**Solution**: Generate keystore and create key.properties
```bash
make keystore
# Then create android/key.properties
```

#### NDK Not Found

```
Error: NDK not configured
```

**Solution**: Install NDK
```bash
sdkmanager "ndk;25.2.9519653"
```

### Deployment Issues

#### Google Play Upload Failed

```
Error: Authentication failed
```

**Solutions**:
1. Verify service account JSON is correct
2. Check service account permissions in Play Console
3. Ensure app is linked in Play Console API access

#### F-Droid GPG Signing Failed

```
Error: GPG signing failed
```

**Solutions**:
1. Verify GPG key is imported
2. Check key has signing capability
3. Ensure key is not expired

### CI/CD Issues

#### Workflow Not Triggering

**Solutions**:
1. Check workflow file is in `.github/workflows/`
2. Verify trigger conditions (branch, tag pattern)
3. Check GitHub Actions are enabled in repository settings

#### Secrets Not Working

**Solutions**:
1. Verify secret names match workflow references
2. Check secrets are set at repository level (not environment)
3. Ensure secret values are correctly formatted

---

## 📚 Quick Reference

### Essential Commands

```bash
# Build
make build                    # Debug APK
make release VERSION=1.0.0    # Release APK + AAB

# Test
make test                     # All tests
make validate                 # Full validation

# Deploy
make deploy-internal          # Google Play Internal
make deploy-github VERSION=1.0.0  # GitHub release
make build-fdroid VERSION=1.0.0  # F-Droid APK

# Utility
make clean                    # Clean build artifacts
make help                     # Show all commands
```

### File Locations

| File | Location |
|------|----------|
| Build config | `android/app/build.gradle.kts` |
| Signing config | `android/key.properties` |
| Fastlane config | `android/fastlane/Fastfile` |
| Store metadata | `android/fastlane/metadata/android/en-US/` |
| CI workflows | `.github/workflows/` |
| Build scripts | `scripts/` |

### Support

- **Documentation**: [DOCUMENTATION.md](DOCUMENTATION.md)
- **Deployment Guide**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Issues**: [GitHub Issues](https://github.com/serverul/momclaw/issues)
- **Discussions**: [GitHub Discussions](https://github.com/serverul/momclaw/discussions)

---

**Last Updated**: 2026-04-06

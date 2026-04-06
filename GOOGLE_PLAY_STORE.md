# Google Play Store Setup Guide

Complete guide for deploying MomClAW to Google Play Store.

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [Store Listing](#store-listing)
- [Content Rating](#content-rating)
- [Pricing & Distribution](#pricing--distribution)
- [Release Management](#release-management)
- [Automated Deployment](#automated-deployment)
- [Store Assets](#store-assets)
- [Compliance](#compliance)
- [Troubleshooting](#troubleshooting)

---

## Overview

MomClAW can be distributed through Google Play Store for maximum reach and automatic updates. This guide covers the complete setup and deployment process.

### Distribution Channels

| Channel | Purpose | Users |
|---------|---------|-------|
| **Internal Test** | Development testing | Up to 100 testers |
| **Closed Alpha** | Early adopters | Up to 100 testers |
| **Open Beta** | Public testing | Unlimited |
| **Production** | Public release | Unlimited |

### Requirements

- ✅ Google Play Developer Account ($25 one-time fee)
- ✅ Signed release APK/AAB
- ✅ Store listing assets (screenshots, icons, graphics)
- ✅ Privacy policy URL
- ✅ Content rating questionnaire
- ✅ Target audience declaration

---

## Prerequisites

### 1. Google Play Developer Account

```bash
# Create account
# Go to: https://play.google.com/console/signup

# Requirements:
- Google account
- $25 registration fee (one-time)
- Developer identity verification
```

### 2. Application Signing

```bash
# Generate signing key (if not done already)
./scripts/ci-build.sh keystore:generate

# Or manually
keytool -genkeypair \
  -alias MOMCLAW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore MOMCLAW-release-key.jks \
  -storetype PKCS12

# Backup keystore securely!
# Store in multiple secure locations
```

### 3. Play Console API Access

For automated deployments:

```bash
# 1. Go to Play Console
https://play.google.com/console

# 2. Navigate to Setup → API access

# 3. Create service account
- Click "Create new service account"
- Follow link to Google Cloud Console
- Create service account with these permissions:
  * "App Configuration Manager"
  * "App Content Manager"
  * "Release Manager"
  * "Testing Manager"

# 4. Create and download JSON key
- Service account → Keys → Add Key → Create new key → JSON
- Save as: play-store-service-account.json
- Add to GitHub secrets: PLAY_STORE_SERVICE_ACCOUNT
```

### 4. Fastlane Setup

MomClAW uses Fastlane for automated deployments:

```bash
# Install Fastlane (if not installed)
gem install fastlane

# Navigate to android directory
cd android

# Fastlane is already configured
# See: android/fastlane/Fastfile
```

---

## Initial Setup

### Create Application

1. **Open Play Console**
   ```
   https://play.google.com/console
   ```

2. **Create App**
   - Click "Create app"
   - App name: `MomClAW`
   - Default language: `English (United States)`
   - Free or paid: `Free`
   - Check all declarations

3. **App Settings**
   - Access: `All users`
   - Age group: `Your target audience`

### Dashboard Setup

Play Console shows a checklist. Complete each section:

1. ✅ **Main Store Listing** - Title, description, screenshots
2. ✅ **Content rating questionnaire** - Complete IARC form
3. ✅ **Privacy policy** - Add URL
4. ✅ **Target audience** - Select age groups
5. ✅ **News apps** - Not applicable (skip)
6. ✅ **App access** - Declare permissions
7. ✅ **Ads** - No ads declaration
8. ✅ **App content** - Privacy policy, app access, etc.

---

## Store Listing

### Main Store Listing

Navigate to **Main Store Listing** and fill in:

#### App Details

| Field | Value |
|-------|-------|
| **App name** | `MomClAW` |
| **Short description** | `AI Agent 100% offline - Privacy-first mobile AI` |
| **Full description** | See below |

#### Full Description

```
MomClAW - Mobile Offline Model Agent

🧠 Your Private AI Assistant

MomClAW is a fully autonomous AI agent that runs entirely on your phone - zero cloud, zero tracking, 100% offline.

✨ KEY FEATURES

🤖 Conversational AI
- Advanced reasoning with Gemma 3 E4B-it
- Natural language understanding
- Context-aware responses
- Multi-turn conversations

🔧 Tool Integration
- Shell commands
- File operations
- Web search capabilities
- Custom tool support

💾 Persistent Memory
- Complete conversation history
- SQLite database storage
- Searchable chat logs
- Export capabilities

📱 External Channels
- Telegram integration (online mode)
- Discord integration (online mode)
- OpenClaw synchronization
- API access

🔒 Privacy First
- All data stays on device
- No cloud processing
- No tracking or telemetry
- Open source

⚡ Optimized Performance
- LiteRT-LM inference engine
- Mobile-optimized quantization
- Efficient memory usage
- Battery-friendly

🎨 Modern UI
- Material You design
- Dark theme
- Dynamic colors
- Responsive layout

📊 USE CASES

- Personal AI assistant
- Code assistance and review
- Research and analysis
- Document summarization
- Creative writing
- Language translation
- Learning and tutoring

🛠️ SYSTEM REQUIREMENTS

- Android 9.0 (API 28) or higher
- 4GB+ RAM recommended
- 3GB+ free storage for model
- Initial model download (~2.5GB)

📦 MODEL

MomClAW uses Gemma 3 E4B-it, a state-of-the-art open model optimized for mobile devices. The model is downloaded on first use and stored locally on your device.

🤝 OPEN SOURCE

MomClAW is open source software. Contribute at:
https://github.com/serverul/MOMCLAW

📄 LICENSE

Apache License 2.0

Built with ❤️ by LinuxOnAsteroids
Powered by NullClaw + llama.cpp + LiteRT-LM
```

#### Screenshots

Upload at least 2 screenshots per device type:

**Phone (required)**
- Minimum: 2 screenshots
- Recommended: 4-8 screenshots
- Format: PNG or JPEG
- Aspect ratio: 16:9 or 9:16
- Size: 320-3840px
- Max: 8MB each

**Tablet (recommended)**
- 7-inch tablet: 2+ screenshots
- 10-inch tablet: 2+ screenshots

**Screenshot Guidelines**
```bash
# Generate screenshots
# Use Android Studio Layout Validation or real devices

# Recommended screenshots:
1. Main chat interface
2. Model management screen
3. Settings screen
4. Tool execution demo
5. Memory/conversation history
6. Channel integration
7. Dark mode showcase

# Add text overlays for clarity
# Use consistent branding
```

#### Graphics

| Graphic | Size | Format | Purpose |
|---------|------|--------|---------|
| **App icon** | 512x512 | PNG | Play Store listing |
| **Feature graphic** | 1024x500 | PNG | Top of store listing |
| **Video** | YouTube URL | - | Optional showcase |

**Feature Graphic**

Create a compelling banner:

```bash
# Requirements
- Size: 1024x500 pixels
- Format: PNG or JPEG
- Max: 8MB

# Design tips:
- Include app name prominently
- Show key features
- Use consistent branding
- Works on light and dark backgrounds
- Avoid text that's too small
```

### Short Description

```
🤖 100% offline AI assistant. Advanced reasoning, tool integration, persistent memory. No cloud, no tracking - your data stays on device.
```

### Categories

| Field | Selection |
|-------|-----------|
| **App category** | Productivity |
| **Secondary category** | Tools |
| **Tags** | AI, Assistant, Privacy, Offline, Open Source |

### Contact Details

| Field | Value |
|-------|-------|
| **Email** | `support@momclaw.app` |
| **Website** | `https://github.com/serverul/MOMCLAW` |
| **Phone** | (optional) |

---

## Content Rating

### IARC Questionnaire

Complete the International Age Rating Coalition (IARC) questionnaire:

1. Navigate to **Content rating questionnaire**
2. Fill in the form:

```
General Questions:
- Violence: None
- Sexual content: None
- Language: None
- Controlled substances: None
- Gambling: None
- User interaction: None
- Data sharing: None
- Miscellaneous: None

Result: ESRB E (Everyone) / PEGI 3
```

3. **Generate certificate**
4. **Save and continue**

---

## Pricing & Distribution

### Pricing

| Field | Value |
|-------|-------|
| **Price** | Free |
| **Countries** | All countries (or select specific) |
| **Primary distribution** | Google Play Store |

### Distribution Options

- ✅ **Google Play** - Primary distribution
- ✅ **Android TV** - Not applicable (uncheck)
- ✅ **Android Auto** - Not applicable (uncheck)
- ✅ **Wear OS** - Not applicable (uncheck)
- ✅ **Chrome OS** - Yes (compatible)

### Target Audience

**Age Groups**
- ✅ Under 13 - Not designed for children
- ✅ 13-15 - Not designed for children
- ✅ 16-17 - Not designed for children
- ✅ 18+ - Yes (primary target)

**App Content**
- Privacy policy URL: `https://github.com/serverul/MOMCLAW/blob/main/PRIVACY_POLICY.md`
- App access: Declare all permissions
- Ads: No ads

---

## Release Management

### Track Structure

```
┌─────────────────┐
│  Internal Test  │ ← Development builds
│   (100 users)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Closed Alpha   │ ← Early testers
│   (100 users)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Open Beta     │ ← Public testing
│   (unlimited)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Production     │ ← Public release
│   (unlimited)   │
└─────────────────┘
```

### Create Release

#### Internal Test (First Release)

1. **Navigate**: Release → Testing → Internal testing
2. **Create release**:
   ```bash
   # Build release AAB
   ./scripts/ci-build.sh build:release 1.0.0
   
   # Upload AAB
   android/app/build/outputs/bundle/release/app-release.aab
   ```
3. **Release notes**:
   ```
   Version 1.0.0 - Initial Release
   
   🎉 First public release of MomClAW!
   
   Features:
   - AI-powered conversations with Gemma 3 E4B-it
   - Tool integration (shell, files, web search)
   - Persistent memory with SQLite
   - Material You design with dark theme
   - 100% offline, privacy-first
   
   Known Issues:
   - Model download is large (~2.5GB)
   - Initial load time can be 30-60 seconds
   - Memory usage varies with conversation length
   ```

4. **Review and rollout**: 100% to internal testers

#### Alpha/Beta Releases

1. **Promote from internal** or **create new release**
2. **Add testers**:
   - Create email list
   - Add tester emails
   - Send opt-in link: `https://play.google.com/apps/testing/com.loa.MOMCLAW`
3. **Rollout percentage**: Start with 1%, increase gradually

#### Production Release

1. **Promote from beta** or **create production release**
2. **Staged rollout**:
   - Start: 1% (monitor for 24-48h)
   - Increase: 5% → 10% → 25% → 50% → 100%
   - Monitor crash reports, ANRs, reviews

3. **Production checklist**:
   - [ ] All tests passing
   - [ ] No critical bugs
   - [ ] Beta testing complete
   - [ ] Store listing updated
   - [ ] Screenshots current
   - [ ] Release notes clear
   - [ ] Crash reporting enabled
   - [ ] Analytics configured

---

## Automated Deployment

### GitHub Actions Setup

MomClAW includes automated deployment to Google Play:

#### 1. Configure Secrets

Add these secrets to GitHub repository:

```bash
# Go to: Settings → Secrets → Actions

# Required secrets:
PLAY_STORE_SERVICE_ACCOUNT  # Service account JSON
KEYSTORE_BASE64            # Base64 encoded keystore
STORE_PASSWORD             # Keystore password
KEY_PASSWORD               # Key password
KEY_ALIAS                  # Key alias (MOMCLAW)
```

See [`.github/SECRETS_SETUP.md`](.github/SECRETS_SETUP.md) for detailed setup.

#### 2. Deploy via GitHub Actions

```bash
# Manual deployment
# Go to: Actions → google-play-deploy → Run workflow

# Select track:
- internal    # Internal testing
- alpha       # Closed alpha
- beta        # Open beta
- production  # Production release
```

#### 3. Automated Deployment

Deployment triggers automatically on tag push:

```bash
# Create and push tag
git tag v1.0.0
git push --tags

# This triggers:
# 1. Build release AAB
# 2. Sign with release key
# 3. Upload to Google Play (internal track)
# 4. Create GitHub release
```

### Fastlane Deployment

Manual deployment using Fastlane:

```bash
cd android

# Deploy to internal
fastlane deploy_internal

# Deploy to alpha
fastlane deploy_alpha

# Deploy to beta
fastlane deploy_beta

# Deploy to production
fastlane deploy_production

# Promote between tracks
fastlane promote_alpha_to_beta
fastlane promote_beta_to_production
```

---

## Store Assets

### Required Assets Checklist

- [ ] **App icon** (512x512 PNG)
- [ ] **Feature graphic** (1024x500 PNG)
- [ ] **Phone screenshots** (2-8 screenshots)
- [ ] **7" tablet screenshots** (optional but recommended)
- [ ] **10" tablet screenshots** (optional but recommended)
- [ ] **Promo video** (YouTube URL, optional)

### Asset Generation

```bash
# Generate app icons
./scripts/generate-icons.sh generate-all assets/logo-base.png

# This generates:
# - android/app/src/main/res/mipmap-*/ic_launcher*.png
# - assets/icon.png (512x512 for Play Store)
```

### Screenshot Guidelines

**Phone Screenshots (16:9)**
- Resolution: 1080x1920 or 1440x2560
- Show app in action
- Use high-quality devices
- Avoid generic stock photos
- Show dark and light themes

**Tablet Screenshots (optional)**
- 7": 1024x768 or higher
- 10": 1280x800 or higher

**Feature Graphic**
- Size: 1024x500
- Use simple, clear design
- Include app name
- Show key benefit

### Asset Locations

```
assets/
├── icon.png                  # 512x512 app icon
├── feature-graphic.png       # 1024x500 banner
└── screenshots/
    ├── phone/
    │   ├── 1-chat.png
    │   ├── 2-models.png
    │   ├── 3-settings.png
    │   └── ...
    ├── tablet-7/
    │   └── ...
    └── tablet-10/
        └── ...
```

---

## Compliance

### Data Safety Section

Complete the Data Safety form in Play Console:

**Data Collection**
- ✅ No data collected
- ✅ No data shared
- ✅ No data sold

**Security Practices**
- Data encrypted in transit: Yes
- Data encrypted at rest: Yes
- Data deletion request: Yes (user can clear app data)

### Privacy Policy

**Required**: Host a privacy policy and add URL to store listing.

```bash
# Privacy policy is included in repository
# Host at: https://github.com/serverul/MOMCLAW/blob/main/PRIVACY_POLICY.md

# Or host on dedicated page:
# - GitHub Pages
# - Website
# - Dedicated privacy policy service
```

### App Content Declarations

**Permissions**
```xml
<!-- Declare in AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

**Justification in Play Console**
- INTERNET: Optional external channels (Telegram/Discord)
- FOREGROUND_SERVICE: Background AI processing
- STORAGE: Model and conversation storage

### Export Compliance

**Target API Level**
- Current: API 35 (Android 15)
- Required: API 34+ for new apps (as of Aug 2024)

**Permissions Review**
- All permissions justified
- No unnecessary permissions
- Runtime permissions implemented

---

## Troubleshooting

### Common Issues

#### Upload Failed

```bash
# Error: "Your APK is not signed"
Solution: Sign APK with release keystore
./scripts/ci-build.sh build:release 1.0.0

# Error: "Version code already exists"
Solution: Increment version code
./scripts/version-manager.sh bump patch

# Error: "APK too large"
Solution: Use APK splits or reduce resources
# APK splits are already configured
```

#### Review Rejected

```
Common reasons:
1. Missing privacy policy → Add URL to store listing
2. Permissions not justified → Add permission justification
3. Metadata policy violation → Complete data safety form
4. Content rating mismatch → Review IARC questionnaire
```

#### Deployment Failed

```bash
# Check service account permissions
# Required: Release Manager, App Configuration Manager

# Verify credentials
echo $PLAY_STORE_SERVICE_ACCOUNT | jq .

# Test with Fastlane locally
cd android
fastlane deploy_internal
```

#### Stuck in Review

```
Normal review time: 1-7 days
If longer: Contact Google Play support

Speed up review:
- Complete all store listing sections
- Provide clear privacy policy
- Respond to any reviewer questions promptly
```

### Support Resources

- **Play Console Help**: https://support.google.com/googleplay/android-developer
- **Policy Center**: https://play.google.com/about/developer-content-policy/
- **Contact Support**: Play Console → Help → Contact support

---

## Quick Reference

### Play Console URLs

```
Dashboard:      https://play.google.com/console
Store Listing:  https://play.google.com/console/app/store-listing
Releases:       https://play.google.com/console/app/tracks
Statistics:     https://play.google.com/console/app/statistics
Reviews:        https://play.google.com/console/app/reviews
Crashlytics:    https://play.google.com/console/app/crashlytics
```

### Deployment Commands

```bash
# Build release
./scripts/ci-build.sh build:release 1.0.0

# Deploy to tracks
./scripts/ci-build.sh deploy:internal     # Internal testing
./scripts/ci-build.sh deploy:alpha        # Alpha
./scripts/ci-build.sh deploy:beta         # Beta
./scripts/ci-build.sh deploy:production   # Production

# Using Fastlane
cd android
fastlane deploy_internal
fastlane promote_alpha_to_beta
```

### Version Management

```bash
# Update version
./scripts/version-manager.sh bump minor   # 1.0.0 → 1.1.0
./scripts/version-manager.sh release 1.1.0

# Check current version
./scripts/version-manager.sh current
```

---

## Additional Resources

- [DEPLOYMENT.md](DEPLOYMENT.md) - Complete deployment guide
- [BUILD-DEPLOYMENT-GUIDE.md](BUILD-DEPLOYMENT-GUIDE.md) - All-in-one reference
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) - Pre-release checklist
- [PRIVACY_POLICY.md](PRIVACY_POLICY.md) - Privacy policy
- [`.github/SECRETS_SETUP.md`](.github/SECRETS_SETUP.md) - CI/CD secrets setup

---

**This document is maintained alongside the codebase. Last updated: 2026-04-06**

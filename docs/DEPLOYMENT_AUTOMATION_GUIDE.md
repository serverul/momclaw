# MOMCLAW Deployment Automation Guide

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## Overview

This guide covers automated deployment of MOMCLAW to multiple distribution channels:
- **GitHub Releases** - APK + AAB for direct download
- **Google Play Store** - Internal, Alpha, Beta, and Production tracks
- **F-Droid** - Open-source Android repository

All deployment is handled through the unified `scripts/deploy.sh` script.

---

## Prerequisites

### Required Tools

| Tool | Version | Purpose | Install Command |
|------|---------|---------|-----------------|
| JDK | 17+ | Build Android app | `sudo apt install openjdk-17-jdk` |
| GitHub CLI | 2.0+ | GitHub releases | `brew install gh` or `sudo apt install gh` |
| Fastlane | 2.200+ | Play Store deploy | `gem install fastlane` |
| GPG | 2.2+ | F-Droid signing | `sudo apt install gnupg` |
| Ruby | 2.6+ | Fastlane runtime | `sudo apt install ruby` |

### Authentication

#### GitHub CLI
```bash
# Install GitHub CLI
gh auth login

# Verify authentication
gh auth status
```

#### Google Play Console
1. Create Google Play Developer Account ($25 fee)
2. Create service account in Google Cloud Console
3. Download JSON key file
4. Grant service account permissions in Play Console
5. Save JSON as `android/google-play-service-account.json`

#### GPG (for F-Droid)
```bash
# Generate GPG key
gpg --full-generate-key

# Publish to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

---

## Deployment Script Usage

### Basic Usage

```bash
# Deploy to GitHub only
./scripts/deploy.sh 1.0.0 release github

# Deploy to Google Play
./scripts/deploy.sh 1.1.0 release play

# Deploy to F-Droid
./scripts/deploy.sh 1.0.0 release fdroid

# Deploy to all targets
./scripts/deploy.sh 2.0.0 release all
```

### Version Naming Convention

| Version Format | Release Type | Play Store Track |
|---------------|--------------|------------------|
| `1.0.0` | Production | Internal (promote manually) |
| `1.1.0-alpha` | Alpha | Alpha |
| `1.2.0-beta` | Beta | Beta |
| `2.0.0-rc1` | Release Candidate | Internal |
| `2.0.0` | Production | Internal (promote manually) |

### Build Types

- **release**: Optimized, signed, minified (for production)
- **debug**: Development builds (not for distribution)

---

## Deployment Targets

### 1. GitHub Releases

#### What Gets Deployed
- Signed APK file
- Signed AAB file (Android App Bundle)
- Release notes with changelog
- Pre-release flag (if version contains alpha/beta/rc)

#### Prerequisites
- GitHub CLI authenticated
- GitHub repository secrets configured

#### Required Secrets
```bash
# Set these in GitHub repository settings
KEYSTORE_BASE64       # Base64-encoded keystore file
STORE_PASSWORD        # Keystore password
KEY_PASSWORD          # Key password
KEY_ALIAS             # Key alias (e.g., "momclaw")
```

#### Manual Deployment
```bash
# Using deploy script
./scripts/deploy.sh 1.0.0 release github

# Or using GitHub CLI directly
gh release create v1.0.0 \
  momclaw-1.0.0.apk \
  momclaw-1.0.0.aab \
  --title "MOMCLAW v1.0.0" \
  --notes-file changelog.md
```

#### Automated via GitHub Actions
1. Create and push a tag:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

2. GitHub Actions workflow `.github/workflows/release.yml` will:
   - Build signed APK + AAB
   - Create GitHub release
   - Upload artifacts

---

### 2. Google Play Store

#### Deployment Tracks

| Track | Purpose | Version Pattern |
|-------|---------|-----------------|
| Internal | Internal team testing | All versions |
| Alpha | Limited external testing | `*-alpha` |
| Beta | Open beta testing | `*-beta` |
| Production | Public release | Production versions |

#### Prerequisites
- Google Play Developer Account
- Service account JSON key
- Signing keystore configured
- Fastlane installed

#### Configuration

1. Create `android/key.properties`:
   ```properties
   storePassword=YOUR_STORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=momclaw
   storeFile=../momclaw-release-key.jks
   ```

2. Place service account JSON:
   ```bash
   cp ~/Downloads/service-account.json android/google-play-service-account.json
   ```

#### Deployment Commands

```bash
# Deploy to specific track
./scripts/deploy.sh 1.0.0 release play  # Auto-selects track based on version

# Or using Fastlane directly
cd android
fastlane internal version:"1.0.0"
fastlane alpha version:"1.1.0-alpha"
fastlane beta version:"1.2.0-beta"
fastlane production version:"2.0.0"
```

#### Track Promotion

```bash
cd android

# Promote Internal → Alpha
fastlane promote_internal_to_alpha

# Promote Alpha → Beta
fastlane promote_alpha_to_beta

# Promote Beta → Production
fastlane promote_beta_to_production
```

#### Automated via GitHub Actions
1. Go to Actions → google-play-deploy.yml
2. Click "Run workflow"
3. Select version and track
4. Click "Run workflow"

---

### 3. F-Droid

#### What Gets Built
- Unsigned APK (F-Droid compatible)
- GPG signature file (.asc)
- SHA256 checksum file

#### Prerequisites
- GPG key generated and published
- F-Droid metadata prepared

#### Build Command

```bash
# Using deploy script
./scripts/deploy.sh 1.0.0 release fdroid

# Or using build script directly
./scripts/build-fdroid.sh 1.0.0
```

#### Output Files
- `momclaw-1.0.0-fdroid.apk` - Unsigned APK
- `momclaw-1.0.0-fdroid.apk.asc` - GPG signature
- `momclaw-1.0.0-fdroid.apk.sha256` - SHA256 checksum

#### F-Droid Submission

**Option 1: fdroiddata repository (recommended)**
1. Fork https://gitlab.com/fdroid/fdroiddata
2. Add metadata YAML file
3. Submit merge request

**Option 2: Self-hosted repository**
1. Create F-Droid repository
2. Add APK to repository
3. Publish repository URL

---

## Signing Configuration

### Generate Release Keystore

```bash
# Generate keystore (DO THIS ONCE)
keytool -genkey -v \
  -keystore momclaw-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias momclaw

# Backup keystore (CRITICAL!)
cp momclaw-release-key.jks ~/backup/
# Store backup in multiple secure locations!

# Convert to Base64 for GitHub Secrets
base64 -w 0 momclaw-release-key.jks > keystore-base64.txt
```

### Configure GitHub Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Add repository secrets:

   | Secret Name | Value | Source |
   |-------------|-------|--------|
   | `KEYSTORE_BASE64` | Content of `keystore-base64.txt` | `base64 momclaw-release-key.jks` |
   | `STORE_PASSWORD` | Keystore password | From keytool command |
   | `KEY_PASSWORD` | Key password | From keytool command |
   | `KEY_ALIAS` | Key alias | `momclaw` |

3. (Optional) Add Play Store secret:
   - `GOOGLE_PLAY_SERVICE_ACCOUNT` - Paste JSON key content

4. (Optional) Add Discord webhook for notifications:
   - `DISCORD_WEBHOOK_ID` - Webhook ID
   - `DISCORD_WEBHOOK_TOKEN` - Webhook token

### Local Signing Configuration

Create `android/key.properties` (gitignored):

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=momclaw
storeFile=../momclaw-release-key.jks
```

**⚠️ WARNING**: Never commit `key.properties` or keystore files to Git!

---

## Workflow Examples

### Complete Release Workflow

```bash
# 1. Update version in build.gradle.kts
# versionCode = 2
# versionName = "1.1.0"

# 2. Update CHANGELOG.md
# ## [1.1.0] - 2026-04-10
# ### Added
# - New feature X
# - Improved performance

# 3. Create git commit
git add .
git commit -m "chore: release v1.1.0"
git push

# 4. Create and push tag
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0

# 5. GitHub Actions will automatically:
# - Build signed APK + AAB
# - Create GitHub release
# - Upload artifacts

# 6. Deploy to Google Play (optional)
./scripts/deploy.sh 1.1.0 release play

# 7. Deploy to F-Droid (optional)
./scripts/deploy.sh 1.1.0 release fdroid
```

### Alpha Release Workflow

```bash
# 1. Update version
# versionName = "1.2.0-alpha"

# 2. Commit and tag
git commit -am "chore: release v1.2.0-alpha"
git tag v1.2.0-alpha
git push origin main --tags

# 3. Deploy to Play Store Alpha track
./scripts/deploy.sh 1.2.0-alpha release play

# This will:
# - Build signed artifacts
# - Create GitHub pre-release
# - Upload to Play Store Alpha track
```

### Emergency Hotfix Workflow

```bash
# 1. Create hotfix branch from production
git checkout -b hotfix/1.0.1 v1.0.0

# 2. Apply fix and update version
# versionCode = 3
# versionName = "1.0.1"

# 3. Commit and tag
git commit -am "fix: critical bug"
git tag v1.0.1

# 4. Build and deploy
./scripts/deploy.sh 1.0.1 release play

# 5. Promote immediately to production
cd android
fastlane promote_internal_to_alpha
fastlane promote_alpha_to_beta
fastlane promote_beta_to_production

# 6. Merge back to main
git checkout main
git merge hotfix/1.0.1
git push
```

---

## Troubleshooting

### GitHub Release Fails

**Error**: "gh: command not found"
```bash
# Install GitHub CLI
brew install gh  # macOS
sudo apt install gh  # Ubuntu

# Authenticate
gh auth login
```

**Error**: "resource not accessible by integration"
```bash
# Check repository permissions
# Go to Settings → Actions → General → Workflow permissions
# Select "Read and write permissions"
```

### Play Store Deployment Fails

**Error**: "Could not find google-play-service-account.json"
```bash
# Download service account JSON from Google Cloud Console
# Place in android/ directory
cp ~/Downloads/service-account.json android/google-play-service-account.json
```

**Error**: "apk is not signed"
```bash
# Verify key.properties exists
cat android/key.properties

# Verify keystore exists
ls -la momclaw-release-key.jks

# Test local build
cd android
./gradlew assembleRelease
jarsigner -verify app/build/outputs/apk/release/app-release.apk
```

### F-Droid Build Fails

**Error**: "gpg: no default secret key"
```bash
# Generate GPG key if not exists
gpg --full-generate-key

# List keys
gpg --list-secret-keys

# Set default key in ~/.gnupg/gpg.conf
echo "default-key YOUR_KEY_ID" >> ~/.gnupg/gpg.conf
```

---

## Monitoring & Post-Deployment

### Monitor Release

**GitHub Releases**
- Check release page: https://github.com/serverul/momclaw/releases
- Verify download counts
- Check for user feedback

**Google Play Console**
- Monitor crash reports
- Check ANR rates
- Review user ratings and feedback
- Track install metrics

**F-Droid**
- Check repository build status
- Monitor user reviews on f-droid.org

### Rollback Plan

**Google Play Store**
```bash
# Halt rollout in Play Console
# Or promote previous version
cd android
fastlane promote_beta_to_production version:"1.0.0"
```

**GitHub**
```bash
# Delete release
gh release delete v1.1.0

# Re-release previous version
gh release create v1.0.0 momclaw-1.0.0.apk --title "MOMCLAW v1.0.0"
```

---

## Best Practices

### Version Management
- ✅ Always increment `versionCode` for every release
- ✅ Follow semantic versioning (MAJOR.MINOR.PATCH)
- ✅ Use suffixes for pre-releases (-alpha, -beta, -rc)
- ✅ Update CHANGELOG.md for every release

### Security
- ✅ Never commit secrets to repository
- ✅ Use GitHub Secrets for CI/CD
- ✅ Backup keystore in multiple secure locations
- ✅ Rotate service account keys annually
- ✅ Enable 2FA on all accounts

### Quality
- ✅ Run full test suite before release
- ✅ Test on multiple devices (API 28-35)
- ✅ Verify ProGuard rules don't break functionality
- ✅ Check release APK size and performance
- ✅ Review permissions in final APK

### Communication
- ✅ Write clear, user-friendly release notes
- ✅ Announce releases on Discord/GitHub Discussions
- ✅ Document breaking changes prominently
- ✅ Provide upgrade guides for major versions

---

## Quick Reference

### Deployment Commands
```bash
# GitHub release
./scripts/deploy.sh 1.0.0 release github

# Play Store release
./scripts/deploy.sh 1.1.0 release play

# F-Droid release
./scripts/deploy.sh 1.0.0 release fdroid

# All targets
./scripts/deploy.sh 2.0.0 release all
```

### Fastlane Lanes
```bash
cd android
fastlane internal            # Deploy to Internal
fastlane alpha               # Deploy to Alpha
fastlane beta                # Deploy to Beta
fastlane production          # Deploy to Production
fastlane promote_internal_to_alpha
fastlane promote_alpha_to_beta
fastlane promote_beta_to_production
```

### Useful Commands
```bash
# Check prerequisites
./scripts/deploy.sh --help

# Validate build
./scripts/validate-build.sh

# Run tests
./scripts/run-tests.sh

# Build locally
cd android
./gradlew assembleRelease
./gradlew bundleRelease
```

---

## Support

- **Documentation**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Build Guide**: [BUILD.md](BUILD.md)
- **Issues**: [GitHub Issues](https://github.com/serverul/momclaw/issues)
- **Discussions**: [GitHub Discussions](https://github.com/serverul/momclaw/discussions)

---

**Last Updated**: 2026-04-06  
**Maintainer**: Release Engineering Team

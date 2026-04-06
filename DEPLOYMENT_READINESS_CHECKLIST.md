# MOMCLAW Deployment Readiness Checklist

## ✅ Completed Fixes

### Build Configuration
- [x] Fixed compileSdk/minSdk mismatch (agent: 34/26 → 35/28 to match app/bridge)
- [x] Removed circular dependency (agent no longer depends on app module)
- [x] Verified Gradle wrapper version (8.9)
- [x] Confirmed JDK 17 compatibility

### Documentation
- [x] README.md - Complete with features, tech stack, quick start
- [x] BUILD.md - Detailed build instructions
- [x] DEVELOPMENT.md - Architecture, project structure, development guide
- [x] DEPLOYMENT.md - Google Play Store and F-Droid deployment guides
- [x] DOCUMENTATION.md - Comprehensive technical documentation
- [x] QUICKSTART.md - 5-minute setup guide
- [x] PRODUCTION-CHECKLIST.md - Single-source production release checklist
- [x] RELEASE_CHECKLIST.md - Pre-release validation checklist
- [x] DOCUMENTATION-INDEX.md - Complete documentation index

### CI/CD Workflows
- [x] ci.yml - Basic CI for PRs
- [x] android-build.yml - Full pipeline with matrix testing
- [x] release.yml - Automated GitHub releases
- [x] google-play-deploy.yml - Manual Play Store deployment
- [x] fdroid-build.yml - F-Droid build workflow
- [x] security.yml - Weekly security scanning

### Scripts & Automation
- [x] ci-build.sh - Main automation script
- [x] build-release.sh - Release build helper
- [x] build-fdroid.sh - F-Droid specific build
- [x] run-tests.sh - Test runner
- [x] validate-build.sh - Build validation
- [x] Makefile - Convenience commands
- [x] download-model.sh - Model acquisition

## ⚠️ Pre-Release Requirements

### 1. Screenshots (Required for Store Listings)
- [ ] Add actual screenshots to:
  - `fastlane/metadata/android/en-US/images/phoneScreenshots/`
  - Minimum 2 screenshots showing core functionality
  - Recommended: chat, models, settings, dark theme

### 2. Signing Configuration
- [ ] Generate release keystore:
  ```bash
  keytool -genkey -v \
    -keystore MOMCLAW-release-key.jks \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -alias MOMCLAW
  ```
- [ ] Backup keystore securely (DO NOT commit to repo)
- [ ] Create `android/key.properties` (gitignored):
  ```properties
  storePassword=YOUR_STORE_PASSWORD
  keyPassword=YOUR_KEY_PASSWORD
  keyAlias=MOMCLAW
  storeFile=../MOMCLAW-release-key.jks
  ```

### 3. Agent Binary
- [ ] Obtain NullClaw agent binary for:
  - `android/app/src/main/assets/nullclaw` (ARM64)
  - Optional: additional architectures in assets/
- [ ] Ensure binary is executable

### 4. Model Files
- [ ] Download Gemma 3 E4B-it model (~2.5GB):
  ```bash
  ./scripts/download-model.sh ./models
  ```
- [ ] Verify model integrity and size

## 🚀 Deployment Verification

### Local Build Tests
- [ ] Debug build: `./android/gradlew assembleDebug`
- [ ] Release build: `./android/gradlew assembleRelease`
- [ ] AAB build: `./android/gradlew bundleRelease`
- [ ] Clean build: `./android/gradlew clean`

### Test Suite
- [ ] Unit tests: `./android/gradlew testDebugUnitTest`
- [ ] Lint: `./android/gradlew lintDebug`
- [ ] Detekt: `./android/gradlew detekt`

### Device Testing (Recommended)
- [ ] Install debug APK on test device
- [ ] Verify core functionality:
  - Chat interface loads
  - Settings accessible
  - Model management works
  - Navigation functions

### CI/CD Validation
- [ ] Push test branch to trigger CI workflows
- [ ] Verify all workflows pass (build, test, lint)
- [ ] Check artifact uploads function correctly

## 📋 Release Process

### Version Updates
- [ ] Increment versionCode in `android/app/build.gradle.kts`
- [ ] Update versionName in `android/app/build.gradle.kts`
- [ ] Update CHANGELOG.md with release notes
- [ ] Update Fastlane changelog: `fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt`

### Deployment Options

#### Google Play Store (Recommended)
1. Build AAB: `./android/gradlew bundleRelease`
2. Upload via:
   - Google Play Console (web UI)
   - Fastlane: `fastlane internal` (or alpha/beta/production)
   - GitHub Actions: workflow_dispatch on google-play-deploy.yml

#### GitHub Releases
1. Create tag: `git tag -a v1.0.0 -m "Release v1.0.0"`
2. Push tag: `git push origin v1.0.0`
3. GitHub Actions (release.yml) will:
   - Build signed AAB/APK
   - Create GitHub release
   - Upload artifacts
   - Generate release notes

#### F-Droid (Optional)
1. Build unsigned APK: `./scripts/build-fdroid.sh 1.0.0`
2. Sign with GPG
3. Submit to fdroiddata repository or self-hosted

### Post-Release Tasks
- [ ] Monitor crash reports (Play Console)
- [ ] Announce release on GitHub Discussions
- [ ] Update website/documentation if needed
- [ ] Close resolved GitHub issues
- [ ] Track release metrics

## 🔐 Security Verification

- [ ] No secrets in code (verified via Detekt/secrets scanning)
- [ ] ProGuard rules tested with release build
- [ ] No debug flags in release build
- [ ] Dependencies checked for vulnerabilities
- [ ] key.properties not in git
- [ ] Keystore backed up securely

## 📊 Metrics to Track Post-Release

| Metric | Target | Tracking Method |
|--------|--------|-----------------|
| Crash-free rate | >99.5% | Play Console |
| ANR rate | <0.5% | Play Console |
| Store rating | >4.5 | Play Console |
| Uninstall rate (7-day) | <5% | Play Console |
| MAU growth | +5%/month | Analytics |
| GitHub stars | Track | GitHub |
| Download count | Track | Distribution channels |

---

**Last Updated**: 2026-04-06  
**Valid Until**: Next release cycle  
**Maintainer**: Release Engineering Team  

> **Note**: This checklist should be reviewed and updated before each release to reflect process improvements and lessons learned.
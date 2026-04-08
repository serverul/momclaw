# MOMCLAW Production Checklist

**Single-source checklist for production releases**

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## 📋 Overview

This checklist consolidates all pre-release, deployment, and post-release tasks. Use this for every production release.

---

## ✅ Pre-Release Checklist

### 1. Code Quality

```bash
# Run all quality checks
make validate
```

- [ ] All unit tests pass: `./android/gradlew testDebugUnitTest`
- [ ] Lint passes with no errors: `./android/gradlew lintDebug`
- [ ] Detekt passes: `./android/gradlew detekt`
- [ ] No compiler warnings
- [ ] Code coverage acceptable (target: 60%+)
- [x] All TODOs resolved or documented

### 2. Version Updates

- [ ] `versionCode` incremented in `android/app/build.gradle.kts`
- [ ] `versionName` updated in `android/app/build.gradle.kts`
- [ ] `CHANGELOG.md` updated with release notes
- [ ] Fastlane changelog created: `android/fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt`
- [ ] `DOCUMENTATION-INDEX.md` last updated date is current

### 3. Documentation Review

- [ ] `README.md` - Check badges, links, version references
- [ ] `CHANGELOG.md` - All changes documented
- [ ] `DOCUMENTATION.md` - API docs current
- [ ] `DEPLOYMENT.md` - Instructions accurate
- [ ] `BUILD-DEPLOYMENT-GUIDE.md` - Steps accurate
- [ ] `QUICKSTART.md` - Quick start still works

### 4. Assets

- [ ] Screenshots current and representative (phone, 7", 10")
- [ ] Feature graphic (1024x500) up to date
- [ ] App icon (512x512) matches current version
- [ ] Store metadata reviewed:
  - [ ] Title
  - [ ] Short description
  - [ ] Full description
  - [ ] Privacy policy URL

### 5. Security

- [ ] No secrets in code (use `./android/gradlew detekt` to check)
- [ ] ProGuard rules tested with release build
- [ ] No debug flags in release build
- [ ] All dependencies have no known vulnerabilities
- [ ] `key.properties` not in git
- [ ] Keystore backed up securely

---

## 🔐 Signing & Secrets Checklist

### Keystore

- [ ] Keystore exists: `MOMCLAW-release-key.jks`
- [ ] Keystore backed up securely (offline, encrypted)
- [ ] Keystore password accessible to team
- [ ] `key.properties` created in `android/` directory

### GitHub Secrets

- [ ] `KEYSTORE_BASE64` - Base64-encoded keystore
- [ ] `STORE_PASSWORD` - Keystore password
- [ ] `KEY_PASSWORD` - Key password
- [ ] `KEY_ALIAS` - Key alias (e.g., "MOMCLAW")

### Optional Secrets (for Play Store)

- [ ] `GOOGLE_PLAY_SERVICE_ACCOUNT` - JSON key for Play Console API

### Optional Secrets (for F-Droid)

- [ ] `GPG_PRIVATE_KEY` - GPG key for signing
- [ ] GPG key published to keyserver

### Optional Secrets (for Notifications)

- [ ] `DISCORD_WEBHOOK_ID` - Discord webhook ID
- [ ] `DISCORD_WEBHOOK_TOKEN` - Discord webhook token

---

## 🏗️ Build Checklist

### Local Build Verification

```bash
# Clean and build
make clean
make build

# Validate
make validate

# Build release
make release VERSION=X.X.X
```

- [ ] Clean build succeeds: `./android/gradlew clean`
- [ ] Debug build succeeds: `./android/gradlew assembleDebug`
- [ ] Release build succeeds: `./android/gradlew assembleRelease`
- [ ] AAB build succeeds: `./android/gradlew bundleRelease`

### Build Output Verification

- [ ] APK size reasonable (<50MB without model)
- [ ] APK installs on test device
- [ ] App launches successfully
- [ ] Core features work:
  - [ ] Chat interface loads
  - [ ] Settings screen accessible
  - [ ] Model management works
  - [ ] Navigation functions

### Performance Check

- [ ] App performs well on low-end device (if available)
- [ ] Memory usage acceptable (<500MB typical)
- [ ] No ANR (Application Not Responding) crashes
- [ ] Battery usage reasonable

---

## 📱 Device Testing Checklist

### Android Versions (Test on at least 3)

- [ ] Android 14/15 (API 34/35) - Latest
- [ ] Android 12/13 (API 31-33) - Recent
- [ ] Android 10/11 (API 29-30) - Common
- [ ] Android 9 (API 28) - Minimum supported

### Screen Sizes (Test on at least 2)

- [ ] Phone (1080x1920 or similar)
- [ ] Large phone (1440x2960 or similar)
- [ ] Tablet (if supported)

### Features

- [ ] Dark theme renders correctly
- [ ] Light theme renders correctly
- [ ] Landscape orientation works
- [ ] Split screen mode works
- [ ] Notifications work (if applicable)

---

## 🚀 Deployment Checklist

### Step 1: Create Git Tag

```bash
git tag -a vX.X.X -m "Release vX.X.X"
git push origin vX.X.X
```

- [ ] Tag created with correct version
- [ ] Tag pushed to origin
- [ ] GitHub Actions release workflow triggered

### Step 2: Google Play Store (Primary)

```bash
# Deploy to Internal Testing first
make deploy-internal
```

- [ ] Build signed AAB
- [ ] AAB uploaded to Play Console
- [ ] Store listing reviewed:
  - [ ] Title: "MOMCLAW - Offline AI Agent"
  - [ ] Short description
  - [ ] Full description
  - [ ] Screenshots (all sizes)
  - [ ] Feature graphic
  - [ ] Privacy policy URL
- [ ] Content rating questionnaire completed
- [ ] Target audience selected
- [ ] Release notes added
- [ ] Rollout started (start with 10%)

### Step 3: GitHub Release

```bash
make deploy-github VERSION=X.X.X
```

- [ ] GitHub release created
- [ ] APK attached to release
- [ ] AAB attached to release
- [ ] Release notes from CHANGELOG
- [ ] Release not marked as draft

### Step 4: F-Droid (Optional)

```bash
make build-fdroid VERSION=X.X.X
```

- [ ] F-Droid APK built
- [ ] GPG signature created
- [ ] SHA256 checksum generated
- [ ] Metadata YAML updated
- [ ] Submitted to fdroiddata OR self-hosted repo

---

## 📣 Post-Release Checklist

### Immediate (Day 1)

- [ ] Monitor GitHub Actions for successful completion
- [ ] Verify release appears on GitHub Releases page
- [ ] Verify app appears in Google Play Console
- [ ] Test download from GitHub release
- [ ] Test installation from Play Store (Internal track)

### Short-term (Week 1)

- [ ] Monitor crash reports in Play Console
- [ ] Monitor user feedback
- [ ] Watch GitHub Issues for problems
- [ ] Check app store ratings
- [ ] Respond to user questions

### Documentation Updates

- [ ] Update MOMCLAW-PLAN.md roadmap
- [ ] Close completed GitHub issues
- [ ] Update VERSION_HISTORY in CHANGELOG.md
- [ ] Create release announcement (if applicable)

---

## 🆘 Rollback Plan

### Google Play Rollback

1. Go to Play Console → Release → Release overview
2. Halt current rollout
3. Previous version remains available to users
4. Fix issues and release new version

### GitHub Rollback

1. Delete the release (keeps the tag)
2. Fix issues
3. Re-release with same version or bump version

### F-Droid Rollback

1. Contact F-Droid maintainers (if in main repo)
2. Submit updated version
3. May take time to propagate

---

## 📊 Release Metrics Template

Track these metrics after each release:

| Metric | Target | Actual | Notes |
|--------|--------|--------|-------|
| Crash-free rate | >99.5% | | |
| ANR rate | <0.5% | | |
| Store rating | >4.5 | | |
| Uninstall rate (7-day) | <5% | | |
| MAU growth | +5%/month | | |
| GitHub stars | Track | | |
| Download count | Track | | |

---

## 📅 Release Schedule Template

### Version Naming Convention

- **Major (X.0.0)**: Breaking changes, major features
- **Minor (1.X.0)**: New features, enhancements
- **Patch (1.0.X)**: Bug fixes, minor improvements

### Typical Release Cadence

| Stage | Duration | Purpose |
|-------|----------|---------|
| Internal | 1-2 days | Internal QA testing |
| Alpha | 1 week | Early adopter testing |
| Beta | 1-2 weeks | Public beta testing |
| Production | Ongoing | General availability |

### Release Naming

```
v1.0.0       - Initial production release
v1.0.1       - Bug fixes
v1.1.0       - New features
v1.1.0-beta.1 - Beta release
v1.1.0-rc.1  - Release candidate
```

---

## 🔗 Quick Links

### Documentation

- [BUILD-DEPLOYMENT-GUIDE.md](BUILD-DEPLOYMENT-GUIDE.md) - Complete build/deploy guide
- [DEPLOYMENT.md](DEPLOYMENT.md) - Detailed deployment instructions
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) - Detailed release checklist
- [.github/SECRETS_SETUP.md](.github/SECRETS_SETUP.md) - GitHub secrets setup

### External Resources

- [Google Play Console](https://play.google.com/console)
- [GitHub Releases](https://github.com/serverul/MOMCLAW/releases)
- [Fastlane Docs](https://docs.fastlane.tools)
- [F-Droid Manual](https://f-droid.org/en/docs/)

### Support

- **Issues**: [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)
- **Discussions**: [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions)

---

## 📝 Release Log Template

Use this template to log each release:

```markdown
## vX.X.X - YYYY-MM-DD

### Pre-Release
- [ ] Code quality checks passed
- [ ] Version updated
- [ ] Documentation reviewed
- [ ] Assets verified
- [ ] Security checked

### Build
- [ ] Debug build: ✅
- [ ] Release build: ✅
- [ ] AAB build: ✅

### Testing
- Devices tested: [list devices]
- Android versions: [list versions]
- Issues found: [list or "none"]

### Deployment
- [ ] Git tag: vX.X.X
- [ ] GitHub release: [link]
- [ ] Play Store: [track]
- [ ] F-Droid: [status]

### Post-Release
- Crash-free rate: [value]
- Issues reported: [count]
- User feedback: [summary]

### Notes
[Any additional notes]
```

---

**Last Updated**: 2026-04-06

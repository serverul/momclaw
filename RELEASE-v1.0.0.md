# MomClAW v1.0.0 Final Release Checklist

**Release Date**: 2026-04-06  
**Version**: 1.0.0  
**Status**: 🚧 Pre-Release

---

## ✅ Code & Quality

- [ ] All unit tests pass: `cd android && ./gradlew testDebugUnitTest`
- [ ] Lint clean: `cd android && ./gradlew lintDebug`
- [ ] Detekt clean: `cd android && ./gradlew detekt`
- [ ] No compiler warnings
- [ ] Code review completed
- [ ] No TODOs remaining in production code

## ✅ Version & Metadata

- [x] `versionName = "1.0.0"` in `android/app/build.gradle.kts`
- [x] `versionCode` incremented
- [x] `CHANGELOG.md` has `[1.0.0]` section with all changes
- [x] `README.md` version badge updated
- [x] Fastlane changelog created: `android/fastlane/metadata/android/en-US/changelogs/`

## ✅ Documentation

- [x] `README.md` — overview, quick start, badges
- [x] `USER_GUIDE.md` — end-user documentation
- [x] `QUICKSTART.md` — 5-minute setup
- [x] `DOCUMENTATION.md` — complete technical docs
- [x] `BUILD_CONFIGURATION.md` — signing, ProGuard, CI/CD
- [x] `GOOGLE_PLAY_STORE.md` — Play Store setup guide
- [x] `DEPLOYMENT.md` — deployment to Play + F-Droid
- [x] `PRODUCTION-CHECKLIST.md` — production release checklist
- [x] `DEVELOPMENT.md` — developer guide + architecture
- [x] `TESTING.md` — testing strategy
- [x] `SPEC.md` — technical specifications
- [x] `CONTRIBUTING.md` — contributor guidelines
- [x] `SECURITY.md` — security policy
- [x] `PRIVACY_POLICY.md` — privacy policy
- [x] `CHANGELOG.md` — version history
- [x] `DOCUMENTATION-INDEX.md` — doc index updated
- [x] `.github/SECRETS_SETUP.md` — CI/CD secrets guide

## ✅ GitHub Actions Workflows

- [x] `.github/workflows/android-build.yml` — CI build + test
- [x] `.github/workflows/release.yml` — tag-triggered release
- [x] `.github/workflows/google-play-deploy.yml` — Play Store deploy
- [x] `.github/workflows/security.yml` — security scanning
- [x] `.github/workflows/fdroid-build.yml` — F-Droid build
- [x] `.github/workflows/ci.yml` — PR validation CI
- [x] `.github/workflows/dependabot-auto-merge.yml` — Dependabot auto-merge

## ✅ Build Scripts

- [x] `scripts/ci-build.sh` — main CI/CD automation
- [x] `scripts/build-release.sh` — release build
- [x] `scripts/build-fdroid.sh` — F-Droid build
- [x] `scripts/version-manager.sh` — version management
- [x] `scripts/validate-build.sh` — pre-release validation
- [x] `scripts/validate-startup.sh` — startup validation
- [x] `scripts/validate-integration.sh` — integration validation
- [x] `scripts/run-tests.sh` — test runner
- [x] `scripts/run-integration-tests.sh` — integration tests
- [x] `scripts/setup.sh` — project setup
- [x] `scripts/download-model.sh` — model download
- [x] `scripts/generate-icons.sh` — icon generation
- [x] `Makefile` — top-level build shortcuts

## ✅ Signing & Security

- [ ] Keystore generated: `MOMCLAW-release-key.jks`
- [ ] Keystore backed up securely (offline + encrypted)
- [ ] `key.properties` created (NOT in git)
- [ ] `.gitignore` excludes `key.properties` and keystores
- [ ] No secrets in source code
- [ ] ProGuard rules tested with release build
- [ ] GitHub Secrets configured:
  - [ ] `KEYSTORE_BASE64`
  - [ ] `STORE_PASSWORD`
  - [ ] `KEY_PASSWORD`
  - [ ] `KEY_ALIAS`
  - [ ] `PLAY_STORE_SERVICE_ACCOUNT` (optional)

## ✅ Store Assets

- [ ] App icon 512x512 (`assets/icon.png`)
- [ ] Feature graphic 1024x500 (`assets/feature-graphic.png`)
- [ ] Phone screenshots (2-8 per language)
- [ ] Tablet screenshots (recommended)
- [ ] Store listing metadata:
  - [ ] Title: "MomClAW - Offline AI Agent"
  - [ ] Short description
  - [ ] Full description
  - [ ] Privacy policy URL

## ✅ Build Verification

- [ ] Clean build: `cd android && ./gradlew clean`
- [ ] Debug APK builds: `cd android && ./gradlew assembleDebug`
- [ ] Release APK builds: `cd android && ./gradlew assembleRelease`
- [ ] Release AAB builds: `cd android && ./gradlew bundleRelease`
- [ ] APK installs on test device
- [ ] App launches successfully
- [ ] Core features work:
  - [ ] Chat interface
  - [ ] Model loading
  - [ ] Settings persistence
  - [ ] Conversation history

## ✅ Device Testing

- [ ] Android 14/15 (API 34/35)
- [ ] Android 12/13 (API 31-33)
- [ ] Android 10/11 (API 29-30)
- [ ] Android 9 (API 28) — minimum supported
- [ ] Phone form factor
- [ ] Dark theme + light theme

## ✅ Deployment Steps

### Step 1: Tag & Push
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push && git push --tags
```

### Step 2: Verify CI
- [ ] GitHub Actions `android-build.yml` passes
- [ ] GitHub Actions `release.yml` passes
- [ ] Release appears on GitHub Releases page

### Step 3: Google Play Store
- [ ] AAB uploaded to Play Console
- [ ] Store listing complete
- [ ] Content rating questionnaire done
- [ ] Target audience selected
- [ ] Release to Internal Testing track first
- [ ] Verify install from Play Store

### Step 4: Post-Release
- [ ] Monitor crash reports (24-48h)
- [ ] Monitor user feedback
- [ ] Update MOMCLAW-PLAN.md roadmap
- [ ] Close completed issues
- [ ] Bump to next snapshot: `./scripts/version-manager.sh snapshot`

## ✅ Compliance

- [x] Privacy policy hosted and URL in store listing
- [x] Data Safety section completed (no data collected)
- [x] Content rating appropriate (Everyone / PEGI 3)
- [x] Export compliance (encryption declaration)
- [x] Target API level 35 (Android 15)

---

## 📋 Sign-off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Developer | | | |
| QA | | | |
| Release Manager | | | |

---

**Ready for release when all checkboxes above are checked.**

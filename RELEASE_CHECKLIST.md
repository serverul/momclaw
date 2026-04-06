# MOMCLAW Release Checklist

Use this checklist for every release to ensure nothing is missed.

---

## 📋 Pre-Release Checklist

### Code Quality

- [ ] All tests pass: `./scripts/ci-build.sh test:all`
- [ ] Lint passes: `./android/gradlew lintDebug`
- [ ] Detekt passes: `./android/gradlew detekt`
- [ ] No compiler warnings
- [ ] Code coverage acceptable (target: 60%+)

### Version Updates

- [ ] Version code incremented in `android/app/build.gradle.kts`
- [ ] Version name updated in `android/app/build.gradle.kts`
- [ ] CHANGELOG.md updated with release notes
- [ ] Fastlane changelog created: `android/fastlane/metadata/android/en-US/changelogs/VERSION_CODE.txt`

### Documentation

- [ ] README.md reviewed and updated
- [ ] DOCUMENTATION.md updated if needed
- [ ] CHANGELOG.md has all changes listed
- [ ] CONTRIBUTING.md reviewed
- [ ] API documentation current (if API changed)

### Assets

- [ ] Screenshots current and representative
- [ ] Store metadata reviewed (title, description)
- [ ] Feature graphic updated (if needed)
- [ ] App icon matches current version

### Security

- [ ] No secrets in code
- [ ] ProGuard rules tested
- [ ] Release build signed correctly
- [ ] No debug flags in release

---

## 🔐 Signing & Secrets Checklist

### Keystore

- [ ] Keystore backed up securely
- [ ] Keystore password accessible to team
- [ ] key.properties not in git
- [ ] key.properties.example updated

### CI/CD Secrets

- [ ] KEYSTORE_BASE64 in GitHub Secrets
- [ ] STORE_PASSWORD in GitHub Secrets
- [ ] KEY_PASSWORD in GitHub Secrets
- [ ] KEY_ALIAS in GitHub Secrets
- [ ] GOOGLE_PLAY_SERVICE_ACCOUNT (if deploying to Play)
- [ ] GPG_PRIVATE_KEY (for F-Droid builds)

---

## 🏗️ Build Checklist

### Local Build

- [ ] Clean build succeeds: `./android/gradlew clean`
- [ ] Debug build succeeds: `./android/gradlew assembleDebug`
- [ ] Release build succeeds: `./android/gradlew assembleRelease`
- [ ] AAB build succeeds: `./android/gradlew bundleRelease`

### Build Verification

- [ ] APK size reasonable (<50MB without model)
- [ ] APK installs on test device
- [ ] App launches successfully
- [ ] Core features work:
  - [ ] Chat interface
  - [ ] Model loading
  - [ ] Settings persistence
  - [ ] Memory/database

### Performance Test

- [ ] App performs well on low-end device
- [ ] Memory usage acceptable
- [ ] Battery usage acceptable
- [ ] No ANR (Application Not Responding) crashes

---

## 📱 Device Testing Checklist

### Android Versions

- [ ] Android 14 (API 34)
- [ ] Android 13 (API 33)
- [ ] Android 12 (API 31)
- [ ] Android 11 (API 30)
- [ ] Android 10 (API 29)
- [ ] Android 9 (API 28) - Minimum supported

### Screen Sizes

- [ ] Phone (1080x1920)
- [ ] Large phone (1440x2960)
- [ ] Tablet (if supported)

### Features

- [ ] Dark theme
- [ ] Light theme
- [ ] Landscape orientation
- [ ] Split screen mode
- [ ] Notifications

---

## 🚀 Deployment Checklist

### Google Play Store

- [ ] Build signed AAB
- [ ] AAB uploaded to Play Console
- [ ] Store listing reviewed:
  - [ ] Title
  - [ ] Short description
  - [ ] Full description
  - [ ] Screenshots
  - [ ] Feature graphic
  - [ ] Privacy policy URL
- [ ] Content rating questionnaire completed
- [ ] Target audience selected
- [ ] Release notes added
- [ ] Rollout percentage set (start with 10%)
- [ ] Review started

### F-Droid (if applicable)

- [ ] Build unsigned APK
- [ ] Sign with GPG
- [ ] Generate checksums
- [ ] Update metadata YAML
- [ ] Submit to fdroiddata (or self-hosted repo)

### GitHub Release

- [ ] Git tag created: `git tag -a v1.0.0 -m "Release v1.0.0"`
- [ ] Tag pushed: `git push origin v1.0.0`
- [ ] GitHub Actions workflow started
- [ ] Release published on GitHub
- [ ] APK/AAB attached to release
- [ ] Release notes from CHANGELOG

---

## 📣 Post-Release Checklist

### Communication

- [ ] Announcement on GitHub Discussions
- [ ] Social media post (if applicable)
- [ ] Discord/Telegram notification (if applicable)
- [ ] Update website (if applicable)

### Monitoring

- [ ] Monitor crash reports (Play Console)
- [ ] Monitor user feedback
- [ ] Watch GitHub Issues for problems
- [ ] Check app store ratings

### Documentation

- [ ] Update ROADMAP in MOMCLAW-PLAN.md
- [ ] Close completed issues
- [ ] Update VERSION_HISTORY in CHANGELOG.md

---

## 🔄 Rollback Plan

If critical issues are found:

1. **Google Play:**
   - Halt rollout in Play Console
   - Previous version remains available
   - Fix and release new version

2. **GitHub:**
   - Delete release (keeps tag)
   - Re-release when fixed
   - Notify users via Discussions

3. **F-Droid:**
   - Contact maintainers
   - Submit updated version
   - May take time to propagate

---

## 📅 Release Schedule Template

### Version Naming

- **Major (X.0.0):** Breaking changes
- **Minor (1.X.0):** New features
- **Patch (1.0.X):** Bug fixes

### Typical Cadence

- **Alpha:** Internal testing (as needed)
- **Beta:** Public testing (1-2 weeks before release)
- **Production:** Stable release (monthly or as needed)

---

## 🆘 Emergency Contacts

- **GitHub Issues:** https://github.com/serverul/MOMCLAW/issues
- **Discussions:** https://github.com/serverul/MOMCLAW/discussions
- **Security Issues:** security@example.com (TODO)

---

## 📊 Release Metrics Template

Track these metrics after each release:

| Metric | Target | Actual |
|--------|--------|--------|
| Crash-free rate | >99.5% | |
| ANR rate | <0.5% | |
| Store rating | >4.5 | |
| Uninstall rate | <5%/week | |
| MAU growth | +5%/month | |

---

**Last Updated:** 2026-04-06

*Update this checklist as processes evolve.*

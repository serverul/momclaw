# 🚀 MomClAW Release Checklist

Use this checklist for every release to ensure nothing is missed.

## 📋 Pre-Release Checklist

### 1. Code Quality
- [ ] All tests passing locally (`./scripts/run-tests.sh`)
- [ ] Lint checks pass (`./gradlew lintDebug`)
- [ ] Detekt analysis clean (`./gradlew detekt`)
- [ ] No security vulnerabilities (review CodeQL alerts)
- [ ] Code reviewed and approved (if team project)

### 2. Version Management
- [ ] Update `versionCode` in `android/app/build.gradle.kts`
- [ ] Update `versionName` in `android/app/build.gradle.kts`
- [ ] Update CHANGELOG.md with new version section
- [ ] Move items from `[Unreleased]` to new version section
- [ ] Add release date to CHANGELOG.md
- [ ] Commit version bump: `git commit -am "chore: bump version to X.Y.Z"`

### 3. Documentation
- [ ] README.md up to date
- [ ] CHANGELOG.md updated with all changes
- [ ] Breaking changes documented (if any)
- [ ] Migration guide provided (if breaking changes)
- [ ] Screenshots updated (if UI changes)

### 4. Testing
- [ ] Unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] Integration tests pass (if available)
- [ ] Manual testing on physical device
- [ ] Test on multiple Android versions (minSdk to targetSdk)
- [ ] Test on different screen sizes
- [ ] Test offline functionality
- [ ] Test memory usage and performance

### 5. Build Verification
- [ ] Debug APK builds successfully (`./gradlew assembleDebug`)
- [ ] Release APK builds successfully (`./gradlew assembleRelease`)
- [ ] Release AAB builds successfully (`./gradlew bundleRelease`)
- [ ] APK installs and runs correctly
- [ ] APK signing works (if keystore configured)

## 🏷️ Release Process

### Option A: Automated (Recommended)

1. **Prepare Release:**
   ```bash
   # Ensure clean working directory
   git status
   
   # Create and push tag
   git tag -a v1.0.1 -m "Release v1.0.1: Bug fixes and improvements"
   git push origin v1.0.1
   ```

2. **Monitor Workflow:**
   - Go to GitHub Actions → release workflow
   - Wait for build to complete (~15-20 minutes)
   - Check all steps passed

3. **Verify Release:**
   - Check GitHub Releases page
   - Download APKs and test
   - Verify changelog is correct
   - Test installation on device

### Option B: Manual

1. **Build Artifacts:**
   ```bash
   # Clean build
   ./gradlew clean
   
   # Build release APK
   ./gradlew assembleRelease
   
   # Build release AAB
   ./gradlew bundleRelease
   ```

2. **Sign APKs (if not auto-signed):**
   ```bash
   # Use apksigner if needed
   apksigner sign --ks MOMCLAW-release-key.jks \
     --out app-release-signed.apk \
     app/build/outputs/apk/release/app-release-unsigned.apk
   ```

3. **Create GitHub Release:**
   - Go to GitHub → Releases → Draft a new release
   - Choose tag (create new if needed)
   - Fill in title and description
   - Upload APKs and AAB
   - Mark as pre-release if needed
   - Publish release

## 📱 Google Play Store Deployment

### Prerequisites
- [ ] Google Play Console account
- [ ] Service account JSON configured in GitHub Secrets
- [ ] App reviewed and approved at least once
- [ ] Store listing complete (screenshots, descriptions, etc.)

### Deployment Steps

1. **Internal Testing (First):**
   ```bash
   # Tag with -alpha suffix
   git tag -a v1.1.0-alpha.1 -m "Alpha release for internal testing"
   git push origin v1.1.0-alpha.1
   ```
   - Workflow will deploy to Internal Testing track
   - Test with internal testers

2. **Alpha/Beta Testing:**
   ```bash
   # After internal testing passes
   git tag -a v1.1.0-beta.1 -m "Beta release for testing"
   git push origin v1.1.0-beta.1
   ```
   - Deploy to Alpha/Beta track
   - Gather feedback from testers

3. **Production Release:**
   ```bash
   # After beta testing passes
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```
   - Deploy to Production track
   - Monitor for issues

### Post-Deployment
- [ ] Monitor crash reports
- [ ] Monitor user feedback
- [ ] Check install metrics
- [ ] Be ready to rollback if critical issues found

## 🔍 Post-Release Verification

- [ ] GitHub Release page shows all artifacts
- [ ] APKs download and install successfully
- [ ] App version shows correctly in Settings
- [ ] No crash on first launch
- [ ] Core functionality works
- [ ] Google Play listing updated (if deployed)
- [ ] Social media announcement (optional)

## 🐛 Hotfix Process

If critical bug found after release:

1. **Create hotfix branch:**
   ```bash
   git checkout -b hotfix/v1.0.1 v1.0.0
   ```

2. **Fix bug and test:**
   - Make minimal fix
   - Test thoroughly
   - Update version to 1.0.1

3. **Merge and release:**
   ```bash
   git checkout main
   git merge hotfix/v1.0.1
   git tag -a v1.0.1 -m "Hotfix: Critical bug fix"
   git push origin main --tags
   ```

4. **Document:**
   - Update CHANGELOG.md
   - Add hotfix notes
   - Notify users

## 📊 Monitoring

After release, monitor:
- GitHub Issues for bug reports
- Crash reporting (if integrated)
- User feedback
- Performance metrics
- Download statistics

## 🎯 Quick Commands

```bash
# Check current version
grep "versionName" android/app/build.gradle.kts

# Run all tests
./scripts/run-tests.sh

# Build release APK
./gradlew assembleRelease

# Build release AAB
./gradlew bundleRelease

# Create signed release (if keystore exists)
./scripts/build-release.sh

# Validate build
./scripts/validate-build.sh

# Create and push tag
git tag -a v1.0.1 -m "Release v1.0.1"
git push origin v1.0.1

# Delete tag (if needed)
git tag -d v1.0.1
git push origin :refs/tags/v1.0.1
```

## 📝 Version Naming Convention

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR.MINOR.PATCH** (e.g., 1.0.0)
  - MAJOR: Breaking changes
  - MINOR: New features, backwards compatible
  - PATCH: Bug fixes, backwards compatible

- **Pre-release suffixes:**
  - `-alpha.N`: Internal testing
  - `-beta.N`: Public beta testing
  - `-rc.N`: Release candidate

Examples:
- `v1.0.0` - Initial stable release
- `v1.0.1` - Bug fix release
- `v1.1.0` - New features release
- `v2.0.0` - Breaking changes release
- `v1.2.0-alpha.1` - Alpha for internal testing
- `v1.2.0-beta.1` - Beta for public testing
- `v1.2.0-rc.1` - Release candidate

## ✅ Final Checklist Before Tagging

- [ ] Version bumped in build.gradle.kts
- [ ] CHANGELOG.md updated
- [ ] All tests passing
- [ ] Working directory clean (no uncommitted changes)
- [ ] On correct branch (main for stable releases)
- [ ] Git status shows nothing to commit
- [ ] Ready to create tag

---

**Remember:** Once a release is tagged and pushed, it cannot be easily undone. Take your time with the checklist!

# MOMCLAW Version Management

**Document Purpose**: Guide for managing versions across the project  
**Last Updated**: 2026-04-06

---

## 📖 Overview

MOMCLAW follows [Semantic Versioning (SemVer)](https://semver.org/) with the format `MAJOR.MINOR.PATCH`:

- **MAJOR**: Incompatible API changes
- **MINOR**: New features, backward-compatible
- **PATCH**: Bug fixes, backward-compatible

### Version Format

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
- 1.0.0        (stable release)
- 1.1.0-beta.1 (beta release)
- 1.2.0-rc.1   (release candidate)
- 2.0.0-alpha  (alpha release)
```

---

## 🛠️ Version Manager Script

### Location
```bash
scripts/version-manager.sh
```

### Usage

#### Get Current Version
```bash
./scripts/version-manager.sh current
# Output: 1.0.0 (1)
```

#### Increment Version
```bash
# Increment patch version (1.0.0 → 1.0.1)
./scripts/version-manager.sh increment patch

# Increment minor version (1.0.0 → 1.1.0)
./scripts/version-manager.sh increment minor

# Increment major version (1.0.0 → 2.0.0)
./scripts/version-manager.sh increment major
```

#### Set Specific Version
```bash
./scripts/version-manager.sh set 2.0.0 2
# Sets versionName = "2.0.0" and versionCode = 2
```

#### Create Pre-release
```bash
# Create beta version
./scripts/version-manager.sh prerelease beta 1

# Create release candidate
./scripts/version-manager.sh prerelease rc 2

# Create alpha version
./scripts/version-manager.sh prerelease alpha
```

---

## 📝 Files Updated

When version changes, these files are updated:

### 1. `android/app/build.gradle.kts`

```kotlin
android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

### 2. `CHANGELOG.md`

```markdown
## [1.0.0] - 2026-04-06

### Added
- Initial release
- Chat interface with Gemma 3 E4B-it
- Model management system
- Settings and preferences

### Changed
- N/A

### Fixed
- N/A
```

### 3. `README.md`

Version badge:
```markdown
[![Release](https://img.shields.io/github/v/release/serverul/MOMCLAW)](https://github.com/serverul/MOMCLAW/releases)
```

### 4. `android/fastlane/metadata/android/en-US/changelogs/1000000.txt`

```
Initial release of MomClAW!

Features:
- Offline AI chat with Gemma 3 E4B-it
- Persistent conversation memory
- Tool execution (shell, files, web search)
- Modern Material You design
```

---

## 🔄 Release Process

### Step 1: Update Version

```bash
# Option A: Increment patch
./scripts/version-manager.sh increment patch

# Option B: Increment minor
./scripts/version-manager.sh increment minor

# Option C: Set specific version
./scripts/version-manager.sh set 1.2.0 120
```

### Step 2: Update Changelog

Edit `CHANGELOG.md`:

```bash
# Add new version section
nano CHANGELOG.md
```

### Step 3: Commit Version Bump

```bash
git add android/app/build.gradle.kts CHANGELOG.md
git commit -m "chore: bump version to 1.2.0"
git push
```

### Step 4: Create Git Tag

```bash
git tag -a v1.2.0 -m "Release v1.2.0"
git push --tags
```

### Step 5: Verify CI/CD

Check GitHub Actions:
- ✅ CI workflow passes
- ✅ Release workflow creates artifacts
- ✅ GitHub release created
- ✅ Google Play deployment (if configured)

---

## 🏷️ Version Naming Conventions

### Stable Releases
- Format: `vMAJOR.MINOR.PATCH`
- Examples: `v1.0.0`, `v1.1.0`, `v2.0.0`

### Pre-releases
- Alpha: `v1.0.0-alpha`, `v1.0.0-alpha.1`
- Beta: `v1.0.0-beta`, `v1.0.0-beta.2`
- Release Candidate: `v1.0.0-rc.1`, `v1.0.0-rc.2`

### Build Metadata
- Format: `v1.0.0+build.123`
- Used for internal builds

---

## 📊 Version Code Strategy

Version codes follow a pattern to ensure uniqueness:

```
MAJOR * 1000000 + MINOR * 1000 + PATCH

Examples:
- 1.0.0 → 1000000
- 1.1.0 → 1001000
- 1.2.3 → 1002003
- 2.0.0 → 2000000
```

This ensures:
- Unique version codes
- Monotonically increasing
- Easy to understand

---

## 🔧 Automation

### GitHub Actions

The release workflow automatically:
1. Extracts version from tag
2. Builds signed APK/AAB
3. Creates GitHub release
4. Deploys to Google Play (if configured)

### Fastlane

Fastlane automatically:
1. Reads version from `build.gradle.kts`
2. Updates Play Store metadata
3. Uploads to specified track

---

## 🚫 Common Mistakes

### ❌ Wrong: Manual Version Code
```kotlin
versionCode = 1  // Forgot to increment!
versionName = "1.1.0"
```

### ✅ Correct: Use Script
```bash
./scripts/version-manager.sh increment minor
# Automatically increments both versionName and versionCode
```

---

### ❌ Wrong: Tag Before Commit
```bash
git tag v1.0.0
# Forgot to update build.gradle.kts!
git commit -m "Update version"
```

### ✅ Correct: Commit Then Tag
```bash
# Update version
./scripts/version-manager.sh set 1.0.0 1000000
git add android/app/build.gradle.kts
git commit -m "chore: bump version to 1.0.0"

# Then create tag
git tag v1.0.0
git push && git push --tags
```

---

## 📋 Checklist

Before releasing a new version:

- [ ] Version bumped in `build.gradle.kts`
- [ ] Version code incremented
- [ ] `CHANGELOG.md` updated with new section
- [ ] Fastlane changelog created
- [ ] README.md badges updated (if needed)
- [ ] Git commit created
- [ ] Git tag created
- [ ] Tag pushed to remote
- [ ] CI/CD passes
- [ ] GitHub release appears
- [ ] Google Play deployment successful (if configured)

---

## 🔍 Troubleshooting

### Version Code Already Used

**Error**: `versionCode 1 has already been used`

**Solution**: Increment version code
```bash
./scripts/version-manager.sh increment patch
```

### Tag Already Exists

**Error**: `fatal: tag 'v1.0.0' already exists`

**Solution**: Delete and recreate
```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
git tag v1.0.0
git push --tags
```

### Version Mismatch

**Error**: Version in code ≠ version in tag

**Solution**: Ensure consistency
```bash
# Check current version
./scripts/version-manager.sh current

# Set to match tag
./scripts/version-manager.sh set 1.0.0 1000000
```

---

## 📚 Resources

- [Semantic Versioning Spec](https://semver.org/)
- [Android Versioning Guide](https://developer.android.com/studio/publish/versioning)
- [Google Play Versioning](https://support.google.com/googleplay/android-developer/answer/9859350)

---

## 💡 Best Practices

1. **Always use the version manager script** - Avoid manual edits
2. **Update CHANGELOG.md before tagging** - Document changes
3. **Test on multiple devices** - Verify before release
4. **Use pre-release tags** - Test with alpha/beta/rc
5. **Monitor Google Play Console** - Check for issues after deployment

---

**Last Updated**: 2026-04-06  
**Maintained by**: MomClAW Team

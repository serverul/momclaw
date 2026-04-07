# GitHub Actions Workflows

This directory contains CI/CD workflows for MOMCLAW.

---

## 📂 Workflow Files

### 1. `ci.yml` - Continuous Integration

**Triggers**: 
- Push to `main` or `develop`
- Pull requests to `main` or `develop`

**Jobs**:
- **lint**: Run Android Lint
- **detekt**: Run Kotlin static analysis
- **unit-tests**: Run unit tests
- **build-debug**: Build debug APK
- **build-release**: Build unsigned release APK

**Usage**:
```bash
# Automatic on push/PR
git push origin main
```

---

### 2. `release.yml` - Release Automation

**Triggers**:
- Push tags matching `v*.*.*`

**Jobs**:
- **build-signed-release**: Build signed APK and AAB
- **github-release**: Create GitHub release with artifacts
- **deploy-internal**: Deploy to Google Play Internal track
- **notify**: Send Discord notification

**Usage**:
```bash
# Create and push a tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push --tags
```

**Required Secrets**:
- `KEYSTORE_BASE64` - Base64-encoded keystore
- `STORE_PASSWORD` - Keystore password
- `KEY_PASSWORD` - Key password
- `KEY_ALIAS` - Key alias
- `PLAY_STORE_SERVICE_ACCOUNT` - Google Play service account JSON (optional)
- `DISCORD_WEBHOOK` - Discord webhook URL (optional)

---

### 3. `play-store.yml` - Google Play Deployment

**Triggers**:
- Manual workflow dispatch

**Inputs**:
- `track`: Target track (internal, alpha, beta, production)
- `version`: Version to deploy (optional)

**Jobs**:
- **build**: Build release AAB
- **deploy**: Deploy to selected track
- **notify**: Send Discord notification

**Usage**:
1. Go to Actions → "Deploy to Google Play"
2. Select track (internal, alpha, beta, production)
3. Optionally specify version
4. Click "Run workflow"

---

### 4. `security.yml` - Security Scanning

**Triggers**:
- Push to `main` or `develop`
- Pull requests to `main`
- Weekly schedule (Monday 2 AM UTC)

**Jobs**:
- **dependency-check**: Check for vulnerable dependencies
- **secrets-scan**: Scan for leaked secrets (TruffleHog)
- **codeql**: CodeQL security analysis
- **owasp-dependency-check**: OWASP dependency check
- **android-lint-security**: Android Lint with security focus
- **scorecards**: OpenSSF Scorecards analysis

**Usage**:
```bash
# Automatic on push/PR
# Or manual via workflow_dispatch
```

---

### 5. `fdroid.yml` - F-Droid Build

**Triggers**:
- Push tags matching `v*.*.*`
- Manual workflow dispatch

**Inputs**:
- `version`: Version to build (optional)

**Jobs**:
- **build-fdroid**: Build FOSS-compliant APK
- **verify-foss**: Verify no proprietary dependencies
- **create-release**: Create F-Droid release on GitHub
- **notify**: Send Discord notification

**Usage**:
```bash
# Automatic with release tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push --tags

# Or manual via workflow_dispatch
```

---

## 🔐 Required Secrets

### Signing Secrets

| Secret | Description | Required For |
|--------|-------------|--------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file | Release builds |
| `STORE_PASSWORD` | Keystore password | Release builds |
| `KEY_PASSWORD` | Key password | Release builds |
| `KEY_ALIAS` | Key alias | Release builds |

### Deployment Secrets

| Secret | Description | Required For |
|--------|-------------|--------------|
| `PLAY_STORE_SERVICE_ACCOUNT` | Google Play service account JSON | Play Store deployment |
| `DISCORD_WEBHOOK` | Discord webhook URL | Notifications |

### How to Set Secrets

1. Go to repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Enter name and value
4. Click "Add secret"

See [SECRETS_SETUP.md](./SECRETS_SETUP.md) for detailed instructions.

---

## 📊 Workflow Status Badges

Add these to your README.md:

```markdown
[![CI](https://github.com/serverul/MOMCLAW/workflows/CI/badge.svg)](https://github.com/serverul/MOMCLAW/actions/workflows/ci.yml)
[![Release](https://github.com/serverul/MOMCLAW/workflows/Release/badge.svg)](https://github.com/serverul/MOMCLAW/actions/workflows/release.yml)
[![Security](https://github.com/serverul/MOMCLAW/workflows/Security/badge.svg)](https://github.com/serverul/MOMCLAW/actions/workflows/security.yml)
```

---

## 🚀 Quick Start

### 1. Set Up Secrets

```bash
# Generate keystore
keytool -genkey -v -keystore MOMCLAW-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias MOMCLAW

# Encode to base64
base64 -i MOMCLAW-release-key.jks | pbcopy  # macOS
base64 -w 0 MOMCLAW-release-key.jks         # Linux

# Add to GitHub Secrets
# KEYSTORE_BASE64: <paste base64>
# STORE_PASSWORD: <your-store-password>
# KEY_PASSWORD: <your-key-password>
# KEY_ALIAS: MOMCLAW
```

### 2. Push to Trigger CI

```bash
git add .
git commit -m "feat: add new feature"
git push origin main
```

### 3. Create Release

```bash
# Update version
./scripts/version-manager.sh increment minor

# Commit and tag
git add .
git commit -m "chore: bump version to 1.1.0"
git tag -a v1.1.0 -m "Release v1.1.0"
git push && git push --tags
```

### 4. Verify

- Check GitHub Actions: https://github.com/serverul/MOMCLAW/actions
- Check GitHub Releases: https://github.com/serverul/MOMCLAW/releases
- Check Google Play Console (if configured)

---

## 🔧 Troubleshooting

### Build Fails: "Keystore not found"

**Cause**: `KEYSTORE_BASE64` secret not set or invalid

**Solution**: 
```bash
# Re-encode keystore
base64 -w 0 MOMCLAW-release-key.jks
# Update secret in GitHub
```

### Build Fails: "Keystore was tampered with"

**Cause**: Wrong password in secrets

**Solution**: Verify `STORE_PASSWORD` and `KEY_PASSWORD` are correct

### Play Store Deploy Fails: "Unauthorized"

**Cause**: Invalid or missing `PLAY_STORE_SERVICE_ACCOUNT`

**Solution**: 
1. Create service account in Google Cloud Console
2. Download JSON key
3. Add to GitHub Secrets as `PLAY_STORE_SERVICE_ACCOUNT`

### Release Not Created

**Cause**: Tag doesn't match pattern `v*.*.*`

**Solution**: Ensure tag format is correct (e.g., `v1.0.0`, not `1.0.0`)

---

## 📚 Advanced Configuration

### Skip CI

Add `[skip ci]` to commit message:
```bash
git commit -m "docs: update README [skip ci]"
```

### Manual Trigger

Go to Actions → Select workflow → Run workflow

### Custom Build Matrix

Edit `ci.yml` to add more configurations:
```yaml
strategy:
  matrix:
    api-level: [28, 30, 33, 35]
    target: [google_apis]
```

---

## 📖 Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI Best Practices](https://developer.android.com/studio/build/building-ci)
- [Fastlane Documentation](https://docs.fastlane.tools/)
- [Google Play Developer API](https://developers.google.com/android-publisher)

---

**Last Updated**: 2026-04-06  
**Maintained by**: MOMCLAW Team

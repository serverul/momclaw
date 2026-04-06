# MomClaw Build Scripts

Aceste scripturi automatizează build-ul, testarea și deployment-ul pentru MomClaw.

## 📁 Structura

```
scripts/
├── ci-build.sh          # Main script - entry point pentru toate operațiunile
├── build-release.sh     # Build release APK + AAB
├── build-fdroid.sh      # Build F-Droid APK
├── validate-build.sh    # Validare completă pre-release
├── run-tests.sh         # Rulează toate testele
└── download-model.sh    # Descarcă modelul AI
```

## 🚀 Quick Start

```bash
# Vezi toate comenzile disponibile
./scripts/ci-build.sh help

# Build release
./scripts/ci-build.sh build:release 1.0.0

# Testează
./scripts/ci-build.sh test:all

# Deploayază pe Google Play
./scripts/ci-build.sh deploy:internal
```

## 📖 Comenzi Detaliate

### Build

```bash
# Build debug APK
./scripts/ci-build.sh build:debug

# Build release APK + AAB
./scripts/ci-build.sh build:release 1.0.0
# Output:
#   - momclaw-1.0.0.apk
#   - momclaw-1.0.0.aab
#   - checksums .sha256

# Build F-Droid APK (unsigned + GPG signed)
./scripts/ci-build.sh build:fdroid 1.0.0
# Output:
#   - momclaw-1.0.0-fdroid.apk
#   - momclaw-1.0.0-fdroid.apk.asc
```

### Test

```bash
# Unit tests
./scripts/ci-build.sh test:unit

# Instrumented tests (requires device)
./scripts/ci-build.sh test:instrumented

# All tests + lint
./scripts/ci-build.sh test:all

# With coverage report
./scripts/ci-build.sh test:coverage
```

### Quality

```bash
# Android lint
./scripts/ci-build.sh lint

# Kotlin static analysis
./scripts/ci-build.sh detekt

# Full validation (lint + tests + checks)
./scripts/ci-build.sh validate
```

### Deploy

```bash
# Google Play Internal Testing
./scripts/ci-build.sh deploy:internal

# Google Play Alpha
./scripts/ci-build.sh deploy:alpha

# Google Play Beta
./scripts/ci-build.sh deploy:beta

# Google Play Production
./scripts/ci-build.sh deploy:production

# GitHub Release
./scripts/ci-build.sh deploy:github 1.0.0
```

### Fastlane

```bash
# List available lanes
./scripts/ci-build.sh fastlane:list

# Run specific lane
./scripts/ci-build.sh fastlane promote_alpha_to_beta
```

### Utility

```bash
# Clean build artifacts
./scripts/ci-build.sh clean

# Generate signing keystore
./scripts/ci-build.sh keystore:generate

# Download AI model
./scripts/ci-build.sh model:download
```

## 🔧 Prerequisites

### General
- **JDK 17+** (pentru build-uri)
- **Android SDK** (API 28+)
- **Git** (pentru versioning)

### Google Play Deployment
- **Fastlane**: `gem install fastlane`
- **Service Account JSON**: Download din Google Play Console
- **Keystore**: Pentru semnare APK/AAB

### F-Droid Build
- **GPG**: Pentru semnături
- **GPG Key**: Generat și publicat pe keyserver

### GitHub Release
- **GitHub CLI**: Install from https://cli.github.com/

## 📋 Workflow Recomandat

### Pentru Release

1. **Validare pre-release**:
   ```bash
   ./scripts/ci-build.sh validate
   ```

2. **Build release**:
   ```bash
   ./scripts/ci-build.sh build:release 1.0.0
   ```

3. **Test manual pe device**:
   ```bash
   adb install momclaw-1.0.0.apk
   # Test features...
   ```

4. **Deploy pe Google Play Internal**:
   ```bash
   ./scripts/ci-build.sh deploy:internal
   ```

5. **Review în Play Console** (așteaptă review)

6. **Promote to Alpha/Beta/Production**:
   ```bash
   ./scripts/ci-build.sh fastlane promote_internal_to_alpha
   # sau
   ./scripts/ci-build.sh deploy:alpha
   ```

7. **GitHub Release**:
   ```bash
   ./scripts/ci-build.sh deploy:github 1.0.0
   ```

### Pentru F-Droid

1. **Build F-Droid APK**:
   ```bash
   ./scripts/ci-build.sh build:fdroid 1.0.0
   ```

2. **Verifică signature**:
   ```bash
   gpg --verify momclaw-1.0.0-fdroid.apk.asc
   ```

3. **Submit la F-Droid**:
   - Vezi [DEPLOYMENT.md](../DEPLOYMENT.md) pentru detalii

## 🔐 Configurare Secrets

### Google Play

1. Creează Service Account în Google Cloud Console
2. Download JSON key
3. Salvează ca `android/google-play-service-account.json`
4. **NU commit-ui acest fișier!**

### Keystore

1. Generează keystore:
   ```bash
   ./scripts/ci-build.sh keystore:generate
   ```

2. Creează `android/key.properties`:
   ```properties
   storePassword=YOUR_PASSWORD
   keyPassword=YOUR_PASSWORD
   keyAlias=momclaw
   storeFile=../momclaw-release-key.jks
   ```

3. **Backup keystore-ul securizat!**

### GPG (pentru F-Droid)

1. Generează cheie:
   ```bash
   gpg --full-generate-key
   ```

2. Publică pe keyserver:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

## 🐛 Troubleshooting

### Build fail: "SDK not found"
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Build fail: "Java version mismatch"
```bash
# Verifică versiunea
java -version

# Trebuie să fie 17+
```

### Deploy fail: "Service account not found"
```bash
# Verifică că fișierul există
ls -la android/google-play-service-account.json

# Verifică că e valid JSON
cat android/google-play-service-account.json | jq .
```

### GPG signing fail
```bash
# Verifică că ai cheia
gpg --list-secret-keys

# Import dacă e necesar
gpg --import your-key.asc
```

## 📚 Documentație

- [DEPLOYMENT.md](../DEPLOYMENT.md) - Deployment complet Google Play + F-Droid
- [BUILD.md](../BUILD.md) - Build instructions detaliate
- [DEVELOPMENT.md](../DEVELOPMENT.md) - Developer guide
- [DOCUMENTATION.md](../DOCUMENTATION.md) - Documentație completă

## 🆘 Suport

Pentru probleme sau întrebări:
1. Consultă documentația de mai sus
2. Verifică [Issues](https://github.com/serverul/momclaw/issues)
3. Întreabă pe [Discussions](https://github.com/serverul/momclaw/discussions)

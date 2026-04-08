# MOMCLAW Deployment Guide

Ghid complet pentru deployment MOMCLAW pe Google Play Store și F-Droid.

---

## 📋 Cuprins

- [Prerequisites](#prerequisites)
- [Google Play Store](#google-play-store)
- [F-Droid](#f-droid)
- [Automation](#automation)
- [Troubleshooting](#troubleshooting)

---

## 🛠️ Prerequisites

### General

- JDK 17+
- Android SDK (API 35)
- Android NDK r25c+
- Git
- GPG (pentru semnături F-Droid)

### Google Play

- Cont Google Play Developer ($25 one-time fee)
- Play Console API access configurat
- Service account JSON key
- Keystore pentru semnare

### F-Droid

- Cont F-Droid (gratuit)
- Cheie GPG pentru semnături
- Server pentru hosting APK-uri sau repository F-Droid

---

## 🏪 Google Play Store

### Step 1: Configurare inițială

#### 1.1 Creează Google Play Developer Account

1. Accesează [Google Play Console](https://play.google.com/console)
2. Plătește taxa de înregistrare ($25)
3. Completează profilul dezvoltatorului

#### 1.2 Configurează aplicația

1. **Create app** în Play Console
2. Completează detaliile:
   - **App name**: MOMCLAW - Offline AI Agent
   - **Default language**: English
   - **Free or paid**: Free
   - **Category**: Productivity
   - **Tags**: AI, Privacy, Offline, Assistant

3. Setup **App signing**:
   - Google Play App Signing (recomandat)
   - Upload keystore (opțional, dacă ai deja)

#### 1.3 Generează Service Account

```bash
# 1. Accesează Google Cloud Console
# https://console.cloud.google.com

# 2. Creează Service Account
# IAM & Admin > Service Accounts > Create Service Account

# 3. Adaugă roluri:
# - "Service Account User"
# - "Android Management API User"

# 4. Generează JSON key
# Actions > Create Key > JSON

# 5. Salvează ca google-play-service-account.json
# ATENȚIE: Nu commit-ui acest fișier!
```

### Step 2: Configurează Fastlane

Fastlane automatizează deployment-ul pe Google Play.

#### 2.1 Instalează Fastlane

```bash
# macOS/Linux
gem install fastlane

# sau cu Homebrew (macOS)
brew install fastlane

# Verifică instalarea
fastlane --version
```

#### 2.2 Setup Fastlane în proiect

```bash
cd MOMCLAW/android

# Inițializează Fastlane
fastlane init

# Selectează "Manual setup"
# Vei vedea structura:
# ├── fastlane/
# │   ├── Appfile
# │   └── Fastfile
# └── Gemfile
```

#### 2.3 Configurează Appfile

**android/fastlane/Appfile:**

```ruby
package_name("com.loa.MOMCLAW")

# Google Play Service Account
json_key_file("google-play-service-account.json")

# Versiune implicită
default_version_code(1)
```

#### 2.4 Configurează Fastfile

**android/fastlane/Fastfile:**

```ruby
# This file contains the fastlane.tools configuration
# You can find the documentation at https://docs.fastlane.tools

default_platform(:android)

platform :android do
  
  # Build și upload pe Google Play (Internal Testing)
  desc "Build and upload to Google Play Internal Testing"
  lane :internal do
    gradle(task: "clean bundleRelease")
    
    upload_to_play_store(
      track: 'internal',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      skip_upload_metadata: false,
      skip_upload_images: false,
      skip_upload_screenshots: false
    )
  end
  
  # Build și upload pe Google Play (Alpha)
  desc "Build and upload to Google Play Alpha"
  lane :alpha do
    gradle(task: "clean bundleRelease")
    
    upload_to_play_store(
      track: 'alpha',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      skip_upload_metadata: false
    )
  end
  
  # Build și upload pe Google Play (Beta)
  desc "Build and upload to Google Play Beta"
  lane :beta do
    gradle(task: "clean bundleRelease")
    
    upload_to_play_store(
      track: 'beta',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      skip_upload_metadata: false
    )
  end
  
  # Build și upload pe Google Play (Production)
  desc "Build and upload to Google Play Production"
  lane :production do
    gradle(task: "clean bundleRelease")
    
    upload_to_play_store(
      track: 'production',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      skip_upload_metadata: false
    )
  end
  
  # Promote from one track to another
  desc "Promote from Internal to Alpha"
  lane :promote_internal_to_alpha do
    promote_to_play_store(
      track: 'internal',
      track_promote_to: 'alpha'
    )
  end
  
  desc "Promote from Alpha to Beta"
  lane :promote_alpha_to_beta do
    promote_to_play_store(
      track: 'alpha',
      track_promote_to: 'beta'
    )
  end
  
  desc "Promote from Beta to Production"
  lane :promote_beta_to_production do
    promote_to_play_store(
      track: 'beta',
      track_promote_to: 'production'
    )
  end
  
  # Update metadata
  desc "Update store listing metadata"
  lane :update_metadata do
    upload_to_play_store(
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_images: false,
      skip_upload_screenshots: false
    )
  end
  
  # Lane pentru release complet
  desc "Complete release: build, test, upload to Internal"
  lane :release do
    # Run tests
    gradle(task: "testDebugUnitTest")
    
    # Run lint
    gradle(task: "lintDebug")
    
    # Build release
    gradle(task: "clean bundleRelease")
    
    # Upload to Internal Testing
    upload_to_play_store(
      track: 'internal',
      aab: 'app/build/outputs/bundle/release/app-release.aab'
    )
    
    # Notify
    puts "✅ Release complete! Check Google Play Console."
  end
  
end
```

### Step 3: Metadata și Assets

#### 3.1 Structura metadata

Fastlane folosește un folder pentru metadata:

```
android/fastlane/metadata/android/
├── en-US/
│   ├── title.txt (30 char max)
│   ├── short_description.txt (80 char max)
│   ├── full_description.txt (4000 char max)
│   ├── changelogs/
│   │   └── 1000000.txt (version code)
│   ├── images/
│   │   ├── featureGraphic.png (1024x500)
│   │   ├── icon.png (512x512)
│   │   ├── promoGraphic.png (180x120)
│   │   ├── phoneScreenshots/ (2-8 screenshots)
│   │   │   ├── 1.png (1080x1920 or 16:9)
│   │   │   ├── 2.png
│   │   │   └── 3.png
│   │   ├── sevenInchScreenshots/
│   │   └── tenInchScreenshots/
│   └── video.txt (YouTube URL)
└── ro/
    ├── title.txt
    ├── short_description.txt
    └── full_description.txt
```

#### 3.2 Conținut metadata

**title.txt:**
```
MOMCLAW - Offline AI Agent
```

**short_description.txt:**
```
AI assistant running 100% offline. Privacy-first, no cloud, no tracking.
```

**full_description.txt:**
```
MOMCLAW is a fully autonomous AI agent that runs entirely on your phone — zero cloud, zero tracking, 100% offline.

🧠 POWERFUL FEATURES:

• Conversații inteligente - Advanced reasoning with Gemma 3 E4B-it
• Tool calls native - Shell, file operations, web search
• Memorie persistentă - SQLite database with full conversation history
• Canale externe - Telegram and Discord integration (coming soon)
• OpenClaw Sync - Sync with main OpenClaw instance (coming soon)
• Privacy-first - All data stays on device

🔒 PRIVACY BY DESIGN:

• All model inferences happen directly on your device
• No internet required for core features
• Your prompts, images, and data never leave your phone
• Open source and auditable

⚡ PERFORMANCE:

• Optimized for mobile with LiteRT-LM
• GPU acceleration when available
• Dark theme with Material You design
• Efficient memory management

🎯 USE CASES:

• Personal assistant without privacy concerns
• Offline AI when no internet available
• Development and experimentation with local LLMs
• Learning about on-device AI

📱 REQUIREMENTS:

• Android 9.0 (API 28) or higher
• 4GB+ RAM recommended
• 3GB+ free storage for model

Open source: https://github.com/serverul/MOMCLAW
```

**changelogs/1000000.txt:**
```
Initial release of MOMCLAW!

Features:
• Chat UI with streaming responses
• Offline AI inference with Gemma 3 E4B-it
• Model management (download, load, switch)
• Settings customization
• Persistent conversation history
• Material You design with dark theme
```

### Step 4: Deployment Process

#### 4.1 Manual deployment

```bash
cd MOMCLAW/android

# Internal Testing
fastlane internal

# Alpha Testing
fastlane alpha

# Beta Testing
fastlane beta

# Production
fastlane production
```

#### 4.2 Via GitHub Actions (automat)

Release-urile sunt automatizate prin `.github/workflows/release.yml`:

```bash
# Creează un tag nou
git tag -a v1.0.1 -m "Release v1.0.1"
git push origin v1.0.1

# GitHub Actions va:
# 1. Build release AAB
# 2. Sign cu keystore
# 3. Upload pe GitHub Releases
# 4. (Opțional) Upload pe Google Play
```

#### 4.3 Checklist pre-deployment

- [ ] Version code incrementat în `build.gradle.kts`
- [ ] Version name actualizat
- [ ] CHANGELOG.md actualizat
- [ ] Metadata actualizată (screenshots, descriptions)
- [ ] Teste trecute (`./gradlew test`)
- [ ] Lint fără erori (`./gradlew lint`)
- [ ] APK testat pe device real
- [ ] Keystore backup făcut

---

## 🤖 F-Droid Deployment

F-Droid este un repository alternativ pentru aplicații Android open-source.

### Step 1: Pregătire

#### 1.1 Verifică cerințele

- ✅ Aplicația trebuie să fie FOSS (Free and Open Source Software)
- ✅ Fără dependențe proprietare
- ✅ Build reproductibil
- ✅ Cod sursă public

MOMCLAW îndeplinește toate cerințele:
- Apache 2.0 License
- Open source pe GitHub
- Fără servicii Google proprietare
- Build reproductibil

#### 1.2 Configurează build reproductibil

**android/gradle.properties:**

```properties
# Build reproducibility
android.enableJetifier=false
android.useAndroidX=true

# Fix timestamps
org.gradle.caching=true

# Force same Java version
org.gradle.java.home=/path/to/jdk17
```

### Step 2: Configurare F-Droid

#### 2.1 Creează cont F-Droid

1. Accesează [f-droid.org](https://f-droid.org)
2. Creează un cont (gratuit)
3. Configurează GPG key pentru semnături

#### 2.2 Generează GPG key

```bash
# Generează cheie nouă
gpg --full-generate-key
# Select: (1) RSA and RSA
# Key size: 4096
# Expiration: 2y
# Name: MOMCLAW Releases
# Email: releases@MOMCLAW.example.com

# Listează keys
gpg --list-secret-keys --keyid-format=long

# Export public key
gpg --armor --export YOUR_KEY_ID > MOMCLAW-public-key.asc

# Upload pe servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

#### 2.3 Configurează semnături APK

F-Droid verifică semnăturile APK. Trebuie să semnezi APK-urile cu cheia ta:

```bash
# Semnează APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore MOMCLAW-release-key.jks \
  -signedjar MOMCLAW-1.0.0-signed.apk \
  MOMCLAW-1.0.0-unsigned.apk \
  MOMCLAW

# Verifică semnătura
jarsigner -verify -verbose -certs MOMCLAW-1.0.0-signed.apk
```

### Step 3: Submit la F-Droid

#### 3.1 Opțiunea A: F-Droid Main Repository

Submit pentru includere în repository-ul oficial F-Droid:

1. **Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata)**
2. **Creează fișier metadata:**

**metadata/com.loa.MOMCLAW.yml:**

```yaml
Categories:
  - Productivity
  - Science & Education

License: Apache-2.0

AuthorName: LinuxOnAsteroids
AuthorEmail: MOMCLAW@example.com
AuthorWebSite: https://github.com/serverul

WebSite: https://github.com/serverul/MOMCLAW
SourceCode: https://github.com/serverul/MOMCLAW
IssueTracker: https://github.com/serverul/MOMCLAW/issues
Changelog: https://github.com/serverul/MOMCLAW/blob/main/CHANGELOG.md

Donate: https://github.com/sponsors/serverul

AutoName: MOMCLAW

RequiresRoot: false

RepoType: git
Repo: https://github.com/serverul/MOMCLAW.git

Builds:
  - versionName: '1.0.0'
    versionCode: 1000000
    commit: v1.0.0
    subdir: android
    gradle:
      - yes
    output: app/build/outputs/apk/release/app-release.apk
    prebuild: sed -i -e '/keystoreProperties/d' build.gradle.kts

MaintainerNotes: |
  First release. Model download happens in-app after installation.

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: '1.0.0'
CurrentVersionCode: 1000000
```

3. **Submit merge request:**
   ```bash
   git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
   cd fdroiddata
   git checkout -b add-MOMCLAW
   # Add metadata file
   git add metadata/com.loa.MOMCLAW.yml
   git commit -m "Add MOMCLAW"
   git push origin add-MOMCLAW
   # Create merge request on GitLab
   ```

4. **Review process:**
   - F-Droid maintainers vor verifica codul
   - Verifică license, build, security
   - Poate dura săptămâni/luni
   - Răspunde la feedback

#### 3.2 Opțiunea B: Self-hosted Repository

Mai rapid, dar utilizatorii trebuie să adauge repository-ul manual:

1. **Setup server:**
   ```bash
   # Pe server (VPS/Dedicated)
   mkdir -p /var/www/fdroid/repo
   cd /var/www/fdroid
   
   # Instalează fdroidserver
   pip install fdroidserver
   
   # Inițializează
   fdroid init
   ```

2. **Configurează repo:**
   
   **config.py:**
   ```python
   repo_url = "https://fdroid.MOMCLAW.example.com"
   repo_name = "MOMCLAW Repository"
   repo_description = "Official MOMCLAW releases"
   
   archive_older = 5
   repo_icon = "icons/MOMCLAW.png"
   
   # Signing
   keystore = "/path/to/keystore.jks"
   repo_keyalias = "MOMCLAW"
   keystorepass = "your_password"
   keypass = "your_password"
   ```

3. **Adaugă APK-uri:**
   ```bash
   # Copiază APK-uri
   cp MOMCLAW-1.0.0.apk repo/
   
   # Update index
   fdroid update
   ```

4. **Deploy:**
   ```bash
   # Servește cu nginx/apache
   # Sau upload pe S3/GitHub Pages
   ```

5. **Utilizatori adaugă repo:**
   - În F-Droid app: Settings → Repositories → Add
   - URL: `https://fdroid.MOMCLAW.example.com/repo?fingerprint=YOUR_FINGERPRINT`

### Step 4: Automation F-Droid

#### 4.1 Script de build pentru F-Droid

**scripts/build-fdroid.sh:**

```bash
#!/bin/bash
set -e

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 1.0.0"
  exit 1
fi

echo "🤖 Building MOMCLAW v$VERSION for F-Droid..."

# Clean
./android/gradlew clean

# Build unsigned APK
./android/gradlew assembleRelease \
  -PversionName=$VERSION \
  -PversionCode=$(echo $VERSION | tr -d '.')

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore MOMCLAW-release-key.jks \
  -signedjar MOMCLAW-$VERSION-fdroid.apk \
  android/app/build/outputs/apk/release/app-release-unsigned.apk \
  MOMCLAW

# Verify
jarsigner -verify -verbose -certs MOMCLAW-$VERSION-fdroid.apk

# GPG sign
gpg --armor --detach-sign MOMCLAW-$VERSION-fdroid.apk

echo "✅ F-Droid build complete: MOMCLAW-$VERSION-fdroid.apk"
echo "📄 Signature: MOMCLAW-$VERSION-fdroid.apk.asc"
```

#### 4.2 GitHub Actions pentru F-Droid

Adaugă în `.github/workflows/fdroid.yml`:

```yaml
name: F-Droid Build

on:
  push:
    tags:
      - 'v*-fdroid'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Build unsigned APK
      run: |
        chmod +x android/gradlew
        ./android/gradlew clean assembleRelease
    
    - name: Import GPG key
      env:
        GPG_PRIVATE_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
      run: |
        echo -e "$GPG_PRIVATE_KEY" | gpg --import --batch
    
    - name: Sign APK with GPG
      run: |
        VERSION=${GITHUB_REF#refs/tags/v}
        VERSION=${VERSION%-fdroid}
        
        mv android/app/build/outputs/apk/release/app-release-unsigned.apk \
           MOMCLAW-$VERSION-unsigned.apk
        
        gpg --armor --detach-sign MOMCLAW-$VERSION-unsigned.apk
    
    - name: Upload artifact
      uses: actions/upload-artifact@v4
      with:
        name: MOMCLAW-fdroid
        path: |
          MOMCLAW-*.apk
          MOMCLAW-*.apk.asc
```

---

## 🤖 Automation Script-uri

### Script-uri helper

Am creat următoarele script-uri în `scripts/`:

- `build-release.sh` - Build APK + AAB pentru release
- `validate-build.sh` - Validare build înainte de release
- `run-tests.sh` - Rulează toate testele
- `build-fdroid.sh` - Build specific pentru F-Droid

### Usage:

```bash
# Build release complet
./scripts/build-release.sh 1.0.1

# Validare înainte de release
./scripts/validate-build.sh

# Run tests
./scripts/run-tests.sh

# Build F-Droid
./scripts/build-fdroid.sh 1.0.1
```

---

## 🔧 Troubleshooting

### Google Play

#### Eroare: "Upload failed: Version code already used"
```bash
# Incrementează versionCode în app/build.gradle.kts
versionCode = 1000001  // nu 1000000
```

#### Eroare: "APK is not signed"
```bash
# Verifică keystore
keytool -list -v -keystore MOMCLAW-release-key.jks

# Resemnează
jarsigner -verify -verbose -certs app-release.apk
```

#### Eroare: "Privacy policy URL required"
```
Adaugă URL la privacy policy în Play Console:
Store presence → Store listing → Privacy policy
https://github.com/serverul/MOMCLAW/blob/main/PRIVACY_POLICY.md
```

### F-Droid

#### Eroare: "Build failed: Could not find dependency"
```
Verifică că toate dependențele sunt FOSS.
Elimină Google Play Services și alte librării proprietare.
```

#### Eroare: "Signature verification failed"
```bash
# Verifică GPG signature
gpg --verify MOMCLAW-1.0.0.apk.asc MOMCLAW-1.0.0.apk

# Dacă e invalid, resemnează
gpg --armor --detach-sign MOMCLAW-1.0.0.apk
```

#### Eroare: "Reproducible build failed"
```
Verifică:
- Java version fixă (JDK 17)
- Gradle version fixă (8.9)
- Timestamp-uri fixe
- Fără variabile de environment în build
```

---

## 📊 Deployment Checklist

### Pre-Release

- [ ] Version code incrementat
- [ ] Version name actualizat
- [ ] CHANGELOG.md actualizat
- [ ] Metadata actualizată (Google Play)
- [ ] Screenshots actualizate
- [ ] Teste trecute
- [ ] Lint fără erori
- [ ] APK testat pe device real
- [ ] Privacy policy URL valid

### Google Play Deployment

- [ ] Service account JSON configurat
- [ ] Keystore backup securizat
- [ ] Fastlane configurat
- [ ] Metadata în `fastlane/metadata/`
- [ ] Screenshots în format corect
- [ ] AAB build testat
- [ ] Upload pe Internal Testing
- [ ] Review intern
- [ ] Promote to Alpha/Beta/Production

### F-Droid Deployment

- [ ] GPG key generat și publicat
- [ ] Metadata YAML creat
- [ ] Build reproductibil verificat
- [ ] APK semnat cu GPG
- [ ] APK testat
- [ ] Submit la F-Droid (sau self-hosted)
- [ ] Merge request creat (dacă e cazul)

### Post-Release

- [ ] GitHub Release creat
- [ ] Website actualizat
- [ ] Social media anunțat
- [ ] Discord/Telegram notificat
- [ ] Issues închise/rezolvate
- [ ] Release notes publicate

---

## 🔗 Resurse

### Google Play

- [Play Console Help](https://support.google.com/googleplay/android-developer/)
- [Fastlane Docs](https://docs.fastlane.tools/)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [Play Developer API](https://developers.google.com/android-publisher)

### F-Droid

- [F-Droid Docs](https://f-droid.org/docs/)
- [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata)
- [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)
- [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)

---

**Ultima actualizare:** 2026-04-06
**Versiune document:** 1.0

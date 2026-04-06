# MomClaw Quick Start Guide

Get MomClaw up and running in 5 minutes.

---

## Prerequisites Check

```bash
# Check Java version (must be 17+)
java -version

# Check Android SDK
echo $ANDROID_HOME

# Check if Android NDK is installed
ls $ANDROID_HOME/ndk/
```

If any are missing, see [DOCUMENTATION.md](DOCUMENTATION.md#prerequisites).

---

## Quick Setup

### 1. Clone and Build (2 min)

```bash
# Clone
git clone https://github.com/serverul/momclaw.git
cd momclaw

# Make scripts executable
chmod +x scripts/*.sh android/gradlew

# Build debug APK
./scripts/ci-build.sh build:debug
```

### 2. Install on Device (1 min)

```bash
# Connect device (enable USB debugging first)
adb devices

# Install
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

### 3. Download Model (2 min)

```bash
# Download Gemma 3 E4B-it (~2.5GB)
./scripts/download-model.sh ./models

# Push to device
adb push models/gemma-3-E4B-it.litertlm \
    /sdcard/Android/data/com.loa.momclaw/files/models/
```

### 4. Run!

1. Open MomClaw on your device
2. Grant necessary permissions
3. Start chatting!

---

## Using Android Studio

1. **File → Open** → Select `momclaw/android`
2. Wait for Gradle sync
3. Select device/emulator
4. Click **Run** (▶️)
5. Download model via in-app downloader

---

## Common Commands

```bash
# Build
./scripts/ci-build.sh build:debug      # Debug APK
./scripts/ci-build.sh build:release 1.0.0  # Release APK + AAB

# Test
./scripts/ci-build.sh test:unit       # Unit tests
./scripts/ci-build.sh test:all        # All tests + lint

# Deploy
./scripts/ci-build.sh deploy:internal # Google Play Internal
./scripts/ci-build.sh deploy:github 1.0.0  # GitHub Release

# Utility
./scripts/ci-build.sh clean           # Clean build
./scripts/ci-build.sh help            # Show all commands
```

---

## Troubleshooting

### Build fails with "SDK not found"
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Device not detected
- Enable USB debugging in Developer Options
- Accept RSA key fingerprint on device
- Try different USB cable/port

### Model download fails
- Check internet connection
- Verify ~3GB free space
- Try manual download from [HuggingFace](https://huggingface.co/google/gemma-3-e4b-it)

### App crashes on launch
- Check logcat: `adb logcat | grep -i momclaw`
- Verify model is in correct location
- Ensure device has 4GB+ RAM

---

## Next Steps

- Read [DOCUMENTATION.md](DOCUMENTATION.md) for full docs
- See [DEVELOPMENT.md](DEVELOPMENT.md) for development guide
- Check [DEPLOYMENT.md](DEPLOYMENT.md) for store deployment
- Join [Discussions](https://github.com/serverul/momclaw/discussions) for help

---

**Questions?** Open an [Issue](https://github.com/serverul/momclaw/issues) or start a [Discussion](https://github.com/serverul/momclaw/discussions).

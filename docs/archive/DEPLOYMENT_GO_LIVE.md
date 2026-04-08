# MomClAW v1.0.0 — Deployment Go-Live Checklist

**Date**: 2026-04-07  
**Status**: Ready for deployment after completing this checklist

---

## Pre-Deployment Checks

### 1. Install Java 17 (5 min)

```bash
sudo apt-get update
sudo apt-get install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
java -version  # expect 17.x
```

### 2. Build & Test (20 min)

```bash
cd /home/userul/.openclaw/workspace/momclaw/android

# Clean build
./gradlew clean

# Debug APK
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Release APKs (requires key.properties)
./gradlew assembleRelease

# AAB for Play Store
./gradlew bundleRelease
```

### 3. Device Testing (30 min)

```bash
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Test checklist:
- [ ] App launches without crash
- [ ] Services start (InferenceService:8080, AgentService:9090)
- [ ] Chat works — send/receive
- [ ] Streaming responses visible
- [ ] Memory persists across restarts
- [ ] Offline mode works (airplane mode test)
- [ ] Settings screen functional
- [ ] Dark theme works
- [ ] No ANR (Application Not Responding)

### 4. Set GitHub Secrets (10 min)

Go to: `https://github.com/serverul/MOMCLAW/settings/secrets/actions`

| Secret | How to get it |
|--------|---------------|
| `KEYSTORE_BASE64` | `base64 -w 0 android/momclaw-release-key.jks` |
| `STORE_PASSWORD` | From `android/key.properties` |
| `KEY_PASSWORD` | From `android/key.properties` |
| `KEY_ALIAS` | `MOMCLAW` |

### 5. Create Store Assets (30 min)

Required for Play Store:
- Phone screenshots (2-8): `1080x1920` minimum
- 7" tablet screenshot (optional but recommended)
- 10" tablet screenshot (optional but recommended)
- Feature graphic: `1024x500px` PNG/JPEG

Capture screenshots:
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png momclaw-screenshot-01.png
```

### 6. Tag & Push Release (5 min)

```bash
cd /home/userul/.openclaw/workspace/momclaw
git tag -a v1.0.0 -m "Release v1.0.0 — Production ready"
git push origin v1.0.0
```

This triggers CI/CD which will:
- Build release APKs (signed)
- Run security scans
- Create GitHub Release with assets

### 7. Monitor CI (15 min)

Watch: `https://github.com/serverul/MOMCLAW/actions`

Expected:
- ✅ CI workflow passes
- ✅ Security scan passes
- ✅ Release APK built and uploaded
- ✅ GitHub release created at `https://github.com/serverul/MOMCLAW/releases/tag/v1.0.0`

---

## Post-Release

- [ ] Download release APK and install on device
- [ ] Verify all features work
- [ ] Monitor GitHub issues
- [ ] Plan F-Droid submission (if desired)
- [ ] Setup Play Console listing (if desired)

---

**Estimated total time**: ~1.5 hours

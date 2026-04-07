# MOMCLAW Troubleshooting Guide

Complete troubleshooting guide for common issues with MOMCLAW.

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## 📋 Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [Build Issues](#build-issues)
- [Installation Issues](#installation-issues)
- [Runtime Issues](#runtime-issues)
- [Performance Issues](#performance-issues)
- [Model Issues](#model-issues)
- [Memory & Storage Issues](#memory--storage-issues)
- [Network & Connectivity](#network--connectivity)
- [UI & Display Issues](#ui--display-issues)
- [Deployment Issues](#deployment-issues)
- [Getting Help](#getting-help)

---

## 🔍 Quick Diagnostics

### System Health Check

Run these commands to diagnose common issues:

```bash
# Check device compatibility
adb shell getprop ro.build.version.sdk  # Should be 28+ (Android 9+)
adb shell getprop ro.product.cpu.abi     # Should be arm64-v8a or armeabi-v7a

# Check available storage
adb shell df -h /sdcard

# Check available memory
adb shell cat /proc/meminfo | grep MemAvailable

# Check if app is installed
adb shell pm list packages | grep momclaw

# Check app logs
adb logcat -s MOMCLAW:* | tail -100
```

### Log Collection

**Collect detailed logs for bug reports**:

```bash
# Clear old logs
adb logcat -c

# Reproduce the issue, then:
adb logcat -d > momclaw_logs.txt

# For specific app logs:
adb logcat -s MOMCLAW:* LiteRT:* NullClaw:* > momclaw_app_logs.txt
```

---

## 🏗️ Build Issues

### Issue: SDK not found

**Symptoms**:
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable.
```

**Solution**:
```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Add to ~/.bashrc or ~/.zshrc for persistence
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools' >> ~/.bashrc
```

### Issue: Java version mismatch

**Symptoms**:
```
Unsupported Java version
```

**Solution**:
```bash
# Check Java version (must be 17+)
java -version

# Install JDK 17
# Ubuntu/Debian:
sudo apt install openjdk-17-jdk

# macOS:
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Issue: Gradle build fails with memory error

**Symptoms**:
```
Expiring Daemon because JVM heap space is exhausted
```

**Solution**:
```bash
# Increase Gradle memory in gradle.properties
echo "org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m" >> gradle.properties

# Or use daemon-free build
./android/gradlew assembleDebug --no-daemon
```

### Issue: NDK not found

**Symptoms**:
```
No version of NDK matched the requested version
```

**Solution**:
```bash
# Install NDK via Android Studio SDK Manager
# Or via command line:
sdkmanager "ndk;25.2.9519653"

# Set in local.properties
echo "ndk.dir=$ANDROID_HOME/ndk/25.2.9519653" > local.properties
```

### Issue: CMake configuration fails

**Symptoms**:
```
CMake Error: CMake was unable to find a build program corresponding to "Ninja"
```

**Solution**:
```bash
# Install CMake and Ninja
# Ubuntu/Debian:
sudo apt install cmake ninja-build

# macOS:
brew install cmake ninja

# Or use Android Studio SDK Manager to install CMake
```

---

## 📦 Installation Issues

### Issue: App won't install - "App not installed"

**Symptoms**:
- Installation fails with "App not installed" error

**Solutions**:

1. **Enable Unknown Sources**:
   ```
   Settings → Security → Unknown sources → Enable
   ```

2. **Uninstall previous version**:
   ```bash
   adb uninstall com.loa.MOMCLAW
   adb install app-release.apk
   ```

3. **Check architecture compatibility**:
   ```bash
   # Check device architecture
   adb shell getprop ro.product.cpu.abi
   
   # Use matching APK:
   # arm64-v8a → Most modern devices
   # armeabi-v7a → Older 32-bit devices
   ```

4. **Verify APK signature**:
   ```bash
   # Check if APK is signed
   jarsigner -verify -verbose app-release.apk
   ```

### Issue: "Parse error" when installing

**Symptoms**:
- "There was a problem parsing the package"

**Solutions**:

1. **Download APK again** - File may be corrupted
2. **Check Android version** - Must be 9.0 (API 28) or higher
3. **Check APK integrity**:
   ```bash
   # Verify APK is not corrupted
   sha256sum app-release.apk
   ```

### Issue: "Installation failed" due to existing data

**Symptoms**:
```
INSTALL_FAILED_ALREADY_EXISTS
```

**Solution**:
```bash
# Uninstall existing app
adb uninstall com.loa.MOMCLAW

# Or use -r flag to replace
adb install -r app-release.apk
```

---

## ⚙️ Runtime Issues

### Issue: App crashes on launch

**Symptoms**:
- App closes immediately after opening
- Black screen then crash

**Diagnostic Steps**:
```bash
# Check crash logs
adb logcat -s AndroidRuntime:E | grep -A 10 MOMCLAW
```

**Solutions**:

1. **Clear app cache**:
   ```bash
   adb shell pm clear com.loa.MOMCLAW
   ```

2. **Check device compatibility**:
   - Android 9.0+ required
   - ARM64 or ARMv7 processor
   - 4GB+ RAM recommended

3. **Reinstall app**:
   ```bash
   adb uninstall com.loa.MOMCLAW
   adb install app-release.apk
   ```

4. **Check for missing permissions**:
   ```bash
   adb shell dumpsys package com.loa.MOMCLAW | grep permission
   ```

### Issue: App freezes or becomes unresponsive

**Symptoms**:
- App stops responding
- ANR (Application Not Responding) dialog

**Solutions**:

1. **Check memory usage**:
   ```bash
   adb shell dumpsys meminfo com.loa.MOMCLAW
   ```

2. **Close background apps** - Free up RAM

3. **Reduce model parameters**:
   - Lower max_tokens in settings
   - Reduce temperature

4. **Restart app**:
   ```bash
   adb shell am force-stop com.loa.MOMCLAW
   adb shell am start -n com.loa.MOMCLAW/.MainActivity
   ```

### Issue: Permissions denied

**Symptoms**:
- "Permission denied" errors
- Features not working

**Solutions**:

1. **Grant runtime permissions**:
   ```bash
   # Storage permission
   adb shell pm grant com.loa.MOMCLAW android.permission.READ_EXTERNAL_STORAGE
   adb shell pm grant com.loa.MOMCLAW android.permission.WRITE_EXTERNAL_STORAGE
   
   # For Android 13+ use:
   adb shell pm grant com.loa.MOMCLAW android.permission.READ_MEDIA_IMAGES
   ```

2. **Grant all permissions**:
   ```bash
   adb shell pm grant com.loa.MOMCLAW android.permission.READ_EXTERNAL_STORAGE
   adb shell pm grant com.loa.MOMCLAW android.permission.WRITE_EXTERNAL_STORAGE
   adb shell pm grant com.loa.MOMCLAW android.permission.INTERNET
   ```

---

## 🚀 Performance Issues

### Issue: Slow model loading

**Symptoms**:
- Model takes >60 seconds to load
- "Loading model..." stuck for long time

**Solutions**:

1. **Check available RAM**:
   ```bash
   adb shell cat /proc/meminfo | grep MemAvailable
   # Should have 2GB+ free
   ```

2. **Close background apps** - Free memory

3. **Check storage speed**:
   ```bash
   # Model on slow SD card? Move to internal storage
   adb shell ls -la /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

4. **Restart device** - Clear system memory

5. **Check for thermal throttling**:
   - Device may be hot
   - Let it cool down

### Issue: Slow response generation

**Symptoms**:
- Long wait times between messages
- Streaming very slow (<5 tokens/sec)

**Solutions**:

1. **Check CPU usage**:
   ```bash
   adb shell top | grep momclaw
   ```

2. **Reduce response length**:
   - Lower max_tokens (try 1024 instead of 2048)

3. **Check for background processes**:
   ```bash
   adb shell ps | grep momclaw
   ```

4. **Device optimization**:
   - Close unnecessary apps
   - Disable battery optimization for MOMCLAW
   - Restart device

5. **Check device specs**:
   - Low-end devices will be slower
   - Expected: 10-20 tokens/sec on mid-range
   - High-end: 20-30+ tokens/sec

### Issue: High battery usage

**Symptoms**:
- Battery drains quickly when using app

**Solutions**:

1. **Unload model when not in use**:
   - Settings → Models → Unload Model

2. **Reduce generation parameters**:
   - Lower max_tokens
   - Lower temperature (less computation)

3. **Check for background service**:
   - Stop agent service when not needed

4. **Use dark theme** - Saves battery on OLED screens

---

## 🧠 Model Issues

### Issue: "Model file not found"

**Symptoms**:
```
Error: Model file not found at /sdcard/Android/data/com.loa.MOMCLAW/files/models/
```

**Solutions**:

1. **Check if model exists**:
   ```bash
   adb shell ls -la /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

2. **Download model**:
   ```bash
   # On host machine
   ./scripts/download-model.sh ./models
   
   # Push to device
   adb push models/gemma-3-E4B-it.litertlm \
       /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

3. **Verify model integrity**:
   ```bash
   adb shell sha256sum /sdcard/Android/data/com.loa.MOMCLAW/files/models/gemma-3-E4B-it.litertlm
   ```

### Issue: "Failed to load model"

**Symptoms**:
- "Failed to initialize model" error
- Model loading never completes

**Solutions**:

1. **Check available memory**:
   ```bash
   adb shell cat /proc/meminfo | grep MemAvailable
   # Need 3GB+ for model loading
   ```

2. **Verify model file**:
   ```bash
   # Check file size (should be ~2.5GB)
   adb shell ls -lh /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

3. **Re-download model**:
   ```bash
   # Delete corrupted file
   adb shell rm /sdcard/Android/data/com.loa.MOMCLAW/files/models/gemma-3-E4B-it.litertlm
   
   # Download again
   ./scripts/download-model.sh ./models
   adb push models/gemma-3-E4B-it.litertlm \
       /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

4. **Check device compatibility**:
   - ARM64 or ARMv7 required
   - Android 9.0+ required

5. **Free RAM**:
   ```bash
   # Restart device to clear memory
   adb reboot
   ```

### Issue: Model download fails

**Symptoms**:
- Download stuck or fails
- "Download failed" error

**Solutions**:

1. **Check internet connection**:
   ```bash
   ping huggingface.co
   ```

2. **Check available storage**:
   ```bash
   # Need 3GB+ free
   adb shell df -h /sdcard
   ```

3. **Manual download**:
   ```bash
   # Download manually
   wget https://huggingface.co/litert-community/gemma-3-E4B-it-litertlm/resolve/main/gemma-3-E4B-it.litertlm
   
   # Push to device
   adb push gemma-3-E4B-it.litertlm \
       /sdcard/Android/data/com.loa.MOMCLAW/files/models/
   ```

4. **Use different network**:
   - Try different Wi-Fi network
   - Avoid metered connections

---

## 💾 Memory & Storage Issues

### Issue: "Out of memory" error

**Symptoms**:
- App crashes with OOM error
- "Insufficient memory" message

**Solutions**:

1. **Unload model**:
   - Settings → Models → Unload Model

2. **Clear conversation history**:
   - Settings → Memory → Clear Conversations

3. **Reduce context size**:
   - Lower max_tokens in settings

4. **Restart device** - Clear system memory

5. **Check memory usage**:
   ```bash
   adb shell dumpsys meminfo com.loa.MOMCLAW
   ```

### Issue: "Storage full" error

**Symptoms**:
- Can't save conversations
- Download fails

**Solutions**:

1. **Check available storage**:
   ```bash
   adb shell df -h /sdcard
   ```

2. **Delete old models**:
   - Models → Long-press old model → Delete

3. **Export and clear conversations**:
   - History → Select conversation → Export
   - Settings → Memory → Clear Conversations

4. **Move to SD card** (if available):
   - Settings → Storage → Change to SD card

5. **Clear app cache**:
   ```bash
   adb shell pm clear com.loa.MOMCLAW
   # Note: This deletes all app data
   ```

### Issue: Database error

**Symptoms**:
- "Database error" message
- Conversations not saving

**Solutions**:

1. **Clear app data**:
   ```bash
   adb shell pm clear com.loa.MOMCLAW
   ```

2. **Check storage permissions**:
   ```bash
   adb shell pm grant com.loa.MOMCLAW android.permission.WRITE_EXTERNAL_STORAGE
   ```

3. **Reinstall app**:
   ```bash
   adb uninstall com.loa.MOMCLAW
   adb install app-release.apk
   ```

---

## 🌐 Network & Connectivity

### Issue: Web search not working

**Symptoms**:
- "Web search failed" error
- Tool returns no results

**Solutions**:

1. **Check internet permission**:
   ```bash
   adb shell pm grant com.loa.MOMCLAW android.permission.INTERNET
   ```

2. **Check network connection**:
   ```bash
   adb shell ping -c 3 google.com
   ```

3. **Disable airplane mode**:
   - Settings → Network → Airplane mode → OFF

4. **Check if tool is enabled**:
   - Settings → Tools → Web search → Enable

### Issue: Offline mode issues

**Symptoms**:
- Features not working offline
- Error messages about connectivity

**Note**: Most features work offline except:
- Initial model download
- Web search tool
- External channel integrations (Telegram/Discord)

**Solution**: Ensure model is downloaded before going offline

---

## 🎨 UI & Display Issues

### Issue: UI elements not displaying correctly

**Symptoms**:
- Text cut off
- Layout broken
- Buttons not visible

**Solutions**:

1. **Check font size settings**:
   - Settings → Display → Font size → Normal

2. **Check display size**:
   - Settings → Display → Display size → Default

3. **Try different theme**:
   - Settings → Appearance → Theme → Light/Dark

4. **Clear app cache**:
   ```bash
   adb shell pm clear com.loa.MOMCLAW
   ```

### Issue: Dark theme not working

**Symptoms**:
- Dark theme doesn't apply
- Mixed light/dark elements

**Solutions**:

1. **Check Android version**:
   - Dark theme requires Android 10+ for system integration

2. **Force dark mode**:
   - Settings → Appearance → Theme → Dark

3. **Restart app**:
   ```bash
   adb shell am force-stop com.loa.MOMCLAW
   ```

### Issue: Split screen issues

**Symptoms**:
- UI breaks in split screen
- App crashes in multi-window

**Solutions**:

1. **Use full screen mode** - Split screen not fully tested

2. **Report issue** with device model and Android version

---

## 🚀 Deployment Issues

### Issue: Google Play upload fails

**Symptoms**:
- "Upload failed" in Play Console
- AAB rejected

**Solutions**:

1. **Check AAB size**:
   ```bash
   ls -lh android/app/build/outputs/bundle/release/app-release.aab
   # Should be <150MB
   ```

2. **Verify AAB format**:
   ```bash
   bundletool build-apks --bundle=app-release.aab --output=test.apks --mode=universal
   ```

3. **Check signing**:
   ```bash
   jarsigner -verify -verbose app-release.aab
   ```

4. **Review Play Console errors** - Console shows specific rejection reasons

### Issue: F-Droid build fails

**Symptoms**:
- F-Droid scanner rejects APK
- Build fails in fdroiddata CI

**Solutions**:

1. **Check for proprietary dependencies**:
   - All dependencies must be FOSS
   - No Google Play Services

2. **Verify build reproducibility**:
   ```bash
   # Build twice, compare
   ./scripts/build-fdroid.sh 1.0.0
   sha256sum MOMCLAW-1.0.0-fdroid.apk
   # Clean and rebuild
   ./scripts/ci-build.sh clean
   ./scripts/build-fdroid.sh 1.0.0
   sha256sum MOMCLAW-1.0.0-fdroid.apk
   # Should match
   ```

3. **Check metadata**:
   - Verify `metadata/com.loa.MOMCLAW.yml` is correct

### Issue: GitHub release workflow fails

**Symptoms**:
- GitHub Actions fails
- Release not created

**Solutions**:

1. **Check secrets**:
   - Verify `KEYSTORE_BASE64` exists
   - Verify `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS` exist

2. **Check workflow logs**:
   - Go to Actions tab in GitHub
   - Click failed workflow
   - Review error messages

3. **Manual release**:
   ```bash
   # Build locally
   ./scripts/ci-build.sh build:release 1.0.0
   
   # Create GitHub release manually
   # Upload APK and AAB
   ```

---

## 🆘 Getting Help

### Before Asking for Help

1. **Check this guide** - Most common issues are covered
2. **Search existing issues** - [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)
3. **Collect diagnostic info**:
   ```bash
   # Device info
   adb shell getprop ro.build.version.release
   adb shell getprop ro.product.model
   adb shell getprop ro.product.cpu.abi
   
   # App version
   adb shell dumpsys package com.loa.MOMCLAW | grep versionName
   
   # Logs
   adb logcat -d > momclaw_logs.txt
   ```

### Reporting Issues

**Where**: [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)

**Template**:
```markdown
**Description**
[Clear description of the issue]

**Environment**
- Device: [e.g., Pixel 6]
- Android version: [e.g., 14]
- MOMCLAW version: [e.g., 1.0.0]
- Architecture: [e.g., arm64-v8a]

**Steps to Reproduce**
1. [First step]
2. [Second step]
3. [Issue occurs]

**Expected Behavior**
[What should happen]

**Actual Behavior**
[What actually happens]

**Logs**
```
[Paste relevant logs here]
```

**Screenshots**
[If applicable]
```

### Support Channels

| Channel | Purpose | Response Time |
|---------|---------|---------------|
| [GitHub Issues](https://github.com/serverul/MOMCLAW/issues) | Bug reports, feature requests | 1-3 days |
| [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions) | Questions, general discussion | 1-2 days |
| Email: support@momclaw.app | Private inquiries | 2-5 days |

### Community Resources

- **Documentation**: [DOCUMENTATION.md](DOCUMENTATION.md)
- **User Guide**: [USER_GUIDE.md](USER_GUIDE.md)
- **FAQ**: [USER_GUIDE.md#faq](USER_GUIDE.md#faq)

---

## 📝 Debug Checklist

When troubleshooting, go through this checklist:

- [ ] **Device compatibility**: Android 9.0+, ARM64/ARMv7
- [ ] **Storage**: 3GB+ free space
- [ ] **Memory**: 2GB+ available RAM
- [ ] **Model**: Downloaded and verified
- [ ] **Permissions**: All required permissions granted
- [ ] **Logs**: Collected and reviewed
- [ ] **Searched**: Existing issues checked
- [ ] **Updated**: Using latest version

---

## 🔧 Quick Fix Commands

```bash
# Grant all permissions
adb shell pm grant com.loa.MOMCLAW android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.loa.MOMCLAW android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant com.loa.MOMCLAW android.permission.INTERNET

# Clear app data
adb shell pm clear com.loa.MOMCLAW

# Restart app
adb shell am force-stop com.loa.MOMCLAW
adb shell am start -n com.loa.MOMCLAW/.MainActivity

# Check logs
adb logcat -c && adb logcat -s MOMCLAW:*

# Reinstall app
adb uninstall com.loa.MOMCLAW
adb install app-release.apk

# Check storage
adb shell df -h /sdcard

# Check memory
adb shell cat /proc/meminfo | grep MemAvailable

# Device info
adb shell getprop ro.build.version.sdk
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.product.model
```

---

**Last Updated**: 2026-04-06  
**Version**: 1.0.0

**Need more help?** Check [USER_GUIDE.md](USER_GUIDE.md) or [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions)

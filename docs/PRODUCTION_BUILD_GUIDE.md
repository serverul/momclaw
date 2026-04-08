# MOMCLAW Production Build Guide

**Complete guide for production builds and deployment**  
**Version:** 1.0.0  
**Last Updated:** 2026-04-06

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Production Build Setup](#production-build-setup)
- [Signing Configuration](#signing-configuration)
- [Build Commands](#build-commands)
- [Release Checklist](#release-checklist)
- [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisites

### Required Software

```bash
# Check versions
java -version              # Must be JDK 17+
./android/gradlew --version  # Must be Gradle 8.9+
adb version                # Android Debug Bridge

# Android SDK components
sdkmanager --list | grep -E "build-tools|platforms|ndk"
```

### Environment Setup

```bash
# Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Add to PATH
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

---

## 🏗️ Production Build Setup

### Step 1: Configure Signing

Generate keystore (first time only):

```bash
# Generate production keystore
keytool -genkeypair \
  -alias MOMCLAW \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -keystore MOMCLAW-release-key.jks \
  -storetype PKCS12 \
  -dname "CN=MOMCLAW, OU=Development, O=LinuxOnAsteroids, L=Bucharest, ST=Bucharest, C=RO"

# Output:
# MOMCLAW-release-key.jks
```

**Important:** 
- Store keystore password securely
- Backup keystore to multiple secure locations
- Never commit keystore to git

Create signing configuration:

```bash
# Create key.properties (DO NOT COMMIT!)
cat > android/key.properties << 'EOF'
storePassword=YOUR_SECURE_PASSWORD
keyPassword=YOUR_SECURE_PASSWORD
keyAlias=MOMCLAW
storeFile=../MOMCLAW-release-key.jks
EOF

# Ensure it's ignored
echo "key.properties" >> android/.gitignore
```

### Step 2: Verify Build Configuration

Check `android/gradle.properties`:

```properties
# Production optimizations
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Android optimizations
android.enableR8.fullMode=true
android.nonTransitiveRClass=true

# Kotlin optimizations
kotlin.incremental=true
kotlin.caching.enabled=true
```

### Step 3: Configure ProGuard

Verify ProGuard rules in `android/app/proguard-rules.pro`:

```proguard
# Production optimizations
-optimizationpasses 7
-allowaccessmodification
-repackageclasses 'a'

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Keep crash stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
```

---

## 🔐 Signing Configuration

### GitHub Secrets (for CI/CD)

Configure these secrets in your repository:

```bash
# Go to: https://github.com/serverul/MOMCLAW/settings/secrets/actions

# Required secrets:
KEYSTORE_BASE64         # Base64 encoded keystore file
STORE_PASSWORD          # Keystore password
KEY_PASSWORD            # Key password
KEY_ALIAS               # Key alias (MOMCLAW)

# Encode keystore for GitHub
base64 -w 0 MOMCLAW-release-key.jks > keystore_base64.txt
# Copy content to KEYSTORE_BASE64 secret
```

### Local Build Signing

The build system will automatically use `key.properties` if it exists:

```bash
# Signing config is in android/app/build.gradle.kts
signingConfigs {
    create("release") {
        if (keyPropertiesFile.exists()) {
            storeFile = file(keyProperties["storeFile"] as String)
            storePassword = keyProperties["storePassword"] as String
            keyAlias = keyProperties["keyAlias"] as String
            keyPassword = keyProperties["keyPassword"] as String
        }
    }
}
```

---

## 🚀 Build Commands

### Debug Build

```bash
# Quick debug build (no signing required)
./android/gradlew assembleDebug

# Output: android/app/build/outputs/apk/debug/app-debug.apk
# Size: ~150MB (larger due to debug symbols)
```

### Release Build (APK)

```bash
# Build signed release APK
./android/gradlew assembleRelease

# Output: android/app/build/outputs/apk/release/app-release.apk
# Size: ~80MB (optimized with ProGuard)
```

### Release Build (AAB - Google Play)

```bash
# Build Android App Bundle
./android/gradlew bundleRelease

# Output: android/app/build/outputs/bundle/release/app-release.aab
# Size: ~70MB (smaller than APK)
```

### APK Splits (Optimized)

Build optimized APKs per architecture:

```bash
# Build split APKs
./android/gradlew assembleRelease

# Outputs:
# app-arm64-v8a-release.apk      (~60MB) - Most modern devices
# app-armeabi-v7a-release.apk    (~55MB) - Older 32-bit devices
# app-x86_64-release.apk         (~65MB) - Emulators
# app-universal-release.apk      (~90MB) - All devices
```

### Build All Variants

```bash
# Build everything
./android/gradlew assembleDebug assembleRelease bundleRelease

# Or use the main build script
./scripts/ci-build.sh build:release 1.0.0
```

---

## ✅ Release Checklist

### Pre-Build Checklist

- [ ] Update version in `android/app/build.gradle.kts`
  ```kotlin
  versionCode = 1000000  // Increment for each release
  versionName = "1.0.0"
  ```

- [ ] Update `CHANGELOG.md` with release notes

- [ ] Update Fastlane changelog
  ```bash
  # Create: android/fastlane/metadata/android/en-US/changelogs/1000000.txt
  ```

- [ ] Verify all tests pass
  ```bash
  ./android/gradlew testDebugUnitTest
  ./android/gradlew connectedAndroidTest  # Requires device
  ```

- [ ] Run lint checks
  ```bash
  ./android/gradlew lintDebug
  ./android/gradlew detekt
  ```

- [ ] Verify ProGuard rules
  ```bash
  # Test release build with ProGuard
  ./android/gradlew assembleRelease
  # Check for warnings in build output
  ```

### Build Checklist

- [ ] Clean previous builds
  ```bash
  ./android/gradlew clean
  ```

- [ ] Build release APK
  ```bash
  ./android/gradlew assembleRelease
  ```

- [ ] Verify APK signature
  ```bash
  jarsigner -verify -verbose android/app/build/outputs/apk/release/app-release.apk
  ```

- [ ] Check APK size
  ```bash
  ls -lh android/app/build/outputs/apk/release/
  # Should be <100MB for universal APK
  ```

- [ ] Test APK on device
  ```bash
  adb install -r android/app/build/outputs/apk/release/app-release.apk
  # Test all major features
  ```

- [ ] Build AAB for Google Play
  ```bash
  ./android/gradlew bundleRelease
  ```

### Post-Build Checklist

- [ ] Create git tag
  ```bash
  git tag -a v1.0.0 -m "Release v1.0.0"
  git push origin v1.0.0
  ```

- [ ] Create GitHub release
  ```bash
  gh release create v1.0.0 \
    android/app/build/outputs/apk/release/app-release.apk \
    android/app/build/outputs/bundle/release/app-release.aab \
    --title "MOMCLAW v1.0.0" \
    --notes-file CHANGELOG.md
  ```

- [ ] Upload to Google Play (if configured)
  ```bash
  ./scripts/ci-build.sh deploy:internal
  ```

- [ ] Backup build artifacts
  ```bash
  cp android/app/build/outputs/apk/release/app-release.apk \
     backups/releases/MOMCLAW-1.0.0.apk
  ```

---

## 🐛 Troubleshooting

### Build Fails: "Keystore not found"

**Error:**
```
Keystore file not found for signing config 'release'
```

**Solution:**
```bash
# Create key.properties
cat > android/key.properties << EOF
storePassword=YOUR_PASSWORD
keyPassword=YOUR_PASSWORD
keyAlias=MOMCLAW
storeFile=../MOMCLAW-release-key.jks
EOF

# Verify keystore exists
ls -la MOMCLAW-release-key.jks
```

### Build Fails: "ProGuard errors"

**Error:**
```
ProGuard: Warning: can't find referenced class...
```

**Solution:**
```bash
# Add keep rules to android/app/proguard-rules.pro
-keep class com.example.ProblematicClass { *; }

# Or disable obfuscation temporarily for debugging
# In build.gradle.kts:
isMinifyEnabled = false  // Temporary
```

### Build Fails: "Out of memory"

**Error:**
```
Expiring Daemon because JVM heap space is exhausted
```

**Solution:**
```bash
# Increase Gradle memory in gradle.properties
echo "org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g" >> android/gradle.properties

# Or build without daemon
./android/gradlew assembleRelease --no-daemon
```

### APK Signature Verification Fails

**Error:**
```
jarsigner: unable to sign jar: java.lang.SecurityException
```

**Solution:**
```bash
# Verify keystore
keytool -list -keystore MOMCLAW-release-key.jks

# Check key.properties has correct passwords
cat android/key.properties

# Clean and rebuild
./android/gradlew clean assembleRelease
```

### Build Too Slow

**Problem:** Build takes >10 minutes

**Solution:**
```bash
# Enable parallel builds
echo "org.gradle.parallel=true" >> android/gradle.properties

# Enable build caching
echo "org.gradle.caching=true" >> android/gradle.properties

# Enable configuration caching
echo "org.gradle.configuration-cache=true" >> android/gradle.properties

# Use faster JVM
echo "org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC" >> android/gradle.properties
```

### Release APK Won't Install

**Error:**
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package has different signature]
```

**Solution:**
```bash
# Uninstall existing app
adb uninstall com.loa.MOMCLAW

# Install new release
adb install android/app/build/outputs/apk/release/app-release.apk
```

---

## 📊 Build Size Optimization

### Check APK Size Breakdown

```bash
# Analyze APK
./android/gradlew assembleRelease
cd android/app/build/outputs/apk/release/

# Use Android Studio APK Analyzer
# Or use apkanalyzer from command line:
apkanalyzer apk summary app-release.apk
apkanalyzer apk file-size app-release.apk
apkanalyzer dex list app-release.apk
```

### Reduce APK Size

1. **Enable shrinking:**
   ```kotlin
   // In build.gradle.kts
   isMinifyEnabled = true
   isShrinkResources = true
   ```

2. **Use APK splits:**
   ```kotlin
   splits {
       abi {
           isEnable = true
           reset()
           include("arm64-v8a", "armeabi-v7a")
           isUniversalApk = false  // Skip universal APK
       }
   }
   ```

3. **Remove unused resources:**
   ```bash
   ./android/gradlew lint
   # Check for unused resources in report
   ```

4. **Optimize images:**
   ```bash
   # Use WebP instead of PNG
   # Compress large images
   ```

---

## 🔒 Security Best Practices

### Keystore Security

1. **Never commit keystore:**
   ```bash
   # Add to .gitignore
   echo "*.jks" >> .gitignore
   echo "key.properties" >> android/.gitignore
   ```

2. **Backup keystore:**
   ```bash
   # Encrypt and backup
   gpg --symmetric --cipher-algo AES256 MOMCLAW-release-key.jks
   # Store encrypted file in secure location
   ```

3. **Use strong passwords:**
   ```bash
   # Generate strong password
   openssl rand -base64 32
   ```

### GitHub Secrets Security

1. **Limit secret access:**
   - Only required secrets for CI/CD
   - Use repository secrets, not organization

2. **Rotate secrets periodically:**
   - Change passwords every 6 months
   - Update GitHub secrets accordingly

3. **Audit secret usage:**
   ```bash
   # Check which workflows use secrets
   grep -r "secrets\." .github/workflows/
   ```

---

## 📈 Performance Benchmarks

### Expected Build Times

| Build Type | Clean Build | Incremental |
|------------|-------------|-------------|
| Debug APK  | 3-5 min     | 30-60 sec   |
| Release APK | 5-8 min    | 1-2 min     |
| Release AAB | 6-10 min   | 1-3 min     |

### Expected APK Sizes

| APK Type | Size (with model) | Size (without model) |
|----------|-------------------|----------------------|
| Debug | ~150MB | ~40MB |
| Release (universal) | ~90MB | ~25MB |
| Release (arm64-v8a) | ~60MB | ~18MB |
| Release (armeabi-v7a) | ~55MB | ~16MB |

---

## 🚀 Automated Builds

### Use Main Build Script

```bash
# All-in-one build script
./scripts/ci-build.sh COMMAND [ARGS]

# Available commands:
./scripts/ci-build.sh build:debug
./scripts/ci-build.sh build:release 1.0.0
./scripts/ci-build.sh deploy:internal
./scripts/ci-build.sh deploy:github 1.0.0
./scripts/ci-build.sh validate
```

### GitHub Actions CI/CD

The project includes automated workflows:

- **`.github/workflows/ci.yml`** - Runs on every push
- **`.github/workflows/release.yml`** - Runs on tag push (v*)
- **`.github/workflows/security.yml`** - Daily security scans

See `.github/WORKFLOWS_GUIDE.md` for details.

---

## 📚 Additional Resources

- [BUILD_CONFIGURATION.md](BUILD_CONFIGURATION.md) - Detailed build configuration
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guides
- [PRODUCTION-CHECKLIST.md](PRODUCTION-CHECKLIST.md) - Complete release checklist
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions

---

**Last Updated:** 2026-04-06  
**Maintained by:** MOMCLAW Team

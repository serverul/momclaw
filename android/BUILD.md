# MomClaw Android - Build Instructions

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/serverul/MOMCLAW.git
   cd MOMCLAW/android
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `android` directory
   - Click "OK"

3. **Sync Gradle**
   - Android Studio will prompt to sync Gradle
   - Click "Sync Now"

4. **Build and Run**
   - Select a device or emulator (API 26+)
   - Click the Run button or press `Shift+F10`

## Prerequisites

### Required
- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: 17 (included with Android Studio)
- **Android SDK**: API 34 (Android 14)
- **Gradle**: 8.4 (wrapper included)

### Optional
- **Physical Device**: ARM64 Android device (API 26+)
- **NullClaw Binary**: Compiled for `aarch64-linux-android`

## Build Variants

### Debug Build
```bash
./gradlew assembleDebug
```
- Includes debugging information
- No code optimization
- Larger APK size
- Faster build times

### Release Build
```bash
./gradlew assembleRelease
```
- ProGuard/R8 code shrinking
- Full optimization
- Smaller APK size
- Slower build times

## Module Dependencies

### Building Individual Modules
```bash
# Build app module
./gradlew :app:build

# Build bridge module
./gradlew :bridge:build

# Build agent module
./gradlew :agent:build
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
# Requires connected device or emulator
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## Generating Signed APK

1. **Create keystore**
   ```bash
   keytool -genkey -v -keystore momclaw-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias momclaw
   ```

2. **Configure signing**
   Create `app/keystore.properties`:
   ```properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=momclaw
   storeFile=../momclaw-release.jks
   ```

3. **Build release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Locate APK**
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

## Preparing NullClaw Binary

The NullClaw binary must be compiled separately:

1. **Clone NullClaw**
   ```bash
   cd ../native
   git submodule add https://github.com/nullclaw/nullclaw.git
   cd nullclaw
   ```

2. **Cross-compile for Android**
   ```bash
   zig build -Dtarget=aarch64-linux-android -Doptimize=ReleaseSmall
   ```

3. **Copy to assets**
   ```bash
   cp zig-out/bin/nullclaw ../../android/agent/src/main/assets/
   ```

## Downloading Gemma Model

1. **Manual download**
   ```bash
   cd ../models
   bash download-model.sh
   ```

2. **Push to device**
   ```bash
   adb push gemma-4-E4B-it-litertlm.litertlm /sdcard/MOMCLAW/models/
   ```

3. **Move to app storage**
   - In the app, go to Settings → Models
   - Select "Import Model"
   - Navigate to `/sdcard/MOMCLAW/models/`

## Troubleshooting

### Build Issues

**Gradle sync failed**
```bash
# Clear Gradle cache
./gradlew cleanBuildCache
./gradlew --stop
rm -rf .gradle
rm -rf build
```

**SDK not found**
- Open SDK Manager in Android Studio
- Install Android SDK 34
- Install Android SDK Build-Tools 34

**Kotlin version mismatch**
- Check `build.gradle.kts` for Kotlin version
- Update IDE plugin if needed

### Runtime Issues

**App crashes on startup**
- Check logcat for errors
- Verify all dependencies are included
- Ensure Hilt is properly configured

**Model not loading**
- Verify model file exists
- Check file permissions
- Ensure sufficient storage space

**Agent service not starting**
- Check foreground service permissions
- Verify NullClaw binary is executable
- Review service logs

### Performance Issues

**Slow builds**
- Increase Gradle memory in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4096m
  ```

**Slow app performance**
- Enable ProGuard in release builds
- Check for memory leaks
- Profile with Android Profiler

## CI/CD Setup

### GitHub Actions

Create `.github/workflows/android.yml`:
```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
```

## Distribution

### Play Store

1. Build signed release APK
2. Create Play Store listing
3. Upload APK/AAB
4. Submit for review

### Direct Distribution

1. Build release APK
2. Generate APK checksum
3. Create release notes
4. Distribute via:
   - GitHub Releases
   - Direct download
   - F-Droid (requires FOSS compliance)

## Maintenance

### Updating Dependencies

```bash
# Check for updates
./gradlew dependencyUpdates

# Update Compose BOM
# Edit app/build.gradle.kts
# implementation(platform("androidx.compose:compose-bom:NEW_VERSION"))
```

### Version Management

Update `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 2
        versionName = "1.1.0"
    }
}
```

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Gradle Documentation](https://docs.gradle.org/)

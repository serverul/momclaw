# MOMCLAW Build Guide

This document explains how to build the MOMCLAW Android application from source.

## Prerequisites

- **Java Development Kit (JDK) 17** or higher
- **Android SDK** with API level 35 (Android 15) and build tools
- **Android NDK** r25c or higher (for native components)
- **Gradle 8.9+** (wrapper included)
- **Git**

## Quick Start

```bash
# Clone the repository
git clone https://github.com/serverul/MOMCLAW.git
cd MOMCLAW

# Make gradlew executable (Unix/Linux/macOS)
chmod +x android/gradlew

# Build debug APK
./android/gradlew assembleDebug

# Build release APK
./android/gradlew assembleRelease

# Build Android App Bundle (release)
./android/gradlew bundleRelease
```

## Build Variants

MOMCLAW supports the following build types:

- **debug**: For development and testing (includes debug symbols, no minification)
- **release**: For distribution (minified, obfuscated, signed)

## Build Tasks

### Common Gradle Tasks

| Task | Description |
|------|-------------|
| `assembleDebug` | Build debug APK |
| `assembleRelease` | Build release APK |
| `bundleRelease` | Build release AAB (Google Play format) |
| `clean` | Delete build outputs |
| `testDebugUnitTest` | Run unit tests |
| `connectedAndroidTest` | Run instrumented tests on device/emulator |
| `lint` | Run Android lint |
| `detekt` | Run Kotlin static analysis |

### Module-specific Tasks

MOMCLAW is a multi-module project:

- `:app` - Main Android application
- `:bridge` - Kotlin HTTP server (LiteRT bridge)
- `:agent` - NullClaw agent integration

Build specific modules:
```bash
# Build only the app module
./android/gradlew :app:assembleDebug

# Build all modules
./android/gradlew assembleDebug
```

## Native Build Configuration

MOMCLAW uses CMake for native components (future extensions). Current native setup:

- **CMake version**: 3.22.1+
- **C++ Standard**: C++17
- **ABI Filters**: armeabi-v7a, arm64-v8a, x86, x86_64 (all by default)
- **Optimization**: -O3 for release builds

The native build is configured in `app/build.gradle.kts`:
```kotlin
externalNativeBuild {
    cmake {
        path("src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}
```

Currently, no native libraries are built as MOMCLAW uses precompiled LiteRT-LM and NullClaw binaries.

## Dependencies

MOMCLAW uses the following key dependencies:

### AndroidX & Jetpack
- Compose BOM 2024.10.01 (Material 3)
- Lifecycle, Room, DataStore, Navigation, WorkManager
- Hilt for dependency injection

### Kotlin
- Kotlin stdlib 2.0.21
- Kotlinx Coroutines 1.9.0
- Kotlinx Serialization JSON 1.6.3

### Networking
- Ktor 2.3.8 (bridge module)
- OkHttp 4.12.0 + OkHttp-SSE

### Testing
- JUnit 4, Espresso, Mockito
- Compose UI Testing

## Signing Release Builds

To create a signed release build:

1. Create a keystore (if you don't have one):
   ```bash
   keytool -genkey -v -keystore MOMCLAW-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias MOMCLAW
   ```

2. Place the keystore in the project root and create `key.properties`:
   ```properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=MOMCLAW
   storeFile=./MOMCLAW-release-key.jks
   ```

3. Build the signed AAB:
   ```bash
   ./android/gradlew bundleRelease
   ```

The signed bundle will be at `android/app/build/outputs/bundle/release/app-release.aab`

## Troubleshooting

### Common Issues

1. **SDK not found**: Ensure ANDROID_HOME is set or install via Android Studio
2. **Java version mismatch**: Use JDK 17 (not 11 or 21)
3. **Gradle daemon issues**: Add `--no-daemon` to gradle commands
4. **Out of memory**: Increase `-Xmx` in `gradle.properties`
5. **Signature errors**: Verify keystore passwords and alias

### Performance Tips

- Enable Gradle configuration cache: `org.gradle.configuration-cache=true`
- Enable Gradle build cache: `org.gradle.caching=true`
- Use Gradle daemon for faster incremental builds
- For CI: Consider using `--parallel` flag

## CI/CD Integration

MOMCLAW includes GitHub Actions workflows in `.github/workflows/`:

- `android-build.yml`: Comprehensive build/test matrix
- `ci.yml`: Simplified CI for PRs
- `release.yml`: Automated releases (when tagged)

## Docker Build (Alternative)

For consistent builds, use Docker:

```bash
docker build -t MOMCLAW-builder -f android/Dockerfile .
docker run --rm -v $(pwd):/workspace MOMCLAW-builder ./gradlew assembleDebug
```

*Note: Dockerfile would need to be created for this approach.*

## Verifying the Build

After building, verify the APK:

```bash
# Check APK contents
jar tf android/app/build/outputs/apk/debug/app-debug.apk

# Verify signature (release APK only)
jarsigner -verify -verbose -certs android/app/build/outputs/apk/release/app-release.apk

# Check size
du -h android/app/build/outputs/apk/debug/app-debug.apk
```

## Supported ABIs

By default, MOMCLAW builds for all ABIs:
- armeabi-v7a (32-bit ARM)
- arm64-v8a (64-bit ARM)
- x86 (32-bit x86)
- x86_64 (64-bit x86)

To limit ABIs (faster builds, smaller APK), modify `gradle.properties`:
```properties
# Uncomment and set desired ABIs
android.defaultConfig.ndk.abiFilters=arm64-v8a
```

## License

See [LICENSE](LICENSE) file for details.
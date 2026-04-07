# MomClaw Dependencies Reference

This document lists all dependencies used in the MomClaw Android project.

## Root Project Dependencies

### Plugins
- `com.android.application`: 8.3.0
- `com.android.library`: 8.3.0
- `org.jetbrains.kotlin.android`: 1.9.22
- `com.google.dagger.hilt.android`: 2.50

## App Module

### Core
- `androidx.core:core-ktx`: 1.12.0
- `androidx.appcompat:appcompat`: 1.6.1

### Jetpack Compose
- `androidx.compose:compose-bom`: 2024.02.00
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-graphics`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.material3:material3`
- `androidx.compose.material:material-icons-extended`

### Activity & Navigation
- `androidx.activity:activity-compose`: 1.8.2
- `androidx.navigation:navigation-compose`: 2.7.6

### Lifecycle
- `androidx.lifecycle:lifecycle-viewmodel-compose`: 2.7.0
- `androidx.lifecycle:lifecycle-runtime-compose`: 2.7.0
- `androidx.lifecycle:lifecycle-runtime-ktx`: 2.7.0

### Room Database
- `androidx.room:room-runtime`: 2.6.1
- `androidx.room:room-ktx`: 2.6.1
- `androidx.room:room-compiler`: 2.6.1 (kapt)

### DataStore
- `androidx.datastore:datastore-preferences`: 1.0.0

### Networking
- `com.squareup.okhttp3:okhttp`: 4.12.0
- `com.squareup.okhttp3:okhttp-sse`: 4.12.0

### Dependency Injection
- `com.google.dagger:hilt-android`: 2.50
- `com.google.dagger:hilt-compiler`: 2.50 (kapt)
- `androidx.hilt:hilt-navigation-compose`: 1.1.0

### Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`: 1.7.3
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`: 1.7.3

### Serialization
- `org.jetbrains.kotlinx:kotlinx-serialization-json`: 1.6.2

### Debug
- `androidx.compose.ui:ui-tooling` (debugImplementation)
- `androidx.compose.ui:ui-test-manifest` (debugImplementation)

### Testing
- `junit:junit`: 4.13.2 (testImplementation)
- `org.mockito.kotlin:mockito-kotlin`: 5.2.1 (testImplementation)
- `org.jetbrains.kotlinx:kotlinx-coroutines-test`: 1.7.3 (testImplementation)
- `androidx.room:room-testing`: 2.6.1 (testImplementation)
- `androidx.test.ext:junit`: 1.1.5 (androidTestImplementation)
- `androidx.test.espresso:espresso-core`: 3.5.1 (androidTestImplementation)
- `androidx.compose.ui:ui-test-junit4` (androidTestImplementation)

## Bridge Module

### Ktor Server
- `io.ktor:ktor-server-netty`: 2.3.7
- `io.ktor:ktor-server-content-negotiation`: 2.3.7
- `io.ktor:ktor-serialization-kotlinx-json`: 2.3.7
- `io.ktor:ktor-server-cors`: 2.3.7
- `io.ktor:ktor-server-call-logging`: 2.3.7

### Kotlinx Serialization
- `org.jetbrains.kotlinx:kotlinx-serialization-json`: 1.6.2

### Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`: 1.7.3
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`: 1.7.3

### Core
- `androidx.core:core-ktx`: 1.12.0
- `androidx.appcompat:appcompat`: 1.6.1

### Dependency Injection
- `com.google.dagger:hilt-android`: 2.50
- `com.google.dagger:hilt-compiler`: 2.50 (kapt)

### Testing
- `junit:junit`: 4.13.2 (testImplementation)
- `androidx.test.ext:junit`: 1.1.5 (androidTestImplementation)
- `androidx.test.espresso:espresso-core`: 3.5.1 (androidTestImplementation)

## Agent Module

### Core
- `androidx.core:core-ktx`: 1.12.0
- `androidx.appcompat:appcompat`: 1.6.1

### Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`: 1.7.3
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`: 1.7.3

### Serialization
- `org.jetbrains.kotlinx:kotlinx-serialization-json`: 1.6.2

### Dependency Injection
- `com.google.dagger:hilt-android`: 2.50
- `com.google.dagger:hilt-compiler`: 2.50 (kapt)

### Lifecycle
- `androidx.lifecycle:lifecycle-runtime-ktx`: 2.7.0

### Testing
- `junit:junit`: 4.13.2 (testImplementation)
- `androidx.test.ext:junit`: 1.1.5 (androidTestImplementation)
- `androidx.test.espresso:espresso-core`: 3.5.1 (androidTestImplementation)

## Plugin Versions

### Kotlin
- **Version**: 1.9.22
- **Compiler Extension**: 1.5.8 (Compose)

### Gradle Plugins
- **Android Gradle Plugin**: 8.3.0
- **Kotlin Gradle Plugin**: 1.9.22
- **Hilt Gradle Plugin**: 2.50

## Gradle Wrapper
- **Version**: 8.4
- **Distribution**: gradle-8.4-bin.zip

## Java Version
- **Target**: 17
- **Compatibility**: Java 17

## Android SDK
- **compileSdk**: 34 (Android 14)
- **targetSdk**: 34
- **minSdk**: 26 (Android 8.0)

## Update Policy

Dependencies are managed at the module level but follow these guidelines:

1. **Compose BOM**: Use latest stable BOM (currently 2024.02.00)
2. **Kotlin**: Match Compose compiler version
3. **Hilt**: Use latest stable (currently 2.50)
4. **Room**: Use latest stable (currently 2.6.1)
5. **Security updates**: Apply immediately
6. **Major version updates**: Test thoroughly before upgrading

## Checking for Updates

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# View dependency tree
./gradlew :app:dependencies
./gradlew :bridge:dependencies
./gradlew :agent:dependencies
```

## Security Vulnerabilities

Regularly scan for vulnerabilities:

```bash
# Using OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# Using Gradle Versions Plugin
./gradlew dependencyUpdates -Drevision=release
```

## Notes

- All versions are as of implementation date (April 2026)
- LiteRT-LM SDK is not yet included (awaiting Google AI Edge release)
- NullClaw binary is compiled externally, not a Gradle dependency

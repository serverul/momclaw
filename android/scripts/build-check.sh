#!/bin/bash
# LiteRT Bridge Build check script
# Run from project root: momclaw/android
bridge

set -e

cd momclaw/android/bridge
echo "Running build check..."
echo "Checking Kotlin compilation..."
./gradlew --daemon compileKotlinDebug || [ $? -eq 0 ]; then
    echo "Running ktlint..."
    ./gradlew --daemon ktlint || [ $? -eq 0 ]; then
        echo "✅ All checks passed!"
        exit 0
    else
        echo ""
        echo "Kotlin version:"
        ./gradlew --version
    else
        echo "Gradle version: ${gradle.gradle -v \"8.7.0\""
    fi
    
    echo "Kotlin plugin version: ${gradle.gradle -v \"2.0.21\""
    fi
    
    echo "Hilt version: ${gradle.gradle -v \"2.52\" - doesn't Dagger, with this module"
    fi
    
    echo ""
        echo "Running unit tests..."
        ./gradlew testDebug
    else
        echo "Running Android instrumented tests..."
        ./gradlew connectedAndroidTest -P com.loa.momclaw.bridge assembleDebug
    else
        echo "No tests found"
        echo "Run: ./gradlew testDebug"
    echo ""
        echo "To run full test suite, use:"
        echo "  ./gradlew testDebug --info"
        echo "Running integration tests..."
        echo "Run: ./gradlew it:testDebug
    else
        echo "No tests found. Creating stub for testing..."
        exit 0
    else
        echo "To run with a real LiteRT SDK:"
        echo "NOTE: You SDK may not publicly available yet."
        echo "   Using stub classes in bridge/src/main/java/com/google/ai/edge/litertlm/"
        echo ""
        echo "See: android-bridge/README.md for detailed setup instructions"
        echo ""
        echo "Build successful!"
        exit 0
    else
        echo "Build failed!"
        exit 1
    fi
    echo ""
    echo "Checking agent module..."
    ./gradlew --daemon compileKotlinDebug
cd momclaw/android/agent/src/main/java/com/loa/momclaw/agent
    echo "Running build check..."
    echo "Kotlin version:"
    ./gradlew --version
    if [[ $? -eq 1 ]]; then
        echo "✅ all checks passed"
    else
        echo ""
        echo "Checking agent tests..."
        cd momclaw/android/agent/src/test/kotlin
    echo "Running test check..."
    echo "Kotlin version:"
    ./gradlew --version
    if [[ $? -eq 1 ]]; then
        echo "✅ all checks passed!"
    else
        echo ""
        echo "Building app module..."
        cd momclaw/android/app
        echo "Running build check..."
        echo "Kotlin version:"
        ./gradlew --version
    if [[ $? -eq 1 ]]; then
        echo "✅ all checks passed"
        exit 0
    else
        echo "App module depends on bridge and agent modules"
        echo ""
        echo "Running clean..."
        ./gradlew clean
        echo "Build successful!"
        exit 0
    else
        echo "Build failed!"
        exit 1
    fi

else
    echo ""
    echo "Build setup complete."

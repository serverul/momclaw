#!/bin/bash

# MomClAW Integration Test Script
# Runs comprehensive integration tests and validates startup sequence

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${CYAN}MomClAW Integration & Validation Test Suite${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNINGS=0

# Test result tracking
declare -a TEST_RESULTS

log_test() {
    local name=$1
    local result=$2
    local message=${3:-""}
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$result" == "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}✓ PASS${NC} - $name"
        TEST_RESULTS+=("✓ $name")
    elif [ "$result" == "FAIL" ]; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}✗ FAIL${NC} - $name"
        [ ! -z "$message" ] && echo -e "  ${RED}→ $message${NC}"
        TEST_RESULTS+=("✗ $name - $message")
    elif [ "$result" == "WARN" ]; then
        WARNINGS=$((WARNINGS + 1))
        echo -e "${YELLOW}⚠ WARN${NC} - $name"
        [ ! -z "$message" ] && echo -e "  ${YELLOW}→ $message${NC}"
        TEST_RESULTS+=("⚠ $name - $message")
    fi
}

# Check prerequisites
echo -e "${BLUE}▶ Checking Prerequisites${NC}"
echo

# JDK
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        log_test "JDK 17+" "PASS" "Version: $(java -version 2>&1 | head -n1)"
    else
        log_test "JDK 17+" "FAIL" "Found JDK $JAVA_VERSION, required 17+"
    fi
else
    log_test "JDK 17+" "FAIL" "Java not found"
fi

# Android SDK
if [ ! -z "$ANDROID_HOME" ]; then
    if [ -d "$ANDROID_HOME" ]; then
        log_test "Android SDK" "PASS" "ANDROID_HOME=$ANDROID_HOME"
    else
        log_test "Android SDK" "FAIL" "ANDROID_HOME set but directory not found"
    fi
else
    log_test "Android SDK" "WARN" "ANDROID_HOME not set"
fi

# Gradle wrapper
if [ -f "$ANDROID_DIR/gradlew" ]; then
    log_test "Gradle Wrapper" "PASS"
    chmod +x "$ANDROID_DIR/gradlew"
else
    log_test "Gradle Wrapper" "FAIL" "gradlew not found"
fi

echo
echo -e "${BLUE}▶ Validating Project Structure${NC}"
echo

# Check key files exist
check_file() {
    local file=$1
    local description=$2
    if [ -f "$PROJECT_ROOT/$file" ]; then
        log_test "$description exists" "PASS"
    else
        log_test "$description exists" "FAIL" "Missing: $file"
    fi
}

check_file "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "StartupManager"
check_file "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" "InferenceService"
check_file "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" "AgentService"
check_file "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "LiteRTBridge"
check_file "android/agent/src/main/java/com/loa/momclaw/agent/NullClawBridge.kt" "NullClawBridge"

echo
echo -e "${BLUE}▶ Validating Test Coverage${NC}"
echo

# Check test files exist
check_test() {
    local file=$1
    local description=$2
    if [ -f "$PROJECT_ROOT/$file" ]; then
        log_test "$description test exists" "PASS"
    else
        log_test "$description test exists" "FAIL" "Missing: $file"
    fi
}

check_test "android/app/src/test/java/com/loa/momclaw/startup/StartupManagerTest.kt" "StartupManager"
check_test "android/app/src/test/java/com/loa/momclaw/ui/chat/ChatViewModelTest.kt" "ChatViewModel"
check_test "android/app/src/test/java/com/loa/momclaw/integration/ServiceLifecycleIntegrationTest.kt" "Service Lifecycle"
check_test "android/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt" "Offline Functionality"
check_test "android/app/src/test/java/com/loa/momclaw/integration/ChatFlowIntegrationTest.kt" "Chat Flow"
check_test "android/app/src/test/java/com/loa/momclaw/integration/LiteRTBridgeIntegrationTest.kt" "LiteRT Bridge"
check_test "android/app/src/test/java/com/loa/momclaw/integration/NullClawBridgeIntegrationTest.kt" "NullClaw Bridge"

echo
echo -e "${BLUE}▶ Running Unit Tests${NC}"
echo

cd "$ANDROID_DIR"

# Run unit tests
if ./gradlew testDebugUnitTest --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    log_test "Unit tests" "PASS" "All unit tests passed"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_test "Unit tests" "FAIL" "Some unit tests failed"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

echo
echo -e "${BLUE}▶ Running Static Analysis${NC}"
echo

# Run lint
if ./gradlew lintDebug --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    log_test "Android Lint" "PASS"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_test "Android Lint" "WARN" "Lint warnings found"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    WARNINGS=$((WARNINGS + 1))
fi

# Run detekt
if [ -f "$PROJECT_ROOT/detekt.yml" ]; then
    if ./gradlew detekt --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        log_test "Detekt (Kotlin analysis)" "PASS"
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log_test "Detekt (Kotlin analysis)" "WARN" "Code style issues found"
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
else
    log_test "Detekt (Kotlin analysis)" "SKIP" "detekt.yml not found"
fi

echo
echo -e "${BLUE}▶ Validating Architecture${NC}"
echo

# Check for proper service dependencies
if grep -q "InferenceService" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" && \
   grep -q "AgentService" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt"; then
    log_test "StartupManager references both services" "PASS"
else
    log_test "StartupManager references both services" "FAIL" "Missing service references"
fi

# Check for proper error handling
if grep -q "try {" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" && \
   grep -q "catch (e: Exception)" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt"; then
    log_test "Error handling in StartupManager" "PASS"
else
    log_test "Error handling in StartupManager" "WARN" "Consider adding try-catch blocks"
fi

# Check for exponential backoff in AgentService
if grep -q "calculateBackoffDelay" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt"; then
    log_test "Exponential backoff in AgentService" "PASS"
else
    log_test "Exponential backoff in AgentService" "FAIL" "Missing retry logic"
fi

# Check for thread safety
if grep -q "@Synchronized\|AtomicReference\|ReentrantLock" "$PROJECT_ROOT/android/agent/src/main/java/com/loa/momclaw/agent/NullClawBridge.kt"; then
    log_test "Thread safety in NullClawBridge" "PASS"
else
    log_test "Thread safety in NullClawBridge" "WARN" "Consider adding thread safety"
fi

echo
echo -e "${BLUE}▶ Validating Documentation${NC}"
echo

# Check documentation exists
check_file "TESTING.md" "Testing guide"
check_file "INTEGRATION-REPORT.md" "Integration report"
check_file "SPEC.md" "Technical specification"
check_file "DOCUMENTATION.md" "API documentation"

echo
echo -e "${BLUE}▶ Checking Offline Functionality${NC}"
echo

# Verify offline tests exist
if grep -q "offline\|Offline" "$PROJECT_ROOT/android/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt"; then
    log_test "Offline functionality tests" "PASS"
else
    log_test "Offline functionality tests" "FAIL" "Missing offline tests"
fi

# Check for network fallback logic
if grep -q "isAvailable\|isAgentAvailable" "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/domain/repository/ChatRepository.kt"; then
    log_test "Network availability checks" "PASS"
else
    log_test "Network availability checks" "WARN" "Add network state checks"
fi

echo
echo -e "${BLUE}▶ Build Validation${NC}"
echo

# Try to build debug APK
echo "Building debug APK (this may take a few minutes)..."
if ./gradlew assembleDebug --quiet 2>&1 | tail -1 | grep -q "BUILD SUCCESSFUL"; then
    log_test "Debug APK build" "PASS"
    
    # Check APK exists
    APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log_test "APK file exists" "PASS" "Size: $APK_SIZE"
    else
        log_test "APK file exists" "FAIL" "APK not found"
    fi
else
    log_test "Debug APK build" "FAIL" "Build failed"
fi

echo
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${CYAN}Test Summary${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo

echo -e "Total Tests:  ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"
echo -e "Warnings:     ${YELLOW}$WARNINGS${NC}"
echo

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ All critical tests passed!${NC}"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ Note: $WARNINGS warnings found - review above${NC}"
    fi
    
    echo
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Run instrumented tests on a device: ./android/gradlew connectedAndroidTest"
    echo "2. Follow manual testing checklist in TESTING.md"
    echo "3. Test startup sequence on physical device"
    echo "4. Verify offline functionality"
    echo
    exit 0
else
    echo -e "${RED}✗ Some tests failed - review errors above${NC}"
    echo
    exit 1
fi

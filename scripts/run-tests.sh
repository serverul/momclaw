#!/bin/bash

# MomClaw Test Runner Script
# Runs all test suites

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}MomClaw Test Suite${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and track results
run_test() {
    local name=$1
    local command=$2
    
    echo -e "${YELLOW}Running: $name${NC}"
    echo
    
    if eval $command; then
        echo -e "${GREEN}✓ $name passed${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ $name failed${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo
    echo -e "${BLUE}─────────────────────────────────────${NC}"
    echo
}

# Check if in correct directory
if [ ! -f "android/gradlew" ]; then
    echo -e "${RED}Error: Run this script from momclaw root directory${NC}"
    exit 1
fi

# Make gradlew executable
chmod +x android/gradlew

# Unit Tests
echo -e "${BLUE}Unit Tests${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
run_test "Unit Tests (Debug)" "./android/gradlew testDebugUnitTest --info"

# Instrumented Tests (if device connected)
echo -e "${BLUE}Instrumented Tests${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"

# Check if device is connected
if adb devices | grep -q "device$"; then
    echo -e "${GREEN}Android device connected${NC}"
    run_test "Instrumented Tests" "./android/gradlew connectedAndroidTest"
else
    echo -e "${YELLOW}No Android device connected - skipping instrumented tests${NC}"
    echo -e "${YELLOW}Connect a device or start an emulator to run instrumented tests${NC}"
fi

# Lint
echo -e "${BLUE}Static Analysis${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
run_test "Android Lint" "./android/gradlew lintDebug"

# Detekt (Kotlin static analysis)
if [ -f "detekt.yml" ]; then
    run_test "Detekt (Kotlin Analysis)" "./android/gradlew detekt"
else
    echo -e "${YELLOW}detekt.yml not found - skipping Detekt${NC}"
fi

# Coverage Report (optional)
if [ "$1" == "--coverage" ]; then
    echo -e "${BLUE}Coverage Report${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    run_test "Code Coverage" "./android/gradlew testDebugUnitTestCoverage"
    
    if [ -f "android/app/build/reports/coverage/test/debug/index.html" ]; then
        echo -e "${GREEN}Coverage report generated${NC}"
        echo "Location: android/app/build/reports/coverage/test/debug/index.html"
    fi
fi

# Summary
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo
echo -e "Total:   ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:  ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:  ${RED}$FAILED_TESTS${NC}"
echo

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi

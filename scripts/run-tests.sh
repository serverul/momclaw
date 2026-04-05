#!/usr/bin/env bash
# run-tests.sh — Comprehensive test suite for MomClaw
# Usage: ./run-tests.sh [--unit] [--instrumented] [--lint] [--coverage] [--all]
#
# Runs all tests and validation checks
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Flags
RUN_UNIT=false
RUN_INSTRUMENTED=false
RUN_LINT=false
RUN_COVERAGE=false
RUN_ALL=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit)
            RUN_UNIT=true
            shift
            ;;
        --instrumented)
            RUN_INSTRUMENTED=true
            shift
            ;;
        --lint)
            RUN_LINT=true
            shift
            ;;
        --coverage)
            RUN_COVERAGE=true
            shift
            ;;
        --all)
            RUN_ALL=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --unit          Run unit tests"
            echo "  --instrumented  Run instrumented tests (requires device/emulator)"
            echo "  --lint          Run lint checks"
            echo "  --coverage      Run tests with coverage report"
            echo "  --all           Run all tests and checks"
            echo "  -h, --help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --unit              # Run only unit tests"
            echo "  $0 --lint              # Run only lint checks"
            echo "  $0 --all               # Run everything"
            echo "  $0 --coverage --unit   # Run unit tests with coverage"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Default to all if no flags specified
if [ "$RUN_UNIT" = false ] && [ "$RUN_INSTRUMENTED" = false ] && [ "$RUN_LINT" = false ] && [ "$RUN_COVERAGE" = false ]; then
    RUN_ALL=true
fi

if [ "$RUN_ALL" = true ]; then
    RUN_UNIT=true
    RUN_INSTRUMENTED=false  # Skip instrumented by default (requires device)
    RUN_LINT=true
    RUN_COVERAGE=false
fi

cd "$ANDROID_DIR"

echo -e "${BLUE}=== MomClaw Test Suite ===${NC}"
echo ""

# Ensure gradlew is executable
if [ ! -x "gradlew" ]; then
    echo -e "${YELLOW}Making gradlew executable...${NC}"
    chmod +x gradlew
fi

# Function to run unit tests
run_unit_tests() {
    echo -e "${BLUE}=== Running Unit Tests ===${NC}"
    
    if [ "$RUN_COVERAGE" = true ]; then
        ./gradlew testDebugUnitTestCoverage
    else
        ./gradlew testDebugUnitTest
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Unit tests passed${NC}"
        
        if [ "$RUN_COVERAGE" = true ]; then
            echo ""
            echo -e "${BLUE}Coverage Report:${NC}"
            echo "  file://$PWD/app/build/reports/coverage/test/debug/index.html"
        fi
    else
        echo -e "${RED}❌ Unit tests failed${NC}"
        exit 1
    fi
    echo ""
}

# Function to run instrumented tests
run_instrumented_tests() {
    echo -e "${BLUE}=== Running Instrumented Tests ===${NC}"
    echo -e "${YELLOW}Requires connected device or emulator${NC}"
    
    # Check for connected devices
    DEVICES=$(adb devices | grep -v "List of devices" | grep -c "device")
    if [ "$DEVICES" -eq 0 ]; then
        echo -e "${RED}❌ No devices connected${NC}"
        echo "Connect a device or start an emulator first"
        exit 1
    fi
    
    ./gradlew connectedAndroidTest
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Instrumented tests passed${NC}"
    else
        echo -e "${RED}❌ Instrumented tests failed${NC}"
        exit 1
    fi
    echo ""
}

# Function to run lint checks
run_lint_checks() {
    echo -e "${BLUE}=== Running Lint Checks ===${NC}"
    
    # Android Lint
    echo "Running Android Lint..."
    ./gradlew lintDebug
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Android Lint failed${NC}"
        echo "Report: file://$PWD/app/build/reports/lint-results-debug.html"
        exit 1
    fi
    echo -e "${GREEN}✅ Android Lint passed${NC}"
    
    # Detekt (if configured)
    if grep -q "detekt" build.gradle.kts; then
        echo ""
        echo "Running Detekt..."
        ./gradlew detekt
        
        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ Detekt failed${NC}"
            echo "Report: file://$PWD/build/reports/detekt/detekt.html"
            exit 1
        fi
        echo -e "${GREEN}✅ Detekt passed${NC}"
    fi
    
    echo ""
}

# Run tests based on flags
if [ "$RUN_UNIT" = true ]; then
    run_unit_tests
fi

if [ "$RUN_INSTRUMENTED" = true ]; then
    run_instrumented_tests
fi

if [ "$RUN_LINT" = true ]; then
    run_lint_checks
fi

echo -e "${GREEN}=== All Tests Passed! ===${NC}"
echo ""

# Summary
echo -e "${BLUE}Test Summary:${NC}"
if [ "$RUN_UNIT" = true ]; then
    echo "  ✅ Unit tests"
fi
if [ "$RUN_INSTRUMENTED" = true ]; then
    echo "  ✅ Instrumented tests"
fi
if [ "$RUN_LINT" = true ]; then
    echo "  ✅ Lint checks"
fi
if [ "$RUN_COVERAGE" = true ]; then
    echo "  ✅ Coverage report generated"
fi

echo ""
echo -e "${BLUE}Reports:${NC}"
echo "  Unit tests:   $ANDROID_DIR/app/build/test-results/testDebugUnitTest/"
echo "  Lint:         $ANDROID_DIR/app/build/reports/lint-results-debug.html"
if [ "$RUN_COVERAGE" = true ]; then
    echo "  Coverage:     $ANDROID_DIR/app/build/reports/coverage/test/debug/index.html"
fi

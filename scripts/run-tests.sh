#!/bin/bash
# MOMCLAW Test Runner - CI/CD Integration
# Runs all test suites and generates reports
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_DIR/android"
REPORT_DIR="$PROJECT_DIR/test-reports"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Create report directory
mkdir -p "$REPORT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   MOMCLAW Test Suite Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0

run_test_category() {
    local name="$1"
    local command="$2"
    
    echo -e "${YELLOW}[TEST] $name${NC}"
    
    if eval "$command" > "$REPORT_DIR/${name// /_}.log" 2>&1; then
        echo -e "${GREEN}  ✅ $name PASSED${NC}"
        ((TOTAL_PASSED++))
        return 0
    else
        echo -e "${RED}  ❌ $name FAILED${NC}"
        ((TOTAL_FAILED++))
        return 1
    fi
}

# Check for Java
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}ERROR: Java not found. Set JAVA_HOME.${NC}"
        exit 1
    fi
    echo -e "${GREEN}Java: $(java -version 2>&1 | head -1)${NC}"
}

# Check for Android SDK
check_android_sdk() {
    if [ -z "${ANDROID_HOME:-}" ]; then
        echo -e "${YELLOW}WARNING: ANDROID_HOME not set${NC}"
    else
        echo -e "${GREEN}Android SDK: $ANDROID_HOME${NC}"
    fi
}

# Run unit tests
run_unit_tests() {
    echo ""
    echo -e "${BLUE}--- Unit Tests ---${NC}"
    
    run_test_category "Startup_Validation" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.startup.*'"
    
    run_test_category "Architecture_Validation" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.architecture.*'"
    
    run_test_category "ChatViewModel" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.ui.chat.*'"
    
    run_test_category "Error_Scenarios" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.ErrorScenarioTest'"
    
    run_test_category "Performance_Benchmarks" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.PerformanceBenchmarkTest'"
    
    run_test_category "Health_Monitoring" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.ServiceHealthMonitoringTest'"
    
    run_test_category "Offline_Functionality" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.OfflineFunctionalityTest'"
    
    run_test_category "E2E_Integration" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.EndToEndIntegrationTest'"
    
    run_test_category "Error_Cascade" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.ErrorCascadeHandlingTest'"
    
    run_test_category "Race_Conditions" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.RaceConditionDetectionTest'"
    
    run_test_category "Deadlock_Prevention" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.DeadlockDetectionPreventionTest'"
    
    run_test_category "Retry_Logic" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.RetryLogicTransientFailureTest'"
    
    run_test_category "Service_Lifecycle" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.ServiceLifecycleIntegrationTest'"
    
    run_test_category "Performance_Memory" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest --tests 'com.loa.momclaw.integration.PerformanceAndMemoryTest'"
}

# Run all unit tests at once (faster)
run_all_unit_tests() {
    echo ""
    echo -e "${BLUE}--- All Unit Tests (batch) ---${NC}"
    
    run_test_category "All_Unit_Tests" \
        "cd '$ANDROID_DIR' && ./gradlew :app:testDebugUnitTest"
}

# Run bridge module tests
run_bridge_tests() {
    echo ""
    echo -e "${BLUE}--- Bridge Module Tests ---${NC}"
    
    run_test_category "LiteRT_Bridge" \
        "cd '$ANDROID_DIR' && ./gradlew :bridge:testDebugUnitTest"
}

# Run agent module tests
run_agent_tests() {
    echo ""
    echo -e "${BLUE}--- Agent Module Tests ---${NC}"
    
    run_test_category "NullClaw_Agent" \
        "cd '$ANDROID_DIR' && ./gradlew :agent:testDebugUnitTest"
}

# Generate coverage report
generate_coverage() {
    echo ""
    echo -e "${BLUE}--- Coverage Report ---${NC}"
    
    run_test_category "JaCoCo_Coverage" \
        "cd '$ANDROID_DIR' && ./gradlew :app:jacocoTestReport"
    
    if [ -f "$ANDROID_DIR/app/build/reports/jacoco/jacocoTestReport/html/index.html" ]; then
        echo -e "${GREEN}  Coverage report: android/app/build/reports/jacoco/jacocoTestReport/html/index.html${NC}"
    fi
}

# Run lint and static analysis
run_static_analysis() {
    echo ""
    echo -e "${BLUE}--- Static Analysis ---${NC}"
    
    run_test_category "Android_Lint" \
        "cd '$ANDROID_DIR' && ./gradlew :app:lintDebug"
    
    run_test_category "Detekt" \
        "cd '$ANDROID_DIR' && ./gradlew detekt"
}

# Generate summary report
generate_summary() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}   Test Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "  ${GREEN}Passed: $TOTAL_PASSED${NC}"
    echo -e "  ${RED}Failed: $TOTAL_FAILED${NC}"
    echo ""
    
    # Write summary to file
    cat > "$REPORT_DIR/summary.md" << EOF
# MOMCLAW Test Report

**Date:** $(date -u +"%Y-%m-%d %H:%M:%S UTC")
**Commit:** $(cd "$PROJECT_DIR" && git rev-parse --short HEAD 2>/dev/null || echo "N/A")

## Results

| Category | Status |
|----------|--------|
| Passed | $TOTAL_PASSED |
| Failed | $TOTAL_FAILED |
| **Total** | $((TOTAL_PASSED + TOTAL_FAILED)) |

## Test Categories

- Startup Validation (24/24 checks)
- Architecture Validation (MVVM + Clean Architecture)
- Performance Benchmarks (token streaming, memory, latency)
- Error Scenarios (model loading, network, crashes, resources)
- Offline Functionality
- E2E Integration
- Service Health Monitoring
- Race Condition Detection
- Deadlock Prevention
- Retry Logic
- Service Lifecycle

## Thresholds

- Token Streaming: >10 tokens/sec
- Startup Time: <3s
- Message Send: <5s
- Memory Growth: <50MB per 500 tokens
EOF

    echo -e "${GREEN}Report saved to: $REPORT_DIR/summary.md${NC}"
    
    if [ $TOTAL_FAILED -gt 0 ]; then
        echo -e "${RED}Some tests failed. Check logs in $REPORT_DIR/${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}All tests passed! ✅${NC}"
}

# Main
main() {
    local mode="${1:-full}"
    
    case "$mode" in
        quick)
            run_all_unit_tests
            ;;
        unit)
            run_unit_tests
            ;;
        coverage)
            run_all_unit_tests
            generate_coverage
            ;;
        full)
            check_java
            check_android_sdk
            run_unit_tests
            run_bridge_tests
            run_agent_tests
            generate_coverage
            run_static_analysis
            ;;
        ci)
            # CI mode - run everything needed for pipeline
            run_all_unit_tests
            run_bridge_tests
            run_agent_tests
            generate_coverage
            ;;
        *)
            echo "Usage: $0 {quick|unit|coverage|full|ci}"
            exit 1
            ;;
    esac
    
    generate_summary
}

main "$@"

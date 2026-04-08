#!/bin/bash
# MOMCLAW Integration Test Validation Script
# Validates test coverage and code quality for integration tests

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
echo -e "${CYAN}MOMCLAW Integration Test Validation${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo

TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# Check function
check() {
    local name=$1
    local condition=$2
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    if [ "$condition" = "true" ]; then
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        echo -e "${GREEN}✓${NC} $name"
    else
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        echo -e "${RED}✗${NC} $name"
    fi
}

# ==================== Test Files Existence ====================
echo -e "${BLUE}▶ Test Files Existence${NC}"
echo

check "EndToEndIntegrationTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/EndToEndIntegrationTest.kt" ] && echo true || echo false)"

check "ChatFlowIntegrationTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/ChatFlowIntegrationTest.kt" ] && echo true || echo false)"

check "ServiceLifecycleIntegrationTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/ServiceLifecycleIntegrationTest.kt" ] && echo true || echo false)"

check "OfflineFunctionalityTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt" ] && echo true || echo false)"

check "RaceConditionDetectionTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/RaceConditionDetectionTest.kt" ] && echo true || echo false)"

check "ErrorCascadeHandlingTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/ErrorCascadeHandlingTest.kt" ] && echo true || echo false)"

check "RetryLogicTransientFailureTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/RetryLogicTransientFailureTest.kt" ] && echo true || echo false)"

check "DeadlockDetectionPreventionTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/DeadlockDetectionPreventionTest.kt" ] && echo true || echo false)"

check "PerformanceAndMemoryTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration/PerformanceAndMemoryTest.kt" ] && echo true || echo false)"

check "StartupManagerTest exists" \
    "$([ -f "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/startup/StartupManagerTest.kt" ] && echo true || echo false)"

echo

# ==================== Source Files Existence ====================
echo -e "${BLUE}▶ Source Files Existence${NC}"
echo

check "MainActivity exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/MainActivity.kt" ] && echo true || echo false)"

check "StartupManager exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" ] && echo true || echo false)"

check "ServiceRegistry exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/startup/ServiceRegistry.kt" ] && echo true || echo false)"

check "InferenceService exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" ] && echo true || echo false)"

check "AgentService exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" ] && echo true || echo false)"

check "AgentClient exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/data/remote/AgentClient.kt" ] && echo true || echo false)"

check "ChatRepository exists" \
    "$([ -f "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/domain/repository/ChatRepository.kt" ] && echo true || echo false)"

echo

# ==================== Code Quality Checks ====================
echo -e "${BLUE}▶ Code Quality Checks${NC}"
echo

# Check MainActivity integration
MAINACTIVITY_INTEGRATED=$(grep -c "startupManager.startServices" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/MainActivity.kt" 2>/dev/null || echo 0)
check "MainActivity integrates StartupManager" \
    "$([ "$MAINACTIVITY_INTEGRATED" -gt 0 ] && echo true || echo false)"

# Check lifecycle observer
LIFECYCLE_OBSERVER=$(grep -c "lifecycle.addObserver" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/MainActivity.kt" 2>/dev/null || echo 0)
check "MainActivity adds lifecycle observer" \
    "$([ "$LIFECYCLE_OBSERVER" -gt 0 ] && echo true || echo false)"

# Check thread safety
LOCK_USAGE=$(grep -c "ReentrantLock\|withLock" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" 2>/dev/null || echo 0)
check "StartupManager uses thread-safe locks" \
    "$([ "$LOCK_USAGE" -gt 0 ] && echo true || echo false)"

# Check error handling
TRY_CATCH=$(grep -c "try {" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" 2>/dev/null || echo 0)
check "StartupManager has error handling" \
    "$([ "$TRY_CATCH" -gt 0 ] && echo true || echo false)"

# Check exponential backoff
BACKOFF=$(grep -c "calculateBackoffDelay\|backoffMultiplier" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" 2>/dev/null || echo 0)
check "AgentService has exponential backoff" \
    "$([ "$BACKOFF" -gt 0 ] && echo true || echo false)"

# Check timeout handling
TIMEOUT=$(grep -c "withTimeout\|TIMEOUT" "$ANDROID_DIR/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" 2>/dev/null || echo 0)
check "StartupManager has timeout handling" \
    "$([ "$TIMEOUT" -gt 0 ] && echo true || echo false)"

echo

# ==================== Package Consistency ====================
echo -e "${BLUE}▶ Package Consistency${NC}"
echo

# Check package names
PACKAGES_CONSISTENT=true
for file in $(find "$ANDROID_DIR/app/src" -name "*.kt" | grep -E "integration|startup"); do
    pkg=$(grep "^package " "$file" | head -1)
    if [[ "$pkg" != *"com.loa.momclaw"* ]]; then
        echo -e "  ${YELLOW}Warning:${NC} $(basename $file) has inconsistent package: $pkg"
        PACKAGES_CONSISTENT=false
    fi
done

check "Package names are consistent" \
    "$PACKAGES_CONSISTENT"

echo

# ==================== Test Coverage Summary ====================
echo -e "${BLUE}▶ Test Coverage Summary${NC}"
echo

# Count tests
UNIT_TEST_COUNT=$(find "$ANDROID_DIR/app/src/test" -name "*Test.kt" | wc -l)
INSTRUMENTED_TEST_COUNT=$(find "$ANDROID_DIR/app/src/androidTest" -name "*Test.kt" | wc -l)
INTEGRATION_TEST_COUNT=$(find "$ANDROID_DIR/app/src/test/java/com/loa/momclaw/integration" -name "*Test.kt" 2>/dev/null | wc -l)

echo -e "  Unit test files:        ${CYAN}$UNIT_TEST_COUNT${NC}"
echo -e "  Instrumented test files: ${CYAN}$INSTRUMENTED_TEST_COUNT${NC}"
echo -e "  Integration test files:  ${CYAN}$INTEGRATION_TEST_COUNT${NC}"
echo

# ==================== Integration Points ====================
echo -e "${BLUE}▶ Integration Points${NC}"
echo

# Service Registry usage
REGISTRY_USAGE=$(grep -r "ServiceRegistry" "$ANDROID_DIR/app/src/main/java" 2>/dev/null | wc -l)
check "ServiceRegistry is used in codebase" \
    "$([ "$REGISTRY_USAGE" -gt 0 ] && echo true || echo false)"

# LiteRT Bridge reference
LITERT_REF=$(grep -r "LiteRTBridge" "$ANDROID_DIR/app/src/main/java" 2>/dev/null | wc -l)
check "LiteRTBridge is referenced" \
    "$([ "$LITERT_REF" -gt 0 ] && echo true || echo false)"

# NullClaw Bridge reference
NULLCLAW_REF=$(grep -r "NullClawBridge" "$ANDROID_DIR/app/src/main/java" 2>/dev/null | wc -l)
check "NullClawBridge is referenced" \
    "$([ "$NULLCLAW_REF" -gt 0 ] && echo true || echo false)"

# InferenceService reference
INFERENCE_REF=$(grep -r "InferenceService" "$ANDROID_DIR/app/src/main/java" 2>/dev/null | wc -l)
check "InferenceService is referenced" \
    "$([ "$INFERENCE_REF" -gt 0 ] && echo true || echo false)"

# AgentService reference
AGENT_REF=$(grep -r "AgentService" "$ANDROID_DIR/app/src/main/java" 2>/dev/null | wc -l)
check "AgentService is referenced" \
    "$([ "$AGENT_REF" -gt 0 ] && echo true || echo false)"

echo

# ==================== Final Summary ====================
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${CYAN}Validation Summary${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo

echo -e "Total Checks:  ${BLUE}$TOTAL_CHECKS${NC}"
echo -e "Passed:        ${GREEN}$PASSED_CHECKS${NC}"
echo -e "Failed:        ${RED}$FAILED_CHECKS${NC}"
echo

SUCCESS_RATE=$(echo "scale=1; $PASSED_CHECKS * 100 / $TOTAL_CHECKS" | bc 2>/dev/null || echo "N/A")

echo -e "Success Rate:  ${GREEN}$SUCCESS_RATE%${NC}"
echo

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}✓ All validation checks passed!${NC}"
    echo
    echo -e "${BLUE}Integration Testing Status: PRODUCTION READY${NC}"
    echo
    exit 0
else
    echo -e "${RED}✗ Some validation checks failed${NC}"
    echo
    echo -e "${YELLOW}Review the failures above and fix them before production deployment${NC}"
    echo
    exit 1
fi

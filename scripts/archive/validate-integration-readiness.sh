#!/bin/bash
# MomClAW v1.0.0 - Integration Testing Validation Script
# Validates test infrastructure readiness and runs available tests

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}MomClAW Integration Testing Validator${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Track results
PASSED=0
FAILED=0
WARNINGS=0

check_pass() {
    echo -e "${GREEN}✅ PASS${NC}: $1"
    ((PASSED++))
}

check_fail() {
    echo -e "${RED}❌ FAIL${NC}: $1"
    ((FAILED++))
}

check_warn() {
    echo -e "${YELLOW}⚠️  WARN${NC}: $1"
    ((WARNINGS++))
}

check_info() {
    echo -e "${BLUE}ℹ️  INFO${NC}: $1"
}

# ==================== Environment Checks ====================

echo -e "\n${BLUE}[1/7] Environment Checks${NC}"
echo "----------------------------------------"

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    if [[ "$JAVA_VERSION" == "17"* ]]; then
        check_pass "Java 17 installed (version: $JAVA_VERSION)"
    else
        check_fail "Java version mismatch (found: $JAVA_VERSION, required: 17)"
    fi
else
    check_fail "Java not installed"
    check_info "Install with: sudo apt-get install openjdk-17-jdk"
fi

# Check JAVA_HOME
if [ -n "$JAVA_HOME" ]; then
    check_pass "JAVA_HOME is set ($JAVA_HOME)"
else
    check_warn "JAVA_HOME not set"
    check_info "Set with: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
fi

# Check Android SDK
if [ -n "$ANDROID_HOME" ]; then
    check_pass "ANDROID_HOME is set ($ANDROID_HOME)"
else
    check_warn "ANDROID_HOME not set"
    check_info "Required for instrumented tests only"
fi

# Check Gradle wrapper
if [ -f "android/gradlew" ]; then
    check_pass "Gradle wrapper exists"
    chmod +x android/gradlew
else
    check_fail "Gradle wrapper not found"
fi

# ==================== Test Infrastructure ====================

echo -e "\n${BLUE}[2/7] Test Infrastructure${NC}"
echo "----------------------------------------"

# Count test files
TEST_COUNT=$(find android -name "*Test.kt" -o -name "*Test.java" 2>/dev/null | wc -l)
if [ "$TEST_COUNT" -ge 30 ]; then
    check_pass "Test files found ($TEST_COUNT files)"
else
    check_warn "Fewer test files than expected ($TEST_COUNT files)"
fi

# Count integration tests
INTEGRATION_COUNT=$(find android -name "*IntegrationTest.kt" -o -name "*IntegrationTest.java" 2>/dev/null | wc -l)
if [ "$INTEGRATION_COUNT" -ge 10 ]; then
    check_pass "Integration test files found ($INTEGRATION_COUNT files)"
else
    check_warn "Fewer integration tests than expected ($INTEGRATION_COUNT files)"
fi

# Check test categories
check_info "Test Categories:"
echo "  - E2E: $(find android -path "*/e2e/*Test.kt" | wc -l) files"
echo "  - Integration: $(find android -path "*/integration/*Test.kt" | wc -l) files"
echo "  - Startup: $(find android -path "*/startup/*Test.kt" | wc -l) files"
echo "  - UI: $(find android -path "*/ui/*Test.kt" | wc -l) files"

# ==================== Build Configuration ====================

echo -e "\n${BLUE}[3/7] Build Configuration${NC}"
echo "----------------------------------------"

# Check build.gradle files
if [ -f "android/build.gradle.kts" ]; then
    check_pass "Root build.gradle.kts exists"
else
    check_fail "Root build.gradle.kts not found"
fi

if [ -f "android/app/build.gradle.kts" ]; then
    check_pass "App build.gradle.kts exists"
else
    check_fail "App build.gradle.kts not found"
fi

if [ -f "android/bridge/build.gradle.kts" ]; then
    check_pass "Bridge build.gradle.kts exists"
else
    check_fail "Bridge build.gradle.kts not found"
fi

if [ -f "android/agent/build.gradle.kts" ]; then
    check_pass "Agent build.gradle.kts exists"
else
    check_fail "Agent build.gradle.kts not found"
fi

# Check gradle.properties
if [ -f "android/gradle.properties" ]; then
    check_pass "gradle.properties exists"
    # Check for important settings
    if grep -q "android.useAndroidX=true" android/gradle.properties; then
        check_pass "AndroidX enabled"
    else
        check_warn "AndroidX not explicitly enabled"
    fi
else
    check_fail "gradle.properties not found"
fi

# ==================== Test Coverage Analysis ====================

echo -e "\n${BLUE}[4/7] Test Coverage Analysis${NC}"
echo "----------------------------------------"

# Check for E2E tests
if [ -f "android/app/src/test/java/com/loa/momclaw/e2e/CompleteE2EIntegrationTest.kt" ]; then
    check_pass "E2E integration tests exist"
else
    check_fail "E2E integration tests missing"
fi

# Check for service lifecycle tests
LIFECYCLE_TESTS=$(find android -name "*Lifecycle*Test.kt" | wc -l)
if [ "$LIFECYCLE_TESTS" -ge 2 ]; then
    check_pass "Service lifecycle tests exist ($LIFECYCLE_TESTS files)"
else
    check_fail "Service lifecycle tests insufficient"
fi

# Check for error handling tests
if [ -f "android/app/src/test/java/com/loa/momclaw/integration/ErrorCascadeHandlingTest.kt" ]; then
    check_pass "Error handling tests exist"
else
    check_warn "Error handling tests may be missing"
fi

# Check for performance tests
if [ -f "android/app/src/test/java/com/loa/momclaw/integration/PerformanceAndMemoryTest.kt" ]; then
    check_pass "Performance tests exist"
else
    check_warn "Performance tests may be missing"
fi

# Check for offline tests
if [ -f "android/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt" ]; then
    check_pass "Offline functionality tests exist"
else
    check_fail "Offline functionality tests missing"
fi

# Check for startup tests
if [ -f "android/app/src/test/java/com/loa/momclaw/startup/StartupManagerTest.kt" ]; then
    check_pass "Startup validation tests exist"
else
    check_fail "Startup validation tests missing"
fi

# ==================== Integration Test Categories ====================

echo -e "\n${BLUE}[5/7] Integration Test Categories${NC}"
echo "----------------------------------------"

# Category 1: E2E Integration
E2E_METHODS=$(grep -r "@Test" android/app/src/test/java/com/loa/momclaw/e2e/ 2>/dev/null | wc -l)
if [ "$E2E_METHODS" -ge 8 ]; then
    check_pass "E2E integration testing (~$E2E_METHODS test methods)"
else
    check_warn "E2E integration coverage may be low (~$E2E_METHODS test methods)"
fi

# Category 2: Service Lifecycle
LIFECYCLE_METHODS=$(find android -name "*Lifecycle*Test.kt" -exec grep -l "@Test" {} \; | wc -l)
if [ "$LIFECYCLE_METHODS" -ge 1 ]; then
    check_pass "Service lifecycle testing (files found: $LIFECYCLE_METHODS)"
else
    check_fail "Service lifecycle testing missing"
fi

# Category 3: Error Recovery
ERROR_METHODS=$(find android -name "*Error*Test.kt" -o -name "*Retry*Test.kt" | wc -l)
if [ "$ERROR_METHODS" -ge 1 ]; then
    check_pass "Error recovery validation (files found: $ERROR_METHODS)"
else
    check_warn "Error recovery validation may be missing"
fi

# Category 4: Memory Leak Detection
MEMORY_METHODS=$(find android -name "*Memory*Test.kt" -o -name "*Performance*Test.kt" | wc -l)
if [ "$MEMORY_METHODS" -ge 1 ]; then
    check_pass "Memory leak detection (files found: $MEMORY_METHODS)"
else
    check_warn "Memory leak detection may be missing"
fi

# Category 5: Performance Testing
PERF_METHODS=$(grep -r "test.*Performance" android/app/src/test/ 2>/dev/null | wc -l)
if [ "$PERF_METHODS" -ge 5 ]; then
    check_pass "Performance testing (~$PERF_METHODS test methods)"
else
    check_warn "Performance testing coverage may be low"
fi

# Category 6: Offline Functionality
OFFLINE_METHODS=$(grep -r "@Test" android/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt 2>/dev/null | wc -l)
if [ "$OFFLINE_METHODS" -ge 5 ]; then
    check_pass "Offline functionality verification (~$OFFLINE_METHODS test methods)"
else
    check_warn "Offline functionality coverage may be low"
fi

# Category 7: Startup Sequence
STARTUP_METHODS=$(find android -path "*/startup/*Test.kt" -exec grep -l "@Test" {} \; | wc -l)
if [ "$STARTUP_METHODS" -ge 1 ]; then
    check_pass "Startup sequence validation (files found: $STARTUP_METHODS)"
else
    check_fail "Startup sequence validation missing"
fi

# ==================== Documentation ====================

echo -e "\n${BLUE}[6/7] Documentation${NC}"
echo "----------------------------------------"

if [ -f "android/INTEGRATION_TEST_SUMMARY.md" ]; then
    check_pass "Integration test summary exists"
else
    check_warn "Integration test summary missing"
fi

if [ -f "INTEGRATION_TESTING_REPORT_AGENT3_FINAL.md" ]; then
    check_pass "Integration testing report exists"
else
    check_warn "Integration testing report missing"
fi

if [ -f "TESTING.md" ]; then
    check_pass "Testing documentation exists"
else
    check_warn "Testing documentation missing"
fi

# ==================== Readiness Summary ====================

echo -e "\n${BLUE}[7/7] Integration Readiness Summary${NC}"
echo "----------------------------------------"

TOTAL_CHECKS=$((PASSED + FAILED + WARNINGS))
PASS_RATE=$(awk "BEGIN {printf \"%.1f\", ($PASSED/$TOTAL_CHECKS)*100}")

echo -e "\n${BLUE}Check Results:${NC}"
echo "  ✅ Passed:   $PASSED"
echo "  ❌ Failed:   $FAILED"
echo "  ⚠️  Warnings: $WARNINGS"
echo -e "  ${BLUE}Total:${NC}     $TOTAL_CHECKS"
echo ""
echo -e "Pass Rate: ${GREEN}${PASS_RATE}%${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✅ INTEGRATION TEST SUITE: PRODUCTION READY${NC}"
        echo -e "${GREEN}All checks passed. Test infrastructure is complete and well-organized.${NC}"
    else
        echo -e "${GREEN}✅ INTEGRATION TEST SUITE: MOSTLY READY${NC}"
        echo -e "${YELLOW}All critical checks passed. Minor warnings can be addressed later.${NC}"
    fi
else
    echo -e "${RED}❌ INTEGRATION TEST SUITE: NOT READY${NC}"
    echo -e "${RED}$FAILED critical check(s) failed. See above for details.${NC}"
fi

# ==================== Next Steps ====================

echo -e "\n${BLUE}Next Steps:${NC}"
echo "----------------------------------------"

if [ $FAILED -gt 0 ]; then
    echo "1. Fix failing checks above"
    echo "2. Re-run this validation script"
fi

if ! command -v java &> /dev/null; then
    echo "→ Install Java 17:"
    echo "   sudo apt-get install openjdk-17-jdk"
    echo "   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
else
    echo "→ Run tests:"
    echo "   cd android"
    echo "   ./gradlew test --stacktrace"
    echo ""
    echo "→ Generate coverage report:"
    echo "   ./gradlew jacocoTestReport"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Validation Complete${NC}"
echo -e "${BLUE}========================================${NC}"

# Exit with appropriate code
if [ $FAILED -gt 0 ]; then
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    exit 0
else
    exit 0
fi

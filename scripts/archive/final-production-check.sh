#!/bin/bash

# MomClAW Final Production Check
# Quick validation of production readiness

# set -e  # Disabled for better error handling

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

check_pass() {
    echo -e "${GREEN}✅ PASS${NC}: $1"
    PASS=$((PASS + 1))
}

check_fail() {
    echo -e "${RED}❌ FAIL${NC}: $1"
    FAIL=$((FAIL + 1))
}

check_warn() {
    echo -e "${YELLOW}⚠️  WARN${NC}: $1"
    WARN=$((WARN + 1))
}

check_exists() {
    if [ -f "$1" ]; then
        check_pass "$2"
        return 0
    else
        check_fail "$2"
        return 1
    fi
}

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}MomClAW Final Production Check${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# 1. Critical Files
echo -e "${BLUE}[1/6] Critical Files${NC}"
echo "----------------------------------------"
check_exists "android/app/build.gradle.kts" "Build configuration"
check_exists "android/app/proguard-rules.pro" "ProGuard rules"
check_exists "android/gradle.properties" "Gradle properties"
check_exists "android/key.properties" "Signing configuration"
check_exists "android/momclaw-release-key.jks" "Release keystore"
check_exists "README.md" "README"
check_exists "LICENSE" "License"
check_exists "PRIVACY_POLICY.md" "Privacy policy"
echo ""

# 2. Source Code
echo -e "${BLUE}[2/6] Source Code${NC}"
echo "----------------------------------------"
MAIN_FILES=$(find android -name "*.kt" -path "*/main/*" | wc -l)
TEST_FILES=$(find android -name "*.kt" -path "*/test/*" | wc -l)
ANDROID_TEST_FILES=$(find android -name "*.kt" -path "*/androidTest/*" | wc -l)

if [ "$MAIN_FILES" -gt 50 ]; then
    check_pass "Main source files ($MAIN_FILES files)"
else
    check_fail "Main source files ($MAIN_FILES files)"
fi

if [ "$TEST_FILES" -gt 20 ]; then
    check_pass "Unit test files ($TEST_FILES files)"
else
    check_fail "Unit test files ($TEST_FILES files)"
fi

if [ "$ANDROID_TEST_FILES" -gt 5 ]; then
    check_pass "Instrumented tests ($ANDROID_TEST_FILES files)"
else
    check_warn "Instrumented tests ($ANDROID_TEST_FILES files)"
fi
echo ""

# 3. CI/CD
echo -e "${BLUE}[3/6] CI/CD Setup${NC}"
echo "----------------------------------------"
WORKFLOWS=$(ls .github/workflows/*.yml 2>/dev/null | wc -l)
if [ "$WORKFLOWS" -ge 5 ]; then
    check_pass "GitHub workflows ($WORKFLOWS files)"
else
    check_fail "GitHub workflows ($WORKFLOWS files)"
fi

SCRIPTS=$(ls scripts/*.sh 2>/dev/null | wc -l)
if [ "$SCRIPTS" -ge 15 ]; then
    check_pass "Build scripts ($SCRIPTS files)"
else
    check_fail "Build scripts ($SCRIPTS files)"
fi
echo ""

# 4. Documentation
echo -e "${BLUE}[4/6] Documentation${NC}"
echo "----------------------------------------"
check_exists "USER_GUIDE.md" "User guide"
check_exists "DEVELOPMENT.md" "Developer guide"
check_exists "API_DOCUMENTATION.md" "API documentation"
check_exists "TROUBLESHOOTING.md" "Troubleshooting guide"
check_exists "FAQ.md" "FAQ"

DOC_FILES=$(find . -maxdepth 1 -name "*.md" | wc -l)
if [ "$DOC_FILES" -gt 50 ]; then
    check_pass "Total documentation ($DOC_FILES files)"
else
    check_warn "Total documentation ($DOC_FILES files)"
fi
echo ""

# 5. Security
echo -e "${BLUE}[5/6] Security${NC}"
echo "----------------------------------------"
check_exists ".gitignore" ".gitignore"
check_exists ".gitleaks.toml" "Secret scanning config"
check_exists "SECURITY.md" "Security policy"

if grep -q "key.properties" .gitignore 2>/dev/null; then
    check_pass "key.properties in .gitignore"
else
    check_fail "key.properties in .gitignore"
fi
echo ""

# 6. Environment
echo -e "${BLUE}[6/6] Environment${NC}"
echo "----------------------------------------"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -eq 17 ]; then
        check_pass "Java 17 installed"
    else
        check_warn "Java version: $JAVA_VERSION (need 17)"
    fi
else
    check_fail "Java not installed"
fi

if command -v gradle &> /dev/null; then
    check_pass "Gradle installed"
else
    check_warn "Gradle not in PATH (using wrapper)"
fi
echo ""

# Summary
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}Passed:${NC} $PASS"
echo -e "${RED}Failed:${NC} $FAIL"
echo -e "${YELLOW}Warnings:${NC} $WARN"
echo ""

TOTAL=$((PASS + FAIL + WARN))
SUCCESS_RATE=$((PASS * 100 / TOTAL))

echo -e "${BLUE}Success Rate: ${SUCCESS_RATE}%${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✅ PRODUCTION READY${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Install Java 17 (if not installed)"
    echo "2. Run: cd android && ./gradlew test"
    echo "3. Run: cd android && ./gradlew assembleDebug"
    echo "4. Test on physical device"
    echo "5. Set GitHub Secrets"
    echo "6. Tag release: git tag v1.0.0 && git push --tags"
    exit 0
else
    echo -e "${RED}❌ NOT PRODUCTION READY${NC}"
    echo ""
    echo "Please fix the failed checks above before release."
    exit 1
fi

#!/bin/bash

# MomClAW Release Validation Script
# Comprehensive pre-release validation for v1.0.0

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ERRORS=$((ERRORS + 1))
}

check_warn() {
    echo -e "${YELLOW}!${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

echo -e "${BLUE}"
cat << "EOF"
  __  __       _   _      ___ _    ___
 |  \/  | ___ | |_| |__  / __| |  / / |
 | |\/| |/ _ \| __| '_ \| |  | | / /| |
 | |  | | (_) | |_| | | | |__| |/ / | |
 |_|  |_|\___/ \__|_| |_|\____|_/___||_|

  Release Validation Script
EOF
echo -e "${NC}"

echo -e "${BLUE}=== Checking Build Configuration ===${NC}"

# Check gradle wrapper
if [ -f "android/gradlew" ]; then
    check_pass "Gradle wrapper exists"
else
    check_fail "Gradle wrapper missing"
fi

# Check signing config
if [ -f "android/key.properties" ]; then
    check_pass "Signing configuration found"
else
    check_warn "No signing configuration (key.properties not found)"
fi

# Check keystore
if [ -f "MOMCLAW-release-key.jks" ]; then
    check_pass "Release keystore exists"
else
    check_warn "No release keystore found"
fi

echo ""
echo -e "${BLUE}=== Checking Documentation ===${NC}"

DOCS="README.md USER_GUIDE.md QUICKSTART.md DOCUMENTATION.md BUILD_CONFIGURATION.md DEPLOYMENT.md SECURITY.md PRIVACY_POLICY.md CHANGELOG.md"

for doc in $DOCS; do
    if [ -f "$doc" ]; then
        check_pass "$doc exists"
    else
        check_fail "$doc missing"
    fi
done

echo ""
echo -e "${BLUE}=== Checking Version Info ===${NC}"

# Check version in build.gradle.kts
VERSION_NAME=$(grep "versionName" android/app/build.gradle.kts | grep -o '"[^"]*"' | tr -d '"')
VERSION_CODE=$(grep "versionCode" android/app/build.gradle.kts | grep -o "= [0-9]*" | grep -o "[0-9]*")

if [ -n "$VERSION_NAME" ]; then
    check_pass "Version name: $VERSION_NAME"
else
    check_fail "Version name not found in build.gradle.kts"
fi

if [ -n "$VERSION_CODE" ]; then
    check_pass "Version code: $VERSION_CODE"
else
    check_fail "Version code not found in build.gradle.kts"
fi

# Check CHANGELOG
if grep -q "\[$VERSION_NAME\]" CHANGELOG.md 2>/dev/null; then
    check_pass "Changelog updated for $VERSION_NAME"
else
    check_warn "Changelog not updated for $VERSION_NAME"
fi

echo ""
echo -e "${BLUE}=== Checking CI/CD Configuration ===${NC}

# Check workflows
WORKFLOWS="android-build.yml release.yml google-play-deploy.yml security.yml fdroid-build.yml"

for workflow in $WORKFLOWS; do
    if [ -f ".github/workflows/$workflow" ]; then
        check_pass "Workflow $workflow exists"
    else
        check_fail "Workflow $workflow missing"
    fi
done

# Check scripts
SCRIPTS="ci-build.sh build-release.sh version-manager.sh validate-build.sh"

for script in $SCRIPTS; do
    if [ -f "scripts/$script" ]; then
        check_pass "Script $script exists"
    else
        check_fail "Script $script missing"
    fi
done

echo ""
echo -e "${BLUE}=== Checking Security ===${NC}

# Check .gitignore
if grep -q "key.properties" .gitignore 2>/dev/null; then
    check_pass "key.properties in .gitignore"
else
    check_fail "key.properties NOT in .gitignore (SECURITY RISK)"
fi

if grep -q "\.jks" .gitignore 2>/dev/null; then
    check_pass "*.jks in .gitignore"
else
    check_fail "*.jks NOT in .gitignore (SECURITY RISK)"
fi

# Check for secrets in code
if grep -rq "password\s*=\s*\"" android/app/src --include="*.kt" 2>/dev/null; then
    check_fail "Hardcoded passwords found in source code"
else
    check_pass "No hardcoded passwords found"
fi

echo ""
echo -e "${BLUE}=== Running Quick Build Test ===${NC}

cd android
if ./gradlew assembleDebug --quiet; then
    check_pass "Debug build successful"
else
    check_fail "Debug build failed"
fi

# Check APK size
APK_SIZE=$(stat -c%s app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1)
if [ -n "$APK_SIZE" ]; then
    APK_SIZE_MB=$((APK_SIZE / 1024 / 1024))
    if [ $APK_SIZE_MB -lt 150 ]; then
        check_pass "Debug APK size: ${APK_SIZE_MB}MB (under 150MB limit)"
    else
        check_warn "Debug APK size: ${APK_SIZE_MB}MB (over 150MB, check for large resources)"
    fi
else
    check_warn "Could not determine APK size"
fi

cd ..

echo ""
echo -e "${BLUE}=== Validation Summary ===${NC}"
echo ""
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ Validation passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run full test suite: ./scripts/run-tests.sh"
    echo "  2. Build release APK: ./scripts/ci-build.sh build:release $VERSION_NAME"
    echo "  3. Tag release: git tag -a v$VERSION_NAME -m 'Release v$VERSION_NAME'"
    echo "  4. Push tag: git push --tags"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Validation failed with $ERRORS error(s)${NC}"
    echo ""
    echo "Fix the errors above before releasing."
    exit 1
fi

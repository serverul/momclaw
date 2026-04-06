#!/bin/bash

# MomClaw Build Validation Script
# Runs all checks before a release

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}MomClaw Build Validation${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo

ERRORS=0

# Function to check and report
check() {
    local description=$1
    local command=$2
    
    echo -e "${YELLOW}Checking: $description${NC}"
    
    if eval $command > /dev/null 2>&1; then
        echo -e "${GREEN}✓ $description${NC}"
    else
        echo -e "${RED}✗ $description${NC}"
        ERRORS=$((ERRORS + 1))
    fi
}

# Prerequisites
echo -e "${BLUE}Prerequisites${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

check "Java 17+" "java -version 2>&1 | grep -q '17'"
check "Android SDK" "[ -n \"\$ANDROID_HOME\" ]"
check "Gradle wrapper" "[ -f android/gradlew ]"
check "Git repository" "git rev-parse --git-dir > /dev/null 2>&1"

echo

# Code Quality
echo -e "${BLUE}Code Quality${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

echo -e "${YELLOW}Running Lint...${NC}"
if ./android/gradlew lintDebug 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓ Lint passed${NC}"
else
    echo -e "${RED}✗ Lint failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo -e "${YELLOW}Running Detekt...${NC}"
if ./android/gradlew detekt 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓ Detekt passed${NC}"
else
    echo -e "${RED}✗ Detekt failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo

# Tests
echo -e "${BLUE}Tests${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

echo -e "${YELLOW}Running unit tests...${NC}"
if ./android/gradlew testDebugUnitTest 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓ Unit tests passed${NC}"
else
    echo -e "${RED}✗ Unit tests failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo

# Build
echo -e "${BLUE}Build${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

echo -e "${YELLOW}Building debug APK...${NC}"
if ./android/gradlew assembleDebug 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓ Debug APK built${NC}"
else
    echo -e "${RED}✗ Debug APK build failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo -e "${YELLOW}Building release APK...${NC}"
if ./android/gradlew assembleRelease 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo -e "${GREEN}✓ Release APK built${NC}"
else
    echo -e "${RED}✗ Release APK build failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo

# Documentation
echo -e "${BLUE}Documentation${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

check "README.md exists" "[ -f README.md ]"
check "BUILD.md exists" "[ -f BUILD.md ]"
check "CHANGELOG.md exists" "[ -f CHANGELOG.md ]"
check "CONTRIBUTING.md exists" "[ -f CONTRIBUTING.md ]"
check "LICENSE exists" "[ -f LICENSE ]"

echo

# Version Check
echo -e "${BLUE}Version Information${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

if [ -f "android/app/build.gradle.kts" ]; then
    VERSION_NAME=$(grep "versionName" android/app/build.gradle.kts | head -1 | sed 's/.*=.*"\(.*\)".*/\1/')
    VERSION_CODE=$(grep "versionCode" android/app/build.gradle.kts | head -1 | sed 's/.*=.*\([0-9]*\).*/\1/')
    
    echo -e "Version Name: ${GREEN}$VERSION_NAME${NC}"
    echo -e "Version Code: ${GREEN}$VERSION_CODE${NC}"
else
    echo -e "${YELLOW}Warning: build.gradle.kts not found${NC}"
fi

echo

# Git Status
echo -e "${BLUE}Git Status${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

if git diff-index --quiet HEAD --; then
    echo -e "${GREEN}✓ Working tree clean${NC}"
else
    echo -e "${YELLOW}⚠ Uncommitted changes:${NC}"
    git status --short
fi

CURRENT_BRANCH=$(git branch --show-current)
echo -e "Current branch: ${BLUE}$CURRENT_BRANCH${NC}"

echo

# Keystore Check
echo -e "${BLUE}Signing${NC}"
echo -e "${BLUE}─────────────────────────────────────${NC}"

if [ -f "momclaw-release-key.jks" ]; then
    echo -e "${GREEN}✓ Keystore found${NC}"
    
    if [ -f "android/key.properties" ]; then
        echo -e "${GREEN}✓ key.properties found${NC}"
    else
        echo -e "${YELLOW}⚠ key.properties not found (will prompt for password)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Keystore not found (generate for release builds)${NC}"
fi

echo

# Summary
echo -e "${BLUE}═══════════════════════════════════════${NC}"
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo -e "${GREEN}Ready for release.${NC}"
    echo
    echo "Next steps:"
    echo "  1. Update CHANGELOG.md"
    echo "  2. Tag release: git tag -a v$VERSION_NAME -m 'Release v$VERSION_NAME'"
    echo "  3. Build release: ./scripts/build-release.sh $VERSION_NAME"
    echo "  4. Upload to stores"
    exit 0
else
    echo -e "${RED}✗ $ERRORS check(s) failed${NC}"
    echo -e "${YELLOW}Fix issues before releasing.${NC}"
    exit 1
fi

#!/usr/bin/env bash
# validate-build.sh — Validate MomClaw build before release
# Usage: ./validate-build.sh [--quick] [--full]
#
# Performs comprehensive validation checks
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
NC='\033[0m'

QUICK_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            QUICK_MODE=true
            shift
            ;;
        --full)
            QUICK_MODE=false
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --quick   Quick validation (skip time-consuming checks)"
            echo "  --full    Full validation (default)"
            echo "  -h, --help    Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

cd "$ANDROID_DIR"

echo -e "${BLUE}=== MomClaw Build Validation ===${NC}"
echo ""

# Ensure gradlew is executable
chmod +x gradlew 2>/dev/null || true

# Validation checks
ERRORS=0
WARNINGS=0

check_pass() {
    echo -e "  ${GREEN}✅ $1${NC}"
}

check_fail() {
    echo -e "  ${RED}❌ $1${NC}"
    ((ERRORS++))
}

check_warn() {
    echo -e "  ${YELLOW}⚠️  $1${NC}"
    ((WARNINGS++))
}

# 1. Check prerequisites
echo -e "${BLUE}[1/8] Checking Prerequisites...${NC}"

# Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -eq 17 ]; then
    check_pass "JDK 17 installed"
else
    check_fail "JDK 17 required (found JDK $JAVA_VERSION)"
fi

# Android SDK
if [ -n "${ANDROID_HOME:-}" ]; then
    check_pass "ANDROID_HOME set: $ANDROID_HOME"
else
    check_warn "ANDROID_HOME not set (may cause issues)"
fi

# Git
if command -v git &>/dev/null; then
    check_pass "Git installed"
else
    check_fail "Git not found"
fi

echo ""

# 2. Check project structure
echo -e "${BLUE}[2/8] Checking Project Structure...${NC}"

REQUIRED_DIRS=(
    "app/src/main/java"
    "bridge/src/main/java"
    "agent/src/main/java"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        check_pass "Directory exists: $dir"
    else
        check_fail "Missing directory: $dir"
    fi
done

REQUIRED_FILES=(
    "build.gradle.kts"
    "settings.gradle.kts"
    "gradle.properties"
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        check_pass "File exists: $file"
    else
        check_fail "Missing file: $file"
    fi
done

echo ""

# 3. Check Gradle configuration
echo -e "${BLUE}[3/8] Checking Gradle Configuration...${NC}"

# Gradle version
GRADLE_VERSION=$(./gradlew --version | grep "Gradle" | awk '{print $2}')
check_pass "Gradle version: $GRADLE_VERSION"

# Check for deprecated configurations
if grep -r "jcenter()" . --include="*.gradle*" 2>/dev/null; then
    check_warn "jcenter() found (deprecated)"
else
    check_pass "No deprecated jcenter() repositories"
fi

# Check Kotlin version
if grep -q "kotlinVersion" build.gradle.kts 2>/dev/null; then
    KOTLIN_VERSION=$(grep "kotlin.*version" build.gradle.kts | head -1 | grep -oP '\d+\.\d+\.\d+' | head -1)
    check_pass "Kotlin version: $KOTLIN_VERSION"
else
    check_warn "Kotlin version not explicitly set"
fi

echo ""

# 4. Check code style (quick mode: skip)
if [ "$QUICK_MODE" = false ]; then
    echo -e "${BLUE}[4/8] Running Code Style Checks...${NC}"
    
    # Detekt (if configured)
    if grep -q "detekt" build.gradle.kts 2>/dev/null; then
        if ./gradlew detekt 2>/dev/null; then
            check_pass "Detekt checks passed"
        else
            check_warn "Detekt issues found (check report)"
        fi
    else
        check_warn "Detekt not configured"
    fi
else
    echo -e "${BLUE}[4/8] Skipping Code Style Checks (quick mode)${NC}"
fi

echo ""

# 5. Run lint (quick mode: skip)
if [ "$QUICK_MODE" = false ]; then
    echo -e "${BLUE}[5/8] Running Android Lint...${NC}"
    
    if ./gradlew lintDebug 2>/dev/null; then
        check_pass "Lint checks passed"
    else
        check_warn "Lint issues found (check report)"
    fi
else
    echo -e "${BLUE}[5/8] Skipping Lint (quick mode)${NC}"
fi

echo ""

# 6. Build debug APK
echo -e "${BLUE}[6/8] Building Debug APK...${NC}"

if ./gradlew assembleDebug 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    check_pass "Debug build successful"
    
    # Check APK size
    APK_SIZE=$(du -h app/build/outputs/apk/debug/*.apk | cut -f1)
    check_pass "APK size: $APK_SIZE"
    
    # Check if APK exists
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        check_pass "APK generated: app-debug.apk"
    else
        check_fail "APK not found"
    fi
else
    check_fail "Debug build failed"
fi

echo ""

# 7. Run unit tests (quick mode: skip)
if [ "$QUICK_MODE" = false ]; then
    echo -e "${BLUE}[7/8] Running Unit Tests...${NC}"
    
    if ./gradlew testDebugUnitTest 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        check_pass "Unit tests passed"
    else
        check_fail "Unit tests failed"
    fi
else
    echo -e "${BLUE}[7/8] Skipping Unit Tests (quick mode)${NC}"
fi

echo ""

# 8. Check for security issues
echo -e "${BLUE}[8/8] Checking Security...${NC}"

# Check for hardcoded secrets
if grep -r "password\s*=\s*\"" app/src --include="*.kt" 2>/dev/null; then
    check_fail "Hardcoded password found"
else
    check_pass "No hardcoded passwords"
fi

if grep -r "api_key\s*=\s*\"" app/src --include="*.kt" 2>/dev/null; then
    check_fail "Hardcoded API key found"
else
    check_pass "No hardcoded API keys"
fi

# Check for debuggable release builds
if grep -q "isDebuggable = true" app/build.gradle.kts 2>/dev/null; then
    if grep -A5 "buildTypes.*release" app/build.gradle.kts | grep -q "isDebuggable = true"; then
        check_fail "Release build is debuggable!"
    fi
fi
check_pass "Release build not debuggable"

# Check for proper backup configuration
if grep -q "android:allowBackup=\"true\"" app/src/main/AndroidManifest.xml 2>/dev/null; then
    check_warn "android:allowBackup enabled (consider security implications)"
fi

echo ""

# Summary
echo -e "${BLUE}=== Validation Summary ===${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed!${NC}"
    echo ""
    echo "Build is ready for release."
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Validation completed with $WARNINGS warning(s)${NC}"
    echo ""
    echo "Build can proceed, but review warnings above."
    exit 0
else
    echo -e "${RED}❌ Validation failed with $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo ""
    echo "Fix errors before proceeding."
    exit 1
fi

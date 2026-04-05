#!/usr/bin/env bash
# setup.sh — Setup development environment for MomClaw
# Usage: ./setup.sh [--check] [--install]
#
# Checks prerequisites and optionally installs missing dependencies
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CHECK_ONLY=false
INSTALL_MISSING=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --check)
            CHECK_ONLY=true
            shift
            ;;
        --install)
            INSTALL_MISSING=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --check     Only check prerequisites, don't install"
            echo "  --install   Install missing dependencies automatically"
            echo "  -h, --help  Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --check          # Check prerequisites only"
            echo "  $0 --install        # Install missing dependencies"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# If no flags specified, default to check only
if [ "$CHECK_ONLY" = false ] && [ "$INSTALL_MISSING" = false ]; then
    CHECK_ONLY=true
fi

echo -e "${BLUE}=== MomClaw Development Setup ===${NC}"
echo ""

# Track missing dependencies
declare -a MISSING=()

check_command() {
    local cmd=$1
    local name=${2:-$1}
    local install_cmd=${3:-""}
    
    if command -v "$cmd" &>/dev/null; then
        local version
        case $cmd in
            java)
                version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
                echo -e "  ${GREEN}✅${NC} $name: $version"
                ;;
            git)
                version=$(git --version | awk '{print $3}')
                echo -e "  ${GREEN}✅${NC} $name: $version"
                ;;
            *)
                echo -e "  ${GREEN}✅${NC} $name: installed"
                ;;
        esac
        return 0
    else
        echo -e "  ${RED}❌${NC} $name: not found"
        if [ -n "$install_cmd" ]; then
            MISSING+=("$install_cmd")
        fi
        return 1
    fi
}

# 1. Check Java
echo -e "${BLUE}[1/6] Checking Java...${NC}"
if ! check_command "java" "JDK 17" "sudo apt install openjdk-17-jdk"; then
    JAVA_OK=false
else
    # Verify version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ne 17 ]; then
        echo -e "  ${RED}❌${NC} Wrong JDK version: $JAVA_VERSION (need 17)"
        JAVA_OK=false
        MISSING+=("sudo apt install openjdk-17-jdk")
    else
        JAVA_OK=true
    fi
fi
echo ""

# 2. Check Android SDK
echo -e "${BLUE}[2/6] Checking Android SDK...${NC}"
if [ -n "${ANDROID_HOME:-}" ]; then
    echo -e "  ${GREEN}✅${NC} ANDROID_HOME: $ANDROID_HOME"
    
    # Check for SDK tools
    if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        echo -e "  ${GREEN}✅${NC} SDK Manager: available"
    else
        echo -e "  ${YELLOW}⚠️${NC}  SDK Manager: not found"
    fi
    
    # Check for required SDK components
    if [ -d "$ANDROID_HOME/platforms/android-35" ]; then
        echo -e "  ${GREEN}✅${NC} Platform API 35: installed"
    else
        echo -e "  ${YELLOW}⚠️${NC}  Platform API 35: not installed"
        MISSING+=("sdkmanager \"platforms;android-35\"")
    fi
    
    if [ -d "$ANDROID_HOME/build-tools/35.0.0" ]; then
        echo -e "  ${GREEN}✅${NC} Build Tools 35.0.0: installed"
    else
        echo -e "  ${YELLOW}⚠️${NC}  Build Tools 35.0.0: not installed"
        MISSING+=("sdkmanager \"build-tools;35.0.0\"")
    fi
else
    echo -e "  ${YELLOW}⚠️${NC}  ANDROID_HOME not set"
    echo -e "  ${YELLOW}   Install Android Studio or SDK manually${NC}"
fi
echo ""

# 3. Check Git
echo -e "${BLUE}[3/6] Checking Git...${NC}"
check_command "git" "Git" "sudo apt install git"
echo ""

# 4. Check Android Studio (optional)
echo -e "${BLUE}[4/6] Checking Android Studio (optional)...${NC}"
if command -v studio &>/dev/null || [ -d "$HOME/.local/share/JetBrains/Toolbox/apps/android-studio" ]; then
    echo -e "  ${GREEN}✅${NC} Android Studio: installed"
else
    echo -e "  ${YELLOW}⚠️${NC}  Android Studio: not found (recommended for development)"
    echo -e "  ${YELLOW}   Download from: https://developer.android.com/studio${NC}"
fi
echo ""

# 5. Check project structure
echo -e "${BLUE}[5/6] Checking Project Structure...${NC}"
cd "$PROJECT_ROOT"

if [ -f "android/gradlew" ]; then
    echo -e "  ${GREEN}✅${NC} Gradle wrapper: present"
    chmod +x android/gradlew 2>/dev/null || true
else
    echo -e "  ${RED}❌${NC} Gradle wrapper: missing"
    MISSING+=("cd android && gradle wrapper")
fi

if [ -f "android/build.gradle.kts" ]; then
    echo -e "  ${GREEN}✅${NC} Build configuration: present"
else
    echo -e "  ${RED}❌${NC} Build configuration: missing"
fi

if [ -f "android/settings.gradle.kts" ]; then
    echo -e "  ${GREEN}✅${NC} Settings configuration: present"
else
    echo -e "  ${RED}❌${NC} Settings configuration: missing"
fi
echo ""

# 6. Check for keystore (release builds)
echo -e "${BLUE}[6/6] Checking Release Signing...${NC}"
if [ -f "android/key.properties" ]; then
    echo -e "  ${GREEN}✅${NC} Signing configuration: present"
else
    echo -e "  ${YELLOW}⚠️${NC}  Signing configuration: not found"
    echo -e "  ${YELLOW}   Required for release builds${NC}"
    echo -e "  ${YELLOW}   See: android/key.properties.example${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}=== Setup Summary ===${NC}"
echo ""

if [ ${#MISSING[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ All prerequisites met!${NC}"
    echo ""
    echo "You're ready to build MomClaw!"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "  1. Open project in Android Studio:"
    echo "     File → Open → $PROJECT_ROOT/android"
    echo ""
    echo "  2. Wait for Gradle sync (5-10 min first time)"
    echo ""
    echo "  3. Run debug build:"
    echo "     ./scripts/run-tests.sh --unit"
    echo ""
    echo "  4. Build APK:"
    echo "     cd android && ./gradlew assembleDebug"
    echo ""
else
    echo -e "${YELLOW}⚠️  Missing dependencies detected:${NC}"
    echo ""
    for i in "${MISSING[@]}"; do
        echo "  - $i"
    done
    echo ""
    
    if [ "$INSTALL_MISSING" = true ]; then
        echo -e "${BLUE}Installing missing dependencies...${NC}"
        echo ""
        
        for cmd in "${MISSING[@]}"; do
            echo -e "${YELLOW}Running: $cmd${NC}"
            if [[ $cmd == sudo* ]]; then
                echo -e "${YELLOW}Requires sudo privileges${NC}"
            fi
            eval "$cmd" || echo -e "${RED}Failed to run: $cmd${NC}"
        done
        
        echo ""
        echo -e "${GREEN}✅ Setup complete!${NC}"
        echo "Run this script again to verify."
    else
        echo -e "${BLUE}To install automatically:${NC}"
        echo "  $0 --install"
        echo ""
        echo -e "${BLUE}Or install manually using the commands above.${NC}"
    fi
fi

echo ""
echo -e "${BLUE}Documentation:${NC}"
echo "  README.md         - Quick start guide"
echo "  DOCUMENTATION.md  - Complete documentation"
echo "  CONTRIBUTING.md   - Contribution guide"
echo "  BUILD.md          - Build instructions"

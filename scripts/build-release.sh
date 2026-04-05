#!/usr/bin/env bash
# build-release.sh — Build release APK/AAB with signing
# Usage: ./build-release.sh [--apk] [--aab] [--version 1.0.0]
#
# Builds signed release artifacts for distribution
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

# Default values
BUILD_APK=true
BUILD_AAB=true
VERSION_NAME=""
VERSION_CODE=""
KEYSTORE_PATH=""
STORE_PASSWORD=""
KEY_ALIAS=""
KEY_PASSWORD=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --apk)
            BUILD_AAB=false
            shift
            ;;
        --aab)
            BUILD_APK=false
            shift
            ;;
        --version)
            VERSION_NAME="$2"
            shift 2
            ;;
        --version-code)
            VERSION_CODE="$2"
            shift 2
            ;;
        --keystore)
            KEYSTORE_PATH="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --apk              Build only APK"
            echo "  --aab              Build only AAB"
            echo "  --version VERSION  Set version name (e.g., 1.0.0)"
            echo "  --version-code N   Set version code"
            echo "  --keystore PATH    Path to keystore file"
            echo "  -h, --help         Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --version 1.0.0              # Build both APK and AAB"
            echo "  $0 --apk --version 1.0.0        # Build only APK"
            echo "  $0 --aab --version 1.0.0        # Build only AAB"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

cd "$ANDROID_DIR"

echo -e "${BLUE}=== MomClaw Release Build ===${NC}"
echo ""

# Ensure gradlew is executable
chmod +x gradlew 2>/dev/null || true

# Check for signing configuration
KEY_PROPERTIES="key.properties"

if [ ! -f "$KEY_PROPERTIES" ]; then
    echo -e "${YELLOW}⚠️  key.properties not found${NC}"
    echo ""
    echo "To create a signed release build:"
    echo ""
    echo "1. Generate keystore (if you don't have one):"
    echo "   keytool -genkey -v -keystore momclaw-release-key.jks \\"
    echo "       -keyalg RSA -keysize 2048 -validity 10000 -alias momclaw"
    echo ""
    echo "2. Create key.properties in android/ directory:"
    echo "   storePassword=YOUR_PASSWORD"
    echo "   keyPassword=YOUR_KEY_PASSWORD"
    echo "   keyAlias=momclaw"
    echo "   storeFile=../momclaw-release-key.jks"
    echo ""
    echo "3. Run this script again"
    echo ""
    echo -e "${YELLOW}Building UNSIGNED release (not suitable for distribution)${NC}"
    echo ""
    
    read -p "Continue with unsigned build? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    
    UNSIGNED=true
else
    UNSIGNED=false
    echo -e "${GREEN}✅ key.properties found${NC}"
fi

# Set version if provided
if [ -n "$VERSION_NAME" ]; then
    echo -e "${BLUE}Setting version: $VERSION_NAME${NC}"
    # Note: Version update would require editing build.gradle.kts or using -P flags
fi

# Clean build
echo -e "${BLUE}Cleaning previous build...${NC}"
./gradlew clean

echo ""

# Build release APK
if [ "$BUILD_APK" = true ]; then
    echo -e "${BLUE}=== Building Release APK ===${NC}"
    
    BUILD_ARGS="assembleRelease"
    
    if [ -n "$VERSION_NAME" ]; then
        BUILD_ARGS="$BUILD_ARGS -PversionName=$VERSION_NAME"
    fi
    
    if [ -n "$VERSION_CODE" ]; then
        BUILD_ARGS="$BUILD_ARGS -PversionCode=$VERSION_CODE"
    fi
    
    ./gradlew $BUILD_ARGS
    
    if [ $? -eq 0 ]; then
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        
        if [ -f "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            echo -e "${GREEN}✅ Release APK built successfully${NC}"
            echo "   Path: $APK_PATH"
            echo "   Size: $APK_SIZE"
            
            # Rename with version if provided
            if [ -n "$VERSION_NAME" ]; then
                RENAMED_APK="app/build/outputs/apk/release/momclaw-$VERSION_NAME.apk"
                cp "$APK_PATH" "$RENAMED_APK"
                echo "   Renamed: $RENAMED_APK"
            fi
            
            # Verify signature if signed
            if [ "$UNSIGNED" = false ]; then
                echo ""
                echo -e "${BLUE}Verifying APK signature...${NC}"
                if jarsigner -verify -verbose -certs "$APK_PATH" 2>&1 | grep -q "jar verified"; then
                    echo -e "${GREEN}✅ APK signature verified${NC}"
                else
                    echo -e "${RED}❌ APK signature verification failed${NC}"
                fi
            fi
        else
            echo -e "${RED}❌ APK not found at $APK_PATH${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ APK build failed${NC}"
        exit 1
    fi
    
    echo ""
fi

# Build release AAB
if [ "$BUILD_AAB" = true ]; then
    echo -e "${BLUE}=== Building Release AAB ===${NC}"
    
    BUILD_ARGS="bundleRelease"
    
    if [ -n "$VERSION_NAME" ]; then
        BUILD_ARGS="$BUILD_ARGS -PversionName=$VERSION_NAME"
    fi
    
    if [ -n "$VERSION_CODE" ]; then
        BUILD_ARGS="$BUILD_ARGS -PversionCode=$VERSION_CODE"
    fi
    
    ./gradlew $BUILD_ARGS
    
    if [ $? -eq 0 ]; then
        AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
        
        if [ -f "$AAB_PATH" ]; then
            AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
            echo -e "${GREEN}✅ Release AAB built successfully${NC}"
            echo "   Path: $AAB_PATH"
            echo "   Size: $AAB_SIZE"
            
            # Rename with version if provided
            if [ -n "$VERSION_NAME" ]; then
                RENAMED_AAB="app/build/outputs/bundle/release/momclaw-$VERSION_NAME.aab"
                cp "$AAB_PATH" "$RENAMED_AAB"
                echo "   Renamed: $RENAMED_AAB"
            fi
        else
            echo -e "${RED}❌ AAB not found at $AAB_PATH${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ AAB build failed${NC}"
        exit 1
    fi
    
    echo ""
fi

# Summary
echo -e "${GREEN}=== Build Complete ===${NC}"
echo ""

if [ "$UNSIGNED" = true ]; then
    echo -e "${YELLOW}⚠️  UNSIGNED BUILD${NC}"
    echo "This build is not suitable for production distribution."
    echo "Configure signing to create a signed release build."
else
    echo -e "${GREEN}✅ SIGNED BUILD${NC}"
    echo "Ready for distribution!"
fi

echo ""
echo -e "${BLUE}Artifacts:${NC}"
if [ "$BUILD_APK" = true ]; then
    echo "  APK: app/build/outputs/apk/release/"
fi
if [ "$BUILD_AAB" = true ]; then
    echo "  AAB: app/build/outputs/bundle/release/"
fi

echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Test the APK on multiple devices"
echo "  2. Upload AAB to Google Play Console (if applicable)"
echo "  3. Create GitHub release with artifacts"
echo "  4. Update CHANGELOG.md"

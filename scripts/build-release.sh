#!/bin/bash

# MomClaw Release Build Script
# Builds signed APK and AAB for Google Play distribution

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Version
VERSION=$1
BUILD_NUMBER=${2:-1}

if [ -z "$VERSION" ]; then
    echo -e "${RED}Error: Version not specified${NC}"
    echo "Usage: $0 <version> [build_number]"
    echo "Example: $0 1.0.0 1"
    exit 1
fi

echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}MomClaw Release Build v$VERSION (build $BUILD_NUMBER)${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found. Please install JDK 17+${NC}"
    exit 1
fi

if [ ! -f "android/gradlew" ]; then
    echo -e "${RED}Error: android/gradlew not found. Run from momclaw root.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites OK${NC}"
echo

# Calculate version code (e.g., 1.0.0 -> 1000000, 1.0.1 -> 1000001)
VERSION_CODE=$(echo $VERSION | tr -d '.')$BUILD_NUMBER
echo -e "${BLUE}Version Code: $VERSION_CODE${NC}"
echo

# Clean build
echo -e "${YELLOW}Cleaning previous builds...${NC}"
./android/gradlew clean
rm -f momclaw-*.apk momclaw-*.aab
echo -e "${GREEN}✓ Clean complete${NC}"
echo

# Check keystore
KEYSTORE="momclaw-release-key.jks"
if [ ! -f "$KEYSTORE" ]; then
    echo -e "${RED}Error: Keystore not found at $KEYSTORE${NC}"
    echo -e "${YELLOW}Generate a keystore first:${NC}"
    echo "  keytool -genkey -v -keystore $KEYSTORE \\"
    echo "    -keyalg RSA -keysize 2048 -validity 10000 -alias momclaw"
    exit 1
fi

echo -e "${GREEN}✓ Keystore found${NC}"
echo

# Read credentials
KEY_ALIAS="momclaw"
KEY_PASS=""
STORE_PASS=""

# Check for key.properties
if [ -f "android/key.properties" ]; then
    echo -e "${YELLOW}Reading credentials from key.properties${NC}"
    while IFS='=' read -r key value; do
        case $key in
            keyAlias) KEY_ALIAS="$value" ;;
            keyPassword) KEY_PASS="$value" ;;
            storePassword) STORE_PASS="$value" ;;
        esac
    done < android/key.properties
fi

# Prompt if not found
if [ -z "$STORE_PASS" ]; then
    echo -e "${YELLOW}Keystore password required${NC}"
    read -s -p "Enter keystore password: " STORE_PASS
    echo
fi

if [ -z "$KEY_PASS" ]; then
    KEY_PASS=$STORE_PASS
fi

echo

# Build release APK
echo -e "${YELLOW}Building release APK...${NC}"
./android/gradlew assembleRelease \
    -PversionName=$VERSION \
    -PversionCode=$VERSION_CODE

if [ ! -f "android/app/build/outputs/apk/release/app-release.apk" ]; then
    echo -e "${RED}Error: APK build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ APK built successfully${NC}"
echo

# Build release AAB
echo -e "${YELLOW}Building release AAB...${NC}"
./android/gradlew bundleRelease \
    -PversionName=$VERSION \
    -PversionCode=$VERSION_CODE

if [ ! -f "android/app/build/outputs/bundle/release/app-release.aab" ]; then
    echo -e "${RED}Error: AAB build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ AAB built successfully${NC}"
echo

# Verify signatures
echo -e "${YELLOW}Verifying APK signature...${NC}"
jarsigner -verify -verbose -certs \
    android/app/build/outputs/apk/release/app-release.apk

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: APK signature verification failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ APK signature verified${NC}"
echo

# Rename artifacts
echo -e "${YELLOW}Renaming artifacts...${NC}"

cp android/app/build/outputs/apk/release/app-release.apk \
   momclaw-$VERSION.apk

cp android/app/build/outputs/bundle/release/app-release.aab \
   momclaw-$VERSION.aab

echo -e "${GREEN}✓ Artifacts renamed${NC}"
echo

# Generate checksums
echo -e "${YELLOW}Generating checksums...${NC}"

sha256sum momclaw-$VERSION.apk > momclaw-$VERSION.apk.sha256
sha256sum momclaw-$VERSION.aab > momclaw-$VERSION.aab.sha256

echo -e "${GREEN}✓ Checksums generated${NC}"
echo

# Summary
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${GREEN}Release build complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo
echo "Files:"
echo "  - momclaw-$VERSION.apk ($(du -h momclaw-$VERSION.apk | cut -f1))"
echo "  - momclaw-$VERSION.aab ($(du -h momclaw-$VERSION.aab | cut -f1))"
echo "  - momclaw-$VERSION.apk.sha256"
echo "  - momclaw-$VERSION.aab.sha256"
echo
echo "Next steps:"
echo "  1. Test APK: adb install momclaw-$VERSION.apk"
echo "  2. Upload to Google Play:"
echo "     cd android && fastlane internal"
echo "  3. Create GitHub release:"
echo "     gh release create v$VERSION momclaw-$VERSION.apk momclaw-$VERSION.aab"
echo
echo -e "${GREEN}✓ Done${NC}"

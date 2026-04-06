#!/bin/bash

# MomClaw F-Droid Build Script
# Builds unsigned APK and signs with GPG for F-Droid distribution

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Version
VERSION=$1

if [ -z "$VERSION" ]; then
    echo -e "${RED}Error: Version not specified${NC}"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}MomClaw F-Droid Build v$VERSION${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found. Please install JDK 17+${NC}"
    exit 1
fi

if ! command -v gpg &> /dev/null; then
    echo -e "${RED}Error: GPG not found. Please install gnupg${NC}"
    exit 1
fi

if [ ! -f "android/gradlew" ]; then
    echo -e "${RED}Error: android/gradlew not found. Run from momclaw root.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites OK${NC}"
echo

# Calculate version code
VERSION_CODE=$(echo $VERSION | tr -d '.')
echo -e "${BLUE}Version Code: $VERSION_CODE${NC}"
echo

# Clean build
echo -e "${YELLOW}Cleaning previous builds...${NC}"
./android/gradlew clean
rm -f momclaw-*-fdroid.apk momclaw-*-fdroid.apk.asc
echo -e "${GREEN}✓ Clean complete${NC}"
echo

# Build unsigned APK
echo -e "${YELLOW}Building unsigned APK...${NC}"
./android/gradlew assembleRelease \
    -PversionName=$VERSION \
    -PversionCode=$VERSION_CODE

if [ ! -f "android/app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo -e "${RED}Error: APK build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ APK built successfully${NC}"
echo

# Check if keystore exists
KEYSTORE="momclaw-release-key.jks"
if [ ! -f "$KEYSTORE" ]; then
    echo -e "${YELLOW}Warning: Keystore not found at $KEYSTORE${NC}"
    echo -e "${YELLOW}Building unsigned APK only (for F-Droid build server)${NC}"
    
    cp android/app/build/outputs/apk/release/app-release-unsigned.apk \
       momclaw-$VERSION-fdroid-unsigned.apk
    
    echo -e "${GREEN}✓ Unsigned APK: momclaw-$VERSION-fdroid-unsigned.apk${NC}"
    
    # GPG sign
    echo -e "${YELLOW}Signing with GPG...${NC}"
    gpg --armor --detach-sign momclaw-$VERSION-fdroid-unsigned.apk
    
    if [ -f "momclaw-$VERSION-fdroid-unsigned.apk.asc" ]; then
        echo -e "${GREEN}✓ GPG signature: momclaw-$VERSION-fdroid-unsigned.apk.asc${NC}"
    fi
    
    echo
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${GREEN}F-Droid build complete!${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo
    echo "Files:"
    echo "  - momclaw-$VERSION-fdroid-unsigned.apk"
    echo "  - momclaw-$VERSION-fdroid-unsigned.apk.asc"
    echo
    echo "Next steps for F-Droid:"
    echo "  1. Submit metadata YAML to fdroiddata"
    echo "  2. Tag release: git tag -a v$VERSION-fdroid -m 'F-Droid Release'"
    echo "  3. Push tag: git push origin v$VERSION-fdroid"
    exit 0
fi

# Sign with jarsigner (for self-hosted F-Droid repo)
echo -e "${YELLOW}Signing APK with keystore...${NC}"

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

# Sign
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore $KEYSTORE \
    -storepass $STORE_PASS \
    -keypass $KEY_PASS \
    -signedjar momclaw-$VERSION-fdroid.apk \
    android/app/build/outputs/apk/release/app-release-unsigned.apk \
    $KEY_ALIAS

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: APK signing failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ APK signed${NC}"
echo

# Verify
echo -e "${YELLOW}Verifying signature...${NC}"
jarsigner -verify -verbose -certs momclaw-$VERSION-fdroid.apk

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Signature verification failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Signature verified${NC}"
echo

# GPG sign
echo -e "${YELLOW}Signing with GPG...${NC}"
gpg --armor --detach-sign momclaw-$VERSION-fdroid.apk

if [ -f "momclaw-$VERSION-fdroid.apk.asc" ]; then
    echo -e "${GREEN}✓ GPG signature: momclaw-$VERSION-fdroid.apk.asc${NC}"
else
    echo -e "${YELLOW}Warning: GPG signing failed (optional for self-hosted repos)${NC}"
fi

echo

# Summary
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${GREEN}F-Droid build complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo
echo "Files:"
echo "  - momclaw-$VERSION-fdroid.apk ($(du -h momclaw-$VERSION-fdroid.apk | cut -f1))"
if [ -f "momclaw-$VERSION-fdroid.apk.asc" ]; then
    echo "  - momclaw-$VERSION-fdroid.apk.asc"
fi
echo
echo "Next steps for F-Droid:"
echo "  1. Test APK on device: adb install momclaw-$VERSION-fdroid.apk"
echo "  2. Upload to self-hosted repo or submit to fdroiddata"
echo "  3. Update metadata YAML with new version"
echo
echo -e "${GREEN}✓ Done${NC}"

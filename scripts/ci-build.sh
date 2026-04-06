#!/bin/bash

# MomClaw CI/CD Script
# Main automation script for all build/deploy operations

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
show_banner() {
    echo -e "${CYAN}"
    cat << "EOF"
  __  __       _   _
 |  \/  | ___ | |_| |__   ___ _ __
 | |\/| |/ _ \| __| '_ \ / _ \ '__|
 | |  | | (_) | |_| | | |  __/ |
 |_|  |_|\___/ \__|_| |_|\___|_|

  Mobile Offline Model Agent
  AI Agent 100% offline pe Android
EOF
    echo -e "${NC}"
}

# Help
show_help() {
    show_banner
    echo
    echo "Usage: $0 <command> [options]"
    echo
    echo "Commands:"
    echo
    echo -e "${GREEN}Build${NC}"
    echo "  build:debug              Build debug APK"
    echo "  build:release <version>  Build release APK + AAB"
    echo "  build:fdroid <version>   Build F-Droid APK"
    echo
    echo -e "${GREEN}Test${NC}"
    echo "  test:unit                Run unit tests"
    echo "  test:instrumented        Run instrumented tests (requires device)"
    echo "  test:all                 Run all tests + lint"
    echo "  test:coverage            Run tests with coverage report"
    echo
    echo -e "${GREEN}Quality${NC}"
    echo "  lint                     Run Android lint"
    echo "  detekt                   Run Kotlin static analysis"
    echo "  validate                 Full validation (lint + tests)"
    echo
    echo -e "${GREEN}Deploy${NC}"
    echo "  deploy:internal          Deploy to Google Play Internal"
    echo "  deploy:alpha             Deploy to Google Play Alpha"
    echo "  deploy:beta              Deploy to Google Play Beta"
    echo "  deploy:production        Deploy to Google Play Production"
    echo "  deploy:github <version>  Create GitHub release"
    echo
    echo -e "${GREEN}Fastlane${NC}"
    echo "  fastlane <command>       Run fastlane command"
    echo "  fastlane:list            List available fastlane lanes"
    echo
    echo -e "${GREEN}Utility${NC}"
    echo "  clean                    Clean build artifacts"
    echo "  keystore:generate        Generate signing keystore"
    echo "  model:download           Download AI model"
    echo "  help                     Show this help message"
    echo
    echo "Examples:"
    echo "  $0 build:release 1.0.0"
    echo "  $0 test:all"
    echo "  $0 deploy:internal"
    echo "  $0 fastlane promote_alpha_to_beta"
    echo
}

# Check if in correct directory
check_directory() {
    if [ ! -f "android/gradlew" ]; then
        echo -e "${RED}Error: Run this script from momclaw root directory${NC}"
        exit 1
    fi
}

# Ensure gradlew is executable
ensure_gradlew() {
    chmod +x android/gradlew
}

# Command handlers
build_debug() {
    echo -e "${BLUE}Building debug APK...${NC}"
    ensure_gradlew
    ./android/gradlew assembleDebug
    echo -e "${GREEN}✓ Debug APK built${NC}"
    echo "Location: android/app/build/outputs/apk/debug/app-debug.apk"
}

build_release() {
    local version=$1
    
    if [ -z "$version" ]; then
        echo -e "${RED}Error: Version not specified${NC}"
        echo "Usage: $0 build:release <version>"
        exit 1
    fi
    
    ./scripts/build-release.sh "$version"
}

build_fdroid() {
    local version=$1
    
    if [ -z "$version" ]; then
        echo -e "${RED}Error: Version not specified${NC}"
        echo "Usage: $0 build:fdroid <version>"
        exit 1
    fi
    
    ./scripts/build-fdroid.sh "$version"
}

test_unit() {
    echo -e "${BLUE}Running unit tests...${NC}"
    ensure_gradlew
    ./android/gradlew testDebugUnitTest
    echo -e "${GREEN}✓ Unit tests passed${NC}"
}

test_instrumented() {
    echo -e "${BLUE}Running instrumented tests...${NC}"
    
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}Error: No Android device connected${NC}"
        exit 1
    fi
    
    ensure_gradlew
    ./android/gradlew connectedAndroidTest
    echo -e "${GREEN}✓ Instrumented tests passed${NC}"
}

test_all() {
    ./scripts/run-tests.sh
}

test_coverage() {
    ./scripts/run-tests.sh --coverage
}

run_lint() {
    echo -e "${BLUE}Running lint...${NC}"
    ensure_gradlew
    ./android/gradlew lint
    echo -e "${GREEN}✓ Lint passed${NC}"
    echo "Report: android/app/build/reports/lint-results.html"
}

run_detekt() {
    echo -e "${BLUE}Running detekt...${NC}"
    ensure_gradlew
    ./android/gradlew detekt
    echo -e "${GREEN}✓ Detekt passed${NC}"
}

validate() {
    ./scripts/validate-build.sh
}

deploy_google_play() {
    local track=$1
    
    if [ -z "$track" ]; then
        echo -e "${RED}Error: Track not specified${NC}"
        exit 1
    fi
    
    check_directory
    
    # Check if fastlane is installed
    if ! command -v fastlane &> /dev/null; then
        echo -e "${RED}Error: fastlane not installed${NC}"
        echo "Install with: gem install fastlane"
        exit 1
    fi
    
    # Check for service account
    if [ ! -f "android/google-play-service-account.json" ]; then
        echo -e "${RED}Error: google-play-service-account.json not found${NC}"
        echo "Download from Google Play Console > Setup > API access"
        exit 1
    fi
    
    echo -e "${BLUE}Deploying to Google Play $track...${NC}"
    cd android
    fastlane "$track"
    echo -e "${GREEN}✓ Deployed to $track${NC}"
}

deploy_github() {
    local version=$1
    
    if [ -z "$version" ]; then
        echo -e "${RED}Error: Version not specified${NC}"
        echo "Usage: $0 deploy:github <version>"
        exit 1
    fi
    
    check_directory
    
    # Check if gh CLI is installed
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}Error: GitHub CLI not installed${NC}"
        echo "Install from: https://cli.github.com/"
        exit 1
    fi
    
    echo -e "${BLUE}Creating GitHub release v$version...${NC}"
    cd android
    fastlane github_release version:"$version"
    echo -e "${GREEN}✓ GitHub release created${NC}"
}

run_fastlane() {
    check_directory
    
    if ! command -v fastlane &> /dev/null; then
        echo -e "${RED}Error: fastlane not installed${NC}"
        exit 1
    fi
    
    local command=$1
    
    if [ -z "$command" ]; then
        echo -e "${RED}Error: Fastlane command not specified${NC}"
        echo "Usage: $0 fastlane <command>"
        exit 1
    fi
    
    cd android
    fastlane "$command"
}

list_fastlane_lanes() {
    echo -e "${BLUE}Available Fastlane lanes:${NC}"
    check_directory
    cd android
    fastlane lanes
}

clean_build() {
    echo -e "${BLUE}Cleaning build artifacts...${NC}"
    ensure_gradlew
    ./android/gradlew clean
    rm -rf momclaw-*.apk momclaw-*.aab
    echo -e "${GREEN}✓ Cleaned${NC}"
}

generate_keystore() {
    echo -e "${BLUE}Generating signing keystore...${NC}"
    
    if [ -f "momclaw-release-key.jks" ]; then
        echo -e "${YELLOW}Warning: Keystore already exists${NC}"
        read -p "Overwrite? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi
    
    keytool -genkey -v -keystore momclaw-release-key.jks \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias momclaw
    
    echo -e "${GREEN}✓ Keystore generated${NC}"
    echo -e "${YELLOW}Important: Backup this keystore securely!${NC}"
    echo "Location: momclaw-release-key.jks"
}

download_model() {
    echo -e "${BLUE}Downloading Gemma 3 E4B-it model...${NC}"
    
    if [ ! -f "scripts/download-model.sh" ]; then
        echo -e "${RED}Error: scripts/download-model.sh not found${NC}"
        exit 1
    fi
    
    ./scripts/download-model.sh ./models
}

# Main
case "$1" in
    build:debug)
        check_directory
        build_debug
        ;;
    build:release)
        check_directory
        build_release "$2"
        ;;
    build:fdroid)
        check_directory
        build_fdroid "$2"
        ;;
    test:unit)
        check_directory
        test_unit
        ;;
    test:instrumented)
        check_directory
        test_instrumented
        ;;
    test:all)
        check_directory
        test_all
        ;;
    test:coverage)
        check_directory
        test_coverage
        ;;
    lint)
        check_directory
        run_lint
        ;;
    detekt)
        check_directory
        run_detekt
        ;;
    validate)
        check_directory
        validate
        ;;
    deploy:internal)
        deploy_google_play "internal"
        ;;
    deploy:alpha)
        deploy_google_play "alpha"
        ;;
    deploy:beta)
        deploy_google_play "beta"
        ;;
    deploy:production)
        deploy_google_play "production"
        ;;
    deploy:github)
        deploy_github "$2"
        ;;
    fastlane)
        run_fastlane "$2"
        ;;
    fastlane:list)
        list_fastlane_lanes
        ;;
    clean)
        check_directory
        clean_build
        ;;
    keystore:generate)
        generate_keystore
        ;;
    model:download)
        download_model
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$1'${NC}"
        echo "Run '$0 help' for usage information"
        exit 1
        ;;
esac

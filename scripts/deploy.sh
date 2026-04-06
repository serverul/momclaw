#!/bin/bash
# MOMCLAW Deployment Automation Script
# Handles Google Play, F-Droid, and GitHub releases

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"
VERSION="${1:-}"
BUILD_TYPE="${2:-release}"
DEPLOY_TARGET="${3:-github}"  # github, play, fdroid, all

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[✓]${NC} $*"; }
log_warning() { echo -e "${YELLOW}[⚠]${NC} $*"; }
log_error() { echo -e "${RED}[✗]${NC} $*"; exit 1; }

# Usage
usage() {
    cat <<EOF
MOMCLAW Deployment Script

Usage: $0 <version> [build_type] [deploy_target]

Arguments:
    version         Version number (e.g., 1.0.0, 1.1.0-beta)
    build_type      Build type: release (default), debug
    deploy_target   Deployment target:
                    - github: GitHub release only (default)
                    - play: Google Play Store
                    - fdroid: F-Droid build
                    - all: All targets

Examples:
    $0 1.0.0                    # Deploy v1.0.0 to GitHub
    $0 1.1.0 release play       # Deploy v1.1.0 to Google Play
    $0 2.0.0-beta release all   # Deploy v2.0.0-beta everywhere

Prerequisites:
    - For GitHub: GitHub CLI (gh) installed and authenticated
    - For Play Store: Fastlane configured + service account
    - For F-Droid: GPG key configured

EOF
    exit 1
}

# Validation
validate_version() {
    if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
        log_error "Invalid version format: $VERSION (expected: X.Y.Z or X.Y.Z-suffix)"
    fi
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install JDK 17+"
    fi
    
    # Check Gradle wrapper
    if [[ ! -x "$ANDROID_DIR/gradlew" ]]; then
        log_info "Making gradlew executable..."
        chmod +x "$ANDROID_DIR/gradlew"
    fi
    
    # Check deployment-specific tools
    case "$DEPLOY_TARGET" in
        github)
            if ! command -v gh &> /dev/null; then
                log_error "GitHub CLI (gh) not found. Install from: https://cli.github.com"
            fi
            ;;
        play)
            if ! command -v fastlane &> /dev/null; then
                log_error "Fastlane not found. Install: gem install fastlane"
            fi
            if [[ ! -f "$ANDROID_DIR/key.properties" ]]; then
                log_error "key.properties not found. Create signing config first."
            fi
            ;;
        fdroid)
            if ! command -v gpg &> /dev/null; then
                log_error "GPG not found. Required for F-Droid signing."
            fi
            ;;
        all)
            if ! command -v gh &> /dev/null; then
                log_warning "GitHub CLI not found - GitHub release will be skipped"
            fi
            if ! command -v fastlane &> /dev/null; then
                log_warning "Fastlane not found - Play Store deployment will be skipped"
            fi
            if ! command -v gpg &> /dev/null; then
                log_warning "GPG not found - F-Droid signing will be skipped"
            fi
            ;;
    esac
    
    log_success "Prerequisites OK"
}

# Build functions
build_apk() {
    log_info "Building APK ($BUILD_TYPE)..."
    
    cd "$ANDROID_DIR"
    
    # Clean previous builds
    ./gradlew clean
    
    # Build APK
    if [[ "$BUILD_TYPE" == "release" ]]; then
        ./gradlew assembleRelease \
            -PversionName="$VERSION" \
            -PversionCode=$(git rev-list --count HEAD)
    else
        ./gradlew assembleDebug
    fi
    
    # Find and copy APK
    local apk_pattern="app/build/outputs/apk/${BUILD_TYPE}/*.apk"
    local apk_file=$(ls -t $apk_pattern 2>/dev/null | head -1)
    
    if [[ -z "$apk_file" ]]; then
        log_error "APK not found after build"
    fi
    
    local output_name="MOMCLAW-$VERSION.apk"
    cp "$apk_file" "$PROJECT_ROOT/$output_name"
    
    log_success "APK built: $output_name"
    echo "$output_name"
}

build_aab() {
    if [[ "$BUILD_TYPE" != "release" ]]; then
        log_info "Skipping AAB for debug builds"
        return 0
    fi
    
    log_info "Building Android App Bundle (AAB)..."
    
    cd "$ANDROID_DIR"
    ./gradlew bundleRelease \
        -PversionName="$VERSION" \
        -PversionCode=$(git rev-list --count HEAD)
    
    # Find and copy AAB
    local aab_file="app/build/outputs/bundle/release/app-release.aab"
    if [[ ! -f "$aab_file" ]]; then
        log_error "AAB not found after build"
    fi
    
    local output_name="MOMCLAW-$VERSION.aab"
    cp "$aab_file" "$PROJECT_ROOT/$output_name"
    
    log_success "AAB built: $output_name"
    echo "$output_name"
}

# Deployment functions
deploy_github() {
    log_info "Creating GitHub release..."
    
    local apk_file="$PROJECT_ROOT/MOMCLAW-$VERSION.apk"
    local aab_file="$PROJECT_ROOT/MOMCLAW-$VERSION.aab"
    
    if [[ ! -f "$apk_file" ]]; then
        log_error "APK not found: $apk_file"
    fi
    
    # Generate changelog
    local changelog=""
    local prev_tag=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")
    
    if [[ -n "$prev_tag" ]]; then
        changelog=$(git log --pretty=format:"- %s (%h)" $prev_tag..HEAD)
    else
        changelog="Initial release"
    fi
    
    # Create release
    local release_title="MOMCLAW v$VERSION"
    local prerelease_flag=""
    
    if [[ "$VERSION" =~ -(alpha|beta|rc) ]]; then
        prerelease_flag="--prerelease"
    fi
    
    gh release create "v$VERSION" \
        "$apk_file" \
        ${aab_file:+$aab_file} \
        --title "$release_title" \
        --notes "## MOMCLAW v$VERSION

### 🚀 Changes
$changelog

### 📦 Downloads
- **APK**: \`MOMCLAW-$VERSION.apk\`
$(if [[ -f "$aab_file" ]]; then echo "- **AAB**: \`MOMCLAW-$VERSION.aab\`"; fi)

### 📋 System Requirements
- Android 9.0 (API 28) or higher
- 4GB+ RAM recommended
- 3GB+ free storage for model

### 🔧 Installation
1. Download the APK
2. Enable \"Install from unknown sources\" in settings
3. Install the APK
4. Download the model using in-app downloader

Full documentation: [DOCUMENTATION.md](DOCUMENTATION.md)" \
        $prerelease_flag
    
    log_success "GitHub release created: https://github.com/serverul/MOMCLAW/releases/tag/v$VERSION"
}

deploy_play_store() {
    log_info "Deploying to Google Play Store..."
    
    local track="internal"  # Default to internal testing
    
    if [[ "$VERSION" =~ -alpha ]]; then
        track="alpha"
    elif [[ "$VERSION" =~ -beta ]]; then
        track="beta"
    elif [[ ! "$VERSION" =~ -(alpha|beta|rc) ]]; then
        # Production release - but start with internal for safety
        track="internal"
        log_warning "Production version detected. Deploying to Internal Testing first."
        log_warning "Use Fastlane to promote: fastlane promote_internal_to_alpha"
    fi
    
    cd "$ANDROID_DIR"
    
    fastlane "$track" version:"$VERSION"
    
    log_success "Deployed to Google Play ($track track)"
}

deploy_fdroid() {
    log_info "Building F-Droid APK..."
    
    cd "$SCRIPT_DIR"
    ./build-fdroid.sh "$VERSION"
    
    log_success "F-Droid APK ready: MOMCLAW-$VERSION-fdroid.apk"
}

# Main
main() {
    if [[ -z "$VERSION" ]]; then
        usage
    fi
    
    validate_version
    check_prerequisites
    
    log_info "====================================="
    log_info "MOMCLAW Deployment v$VERSION"
    log_info "Build Type: $BUILD_TYPE"
    log_info "Target: $DEPLOY_TARGET"
    log_info "====================================="
    
    # Build artifacts
    local apk=$(build_apk)
    local aab=""
    
    if [[ "$BUILD_TYPE" == "release" ]]; then
        aab=$(build_aab)
    fi
    
    # Deploy to target(s)
    case "$DEPLOY_TARGET" in
        github)
            deploy_github
            ;;
        play)
            deploy_play_store
            ;;
        fdroid)
            deploy_fdroid
            ;;
        all)
            if command -v gh &> /dev/null; then
                deploy_github
            fi
            if command -v fastlane &> /dev/null && [[ -f "$ANDROID_DIR/key.properties" ]]; then
                deploy_play_store
            fi
            if command -v gpg &> /dev/null; then
                deploy_fdroid
            fi
            ;;
    esac
    
    log_success "====================================="
    log_success "Deployment Complete!"
    log_success "====================================="
}

main "$@"

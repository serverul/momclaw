#!/bin/bash
#
# MOMCLAW Optimized Build Script
# Usage: ./build-optimized.sh [debug|release|clean|test|all]
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_ROOT/android"
GRADLE="$ANDROID_DIR/gradlew"

# Build timing
START_TIME=$(date +%s)

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_duration() {
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))
    echo -e "${GREEN}✓${NC} Completed in ${minutes}m ${seconds}s"
}

check_gradle() {
    if [ ! -f "$GRADLE" ]; then
        log_error "Gradle wrapper not found at $GRADLE"
        exit 1
    fi
    
    # Make gradlew executable
    chmod +x "$GRADLE"
}

build_debug() {
    log_info "Building Debug APK..."
    cd "$ANDROID_DIR"
    "$GRADLE" assembleDebug \
        --parallel \
        --build-cache \
        --configuration-cache \
        --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "Debug APK built successfully!"
        log_info "APK location: $ANDROID_DIR/app/build/outputs/apk/debug/"
        show_duration
    else
        log_error "Debug build failed!"
        exit 1
    fi
}

build_release() {
    log_info "Building Release APK..."
    
    # Check for keystore
    if [ ! -f "$ANDROID_DIR/keystore.jks" ] && [ ! -f "$ANDROID_DIR/key.properties" ]; then
        log_warning "No keystore found. Building unsigned release..."
        UNSIGNED="-unsigned"
    else
        UNSIGNED=""
    fi
    
    cd "$ANDROID_DIR"
    "$GRADLE" assembleRelease \
        --parallel \
        --build-cache \
        --configuration-cache \
        --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "Release APK built successfully!"
        log_info "APK location: $ANDROID_DIR/app/build/outputs/apk/release/"
        show_duration
    else
        log_error "Release build failed!"
        exit 1
    fi
}

build_bundle() {
    log_info "Building Android App Bundle (AAB)..."
    cd "$ANDROID_DIR"
    "$GRADLE" bundleRelease \
        --parallel \
        --build-cache \
        --configuration-cache \
        --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "AAB built successfully!"
        log_info "AAB location: $ANDROID_DIR/app/build/outputs/bundle/release/"
        show_duration
    else
        log_error "AAB build failed!"
        exit 1
    fi
}

clean_build() {
    log_info "Cleaning build artifacts..."
    cd "$ANDROID_DIR"
    "$GRADLE" clean --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "Clean completed!"
        show_duration
    else
        log_error "Clean failed!"
        exit 1
    fi
}

run_tests() {
    log_info "Running unit tests..."
    cd "$ANDROID_DIR"
    "$GRADLE" testDebugUnitTest \
        --parallel \
        --build-cache \
        --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "Unit tests passed!"
        log_info "Test reports: $ANDROID_DIR/app/build/reports/tests/"
        show_duration
    else
        log_error "Tests failed!"
        exit 1
    fi
}

run_lint() {
    log_info "Running lint checks..."
    cd "$ANDROID_DIR"
    "$GRADLE" lintDebug \
        --parallel \
        --build-cache \
        --console=plain
    
    if [ $? -eq 0 ]; then
        log_success "Lint checks passed!"
        log_info "Lint report: $ANDROID_DIR/app/build/reports/lint-results-debug.html"
        show_duration
    else
        log_warning "Lint found issues (check report)"
    fi
}

analyze_apk() {
    log_info "Analyzing APK size..."
    cd "$ANDROID_DIR"
    "$GRADLE" app:analyzeReleaseBundle --console=plain || true
    
    log_info "APK analysis complete!"
}

show_help() {
    echo "MOMCLAW Build Script"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  debug       Build debug APK"
    echo "  release     Build release APK"
    echo "  bundle      Build Android App Bundle (AAB)"
    echo "  test        Run unit tests"
    echo "  lint        Run lint checks"
    echo "  clean       Clean build artifacts"
    echo "  analyze     Analyze APK size"
    echo "  all         Run clean, lint, test, and build"
    echo "  help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 debug              # Fast debug build"
    echo "  $0 release            # Optimized release build"
    echo "  $0 all                # Full CI/CD pipeline"
    echo ""
}

build_all() {
    log_info "Running full build pipeline..."
    clean_build
    run_lint
    run_tests
    build_debug
    build_release
    analyze_apk
    log_success "Full build pipeline completed!"
}

# Main execution
check_gradle

case "${1:-help}" in
    debug)
        build_debug
        ;;
    release)
        build_release
        ;;
    bundle)
        build_bundle
        ;;
    clean)
        clean_build
        ;;
    test)
        run_tests
        ;;
    lint)
        run_lint
        ;;
    analyze)
        analyze_apk
        ;;
    all)
        build_all
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac

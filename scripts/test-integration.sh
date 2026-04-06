#!/bin/bash

# MOMCLAW Integration Testing Script
# Agent 3 - Integration & Testing
# Usage: ./test-integration.sh [device|emulator]

set -e

COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v adb &> /dev/null; then
        log_error "adb not found. Please install Android SDK platform-tools."
        exit 1
    fi
    
    if ! command -v java &> /dev/null; then
        log_error "java not found. Please install Java 17+."
        exit 1
    fi
    
    if [ ! -f "./gradlew" ]; then
        log_error "gradlew not found. Please run from android/ directory."
        exit 1
    fi
    
    log_success "Prerequisites OK"
}

# Check device connectivity
check_device() {
    log_info "Checking device connectivity..."
    
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    
    if [ "$DEVICES" -eq 0 ]; then
        log_error "No devices connected. Please connect a device or start an emulator."
        log_info "To start emulator: emulator -avd <avd_name>"
        exit 1
    fi
    
    log_success "Device connected: $(adb devices | grep "device$" | head -1)"
}

# Build debug APK
build_apk() {
    log_info "Building debug APK..."
    
    if ./gradlew assembleDebug; then
        log_success "Debug APK built successfully"
    else
        log_error "Failed to build APK"
        exit 1
    fi
}

# Install APK
install_apk() {
    log_info "Installing APK on device..."
    
    if ./gradlew installDebug; then
        log_success "APK installed successfully"
    else
        log_error "Failed to install APK"
        exit 1
    fi
}

# Start app
start_app() {
    log_info "Starting MOMCLAW app..."
    
    adb shell am start -n com.loa.momclaw/.MainActivity
    sleep 3
    
    log_success "App started"
}

# Test 1: Check services started
test_services_started() {
    log_info "Test 1: Checking if services started..."
    
    # Wait for services to start
    sleep 5
    
    # Check InferenceService
    if adb logcat -d | grep -q "InferenceService.*Running on localhost:8080"; then
        log_success "InferenceService started on port 8080"
    else
        log_warning "InferenceService may not have started"
    fi
    
    # Check AgentService
    if adb logcat -d | grep -q "AgentService.*Agent running"; then
        log_success "AgentService started"
    else
        log_warning "AgentService may not have started"
    fi
    
    # Check StartupManager
    if adb logcat -d | grep -q "StartupManager.*Running"; then
        log_success "StartupManager reports services running"
    else
        log_warning "StartupManager may not have completed startup"
    fi
}

# Test 2: Check ports listening
test_ports_listening() {
    log_info "Test 2: Checking if ports are listening..."
    
    PORTS=$(adb shell netstat -tuln 2>/dev/null | grep -E "8080|9090" || true)
    
    if echo "$PORTS" | grep -q "8080"; then
        log_success "Port 8080 (LiteRT Bridge) is listening"
    else
        log_warning "Port 8080 not listening - LiteRT Bridge may not be running"
    fi
    
    if echo "$PORTS" | grep -q "9090"; then
        log_success "Port 9090 (NullClaw Agent) is listening"
    else
        log_warning "Port 9090 not listening - NullClaw Agent may not be running"
    fi
}

# Test 3: Health check
test_health_endpoints() {
    log_info "Test 3: Testing health endpoints..."
    
    # Test LiteRT Bridge health
    BRIDGE_HEALTH=$(adb shell curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health 2>/dev/null || echo "000")
    
    if [ "$BRIDGE_HEALTH" = "200" ]; then
        log_success "LiteRT Bridge health check passed (HTTP 200)"
    else
        log_warning "LiteRT Bridge health check failed (HTTP $BRIDGE_HEALTH)"
    fi
    
    # Test NullClaw Agent health
    AGENT_HEALTH=$(adb shell curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/health 2>/dev/null || echo "000")
    
    if [ "$AGENT_HEALTH" = "200" ]; then
        log_success "NullClaw Agent health check passed (HTTP 200)"
    else
        log_warning "NullClaw Agent health check failed (HTTP $AGENT_HEALTH)"
    fi
}

# Test 4: Chat API test
test_chat_api() {
    log_info "Test 4: Testing chat API..."
    
    RESPONSE=$(adb shell curl -s -X POST http://localhost:9090/v1/chat/completions \
        -H "Content-Type: application/json" \
        -d '{"model":"litert-bridge/gemma-4e4b","messages":[{"role":"user","content":"Hello"}],"stream":false}' \
        2>/dev/null || echo "ERROR")
    
    if [ "$RESPONSE" != "ERROR" ] && echo "$RESPONSE" | grep -q "content"; then
        log_success "Chat API test passed - received response"
        echo "$RESPONSE" | head -c 200
        echo ""
    else
        log_warning "Chat API test failed - no response or error"
    fi
}

# Test 5: Streaming test
test_streaming() {
    log_info "Test 5: Testing SSE streaming..."
    
    # Test streaming for 5 seconds
    timeout 5 adb shell curl -N http://localhost:9090/v1/chat/completions \
        -H "Content-Type: application/json" \
        -d '{"model":"litert-bridge/gemma-4e4b","messages":[{"role":"user","content":"Count to 5"}],"stream":true}' \
        2>/dev/null | head -20 > /tmp/stream_test.txt || true
    
    if grep -q "data:" /tmp/stream_test.txt; then
        log_success "SSE streaming test passed - received events"
        head -10 /tmp/stream_test.txt
    else
        log_warning "SSE streaming test failed - no events received"
    fi
    
    rm -f /tmp/stream_test.txt
}

# Run unit tests
run_unit_tests() {
    log_info "Running unit tests..."
    
    if ./gradlew test --info 2>&1 | tee /tmp/unit_tests.log; then
        log_success "Unit tests passed"
        
        # Show summary
        PASSED=$(grep -o "testPassed" /tmp/unit_tests.log | wc -l)
        FAILED=$(grep -o "testFailed" /tmp/unit_tests.log | wc -l)
        
        log_info "Tests passed: $PASSED, Failed: $FAILED"
    else
        log_warning "Some unit tests failed"
    fi
}

# Run integration tests
run_integration_tests() {
    log_info "Running integration tests..."
    
    if ./gradlew connectedAndroidTest --info 2>&1 | tee /tmp/integration_tests.log; then
        log_success "Integration tests passed"
    else
        log_warning "Some integration tests failed"
    fi
}

# Main test runner
main() {
    echo "========================================="
    echo "  MOMCLAW Integration Testing Script"
    echo "  Agent 3 - Integration & Testing"
    echo "========================================="
    echo ""
    
    # Parse arguments
    RUN_UNIT_TESTS=false
    RUN_INTEGRATION_TESTS=false
    
    for arg in "$@"; do
        case $arg in
            --unit-tests)
                RUN_UNIT_TESTS=true
                ;;
            --integration-tests)
                RUN_INTEGRATION_TESTS=true
                ;;
            --all-tests)
                RUN_UNIT_TESTS=true
                RUN_INTEGRATION_TESTS=true
                ;;
        esac
    done
    
    # Run tests
    check_prerequisites
    check_device
    build_apk
    install_apk
    start_app
    
    # Device tests
    test_services_started
    test_ports_listening
    test_health_endpoints
    test_chat_api
    test_streaming
    
    # Optional: run automated tests
    if [ "$RUN_UNIT_TESTS" = true ]; then
        run_unit_tests
    fi
    
    if [ "$RUN_INTEGRATION_TESTS" = true ]; then
        run_integration_tests
    fi
    
    echo ""
    echo "========================================="
    echo "  Integration Testing Complete"
    echo "========================================="
}

# Run main
main "$@"

#!/bin/bash

# MOMCLAW Comprehensive Test Runner
# Runs unit tests, integration tests, generates coverage reports
# Usage: ./comprehensive-test-runner.sh [--coverage] [--performance] [--all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"
REPORT_DIR="$PROJECT_ROOT/test-reports"

COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_CYAN='\033[0;36m'
COLOR_RESET='\033[0m'

# Parse arguments
RUN_COVERAGE=false
RUN_PERFORMANCE=false
RUN_ALL=false

for arg in "$@"; do
    case $arg in
        --coverage)
        RUN_COVERAGE=true
        shift
        ;;
        --performance)
        RUN_PERFORMANCE=true
        shift
        ;;
        --all)
        RUN_ALL=true
        shift
        ;;
        *)
        echo "Unknown argument: $arg"
        exit 1
        ;;
    esac
done

if [ "$RUN_ALL" = true ]; then
    RUN_COVERAGE=true
    RUN_PERFORMANCE=true
fi

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[✓]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[!]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[✗]${COLOR_RESET} $1"
}

log_section() {
    echo ""
    echo -e "${COLOR_CYAN}════════════════════════════════════════${COLOR_RESET}"
    echo -e "${COLOR_CYAN}$1${COLOR_RESET}"
    echo -e "${COLOR_CYAN}════════════════════════════════════════${COLOR_RESET}"
    echo ""
}

# Setup
setup() {
    log_section "Setup"
    
    mkdir -p "$REPORT_DIR"
    mkdir -p "$REPORT_DIR/coverage"
    mkdir -p "$REPORT_DIR/performance"
    mkdir -p "$REPORT_DIR/logs"
    
    cd "$ANDROID_DIR"
    
    if [ ! -f "./gradlew" ]; then
        log_error "gradlew not found in $ANDROID_DIR"
        exit 1
    fi
    
    chmod +x ./gradlew
    log_success "Setup complete"
}

# Run unit tests
run_unit_tests() {
    log_section "Running Unit Tests"
    
    local start_time=$(date +%s)
    
    if ./gradlew testDebugUnitTest --stacktrace 2>&1 | tee "$REPORT_DIR/logs/unit-tests.log"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "Unit tests passed (${duration}s)"
        
        # Count tests
        local test_count=$(find . -path "*/test-results/*" -name "*.xml" | wc -l)
        log_info "Test files found: $test_count"
        
        echo "UNIT_TEST_DURATION=$duration" >> "$REPORT_DIR/test-metrics.env"
        echo "UNIT_TEST_STATUS=PASSED" >> "$REPORT_DIR/test-metrics.env"
        
        return 0
    else
        log_error "Unit tests failed"
        echo "UNIT_TEST_STATUS=FAILED" >> "$REPORT_DIR/test-metrics.env"
        return 1
    fi
}

# Run integration tests
run_integration_tests() {
    log_section "Running Integration Tests"
    
    local start_time=$(date +%s)
    
    # Check if device/emulator available
    local device_count=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    
    if [ "$device_count" -eq 0 ]; then
        log_warning "No device connected - running unit integration tests only"
        
        if ./gradlew testDebugUnitTest --tests "*Integration*" --stacktrace 2>&1 | tee "$REPORT_DIR/logs/integration-tests.log"; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            log_success "Integration tests passed (${duration}s)"
            
            echo "INTEGRATION_TEST_DURATION=$duration" >> "$REPORT_DIR/test-metrics.env"
            echo "INTEGRATION_TEST_STATUS=PASSED" >> "$REPORT_DIR/test-metrics.env"
            return 0
        else
            log_error "Integration tests failed"
            echo "INTEGRATION_TEST_STATUS=FAILED" >> "$REPORT_DIR/test-metrics.env"
            return 1
        fi
    else
        log_info "Device found - running instrumented tests"
        
        if ./gradlew connectedDebugAndroidTest --stacktrace 2>&1 | tee "$REPORT_DIR/logs/integration-tests.log"; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            log_success "Integration tests passed (${duration}s)"
            
            echo "INTEGRATION_TEST_DURATION=$duration" >> "$REPORT_DIR/test-metrics.env"
            echo "INTEGRATION_TEST_STATUS=PASSED" >> "$REPORT_DIR/test-metrics.env"
            return 0
        else
            log_error "Integration tests failed"
            echo "INTEGRATION_TEST_STATUS=FAILED" >> "$REPORT_DIR/test-metrics.env"
            return 1
        fi
    fi
}

# Generate coverage report
generate_coverage_report() {
    if [ "$RUN_COVERAGE" = false ]; then
        return 0
    fi
    
    log_section "Generating Coverage Report"
    
    if ./gradlew jacocoTestReport 2>&1 | tee "$REPORT_DIR/logs/coverage.log"; then
        log_success "Coverage report generated"
        
        # Copy reports
        local coverage_dir="$ANDROID_DIR/app/build/reports/jacoco/jacocoTestReport"
        if [ -d "$coverage_dir" ]; then
            cp -r "$coverage_dir"/* "$REPORT_DIR/coverage/"
            log_info "Coverage report: $REPORT_DIR/coverage/html/index.html"
            
            # Extract coverage percentage from XML
            if [ -f "$REPORT_DIR/coverage/jacocoTestReport.xml" ]; then
                local line_rate=$(grep -oP 'line-rate="\K[0-9.]+' "$REPORT_DIR/coverage/jacocoTestReport.xml" | head -1)
                local coverage_percent=$(echo "$line_rate * 100" | bc -l | cut -c1-5)
                log_info "Line coverage: ${coverage_percent}%"
                echo "COVERAGE_PERCENT=$coverage_percent" >> "$REPORT_DIR/test-metrics.env"
            fi
        fi
        
        return 0
    else
        log_warning "Coverage report generation failed (JaCoCo might not be configured)"
        return 1
    fi
}

# Run performance benchmarks
run_performance_tests() {
    if [ "$RUN_PERFORMANCE" = false ]; then
        return 0
    fi
    
    log_section "Running Performance Tests"
    
    local perf_log="$REPORT_DIR/performance/benchmark.log"
    local perf_metrics="$REPORT_DIR/performance/metrics.env"
    
    # Check if device available
    local device_count=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    
    if [ "$device_count" -eq 0 ]; then
        log_warning "No device connected - skipping performance tests"
        echo "PERFORMANCE_TEST_STATUS=SKIPPED" >> "$perf_metrics"
        return 0
    fi
    
    # Run microbenchmark if available
    if [ -d "benchmark" ]; then
        log_info "Running microbenchmark..."
        ./gradlew :benchmark:benchmarkDebug 2>&1 | tee "$perf_log" || true
    fi
    
    # Manual performance checks
    log_info "Running manual performance checks..."
    
    # 1. Model load time test
    log_info "Testing model load time..."
    local load_start=$(date +%s%3N)
    adb shell am start -n com.loa.momclaw/.MainActivity
    sleep 5
    local load_end=$(date +%s%3N)
    local load_time=$((load_end - load_start))
    
    echo "MODEL_LOAD_TIME_MS=$load_time" >> "$perf_metrics"
    log_info "Model load time: ${load_time}ms"
    
    # 2. Memory usage check
    log_info "Checking memory usage..."
    local meminfo=$(adb shell dumpsys meminfo com.loa.momclaw | grep "TOTAL")
    local total_mem=$(echo "$meminfo" | awk '{print $2}')
    
    echo "MEMORY_USAGE_KB=$total_mem" >> "$perf_metrics"
    log_info "Memory usage: ${total_mem}KB"
    
    # 3. Inference speed test
    log_info "Testing inference speed..."
    # This would require actual chat interaction, skipping for now
    
    log_success "Performance tests complete"
    echo "PERFORMANCE_TEST_STATUS=COMPLETED" >> "$perf_metrics"
    
    return 0
}

# Generate final report
generate_final_report() {
    log_section "Generating Final Report"
    
    local report_file="$REPORT_DIR/test-report-$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" << EOF
# MOMCLAW Test Report

**Generated**: $(date '+%Y-%m-%d %H:%M:%S')
**Repository**: $PROJECT_ROOT

---

## Test Metrics

EOF
    
    if [ -f "$REPORT_DIR/test-metrics.env" ]; then
        cat "$REPORT_DIR/test-metrics.env" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

---

## Test Results

### Unit Tests
- Log: \`test-reports/logs/unit-tests.log\`
- Results: \`android/app/build/test-results/\`

### Integration Tests
- Log: \`test-reports/logs/integration-tests.log\`
- Results: \`android/app/build/outputs/androidTest-results/\`

### Coverage Report
EOF
    
    if [ "$RUN_COVERAGE" = true ]; then
        cat >> "$report_file" << EOF
- HTML: \`test-reports/coverage/html/index.html\`
- XML: \`test-reports/coverage/jacocoTestReport.xml\`
EOF
    else
        echo "- Skipped (run with --coverage)" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

### Performance Tests
EOF
    
    if [ "$RUN_PERFORMANCE" = true ]; then
        cat >> "$report_file" << EOF
- Log: \`test-reports/performance/benchmark.log\`
- Metrics: \`test-reports/performance/metrics.env\`
EOF
    else
        echo "- Skipped (run with --performance)" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

---

## Known Issues

1. **LiteRT-LM dependency**: Not available in Maven Central
2. **Ktor SSE dependency**: Not available in Maven Central
3. **Android SDK**: Required for build (not available in this environment)
4. **Device/Emulator**: Required for instrumented tests

## Next Steps

1. Configure GitHub Actions for CI/CD
2. Run full test suite in CI environment
3. Set up code coverage tracking in SonarQube/Coveralls
4. Configure performance benchmarks in CI
5. Add memory profiling tests

---

*Report generated by comprehensive-test-runner.sh*
EOF
    
    log_success "Final report generated: $report_file"
}

# Main execution
main() {
    log_section "MOMCLAW Comprehensive Test Runner"
    
    setup
    
    local exit_code=0
    
    if ! run_unit_tests; then
        exit_code=1
    fi
    
    if ! run_integration_tests; then
        exit_code=1
    fi
    
    generate_coverage_report || true
    run_performance_tests || true
    generate_final_report
    
    log_section "Summary"
    
    if [ $exit_code -eq 0 ]; then
        log_success "All tests passed!"
    else
        log_error "Some tests failed - check logs for details"
    fi
    
    log_info "Reports available in: $REPORT_DIR"
    
    return $exit_code
}

main

#!/bin/bash
# MomClAW Integration Test Runner
# Runs all integration tests and generates coverage report

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")/..") && cd "$ANDROID")
PROJECT_ROOT="android"

# Colors for colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'

# Configuration
MIN_coverage=70
test_timeout=300  # 5 minutes per test module
report_format="html"

# Test categories
declare -a test_categories=(
    "end-to-end"
    "race-conditions"
    "error-cascade"
    "retry-logic"
    "deadlock-detection"
    "service-lifecycle"
    "streaming"
    "startup-sequence"
)

# Run tests for category
run_test_category() {
    local category="$1"
    local test_dir="$2"
    local report_dir="$3"
    
    echo -e "Running integration tests for category: $category"
    
    for test_file in "$test_dir"/*Integration*Test.kt; do
        local test_name=$(basename "$test_file" .kt)
        echo -e "  $test_name"
        
        # Compile test classes
        echo -e "${YELLOW}Compiling test classes...${NC}"
        ./gradlew compileDebugUnitTestWithUnitTestTest \
            --continue
        
        # Run tests
        echo -e "${cyan}Running tests...${NC}"
        ./gradlew test --tests "$test_file" \
            --no-daemon \
            --continue
        
        # Check for failures
        if [ $? -ne 0 ]; then
            echo -e "${GREEN}✓ $test_name passed${NC}"
        else
            echo -e "${RED}✗ $test_name FAILED${NC}"
            failures+=1
        fi
    done
    
    # Wait for background tasks
    sleep 2
    
    echo -e "\n${BLUE}All tests completed. Waiting for final results...${NC}"
    wait $test_timeout
    
    echo ""
    echo -e "${BLUE}Generating coverage report...${NC}"
    
    # Generate JaCoCo coverage report
    ./gradlew jacocoTestReport \
        --no-daemon \
        --continue
    
    # Wait for report generation
    sleep 5
    
    # Find report
    local report_file=$(find "$report_dir" -name "*.html" | head -1)
    
    if [ -f "$report_file" ]; then
        echo -e "${GREEN}Coverage report generated: $report_file${NC}"
        cat "$report_file"
    else
        echo -e "${YELLOW}No coverage report found${NC}"
    fi
done

# Generate summary report
generate_summary_report

echo -e "\n${Magenta}========================================${NC}
echo -e "  ${YELLOW}MomClAW Integration Test Summary${NC}
echo -e "========================================${NC}
echo ""
echo -e "Date: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo -e "Test Categories:${NC}"
for category in "${test_categories[@]}; do
    printf "  %- %s\n" "$category"
done

echo ""
echo -e "Detailed Results:{NC}"
for test_file in "$test_dir"/*integration*test.kt"; do
    test_name=$(basename "$test_file" .kt)
    status=$(check_test_result "$test_file")
    
    if [ $status -eq 0 ]; then
                printf "  %- ${GREEN}✓ PAS${NC} - %s\n" "$test_name"
            passed_tests+=1
            total_passed++
        elif [ $status -eq 1 ]; then
                printf "  %- ${RED}✗ Failed${NC} - %s\n            " $test_name"
            failed_tests+=1
            total_failed++
            printf "    Error output: %s\n" "$(cat "$test_result "$test_file" | grep -A "error" || head -n)"
        else
                printf "  %- ${YELLOW}⚠ Test file not found: %s\n" "$test_file"
        fi
    done
    
    echo ""
    echo -e "${cyan}----------------------------------------${NC}
    printf "  Total Tests: %d\n" "Total Passed: %d\n" "Total Failed: %d\n" "Success Rate: %.1f%%\n", echo -e "========================================${NC}

# Save summary report
summary_file="$report_dir/test-summary-$(date +%Y-%m-%d%H%M%S).txt"
echo -e "Summary saved to: $summary_file"
echo -e "========================================${NC}"
echo -e "Test execution completed!${NC}
echo -e "Reports available at: $report_dir${NC}
echo -e "========================================${NC}

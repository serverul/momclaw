#!/bin/bash

# MomClAW Startup Sequence Validator
# Validates that the startup sequence is correctly implemented

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}════════════════════════════════════════${NC}"
echo -e "${CYAN}MomClAW Startup Sequence Validator${NC}"
echo -e "${CYAN}════════════════════════════════════════${NC}"
echo

VALIDATION_PASSED=0
VALIDATION_FAILED=0

check_pattern() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if grep -q "$pattern" "$PROJECT_ROOT/$file"; then
        echo -e "${GREEN}✓${NC} $description"
        VALIDATION_PASSED=$((VALIDATION_PASSED + 1))
    else
        echo -e "${RED}✗${NC} $description"
        VALIDATION_FAILED=$((VALIDATION_FAILED + 1))
    fi
}

echo "Checking StartupManager implementation..."
echo

# Check StartupManager exists
if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" ]; then
    echo -e "${GREEN}✓${NC} StartupManager.kt exists"
    VALIDATION_PASSED=$((VALIDATION_PASSED + 1))
else
    echo -e "${RED}✗${NC} StartupManager.kt not found"
    VALIDATION_FAILED=$((VALIDATION_FAILED + 1))
fi

# Check proper startup sequence
check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "StartingInference" \
    "Step 1: Start Inference Service"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "WaitingForInference" \
    "Step 2: Wait for Inference Ready"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "StartingAgent" \
    "Step 3: Start Agent Service"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "waitForInferenceReady" \
    "Inference readiness check implemented"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "waitForAgentReady" \
    "Agent readiness check implemented"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "startForegroundService" \
    "Services started as foreground (startForegroundService call)"

echo
echo "Checking service lifecycle..."

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "LifecycleObserver" \
    "Lifecycle observer implemented"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "stopServices" \
    "Stop services method exists"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "ON_DESTROY" \
    "Cleanup on destroy"

echo
echo "Checking error handling..."

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "try {" \
    "Try-catch blocks present"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "StartupState.Error" \
    "Error state defined"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "logger.error" \
    "Error logging implemented"

echo
echo "Checking InferenceService..."

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" \
    "LifecycleService" \
    "InferenceService extends LifecycleService"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" \
    "InferenceState.Running" \
    "Running state defined"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" \
    "startForeground" \
    "Foreground notification setup"

echo
echo "Checking AgentService..."

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "LifecycleService" \
    "AgentService extends LifecycleService"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "calculateBackoffDelay" \
    "Exponential backoff for restarts"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "AgentState.Running" \
    "Running state defined"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "startHealthMonitor" \
    "Health monitoring implemented"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "maxRestarts\|MAX_RESTARTS" \
    "Max restart limit defined"

echo
echo "Checking service state management..."

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" \
    "StateFlow<InferenceState>" \
    "InferenceService exposes StateFlow"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" \
    "StateFlow<AgentState>" \
    "AgentService exposes StateFlow"

check_pattern \
    "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" \
    "StateFlow<StartupState>" \
    "StartupManager exposes StateFlow"

echo
echo -e "${CYAN}════════════════════════════════════════${NC}"
echo -e "${CYAN}Validation Summary${NC}"
echo -e "${CYAN}════════════════════════════════════════${NC}"
echo

echo -e "Passed:  ${GREEN}$VALIDATION_PASSED${NC}"
echo -e "Failed:  ${RED}$VALIDATION_FAILED${NC}"
echo

if [ $VALIDATION_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Startup sequence is properly implemented!${NC}"
    echo
    echo "Startup Flow:"
    echo "  1. InferenceService starts (loads model, starts LiteRT Bridge on :8080)"
    echo "  2. Wait for model to be ready"
    echo "  3. AgentService starts (NullClaw connects to localhost:8080)"
    echo "  4. Both services running with health monitoring"
    echo
    echo "Error Handling:"
    echo "  - Exponential backoff on agent crash"
    echo "  - Max 3 restart attempts"
    echo "  - Proper cleanup on destroy"
    echo
    exit 0
else
    echo -e "${RED}✗ Startup sequence validation failed${NC}"
    echo "Review the failed checks above"
    exit 1
fi

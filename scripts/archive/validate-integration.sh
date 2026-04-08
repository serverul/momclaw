#!/bin/bash

# Quick Integration Validation Script
# Validates key integration points without requiring full build

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${CYAN}MOMCLAW Quick Integration Validation${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo

VALIDATION_PASSED=0
VALIDATION_FAILED=0

validate_file() {
    local file=$1
    local description=$2
    
    if [ -f "$PROJECT_ROOT/$file" ]; then
        echo -e "${GREEN}✓${NC} $description"
        VALIDATION_PASSED=$((VALIDATION_PASSED + 1))
    else
        echo -e "${RED}✗${NC} $description"
        VALIDATION_FAILED=$((VALIDATION_FAILED + 1))
    fi
}

validate_content() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if [ -f "$PROJECT_ROOT/$file" ] && grep -q "$pattern" "$PROJECT_ROOT/$file"; then
        echo -e "${GREEN}✓${NC} $description"
        VALIDATION_PASSED=$((VALIDATION_PASSED + 1))
    else
        echo -e "${RED}✗${NC} $description"
        VALIDATION_FAILED=$((VALIDATION_FAILED + 1))
    fi
}

echo -e "${YELLOW}▶ Validating Project Structure${NC}"
validate_file "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "StartupManager exists"
# StartupCoordinator was consolidated into StartupManager
validate_file "android/app/src/main/java/com/loa/momclaw/inference/InferenceService.kt" "InferenceService exists"
validate_file "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" "AgentService exists"
validate_file "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "LiteRTBridge exists"
validate_file "android/agent/src/main/java/com/loa/momclaw/agent/NullClawBridge.kt" "NullClawBridge exists"

echo
echo -e "${YELLOW}▶ Validating Startup Sequence${NC}"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "InferenceService" "StartupManager references InferenceService"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "AgentService" "StartupManager references AgentService"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "waitForInferenceReady" "Inference readiness check implemented"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "waitForAgentReady" "Agent readiness check implemented"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "startForegroundService" "Services started as foreground"

echo
echo -e "${YELLOW}▶ Validating HTTP Communication${NC}"
validate_content "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "/health" "Health endpoint implemented"
validate_content "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "/v1/models" "Models endpoint implemented"
validate_content "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "/v1/chat/completions" "Chat completions endpoint implemented"
validate_content "android/bridge/src/main/java/com/loa/momclaw/bridge/LiteRTBridge.kt" "EventStream\|respondTextWriter" "SSE streaming support"
validate_content "android/app/src/main/java/com/loa/momclaw/data/remote/AgentClient.kt" "OkHttpClient" "HTTP client implemented"

echo
echo -e "${YELLOW}▶ Validating Error Handling${NC}"
validate_content "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" "calculateBackoffDelay" "Exponential backoff implemented"
validate_content "android/app/src/main/java/com/loa/momclaw/agent/AgentService.kt" "maxRestarts" "Max restart limit defined"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "try {" "Error handling present"
validate_content "android/app/src/main/java/com/loa/momclaw/startup/StartupManager.kt" "catch (e: Exception)" "Exception handling present"

echo
echo -e "${YELLOW}▶ Validating Persistence${NC}"
validate_file "android/app/src/main/java/com/loa/momclaw/data/local/database/MOMCLAWDatabase.kt" "Room database exists"
validate_file "android/app/src/main/java/com/loa/momclaw/data/local/database/MessageDao.kt" "Message DAO exists"
validate_content "android/app/src/main/java/com/loa/momclaw/data/local/database/MessageDao.kt" "@Dao" "DAO annotation present"
validate_content "android/app/src/main/java/com/loa/momclaw/data/local/database/MessageDao.kt" "@Insert" "Insert operation defined"
validate_content "android/app/src/main/java/com/loa/momclaw/data/local/database/MessageDao.kt" "@Query" "Query operations defined"

echo
echo -e "${YELLOW}▶ Validating Dependency Injection${NC}"
validate_file "android/app/src/main/java/com/loa/momclaw/MOMCLAWApplication.kt" "Application class exists"
validate_content "android/app/src/main/java/com/loa/momclaw/MOMCLAWApplication.kt" "@HiltAndroidApp" "Hilt application annotation"
validate_content "android/app/src/main/java/com/loa/momclaw/di/AppModule.kt" "@Module" "Hilt module defined"
validate_content "android/app/src/main/java/com/loa/momclaw/di/AppModule.kt" "@Provides" "Provider methods defined"
validate_content "android/app/src/main/java/com/loa/momclaw/ui/chat/ChatViewModel.kt" "@HiltViewModel" "ViewModel uses Hilt"

echo
echo -e "${YELLOW}▶ Validating Test Coverage${NC}"
validate_file "android/app/src/test/java/com/loa/momclaw/startup/StartupManagerTest.kt" "StartupManager test exists"
validate_file "android/app/src/test/java/com/loa/momclaw/integration/ServiceLifecycleIntegrationTest.kt" "Service lifecycle test exists"
validate_file "android/app/src/test/java/com/loa/momclaw/integration/OfflineFunctionalityTest.kt" "Offline functionality test exists"
validate_file "android/bridge/src/test/kotlin/com/loa/momclaw/bridge/LiteRTBridgeTest.kt" "LiteRT bridge test exists"
validate_file "android/agent/src/test/java/com/loa/momclaw/agent/NullClawBridgeTest.kt" "NullClaw bridge test exists"

echo
echo -e "${YELLOW}▶ Validating Streaming${NC}"
validate_content "android/app/src/main/java/com/loa/momclaw/data/remote/AgentClient.kt" "EventSourceListener" "SSE streaming client"
validate_content "android/app/src/main/java/com/loa/momclaw/domain/repository/ChatRepository.kt" "sendMessageStream" "Streaming repository method"
validate_content "android/app/src/main/java/com/loa/momclaw/domain/repository/ChatRepository.kt" "StreamState" "Stream state sealed class"
validate_content "android/app/src/main/java/com/loa/momclaw/ui/chat/ChatViewModel.kt" "StateFlow" "StateFlow for reactive UI"

echo
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${CYAN}Validation Summary${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo
echo -e "Passed:  ${GREEN}$VALIDATION_PASSED${NC}"
echo -e "Failed:  ${RED}$VALIDATION_FAILED${NC}"
echo

if [ $VALIDATION_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All integration points validated successfully!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some validation checks failed${NC}"
    exit 1
fi

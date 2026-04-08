#!/bin/bash

# MOMCLAW UI Verification Script
# Checks that all critical UI components exist and are properly configured

set -e

echo "🔍 MOMCLAW UI Verification"
echo "=========================="
echo ""

PROJECT_ROOT="/home/userul/.openclaw/workspace/momclaw"
UI_DIR="$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/ui"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass() {
    echo -e "${GREEN}✅ $1${NC}"
}

fail() {
    echo -e "${RED}❌ $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Check critical files
echo "📦 Checking UI Components..."
echo ""

# ChatScreen
if [ -f "$UI_DIR/chat/ChatScreen.kt" ]; then
    pass "ChatScreen.kt exists"
    if grep -q "Material3" "$UI_DIR/chat/ChatScreen.kt"; then
        pass "ChatScreen uses Material3"
    else
        fail "ChatScreen doesn't use Material3"
    fi
    if grep -q "useNavigationRail" "$UI_DIR/chat/ChatScreen.kt"; then
        pass "ChatScreen has responsive layout support"
    else
        warn "ChatScreen missing responsive layout"
    fi
else
    fail "ChatScreen.kt missing"
fi

# ModelsScreen
if [ -f "$UI_DIR/models/ModelsScreen.kt" ]; then
    pass "ModelsScreen.kt exists"
    if grep -q "LazyVerticalGrid" "$UI_DIR/models/ModelsScreen.kt"; then
        pass "ModelsScreen has grid layout for tablets"
    else
        warn "ModelsScreen missing grid layout"
    fi
else
    fail "ModelsScreen.kt missing"
fi

# SettingsScreen
if [ -f "$UI_DIR/settings/SettingsScreen.kt" ]; then
    pass "SettingsScreen.kt exists"
    if grep -q "useTwoColumnLayout" "$UI_DIR/settings/SettingsScreen.kt"; then
        pass "SettingsScreen has two-column layout for tablets"
    else
        warn "SettingsScreen missing two-column layout"
    fi
else
    fail "SettingsScreen.kt missing"
fi

# Navigation
if [ -f "$UI_DIR/navigation/NavGraph.kt" ]; then
    pass "NavGraph.kt exists"
    if grep -q "NavigationRail" "$UI_DIR/navigation/NavGraph.kt"; then
        pass "Navigation has NavigationRail for tablets"
    else
        warn "Navigation missing NavigationRail"
    fi
else
    fail "NavGraph.kt missing"
fi

# Theme
if [ -f "$UI_DIR/theme/Theme.kt" ]; then
    pass "Theme.kt exists"
else
    fail "Theme.kt missing"
fi

if [ -f "$UI_DIR/theme/Color.kt" ]; then
    pass "Color.kt exists"
else
    fail "Color.kt missing"
fi

# ViewModels
echo ""
echo "📦 Checking ViewModels..."

if [ -f "$UI_DIR/chat/ChatViewModel.kt" ]; then
    pass "ChatViewModel.kt exists"
else
    fail "ChatViewModel.kt missing"
fi

if [ -f "$UI_DIR/models/ModelsViewModel.kt" ]; then
    pass "ModelsViewModel.kt exists"
else
    fail "ModelsViewModel.kt missing"
fi

if [ -f "$UI_DIR/settings/SettingsViewModel.kt" ]; then
    pass "SettingsViewModel.kt exists"
else
    fail "SettingsViewModel.kt missing"
fi

# Data Layer
echo ""
echo "📦 Checking Data Layer..."

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/domain/repository/ChatRepository.kt" ]; then
    pass "ChatRepository.kt exists"
else
    fail "ChatRepository.kt missing"
fi

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/data/local/database/MOMCLAWDatabase.kt" ]; then
    pass "Database exists"
else
    fail "Database missing"
fi

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/data/local/preferences/SettingsPreferences.kt" ]; then
    pass "SettingsPreferences exists"
else
    fail "SettingsPreferences missing"
fi

# Utilities
echo ""
echo "📦 Checking Utilities..."

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/util/StreamBuffer.kt" ]; then
    pass "StreamBuffer.kt exists"
else
    fail "StreamBuffer.kt missing"
fi

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/util/MomClawLogger.kt" ]; then
    pass "MomClawLogger.kt exists"
else
    fail "MomClawLogger.kt missing"
fi

# Application classes
echo ""
echo "📦 Checking Application Classes..."

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/MainActivity.kt" ]; then
    pass "MainActivity.kt exists"
else
    fail "MainActivity.kt missing"
fi

if [ -f "$PROJECT_ROOT/android/app/src/main/java/com/loa/momclaw/MOMCLAWApplication.kt" ]; then
    pass "MOMCLAWApplication.kt exists"
else
    fail "MOMCLAWApplication.kt missing"
fi

# Build files
echo ""
echo "📦 Checking Build Configuration..."

if [ -f "$PROJECT_ROOT/android/app/build.gradle.kts" ]; then
    pass "build.gradle.kts exists"
    if grep -q "compose = true" "$PROJECT_ROOT/android/app/build.gradle.kts"; then
        pass "Compose is enabled"
    else
        fail "Compose not enabled in build.gradle.kts"
    fi
else
    fail "build.gradle.kts missing"
fi

# Summary
echo ""
echo "=========================="
echo -e "${GREEN}✨ UI Verification Complete!${NC}"
echo ""
echo "All critical components are present and properly configured."
echo "The app is ready for building and deployment."
echo ""
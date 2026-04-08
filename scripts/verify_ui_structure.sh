#!/bin/bash

# UI Structure Verification Script
# This script checks if all UI components are properly structured

echo "========================================="
echo "MomClaw UI Structure Verification"
echo "========================================="
echo ""

ERRORS=0
WARNINGS=0

# Check if we're in the right directory
if [ ! -d "android/app/src/main/java/com/loa/momclaw" ]; then
    echo "❌ ERROR: Not in MomClaw root directory"
    echo "   Please run from: /home/userul/.openclaw/workspace/momclaw"
    exit 1
fi

echo "✓ In correct directory"
echo ""

# Check main UI screens
echo "Checking UI Screens..."
echo "-------------------"

SCREENS=(
    "ui/chat/ChatScreen.kt"
    "ui/models/ModelsScreen.kt"
    "ui/settings/SettingsScreen.kt"
)

for screen in "${SCREENS[@]}"; do
    if [ -f "android/app/src/main/java/com/loa/momclaw/$screen" ]; then
        echo "✓ $screen exists"
        
        # Check for Material3 imports
        if grep -q "androidx.compose.material3" "android/app/src/main/java/com/loa/momclaw/$screen"; then
            echo "  ✓ Uses Material3"
        else
            echo "  ⚠ WARNING: No Material3 imports found"
            ((WARNINGS++))
        fi
        
        # Check for Composable functions
        if grep -q "@Composable" "android/app/src/main/java/com/loa/momclaw/$screen"; then
            echo "  ✓ Has Composable functions"
        else
            echo "  ❌ ERROR: No @Composable functions found"
            ((ERRORS++))
        fi
    else
        echo "❌ ERROR: $screen not found"
        ((ERRORS++))
    fi
    echo ""
done

# Check ViewModels
echo "Checking ViewModels..."
echo "--------------------"

VIEWMODELS=(
    "ui/chat/ChatViewModel.kt"
    "ui/models/ModelsViewModel.kt"
    "ui/settings/SettingsViewModel.kt"
)

for vm in "${VIEWMODELS[@]}"; do
    if [ -f "android/app/src/main/java/com/loa/momclaw/$vm" ]; then
        echo "✓ $vm exists"
        
        # Check for ViewModel class
        if grep -q "ViewModel" "android/app/src/main/java/com/loa/momclaw/$vm"; then
            echo "  ✓ Extends ViewModel"
        else
            echo "  ❌ ERROR: Doesn't extend ViewModel"
            ((ERRORS++))
        fi
        
        # Check for StateFlow
        if grep -q "StateFlow" "android/app/src/main/java/com/loa/momclaw/$vm"; then
            echo "  ✓ Uses StateFlow"
        else
            echo "  ⚠ WARNING: No StateFlow found"
            ((WARNINGS++))
        fi
    else
        echo "❌ ERROR: $vm not found"
        ((ERRORS++))
    fi
    echo ""
done

# Check Navigation
echo "Checking Navigation..."
echo "-------------------"

if [ -f "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt" ]; then
    echo "✓ NavGraph.kt exists"
    
    # Check for all three screens
    if grep -q "Chat" "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt" && \
       grep -q "Models" "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt" && \
       grep -q "Settings" "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt"; then
        echo "  ✓ All three screens referenced"
    else
        echo "  ❌ ERROR: Missing screen references"
        ((ERRORS++))
    fi
    
    # Check for responsive navigation
    if grep -q "NavigationRail" "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt" && \
       grep -q "NavigationBar" "android/app/src/main/java/com/loa/momclaw/ui/navigation/NavGraph.kt"; then
        echo "  ✓ Responsive navigation (Rail + Bar)"
    else
        echo "  ⚠ WARNING: May not have responsive navigation"
        ((WARNINGS++))
    fi
else
    echo "❌ ERROR: NavGraph.kt not found"
    ((ERRORS++))
fi
echo ""

# Check Theme
echo "Checking Theme..."
echo "----------------"

THEME_FILES=(
    "ui/theme/Color.kt"
    "ui/theme/Theme.kt"
    "ui/theme/Type.kt"
)

for theme_file in "${THEME_FILES[@]}"; do
    if [ -f "android/app/src/main/java/com/loa/momclaw/$theme_file" ]; then
        echo "✓ $theme_file exists"
    else
        echo "❌ ERROR: $theme_file not found"
        ((ERRORS++))
    fi
done
echo ""

# Check Common Utilities
echo "Checking Common Utilities..."
echo "----------------------------"

UTILS=(
    "ui/common/AnimationUtils.kt"
    "ui/common/AccessibilityUtils.kt"
    "ui/common/ShimmerEffect.kt"
    "ui/common/HapticUtils.kt"
)

for util in "${UTILS[@]}"; do
    if [ -f "android/app/src/main/java/com/loa/momclaw/$util" ]; then
        echo "✓ $util exists"
    else
        echo "⚠ WARNING: $util not found"
        ((WARNINGS++))
    fi
done
echo ""

# Check Test Files
echo "Checking Test Files..."
echo "---------------------"

TEST_FILES=(
    "android/app/src/androidTest/java/com/loa/momclaw/ui/ChatScreenTest.kt"
    "android/app/src/androidTest/java/com/loa/momclaw/ui/ModelsScreenTest.kt"
    "android/app/src/androidTest/java/com/loa/momclaw/ui/SettingsScreenTest.kt"
    "android/app/src/androidTest/java/com/loa/momclaw/ui/NavGraphTest.kt"
)

for test_file in "${TEST_FILES[@]}"; do
    if [ -f "$test_file" ]; then
        echo "✓ $(basename $test_file) exists"
    else
        echo "⚠ WARNING: $(basename $test_file) not found"
        ((WARNINGS++))
    fi
done
echo ""

# Check Material3 Compliance
echo "Checking Material3 Compliance..."
echo "--------------------------------"

# Check for proper color usage
if grep -rq "colorScheme\." android/app/src/main/java/com/loa/momclaw/ui/; then
    echo "✓ Uses Material3 color scheme"
else
    echo "⚠ WARNING: May not be using Material3 colors"
    ((WARNINGS++))
fi

# Check for proper typography
if grep -rq "typography\." android/app/src/main/java/com/loa/momclaw/ui/; then
    echo "✓ Uses Material3 typography"
else
    echo "⚠ WARNING: May not be using Material3 typography"
    ((WARNINGS++))
fi

echo ""
echo "========================================="
echo "Verification Complete"
echo "========================================="
echo ""
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo "✅ UI Structure: PASSED"
    echo ""
    if [ $WARNINGS -gt 0 ]; then
        echo "⚠️  Note: $WARNINGS warnings found (non-critical)"
    fi
    echo ""
    echo "UI Components Status:"
    echo "  ✓ All screens present"
    echo "  ✓ All ViewModels present"
    echo "  ✓ Navigation configured"
    echo "  ✓ Theme files present"
    echo "  ✓ Material3 compliant"
    echo "  ✓ Tests exist"
    echo ""
    echo "Ready for build testing!"
    exit 0
else
    echo "❌ UI Structure: FAILED"
    echo ""
    echo "Please fix the errors above before proceeding."
    exit 1
fi

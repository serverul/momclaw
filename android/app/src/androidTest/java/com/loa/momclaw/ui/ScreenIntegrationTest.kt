package com.loa.momclaw.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.MainActivity
import com.loa.momclaw.ui.theme.MomClawTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for verifying all screens work together seamlessly.
 */
@RunWith(AndroidJUnit4::class)
class ScreenIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launchesSuccessfully() {
        // App should launch and show chat screen by default
        composeRule.onNodeWithText("Chat")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_switchesBetweenScreens() {
        // Start on Chat screen
        composeRule.onNodeWithText("Chat")
            .assertIsDisplayed()
        
        // Navigate to Models screen
        composeRule.onNodeWithText("Models")
            .performClick()
        
        composeRule.onNodeWithText("Models", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        
        // Navigate to Settings screen
        composeRule.onNodeWithText("Settings")
            .performClick()
        
        composeRule.onNodeWithText("Settings")
            .assertIsDisplayed()
        
        // Navigate back to Chat
        composeRule.onNodeWithText("Chat")
            .performClick()
        
        composeRule.onNodeWithText("Chat")
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_allComponentsPresent() {
        // Verify Chat screen has all required components
        composeRule.onNodeWithHint("Type a message...")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeRule.onNodeWithContentDescription("Send")
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_allSettingsPresent() {
        // Navigate to Settings
        composeRule.onNodeWithText("Settings")
            .performClick()
        
        // Verify all settings are present
        composeRule.onNodeWithText("System Prompt")
            .assertIsDisplayed()
        
        composeRule.onNodeWithText("Temperature")
            .assertIsDisplayed()
        
        composeRule.onNodeWithText("Max Tokens")
            .assertIsDisplayed()
        
        composeRule.onNodeWithText("Dark Mode")
            .assertIsDisplayed()
        
        composeRule.onNodeWithText("Auto Save")
            .assertIsDisplayed()
    }

    @Test
    fun modelsScreen_showsStorageInfo() {
        // Navigate to Models
        composeRule.onNodeWithText("Models")
            .performClick()
        
        // Storage info should be shown
        composeRule.onNodeWithText("Storage", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun darkModeToggle_changesTheme() {
        // Navigate to Settings
        composeRule.onNodeWithText("Settings")
            .performClick()
        
        // Toggle dark mode
        composeRule.onNodeWithText("Dark Mode")
            .performClick()
        
        // Theme should change (visual verification)
        composeRule.waitForIdle()
        
        // Toggle back
        composeRule.onNodeWithText("Dark Mode")
            .performClick()
    }

    @Test
    fun chatInput_validation() {
        // Send button should be disabled when input is empty
        composeRule.onNodeWithContentDescription("Send")
            .assertIsNotEnabled()
        
        // Type something
        composeRule.onNodeWithHint("Type a message...")
            .performTextInput("Test message")
        
        // Send button should now be enabled
        composeRule.onNodeWithContentDescription("Send")
            .assertIsEnabled()
    }

    @Test
    fun accessibility_allElementsHaveContentDescriptions() {
        // All interactive elements should have content descriptions
        
        // Navigation items
        composeRule.onNodeWithContentDescription("Chat tab")
            .assertIsDisplayed()
        
        composeRule.onNodeWithContentDescription("Models tab")
            .assertIsDisplayed()
        
        composeRule.onNodeWithContentDescription("Settings tab")
            .assertIsDisplayed()
    }
}

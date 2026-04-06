package com.loa.momclaw.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loa.momclaw.ui.theme.MOMCLAWTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SettingsScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun TestSettingsScreen(
        uiState: SettingsUiState = SettingsUiState(
            systemPrompt = "You are a helpful AI assistant.",
            temperature = 0.7f,
            maxTokens = 2048,
            modelPrimary = "gemma-2b-it",
            baseUrl = "http://localhost:8080",
            darkTheme = true,
            streamingEnabled = true,
            notificationsEnabled = true,
            backgroundAgentEnabled = false,
            isLoading = false,
            hasChanges = false
        )
    ) {
        MOMCLAWTheme {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .width(400.dp)
                        .height(800.dp)
                ) {
                    SettingsScreen(
                        uiState = uiState,
                        onNavigateBack = {},
                        onSystemPromptChange = {},
                        onTemperatureChange = {},
                        onMaxTokensChange = {},
                        onModelPrimaryChange = {},
                        onBaseUrlChange = {},
                        onDarkThemeChange = {},
                        onStreamingEnabledChange = {},
                        onNotificationsEnabledChange = {},
                        onBackgroundAgentChange = {},
                        onResetToDefaults = {},
                        onSave = {}
                    )
                }
            }
        }
    }

    @Test
    fun settingsScreen_displaysAllSections() {
        composeRule.setContent {
            TestSettingsScreen()
        }

        // Verify section headers
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Agent Configuration").assertExists()
        composeRule.onNodeWithText("App Settings").assertExists()
        composeRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun settingsScreen_displaysAgentConfigFields() {
        composeRule.setContent {
            TestSettingsScreen()
        }

        // Verify all agent config fields are present
        composeRule.onNodeWithText("System Prompt").assertExists()
        composeRule.onNodeWithText("Temperature").assertExists()
        composeRule.onNodeWithText("Max Tokens").assertExists()
        composeRule.onNodeWithText("Primary Model").assertExists()
        composeRule.onNodeWithText("Agent URL").assertExists()
    }

    @Test
    fun settingsScreen_displaysAppSettings() {
        composeRule.setContent {
            TestSettingsScreen()
        }

        // Verify app settings toggles
        composeRule.onNodeWithText("Dark Theme").assertExists()
        composeRule.onNodeWithText("Stream Responses").assertExists()
        composeRule.onNodeWithText("Notifications").assertExists()
        composeRule.onNodeWithText("Background Agent").assertExists()
    }

    @Test
    fun settingsScreen_displaysAboutSection() {
        composeRule.setContent {
            TestSettingsScreen()
        }

        // Verify about section content
        composeRule.onNodeWithText("MOMCLAW").assertExists()
        composeRule.onNodeWithText("Version 1.0.0").assertExists()
        composeRule.onNodeWithText("Powered by").assertExists()
        composeRule.onNodeWithText("NullClaw + llama.cpp + Gemma").assertExists()
        composeRule.onNodeWithText("Reset to Defaults").assertExists()
    }

    @Test
    fun settingsScreen_noSaveButtonWhenNoChanges() {
        composeRule.setContent {
            TestSettingsScreen(
                uiState = SettingsUiState(
                    hasChanges = false
                )
            )
        }

        composeRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun settingsScreen_saveButtonWhenChanges() {
        composeRule.setContent {
            TestSettingsScreen(
                uiState = SettingsUiState(
                    systemPrompt = "Modified prompt",
                    temperature = 0.9f,
                    maxTokens = 4096,
                    hasChanges = true
                )
            )
        }

        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_tabletTwoColumnLayout() {
        composeRule.setContent {
            MOMCLAWTheme {
                MaterialTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .width(800.dp)
                            .height(600.dp)
                    ) {
                        SettingsScreen(
                            uiState = SettingsUiState(
                                systemPrompt = "You are a helpful AI.",
                                temperature = 0.7f,
                                maxTokens = 2048,
                                modelPrimary = "gemma-2b",
                                baseUrl = "http://localhost:8080",
                                darkTheme = true,
                                streamingEnabled = true,
                                notificationsEnabled = true,
                                backgroundAgentEnabled = false,
                                isLoading = false,
                                hasChanges = true
                            ),
                            onNavigateBack = {},
                            onSystemPromptChange = {},
                            onTemperatureChange = {},
                            onMaxTokensChange = {},
                            onModelPrimaryChange = {},
                            onBaseUrlChange = {},
                            onDarkThemeChange = {},
                            onStreamingEnabledChange = {},
                            onNotificationsEnabledChange = {},
                            onBackgroundAgentChange = {},
                            onResetToDefaults = {},
                            onSave = {},
                            useNavigationRail = true
                        )
                    }
                }
            }
        }

        // Both sections should be visible in tablet layout
        composeRule.onNodeWithText("Agent Configuration").assertExists()
        composeRule.onNodeWithText("App Settings").assertExists()
        composeRule.onNodeWithText("About").assertExists()
        composeRule.onNodeWithText("Save Changes").assertExists()
    }

    @Test
    fun settingsScreen_supportingTexts() {
        composeRule.setContent {
            TestSettingsScreen()
        }

        // Verify supporting/helper texts
        composeRule.onNodeWithText("Instructions for the AI assistant").assertExists()
        composeRule.onNodeWithText("Controls randomness: 0 = deterministic, 2 = creative").assertExists()
        composeRule.onNodeWithText("Maximum length of responses").assertExists()
        composeRule.onNodeWithText("Model ID for inference").assertExists()
        composeRule.onNodeWithText("NullClaw endpoint").assertExists()
    }
}

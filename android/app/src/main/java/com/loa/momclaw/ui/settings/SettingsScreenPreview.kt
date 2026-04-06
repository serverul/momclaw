package com.loa.momclaw.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.loa.momclaw.ui.theme.MOMCLAWTheme

/**
 * Preview for SettingsScreen - Default state
 */
@Preview(name = "Settings - Default", showBackground = true)
@Composable
fun SettingsScreenDefaultPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
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
            onSave = {}
        )
    }
}

/**
 * Preview for SettingsScreen - With changes
 */
@Preview(name = "Settings - Modified", showBackground = true)
@Composable
fun SettingsScreenModifiedPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are an expert in Android development and Kotlin.",
                temperature = 0.5f,
                maxTokens = 4096,
                modelPrimary = "llama-3-8b",
                baseUrl = "http://10.0.2.2:8080",
                darkTheme = false,
                streamingEnabled = true,
                notificationsEnabled = false,
                backgroundAgentEnabled = true,
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
            onSave = {}
        )
    }
}

/**
 * Preview for SettingsScreen - Loading
 */
@Preview(name = "Settings - Loading", showBackground = true)
@Composable
fun SettingsScreenLoadingPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                isLoading = true
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
            onSave = {}
        )
    }
}

/**
 * Preview for SettingsScreen - Tablet (two-column layout)
 */
@Preview(name = "Settings - Tablet", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun SettingsScreenTabletPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are a helpful AI assistant specialized in coding and technical questions.",
                temperature = 0.8f,
                maxTokens = 2048,
                modelPrimary = "gemma-2b-it",
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

/**
 * Preview for SettingsScreen - Light Theme
 */
@Preview(name = "Settings - Light", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsScreenLightPreview() {
    MOMCLAWTheme(darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are a helpful AI assistant.",
                temperature = 0.7f,
                maxTokens = 2048,
                modelPrimary = "gemma-2b-it",
                baseUrl = "http://localhost:8080",
                darkTheme = false,
                streamingEnabled = true,
                notificationsEnabled = true,
                backgroundAgentEnabled = false,
                isLoading = false,
                hasChanges = false
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
            onSave = {}
        )
    }
}

/**
 * Preview for individual Settings components
 */
@Preview(name = "Settings Components", showBackground = true)
@Composable
fun SettingsComponentsPreview() {
    MOMCLAWTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Slider example
            SettingsSlider(
                label = "Temperature",
                value = 0.7f,
                onValueChange = {},
                valueRange = 0f..2f,
                steps = 19,
                supportingText = "Controls randomness: 0 = deterministic, 2 = creative"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Switch example
            SettingsSwitch(
                title = "Dark Theme",
                subtitle = "Use dark color scheme",
                icon = androidx.compose.material.icons.Icons.Default.DarkMode,
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}

// Required imports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

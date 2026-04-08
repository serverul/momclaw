package com.loa.momclaw.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loa.momclaw.config.NetworkConfig
import com.loa.momclaw.ui.theme.MOMCLAWTheme

@Preview(name = "Settings - Default", showBackground = true)
@Composable
fun SettingsScreenDefaultPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are a helpful AI assistant.",
                temperature = 0.7f, maxTokens = 2048,
                modelPrimary = "gemma-2b-it", baseUrl = NetworkConfig.DEFAULT_BASE_URL,
                darkTheme = true, streamingEnabled = true,
                notificationsEnabled = true, backgroundAgentEnabled = false,
                hasChanges = false
            ),
            onNavigateBack = {}, onSystemPromptChange = {}, onTemperatureChange = {},
            onMaxTokensChange = {}, onModelPrimaryChange = {}, onBaseUrlChange = {},
            onDarkThemeChange = {}, onStreamingEnabledChange = {},
            onNotificationsEnabledChange = {}, onBackgroundAgentChange = {},
            onResetToDefaults = {}, onSave = {}
        )
    }
}

@Preview(name = "Settings - Modified", showBackground = true)
@Composable
fun SettingsScreenModifiedPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are an expert in Android development.",
                temperature = 0.5f, maxTokens = 4096,
                modelPrimary = "llama-3-8b", baseUrl = NetworkConfig.EMULATOR_BASE_URL,
                darkTheme = false, streamingEnabled = true,
                notificationsEnabled = false, backgroundAgentEnabled = true,
                hasChanges = true
            ),
            onNavigateBack = {}, onSystemPromptChange = {}, onTemperatureChange = {},
            onMaxTokensChange = {}, onModelPrimaryChange = {}, onBaseUrlChange = {},
            onDarkThemeChange = {}, onStreamingEnabledChange = {},
            onNotificationsEnabledChange = {}, onBackgroundAgentChange = {},
            onResetToDefaults = {}, onSave = {}
        )
    }
}

@Preview(name = "Settings - Tablet", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun SettingsScreenTabletPreview() {
    MOMCLAWTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are a helpful AI assistant.",
                temperature = 0.8f, maxTokens = 2048,
                modelPrimary = "gemma-2b-it", baseUrl = NetworkConfig.DEFAULT_BASE_URL,
                darkTheme = true, streamingEnabled = true,
                notificationsEnabled = true, backgroundAgentEnabled = false,
                hasChanges = true
            ),
            onNavigateBack = {}, onSystemPromptChange = {}, onTemperatureChange = {},
            onMaxTokensChange = {}, onModelPrimaryChange = {}, onBaseUrlChange = {},
            onDarkThemeChange = {}, onStreamingEnabledChange = {},
            onNotificationsEnabledChange = {}, onBackgroundAgentChange = {},
            onResetToDefaults = {}, onSave = {},
            useNavigationRail = true
        )
    }
}

@Preview(name = "Settings - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsScreenLightPreview() {
    MOMCLAWTheme(darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(
                systemPrompt = "You are a helpful AI assistant.",
                temperature = 0.7f, maxTokens = 2048,
                modelPrimary = "gemma-2b-it", baseUrl = NetworkConfig.DEFAULT_BASE_URL,
                darkTheme = false, streamingEnabled = true,
                notificationsEnabled = true, backgroundAgentEnabled = false,
                hasChanges = false
            ),
            onNavigateBack = {}, onSystemPromptChange = {}, onTemperatureChange = {},
            onMaxTokensChange = {}, onModelPrimaryChange = {}, onBaseUrlChange = {},
            onDarkThemeChange = {}, onStreamingEnabledChange = {},
            onNotificationsEnabledChange = {}, onBackgroundAgentChange = {},
            onResetToDefaults = {}, onSave = {}
        )
    }
}

@Preview(name = "Settings Components", showBackground = true)
@Composable
fun SettingsComponentsPreview() {
    MOMCLAWTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSlider(
                label = "Temperature", value = 0.7f, onValueChange = {},
                valueRange = 0f..2f, steps = 19,
                supportingText = "Controls randomness"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSwitch(
                title = "Dark Theme", subtitle = "Use dark color scheme",
                icon = Icons.Default.DarkMode, checked = true, onCheckedChange = {}
            )
        }
    }
}

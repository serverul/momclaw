package com.loa.momclaw

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.loa.momclaw.ui.settings.SettingsScreen
import com.loa.momclaw.ui.settings.SettingsUiState

/**
 * Route composable that connects ViewModel to SettingsScreen
 * In production, this would use hiltViewModel()
 */
@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onThemeChange: (Boolean) -> Unit
) {
    // Placeholder state - in real app, use viewModel()
    val uiState = SettingsUiState(
        systemPrompt = "You are a helpful AI assistant running on-device.",
        temperature = 0.7f,
        maxTokens = 2048,
        modelPrimary = "litert-bridge/gemma-4-e4b",
        baseUrl = "http://localhost:8080",
        darkTheme = true,
        streamingEnabled = true,
        notificationsEnabled = true,
        backgroundAgentEnabled = false
    )

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSystemPromptChange = {},
        onTemperatureChange = {},
        onMaxTokensChange = {},
        onModelPrimaryChange = {},
        onBaseUrlChange = {},
        onDarkThemeChange = { isDark ->
            onThemeChange(isDark)
        },
        onStreamingEnabledChange = {},
        onNotificationsEnabledChange = {},
        onBackgroundAgentChange = {},
        onResetToDefaults = {},
        onSave = {}
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsRoutePreview() {
    SettingsRoute(
        onNavigateBack = {},
        onThemeChange = {}
    )
}

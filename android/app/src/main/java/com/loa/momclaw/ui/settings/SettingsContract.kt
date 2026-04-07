package com.loa.momclaw.ui.settings

import com.loa.momclaw.domain.model.AgentSettings

/**
 * UI State for Settings screen.
 */
data class SettingsState(
    val settings: AgentSettings = AgentSettings(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * Sealed class representing Settings screen events.
 */
sealed class SettingsEvent {
    data class UpdateSystemPrompt(val prompt: String) : SettingsEvent()
    data class UpdateTemperature(val temperature: Float) : SettingsEvent()
    data class UpdateMaxTokens(val tokens: Int) : SettingsEvent()
    data class UpdateDarkMode(val enabled: Boolean) : SettingsEvent()
    data class UpdateAutoSave(val enabled: Boolean) : SettingsEvent()
    object SaveSettings : SettingsEvent()
    object ResetSettings : SettingsEvent()
    object ClearError : SettingsEvent()
    object ClearSaveSuccess : SettingsEvent()
}

package com.loa.momclaw.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.domain.model.AgentSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadSettings()
    }

    /**
     * Handles settings screen events.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateSystemPrompt -> updateSystemPrompt(event.prompt)
            is SettingsEvent.UpdateTemperature -> updateTemperature(event.temperature)
            is SettingsEvent.UpdateMaxTokens -> updateMaxTokens(event.tokens)
            is SettingsEvent.UpdateDarkMode -> updateDarkMode(event.enabled)
            is SettingsEvent.UpdateAutoSave -> updateAutoSave(event.enabled)
            is SettingsEvent.SaveSettings -> saveSettings()
            is SettingsEvent.ResetSettings -> resetSettings()
            is SettingsEvent.ClearError -> clearError()
            is SettingsEvent.ClearSaveSuccess -> clearSaveSuccess()
        }
    }

    /**
     * Loads current settings from preferences.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            settingsPreferences.getSettings().collect { settings ->
                _state.update { it.copy(
                    settings = settings,
                    isLoading = false
                )}
            }
        }
    }

    /**
     * Updates system prompt in local state.
     */
    private fun updateSystemPrompt(prompt: String) {
        _state.update { it.copy(
            settings = it.settings.copy(systemPrompt = prompt)
        )}
    }

    /**
     * Updates temperature in local state.
     */
    private fun updateTemperature(temperature: Float) {
        _state.update { it.copy(
            settings = it.settings.copy(temperature = temperature)
        )}
    }

    /**
     * Updates max tokens in local state.
     */
    private fun updateMaxTokens(tokens: Int) {
        _state.update { it.copy(
            settings = it.settings.copy(maxTokens = tokens.coerceIn(1, 8192))
        )}
    }

    /**
     * Updates dark mode setting.
     */
    private fun updateDarkMode(enabled: Boolean) {
        _state.update { it.copy(
            settings = it.settings.copy(darkMode = enabled)
        )}
    }

    /**
     * Updates auto save setting.
     */
    private fun updateAutoSave(enabled: Boolean) {
        _state.update { it.copy(
            settings = it.settings.copy(autoSave = enabled)
        )}
    }

    /**
     * Saves settings to preferences.
     */
    private fun saveSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            settingsPreferences.updateSettings(_state.value.settings)
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        saveSuccess = true
                    )}
                    Log.d(TAG, "Settings saved successfully")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to save settings", error)
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Failed to save settings: ${error.message}"
                    )}
                }
        }
    }

    /**
     * Resets settings to defaults.
     */
    private fun resetSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            settingsPreferences.resetSettings()
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        saveSuccess = true
                    )}
                    Log.d(TAG, "Settings reset to defaults")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to reset settings", error)
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Failed to reset settings: ${error.message}"
                    )}
                }
        }
    }

    /**
     * Clears any error message.
     */
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clears the save success indicator.
     */
    private fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
}

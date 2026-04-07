package com.loa.momclaw.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loa.momclaw.data.local.preferences.SettingsPreferences
import com.loa.momclaw.domain.model.AgentConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelPrimary: String = "gemma-4e4b",
    val baseUrl: String = "http://localhost:8080",
    val darkTheme: Boolean = false,
    val streamingEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val backgroundAgentEnabled: Boolean = false,
    val hasChanges: Boolean = false
)

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var originalConfig: AgentConfig? = null

    init {
        loadSettings()
    }

    /**
     * Load current settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsPreferences.agentConfig.collect { config ->
                originalConfig = config
                _uiState.update { state ->
                    state.copy(
                        systemPrompt = config.systemPrompt,
                        temperature = config.temperature,
                        maxTokens = config.maxTokens,
                        modelPrimary = config.modelPrimary,
                        baseUrl = config.baseUrl
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsPreferences.darkTheme.collect { isDark ->
                _uiState.update { it.copy(darkTheme = isDark) }
            }
        }

        viewModelScope.launch {
            settingsPreferences.streamingEnabled.collect { enabled ->
                _uiState.update { it.copy(streamingEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settingsPreferences.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settingsPreferences.backgroundAgentEnabled.collect { enabled ->
                _uiState.update { it.copy(backgroundAgentEnabled = enabled) }
            }
        }
    }

    /**
     * Update system prompt
     */
    fun setSystemPrompt(prompt: String) {
        _uiState.update { it.copy(systemPrompt = prompt, hasChanges = true) }
    }

    /**
     * Update temperature
     */
    fun setTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp, hasChanges = true) }
    }

    /**
     * Update max tokens
     */
    fun setMaxTokens(tokens: Int) {
        _uiState.update { it.copy(maxTokens = tokens, hasChanges = true) }
    }

    /**
     * Update primary model
     */
    fun setModelPrimary(model: String) {
        _uiState.update { it.copy(modelPrimary = model, hasChanges = true) }
    }

    /**
     * Update base URL
     */
    fun setBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url, hasChanges = true) }
    }

    /**
     * Update dark theme preference
     */
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setDarkTheme(enabled)
            _uiState.update { it.copy(darkTheme = enabled) }
        }
    }

    /**
     * Update streaming enabled preference
     */
    fun setStreamingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setStreamingEnabled(enabled)
            _uiState.update { it.copy(streamingEnabled = enabled) }
        }
    }

    /**
     * Update notifications enabled preference
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setNotificationsEnabled(enabled)
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    /**
     * Update background agent enabled preference
     */
    fun setBackgroundAgentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setBackgroundAgentEnabled(enabled)
            _uiState.update { it.copy(backgroundAgentEnabled = enabled) }
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsPreferences.resetToDefaults()
            _uiState.update { it.copy(hasChanges = false) }
        }
    }

    /**
     * Save all changes
     */
    fun saveChanges() {
        viewModelScope.launch {
            val state = _uiState.value
            
            settingsPreferences.setSystemPrompt(state.systemPrompt)
            settingsPreferences.setTemperature(state.temperature)
            settingsPreferences.setMaxTokens(state.maxTokens)
            settingsPreferences.setModelPrimary(state.modelPrimary)
            settingsPreferences.setBaseUrl(state.baseUrl)
            
            _uiState.update { it.copy(hasChanges = false) }
        }
    }
}
package com.loa.momclaw.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.loa.momclaw.domain.model.AgentConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "momclaw_settings")

/**
 * DataStore-based preferences manager for app settings
 */
class SettingsPreferences(private val context: Context) {

    // Keys
    private object Keys {
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val MODEL_PRIMARY = stringPreferencesKey("model_primary")
        val BASE_URL = stringPreferencesKey("base_url")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BACKGROUND_AGENT_ENABLED = booleanPreferencesKey("background_agent_enabled")
        val LAST_CONVERSATION_ID = stringPreferencesKey("last_conversation_id")
    }

    // Default values
    private val defaults = AgentConfig()

    // Getters as Flows

    val systemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SYSTEM_PROMPT] ?: defaults.systemPrompt
    }

    val temperature: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.TEMPERATURE] ?: defaults.temperature
    }

    val maxTokens: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_TOKENS] ?: defaults.maxTokens
    }

    val modelPrimary: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODEL_PRIMARY] ?: defaults.modelPrimary
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.BASE_URL] ?: defaults.baseUrl
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true  // Dark theme by default
    }

    val streamingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.STREAMING_ENABLED] ?: true
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val backgroundAgentEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BACKGROUND_AGENT_ENABLED] ?: false
    }

    val lastConversationId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_CONVERSATION_ID]
    }

    // Combined config flow
    val agentConfig: Flow<AgentConfig> = context.dataStore.data.map { prefs ->
        AgentConfig(
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: defaults.systemPrompt,
            temperature = prefs[Keys.TEMPERATURE] ?: defaults.temperature,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: defaults.maxTokens,
            modelPrimary = prefs[Keys.MODEL_PRIMARY] ?: defaults.modelPrimary,
            baseUrl = prefs[Keys.BASE_URL] ?: defaults.baseUrl
        )
    }

    // Setters

    suspend fun setSystemPrompt(prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun setTemperature(temp: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMPERATURE] = temp.coerceIn(0f, 2f)
        }
    }

    suspend fun setMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_TOKENS] = tokens.coerceIn(256, 8192)
        }
    }

    suspend fun setModelPrimary(model: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MODEL_PRIMARY] = model
        }
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = url
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setStreamingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STREAMING_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setBackgroundAgentEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_AGENT_ENABLED] = enabled
        }
    }

    suspend fun setLastConversationId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[Keys.LAST_CONVERSATION_ID] = id
            } else {
                prefs.remove(Keys.LAST_CONVERSATION_ID)
            }
        }
    }

    // Reset to defaults
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYSTEM_PROMPT] = defaults.systemPrompt
            prefs[Keys.TEMPERATURE] = defaults.temperature
            prefs[Keys.MAX_TOKENS] = defaults.maxTokens
            prefs[Keys.MODEL_PRIMARY] = defaults.modelPrimary
            prefs[Keys.BASE_URL] = defaults.baseUrl
            // Keep UI preferences (dark theme, etc.) as they are
        }
    }
}

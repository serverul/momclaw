package com.loa.momclaw.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.loa.momclaw.domain.model.AgentSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages application settings using DataStore.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        private val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val KEY_CURRENT_CONVERSATION_ID = longPreferencesKey("current_conversation_id")

        private const val DEFAULT_SYSTEM_PROMPT = "You are MOMCLAW, a helpful AI assistant running offline on this device."
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_MAX_TOKENS = 2048
        private const val DEFAULT_DARK_MODE = false
        private const val DEFAULT_AUTO_SAVE = true
    }

    /**
     * Gets agent settings as a Flow.
     */
    fun getSettings(): Flow<AgentSettings> {
        return context.dataStore.data.map { preferences ->
            AgentSettings(
                systemPrompt = preferences[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT,
                temperature = preferences[KEY_TEMPERATURE] ?: DEFAULT_TEMPERATURE,
                maxTokens = preferences[KEY_MAX_TOKENS] ?: DEFAULT_MAX_TOKENS,
                darkMode = preferences[KEY_DARK_MODE] ?: DEFAULT_DARK_MODE,
                autoSave = preferences[KEY_AUTO_SAVE] ?: DEFAULT_AUTO_SAVE
            )
        }
    }

    /**
     * Updates agent settings.
     */
    suspend fun updateSettings(settings: AgentSettings): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[KEY_SYSTEM_PROMPT] = settings.systemPrompt
                preferences[KEY_TEMPERATURE] = settings.temperature
                preferences[KEY_MAX_TOKENS] = settings.maxTokens
                preferences[KEY_DARK_MODE] = settings.darkMode
                preferences[KEY_AUTO_SAVE] = settings.autoSave
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resets settings to defaults.
     */
    suspend fun resetSettings(): Result<Unit> {
        return updateSettings(
            AgentSettings(
                systemPrompt = DEFAULT_SYSTEM_PROMPT,
                temperature = DEFAULT_TEMPERATURE,
                maxTokens = DEFAULT_MAX_TOKENS,
                darkMode = DEFAULT_DARK_MODE,
                autoSave = DEFAULT_AUTO_SAVE
            )
        )
    }

    /**
     * Gets the current conversation ID.
     */
    suspend fun getCurrentConversationId(): Long {
        // Note: This is a simplified version. In production, you'd want to use
        // a more robust method to get the value from DataStore
        return context.dataStore.data.map { preferences ->
            preferences[KEY_CURRENT_CONVERSATION_ID] ?: 0L
        }.first()
    }

    /**
     * Sets the current conversation ID.
     */
    suspend fun setCurrentConversationId(conversationId: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENT_CONVERSATION_ID] = conversationId
        }
    }

    /**
     * Gets dark mode setting as Flow.
     */
    fun getDarkMode(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_DARK_MODE] ?: DEFAULT_DARK_MODE
        }
    }
}

/**
 * Extension function to get first value from Flow.
 */
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect { value ->
        result = value
        return@collect
    }
    return result ?: throw NoSuchElementException("Flow is empty")
}

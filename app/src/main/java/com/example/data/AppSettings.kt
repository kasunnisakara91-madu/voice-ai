package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "assistant_voice_settings"
)

enum class AssistantLanguage(val code: String, val displayName: String, val speechLocale: String) {
    AUTO("auto", "Auto (සිංහල + English)", "si-LK"),
    SINHALA("si", "සිංහල (Sinhala)", "si-LK"),
    ENGLISH("en", "English", "en-US");

    companion object {
        fun fromCode(code: String?): AssistantLanguage =
            entries.firstOrNull { it.code == code } ?: AUTO
    }
}

enum class GeminiModelOption(
    val modelId: String,
    val label: String,
    val description: String
) {
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        label = "Gemini 3.8 Flash (Recommended)",
        description = "Balanced speed and multilingual Sinhala/Singlish reasoning"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        label = "Gemini 3.5 Flash-Lite",
        description = "Ultra-fast responses for quick voice commands"
    ),
    GEMINI_4_ARGON(
        modelId = "gemini-4-argon",
        label = "Gemini 4 Argon",
        description = "Advanced reasoning (requires Firebase Blaze billing)"
    );

    companion object {
        fun fromId(id: String?): GeminiModelOption =
            entries.firstOrNull { it.modelId == id } ?: GEMINI_3_8_FLASH
    }
}

data class AppSettingsState(
    val selectedModel: GeminiModelOption = GeminiModelOption.GEMINI_3_8_FLASH,
    val preferredLanguage: AssistantLanguage = AssistantLanguage.AUTO,
    val speechInputLocale: String = "si-LK",
    val selectedVoiceName: String = "",
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val autoSpeak: Boolean = true,
    val darkTheme: Boolean = true,
    val saveHistory: Boolean = true,
    val includeDeviceContext: Boolean = true,
    val jarvisStandbyEnabled: Boolean = false,
    val jarvisAutoListen: Boolean = true
)

class AppSettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val SPEECH_INPUT_LOCALE = stringPreferencesKey("speech_input_locale")
        val SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val INCLUDE_DEVICE_CONTEXT = booleanPreferencesKey("include_device_context")
        val JARVIS_STANDBY_ENABLED = booleanPreferencesKey("jarvis_standby_enabled")
        val JARVIS_AUTO_LISTEN = booleanPreferencesKey("jarvis_auto_listen")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        AppSettingsState(
            selectedModel = GeminiModelOption.fromId(prefs[Keys.SELECTED_MODEL]),
            preferredLanguage = AssistantLanguage.fromCode(prefs[Keys.PREFERRED_LANGUAGE]),
            speechInputLocale = prefs[Keys.SPEECH_INPUT_LOCALE] ?: "si-LK",
            selectedVoiceName = prefs[Keys.SELECTED_VOICE_NAME] ?: "",
            speechSpeed = (prefs[Keys.SPEECH_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
            speechPitch = (prefs[Keys.SPEECH_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
            autoSpeak = prefs[Keys.AUTO_SPEAK] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            saveHistory = prefs[Keys.SAVE_HISTORY] ?: true,
            includeDeviceContext = prefs[Keys.INCLUDE_DEVICE_CONTEXT] ?: true,
            jarvisStandbyEnabled = prefs[Keys.JARVIS_STANDBY_ENABLED] ?: false,
            jarvisAutoListen = prefs[Keys.JARVIS_AUTO_LISTEN] ?: true
        )
    }

    suspend fun setSelectedModel(model: GeminiModelOption) {
        context.dataStore.edit { it[Keys.SELECTED_MODEL] = model.modelId }
    }

    suspend fun setPreferredLanguage(language: AssistantLanguage) {
        context.dataStore.edit {
            it[Keys.PREFERRED_LANGUAGE] = language.code
            if (language != AssistantLanguage.AUTO) {
                it[Keys.SPEECH_INPUT_LOCALE] = language.speechLocale
            }
        }
    }

    suspend fun setSpeechInputLocale(locale: String) {
        context.dataStore.edit { it[Keys.SPEECH_INPUT_LOCALE] = locale }
    }

    suspend fun setSelectedVoiceName(voiceName: String) {
        context.dataStore.edit { it[Keys.SELECTED_VOICE_NAME] = voiceName }
    }

    suspend fun setSpeechSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.SPEECH_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }

    suspend fun setIncludeDeviceContext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_DEVICE_CONTEXT] = enabled }
    }

    suspend fun setJarvisStandbyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.JARVIS_STANDBY_ENABLED] = enabled }
    }

    suspend fun setJarvisAutoListen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.JARVIS_AUTO_LISTEN] = enabled }
    }
}

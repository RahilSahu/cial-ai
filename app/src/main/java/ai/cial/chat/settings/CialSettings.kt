package ai.cial.chat.settings

import ai.cial.chat.polish.OutputMode
import ai.cial.chat.polish.Tone
import android.content.Context
import android.content.SharedPreferences

/**
 * Reads and writes user settings in app-private SharedPreferences.
 * The API key never leaves this storage except as an Authorization header
 * sent over HTTPS to the provider the user picked.
 */
class CialSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var provider: Provider
        get() = Provider.fromName(prefs.getString(KEY_PROVIDER, null))
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.name).apply()

    var apiKey: String
        get() = SettingsRules.sanitizeApiKey(prefs.getString(KEY_API_KEY, null))
        set(value) = prefs.edit().putString(KEY_API_KEY, SettingsRules.sanitizeApiKey(value)).apply()

    var model: String
        get() = SettingsRules.sanitizeModel(prefs.getString(KEY_MODEL, null), provider)
        set(value) = prefs.edit()
            .putString(KEY_MODEL, SettingsRules.sanitizeModel(value, provider)).apply()

    var inputLanguage: InputLanguage
        get() = InputLanguage.fromName(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.name).apply()

    var outputMode: OutputMode
        get() = SettingsRules.outputModeFromName(prefs.getString(KEY_OUTPUT_MODE, null))
        set(value) = prefs.edit().putString(KEY_OUTPUT_MODE, value.name).apply()

    var tone: Tone
        get() = SettingsRules.toneFromName(prefs.getString(KEY_TONE, null))
        set(value) = prefs.edit().putString(KEY_TONE, value.name).apply()

    val hasApiKey: Boolean get() = apiKey.isNotEmpty()

    companion object {
        private const val PREFS_NAME = "cial_settings"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_LANGUAGE = "input_language"
        private const val KEY_OUTPUT_MODE = "output_mode"
        private const val KEY_TONE = "tone"
    }
}

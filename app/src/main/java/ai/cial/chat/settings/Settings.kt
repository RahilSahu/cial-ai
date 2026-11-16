package ai.cial.chat.settings

import ai.cial.chat.polish.OutputMode
import ai.cial.chat.polish.Tone

/** AI providers with an OpenAI-compatible chat completions endpoint. */
enum class Provider(
    val label: String,
    val chatCompletionsUrl: String,
    val defaultModel: String
) {
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1/chat/completions",
        "openai/gpt-4o-mini"
    ),
    OPENAI(
        "OpenAI",
        "https://api.openai.com/v1/chat/completions",
        "gpt-4o-mini"
    ),
    GEMINI(
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        "gemini-2.0-flash"
    );

    companion object {
        fun fromName(name: String?): Provider =
            entries.firstOrNull { it.name == name } ?: OPENROUTER
    }
}

/** Languages the user can dictate in, with Android speech locale tags. */
enum class InputLanguage(val label: String, val localeTag: String) {
    HINDI("Hindi", "hi-IN"),
    TAMIL("Tamil", "ta-IN"),
    TELUGU("Telugu", "te-IN"),
    MALAYALAM("Malayalam", "ml-IN"),
    KANNADA("Kannada", "kn-IN"),
    ENGLISH("English", "en-IN");

    companion object {
        fun fromName(name: String?): InputLanguage =
            entries.firstOrNull { it.name == name } ?: HINDI

        fun fromLocaleTag(tag: String?): InputLanguage =
            entries.firstOrNull { it.localeTag == tag } ?: HINDI
    }
}

/** Pure validation and defaulting rules for settings values. */
object SettingsRules {

    /** Trims an API key; an empty result means "no key configured". */
    fun sanitizeApiKey(raw: String?): String = raw?.trim().orEmpty()

    /** Falls back to the provider default model when the field is blank. */
    fun sanitizeModel(raw: String?, provider: Provider): String =
        raw?.trim()?.takeIf { it.isNotEmpty() } ?: provider.defaultModel

    /** Only HTTPS endpoints are acceptable for sending an API key. */
    fun isAcceptableEndpoint(url: String): Boolean =
        url.startsWith("https://") && url.contains(".")

    fun outputModeFromName(name: String?): OutputMode =
        OutputMode.entries.firstOrNull { it.name == name } ?: OutputMode.ENGLISH

    fun toneFromName(name: String?): Tone =
        Tone.entries.firstOrNull { it.name == name } ?: Tone.NEUTRAL
}

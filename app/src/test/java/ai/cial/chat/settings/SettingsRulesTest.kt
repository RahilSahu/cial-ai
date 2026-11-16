package ai.cial.chat.settings

import ai.cial.chat.polish.OutputMode
import ai.cial.chat.polish.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {

    @Test
    fun sanitizeApiKeyTrimsWhitespace() {
        assertEquals("sk-abc123", SettingsRules.sanitizeApiKey("  sk-abc123  "))
    }

    @Test
    fun sanitizeApiKeyNullAndBlankBecomeEmpty() {
        assertEquals("", SettingsRules.sanitizeApiKey(null))
        assertEquals("", SettingsRules.sanitizeApiKey("   "))
    }

    @Test
    fun openRouterDefaultModel() {
        assertEquals("openai/gpt-4o-mini", SettingsRules.sanitizeModel("", Provider.OPENROUTER))
        assertEquals("openai/gpt-4o-mini", SettingsRules.sanitizeModel(null, Provider.OPENROUTER))
    }

    @Test
    fun openAiDefaultModel() {
        assertEquals("gpt-4o-mini", SettingsRules.sanitizeModel(" ", Provider.OPENAI))
    }

    @Test
    fun geminiDefaultModel() {
        assertEquals("gemini-2.0-flash", SettingsRules.sanitizeModel("", Provider.GEMINI))
    }

    @Test
    fun sanitizeModelKeepsCustomValueTrimmed() {
        assertEquals("openai/gpt-4.1", SettingsRules.sanitizeModel("  openai/gpt-4.1 ", Provider.OPENROUTER))
    }

    @Test
    fun providerEndpointsAreTheDocumentedOnes() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            Provider.OPENROUTER.chatCompletionsUrl
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            Provider.OPENAI.chatCompletionsUrl
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            Provider.GEMINI.chatCompletionsUrl
        )
    }

    @Test
    fun endpointRuleAcceptsHttpsAndRejectsHttp() {
        assertTrue(SettingsRules.isAcceptableEndpoint("https://api.openai.com/v1/chat/completions"))
        assertFalse(SettingsRules.isAcceptableEndpoint("http://api.openai.com/v1/chat/completions"))
        assertFalse(SettingsRules.isAcceptableEndpoint("not a url"))
    }

    @Test
    fun providerFromNameFallsBackToOpenRouter() {
        assertEquals(Provider.OPENAI, Provider.fromName("OPENAI"))
        assertEquals(Provider.OPENROUTER, Provider.fromName("something-else"))
        assertEquals(Provider.OPENROUTER, Provider.fromName(null))
    }

    @Test
    fun inputLanguagesHaveExpectedLocaleTags() {
        assertEquals("hi-IN", InputLanguage.HINDI.localeTag)
        assertEquals("ta-IN", InputLanguage.TAMIL.localeTag)
        assertEquals("te-IN", InputLanguage.TELUGU.localeTag)
        assertEquals("ml-IN", InputLanguage.MALAYALAM.localeTag)
        assertEquals("kn-IN", InputLanguage.KANNADA.localeTag)
        assertEquals("en-IN", InputLanguage.ENGLISH.localeTag)
        assertEquals(6, InputLanguage.entries.size)
    }

    @Test
    fun inputLanguageLookupByTagAndFallback() {
        assertEquals(InputLanguage.TAMIL, InputLanguage.fromLocaleTag("ta-IN"))
        assertEquals(InputLanguage.HINDI, InputLanguage.fromLocaleTag("xx-YY"))
        assertEquals(InputLanguage.HINDI, InputLanguage.fromLocaleTag(null))
    }

    @Test
    fun outputModeAndToneFallbacks() {
        assertEquals(OutputMode.HINGLISH, SettingsRules.outputModeFromName("HINGLISH"))
        assertEquals(OutputMode.ENGLISH, SettingsRules.outputModeFromName("bogus"))
        assertEquals(OutputMode.ENGLISH, SettingsRules.outputModeFromName(null))
        assertEquals(Tone.FORMAL, SettingsRules.toneFromName("FORMAL"))
        assertEquals(Tone.NEUTRAL, SettingsRules.toneFromName("bogus"))
        assertEquals(Tone.NEUTRAL, SettingsRules.toneFromName(null))
    }
}

package ai.cial.chat.polish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private fun assertCommon(prompt: String) {
        assertTrue(prompt.contains("Return only the rewritten text"))
        assertTrue(prompt.contains("Cial.ai"))
    }

    @Test
    fun englishNeutral() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.ENGLISH, Tone.NEUTRAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("polished English"))
        assertTrue(prompt.contains("natural, professional English"))
        assertTrue(prompt.contains("balanced, neutral tone"))
    }

    @Test
    fun englishCasual() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.ENGLISH, Tone.CASUAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("polished English"))
        assertTrue(prompt.contains("casual, friendly tone"))
    }

    @Test
    fun englishFormal() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.ENGLISH, Tone.FORMAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("polished English"))
        assertTrue(prompt.contains("formal, professional tone"))
    }

    @Test
    fun hindiNeutral() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINDI, Tone.NEUTRAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Hindi written in Devanagari script"))
        assertTrue(prompt.contains("balanced, neutral tone"))
    }

    @Test
    fun hindiCasual() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINDI, Tone.CASUAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Devanagari"))
        assertTrue(prompt.contains("casual, friendly tone"))
    }

    @Test
    fun hindiFormal() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINDI, Tone.FORMAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Devanagari"))
        assertTrue(prompt.contains("formal, professional tone"))
    }

    @Test
    fun hinglishNeutral() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINGLISH, Tone.NEUTRAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("Roman (Latin) script"))
        assertTrue(prompt.contains("balanced, neutral tone"))
    }

    @Test
    fun hinglishCasual() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINGLISH, Tone.CASUAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("casual, friendly tone"))
    }

    @Test
    fun hinglishFormal() {
        val prompt = PromptBuilder.systemPrompt(OutputMode.HINGLISH, Tone.FORMAL)
        assertCommon(prompt)
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("formal, professional tone"))
    }

    @Test
    fun messagesStructure() {
        val messages = PromptBuilder.messages("namaste, kal milte hain", OutputMode.ENGLISH, Tone.NEUTRAL)
        assertEquals(2, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("user", messages[1].role)
        assertEquals("namaste, kal milte hain", messages[1].content)
    }

    @Test
    fun everyCombinationReturnsOnlyTheText() {
        for (mode in OutputMode.entries) {
            for (tone in Tone.entries) {
                val prompt = PromptBuilder.systemPrompt(mode, tone)
                assertTrue("Missing return-only rule for $mode/$tone",
                    prompt.contains("Return only the rewritten text"))
                assertTrue("Missing no-explanations rule for $mode/$tone",
                    prompt.contains("Do not add explanations"))
            }
        }
    }
}

package ai.cial.chat.polish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptCleanerTest {

    @Test
    fun collapsesWhitespaceRuns() {
        assertEquals("hello world again", TranscriptCleaner.clean("hello   world\n\t again"))
    }

    @Test
    fun trimsEnds() {
        assertEquals("hello", TranscriptCleaner.clean("   hello   "))
    }

    @Test
    fun removesSpaceBeforePunctuation() {
        assertEquals("hello, how are you?", TranscriptCleaner.clean("hello , how are you ?"))
    }

    @Test
    fun addsSpaceAfterPunctuationBeforeLetter() {
        assertEquals("hello. How are you? Fine, thanks", TranscriptCleaner.clean("hello.How are you?Fine,thanks"))
    }

    @Test
    fun preservesNumbersWithCommasAndDecimals() {
        val input = "I paid 1,000 rupees for 3.5 kg"
        assertEquals(input, TranscriptCleaner.clean(input))
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertEquals("", TranscriptCleaner.clean(""))
        assertEquals("", TranscriptCleaner.clean("    "))
    }

    @Test
    fun sentenceCaseUppercasesFirstLetter() {
        assertEquals("Hello world", TranscriptCleaner.sentenceCase("hello world"))
    }

    @Test
    fun sentenceCaseLeavesUppercaseAlone() {
        assertEquals("Hello world", TranscriptCleaner.sentenceCase("Hello world"))
    }

    @Test
    fun sentenceCaseHandlesEmptyAndNonLetter() {
        assertEquals("", TranscriptCleaner.sentenceCase(""))
        assertEquals("123 go", TranscriptCleaner.sentenceCase("123 go"))
    }

    @Test
    fun isBlankDetectsUsableText() {
        assertTrue(TranscriptCleaner.isBlank("   "))
        assertTrue(TranscriptCleaner.isBlank(""))
        assertFalse(TranscriptCleaner.isBlank(" hi "))
    }
}

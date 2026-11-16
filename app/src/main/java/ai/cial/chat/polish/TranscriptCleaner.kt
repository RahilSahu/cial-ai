package ai.cial.chat.polish

/**
 * Small cleanup helpers for raw speech recognition output. Pure Kotlin so
 * they can be unit tested on the JVM. These only fix mechanical issues
 * (spacing); they never change words.
 */
object TranscriptCleaner {

    private val whitespaceRun = Regex("\\s+")
    private val spaceBeforePunctuation = Regex("\\s+([,.!?;:])")
    private val missingSpaceAfterPunctuation = Regex("([,.!?;:])([\\p{L}])")

    /** Collapses whitespace runs and fixes spacing around punctuation. */
    fun clean(raw: String): String {
        var result = raw.replace(whitespaceRun, " ").trim()
        result = result.replace(spaceBeforePunctuation, "$1")
        result = result.replace(missingSpaceAfterPunctuation, "$1 $2")
        return result
    }

    /** Uppercases the first letter of the text, if it starts with a letter. */
    fun sentenceCase(text: String): String {
        if (text.isEmpty()) return text
        val first = text[0]
        return if (first.isLetter() && first.isLowerCase()) {
            first.uppercaseChar() + text.substring(1)
        } else {
            text
        }
    }

    /** True when a transcript has no usable words. */
    fun isBlank(raw: String): Boolean = raw.replace(whitespaceRun, " ").trim().isEmpty()
}

package ai.cial.chat.polish

/** One chat message in an OpenAI-compatible chat completions request. */
data class ChatMessage(val role: String, val content: String)

/** The language the polished message should be written in. */
enum class OutputMode(val label: String) {
    ENGLISH("English"),
    HINDI("Hindi"),
    HINGLISH("Hinglish")
}

/** The tone applied to the polished message. */
enum class Tone(val label: String) {
    NEUTRAL("Neutral"),
    CASUAL("Casual"),
    FORMAL("Formal")
}

/**
 * Builds the prompts for the AI polish step. Pure Kotlin with no Android
 * dependencies so it can be unit tested on the JVM.
 */
object PromptBuilder {

    fun systemPrompt(mode: OutputMode, tone: Tone): String {
        val modeInstruction = when (mode) {
            OutputMode.ENGLISH ->
                "Rewrite the message in polished English. The result must read as natural, " +
                    "professional English, the way a fluent speaker would write it in a chat " +
                    "or an email. Fix grammar, spelling and punctuation. Do not translate word " +
                    "for word; keep the meaning, names, numbers and intent exactly as spoken."
            OutputMode.HINDI ->
                "Rewrite the message in Hindi written in Devanagari script. Keep the meaning, " +
                    "names, numbers and intent exactly as spoken, and fix grammar and sentence " +
                    "flow so the result reads naturally in Hindi."
            OutputMode.HINGLISH ->
                "Rewrite the message in Hinglish, which is Hindi written in Roman (Latin) " +
                    "script, in the relaxed style people use when chatting with friends. Keep " +
                    "any English words the speaker used. Keep the meaning, names, numbers and " +
                    "intent exactly as spoken."
        }
        val toneInstruction = when (tone) {
            Tone.NEUTRAL -> "Use a balanced, neutral tone."
            Tone.CASUAL -> "Use a casual, friendly tone, relaxed and warm, like a message to a friend."
            Tone.FORMAL -> "Use a formal, professional tone, courteous and suitable for a work email."
        }
        return "You are the writing engine inside Cial.ai, a voice dictation assistant. " +
            "The user dictated a message by voice, possibly in Hindi, Tamil, Telugu, Malayalam, " +
            "Kannada or English, and speech recognition may have introduced small errors. " +
            modeInstruction + " " + toneInstruction + " " +
            "Return only the rewritten text. Do not add explanations, quotation marks, " +
            "prefixes or notes. If the input is already good, return it lightly cleaned up."
    }

    fun messages(transcript: String, mode: OutputMode, tone: Tone): List<ChatMessage> =
        listOf(
            ChatMessage(role = "system", content = systemPrompt(mode, tone)),
            ChatMessage(role = "user", content = transcript)
        )
}

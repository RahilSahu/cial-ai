package ai.cial.chat.polish

import ai.cial.chat.settings.CialSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Calls the user-selected AI provider (OpenAI-compatible chat completions)
 * to rewrite a transcript. The API key comes from the user's own settings
 * and is sent only to that provider over HTTPS.
 */
class PolishClient(private val settings: CialSettings) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Rewrites [transcript] according to [mode] and [tone]. */
    suspend fun polish(transcript: String, mode: OutputMode, tone: Tone): Result<String> {
        val messages = PromptBuilder.messages(transcript, mode, tone)
        return chat(messages)
    }

    /** Makes the smallest possible chat call to verify key, model and reachability. */
    suspend fun testConnection(): Result<String> {
        val messages = listOf(ChatMessage("user", "Reply with the single word: OK"))
        return chat(messages)
    }

    private suspend fun chat(messages: List<ChatMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            val key = settings.apiKey
            if (key.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No API key is set"))
            }
            val provider = settings.provider
            val body = JSONObject().apply {
                put("model", settings.model)
                put("temperature", 0.3)
                put("messages", JSONArray().apply {
                    messages.forEach { message ->
                        put(JSONObject().apply {
                            put("role", message.role)
                            put("content", message.content)
                        })
                    }
                })
            }
            val request = Request.Builder()
                .url(provider.chatCompletionsUrl)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("HTTP ${response.code}: ${providerError(responseBody)}")
                        )
                    }
                    val content = parseContent(responseBody)
                    if (content.isNullOrBlank()) {
                        Result.failure(IOException("The provider returned an empty response"))
                    } else {
                        Result.success(content.trim())
                    }
                }
            } catch (e: IOException) {
                Result.failure(IOException("Network error: ${e.message ?: "could not reach the provider"}"))
            } catch (e: Exception) {
                Result.failure(IOException("Unexpected error: ${e.message ?: "unknown"}"))
            }
        }

    private fun parseContent(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            choices.getJSONObject(0).optJSONObject("message")?.optString("content")
        } catch (e: Exception) {
            null
        }
    }

    private fun providerError(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            error?.optString("message")?.takeIf { it.isNotBlank() }
                ?: "request failed"
        } catch (e: Exception) {
            "request failed"
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

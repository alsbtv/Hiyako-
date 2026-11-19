package com.hiyako.app.providers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Streams chat completions from any OpenAI-compatible endpoint (SSE "data:" lines).
 * Returns Result so callers can show the error and offer another account on quota errors.
 */
object OpenAiCompatClient {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    /** Builds the messages array from (role, text) pairs, e.g. ("user", "hi"). */
    fun messagesOf(turns: List<Pair<String, String>>): JSONArray {
        val array = JSONArray()
        for ((role, text) in turns) {
            array.put(JSONObject().put("role", role).put("content", text))
        }
        return array
    }

    /** True when the error looks like a quota, credit or rate-limit failure. */
    fun isQuotaError(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("HTTP 429") ||
            message.contains("HTTP 402") ||
            message.contains("credit", ignoreCase = true) ||
            message.contains("quota", ignoreCase = true)
    }

    suspend fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: JSONArray,
        onDelta: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("stream", true)
            val builder = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/chat/completions")
                .post(payload.toString().toRequestBody(jsonType))
                .header("Accept", "text/event-stream")
            if (apiKey.isNotEmpty()) builder.header("Authorization", "Bearer $apiKey")

            http.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val detail = response.body?.string()?.take(300).orEmpty()
                    error("HTTP ${response.code} $detail")
                }
                val source = response.body?.source() ?: error("Empty response body")
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val delta = JSONObject(data)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content", "")
                    if (!delta.isNullOrEmpty()) onDelta(delta)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

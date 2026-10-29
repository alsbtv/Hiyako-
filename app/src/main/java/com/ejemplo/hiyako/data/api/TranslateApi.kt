package com.ejemplo.hiyako.data.api

import com.ejemplo.hiyako.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URLEncoder

@Singleton
class TranslateApi @Inject constructor() {
    private val client = OkHttpClient()

    suspend fun translateToArabic(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        RateLimiter.throttleTranslate()
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val lingvaReq = Request.Builder()
                .url("https://lingva.ml/api/v1/en/ar/$encoded")
                .build()

            client.newCall(lingvaReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val translation = json.optString("translation", "")
                        if (translation.isNotBlank()) return@withContext translation
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to LibreTranslate
        }

        RateLimiter.throttleTranslate()
        try {
            val jsonBody = JSONObject()
                .put("q", text)
                .put("source", "en")
                .put("target", "ar")
                .put("format", "text")

            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://libretranslate.com/translate")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string()
                    if (respBody != null) {
                        val json = JSONObject(respBody)
                        val translated = json.optString("translatedText", "")
                        if (translated.isNotBlank()) return@withContext translated
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return@withContext text
    }
}

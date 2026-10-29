package com.ejemplo.hiyako.data.api

import com.ejemplo.hiyako.data.model.ScrapedAnime
import com.ejemplo.hiyako.utils.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URLEncoder

@Singleton
class JikanApi @Inject constructor() {
    private val client = OkHttpClient()

    suspend fun getAnimeDetails(title: String): ScrapedAnime? = withContext(Dispatchers.IO) {
        RateLimiter.throttleJikan()
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://api.jikan.moe/v4/anime?q=$encodedTitle&limit=1"
            val request = Request.Builder().url(workerUrl).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val data = json.optJSONArray("data")?.optJSONObject(0) ?: return@withContext null

                val synopsis = data.optString("synopsis", null)
                val score = data.optDouble("score", 0.0).let { if (it > 0) it else null }
                val images = data.optJSONObject("images")?.optJSONObject("jpg")
                val posterUrl = images?.optString("large_image_url", images.optString("image_url", "")) ?: ""

                ScrapedAnime(
                    title = title,
                    url = "",
                    posterUrl = posterUrl,
                    synopsis = synopsis,
                    score = score
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

package com.ejemplo.hiyako.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.ejemplo.hiyako.data.model.ScrapedAnime

object JikanApi {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build()

    suspend fun getAnimeDetails(title: String): ScrapedAnime? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val targetUrl = "https://api.jikan.moe/v4/anime?q=$encodedTitle&limit=1"
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$targetUrl"
            
            val request = Request.Builder().url(workerUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val jsonString = response.body?.string() ?: ""
            val data = JSONObject(jsonString).optJSONArray("data") ?: return@withContext null
            if (data.length() == 0) return@withContext null

            val anime = data.getJSONObject(0)
            val images = anime.optJSONObject("images")?.optJSONObject("jpg")
            val poster = images?.optString("large_image_url") ?: images?.optString("image_url") ?: ""
            
            return@withContext ScrapedAnime(
                title = anime.optString("title"),
                url = "jikan_${anime.optInt("mal_id")}",
                posterUrl = poster,
                bannerUrl = poster, // Jikan doesn't provide banners, use poster
                synopsis = anime.optString("synopsis"),
                score = anime.optDouble("score", 0.0),
                status = anime.optString("status"),
                episodeNumber = anime.optInt("episodes").toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

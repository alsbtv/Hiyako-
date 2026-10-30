package com.ejemplo.hiyako.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.ejemplo.hiyako.data.model.ScrapedAnime

object KitsuApi {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build()

    suspend fun getAnimeDetails(title: String): ScrapedAnime? = withContext(Dispatchers.IO) {
        try {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val targetUrl = "https://kitsu.io/api/edge/anime?filter[text]=$encodedTitle&page[limit]=1"
            
            val request = Request.Builder().url(targetUrl)
                .addHeader("Accept", "application/vnd.api+json")
                .addHeader("Content-Type", "application/vnd.api+json")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val jsonString = response.body?.string() ?: ""
            val data = JSONObject(jsonString).optJSONArray("data") ?: return@withContext null
            if (data.length() == 0) return@withContext null

            val anime = data.getJSONObject(0).optJSONObject("attributes") ?: return@withContext null
            val titles = anime.optJSONObject("titles")
            val poster = anime.optJSONObject("posterImage")?.optString("large") ?: ""
            val cover = anime.optJSONObject("coverImage")?.optString("large") ?: poster
            
            return@withContext ScrapedAnime(
                title = titles?.optString("en") ?: titles?.optString("en_jp") ?: anime.optString("canonicalTitle"),
                url = "kitsu_${data.getJSONObject(0).optString("id")}",
                posterUrl = poster,
                bannerUrl = cover,
                synopsis = anime.optString("synopsis"),
                score = anime.optDouble("averageRating", 0.0) / 10.0, // Kitsu uses 0-100
                status = anime.optString("status"),
                episodeNumber = anime.optInt("episodeCount").toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

package com.ejemplo.hiyako.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.ejemplo.hiyako.data.model.ScrapedAnime

object AniListApi {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build()

    suspend fun getAnimeDetails(title: String): ScrapedAnime? = withContext(Dispatchers.IO) {
        try {
            val query = """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    id
                    title { romaji english native }
                    coverImage { extraLarge }
                    bannerImage
                    description
                    averageScore
                    status
                    episodes
                    genres
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().put("query", query).put("variables", JSONObject().put("search", title)).toString()
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://graphql.anilist.co"
            val request = Request.Builder().url(workerUrl).post(jsonBody.toRequestBody("application/json".toMediaType())).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val jsonString = response.body?.string() ?: ""
            val media = JSONObject(jsonString).optJSONObject("data")?.optJSONObject("Media") ?: return@withContext null
            
            val titleObj = media.optJSONObject("title")
            val poster = media.optJSONObject("coverImage")?.optString("extraLarge") ?: ""
            val banner = media.optString("bannerImage").takeIf { it != "null" } ?: poster
            
            return@withContext ScrapedAnime(
                title = titleObj?.optString("english")?.takeIf { it != "null" } ?: titleObj?.optString("romaji") ?: title,
                url = "anilist_${media.optInt("id")}",
                posterUrl = poster,
                bannerUrl = banner,
                synopsis = media.optString("description"),
                score = media.optDouble("averageScore", 0.0),
                status = media.optString("status"),
                episodeNumber = media.optInt("episodes").toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

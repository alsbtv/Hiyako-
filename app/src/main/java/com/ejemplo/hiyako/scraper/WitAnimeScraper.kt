package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.util.Log

class WitAnimeScraper @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        try {
            val query = """
                query {
                  Page(page: 1, perPage: 20) {
                    media(type: ANIME, sort: POPULARITY_DESC) {
                      id
                      title { romaji english }
                      coverImage { extraLarge large }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().put("query", query).toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://graphql.anilist.co"
            val request = Request.Builder().url(workerUrl).post(requestBody).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

            val jsonString = response.body?.string() ?: ""
            val jsonObject = JSONObject(jsonString)
            val mediaArray = jsonObject.getJSONObject("data").getJSONObject("Page").getJSONArray("media")

            val animeList = mutableListOf<ScrapedAnime>()
            for (i in 0 until mediaArray.length()) {
                val animeObj = mediaArray.getJSONObject(i)
                val titleObj = animeObj.getJSONObject("title")
                val title = if (titleObj.isNull("english")) titleObj.getString("romaji") else titleObj.getString("english")
                val url = "https://anilist.co/anime/${animeObj.getInt("id")}"
                val coverImage = animeObj.getJSONObject("coverImage")
                val poster = if (coverImage.isNull("extraLarge")) coverImage.getString("large") else coverImage.getString("extraLarge")
                animeList.add(ScrapedAnime(title, url, poster))
            }
            return@withContext animeList
        } catch (e: Exception) {
            Log.e("ScraperError", "Error: ${e.message}")
            return@withContext emptyList()
        }
    }
}

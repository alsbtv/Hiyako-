package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        // Using corsproxy.io for faster bypass
        val targetUrl = "https://api.jikan.moe/v4/top/anime"
        val proxyUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=" + targetUrl
        
        val request = Request.Builder().url(proxyUrl).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }
        
        val jsonString = response.body?.string() ?: ""
        val jsonObject = JSONObject(jsonString)
        val dataArray = jsonObject.getJSONArray("data")
        
        val animeList = mutableListOf<ScrapedAnime>()
        for (i in 0 until dataArray.length()) {
            val animeObj = dataArray.getJSONObject(i)
            val title = animeObj.getString("title")
            val url = animeObj.getString("url")
            val imagesObj = animeObj.getJSONObject("images").getJSONObject("jpg")
            val poster = imagesObj.getString("image_url")
            animeList.add(ScrapedAnime(title, url, poster))
        }
        return@withContext animeList
    }
}

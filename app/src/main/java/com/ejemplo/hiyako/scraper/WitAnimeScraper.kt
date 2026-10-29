package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WitAnimeScraper @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        val targetUrl = "https://witanime.com/"
        val proxyUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=" + targetUrl
        val request = Request.Builder().url(proxyUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

        val html = response.body?.string() ?: ""
        val doc = Jsoup.parse(html)
        val animeList = mutableListOf<ScrapedAnime>()

        val items = doc.select(".anime-card-container")
        for (item in items) {
            val title = item.select(".anime-card-title").text()
            val link = item.select("h3 > a").attr("href")
            val poster = item.select("img.img-responsive").attr("src")
            if (title.isNotEmpty() && poster.isNotEmpty()) {
                val fullPoster = if (poster.startsWith("//")) "https:$poster" else poster
                animeList.add(ScrapedAnime(title, link, fullPoster))
            }
        }
        if (animeList.isEmpty()) {
            throw Exception("Parsed but found 0 anime. Selectors are wrong.")
        }
        return@withContext animeList
    }
}

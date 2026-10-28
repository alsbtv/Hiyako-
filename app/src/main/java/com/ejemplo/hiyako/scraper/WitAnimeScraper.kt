package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WitAnimeScraper @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            chain.proceed(request)
        }.build()

    private suspend fun getDocument(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return@withContext Jsoup.parse(response.body?.string() ?: "")
    }

    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        val doc = getDocument("https://witanime.cyou/")
        val animeList = mutableListOf<ScrapedAnime>()
        val items = doc.select(".anime-card-container")
        for (item in items) {
            val title = item.select(".anime-card-title").text()
            val link = item.select("a").attr("href")
            val poster = item.select("img").attr("src")
            if (title.isNotEmpty()) animeList.add(ScrapedAnime(title, link, poster))
        }
        return@withContext animeList
    }
}

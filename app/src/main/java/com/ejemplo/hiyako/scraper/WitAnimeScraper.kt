package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
            val request = Request.Builder()
                .url("https://anime3rb.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val animeList = mutableListOf<ScrapedAnime>()

            val episodeLinks = doc.select("a[href*=/episode/]")
            
            for (link in episodeLinks) {
                val href = link.attr("href")
                val rawTitle = link.text().trim()
                var img = link.select("img").attr("data-src")
                if (img.isEmpty()) img = link.select("img").attr("src")
                
                if (rawTitle.isNotEmpty() && img.isNotEmpty() && href.contains("/episode/")) {
                    val fullImg = if (img.startsWith("//")) "https:$img" else img
                    
                    // Extract episode number safely using Kotlin Regex
                    var episodeNum = ""
                    var cleanTitle = rawTitle
                    
                    val epRegex = Regex("""(?:الحلقة|Episode|Ep)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    val match = epRegex.find(rawTitle)
                    if (match != null) {
                        episodeNum = match.groupValues[1] // Correct syntax is groupValues
                        cleanTitle = rawTitle.replace(match.value, "").trim()
                    }
                    
                    animeList.add(ScrapedAnime(cleanTitle, href, fullImg, episodeNum))
                }
            }
            
            return@withContext animeList.distinctBy { it.url }.take(20)
            
        } catch (e: Exception) {
            Log.e("ScraperError", "Error: ${e.message}")
            throw e
        }
    }
}

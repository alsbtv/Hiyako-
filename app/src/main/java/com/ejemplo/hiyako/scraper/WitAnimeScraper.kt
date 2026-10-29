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
            // استخدام المتصفح الوهمي لتجاوز الحظر
            val request = Request.Builder()
                .url("https://anime3rb.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val animeList = mutableListOf<ScrapedAnime>()

            // استخراج الحلقات من الصفحة الرئيسية بناءً على كود بايثون
            val episodeLinks = doc.select("a[href*=/episode/]")
            
            for (link in episodeLinks) {
                val href = link.attr("href")
                val title = link.text().trim()
                val img = link.select("img").attr("src")
                
                if (title.isNotEmpty() && img.isNotEmpty() && href.contains("/episode/")) {
                    val fullImg = if (img.startsWith("//")) "https:$img" else img
                    
                    // Extraer número de episodio del título
                    val epRegex = "(\\d+)".toRegex()
                    val matchResult = epRegex.find(title)
                    val epNumber = matchResult?.groupValue(1) ?: "1"
                    
                    // Limpiar título quitando el número de episodio
                    val cleanTitle = title.replace(epNumber, "").replace("الحلقة", "").trim()
                    
                    animeList.add(ScrapedAnime(cleanTitle, epNumber, href, fullImg))
                }
            }
            
            // إزالة التكرار
            return@withContext animeList.distinctBy { it.url }.take(20)
            
        } catch (e: Exception) {
            Log.e("ScraperError", "Error: ${e.message}")
            throw e
        }
    }
}

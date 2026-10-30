package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.ejemplo.hiyako.data.model.ScrapedAnime

class Anime3rbScraper @Inject constructor() : BaseScraper {
    override val name = "Anime3rb"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        try {
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://anime3rb.com/"
            val request = Request.Builder().url(workerUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val animeList = mutableListOf<ScrapedAnime>()

            // Search for episode links using Fallback Strategy
            val items = doc.select("a[href*=/episode/]")
            for (link in items) {
                val href = link.attr("href")
                var img = link.selectFirst("img")?.attr("data-src") ?: link.selectFirst("img")?.attr("src") ?: ""
                if (img.startsWith("//")) img = "https:$img"
                var title = link.selectFirst("h3")?.text() ?: link.selectFirst("h4")?.text() ?: link.text().trim()
                
                if (title.isNotEmpty() && img.isNotEmpty() && href.contains("/episode/")) {
                    var epNum = ""
                    val match = Regex("""(?:الحلقة|Episode|Ep)\s*(\d+)""").find(title)
                    if (match != null) { 
                        epNum = match.groupValues[1]
                        title = title.replace(match.value, "").trim() 
                    }
                    animeList.add(ScrapedAnime(title = title, url = href, posterUrl = img, episodeNumber = epNum, sourceName = "Anime3rb"))
                }
            }
            return@withContext animeList.distinctBy { it.url }.take(20)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    override suspend fun getVideoUrl(episodeUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val proxiedUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$episodeUrl"
            val request = Request.Builder().url(proxiedUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            
            var playerUrl = doc.selectFirst("iframe[src*=player], iframe[src*=vid3rb]")?.attr("src")
            if (playerUrl == null) {
                playerUrl = Regex("""https?://[^"'\s]*(?:vid3rb|player)[^"'\s]*""").find(html)?.value
            }
            if (playerUrl != null) {
                val proxiedPlayerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$playerUrl"
                val playerReq = Request.Builder().url(proxiedPlayerUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val playerRes = client.newCall(playerReq).execute()
                val playerHtml = playerRes.body?.string() ?: ""
                val m3u8Regex = Regex("""https?://[^"'\s]+\.m3u8[^"'\s]*""")
                val mp4Regex = Regex("""https?://[^"'\s]+\.mp4[^"'\s]*""")
                return@withContext m3u8Regex.find(playerHtml)?.value ?: mp4Regex.find(playerHtml)?.value
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }
}

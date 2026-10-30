package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.ejemplo.hiyako.data.model.ScrapedAnime

class Anime4UpScraper @Inject constructor() : BaseScraper {
    override val name = "Anime4Up"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = "https://anime4up.com/"
            // Use the Cloudflare Worker to bypass block if needed
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$targetUrl"
            val request = Request.Builder().url(workerUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
                
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val animeList = mutableListOf<ScrapedAnime>()

            // Fallback Strategy for Anime4Up selectors
            val items = doc.select(".anime-card-container, .anime-card-poster, article")
            for (item in items) {
                val link = item.selectFirst("a")?.attr("href") ?: continue
                var img = item.selectFirst("img")?.attr("data-src") ?: item.selectFirst("img")?.attr("src") ?: ""
                var title = item.selectFirst(".anime-card-title")?.text() ?: item.selectFirst("h3")?.text() ?: ""
                
                if (title.isNotEmpty() && img.isNotEmpty() && link.isNotEmpty()) {
                    val fullImg = if (img.startsWith("//")) "https:$img" else img
                    animeList.add(ScrapedAnime(title = title, url = link, posterUrl = fullImg, sourceName = "Anime4Up"))
                }
            }
            return@withContext animeList.distinctBy { it.url }.take(20)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    override suspend fun getVideoUrl(episodeUrl: String): String? = withContext(Dispatchers.IO) {
        // We will implement video extraction for Anime4Up later.
        // For now, it returns null to avoid crashing.
        return@withContext null
    }
}

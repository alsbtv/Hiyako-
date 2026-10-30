package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.util.Log
import com.ejemplo.hiyako.data.model.ScrapedAnime

class WitAnimeScraper @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // دالة جلب قائمة الأنمي (من الصفحة الرئيسية)
    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        try {
            val workerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=https://anime3rb.com/"
            val request = Request.Builder().url(workerUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val animeList = mutableListOf<ScrapedAnime>()

            val items = doc.select("a[href*=/episode/]")
            for (link in items) {
                val href = link.attr("href")
                var img = link.selectFirst("img")?.attr("data-src") ?: link.selectFirst("img")?.attr("src") ?: ""
                if (img.startsWith("//")) img = "https:$img"
                var title = link.selectFirst("h3")?.text() ?: link.selectFirst("h4")?.text() ?: link.text().trim()
                
                if (title.isNotEmpty() && img.isNotEmpty() && href.contains("/episode/")) {
                    var epNum = ""
                    val match = Regex("""(?:الحلقة|Episode|Ep)\s*(\d+)""").find(title)
                    if (match != null) { epNum = match.groupValues[1]; title = title.replace(match.value, "").trim() }
                    animeList.add(ScrapedAnime(title = title, url = href, posterUrl = img, episodeNumber = epNum))
                }
            }
            return@withContext animeList.distinctBy { it.url }.take(30)
        } catch (e: Exception) { 
            Log.e("Scraper", "Error fetching list: ${e.message}")
            return@withContext emptyList() 
        }
    }

    // دالة استخراج رابط الفيديو الحقيقي من صفحة الحلقة
    suspend fun getVideoUrl(episodeUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. جلب صفحة الحلقة عبر Worker
            val proxiedUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$episodeUrl"
            val request = Request.Builder().url(proxiedUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""

            // 2. البحث عن رابط المشغل (Iframe أو رابط Vid3rb)
            var playerUrl: String? = null
            val doc = Jsoup.parse(html)
            val iframe = doc.selectFirst("iframe[src*=player], iframe[src*=vid3rb]")
            
            if (iframe != null) {
                playerUrl = iframe.attr("src")
            } else {
                // بحث احتياطي باستخدام Regex
                val match = Regex("""https?://[^"'\s]*(?:vid3rb|player)[^"'\s]*""").find(html)
                playerUrl = match?.value
            }

            // 3. إذا وجدنا رابط المشغل، ندخل إليه ونستخرج الـ m3u8
            if (playerUrl != null) {
                val proxiedPlayerUrl = "https://late-morning-434d.wesaamalsubhi.workers.dev/?url=$playerUrl"
                val playerReq = Request.Builder().url(proxiedPlayerUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val playerRes = client.newCall(playerReq).execute()
                val playerHtml = playerRes.body?.string() ?: ""

                // البحث عن رابط m3u8 أو mp4
                val m3u8Regex = Regex("""https?://[^"'\s]+\.m3u8[^"'\s]*""")
                val mp4Regex = Regex("""https?://[^"'\s]+\.mp4[^"'\s]*""")

                val directUrl = m3u8Regex.find(playerHtml)?.value ?: mp4Regex.find(playerHtml)?.value
                return@withContext directUrl
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e("Scraper", "Error getting video URL: ${e.message}")
            return@withContext null
        }
    }
}

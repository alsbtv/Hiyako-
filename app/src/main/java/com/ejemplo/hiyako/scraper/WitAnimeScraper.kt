package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class WitAnimeScraper {

    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<ScrapedAnime>()
        try {
            val doc = Jsoup.connect("https://witanime.cyou/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            // البحث عن بطاقات الأنمي في الصفحة الرئيسية (عادة ما تكون في عناصر مثل .anime-card أو .item أو ما شابه)
            val elements = doc.select("div.anime-card-container, div.item, div.col-anime, article")
            for (el in elements) {
                val titleEl = el.select("h3 a, h2 a, a.anime-title, .title a").first()
                val linkEl = titleEl ?: el.select("a").first()
                val imgEl = el.select("img").first()

                val title = titleEl?.text()?.trim() ?: linkEl?.attr("title")?.trim() ?: "Unknown Anime"
                val url = linkEl?.attr("abs:href") ?: linkEl?.attr("href") ?: ""
                
                // جلب رابط الصورة سواء من src أو data-src
                var posterUrl = imgEl?.attr("data-src") ?: imgEl?.attr("src") ?: ""
                if (posterUrl.startsWith("//")) {
                    posterUrl = "https:$posterUrl"
                }

                if (title.isNotEmpty() && url.isNotEmpty() && !animeList.any { it.url == url }) {
                    animeList.add(ScrapedAnime(title = title, url = url, posterUrl = posterUrl))
                }
            }

            // إذا لم يتم العثور على عناصر بالطريقة السابقة، نبحث عن أي صور داخل روابط تحتوي على تفاصيل
            if (animeList.isEmpty()) {
                val fallbackElements = doc.select("a[href*=\"anime\"]")
                for (el in fallbackElements) {
                    val img = el.select("img").first()
                    val title = el.attr("title").ifEmpty { img?.attr("alt") ?: "" }
                    val url = el.attr("abs:href")
                    var posterUrl = img?.attr("data-src") ?: img?.attr("src") ?: ""
                    if (posterUrl.startsWith("//")) posterUrl = "https:$posterUrl"

                    if (title.isNotEmpty() && url.isNotEmpty() && posterUrl.isNotEmpty() && !animeList.any { it.url == url }) {
                        animeList.add(ScrapedAnime(title = title, url = url, posterUrl = posterUrl))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        animeList
    }
}

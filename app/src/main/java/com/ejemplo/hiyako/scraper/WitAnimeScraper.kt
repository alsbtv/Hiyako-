package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject

class WitAnimeScraper @Inject constructor() {

    suspend fun getLatestAnime(): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<ScrapedAnime>()
        try {
            val doc = Jsoup.connect("https://witanime.cyou/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            val elements = doc.select("div.anime-card-container, div.item, div.col-anime, article")
            for (el in elements) {
                val titleEl = el.select("h3 a, h2 a, a.anime-title, .title a").first()
                val linkEl = titleEl ?: el.select("a").first()
                val imgEl = el.select("img").first()

                val title = titleEl?.text()?.trim() ?: linkEl?.attr("title")?.trim() ?: "Unknown Anime"
                val url = linkEl?.attr("abs:href") ?: linkEl?.attr("href") ?: ""
                
                var posterUrl = imgEl?.attr("data-src") ?: imgEl?.attr("src") ?: ""
                if (posterUrl.startsWith("//")) {
                    posterUrl = "https:$posterUrl"
                }

                if (title.isNotEmpty() && url.isNotEmpty() && !animeList.any { it.url == url }) {
                    animeList.add(ScrapedAnime(title = title, url = url, posterUrl = posterUrl))
                }
            }

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

    suspend fun searchAnime(query: String): List<ScrapedAnime> = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<ScrapedAnime>()
        try {
            val doc = Jsoup.connect("https://witanime.cyou/?s=$query")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            val elements = doc.select("div.anime-card-container, div.item, div.col-anime, article")
            for (el in elements) {
                val titleEl = el.select("h3 a, h2 a, a.anime-title, .title a").first()
                val linkEl = titleEl ?: el.select("a").first()
                val imgEl = el.select("img").first()

                val title = titleEl?.text()?.trim() ?: linkEl?.attr("title")?.trim() ?: "Anime"
                val url = linkEl?.attr("abs:href") ?: linkEl?.attr("href") ?: ""
                var posterUrl = imgEl?.attr("data-src") ?: imgEl?.attr("src") ?: ""
                if (posterUrl.startsWith("//")) posterUrl = "https:$posterUrl"

                if (title.isNotEmpty() && url.isNotEmpty() && !animeList.any { it.url == url }) {
                    animeList.add(ScrapedAnime(title = title, url = url, posterUrl = posterUrl))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        animeList
    }

    suspend fun getEpisodes(animeUrl: String): List<ScrapedEpisode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<ScrapedEpisode>()
        try {
            val doc = Jsoup.connect(animeUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            val epElements = doc.select("ul.episodes-list li a, div.episodes-list a, a.episode-item")
            for (el in epElements) {
                val url = el.attr("abs:href")
                val number = el.text().trim()
                if (url.isNotEmpty()) {
                    episodes.add(ScrapedEpisode(number = number, url = url))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        episodes
    }

    suspend fun getVideoLink(episodeUrl: String): List<ScrapedVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<ScrapedVideo>()
        try {
            val doc = Jsoup.connect(episodeUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()

            val servers = doc.select("ul.servers-list li, div.server-item")
            for (server in servers) {
                val name = server.text().trim()
                val dataUrl = server.attr("data-url")
                if (dataUrl.isNotEmpty()) {
                    videos.add(ScrapedVideo(serverName = name, videoUrl = dataUrl))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        videos
    }
}

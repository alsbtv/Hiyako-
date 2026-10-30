package com.ejemplo.hiyako.scraper

import com.ejemplo.hiyako.data.model.ScrapedAnime

interface BaseScraper {
    val name: String
    suspend fun getLatestAnime(): List<ScrapedAnime>
    suspend fun getVideoUrl(episodeUrl: String): String?
}

package com.ejemplo.hiyako.data.model

data class ScrapedAnime(
    val title: String,
    val url: String,
    val posterUrl: String,
    val episodeNumber: String = "",
    val bannerUrl: String? = null,
    val synopsis: String? = null,
    val score: Double? = null,
    val status: String? = null,
    val sourceName: String = "Unknown"
)

data class NewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val source: String,
    val publishedAt: String,
    val translatedTitle: String = "",
    val translatedDescription: String = "",
    val relatedAnimeTitle: String = ""
)

package com.ejemplo.hiyako.data.model

data class Episode(
    val number: String,
    val title: String,
    val thumbnailUrl: String,
    val url: String
)

data class Character(
    val name: String,
    val imageUrl: String,
    val role: String
)

data class ScrapedAnime(
    val title: String, 
    val url: String, 
    val posterUrl: String, 
    val episodeNumber: String = "",
    val bannerUrl: String? = null,
    val synopsis: String? = null,
    val score: Double? = null,
    val studios: List<String>? = null,
    val genres: List<String>? = null,
    val episodesList: List<Episode> = emptyList(),
    val characters: List<Character> = emptyList()
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

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
    val sourceName: String = "Unknown" // New Field
)

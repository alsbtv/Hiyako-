package com.ejemplo.hiyako.scraper

data class ScrapedAnime(
    val title: String,
    val episodeNumber: String,
    val url: String,
    val posterUrl: String
)

data class ScrapedEpisode(
    val number: String,
    val url: String
)

data class ScrapedVideo(
    val serverName: String,
    val videoUrl: String,
    val quality: String = "HD"
)

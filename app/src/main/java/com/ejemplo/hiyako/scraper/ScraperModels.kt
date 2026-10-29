package com.ejemplo.hiyako.scraper

data class ScrapedAnime(
    val title: String,
    val url: String,
    val posterUrl: String,
    val episodeNumber: String = ""
)

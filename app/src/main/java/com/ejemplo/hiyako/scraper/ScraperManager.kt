package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import com.ejemplo.hiyako.data.model.ScrapedAnime

@Singleton
class ScraperManager @Inject constructor(
    private val anime3rb: Anime3rbScraper,
    private val anime4up: Anime4UpScraper
) {
    private val allScrapers: List<BaseScraper> = listOf(anime3rb, anime4up)

    fun getAnimeFromSources(): Flow<List<ScrapedAnime>> = flow {
        val chunks = allScrapers.chunked(5)
        var hasData = false

        for ((index, chunk) in chunks.withIndex()) {
            val results = mutableListOf<ScrapedAnime>()
            coroutineScope {
                val deferreds = chunk.map { scraper ->
                    async(Dispatchers.IO) {
                        try { scraper.getLatestAnime() } catch (e: Exception) { emptyList() }
                    }
                }
                deferreds.forEach { deferred -> results.addAll(deferred.await()) }
            }
            
            if (results.isNotEmpty()) {
                emit(results)
                hasData = true
            }
            if (index < chunks.size - 1) delay(10000) // Reduced from 30s to 10s
        }

        // Fallback if all scrapers failed
        if (!hasData) {
            emit(listOf(
                ScrapedAnime("One Piece", "dummy1", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg", "1", sourceName = "Dummy"),
                ScrapedAnime("Bleach", "dummy2", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg", "2", sourceName = "Dummy"),
                ScrapedAnime("Naruto", "dummy3", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx116674-2P4wLh56yH2W.jpg", "3", sourceName = "Dummy")
            ))
        }
    }.flowOn(Dispatchers.IO)
}

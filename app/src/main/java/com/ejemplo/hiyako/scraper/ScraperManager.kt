package com.ejemplo.hiyako.scraper

import kotlinx.coroutines.Dispatchers
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
    // Here is where we add the websites we support
    private val allScrapers: List<BaseScraper> = listOf(anime3rb, anime4up)

    fun getAnimeFromSources(): Flow<List<ScrapedAnime>> = flow {
        // Batch 1: Process scrapers in chunks of 5
        val chunks = allScrapers.chunked(5)
        
        for ((index, chunk) in chunks.withIndex()) {
            val results = mutableListOf<ScrapedAnime>()
            
            // Fetch from all scrapers in this chunk simultaneously (Parallel)
            val deferreds = chunk.map { scraper ->
                kotlinx.coroutines.async(Dispatchers.IO) {
                    try { scraper.getLatestAnime() } catch (e: Exception) { emptyList() }
                }
            }
            
            // Combine results
            deferreds.forEach { deferred ->
                results.addAll(deferred.await())
            }
            
            if (results.isNotEmpty()) {
                emit(results)
            }
            
            if (index < chunks.size - 1) {
                delay(30000) // Wait 30 seconds before next batch
            }
        }
    }.flowOn(Dispatchers.IO)
}

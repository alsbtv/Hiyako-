package com.ejemplo.hiyako.data.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.ejemplo.hiyako.data.model.ScrapedAnime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataAggregator @Inject constructor() {
    
    // In the future, we can add Shikimori, TMDb, etc., here to reach 50 sources.
    suspend fun getEnrichedAnime(title: String): ScrapedAnime? = coroutineScope {
        val aniListDeferred = async { AniListApi.getAnimeDetails(title) }
        val jikanDeferred = async { JikanApi.getAnimeDetails(title) }
        val kitsuDeferred = async { KitsuApi.getAnimeDetails(title) }

        val aniList = aniListDeferred.await()
        val jikan = jikanDeferred.await()
        val kitsu = kitsuDeferred.await()

        // Smart Merge: Prioritize AniList for Banner, Jikan for Synopsis, Kitsu for Cover if higher res
        val base = aniList ?: jikan ?: kitsu ?: return@coroutineScope null

        return@coroutineScope base.copy(
            title = base.title,
            posterUrl = aniList?.posterUrl?.takeIf { it.isNotEmpty() } ?: jikan?.posterUrl ?: kitsu?.posterUrl ?: "",
            bannerUrl = aniList?.bannerUrl?.takeIf { it.isNotEmpty() } ?: kitsu?.bannerUrl ?: base.posterUrl,
            synopsis = jikan?.synopsis?.takeIf { it.isNotEmpty() } ?: aniList?.synopsis ?: kitsu?.synopsis ?: "",
            score = jikan?.score?.takeIf { it > 0 } ?: aniList?.score ?: kitsu?.score ?: 0.0,
            status = aniList?.status ?: jikan?.status ?: kitsu?.status ?: "Unknown"
        )
    }
}

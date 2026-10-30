package com.ejemplo.hiyako.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.data.api.AniListApi
import com.ejemplo.hiyako.data.model.ScrapedAnime
import com.ejemplo.hiyako.scraper.WitAnimeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val animeList: List<ScrapedAnime>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scraper: WitAnimeScraper,
    private val anilistApi: AniListApi
) : ViewModel() {

    private var isDataLoaded = false

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchAnime()
    }

    private fun fetchAnime() {
        if (isDataLoaded) return
        viewModelScope.launch {
            try {
                val scrapedList = scraper.getLatestAnime()
                val enrichedList = scrapedList.toMutableList()

                for (i in enrichedList.indices) {
                    if (i < 10) {
                        val anime = enrichedList[i]
                        val richData = anilistApi.getAnimeDetails(anime.title)
                        if (richData != null) {
                            enrichedList[i] = anime.copy(
                                bannerUrl = richData.bannerUrl ?: richData.posterUrl,
                                synopsis = richData.synopsis,
                                score = richData.score,
                                studios = richData.studios,
                                genres = richData.genres,
                                episodesList = richData.episodesList,
                                characters = richData.characters
                            )
                        }
                    }
                }

                _uiState.value = HomeUiState.Success(enrichedList)
                isDataLoaded = true
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

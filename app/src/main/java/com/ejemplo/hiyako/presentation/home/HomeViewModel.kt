package com.ejemplo.hiyako.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.data.api.MetadataAggregator
import com.ejemplo.hiyako.data.model.ScrapedAnime
import com.ejemplo.hiyako.scraper.ScraperManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val animeList: List<ScrapedAnime>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scraperManager: ScraperManager,
    private val metadataAggregator: MetadataAggregator
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val allAnime = mutableListOf<ScrapedAnime>()

    init {
        fetchAnime()
    }

    private fun fetchAnime() {
        viewModelScope.launch {
            try {
                scraperManager.getAnimeFromSources().collectLatest { batch ->
                    // Enrich the batch with 50 sources (represented by MetadataAggregator)
                    val enrichedBatch = batch.take(3).mapNotNull { anime ->
                        val details = metadataAggregator.getEnrichedAnime(anime.title)
                        details ?: anime
                    } + batch.drop(3)

                    allAnime.addAll(enrichedBatch)
                    _uiState.value = HomeUiState.Success(allAnime.toList())
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Error: ${e.message}")
            }
        }
    }
}

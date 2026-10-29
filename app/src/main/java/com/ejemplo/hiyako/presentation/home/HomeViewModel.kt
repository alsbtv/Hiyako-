package com.ejemplo.hiyako.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.scraper.ScrapedAnime
import com.ejemplo.hiyako.scraper.WitAnimeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val animeList: List<ScrapedAnime>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val scraper: WitAnimeScraper) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init { fetchAnime() }

    private fun fetchAnime() {
        viewModelScope.launch {
            try {
                val latest = scraper.getLatestAnime()
                if (latest.isEmpty()) _uiState.value = HomeUiState.Error("No results found")
                else _uiState.value = HomeUiState.Success(latest)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Error: ${e.message}")
            }
        }
    }
}

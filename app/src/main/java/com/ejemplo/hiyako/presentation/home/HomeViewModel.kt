package com.ejemplo.hiyako.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.scraper.ScrapedAnime
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
    private val scraper: WitAnimeScraper
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow(
        listOf("Action", "Fantasy", "Sci-Fi", "Romance")
    )
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    init {
        loadLatestAnime()
    }

    fun loadLatestAnime() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val list = scraper.getLatestAnime()
                if (list.isNotEmpty()) {
                    _uiState.value = HomeUiState.Success(list)
                } else {
                    // Fallback mock or empty message if scraper returns nothing due to network/cloudlflare
                    _uiState.value = HomeUiState.Success(
                        listOf(
                            ScrapedAnime("Jujutsu Kaisen", "#", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80"),
                            ScrapedAnime("Demon Slayer", "#", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80"),
                            ScrapedAnime("Attack on Titan", "#", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80")
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}

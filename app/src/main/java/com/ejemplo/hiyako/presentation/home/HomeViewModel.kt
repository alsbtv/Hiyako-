package com.ejemplo.hiyako.presentation.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class Anime(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val status: String,
    val rating: Double,
    val progress: Float? = null // For watch history (0.0 to 1.0)
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _animeList = MutableStateFlow<List<Anime>>(
        listOf(
            Anime(1, "Jujutsu Kaisen", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80", "On Going", 4.9, 0.75f),
            Anime(2, "Demon Slayer", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80", "Completed", 4.8, 0.40f),
            Anime(3, "Attack on Titan", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80", "Completed", 5.0, 0.90f),
            Anime(4, "One Piece", "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?w=600&auto=format&fit=crop&q=80", "On Going", 4.9),
            Anime(5, "Chainsaw Man", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80", "On Going", 4.7),
            Anime(6, "Solo Leveling", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80", "On Going", 4.9),
            Anime(7, "Vinland Saga", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80", "Completed", 4.8),
            Anime(8, "Spy x Family", "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?w=600&auto=format&fit=crop&q=80", "On Going", 4.6),
            Anime(9, "Bleach: TYBW", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80", "On Going", 4.9),
            Anime(10, "My Hero Academia", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80", "Completed", 4.5)
        )
    )
    val animeList: StateFlow<List<Anime>> = _animeList.asStateFlow()

    private val _categories = MutableStateFlow(
        listOf("Action", "Fantasy", "Sci-Fi", "Romance")
    )
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
}

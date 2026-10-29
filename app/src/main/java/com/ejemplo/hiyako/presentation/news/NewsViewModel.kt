package com.ejemplo.hiyako.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.data.model.NewsArticle
import com.ejemplo.hiyako.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NewsUiState {
    object Loading : NewsUiState
    data class Success(val articles: List<NewsArticle>) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allArticles = listOf<NewsArticle>()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            try {
                allArticles = newsRepository.fetchNews()
                _uiState.value = NewsUiState.Success(allArticles)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Failed to load news")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val filtered = if (query.isBlank()) {
            allArticles
        } else {
            allArticles.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.translatedTitle.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = NewsUiState.Success(filtered)
    }
}

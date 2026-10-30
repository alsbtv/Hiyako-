package com.ejemplo.hiyako.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.data.model.NewsArticle
import com.ejemplo.hiyako.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(private val repository: NewsRepository) : ViewModel() {
    private val _news = MutableStateFlow<List<NewsArticle>>(emptyList())
    val news: StateFlow<List<NewsArticle>> = _news

    init { loadNews() }

    private fun loadNews() {
        viewModelScope.launch {
            _news.value = repository.fetchNews()
        }
    }
}

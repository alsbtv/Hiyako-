package com.ejemplo.hiyako.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.hiyako.data.preferences.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SettingsPreferences
) : ViewModel() {
    val themeColor = prefs.themeColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Red")
    val titleLanguage = prefs.titleLanguage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Japanese")
    val seekDuration = prefs.seekDuration.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10")

    fun setTheme(color: String) = viewModelScope.launch { prefs.setThemeColor(color) }
    fun setLanguage(lang: String) = viewModelScope.launch { prefs.setTitleLanguage(lang) }
    fun setSeek(duration: String) = viewModelScope.launch { prefs.setSeekDuration(duration) }
}

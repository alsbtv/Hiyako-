package com.ejemplo.hiyako.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "hiyako_settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val TITLE_LANGUAGE = stringPreferencesKey("title_language")
        val SEEK_DURATION = stringPreferencesKey("seek_duration")
    }

    val themeColor: Flow<String> = context.dataStore.data.map { it[THEME_COLOR] ?: "Red" }
    val titleLanguage: Flow<String> = context.dataStore.data.map { it[TITLE_LANGUAGE] ?: "Japanese" }
    val seekDuration: Flow<String> = context.dataStore.data.map { it[SEEK_DURATION] ?: "10" }

    suspend fun setThemeColor(color: String) { context.dataStore.edit { it[THEME_COLOR] = color } }
    suspend fun setTitleLanguage(lang: String) { context.dataStore.edit { it[TITLE_LANGUAGE] = lang } }
    suspend fun setSeekDuration(duration: String) { context.dataStore.edit { it[SEEK_DURATION] = duration } }
}

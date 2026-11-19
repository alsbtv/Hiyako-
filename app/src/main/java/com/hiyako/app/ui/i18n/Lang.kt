package com.hiyako.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The app language. Arabic is the default; English is available in Settings. */
object Lang {
    private const val PREFS = "nexus_settings"
    private const val KEY = "app_lang"

    var arabic: Boolean by mutableStateOf(true)

    fun load(context: Context) {
        arabic = try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "ar") != "en"
        } catch (e: Exception) {
            true
        }
    }

    fun set(context: Context, useArabic: Boolean) {
        arabic = useArabic
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, if (useArabic) "ar" else "en")
                .apply()
        } catch (e: Exception) {
            // Preferences unavailable: the language still changes for this session.
        }
    }
}

/** Returns the English or Arabic text for the current language. Reading it inside composables keeps them in sync. */
fun tr(en: String, ar: String): String = if (Lang.arabic) ar else en

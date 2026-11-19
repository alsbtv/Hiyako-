package com.hiyako.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.hiyako.app.ui.i18n.Lang

data class AppPalette(
    val id: String,
    val name: String,
    val dark: Boolean,
    val surface: Color,
    val secondary: Color,
    val elevated: Color,
    val userBubble: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color
)

object AppPalettes {
    val obsidian = AppPalette(
        id = "obsidian", name = "Obsidian", dark = true,
        surface = Color(0xFF0A0A0A), secondary = Color(0xFF141414),
        elevated = Color(0xFF1B1B1B), userBubble = Color(0xFF2A2A2A), border = Color(0xFF27272A),
        textPrimary = Color(0xFFFFFFFF), textSecondary = Color(0xFFA1A1AA), textMuted = Color(0xFF71717A),
        accent = Color(0xFF8B5CF6), onAccent = Color(0xFFFFFFFF)
    )
    val midnight = AppPalette(
        id = "midnight", name = "Midnight", dark = true,
        surface = Color(0xFF0F172A), secondary = Color(0xFF131C33),
        elevated = Color(0xFF1A2540), userBubble = Color(0xFF1E2A48), border = Color(0xFF1F2A44),
        textPrimary = Color(0xFFF1F5F9), textSecondary = Color(0xFF94A3B8), textMuted = Color(0xFF64748B),
        accent = Color(0xFF60A5FA), onAccent = Color(0xFF0B1020)
    )
    val ember = AppPalette(
        id = "ember", name = "Ember", dark = true,
        surface = Color(0xFF1A110E), secondary = Color(0xFF221713),
        elevated = Color(0xFF2A1D18), userBubble = Color(0xFF3A2620), border = Color(0xFF3A2A24),
        textPrimary = Color(0xFFFFF7ED), textSecondary = Color(0xFFD6BFB0), textMuted = Color(0xFF9A8478),
        accent = Color(0xFFF97316), onAccent = Color(0xFF1A0A00)
    )
    val forest = AppPalette(
        id = "forest", name = "Forest", dark = true,
        surface = Color(0xFF0C1A13), secondary = Color(0xFF112219),
        elevated = Color(0xFF16291F), userBubble = Color(0xFF1D3A2A), border = Color(0xFF1F3A2C),
        textPrimary = Color(0xFFECFDF5), textSecondary = Color(0xFFA7D7BF), textMuted = Color(0xFF6B9A82),
        accent = Color(0xFF34D399), onAccent = Color(0xFF04130C)
    )
    val rose = AppPalette(
        id = "rose", name = "Rose", dark = true,
        surface = Color(0xFF1C0F17), secondary = Color(0xFF25141F),
        elevated = Color(0xFF2E1A27), userBubble = Color(0xFF3B2132), border = Color(0xFF3A2233),
        textPrimary = Color(0xFFFFF1F7), textSecondary = Color(0xFFE0B6CB), textMuted = Color(0xFF9C7088),
        accent = Color(0xFFF472B6), onAccent = Color(0xFF2A0716)
    )
    val paper = AppPalette(
        id = "paper", name = "Paper", dark = false,
        surface = Color(0xFFF8FAFC), secondary = Color(0xFFF1F5F9),
        elevated = Color(0xFFE2E8F0), userBubble = Color(0xFFE2E8F0), border = Color(0xFFCBD5E1),
        textPrimary = Color(0xFF0F172A), textSecondary = Color(0xFF475569), textMuted = Color(0xFF64748B),
        accent = Color(0xFF4F46E5), onAccent = Color(0xFFFFFFFF)
    )

    val all: List<AppPalette> = listOf(obsidian, midnight, ember, forest, rose, paper)
    fun byId(id: String?): AppPalette? = all.firstOrNull { it.id == id }
}

object ThemeState {
    private const val PREFS = "nexus_settings"
    private const val KEY = "app_palette"

    var palette: AppPalette by mutableStateOf(AppPalettes.obsidian)

    fun select(context: Context, id: String) {
        AppPalettes.byId(id)?.let { palette = it }
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
        } catch (e: Exception) {}
    }

    fun loadSaved(context: Context, darkDefault: Boolean) {
        val saved = try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        } catch (e: Exception) { null }
        palette = AppPalettes.byId(saved) ?: if (darkDefault) AppPalettes.obsidian else AppPalettes.paper
    }
}

val OledBlack: Color get() = if (ThemeState.palette.id == "paper") Color(0xFFFFFFFF) else Color(0xFF000000)
val DeepSurface: Color get() = ThemeState.palette.surface
val SecondarySurface: Color get() = ThemeState.palette.secondary
val ElevatedSurface: Color get() = ThemeState.palette.elevated
val UserBubble: Color get() = ThemeState.palette.userBubble
val BorderColor: Color get() = ThemeState.palette.border
val TextPrimary: Color get() = ThemeState.palette.textPrimary
val TextSecondary: Color get() = ThemeState.palette.textSecondary
val TextMuted: Color get() = ThemeState.palette.textMuted
val CardDark: Color get() = ThemeState.palette.secondary
val AccentBlue: Color get() = ThemeState.palette.accent

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember {
        ThemeState.loadSaved(context, darkTheme)
        Lang.load(context)
    }
    val p = ThemeState.palette
    val scheme = if (p.dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, background = OledBlack,
            onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.secondary, onSurfaceVariant = p.textSecondary, outline = p.border
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, background = OledBlack,
            onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.secondary, onSurfaceVariant = p.textSecondary, outline = p.border
        )
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (Lang.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

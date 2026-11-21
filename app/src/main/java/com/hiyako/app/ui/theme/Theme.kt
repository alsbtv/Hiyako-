package com.hiyako.app.ui.theme

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hiyako.app.ui.i18n.Lang

/** The app name shown on the splash screen, in the drawer and in Settings. Change it here only. */
object Brand {
    const val EN = "Hiyako"
    const val AR = "هياكو"
}

data class AppPalette(
    val id: String,
    val name: String,
    val dark: Boolean,
    val base: Color,
    val surface: Color,
    val card: Color,
    val secondary: Color,
    val elevated: Color,
    val userBubble: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val onAccent: Color,
    val danger: Color = Color(0xFFF87171),
    val success: Color = Color(0xFF34D399),
    val warning: Color = Color(0xFFFBBF24),
    val codeBackground: Color = Color(0xFF0B0A14),
    val codeHeader: Color = Color(0xFF181628)
)

object AppPalettes {
    val obsidian = AppPalette(
        id = "obsidian", name = "Aurora", dark = true,
        base = Color(0xFF07060F), surface = Color(0xFF0E0C1C), card = Color(0xFF16132B),
        secondary = Color(0x14FFFFFF), elevated = Color(0x22FFFFFF), userBubble = Color(0xFF6D4AFF), border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFF8F7FF), textSecondary = Color(0xFFB9B5D3), textMuted = Color(0xFF9B97B8),
        accent = Color(0xFF7C5CFF), accent2 = Color(0xFF00B8D9), accent3 = Color(0xFFF472B6), onAccent = Color(0xFFFFFFFF)
    )
    val midnight = AppPalette(
        id = "midnight", name = "Ocean", dark = true,
        base = Color(0xFF040B1A), surface = Color(0xFF0A1428), card = Color(0xFF111E3A),
        secondary = Color(0x14FFFFFF), elevated = Color(0x22FFFFFF), userBubble = Color(0xFF2563EB), border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFF1F5F9), textSecondary = Color(0xFFA9B8CF), textMuted = Color(0xFF8CA0BC),
        accent = Color(0xFF3B82F6), accent2 = Color(0xFF06B6D4), accent3 = Color(0xFF8B5CF6), onAccent = Color(0xFF04101F)
    )
    val ember = AppPalette(
        id = "ember", name = "Ember", dark = true,
        base = Color(0xFF120806), surface = Color(0xFF1D100C), card = Color(0xFF2A1812),
        secondary = Color(0x14FFFFFF), elevated = Color(0x22FFFFFF), userBubble = Color(0xFFEA580C), border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFFFF7ED), textSecondary = Color(0xFFD9C2B4), textMuted = Color(0xFFA68C7E),
        accent = Color(0xFFF97316), accent2 = Color(0xFFFBBF24), accent3 = Color(0xFFEF4444), onAccent = Color(0xFF1A0A00)
    )
    val forest = AppPalette(
        id = "forest", name = "Forest", dark = true,
        base = Color(0xFF04110B), surface = Color(0xFF0A1B12), card = Color(0xFF11291D),
        secondary = Color(0x14FFFFFF), elevated = Color(0x22FFFFFF), userBubble = Color(0xFF059669), border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFECFDF5), textSecondary = Color(0xFFAEDBC4), textMuted = Color(0xFF7FAE97),
        accent = Color(0xFF10B981), accent2 = Color(0xFF84CC16), accent3 = Color(0xFF06B6D4), onAccent = Color(0xFF03130B)
    )
    val rose = AppPalette(
        id = "rose", name = "Rose", dark = true,
        base = Color(0xFF140812), surface = Color(0xFF1F0F1B), card = Color(0xFF2C1727),
        secondary = Color(0x14FFFFFF), elevated = Color(0x22FFFFFF), userBubble = Color(0xFFDB2777), border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFFFF1F7), textSecondary = Color(0xFFE3BCD0), textMuted = Color(0xFFB08AA0),
        accent = Color(0xFFEC4899), accent2 = Color(0xFFA855F7), accent3 = Color(0xFFFB7185), onAccent = Color(0xFFFFFFFF)
    )
    val paper = AppPalette(
        id = "paper", name = "Paper", dark = false,
        base = Color(0xFFF6F5FF), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xB3FFFFFF), elevated = Color(0xE6FFFFFF), userBubble = Color(0xFF4F46E5), border = Color(0x331E1B4B),
        textPrimary = Color(0xFF14122B), textSecondary = Color(0xFF4B4870), textMuted = Color(0xFF6B6890),
        accent = Color(0xFF4F46E5), accent2 = Color(0xFF0EA5E9), accent3 = Color(0xFFEC4899), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEFEDFB), codeHeader = Color(0xFFE2DFF5),
        danger = Color(0xFFDC2626)
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

val OledBlack: Color get() = ThemeState.palette.base
val DeepSurface: Color get() = ThemeState.palette.surface
val SecondarySurface: Color get() = ThemeState.palette.secondary
val ElevatedSurface: Color get() = ThemeState.palette.elevated
val UserBubble: Color get() = ThemeState.palette.userBubble
val BorderColor: Color get() = ThemeState.palette.border
val TextPrimary: Color get() = ThemeState.palette.textPrimary
val TextSecondary: Color get() = ThemeState.palette.textSecondary
val TextMuted: Color get() = ThemeState.palette.textMuted
val CardDark: Color get() = ThemeState.palette.card
val AccentBlue: Color get() = ThemeState.palette.accent
val AccentTwo: Color get() = ThemeState.palette.accent2
val AccentThree: Color get() = ThemeState.palette.accent3
val OnAccent: Color get() = ThemeState.palette.onAccent
val DangerColor: Color get() = ThemeState.palette.danger
val SuccessColor: Color get() = ThemeState.palette.success
val CodeBg: Color get() = ThemeState.palette.codeBackground
val CodeHeader: Color get() = ThemeState.palette.codeHeader

/** Main accent gradient used by buttons, the user bubble, the send button and avatars. */
val AccentBrush: Brush get() = Brush.linearGradient(listOf(ThemeState.palette.accent, ThemeState.palette.accent2))

/** Thin glowing outline used on glass cards. */
val GlassBorderBrush: Brush
    get() = Brush.linearGradient(
        listOf(
            ThemeState.palette.accent.copy(alpha = 0.55f),
            ThemeState.palette.border,
            ThemeState.palette.accent2.copy(alpha = 0.40f)
        )
    )

/** Animated aurora: three slowly drifting colour clouds behind the whole app. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = ThemeState.palette
    val transition = rememberInfiniteTransition(label = "aurora")
    val a by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by transition.animateFloat(1f, 0f, infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(29000, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    val strength = if (p.dark) 0.36f else 0.24f
    Box(
        modifier
            .fillMaxSize()
            .background(p.base)
            .drawBehind {
                val w = size.width
                val h = size.height
                val radius = maxOf(w, h) * 0.78f
                val c1 = Offset(w * (0.05f + 0.60f * a), h * (0.02f + 0.30f * b))
                val c2 = Offset(w * (0.95f - 0.55f * b), h * (0.55f + 0.35f * c))
                val c3 = Offset(w * (0.10f + 0.50f * c), h * (0.95f - 0.25f * a))
                drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = strength), Color.Transparent), center = c1, radius = radius), radius, c1)
                drawCircle(Brush.radialGradient(listOf(p.accent2.copy(alpha = strength * 0.85f), Color.Transparent), center = c2, radius = radius), radius, c2)
                drawCircle(Brush.radialGradient(listOf(p.accent3.copy(alpha = strength * 0.7f), Color.Transparent), center = c3, radius = radius), radius, c3)
            },
        content = content
    )
}

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
            primary = p.accent, onPrimary = p.onAccent, background = p.base,
            onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.card, onSurfaceVariant = p.textSecondary, outline = p.border
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, background = p.base,
            onBackground = p.textPrimary, surface = p.surface, onSurface = p.textPrimary,
            surfaceVariant = p.card, onSurfaceVariant = p.textSecondary, outline = p.border
        )
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (Lang.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

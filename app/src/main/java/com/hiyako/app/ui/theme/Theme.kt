package com.hiyako.app.ui.theme

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
        id = "obsidian", name = "Calm", dark = true,
        base = Color(0xFF111214), surface = Color(0xFF17181B), card = Color(0xFF1D1E22),
        secondary = Color(0xFF1A1B1E), elevated = Color(0xFF232428), userBubble = Color(0xFF2A2C31), border = Color(0xFF2C2E33),
        textPrimary = Color(0xFFE8E6E1), textSecondary = Color(0xFFA8A6A0), textMuted = Color(0xFF8A8883),
        accent = Color(0xFF9DB8A5), accent2 = Color(0xFF9DB8A5), accent3 = Color(0xFF9DB8A5), onAccent = Color(0xFF111214)
    )
    val midnight = AppPalette(
        id = "midnight", name = "Slate", dark = true,
        base = Color(0xFF0F1216), surface = Color(0xFF151A20), card = Color(0xFF1B2128),
        secondary = Color(0xFF181D23), elevated = Color(0xFF212830), userBubble = Color(0xFF273039), border = Color(0xFF2A323B),
        textPrimary = Color(0xFFE6E9ED), textSecondary = Color(0xFFA5ACB5), textMuted = Color(0xFF879099),
        accent = Color(0xFF8FA9C4), accent2 = Color(0xFF8FA9C4), accent3 = Color(0xFF8FA9C4), onAccent = Color(0xFF0F1216)
    )
    val ember = AppPalette(
        id = "ember", name = "Sand", dark = true,
        base = Color(0xFF15120F), surface = Color(0xFF1B1713), card = Color(0xFF221D18),
        secondary = Color(0xFF1E1A16), elevated = Color(0xFF29231D), userBubble = Color(0xFF302922), border = Color(0xFF332C25),
        textPrimary = Color(0xFFECE6DC), textSecondary = Color(0xFFB0A89B), textMuted = Color(0xFF918979),
        accent = Color(0xFFC9A57A), accent2 = Color(0xFFC9A57A), accent3 = Color(0xFFC9A57A), onAccent = Color(0xFF15120F)
    )
    val forest = AppPalette(
        id = "forest", name = "Moss", dark = true,
        base = Color(0xFF0F1411), surface = Color(0xFF151B17), card = Color(0xFF1B231E),
        secondary = Color(0xFF181F1A), elevated = Color(0xFF212B24), userBubble = Color(0xFF28342C), border = Color(0xFF2A352D),
        textPrimary = Color(0xFFE5EBE6), textSecondary = Color(0xFFA6B2A9), textMuted = Color(0xFF86948B),
        accent = Color(0xFF8FB39A), accent2 = Color(0xFF8FB39A), accent3 = Color(0xFF8FB39A), onAccent = Color(0xFF0F1411)
    )
    val rose = AppPalette(
        id = "rose", name = "Dusk", dark = true,
        base = Color(0xFF14111A), surface = Color(0xFF1A161F), card = Color(0xFF211C28),
        secondary = Color(0xFF1D1923), elevated = Color(0xFF282230), userBubble = Color(0xFF2F2838), border = Color(0xFF322B3B),
        textPrimary = Color(0xFFEAE6EE), textSecondary = Color(0xFFABA3B4), textMuted = Color(0xFF8D8498),
        accent = Color(0xFFB59AC0), accent2 = Color(0xFFB59AC0), accent3 = Color(0xFFB59AC0), onAccent = Color(0xFF14111A)
    )
    val paper = AppPalette(
        id = "paper", name = "Paper", dark = false,
        base = Color(0xFFF5F3EE), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEFEDE7), userBubble = Color(0xFFE9E6DF), border = Color(0xFFDDD9D0),
        textPrimary = Color(0xFF22201C), textSecondary = Color(0xFF5C5953), textMuted = Color(0xFF7A766F),
        accent = Color(0xFF4F6B58), accent2 = Color(0xFF4F6B58), accent3 = Color(0xFF4F6B58), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEFEDE7), codeHeader = Color(0xFFE5E2DA),
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
val AccentBrush: Brush get() = SolidColor(ThemeState.palette.accent)

/** Thin glowing outline used on glass cards. */
val GlassBorderBrush: Brush get() = SolidColor(ThemeState.palette.border)

/** Calm background: a flat base colour with a barely visible soft light at the top. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = ThemeState.palette
    Box(
        modifier
            .fillMaxSize()
            .background(p.base)
            .drawBehind {
                val radius = size.width * 1.1f
                val centre = Offset(size.width * 0.5f, -size.height * 0.05f)
                drawCircle(
                    Brush.radialGradient(listOf(p.accent.copy(alpha = if (p.dark) 0.07f else 0.10f), Color.Transparent), center = centre, radius = radius),
                    radius,
                    centre
                )
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

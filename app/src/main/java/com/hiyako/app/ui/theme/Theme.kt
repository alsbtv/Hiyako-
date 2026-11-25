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
    const val EN = "Nexus"
    const val AR = "نيكسوس"
}

data class AppPalette(
    val id: String,
    val name: String,
    val nameAr: String = "",
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

val AppPalette.displayName: String get() = if (Lang.arabic && nameAr.isNotBlank()) nameAr else name

object AppPalettes {
    val obsidian = AppPalette(
        id = "obsidian", name = "Calm", nameAr = "هادئ", dark = true,
        base = Color(0xFF111214), surface = Color(0xFF17181B), card = Color(0xFF1D1E22),
        secondary = Color(0xFF1A1B1E), elevated = Color(0xFF232428), userBubble = Color(0xFF2A2C31), border = Color(0xFF2C2E33),
        textPrimary = Color(0xFFE8E6E1), textSecondary = Color(0xFFA8A6A0), textMuted = Color(0xFF8A8883),
        accent = Color(0xFF9DB8A5), accent2 = Color(0xFF9DB8A5), accent3 = Color(0xFF9DB8A5), onAccent = Color(0xFF111214)
    )
    val midnight = AppPalette(
        id = "midnight", name = "Slate", nameAr = "أردوازي", dark = true,
        base = Color(0xFF0F1216), surface = Color(0xFF151A20), card = Color(0xFF1B2128),
        secondary = Color(0xFF181D23), elevated = Color(0xFF212830), userBubble = Color(0xFF273039), border = Color(0xFF2A323B),
        textPrimary = Color(0xFFE6E9ED), textSecondary = Color(0xFFA5ACB5), textMuted = Color(0xFF879099),
        accent = Color(0xFF8FA9C4), accent2 = Color(0xFF8FA9C4), accent3 = Color(0xFF8FA9C4), onAccent = Color(0xFF0F1216)
    )
    val ember = AppPalette(
        id = "ember", name = "Sand", nameAr = "رمل", dark = true,
        base = Color(0xFF15120F), surface = Color(0xFF1B1713), card = Color(0xFF221D18),
        secondary = Color(0xFF1E1A16), elevated = Color(0xFF29231D), userBubble = Color(0xFF302922), border = Color(0xFF332C25),
        textPrimary = Color(0xFFECE6DC), textSecondary = Color(0xFFB0A89B), textMuted = Color(0xFF918979),
        accent = Color(0xFFC9A57A), accent2 = Color(0xFFC9A57A), accent3 = Color(0xFFC9A57A), onAccent = Color(0xFF15120F)
    )
    val forest = AppPalette(
        id = "forest", name = "Moss", nameAr = "طحلب", dark = true,
        base = Color(0xFF0F1411), surface = Color(0xFF151B17), card = Color(0xFF1B231E),
        secondary = Color(0xFF181F1A), elevated = Color(0xFF212B24), userBubble = Color(0xFF28342C), border = Color(0xFF2A352D),
        textPrimary = Color(0xFFE5EBE6), textSecondary = Color(0xFFA6B2A9), textMuted = Color(0xFF86948B),
        accent = Color(0xFF8FB39A), accent2 = Color(0xFF8FB39A), accent3 = Color(0xFF8FB39A), onAccent = Color(0xFF0F1411)
    )
    val rose = AppPalette(
        id = "rose", name = "Dusk", nameAr = "غسق", dark = true,
        base = Color(0xFF14111A), surface = Color(0xFF1A161F), card = Color(0xFF211C28),
        secondary = Color(0xFF1D1923), elevated = Color(0xFF282230), userBubble = Color(0xFF2F2838), border = Color(0xFF322B3B),
        textPrimary = Color(0xFFEAE6EE), textSecondary = Color(0xFFABA3B4), textMuted = Color(0xFF8D8498),
        accent = Color(0xFFB59AC0), accent2 = Color(0xFFB59AC0), accent3 = Color(0xFFB59AC0), onAccent = Color(0xFF14111A)
    )
    val paper = AppPalette(
        id = "paper", name = "Paper", nameAr = "ورق", dark = false,
        base = Color(0xFFF5F3EE), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEFEDE7), userBubble = Color(0xFFE9E6DF), border = Color(0xFFDDD9D0),
        textPrimary = Color(0xFF22201C), textSecondary = Color(0xFF5C5953), textMuted = Color(0xFF6B6760),
        accent = Color(0xFF4F6B58), accent2 = Color(0xFF4F6B58), accent3 = Color(0xFF4F6B58), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEFEDE7), codeHeader = Color(0xFFE5E2DA),
        danger = Color(0xFFDC2626)
    )

    val ocean = AppPalette(
        id = "ocean", name = "Ocean", nameAr = "محيط", dark = true,
        base = Color(0xFF0F1215), surface = Color(0xFF14191D), card = Color(0xFF1A2126),
        secondary = Color(0xFF161C20), elevated = Color(0xFF1E262C), userBubble = Color(0xFF232C33), border = Color(0xFF252F36),
        textPrimary = Color(0xFFE3E6E8), textSecondary = Color(0xFFA3A9AE), textMuted = Color(0xFF878D92),
        accent = Color(0xFF97B5C4), accent2 = Color(0xFF97B5C4), accent3 = Color(0xFF97B5C4), onAccent = Color(0xFF0F1215)
    )
    val teal = AppPalette(
        id = "teal", name = "Teal", nameAr = "فيروزي", dark = true,
        base = Color(0xFF0F1515), surface = Color(0xFF141C1C), card = Color(0xFF1B2525),
        secondary = Color(0xFF17201F), elevated = Color(0xFF1F2B2A), userBubble = Color(0xFF243231), border = Color(0xFF263534),
        textPrimary = Color(0xFFE3E8E8), textSecondary = Color(0xFFA3AEAD), textMuted = Color(0xFF879292),
        accent = Color(0xFF97C4BE), accent2 = Color(0xFF97C4BE), accent3 = Color(0xFF97C4BE), onAccent = Color(0xFF0F1515)
    )
    val violet = AppPalette(
        id = "violet", name = "Violet", nameAr = "بنفسجي", dark = true,
        base = Color(0xFF120F14), surface = Color(0xFF18151C), card = Color(0xFF201B24),
        secondary = Color(0xFF1B171F), elevated = Color(0xFF25202A), userBubble = Color(0xFF2A2531), border = Color(0xFF2D2734),
        textPrimary = Color(0xFFE5E3E8), textSecondary = Color(0xFFA8A3AE), textMuted = Color(0xFF8C8792),
        accent = Color(0xFFAA97C4), accent2 = Color(0xFFAA97C4), accent3 = Color(0xFFAA97C4), onAccent = Color(0xFF120F14)
    )
    val coffee = AppPalette(
        id = "coffee", name = "Coffee", nameAr = "قهوة", dark = true,
        base = Color(0xFF14120F), surface = Color(0xFF1C1815), card = Color(0xFF24201B),
        secondary = Color(0xFF1F1B17), elevated = Color(0xFF2A2520), userBubble = Color(0xFF312A25), border = Color(0xFF342D27),
        textPrimary = Color(0xFFE8E5E3), textSecondary = Color(0xFFAEA8A3), textMuted = Color(0xFF928C87),
        accent = Color(0xFFCCAD8E), accent2 = Color(0xFFCCAD8E), accent3 = Color(0xFFCCAD8E), onAccent = Color(0xFF14120F)
    )
    val crimson = AppPalette(
        id = "crimson", name = "Crimson", nameAr = "قرمزي", dark = true,
        base = Color(0xFF141010), surface = Color(0xFF1B1516), card = Color(0xFF241C1D),
        secondary = Color(0xFF1F1819), elevated = Color(0xFF2A2022), userBubble = Color(0xFF302527), border = Color(0xFF332729),
        textPrimary = Color(0xFFE8E3E4), textSecondary = Color(0xFFAEA3A5), textMuted = Color(0xFF928788),
        accent = Color(0xFFD1949E), accent2 = Color(0xFFD1949E), accent3 = Color(0xFFD1949E), onAccent = Color(0xFF141010)
    )
    val graphite = AppPalette(
        id = "graphite", name = "Graphite", nameAr = "جرافيت", dark = true,
        base = Color(0xFF070808), surface = Color(0xFF0D0E0F), card = Color(0xFF151517),
        secondary = Color(0xFF101112), elevated = Color(0xFF1A1A1C), userBubble = Color(0xFF1F2022), border = Color(0xFF222325),
        textPrimary = Color(0xFFE3E5E8), textSecondary = Color(0xFFA3A7AE), textMuted = Color(0xFF878A92),
        accent = Color(0xFF9FABBC), accent2 = Color(0xFF9FABBC), accent3 = Color(0xFF9FABBC), onAccent = Color(0xFF070808)
    )
    val indigo = AppPalette(
        id = "indigo", name = "Indigo", nameAr = "نيلي", dark = true,
        base = Color(0xFF0F1015), surface = Color(0xFF15161C), card = Color(0xFF1B1C25),
        secondary = Color(0xFF17181F), elevated = Color(0xFF1F212B), userBubble = Color(0xFF242631), border = Color(0xFF272834),
        textPrimary = Color(0xFFE3E4E8), textSecondary = Color(0xFFA3A4AE), textMuted = Color(0xFF878892),
        accent = Color(0xFF97A0C4), accent2 = Color(0xFF97A0C4), accent3 = Color(0xFF97A0C4), onAccent = Color(0xFF0F1015)
    )
    val cloud = AppPalette(
        id = "cloud", name = "Cloud", nameAr = "غيمة", dark = false,
        base = Color(0xFFF3F5F7), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEAEDF1), userBubble = Color(0xFFE0E6EB), border = Color(0xFFD0D6DC),
        textPrimary = Color(0xFF1A1F23), textSecondary = Color(0xFF4D545B), textMuted = Color(0xFF656B72),
        accent = Color(0xFF3B5772), accent2 = Color(0xFF3B5772), accent3 = Color(0xFF3B5772), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEAEDF1), codeHeader = Color(0xFFDDE3E9),
        danger = Color(0xFFDC2626)
    )
    val mint = AppPalette(
        id = "mint", name = "Mint", nameAr = "نعناع", dark = false,
        base = Color(0xFFF3F7F5), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEAF1ED), userBubble = Color(0xFFE0EBE6), border = Color(0xFFD0DCD6),
        textPrimary = Color(0xFF1A231F), textSecondary = Color(0xFF49554F), textMuted = Color(0xFF626F69),
        accent = Color(0xFF376C52), accent2 = Color(0xFF376C52), accent3 = Color(0xFF376C52), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEAF1ED), codeHeader = Color(0xFFDDE9E3),
        danger = Color(0xFFDC2626)
    )
    val lavender = AppPalette(
        id = "lavender", name = "Lavender", nameAr = "لافندر", dark = false,
        base = Color(0xFFF4F3F7), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEDEAF1), userBubble = Color(0xFFE5E0EB), border = Color(0xFFD5D0DC),
        textPrimary = Color(0xFF1E1A23), textSecondary = Color(0xFF56505E), textMuted = Color(0xFF6A6572),
        accent = Color(0xFF523B72), accent2 = Color(0xFF523B72), accent3 = Color(0xFF523B72), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEDEAF1), codeHeader = Color(0xFFE2DDE9),
        danger = Color(0xFFDC2626)
    )
    val peach = AppPalette(
        id = "peach", name = "Peach", nameAr = "خوخ", dark = false,
        base = Color(0xFFF7F4F3), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFF1ECEA), userBubble = Color(0xFFEBE4E0), border = Color(0xFFDCD5D0),
        textPrimary = Color(0xFF231D1A), textSecondary = Color(0xFF58504B), textMuted = Color(0xFF726965),
        accent = Color(0xFF724F3B), accent2 = Color(0xFF724F3B), accent3 = Color(0xFF724F3B), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFF1ECEA), codeHeader = Color(0xFFE9E1DD),
        danger = Color(0xFFDC2626)
    )
    val sky = AppPalette(
        id = "sky", name = "Sky", nameAr = "سماء", dark = false,
        base = Color(0xFFF3F6F7), surface = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF), elevated = Color(0xFFEAEEF1), userBubble = Color(0xFFE0E7EB), border = Color(0xFFD0D8DC),
        textPrimary = Color(0xFF1A2023), textSecondary = Color(0xFF4B5458), textMuted = Color(0xFF656D72),
        accent = Color(0xFF3B6072), accent2 = Color(0xFF3B6072), accent3 = Color(0xFF3B6072), onAccent = Color(0xFFFFFFFF),
        codeBackground = Color(0xFFEAEEF1), codeHeader = Color(0xFFDDE5E9),
        danger = Color(0xFFDC2626)
    )

    val all: List<AppPalette> = listOf(
        obsidian, midnight, ember, forest, rose, ocean, teal, violet, coffee, crimson, graphite, indigo,
        paper, cloud, mint, lavender, peach, sky
    )
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

package games.supersetdeck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import games.supersetdeck.model.Suit

/** One tonal trio per suit: the symbol's ink, a container, and the text on that container. */
@Immutable
data class SuitColors(val ink: Color, val container: Color, val onContainer: Color)

/** The design's color roles; [scheme] maps them to Material 3 for the stock components. */
@Immutable
data class SsdColors(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    /** Primary used as a text color on surfaces (links, text buttons). */
    val primaryText: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val inversePrimary: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceDim: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val suits: Map<Suit, SuitColors>,
) {
    fun suit(suit: Suit): SuitColors = suits.getValue(suit)

    val scheme: ColorScheme = if (isDark) darkColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary, secondary = secondary, onSecondary = surface, secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer, tertiary = tertiary, onTertiary = surface, tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer, background = surfaceContainer, onBackground = onSurface, surface = surface,
        onSurface = onSurface, surfaceVariant = surfaceContainerHighest, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary, inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface, error = error,
        onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer, outline = outline,
        outlineVariant = outlineVariant, scrim = Color.Black, surfaceBright = surfaceContainerHighest,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest, surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceDim, surfaceDim = surfaceDim,
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary, secondary = secondary, onSecondary = Color.White, secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer, tertiary = tertiary, onTertiary = Color.White, tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer, background = surfaceContainer, onBackground = onSurface, surface = surface,
        onSurface = onSurface, surfaceVariant = surfaceContainerHighest, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary, inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface, error = error,
        onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer, outline = outline,
        outlineVariant = outlineVariant, scrim = Color.Black, surfaceBright = surface,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest, surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = Color.White, surfaceDim = surfaceDim,
    )
}

val LightColors = SsdColors(
    isDark = false,
    primary = Color(0xFFFFA726),
    onPrimary = Color(0xFF2D1A00),
    primaryText = Color(0xFF80561F),
    primaryContainer = Color(0xFFFAC280),
    onPrimaryContainer = Color(0xFF4A2A00),
    secondary = Color(0xFF725B41),
    secondaryContainer = Color(0xFFFDDDBD),
    onSecondaryContainer = Color(0xFF3E2C17),
    inversePrimary = Color(0xFFFFB95C),
    tertiary = Color(0xFFA3214F),
    tertiaryContainer = Color(0xFFFFD9E0),
    onTertiaryContainer = Color(0xFF3F0018),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF382E25),
    inverseOnSurface = Color(0xFFFBEEE3),
    surface = Color(0xFFFFF8F4),
    surfaceContainerLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFBEBDD),
    surfaceContainerHigh = Color(0xFFF6E5D7),
    surfaceContainerHighest = Color(0xFFF1DFCF),
    surfaceDim = Color(0xFFE9D7C7),
    onSurface = Color(0xFF231A12),
    onSurfaceVariant = Color(0xFF594A3D),
    outline = Color(0xFF86786A),
    outlineVariant = Color(0xFFD8C5B5),
    suits = mapOf(
        Suit.Stars to SuitColors(Color(0xFF8A6400), Color(0xFFFFDF93), Color(0xFF2B1E00)),
        Suit.Spades to SuitColors(Color(0xFF3341A6), Color(0xFFDFE0FF), Color(0xFF000F5C)),
        Suit.Hearts to SuitColors(Color(0xFFC0213F), Color(0xFFFFDADB), Color(0xFF40000E)),
        Suit.Clubs to SuitColors(Color(0xFF1E7B3B), Color(0xFFB7F3BE), Color(0xFF00210B)),
        Suit.Diamonds to SuitColors(Color(0xFFC2410C), Color(0xFFFFDCC2), Color(0xFF351000)),
        Suit.Florettes to SuitColors(Color(0xFFA12D9C), Color(0xFFFFD6F6), Color(0xFF380037)),
        Suit.Wheels to SuitColors(Color(0xFF00707F), Color(0xFFB0EEF9), Color(0xFF001F24)),
        Suit.Specials to SuitColors(Color(0xFF6247D4), Color(0xFFE9E0FF), Color(0xFF1F0A63)),
    ),
)

val DarkColors = SsdColors(
    isDark = true,
    primary = Color(0xFFFFA726),
    onPrimary = Color(0xFF2D1A00),
    primaryText = Color(0xFFFFB961),
    primaryContainer = Color(0xFF663E00),
    onPrimaryContainer = Color(0xFFFFDDB9),
    secondary = Color(0xFFDFC1A2),
    secondaryContainer = Color(0xFF4B3821),
    onSecondaryContainer = Color(0xFFF5DCC0),
    inversePrimary = Color(0xFF80561F),
    tertiary = Color(0xFFFFB1C2),
    tertiaryContainer = Color(0xFF8D0A3F),
    onTertiaryContainer = Color(0xFFFFD9DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFFFF8F4),
    inverseOnSurface = Color(0xFF382E25),
    surface = Color(0xFF120D08),
    surfaceContainerLow = Color(0xFF18120B),
    surfaceContainer = Color(0xFF201810),
    surfaceContainerHigh = Color(0xFF261E15),
    surfaceContainerHighest = Color(0xFF2E2419),
    surfaceDim = Color(0xFF0B0805),
    onSurface = Color(0xFFF5E2D2),
    onSurfaceVariant = Color(0xFFB8A899),
    outline = Color(0xFF9A8B7C),
    outlineVariant = Color(0xFF52463A),
    suits = mapOf(
        Suit.Stars to SuitColors(Color(0xFFF0BF5D), Color(0xFF5D4200), Color(0xFFFFDEA4)),
        Suit.Spades to SuitColors(Color(0xFFBCC2FF), Color(0xFF2D3BA0), Color(0xFFDFE0FF)),
        Suit.Hearts to SuitColors(Color(0xFFFFB3B6), Color(0xFF920027), Color(0xFFFFDADA)),
        Suit.Clubs to SuitColors(Color(0xFF80DA8E), Color(0xFF005321), Color(0xFF9CF7A8)),
        Suit.Diamonds to SuitColors(Color(0xFFFFB59D), Color(0xFF832600), Color(0xFFFFDBD0)),
        Suit.Florettes to SuitColors(Color(0xFFFFABF2), Color(0xFF81017F), Color(0xFFFFD7F5)),
        Suit.Wheels to SuitColors(Color(0xFF81D3E3), Color(0xFF004E59), Color(0xFFA1EFFF)),
        Suit.Specials to SuitColors(Color(0xFFCABEFF), Color(0xFF4726B9), Color(0xFFE6DEFF)),
    ),
)

/** The full-screen references gallery is dark in both themes. */
object GalleryColors {
    val background = Color(0xFF15100B)
    val control = Color(0xFF352B22)
    val onBackground = Color(0xFFF1DFCF)
    val paper = Color(0xFFFBEEE3)
    val overlay = Color(0xB815100B)
    val firstPane = Color(0xFFFFB95C)
    val secondPane = Color(0xFFFFB0C8)
}

val LocalSsdColors = staticCompositionLocalOf { LightColors }

/** `#RRGGBB`, for the HTML side. */
fun Color.toHex(): String {
    val argb = (alpha * 255).toInt() shl 24 or ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
    return "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()
}

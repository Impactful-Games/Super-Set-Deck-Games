package games.supersetdeck.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import games.supersetdeck.resources.Res
import games.supersetdeck.ui.components.scrollbarStyle
import games.supersetdeck.resources.lct_picon_bold
import games.supersetdeck.resources.lct_picon_extra_bold
import games.supersetdeck.resources.lct_picon_medium
import games.supersetdeck.resources.lct_picon_regular
import games.supersetdeck.resources.lct_picon_semi_bold
import org.jetbrains.compose.resources.Font

/** The design's type scale: one family everywhere, LCT Picon, which holds every card glyph. */
@Immutable
data class SsdType(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val overline: TextStyle,
)

private fun ssdType(family: FontFamily): SsdType {
    fun style(weight: Int, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp,
    )
    return SsdType(
        displayLarge = style(800, 72, 72, -2.0),
        displayMedium = style(800, 56, 56, -1.5),
        displaySmall = style(800, 40, 42, -1.5),
        headlineLarge = style(700, 34, 40),
        headlineMedium = style(700, 28, 34),
        headlineSmall = style(700, 24, 32),
        titleLarge = style(600, 22, 28),
        titleMedium = style(600, 17, 22),
        bodyLarge = style(400, 17, 28),
        bodyMedium = style(400, 14, 20),
        bodySmall = style(400, 12, 16),
        labelLarge = style(700, 15, 20),
        labelMedium = style(600, 14, 20),
        labelSmall = style(700, 12, 16, 0.8),
        overline = style(700, 12, 16, 0.8),
    )
}

val LocalSsdType = staticCompositionLocalOf<SsdType> { error("No SsdType") }

object SsdTheme {
    val colors: SsdColors @Composable get() = LocalSsdColors.current
    val type: SsdType @Composable get() = LocalSsdType.current
}

@Composable
fun SsdTheme(dark: Boolean, content: @Composable () -> Unit) {
    val family = FontFamily(
        Font(Res.font.lct_picon_regular, FontWeight.Normal),
        Font(Res.font.lct_picon_medium, FontWeight.Medium),
        Font(Res.font.lct_picon_semi_bold, FontWeight.SemiBold),
        Font(Res.font.lct_picon_bold, FontWeight.Bold),
        Font(Res.font.lct_picon_extra_bold, FontWeight.ExtraBold),
    )
    val type = remember(family) { ssdType(family) }
    val colors = if (dark) DarkColors else LightColors
    val typography = remember(type) {
        Typography(
            displayLarge = type.displayLarge,
            displayMedium = type.displayMedium,
            displaySmall = type.displaySmall,
            headlineLarge = type.headlineLarge,
            headlineMedium = type.headlineMedium,
            headlineSmall = type.headlineSmall,
            titleLarge = type.titleLarge,
            titleMedium = type.titleMedium,
            titleSmall = type.labelLarge,
            bodyLarge = type.bodyLarge,
            bodyMedium = type.bodyMedium,
            bodySmall = type.bodySmall,
            labelLarge = type.labelLarge,
            labelMedium = type.labelMedium,
            labelSmall = type.labelSmall.copy(letterSpacing = 0.4.em),
        )
    }
    MaterialTheme(
        colorScheme = colors.scheme,
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) {
        CompositionLocalProvider(
            LocalSsdColors provides colors,
            LocalSsdType provides type,
            LocalScrollbarStyle provides scrollbarStyle(colors.onSurface),
            content = content,
        )
    }
}

package games.supersetdeck.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density

/** The expressive shapes of the design: logo, list avatars, mascots and states. */
enum class Expressive {
    Cookie9, Cookie12, Sunny, Clover4;

    val shape: Shape
        @Composable get() = when (this) {
            Cookie9 -> MaterialShapes.Cookie9Sided
            Cookie12 -> MaterialShapes.Cookie12Sided
            Sunny -> MaterialShapes.Sunny
            Clover4 -> MaterialShapes.Clover4Leaf
        }.toShape()
}

/** A filled [shape] of [size], tilted by [rotation] degrees, with [content] centered on it. */
@Composable
fun ShapeBadge(
    shape: Expressive,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val outlineShape = shape.shape
        Canvas(Modifier.size(size).rotate(rotation)) {
            val outline: Outline = outlineShape.createOutline(this.size, LayoutDirection.Ltr, Density(density))
            drawOutline(outline, color)
        }
        content()
    }
}

/**
 * Rounded corners per side, for the connected button groups: fully round outside, tight inside.
 * The round corners are a percentage: in dp, an oversized radius would shrink the tight ones too.
 */
fun connectedShape(first: Boolean, last: Boolean, inner: Dp = 8.dp): Shape {
    val round = androidx.compose.foundation.shape.CornerSize(50)
    val tight = androidx.compose.foundation.shape.CornerSize(inner)
    return androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (first) round else tight,
        bottomStart = if (first) round else tight,
        topEnd = if (last) round else tight,
        bottomEnd = if (last) round else tight,
    )
}

/** A pill: fully round ends whatever the size. */
val Pill: Shape = androidx.compose.foundation.shape.RoundedCornerShape(50)

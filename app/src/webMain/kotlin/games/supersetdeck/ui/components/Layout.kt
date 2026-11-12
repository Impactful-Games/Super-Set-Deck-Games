package games.supersetdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The window's size class: [wide] gets the navigation rail, the others the bottom bar. */
@Immutable
data class WindowLayout(val width: Dp, val height: Dp) {
    val wide: Boolean get() = width >= 840.dp
    val compact: Boolean get() = width < 600.dp
    val landscape: Boolean get() = width > height
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout(1280.dp, 800.dp) }

/** A grid of as many columns of at least [minItemWidth] as fit, items of a row sharing its height. */
@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    minColumns: Int = 1,
    key: ((T) -> Any)? = null,
    item: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = (((maxWidth + spacing) / (minItemWidth + spacing)).toInt()).coerceAtLeast(minColumns).coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { value ->
                        Box(Modifier.weight(1f)) {
                            if (key != null) androidx.compose.runtime.key(key(value)) { item(value) } else item(value)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

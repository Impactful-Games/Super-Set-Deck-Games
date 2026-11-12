package games.supersetdeck.ui.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import games.supersetdeck.ui.theme.Pill

/** The scrollbars of the theme: a pill, visible at rest, darker when hovered or dragged. */
fun scrollbarStyle(color: Color, thickness: Dp = 8.dp) = ScrollbarStyle(
    minimalHeight = 32.dp,
    thickness = thickness,
    shape = Pill,
    hoverDurationMillis = 200,
    unhoverColor = color.copy(alpha = 0.25f),
    hoverColor = color.copy(alpha = 0.55f),
)

// The scrollbars take the size of their box (matchParentSize) rather than filling the space they
// are offered: in a box sized by its content (a dialog, a menu), filling would stretch the box.
// They also stay out of intrinsic measurements, which menus use to size themselves: measured
// that way, a scrollbar filling its height would ask for an infinite one, and crash.

/** Lays out [content] in the given constraints, but counts for nothing in intrinsic measurements. */
@Composable
private fun WithoutIntrinsics(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier, object : MeasurePolicy {
        override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
            val placeables = measurables.map { it.measure(constraints) }
            return layout(constraints.maxWidth, constraints.maxHeight) { placeables.forEach { it.place(0, 0) } }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
        override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
        override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
        override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
    })
}

/**
 * The scrollbar of a vertical [state], along the end edge of its box. It only shows when the
 * content overflows; [padding] keeps it clear of rounded corners.
 */
@Composable
fun BoxScope.VerticalScrollbarFor(
    state: ScrollState,
    padding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 3.dp),
    style: ScrollbarStyle = LocalScrollbarStyle.current,
) {
    WithoutIntrinsics(Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.CenterEnd) {
            VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.fillMaxHeight(), style = style)
        }
    }
}

/** The scrollbar of a horizontal [state], along the bottom edge of its box. */
@Composable
fun BoxScope.HorizontalScrollbarFor(
    state: ScrollState,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 1.dp),
    style: ScrollbarStyle = LocalScrollbarStyle.current,
) {
    WithoutIntrinsics(Modifier.matchParentSize()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.BottomCenter) {
            HorizontalScrollbar(rememberScrollbarAdapter(state), Modifier.fillMaxWidth(), style = style)
        }
    }
}

/**
 * A column that scrolls vertically, with its scrollbar. [contentPadding] is inside the scrolled
 * area, so the content scrolls through it up to the edges.
 */
@Composable
fun ScrollingColumn(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: ScrollState = rememberScrollState(),
    scrollbarPadding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 3.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(
            contentModifier.verticalScroll(state).padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
        VerticalScrollbarFor(state, scrollbarPadding)
    }
}

/** A row that scrolls horizontally, with a thin scrollbar in the [scrollbarSpace] left under its items. */
@Composable
fun ScrollingRow(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    scrollbarSpace: Dp = 10.dp,
    style: ScrollbarStyle = LocalScrollbarStyle.current.copy(thickness = 4.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier) {
        Row(
            contentModifier.horizontalScroll(state).padding(bottom = scrollbarSpace),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
        HorizontalScrollbarFor(state, PaddingValues(horizontal = 12.dp), style)
    }
}

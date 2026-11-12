package games.supersetdeck.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.SsdTheme

/** The hand cursor of links and buttons. */
fun Modifier.handCursor(enabled: Boolean = true): Modifier = if (enabled) pointerHoverIcon(PointerIcon.Hand) else this

enum class ButtonKind { Filled, Tonal, Container, Outlined, Text, Danger, DangerText }

@Composable
private fun ButtonKind.colors(): Triple<Color, Color, BorderStroke?> {
    val c = SsdTheme.colors
    return when (this) {
        ButtonKind.Filled -> Triple(c.primary, c.onPrimary, null)
        ButtonKind.Tonal -> Triple(c.secondaryContainer, c.onSecondaryContainer, null)
        ButtonKind.Container -> Triple(c.primaryContainer, c.onPrimaryContainer, null)
        ButtonKind.Outlined -> Triple(Color.Transparent, c.onSurface, BorderStroke(1.dp, c.outline))
        ButtonKind.Text -> Triple(Color.Transparent, c.primaryText, null)
        ButtonKind.Danger -> Triple(c.error, c.onError, null)
        ButtonKind.DangerText -> Triple(Color.Transparent, c.error, null)
    }
}

/** The design's buttons: a pill by default, a label and an optional leading icon. */
@Composable
fun SsdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    shape: Shape = Pill,
    textStyle: TextStyle = SsdTheme.type.labelLarge,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(start = if (icon != null) 18.dp else 22.dp, end = 22.dp),
) {
    val (container, content, border) = kind.colors()
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).alpha(if (enabled) 1f else 0.38f).handCursor(enabled),
        shape = shape,
        color = container,
        contentColor = content,
        border = border,
        shadowElevation = elevation,
    ) {
        Row(
            Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(22.dp))
            Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A round icon button; [contentDescription] names it for assistive technologies. */
@Composable
fun SsdIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = SsdTheme.colors.secondaryContainer,
    content: Color = SsdTheme.colors.onSecondaryContainer,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    shape: Shape = Pill,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size).alpha(if (enabled) 1f else 0.38f).handCursor(enabled)
            .semantics { this.contentDescription = contentDescription },
        shape = shape,
        color = container,
        contentColor = content,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize)) }
    }
}

/**
 * One button of a connected group: tight inner corners, round outer ones, and fully round once
 * selected — the expressive shape morph, animated.
 */
@Composable
fun ConnectedToggle(
    selected: Boolean,
    onClick: () -> Unit,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    inner: Dp = 8.dp,
    enabled: Boolean = true,
    selectedColors: Pair<Color, Color> = SsdTheme.colors.primary to SsdTheme.colors.onPrimary,
    unselectedColors: Pair<Color, Color> = SsdTheme.colors.surfaceContainerHigh to SsdTheme.colors.onSurfaceVariant,
    role: Role = Role.Checkbox,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val round = height / 2
    val start by animateDpAsState(if (selected || first) round else inner)
    val end by animateDpAsState(if (selected || last) round else inner)
    val (container, contentColor) = if (selected) selectedColors else unselectedColors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).defaultMinSize(minWidth = height).alpha(if (enabled) 1f else 0.38f)
            .handCursor(enabled)
            .semantics {
                this.role = role
                this.selected = selected
            },
        shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
        color = container,
        contentColor = contentColor,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** A connected group: buttons 3 dp apart. */
@Composable
fun ConnectedRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

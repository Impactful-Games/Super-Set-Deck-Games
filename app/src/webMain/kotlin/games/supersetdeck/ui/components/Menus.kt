package games.supersetdeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.SsdTheme

/** The design's menu: a rounded surface-container card, 8 dp of padding. */
@Composable
fun SsdMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    maxHeight: Dp? = null,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowLimit = LocalWindowLayout.current.height - 160.dp
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.then(if (width != null) Modifier.width(width) else Modifier),
        offset = offset,
        shape = RoundedCornerShape(20.dp),
        containerColor = SsdTheme.colors.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        OverlayMarker()
        // The menu scrolls its content itself, where no scrollbar can be attached: the content
        // scrolls on its own, with a scrollbar, before getting taller than most of the window.
        ScrollingColumn(
            Modifier.heightIn(max = if (maxHeight != null && maxHeight < windowLimit) maxHeight else windowLimit),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            scrollbarPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
            content = content,
        )
    }
}

/**
 * A menu item; with [selected] non-null it is a radio or check item: a check mark and a tonal
 * background once selected, the label aligned on the check mark's right otherwise.
 */
@Composable
fun SsdMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    emphasis: Boolean = false,
    height: Dp = 48.dp,
) {
    val c = SsdTheme.colors
    val isSelected = selected == true
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(14.dp))
            .then(if (isSelected) Modifier.background(c.secondaryContainer, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = enabled, role = if (selected != null) Role.RadioButton else Role.Button) { onClick() }
            .handCursor(enabled)
            .semantics { if (selected != null) this.selected = isSelected }
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = when {
            isSelected -> c.onSecondaryContainer
            emphasis -> c.primaryText
            else -> c.onSurface
        }
        when {
            isSelected -> Icon(Icons.Check, null, Modifier.size(20.dp), tint = content)
            icon != null -> Icon(icon, null, Modifier.size(20.dp), tint = iconTint ?: content)
            selected != null -> Spacer(Modifier.width(20.dp))
        }
        Text(
            text,
            Modifier.weight(1f),
            style = if (isSelected || emphasis) SsdTheme.type.labelLarge else SsdTheme.type.labelLarge.copy(fontWeight = SsdTheme.type.bodyLarge.fontWeight),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) Box { trailing() }
    }
}

@Composable
fun SsdMenuDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = SsdTheme.colors.outlineVariant)
}

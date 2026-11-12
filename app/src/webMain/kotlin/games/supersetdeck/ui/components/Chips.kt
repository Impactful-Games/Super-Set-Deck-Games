package games.supersetdeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import games.supersetdeck.data.UserList
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.SsdTheme

private val ChipShape = RoundedCornerShape(10.dp)

/** A selectable chip: tonal with a check mark once selected, outlined otherwise. */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    height: Dp = 36.dp,
) {
    val c = SsdTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).alpha(if (enabled) 1f else 0.38f).handCursor(enabled)
            .semantics {
                role = Role.Checkbox
                this.selected = selected
            },
        shape = ChipShape,
        color = if (selected) c.secondaryContainer else Color.Transparent,
        contentColor = if (selected) c.onSecondaryContainer else c.onSurface,
        border = if (selected) null else BorderStroke(1.dp, if (enabled) c.outline else c.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = if (selected || icon != null) 10.dp else 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                selected -> Icon(Icons.Check, null, Modifier.size(16.dp))
                icon != null -> Icon(icon, null, Modifier.size(16.dp), tint = iconTint ?: c.primaryText)
            }
            Text(label, style = if (selected) SsdTheme.type.labelLarge.copy(fontSize = SsdTheme.type.labelMedium.fontSize) else SsdTheme.type.labelMedium, maxLines = 1)
        }
    }
}

/** An outlined chip with a leading icon that leads somewhere (a game type to filter on). */
@Composable
fun LinkChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    description: String? = null,
) {
    val c = SsdTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(height).handCursor()
            .semantics { if (description != null) contentDescription = description },
        shape = ChipShape,
        color = Color.Transparent,
        contentColor = c.onSurface,
        border = BorderStroke(1.dp, c.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.primaryText)
            Text(label, style = SsdTheme.type.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The icon of a list: a heart for the favourites, a bookmark for the others. */
@Composable
fun listIcon(list: UserList): Pair<ImageVector, Color> =
    if (list.isFavourites) Icons.Heart to SsdTheme.colors.tertiary else Icons.Bookmark to SsdTheme.colors.primaryText

/** A list a game belongs to: tapping it shows the list's games, the cross removes the game from it. */
@Composable
fun ListChip(
    list: UserList,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    val name = list.displayName(strings)
    val (container, content) = if (list.isFavourites) c.tertiaryContainer to c.onTertiaryContainer else c.primaryContainer to c.onPrimaryContainer
    Surface(modifier.height(height), shape = ChipShape, color = container, contentColor = content) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.fillMaxHeight().clip(ChipShape).clickable(onClickLabel = strings.filterBy(name)) { onClick() }.handCursor()
                    .padding(start = 10.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (icon, tint) = listIcon(list)
                Icon(icon, null, Modifier.size(18.dp), tint = if (list.isFavourites) tint else content)
                Text(name, style = SsdTheme.type.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.width(32.dp).fillMaxHeight().clip(ChipShape).handCursor()
                    .clickable(role = Role.Button) { onRemove() }
                    .semantics { contentDescription = strings.removeFrom(name) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Close, null, Modifier.size(16.dp))
            }
        }
    }
}

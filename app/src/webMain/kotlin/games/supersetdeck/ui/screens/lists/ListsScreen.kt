package games.supersetdeck.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Games
import games.supersetdeck.data.Lists
import games.supersetdeck.data.ListsState
import games.supersetdeck.data.Settings
import games.supersetdeck.data.UserList
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.ui.components.AdaptiveGrid
import games.supersetdeck.ui.components.ButtonKind
import games.supersetdeck.ui.components.ConfirmDialog
import games.supersetdeck.ui.components.ErrorCard
import games.supersetdeck.ui.components.LoadingArea
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.TextFieldDialog
import games.supersetdeck.ui.components.dashedBorder
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.screens.ScreenColumn
import games.supersetdeck.ui.screens.ScreenHeader
import games.supersetdeck.ui.screens.ScreenTitle
import games.supersetdeck.ui.screens.gameCount
import games.supersetdeck.ui.screens.showList
import games.supersetdeck.ui.screens.sync.SyncScreen
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme

/** My Lists; the backup / restore flow opens in place of it. */
@Composable
fun ListsScreen() {
    var syncing by remember { mutableStateOf(false) }
    if (syncing) SyncScreen(onExit = { syncing = false })
    else ListsContent(onSync = { syncing = true })
}

private sealed interface ListDialog {
    data object Create : ListDialog
    data class Rename(val list: UserList) : ListDialog
    data class Delete(val list: UserList, val games: List<String>) : ListDialog
}

@Composable
private fun ListsContent(onSync: () -> Unit) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val state = Lists.state
    var dialog by remember { mutableStateOf<ListDialog?>(null) }
    val page = rememberPageScroll()

    Box(Modifier.fillMaxSize()) {
        ScreenColumn(page, spacing = if (layout.wide) 40.dp else 18.dp) {
            ScreenHeader(actions = {
                if (state is ListsState.Ready) {
                    if (layout.wide) {
                        SsdButton(strings.backupRestore, onSync, kind = ButtonKind.Tonal, icon = Icons.CloudUpload, height = 56.dp)
                        SsdButton(strings.newList, { dialog = ListDialog.Create }, icon = Icons.Plus, height = 64.dp, shape = RoundedCornerShape(20.dp), elevation = 3.dp, textStyle = SsdTheme.type.labelLarge.copy(fontSize = 16.sp))
                    } else {
                        SsdIconButton(Icons.CloudUpload, strings.backupRestore, onSync)
                    }
                }
            }) {
                ScreenTitle(strings.navLists)
            }

            when (state) {
                ListsState.Loading -> LoadingArea()
                is ListsState.Unavailable -> {
                    ErrorCard(strings.databaseError(state.message))
                    Column(Modifier.alpha(0.5f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(2) { Box(Modifier.fillMaxWidth().height(72.dp).dashedBorder(2.dp, SsdTheme.colors.outlineVariant, 28.dp)) }
                    }
                }
                is ListsState.Ready -> AdaptiveGrid(state.data.lists, minItemWidth = 360.dp, spacing = if (layout.wide) 16.dp else 8.dp, key = { it.id }) { list ->
                    ListCard(
                        list,
                        index = state.data.lists.indexOf(list),
                        onRename = { dialog = ListDialog.Rename(list) },
                        onDelete = {
                            val ids = state.data.gamesOf(list.id)
                            if (ids.isEmpty()) Lists.delete(list.id)
                            else dialog = ListDialog.Delete(list, ids.map { id -> Games[id]?.name(Settings.lang.code) ?: id }.sorted())
                        },
                    )
                }
            }
        }

        if (!layout.wide && state is ListsState.Ready) {
            SsdButton(
                strings.newList, { dialog = ListDialog.Create },
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                icon = Icons.Plus, height = 60.dp, shape = RoundedCornerShape(20.dp), elevation = 4.dp,
                textStyle = SsdTheme.type.labelLarge.copy(fontSize = 16.sp),
            )
        }
    }

    when (val d = dialog) {
        null -> {}
        ListDialog.Create -> TextFieldDialog(strings.newGameList, strings.listName, "", strings.create, onConfirm = {
            Lists.create(it)
            dialog = null
        }, onDismiss = { dialog = null })
        is ListDialog.Rename -> TextFieldDialog(strings.renameTitle(d.list.name), strings.listName, d.list.name, strings.rename, onConfirm = {
            Lists.rename(d.list.id, it)
            dialog = null
        }, onDismiss = { dialog = null })
        is ListDialog.Delete -> ConfirmDialog(
            icon = Icons.Delete,
            title = strings.deleteTitle(d.list.name),
            text = d.games.joinToString(", "),
            confirmLabel = strings.delete,
            onConfirm = {
                Lists.delete(d.list.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

/** The avatars of the lists, in turn: the favourites keep their heart. */
private val avatars = listOf(Expressive.Cookie9, Expressive.Sunny, Expressive.Clover4, Expressive.Cookie9)
private val tilts = listOf(-8f, 6f, 0f, 14f)

@Composable
private fun ListCard(list: UserList, index: Int, onRename: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val name = list.displayName(strings)
    val (fill, ink) = when {
        list.isFavourites -> c.tertiaryContainer to c.tertiary
        else -> listOf(
            c.primaryContainer to c.onPrimaryContainer,
            c.suit(games.supersetdeck.model.Suit.Wheels).container to c.suit(games.supersetdeck.model.Suit.Wheels).onContainer,
            c.suit(games.supersetdeck.model.Suit.Stars).container to c.suit(games.supersetdeck.model.Suit.Stars).onContainer,
            c.suit(games.supersetdeck.model.Suit.Clubs).container to c.suit(games.supersetdeck.model.Suit.Clubs).onContainer,
        )[(index - 1).coerceAtLeast(0) % 4]
    }
    Surface(Modifier.fillMaxWidth().fillMaxHeight(), shape = RoundedCornerShape(if (wide) 32.dp else 26.dp), color = c.surfaceContainerLow, contentColor = c.onSurface) {
        Row(Modifier.padding(start = if (wide) 16.dp else 10.dp, end = if (wide) 12.dp else 6.dp, top = if (wide) 16.dp else 10.dp, bottom = if (wide) 16.dp else 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 4.dp)) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button) { showList(list) }.handCursor(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 14.dp),
            ) {
                val size = if (wide) 76.dp else 56.dp
                ShapeBadge(
                    if (list.isFavourites) Expressive.Cookie12 else avatars[(index - 1).coerceAtLeast(0) % avatars.size],
                    fill, size,
                    rotation = if (list.isFavourites) 0f else tilts[(index - 1).coerceAtLeast(0) % tilts.size],
                ) {
                    Text(
                        if (list.isFavourites) "♥" else name.trim().take(1).uppercase(),
                        style = SsdTheme.type.headlineLarge.copy(fontSize = if (wide) 30.sp else 22.sp, fontWeight = SsdTheme.type.displayLarge.fontWeight),
                        color = ink,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = if (wide) SsdTheme.type.headlineSmall.copy(lineHeight = 30.sp) else SsdTheme.type.titleLarge.copy(fontSize = 18.sp, fontWeight = SsdTheme.type.labelLarge.fontWeight), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(strings.gameCount(list.id), style = SsdTheme.type.bodyMedium.copy(fontSize = if (wide) 15.sp else 14.sp), color = c.onSurfaceVariant)
                }
            }
            if (!list.isFavourites) {
                SsdIconButton(Icons.Edit, strings.renameList(name), onRename, container = if (wide) c.surfaceContainerHigh else Color.Transparent, content = if (wide) c.onSurface else c.onSurfaceVariant, size = 44.dp, iconSize = 20.dp)
                SsdIconButton(Icons.Delete, strings.deleteList(name), onDelete, container = Color.Transparent, content = c.error, size = 44.dp, iconSize = 20.dp)
            }
        }
    }
}

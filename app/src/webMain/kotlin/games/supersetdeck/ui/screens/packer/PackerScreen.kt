package games.supersetdeck.ui.screens.packer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Games
import games.supersetdeck.data.GamesState
import games.supersetdeck.data.Settings
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.i18n.Strings
import games.supersetdeck.model.Game
import games.supersetdeck.model.Pack
import games.supersetdeck.model.PackedGame
import games.supersetdeck.model.abbreviatedText
import games.supersetdeck.navigation.Location
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Routes
import games.supersetdeck.platform.normalizeForSearch
import games.supersetdeck.ui.components.AdaptiveGrid
import games.supersetdeck.ui.components.ButtonKind
import games.supersetdeck.ui.components.DeckView
import games.supersetdeck.ui.components.DialogActions
import games.supersetdeck.ui.components.GameConfiguration
import games.supersetdeck.ui.components.LoadingArea
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.SsdDialog
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.SsdMenu
import games.supersetdeck.ui.components.SsdMenuItem
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.layout.Navigation
import games.supersetdeck.ui.screens.ScreenColumn
import games.supersetdeck.ui.screens.ScreenHeader
import games.supersetdeck.ui.screens.ScreenTitle
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.model.Suit

private sealed interface PackerDialog {
    data object Add : PackerDialog
    data class Edit(val packed: PackedGame) : PackerDialog
}

/** "3-5 players. With 1 variant." */
private fun Strings.packedDescription(packed: PackedGame): String =
    players(packed.players.abbreviatedText()) + "." +
        if (packed.game.variants.isNotEmpty()) " " + variants(packed.variants.count { it in packed.game.variants }) + "." else ""

/** The Travel Packer: the cards to take for a selection of games, the selection kept in the URL. */
@Composable
fun PackerScreen(location: Location) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val lang = Settings.lang.code
    val state = Games.state
    val page = rememberPageScroll()
    var dialog by remember { mutableStateOf<PackerDialog?>(null) }
    SideEffect { Navigation.lastPacker = location }

    val games = (state as? GamesState.Loaded)?.games
    val packed = games?.let { Routes.packedGames(location, it) }.orEmpty()
    val pack = remember(packed) { Pack(packed) }
    fun update(list: List<PackedGame>) = Router.replaceParams(Routes.packer(list).params)

    Box(Modifier.fillMaxSize()) {
        ScreenColumn(page, spacing = 32.dp) {
            ScreenHeader(actions = {
                if (layout.wide && games != null) {
                    SsdButton(
                        strings.addGameToPack, { dialog = PackerDialog.Add }, kind = ButtonKind.Container, icon = Icons.Plus,
                        height = 64.dp, shape = RoundedCornerShape(20.dp), elevation = 3.dp, textStyle = SsdTheme.type.labelLarge.copy(fontSize = 16.sp),
                    )
                }
            }) {
                if (layout.wide) {
                    ShapeBadge(Expressive.Clover4, SsdTheme.colors.suit(Suit.Wheels).container, 80.dp, rotation = -8f) {
                        Icon(Icons.Luggage, null, Modifier.size(36.dp), tint = SsdTheme.colors.suit(Suit.Wheels).ink)
                    }
                }
                ScreenTitle(strings.navPacker)
            }

            if (games == null) {
                LoadingArea()
            } else {
                if (packed.isNotEmpty()) {
                    AdaptiveGrid(packed.sortedBy { normalizeForSearch(it.game.name(lang)) }, minItemWidth = 320.dp, spacing = if (layout.wide) 16.dp else 8.dp, key = { it.game.id }) { item ->
                        PackedGameCard(
                            item, lang,
                            onEdit = { dialog = PackerDialog.Edit(item) },
                            onRemove = { update(packed - item) },
                        )
                    }
                }
                DeckView(pack, showCounts = true)
            }
        }

        if (!layout.wide && games != null) {
            SsdIconButton(
                Icons.Plus, strings.addGameToPack, { dialog = PackerDialog.Add },
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                container = SsdTheme.colors.primary, content = SsdTheme.colors.onPrimary, size = 64.dp, iconSize = 28.dp,
                shape = RoundedCornerShape(22.dp),
            )
        }
    }

    when (val d = dialog) {
        null -> {}
        PackerDialog.Add -> AddGameDialog(
            available = games.orEmpty().filter { game -> packed.none { it.game.id == game.id } },
            lang = lang,
            onAdd = {
                update(packed + it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is PackerDialog.Edit -> EditGameDialog(
            d.packed, lang,
            onEdit = { edited ->
                update(packed.map { if (it.game.id == edited.game.id) edited else it })
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun PackedGameCard(item: PackedGame, lang: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val name = item.game.name(lang)
    Surface(Modifier.fillMaxWidth().fillMaxHeight(), shape = RoundedCornerShape(if (wide) 28.dp else 24.dp), color = c.surfaceContainerLow, contentColor = c.onSurface) {
        Row(
            Modifier.padding(start = if (wide) 22.dp else 16.dp, end = if (wide) 14.dp else 6.dp, top = if (wide) 18.dp else 10.dp, bottom = if (wide) 18.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 4.dp),
        ) {
            Column(
                Modifier.weight(1f).then(
                    if (wide) Modifier else Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { Router.navigate(Routes.game(item.game.id)) }.handCursor()
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(name, style = if (wide) SsdTheme.type.headlineSmall else SsdTheme.type.titleLarge.copy(fontSize = 19.sp, fontWeight = SsdTheme.type.labelLarge.fontWeight), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(strings.packedDescription(item), style = SsdTheme.type.bodyMedium.copy(fontSize = if (wide) 15.sp else 14.sp), color = c.onSurfaceVariant)
            }
            if (wide) {
                SsdIconButton(Icons.Book, strings.openPage(name), { Router.navigate(Routes.game(item.game.id)) }, container = c.surfaceContainerHigh, content = c.onSurface, size = 44.dp, iconSize = 20.dp)
            }
            SsdIconButton(Icons.Edit, strings.editConfiguration(name), onEdit, container = c.surfaceContainerHigh, content = c.onSurface, size = 44.dp, iconSize = 20.dp)
            SsdIconButton(Icons.Close, strings.removeFromPack(name), onRemove, container = Color.Transparent, content = c.onSurfaceVariant, size = 44.dp, iconSize = 20.dp)
        }
    }
}

@Composable
private fun AddGameDialog(available: List<Game>, lang: String, onAdd: (PackedGame) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    var chosen by remember { mutableStateOf<PackedGame?>(null) }
    var open by remember { mutableStateOf(false) }
    SsdDialog(onDismiss, strings.addGameToPack, maxWidth = 420.dp) {
        Text(strings.addGameToPack, style = SsdTheme.type.headlineMedium)
        Box {
            Surface(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().height(56.dp).handCursor().semantics { contentDescription = strings.game },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                contentColor = c.onSurface,
                border = BorderStroke(if (open) 2.dp else 1.dp, if (open) c.primaryText else c.outline),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chosen?.game?.name(lang) ?: "",
                        Modifier.weight(1f),
                        style = SsdTheme.type.bodyLarge.copy(fontSize = 16.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(if (open) Icons.ChevronUp else Icons.ChevronDown, null, Modifier.size(20.dp))
                }
            }
            Text(
                strings.game,
                Modifier.offset(x = 12.dp, y = (-9).dp).background(c.surfaceContainerLow).padding(horizontal = 4.dp),
                style = SsdTheme.type.bodySmall.copy(fontWeight = SsdTheme.type.labelMedium.fontWeight),
                color = if (open) c.primaryText else c.onSurfaceVariant,
            )
            SsdMenu(open, { open = false }, width = 340.dp, maxHeight = 360.dp) {
                available.sortedBy { normalizeForSearch(it.name(lang)) }.forEach { game ->
                    SsdMenuItem(game.name(lang), {
                        chosen = PackedGame.full(game)
                        open = false
                    }, selected = if (chosen?.game?.id == game.id) true else null, height = 44.dp)
                }
            }
        }
        chosen?.let { packed ->
            GameConfiguration(packed.game, packed.players, packed.variants, onChange = { players, variants ->
                chosen = packed.copy(players = players, variants = variants)
            })
        }
        DialogActions {
            SsdButton(strings.cancel, onDismiss, kind = ButtonKind.Text, height = 44.dp)
            // Adding is impossible as long as no game is chosen.
            SsdButton(strings.addToPack, { chosen?.let(onAdd) }, enabled = chosen != null, height = 44.dp)
        }
    }
}

@Composable
private fun EditGameDialog(packed: PackedGame, lang: String, onEdit: (PackedGame) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    var edited by remember(packed) { mutableStateOf(packed) }
    SsdDialog(onDismiss, packed.game.name(lang), maxWidth = 420.dp) {
        Text(packed.game.name(lang), style = SsdTheme.type.displaySmall.copy(fontSize = 36.sp, lineHeight = 40.sp))
        GameConfiguration(edited.game, edited.players, edited.variants, onChange = { players, variants ->
            edited = edited.copy(players = players, variants = variants)
        })
        DialogActions {
            SsdButton(strings.cancel, onDismiss, kind = ButtonKind.Text, height = 44.dp)
            SsdButton(strings.edit, { onEdit(edited) }, height = 44.dp)
        }
    }
}

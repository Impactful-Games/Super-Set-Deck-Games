package games.supersetdeck.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.supersetdeck.data.Lists
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.model.Game
import games.supersetdeck.model.Pack
import games.supersetdeck.model.PackedGame
import games.supersetdeck.navigation.Routes
import games.supersetdeck.platform.canShareNatively
import games.supersetdeck.platform.copyToClipboard
import games.supersetdeck.platform.shareNatively
import games.supersetdeck.ui.components.ButtonKind
import games.supersetdeck.ui.components.DeckView
import games.supersetdeck.ui.components.DialogFrame
import games.supersetdeck.ui.components.dialogSurface
import games.supersetdeck.ui.components.GameConfiguration
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.OverlayMarker
import games.supersetdeck.ui.components.ScrollingColumn
import games.supersetdeck.ui.components.Snackbars
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.SsdDialog
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.SsdMenu
import games.supersetdeck.ui.components.SsdMenuDivider
import games.supersetdeck.ui.components.SsdMenuItem
import games.supersetdeck.ui.components.TextFieldDialog
import games.supersetdeck.ui.components.dashedBorder
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.listIcon
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.data.Settings
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch

/** Share by message (where the browser can), by QR code, or by copying the canonical game URL. */
@Composable
fun ShareButton(game: Game) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf(false) }
    val url = Routes.shareUrl(game.id)
    val name = game.name(Settings.lang.code)
    val native = remember { canShareNatively() }

    Box {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                SsdButton(
                    strings.share,
                    onClick = { if (native) shareNatively(name, name, url) else open = true },
                    kind = ButtonKind.Tonal,
                    icon = Icons.Share,
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 18.dp),
                    textStyle = SsdTheme.type.labelMedium.copy(fontSize = 15.sp),
                )
                SsdIconButton(
                    if (open) Icons.ChevronUp else Icons.ChevronDown, strings.shareOptions, { open = !open },
                    container = c.primary, content = c.onPrimary, iconSize = 20.dp,
                )
            }
        } else {
            SsdIconButton(Icons.Share, strings.share, { open = true }, size = 44.dp, iconSize = 20.dp)
        }
        SsdMenu(open, { open = false }, width = 300.dp) {
            if (native) {
                SsdMenuItem(strings.shareByMessage, {
                    open = false
                    shareNatively(name, name, url)
                }, icon = Icons.Message, height = 52.dp)
            }
            SsdMenuItem(strings.shareByQrCode, {
                open = false
                qr = true
            }, icon = Icons.QrCode, height = 52.dp)
            SsdMenuItem(strings.copyGameLink, {
                open = false
                scope.launch {
                    if (copyToClipboard(url)) Snackbars.show(strings.linkCopied)
                    else Snackbars.show(strings.linkNotCopied, error = true)
                }
            }, icon = Icons.Link, height = 52.dp)
        }
    }

    if (qr) {
        SsdDialog({ qr = false }, strings.shareByQrCodeOf(name), maxWidth = 340.dp) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    name,
                    Modifier.align(Alignment.Center).padding(horizontal = 48.dp).semantics { heading() },
                    style = SsdTheme.type.headlineLarge.copy(fontSize = 36.sp, lineHeight = 40.sp, fontWeight = SsdTheme.type.displayLarge.fontWeight),
                    textAlign = TextAlign.Center,
                )
                SsdIconButton(Icons.Close, strings.close, { qr = false }, Modifier.align(Alignment.TopEnd), container = c.surfaceContainerHigh, content = c.onSurface, size = 44.dp, iconSize = 20.dp)
            }
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(240.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(rememberQrCodePainter(url), contentDescription = url, modifier = Modifier.fillMaxSize())
            }
            Text(url.removePrefix("https://"), Modifier.fillMaxWidth(), style = SsdTheme.type.bodySmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Adds the game to the favourites or a list, or to a new list created on the way. */
@Composable
fun AddToListButton(game: Game, height: Dp) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val data = Lists.data ?: return
    val wide = LocalWindowLayout.current.wide
    var open by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            modifier = Modifier.height(height).handCursor().dashedBorder(1.dp, c.outline, 10.dp)
                .semantics { contentDescription = strings.addToList },
            shape = RoundedCornerShape(10.dp),
            color = if (open) c.secondaryContainer else Color.Transparent,
            contentColor = if (open) c.onSecondaryContainer else c.primaryText,
        ) {
            Row(
                Modifier.padding(start = if (wide) 10.dp else 8.dp, end = if (wide) 14.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Plus, null, Modifier.size(if (wide) 18.dp else 16.dp))
                if (wide) Text(strings.addToList, style = SsdTheme.type.labelLarge.copy(fontSize = 14.sp))
            }
        }
        SsdMenu(open, { open = false }, width = 280.dp) {
            data.lists.forEach { list ->
                val inList = game.id in data.gamesOf(list.id)
                val (icon, tint) = listIcon(list)
                SsdMenuItem(
                    list.displayName(strings),
                    onClick = {
                        Lists.addGame(list.id, game.id)
                        open = false
                    },
                    icon = icon,
                    iconTint = tint,
                    enabled = !inList,
                    height = 44.dp,
                    trailing = if (inList) {
                        { Icon(Icons.Check, strings.alreadyInList, Modifier.size(18.dp), tint = c.primaryText) }
                    } else null,
                )
            }
            SsdMenuDivider()
            SsdMenuItem(strings.newList, {
                open = false
                creating = true
            }, icon = Icons.Plus, emphasis = true, height = 44.dp)
        }
    }
    if (creating) {
        TextFieldDialog(
            title = strings.newGameList,
            label = strings.listName,
            initial = "",
            confirmLabel = strings.create,
            onConfirm = { name ->
                val id = Lists.create(name)
                Lists.addGame(id, game.id)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

/** The cards a game needs, for its configuration (every player count and variant at first). */
@Composable
fun CardsNeededDialog(game: Game, lang: String, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    var packed by remember(game.id) { mutableStateOf(PackedGame.full(game)) }
    val pack = remember(packed) { Pack(listOf(packed)) }
    val name = game.name(lang)
    DialogFrame(onDismiss) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(if (LocalWindowLayout.current.compact) 8.dp else 24.dp), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.widthIn(max = 960.dp).fillMaxWidth().heightIn(max = maxHeight).dialogSurface().semantics { paneTitle = strings.cardsNeededFor(name) },
                shape = RoundedCornerShape(36.dp),
                color = c.surface,
                contentColor = c.onSurface,
                shadowElevation = 16.dp,
            ) {
                ScrollingColumn(
                    contentPadding = PaddingValues(28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    scrollbarPadding = PaddingValues(vertical = 32.dp, horizontal = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(c.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Games, null, Modifier.size(28.dp), tint = c.primaryText)
                        }
                        Text(
                            name,
                            Modifier.weight(1f).semantics { heading() },
                            style = SsdTheme.type.displayMedium.copy(fontSize = if (LocalWindowLayout.current.compact) 32.sp else 48.sp, lineHeight = if (LocalWindowLayout.current.compact) 34.sp else 48.sp),
                        )
                        SsdIconButton(Icons.Close, strings.close, onDismiss, container = c.surfaceContainerHigh, content = c.onSurface)
                    }
                    Surface(shape = RoundedCornerShape(24.dp), color = c.surfaceContainerLow, border = BorderStroke(0.dp, Color.Transparent)) {
                        GameConfiguration(
                            game, packed.players, packed.variants,
                            onChange = { players, variants -> packed = packed.copy(players = players, variants = variants) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            inline = true,
                        )
                    }
                    DeckView(pack, showCounts = false)
                }
            }
        }
    }
}

package games.supersetdeck.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Documents
import games.supersetdeck.data.Games
import games.supersetdeck.data.GamesState
import games.supersetdeck.data.Lists
import games.supersetdeck.data.Settings
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.i18n.joinedWithAnd
import games.supersetdeck.model.Game
import games.supersetdeck.model.abbreviatedText
import games.supersetdeck.navigation.GameTab
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Routes
import games.supersetdeck.navigation.Screen
import games.supersetdeck.platform.formatOneDecimal
import games.supersetdeck.platform.openInNewTab
import games.supersetdeck.ui.components.ConnectedToggle
import games.supersetdeck.ui.components.ConnectedRow
import games.supersetdeck.ui.components.ErrorCard
import games.supersetdeck.ui.components.HtmlDocumentView
import games.supersetdeck.ui.components.LinkChip
import games.supersetdeck.ui.components.ListChip
import games.supersetdeck.ui.components.LoadingArea
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.RemoteImage
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.StateMessage
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.layout.PreferencesButton
import games.supersetdeck.ui.screens.BackButton
import games.supersetdeck.ui.screens.ScreenColumn
import games.supersetdeck.ui.screens.openAppLink
import games.supersetdeck.ui.screens.showList
import games.supersetdeck.ui.screens.showType
import games.supersetdeck.ui.screens.suitCycle
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.model.Suit

@Composable
fun GameScreen(screen: Screen.GamePage) {
    val strings = LocalStrings.current
    val state = Games.state
    val page = rememberPageScroll()
    val back = { Router.back(fallback = Routes.catalogue(screen.filters)) }

    if (state !is GamesState.Loaded) {
        ScreenColumn(page) {
            TopBar(null, back)
            LoadingArea()
        }
        return
    }
    val game = state[screen.id]
    if (game == null) {
        ScreenColumn(page) {
            TopBar(null, back)
            StateMessage("Ø", strings.gameNotFound)
        }
        return
    }

    val layout = LocalWindowLayout.current
    val lang = Settings.lang.code
    var cardsNeeded by remember { mutableStateOf(false) }
    var gallery by remember { mutableStateOf<Int?>(null) }
    val tab = if (game.referenceCount == 0) GameTab.Rules else screen.tab

    fun setParam(name: String, value: String?) {
        val params = Router.location.params.filter { it.first != name } + listOfNotNull(value?.let { name to it })
        Router.replaceParams(params)
    }

    ScreenColumn(page, top = if (layout.wide) 28.dp else 12.dp, spacing = if (layout.wide) 28.dp else 14.dp) {
        TopBar(game, back)
        if (layout.wide) WideHeader(game, lang) else NarrowHeader(game, lang)

        if (game.referenceCount > 0) {
            Tabs(tab, game.referenceCount, Modifier.padding(top = if (layout.wide) 20.dp else 0.dp)) {
                setParam("page", if (it == GameTab.References) "references" else null)
            }
        }

        when (tab) {
            GameTab.Rules -> Rules(
                game = game,
                lang = lang,
                section = screen.section,
                page = page,
                onSection = { setParam("section", it) },
                onShowCards = { cardsNeeded = true },
            )
            GameTab.References -> References(game) { gallery = it }
        }
    }

    if (cardsNeeded) CardsNeededDialog(game, lang) { cardsNeeded = false }
    gallery?.let { start -> ReferenceGallery(game, lang, start) { gallery = null } }
}

@Composable
private fun TopBar(game: Game?, back: () -> Unit) {
    val strings = LocalStrings.current
    val wide = LocalWindowLayout.current.wide
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BackButton(strings.backToGames, back, size = if (wide) 48.dp else 44.dp)
        Box(Modifier.weight(1f))
        if (!wide) PreferencesButton(compact = true)
        if (game != null) ShareButton(game)
    }
}

/** "A game by A, B & C, adapted by X." or "A traditional French game." */
@Composable
private fun credits(game: Game): String? {
    val strings = LocalStrings.current
    val designers = game.designers
    val text = when {
        designers.authors != null -> buildString {
            append(strings.gameBy).append(' ').append(designers.authors.joinedWithAnd())
            designers.adaptedBy?.let { append(", ").append(strings.adaptedBy).append(' ').append(it.joinedWithAnd()) }
        }
        designers.tradition != null -> strings.traditional(strings.tradition(designers.tradition))
        designers.adaptedBy != null -> strings.adaptedBy.replaceFirstChar { it.uppercase() } + " " + designers.adaptedBy.joinedWithAnd()
        else -> return null
    }
    return "$text."
}

/** The other BGG names: all of them but the displayed one, sorted, comma separated. */
private fun otherNames(game: Game, lang: String): String =
    game.bgg.names.filter { it != game.name(lang) }.distinct().sorted().joinToString(", ")

@Composable
private fun WideHeader(game: Game, lang: String) {
    val c = SsdTheme.colors
    val suit = suitCycle[Games.all.indexOf(game).coerceAtLeast(0) % suitCycle.size]
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        maxItemsInEachRow = 2,
    ) {
        Box(Modifier.weight(1f).widthIn(min = 360.dp).padding(20.dp)) {
            ShapeBadge(Expressive.Cookie12, c.suit(Suit.Stars).container, 260.dp, Modifier.offset((-28).dp, (-32).dp), rotation = -8f)
            RemoteImage(
                game.imageUrl,
                contentDescription = game.name(lang),
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    .shadow(16.dp, RoundedCornerShape(36.dp), ambientColor = c.suit(suit).ink, spotColor = c.suit(suit).ink)
                    .clip(RoundedCornerShape(36.dp)).background(c.suit(suit).container),
                contentScale = ContentScale.Fit,
                placeholder = c.suit(suit).container,
            )
        }
        Column(Modifier.weight(1f).widthIn(min = 360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PlayersBadge(game, big = true)
            NameRow(game, lang, big = true)
            credits(game)?.let { Text(it, style = SsdTheme.type.bodyLarge.copy(fontSize = 19.sp)) }
            BggStats(game, lang, big = true)
            Chips(game, big = true)
        }
    }
}

@Composable
private fun NarrowHeader(game: Game, lang: String) {
    val c = SsdTheme.colors
    val suit = suitCycle[Games.all.indexOf(game).coerceAtLeast(0) % suitCycle.size]
    RemoteImage(
        game.imageUrl,
        contentDescription = game.name(lang),
        modifier = Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(28.dp)).background(c.suit(suit).container),
        contentScale = ContentScale.Fit,
        placeholder = c.suit(suit).container,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { NameRow(game, lang, big = false) }
        PlayersBadge(game, big = false)
    }
    credits(game)?.let { Text(it, style = SsdTheme.type.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)) }
    BggStats(game, lang, big = false)
    Chips(game, big = false)
}

@Composable
private fun PlayersBadge(game: Game, big: Boolean) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    Surface(shape = Pill, color = c.primaryContainer, contentColor = c.onPrimaryContainer) {
        Row(
            Modifier.height(if (big) 36.dp else 30.dp).padding(horizontal = if (big) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (big) Icon(Icons.Players, null, Modifier.size(18.dp))
            Text(strings.players(game.playerCount.abbreviatedText()), style = SsdTheme.type.labelLarge.copy(fontSize = if (big) 14.sp else 13.sp), maxLines = 1)
        }
    }
}

@Composable
private fun NameRow(game: Game, lang: String, big: Boolean) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val others = otherNames(game, lang)
    var showOthers by remember(game.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(
                game.name(lang),
                Modifier.semantics { heading() },
                style = if (big) SsdTheme.type.displayLarge.copy(fontSize = 96.sp, lineHeight = 92.sp, letterSpacing = (-3).sp)
                else SsdTheme.type.displayMedium.copy(lineHeight = 54.sp, letterSpacing = (-2).sp),
            )
            if (others.isNotEmpty()) {
                SsdIconButton(
                    if (showOthers) Icons.ChevronUp else Icons.ChevronDown,
                    strings.otherBggNames,
                    { showOthers = !showOthers },
                    container = if (showOthers) c.secondaryContainer else Color.Transparent,
                    content = if (showOthers) c.onSecondaryContainer else c.onSurfaceVariant,
                    size = if (big) 40.dp else 32.dp,
                    iconSize = if (big) 20.dp else 16.dp,
                    border = if (showOthers) null else BorderStroke(1.dp, c.outlineVariant),
                )
            }
        }
        if (showOthers && others.isNotEmpty()) {
            Surface(Modifier.widthIn(max = 420.dp), shape = RoundedCornerShape(16.dp), color = c.inverseSurface, contentColor = c.inverseOnSurface) {
                Text(others, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = SsdTheme.type.bodyMedium)
            }
        }
    }
}

@Composable
private fun BggStats(game: Game, lang: String, big: Boolean) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val rating = formatOneDecimal(game.bgg.rating, lang)
    val description = strings.bggDescription(game.name(lang), game.bgg.yearPublished, game.bgg.minAge, game.bgg.playingTime, rating)
    val open = { openInNewTab("https://boardgamegeek.com/boardgame/${game.bgg.bggId}") }
    val outer = if (big) 24.dp else 18.dp
    val inner = if (big) 8.dp else 6.dp
    @Composable
    fun Stat(icon: ImageVector?, text: String, first: Boolean, last: Boolean, modifier: Modifier = Modifier, tinted: Boolean = false) {
        val shape = RoundedCornerShape(
            topStart = if (first) outer else inner, bottomStart = if (first) outer else inner,
            topEnd = if (last) outer else inner, bottomEnd = if (last) outer else inner,
        )
        Box(modifier.clip(shape).background(if (tinted) c.tertiaryContainer else c.surfaceContainerHigh)) {
            Column(
                Modifier.padding(horizontal = if (big) 18.dp else 4.dp, vertical = 12.dp).then(if (big) Modifier else Modifier.fillMaxWidth()),
                horizontalAlignment = if (big) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (big && icon != null) Icon(icon, null, Modifier.size(18.dp), tint = if (tinted) c.tertiary else c.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!big && tinted) Text("★", style = SsdTheme.type.labelLarge.copy(fontSize = 18.sp), color = c.onTertiaryContainer)
                    Text(
                        text,
                        style = SsdTheme.type.headlineSmall.copy(fontSize = if (big) 24.sp else 18.sp, lineHeight = if (big) 30.sp else 22.sp, fontWeight = if (tinted) FontWeight.ExtraBold else FontWeight.Bold),
                        color = if (tinted) c.onTertiaryContainer else c.onSurface,
                    )
                    if (big && tinted) Icon(Icons.External, null, Modifier.size(14.dp), tint = c.onTertiaryContainer)
                }
            }
        }
    }
    Row(
        Modifier.then(if (big) Modifier else Modifier.fillMaxWidth()).clip(RoundedCornerShape(outer))
            .clickable(role = Role.Button, onClick = open).handCursor()
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(if (big) 6.dp else 4.dp),
    ) {
        val weight = if (big) Modifier else Modifier.weight(1f)
        Stat(Icons.Calendar, game.bgg.yearPublished.toString(), first = true, last = false, modifier = weight)
        Stat(Icons.Person, strings.minAge(game.bgg.minAge), first = false, last = false, modifier = weight)
        Stat(Icons.Clock, strings.minutes(game.bgg.playingTime), first = false, last = false, modifier = weight)
        Stat(Icons.Star, rating, first = false, last = true, modifier = weight, tinted = true)
    }
}

@Composable
private fun Chips(game: Game, big: Boolean) {
    val strings = LocalStrings.current
    val height: Dp = if (big) 36.dp else 32.dp
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        game.types.forEach { type ->
            LinkChip(strings.type(type), Icons.Tag, { showType(type) }, height = height, description = strings.filterBy(strings.type(type)))
        }
        if (!big) GameLists(game, height)
    }
    if (big) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GameLists(game, height)
        }
    }
}

/** The lists holding the game, and the "Add to list" menu (only when the lists are available). */
@Composable
private fun GameLists(game: Game, height: Dp) {
    val data = Lists.data ?: return
    data.listsOf(game.id).forEach { list ->
        ListChip(list, onClick = { showList(list) }, onRemove = { Lists.removeGame(list.id, game.id) }, height = height)
    }
    AddToListButton(game, height)
}

@Composable
private fun Tabs(tab: GameTab, references: Int, modifier: Modifier = Modifier, onTab: (GameTab) -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val height = if (wide) 56.dp else 48.dp
    ConnectedRow(modifier.then(if (wide) Modifier else Modifier.fillMaxWidth())) {
        val item = if (wide) Modifier else Modifier.weight(1f)
        ConnectedToggle(
            selected = tab == GameTab.Rules,
            onClick = { onTab(GameTab.Rules) },
            first = true, last = false, height = height, inner = 12.dp, role = Role.Tab, modifier = item,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (wide) 28.dp else 12.dp),
        ) {
            if (wide) Icon(Icons.Book, null, Modifier.size(22.dp))
            Text(strings.rules, style = SsdTheme.type.labelLarge.copy(fontSize = if (wide) 16.sp else 15.sp))
        }
        ConnectedToggle(
            selected = tab == GameTab.References,
            onClick = { onTab(GameTab.References) },
            first = false, last = true, height = height, inner = 12.dp, role = Role.Tab, modifier = item,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (wide) 24.dp else 12.dp),
        ) {
            if (wide) Icon(Icons.Image, null, Modifier.size(22.dp))
            Text(strings.references, style = SsdTheme.type.labelLarge.copy(fontSize = if (wide) 16.sp else 15.sp))
            Box(
                Modifier.size(22.dp).clip(Pill).background(if (tab == GameTab.References) c.onPrimary.copy(alpha = 0.16f) else c.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(references.toString(), style = SsdTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * The rule in the current language (the English one if it does not exist in that language), shown
 * as HTML. Its links set the `section` parameter, which brings the section into view.
 */
@Composable
private fun Rules(
    game: Game,
    lang: String,
    section: String?,
    page: games.supersetdeck.ui.components.PageScroll,
    onSection: (String) -> Unit,
    onShowCards: () -> Unit,
) {
    val strings = LocalStrings.current
    val wide = LocalWindowLayout.current.wide
    val result by produceState<Result<String>?>(
        (Documents.cached(game.ruleUrl(lang)) ?: if (lang !in game.names) Documents.cached(game.ruleUrl("en")) else null)?.let { Result.success(it) },
        game.id, lang,
    ) {
        value = runCatching {
            if (lang in game.names) {
                try {
                    Documents.load(game.ruleUrl(lang))
                } catch (_: Throwable) {
                    Documents.load(game.ruleUrl("en"))
                }
            } else Documents.load(game.ruleUrl("en"))
        }
    }
    when (val r = result) {
        null -> LoadingArea()
        else -> r.fold(
            onSuccess = { html ->
                HtmlDocumentView(
                    html = html,
                    page = page,
                    wide = wide,
                    documentKey = "games/${game.id}",
                    activeSection = section,
                    scrollTo = section,
                    onSection = onSection,
                    onAppLink = ::openAppLink,
                    onShowCards = onShowCards,
                )
            },
            onFailure = { ErrorCard(strings.ruleError) },
        )
    }
}

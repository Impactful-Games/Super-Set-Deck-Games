package games.supersetdeck.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Games
import games.supersetdeck.data.GamesState
import games.supersetdeck.data.Lists
import games.supersetdeck.data.ListsData
import games.supersetdeck.data.Settings
import games.supersetdeck.data.UserList
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.i18n.Strings
import games.supersetdeck.model.Game
import games.supersetdeck.model.abbreviatedText
import games.supersetdeck.model.searched
import games.supersetdeck.navigation.CatalogueFilters
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Routes
import games.supersetdeck.navigation.SortOrder
import games.supersetdeck.platform.normalizeForSearch
import games.supersetdeck.ui.components.AdaptiveGrid
import games.supersetdeck.ui.components.LoadingIndicatorBadge
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.OverlayMarker
import games.supersetdeck.ui.components.RemoteImage
import games.supersetdeck.ui.components.ScrollingColumn
import games.supersetdeck.ui.components.ScrollingRow
import games.supersetdeck.ui.components.SelectableChip
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.SsdMenu
import games.supersetdeck.ui.components.SsdMenuDivider
import games.supersetdeck.ui.components.SsdMenuItem
import games.supersetdeck.ui.components.StateMessage
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.components.listIcon
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.layout.Logo
import games.supersetdeck.ui.layout.Navigation
import games.supersetdeck.ui.layout.PoweredByBgg
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.model.Suit

/** The catalogue after every filter, and what the filters offer. */
private class Catalogue(all: List<Game>, filters: CatalogueFilters, lists: ListsData?, lang: String) {
    /** When a list is selected, only its games (none if it does not exist); without lists, no effect. */
    private val base = if (filters.listId != null && lists != null) {
        if (filters.listId == UserList.FAVOURITES || lists.list(filters.listId) != null) {
            val ids = lists.gamesOf(filters.listId)
            all.filter { it.id in ids }
        } else emptyList()
    } else all

    val games: List<Game> = base
        .filter { it.types.containsAll(filters.types) }
        .filter { it.playerCount.containsAll(filters.playerCounts) }
        .searched(filters.search)
        .sortedBy { normalizeForSearch(it.name(lang)) }
        .let { sorted ->
            // Kotlin's sorts are stable: equal ratings or years stay in alphabetical order.
            when (filters.sort) {
                SortOrder.Name -> sorted
                SortOrder.Rating -> sorted.sortedByDescending { it.bgg.rating }
                SortOrder.Year -> sorted.sortedByDescending { it.bgg.yearPublished }
            }
        }

    val allPlayerCounts: List<Int> = all.flatMap { it.playerCount }.toSortedSet().toList()
    val availablePlayerCounts: Set<Int> = games.flatMap { it.playerCount }.toSet()
    val allTypes: List<String> = all.flatMap { it.types }.toSet().sorted()
    val availableTypes: Set<String> = games.flatMap { it.types }.toSet()
}

private fun <T : Comparable<T>> Iterable<T>.toSortedSet() = toSet().sorted()

@Composable
fun CatalogueScreen(filters: CatalogueFilters) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val lang = Settings.lang.code
    val state = Games.state
    val lists = Lists.data
    SideEffect { Navigation.lastCatalogue = Router.location }

    val catalogue = (state as? GamesState.Loaded)?.let { Catalogue(it.games, filters, lists, lang) }
    fun update(transform: (CatalogueFilters) -> CatalogueFilters) {
        Router.replaceParams(transform(filters).toParams())
    }

    val page = rememberPageScroll()
    ScreenColumn(page, spacing = if (layout.wide) 32.dp else 14.dp) {
        if (layout.wide) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScreenTitle(strings.navGames)
                    if (catalogue != null) {
                        Surface(shape = Pill, color = SsdTheme.colors.primaryContainer, contentColor = SsdTheme.colors.onPrimaryContainer) {
                            Text(strings.games(catalogue.games.size), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = SsdTheme.type.labelLarge.copy(fontSize = 16.sp))
                        }
                    }
                }
                DecorativeSuits()
            }
        } else {
            ScreenHeader {
                Logo(40.dp)
                ScreenTitle(strings.navGames)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(if (layout.wide) 12.dp else 14.dp)) {
            if (layout.wide) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    SearchField(filters.search, { text -> update { it.copy(search = text) } }, Modifier.widthIn(min = 320.dp).weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortButton(filters.sort) { sort -> update { it.copy(sort = sort) } }
                        PlayersButton(filters, catalogue, ::update)
                        TypesButton(filters, catalogue, lists, ::update)
                    }
                }
            } else {
                SearchField(filters.search, { text -> update { it.copy(search = text) } }, Modifier.fillMaxWidth())
                ScrollingRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TypesButton(filters, catalogue, lists, ::update)
                    PlayersButton(filters, catalogue, ::update)
                    SortButton(filters.sort) { sort -> update { it.copy(sort = sort) } }
                }
                if (catalogue != null) {
                    Text(strings.games(catalogue.games.size), style = SsdTheme.type.labelMedium.copy(fontWeight = FontWeight.Bold), color = SsdTheme.colors.onSurfaceVariant)
                }
            }
        }

        when {
            catalogue == null -> LoadingGrid()
            catalogue.games.isEmpty() -> StateMessage("Ø", strings.games(0))
            else -> AdaptiveGrid(
                catalogue.games,
                minItemWidth = if (layout.wide) 196.dp else 150.dp,
                spacing = if (layout.wide) 20.dp else 12.dp,
                minColumns = if (layout.compact) 2 else 1,
                key = { it.id },
            ) { game ->
                GameCard(game, Games.all.indexOf(game), lang) { Router.navigate(Routes.game(game.id, filters)) }
            }
        }

        if (!layout.wide) {
            Spacer(Modifier.height(16.dp))
            PoweredByBgg()
        }
    }
}

@Composable
private fun DecorativeSuits() {
    val c = SsdTheme.colors
    Row(Modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ShapeBadge(Expressive.Cookie9, c.suit(Suit.Stars).container, 76.dp, rotation = -10f) {
            Text("★", style = SsdTheme.type.headlineLarge.copy(fontSize = 34.sp), color = c.suit(Suit.Stars).ink)
        }
        ShapeBadge(Expressive.Clover4, c.suit(Suit.Hearts).container, 64.dp, Modifier.offset(y = 10.dp), rotation = 12f) {
            Text("♥", style = SsdTheme.type.headlineMedium.copy(fontSize = 26.sp), color = c.suit(Suit.Hearts).ink)
        }
        ShapeBadge(Expressive.Sunny, c.suit(Suit.Clubs).container, 70.dp, rotation = -4f) {
            Text("♣", style = SsdTheme.type.headlineLarge.copy(fontSize = 30.sp), color = c.suit(Suit.Clubs).ink)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    Row(
        modifier.height(if (wide) 56.dp else 52.dp).clip(Pill).background(c.surfaceContainerHigh).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Search, null, Modifier.size(24.dp), tint = c.onSurfaceVariant)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(strings.searchPlaceholder, style = SsdTheme.type.bodyLarge.copy(fontSize = 16.sp), color = c.onSurfaceVariant, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = strings.searchLabel },
                singleLine = true,
                textStyle = SsdTheme.type.bodyLarge.copy(fontSize = 16.sp, color = c.onSurface),
                cursorBrush = SolidColor(c.primaryText),
            )
        }
        if (value.isNotEmpty()) {
            SsdIconButton(Icons.Close, strings.clearSearch, { onChange("") }, container = Color.Transparent, content = c.onSurfaceVariant, size = 40.dp, iconSize = 20.dp)
        }
    }
}

/** A filter button: outlined while inactive, tonal with a clear button once a value is selected. */
@Composable
private fun FilterButton(
    icon: ImageVector?,
    label: String,
    active: Boolean,
    open: Boolean,
    description: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    clearDescription: String = "",
) {
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val height = if (wide) 48.dp else 40.dp
    val shape = RoundedCornerShape(if (wide) 16.dp else 12.dp)
    Surface(
        modifier = Modifier.height(height),
        shape = shape,
        color = if (active || open) c.secondaryContainer else Color.Transparent,
        contentColor = if (active || open) c.onSecondaryContainer else c.onSurfaceVariant,
        border = if (active) null else BorderStroke(1.dp, if (open) c.outline else c.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.fillMaxHeight().clip(shape).clickable(role = Role.DropdownList, onClick = onClick).handCursor()
                    .semantics { contentDescription = "$description: $label" }
                    .padding(start = 14.dp, end = if (active && onClear != null) 6.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) Icon(icon, null, Modifier.size(if (wide) 20.dp else 18.dp))
                Text(label, style = SsdTheme.type.labelMedium.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!active || onClear == null) Icon(if (open) Icons.ChevronUp else Icons.ChevronDown, null, Modifier.size(18.dp))
            }
            if (active && onClear != null) {
                Box(
                    Modifier.width(40.dp).fillMaxHeight().clip(shape).clickable(role = Role.Button, onClick = onClear).handCursor()
                        .semantics { contentDescription = clearDescription },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Close, null, Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun SortButton(sort: SortOrder, onSort: (SortOrder) -> Unit) {
    val strings = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    fun label(order: SortOrder) = when (order) {
        SortOrder.Name -> strings.sortByName
        SortOrder.Rating -> strings.sortByRating
        SortOrder.Year -> strings.sortByYear
    }
    Box {
        FilterButton(Icons.Sort, label(sort), active = false, open = open, description = strings.sort, onClick = { open = true }, onClear = null)
        SsdMenu(open, { open = false }, width = 260.dp) {
            SortOrder.entries.forEach { order ->
                SsdMenuItem(label(order), onClick = {
                    onSort(order)
                    open = false
                }, selected = order == sort)
            }
        }
    }
}

@Composable
private fun PlayersButton(filters: CatalogueFilters, catalogue: Catalogue?, update: ((CatalogueFilters) -> CatalogueFilters) -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    var open by remember { mutableStateOf(false) }
    val selected = filters.playerCounts
    val label = if (selected.isNotEmpty()) strings.players(selected.abbreviatedText())
    else strings.players(catalogue?.availablePlayerCounts?.abbreviatedText() ?: "")
    Box {
        FilterButton(
            Icons.Players, label, active = selected.isNotEmpty(), open = open, description = strings.filterPlayers,
            onClick = { open = true }, onClear = { update { it.copy(playerCounts = emptySet()) } }, clearDescription = strings.clearPlayers,
        )
        SsdMenu(open, { open = false }, width = 300.dp) {
            val counts = catalogue?.allPlayerCounts.orEmpty()
            FlowRow(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5,
            ) {
                counts.forEach { count ->
                    val isSelected = count in selected
                    val enabled = isSelected || count in catalogue?.availablePlayerCounts.orEmpty()
                    Surface(
                        onClick = { update { it.copy(playerCounts = if (isSelected) it.playerCounts - count else it.playerCounts + count) } },
                        enabled = enabled,
                        modifier = Modifier.size(width = 48.dp, height = 48.dp).alpha(if (enabled) 1f else 0.38f).handCursor(enabled)
                            .semantics { this.selected = isSelected },
                        shape = Pill,
                        color = if (isSelected) c.primary else Color.Transparent,
                        contentColor = if (isSelected) c.onPrimary else c.onSurface,
                        border = if (isSelected) null else BorderStroke(1.dp, if (enabled) c.outline else c.outlineVariant),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(count.toString(), style = SsdTheme.type.labelLarge.copy(fontSize = 17.sp)) }
                    }
                }
            }
        }
    }
}

/** The label of the types & list filter (spec § 5.3.4). */
private fun Strings.typesLabel(filters: CatalogueFilters, catalogue: Catalogue?, lists: ListsData?): String {
    val listName = filters.listId?.let { id ->
        if (id == UserList.FAVOURITES) favourites else lists?.list(id)?.name ?: filters.listName ?: id
    }
    val selectedCount = filters.types.size + (if (listName != null) 1 else 0)
    return when {
        selectedCount == 1 -> listName ?: type(filters.types.first())
        selectedCount > 1 -> types(selectedCount)
        else -> {
            val available = catalogue?.availableTypes.orEmpty()
            if (available.size == 1) type(available.first()) else types(available.size)
        }
    }
}

@Composable
private fun TypesButton(
    filters: CatalogueFilters,
    catalogue: Catalogue?,
    lists: ListsData?,
    update: ((CatalogueFilters) -> CatalogueFilters) -> Unit,
) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    var open by remember { mutableStateOf(false) }
    val active = filters.types.isNotEmpty() || filters.listId != null
    val label = strings.typesLabel(filters, catalogue, lists)
    val clear = { update { it.copy(types = emptySet(), listId = null, listName = null) } }
    Box {
        FilterButton(
            Icons.Tag, label, active = active, open = open, description = strings.filterTypesAndList,
            onClick = { open = true }, onClear = clear, clearDescription = strings.clearTypesAndList,
        )
        if (layout.wide) {
            SsdMenu(open, { open = false }, width = 460.dp) {
                TypesMenuContent(filters, catalogue, lists, update, sheet = false)
            }
        } else if (open) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { open = false },
                sheetState = sheetState,
                containerColor = SsdTheme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ) {
                OverlayMarker()
                ScrollingColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    scrollbarPadding = PaddingValues(top = 4.dp, bottom = 20.dp, end = 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(40.dp).background(SsdTheme.colors.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Tag, null, Modifier.size(22.dp), tint = SsdTheme.colors.primaryText)
                        }
                        Text(label, Modifier.weight(1f), style = SsdTheme.type.headlineMedium.copy(fontSize = 26.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (active) {
                            SsdIconButton(Icons.Close, strings.clearTypesAndList, clear, container = Color.Transparent, content = SsdTheme.colors.onSurfaceVariant, size = 44.dp, border = BorderStroke(1.dp, SsdTheme.colors.outlineVariant))
                        }
                    }
                    TypesMenuContent(filters, catalogue, lists, update, sheet = true)
                    SsdButton(strings.games(catalogue?.games?.size ?: 0), { open = false }, Modifier.fillMaxWidth(), height = 56.dp)
                }
            }
        }
    }
}

@Composable
private fun TypesMenuContent(
    filters: CatalogueFilters,
    catalogue: Catalogue?,
    lists: ListsData?,
    update: ((CatalogueFilters) -> CatalogueFilters) -> Unit,
    sheet: Boolean,
) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    fun toggleList(list: UserList) {
        update {
            if (it.listId == list.id) it.copy(listId = null, listName = null)
            else it.copy(listId = list.id, listName = list.name)
        }
    }
    fun toggleType(type: String) {
        update { it.copy(types = if (type in it.types) it.types - type else it.types + type) }
    }
    val types = catalogue?.allTypes.orEmpty()
    if (sheet) {
        if (lists != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.navLists.uppercase(), style = SsdTheme.type.overline.copy(fontSize = 13.sp), color = c.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lists.lists.forEach { list ->
                        val selected = filters.listId == list.id
                        val (icon, tint) = listIcon(list)
                        SelectableChip(
                            list.displayName(strings), selected, { toggleList(list) },
                            enabled = selected || filters.listId == null, icon = icon, iconTint = tint,
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.typesSection.uppercase(), style = SsdTheme.type.overline.copy(fontSize = 13.sp), color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    val selected = type in filters.types
                    SelectableChip(strings.type(type), selected, { toggleType(type) }, enabled = selected || type in catalogue?.availableTypes.orEmpty())
                }
            }
        }
    } else {
        if (lists != null) {
            lists.lists.forEach { list ->
                val selected = filters.listId == list.id
                val (icon, tint) = listIcon(list)
                SsdMenuItem(
                    list.displayName(strings), { toggleList(list) },
                    icon = if (selected) null else icon, iconTint = tint, selected = if (selected) true else null,
                    enabled = selected || filters.listId == null, height = 44.dp,
                )
            }
            SsdMenuDivider()
        }
        FlowRow(maxItemsInEachRow = 2, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            types.forEach { type ->
                val selected = type in filters.types
                val enabled = selected || type in catalogue?.availableTypes.orEmpty()
                Row(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (selected) c.secondaryContainer else Color.Transparent)
                        .clickable(enabled = enabled, role = Role.Checkbox) { toggleType(type) }.handCursor(enabled)
                        .semantics { this.selected = selected }
                        .alpha(if (enabled) 1f else 0.38f)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                            .then(if (selected) Modifier.background(c.primary) else Modifier.background(Color.Transparent))
                            .then(if (selected) Modifier else Modifier.border(2.dp, c.onSurfaceVariant, RoundedCornerShape(6.dp))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Check, null, Modifier.size(14.dp), tint = c.onPrimary)
                    }
                    Text(
                        strings.type(type),
                        style = SsdTheme.type.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                        color = if (selected) c.onSecondaryContainer else c.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: Game, index: Int, lang: String, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    val suit = suitCycle[index.coerceAtLeast(0) % suitCycle.size]
    val players = strings.players(game.playerCount.abbreviatedText())
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().fillMaxHeight().handCursor().semantics { contentDescription = "${game.name(lang)}, $players" },
        shape = RoundedCornerShape(if (wide) 28.dp else 24.dp),
        color = c.surfaceContainerLow,
        contentColor = c.onSurface,
    ) {
        Column(
            Modifier.padding(start = if (wide) 10.dp else 8.dp, end = if (wide) 10.dp else 8.dp, top = if (wide) 10.dp else 8.dp, bottom = if (wide) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 8.dp),
        ) {
            RemoteImage(
                game.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if (wide) 20.dp else 18.dp)).background(c.suit(suit).container),
                contentScale = ContentScale.Fit,
                placeholder = c.suit(suit).container,
            ) {
                if (games.supersetdeck.data.Images.cached(game.thumbnailUrl) == null) {
                    Text(suit.symbol.ifEmpty { "‽" }, Modifier.rotate(if (index % 2 == 0) -10f else 8f).alpha(0.5f), style = SsdTheme.type.displayLarge.copy(fontSize = 72.sp, lineHeight = 72.sp), color = c.suit(suit).ink)
                }
            }
            Column(Modifier.padding(horizontal = if (wide) 6.dp else 4.dp), verticalArrangement = Arrangement.spacedBy(if (wide) 4.dp else 2.dp)) {
                Text(game.name(lang), style = if (wide) SsdTheme.type.titleMedium else SsdTheme.type.titleMedium.copy(fontSize = 15.sp, lineHeight = 20.sp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (wide) Icon(Icons.Players, null, Modifier.size(16.dp), tint = c.onSurfaceVariant)
                    Text(players, style = SsdTheme.type.bodyMedium.copy(fontSize = if (wide) 14.sp else 13.sp), color = c.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LoadingGrid() {
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LoadingIndicatorBadge(size = 52.dp)
            Box(Modifier.width(120.dp).height(14.dp).clip(Pill).background(c.surfaceContainerHighest))
        }
        AdaptiveGrid((1..6).toList(), minItemWidth = if (wide) 196.dp else 150.dp, spacing = 12.dp, minColumns = 2) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surfaceContainerLow).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(c.surfaceContainerHighest))
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(Pill).background(c.surfaceContainerHighest))
                Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(Pill).background(c.surfaceContainerHigh))
            }
        }
    }
}

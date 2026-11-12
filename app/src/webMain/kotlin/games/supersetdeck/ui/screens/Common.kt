package games.supersetdeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Games
import games.supersetdeck.data.Lists
import games.supersetdeck.data.UserList
import games.supersetdeck.i18n.Strings
import games.supersetdeck.navigation.CatalogueFilters
import games.supersetdeck.navigation.Location
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Routes
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.PageScroll
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.VerticalScrollbarFor
import games.supersetdeck.ui.components.pageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.layout.PreferencesButton
import games.supersetdeck.ui.theme.SsdTheme

/** A screen's scrolling column, with the design's margins for wide and narrow windows. */
@Composable
fun ScreenColumn(
    page: PageScroll,
    modifier: Modifier = Modifier,
    top: Dp? = null,
    bottom: Dp? = null,
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val wide = LocalWindowLayout.current.wide
    Box(modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().pageScroll(page).padding(
            start = if (wide) 40.dp else 16.dp,
            end = if (wide) 40.dp else 16.dp,
            top = top ?: if (wide) 40.dp else 20.dp,
            bottom = bottom ?: if (wide) 64.dp else 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
    VerticalScrollbarFor(page.state, PaddingValues(vertical = if (wide) 28.dp else 8.dp, horizontal = if (wide) 4.dp else 2.dp))
    }
}

/** The big title of a screen: display large on wide windows, smaller on phones. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
    val wide = LocalWindowLayout.current.wide
    Text(
        text,
        modifier.semantics { heading() },
        style = style ?: if (wide) SsdTheme.type.displayLarge else SsdTheme.type.displaySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A screen's header row: [leading], then [actions] at the end, the preferences button on phones. */
@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    leading: @Composable RowScope.() -> Unit,
) {
    val wide = LocalWindowLayout.current.wide
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 12.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 12.dp), content = leading)
        actions()
        if (!wide) PreferencesButton(compact = true)
    }
}

@Composable
fun BackButton(description: String, onClick: () -> Unit, size: Dp = 48.dp) {
    SsdIconButton(Icons.ArrowBack, description, onClick, size = size, iconSize = 24.dp)
}

/** Opens a link of a document (`/games/X` …) inside the app. */
fun openAppLink(path: String) {
    Router.navigate(Location.parse("#$path"))
}

/** Shows the catalogue filtered on a single list (spec § 6.3.1): every other filter is reset. */
fun showList(list: UserList) {
    Router.navigate(Routes.catalogue(CatalogueFilters(listId = list.id, listName = list.name)))
}

/** Shows the catalogue filtered on a single type. */
fun showType(type: String) {
    Router.navigate(Routes.catalogue(CatalogueFilters(types = setOf(type))))
}

/** A list's number of games, counting only the games of the catalogue. */
fun Strings.gameCount(listId: String): String {
    val ids = Lists.data?.gamesOf(listId).orEmpty()
    val known = if (Games.all.isEmpty()) ids.size else ids.count { Games[it] != null }
    return games(known)
}

/** Each suit's colors in turn, to tint the placeholders of the games. */
val suitCycle = listOf(
    games.supersetdeck.model.Suit.Spades, games.supersetdeck.model.Suit.Hearts, games.supersetdeck.model.Suit.Stars,
    games.supersetdeck.model.Suit.Florettes, games.supersetdeck.model.Suit.Clubs, games.supersetdeck.model.Suit.Wheels,
    games.supersetdeck.model.Suit.Diamonds, games.supersetdeck.model.Suit.Specials,
)

/** A decorative suit symbol, as on the design's placeholders. */
@Composable
fun SuitGlyph(index: Int, size: Dp, modifier: Modifier = Modifier, color: Color? = null) {
    val suit = suitCycle[index % suitCycle.size]
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(suit.symbol.ifEmpty { "‽" }, style = SsdTheme.type.displayLarge.copy(fontSize = size.value.sp, lineHeight = size.value.sp), color = color ?: SsdTheme.colors.suit(suit).ink)
    }
}

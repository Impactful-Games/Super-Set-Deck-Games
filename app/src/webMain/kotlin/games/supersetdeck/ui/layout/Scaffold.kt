package games.supersetdeck.ui.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.data.Settings
import games.supersetdeck.data.ThemeMode
import games.supersetdeck.i18n.Lang
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.navigation.Location
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Screen
import games.supersetdeck.platform.openInNewTab
import games.supersetdeck.ui.components.AppErrorHost
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.ScrollingColumn
import games.supersetdeck.ui.components.SnackbarHost
import games.supersetdeck.ui.components.SsdMenu
import games.supersetdeck.ui.components.SsdMenuDivider
import games.supersetdeck.ui.components.SsdMenuItem
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme

/** The five destinations of the main navigation. */
enum class Destination(val icon: ImageVector) {
    Games(Icons.Games), Lists(Icons.Bookmark), Packer(Icons.Luggage), About(Icons.Info), Changelog(Icons.History);

    companion object {
        fun of(screen: Screen): Destination? = when (screen) {
            is Screen.Catalogue, is Screen.GamePage -> Games
            Screen.MyLists -> Lists
            is Screen.Packer -> Packer
            Screen.About -> About
            Screen.Changelog -> Changelog
            Screen.Unknown -> null
        }
    }
}

/** Where each destination leads; the catalogue keeps its last filters. */
object Navigation {
    var lastCatalogue: Location = Location("/games")

    fun go(destination: Destination) {
        val target = when (destination) {
            Destination.Games -> lastCatalogue
            Destination.Lists -> Location("/lists")
            Destination.Packer -> Router.location.takeIf { it.path == "/packer" } ?: lastPacker
            Destination.About -> Location("/about")
            Destination.Changelog -> Location("/changelog")
        }
        if (target != Router.location) Router.navigate(target)
    }

    var lastPacker: Location = Location("/packer")
}

@Composable
private fun Destination.label(short: Boolean): String {
    val strings = LocalStrings.current
    return when (this) {
        Destination.Games -> strings.navGames
        Destination.Lists -> strings.navLists
        Destination.Packer -> if (short) strings.navPackerShort else strings.navPacker
        Destination.About -> strings.navAbout
        Destination.Changelog -> strings.navChangelog
    }
}

/**
 * The frame of every screen: a navigation rail and a rounded content panel on wide windows, the
 * content above a navigation bar on narrow ones. Global errors and messages show on top.
 */
@Composable
fun AppScaffold(screen: Screen, content: @Composable () -> Unit) {
    val layout = LocalWindowLayout.current
    val c = SsdTheme.colors
    val active = Destination.of(screen)
    Box(Modifier.fillMaxSize().background(if (layout.wide) c.surfaceContainer else c.surface)) {
        if (layout.wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(active)
                Surface(
                    Modifier.weight(1f).fillMaxHeight().padding(12.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = c.surface,
                    contentColor = c.onSurface,
                ) {
                    Box { content() }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                NavigationBar(active)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = if (layout.wide) 28.dp else 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppErrorHost()
            SnackbarHost()
        }
    }
}

/** The logo: a cookie with the interrobang. */
@Composable
fun Logo(size: androidx.compose.ui.unit.Dp) {
    ShapeBadge(Expressive.Cookie9, SsdTheme.colors.primary, size) {
        Text("‽", style = SsdTheme.type.displayLarge.copy(fontSize = (size.value * 0.54f).sp, lineHeight = (size.value * 0.6f).sp), color = SsdTheme.colors.onPrimary)
    }
}

@Composable
private fun NavigationRail(active: Destination?) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    ScrollingColumn(
        Modifier.width(248.dp).fillMaxHeight(),
        contentModifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        scrollbarPadding = PaddingValues(vertical = 16.dp, horizontal = 2.dp),
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).clickable { Navigation.go(Destination.Games) }.handCursor().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Logo(52.dp)
            Column {
                Text("Super-Set Deck", style = SsdTheme.type.labelSmall.copy(fontSize = 13.sp, letterSpacing = 0.4.sp), color = c.onSurfaceVariant)
                Text("Games‽", style = SsdTheme.type.headlineSmall.copy(fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Destination.entries.forEach { destination ->
                val selected = destination == active
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(Pill)
                        .background(if (selected) c.secondaryContainer else Color.Transparent)
                        .clickable(role = Role.Tab) { Navigation.go(destination) }.handCursor()
                        .semantics { this.selected = selected }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val color = if (selected) c.onSecondaryContainer else c.onSurfaceVariant
                    Icon(destination.icon, null, Modifier.size(24.dp), tint = color)
                    Text(destination.label(short = false), style = SsdTheme.type.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = color)
                }
            }
        }
        PreferencesButton(compact = false, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.weight(1f))
        PoweredByBgg()
    }
}

/** "Powered by BoardGameGeek", required by BoardGameGeek's terms of use. */
@Composable
fun PoweredByBgg(modifier: Modifier = Modifier) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    Surface(
        onClick = { openInNewTab("https://boardgamegeek.com") },
        modifier = modifier.fillMaxWidth().handCursor(),
        shape = RoundedCornerShape(20.dp),
        color = c.surfaceContainerHigh,
        contentColor = c.onSurface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(strings.poweredBy, style = SsdTheme.type.bodySmall, color = c.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BoardGameGeek", style = SsdTheme.type.labelLarge)
                Icon(Icons.External, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NavigationBar(active: Destination?) {
    val c = SsdTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surfaceContainer).navigationBarsPadding().heightIn(min = 80.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Destination.entries.forEach { destination ->
            val selected = destination == active
            Column(
                Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Tab) { Navigation.go(destination) }.handCursor()
                    .semantics { this.selected = selected },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.width(56.dp).height(32.dp).clip(Pill).background(if (selected) c.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(destination.icon, null, Modifier.size(24.dp), tint = if (selected) c.onSecondaryContainer else c.onSurfaceVariant)
                }
                Text(
                    destination.label(short = true),
                    style = SsdTheme.type.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                    color = if (selected) c.onSurface else c.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.System -> Icons.Contrast
        ThemeMode.Light -> Icons.Sun
        ThemeMode.Dark -> Icons.Moon
    }

/**
 * The language (by its native name) and theme choice, reachable from every screen: in the rail on
 * wide windows, in each screen's header on narrow ones ([compact]: the language code only).
 */
@Composable
fun PreferencesButton(compact: Boolean, modifier: Modifier = Modifier) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            modifier = Modifier.height(if (compact) 40.dp else 44.dp).handCursor()
                .semantics { contentDescription = "${strings.language}: ${strings.languageName}, ${strings.theme}" },
            shape = Pill,
            color = if (open) c.secondaryContainer else Color.Transparent,
            contentColor = if (open) c.onSecondaryContainer else c.onSurface,
            border = BorderStroke(1.dp, c.outline),
        ) {
            Row(
                Modifier.padding(start = if (compact) 10.dp else 12.dp, end = if (compact) 12.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
            ) {
                Icon(Icons.Globe, null, Modifier.size(if (compact) 18.dp else 20.dp))
                Text(
                    if (compact) Settings.lang.code.uppercase() else strings.languageName,
                    style = if (compact) SsdTheme.type.labelLarge.copy(fontSize = 13.sp) else SsdTheme.type.labelMedium,
                )
                Icon(Settings.themeMode.icon, null, Modifier.size(if (compact) 16.dp else 18.dp))
                if (!compact) Icon(if (open) Icons.ChevronUp else Icons.ChevronDown, null, Modifier.size(18.dp))
            }
        }
        SsdMenu(open, { open = false }, width = 240.dp) {
            MenuHeader(strings.language)
            Lang.entries.forEach { lang ->
                SsdMenuItem(lang.strings.languageName, onClick = {
                    Settings.selectLang(lang)
                    open = false
                }, selected = lang == Settings.lang)
            }
            SsdMenuDivider()
            MenuHeader(strings.theme)
            ThemeMode.entries.forEach { mode ->
                SsdMenuItem(
                    when (mode) {
                        ThemeMode.System -> strings.themeSystem
                        ThemeMode.Light -> strings.themeLight
                        ThemeMode.Dark -> strings.themeDark
                    },
                    onClick = {
                        Settings.selectTheme(mode)
                        open = false
                    },
                    selected = mode == Settings.themeMode,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.MenuHeader(text: String) {
    Text(
        text.uppercase(),
        Modifier.padding(start = 14.dp, top = 6.dp, bottom = 2.dp),
        style = SsdTheme.type.overline,
        color = SsdTheme.colors.onSurfaceVariant,
    )
}

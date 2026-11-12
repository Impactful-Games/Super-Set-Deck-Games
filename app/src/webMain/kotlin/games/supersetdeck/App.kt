package games.supersetdeck

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import games.supersetdeck.data.Settings
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.Screen
import games.supersetdeck.platform.setThemeColor
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.WindowLayout
import games.supersetdeck.ui.layout.AppScaffold
import games.supersetdeck.ui.screens.AboutScreen
import games.supersetdeck.ui.screens.CatalogueScreen
import games.supersetdeck.ui.screens.ChangelogScreen
import games.supersetdeck.ui.screens.UnknownScreen
import games.supersetdeck.ui.screens.game.GameScreen
import games.supersetdeck.ui.screens.lists.ListsScreen
import games.supersetdeck.ui.screens.packer.PackerScreen
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.ui.theme.toHex

@Composable
fun App() {
    SsdTheme(dark = Settings.isDark) {
        val colors = SsdTheme.colors
        SideEffect { setThemeColor(colors.surfaceContainer.toHex()) }
        CompositionLocalProvider(
            LocalStrings provides Settings.lang.strings,
            LocalContentColor provides colors.onSurface,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalWindowLayout provides WindowLayout(maxWidth, maxHeight)) {
                    val screen = Screen.of(Router.location)
                    AppScaffold(screen) {
                        // A new history entry is a new page: new state, scroll at the top or restored.
                        key(Router.entry) {
                            when (screen) {
                                is Screen.Catalogue -> CatalogueScreen(screen.filters)
                                is Screen.GamePage -> key(screen.id) { GameScreen(screen) }
                                Screen.MyLists -> ListsScreen()
                                is Screen.Packer -> PackerScreen(screen.location)
                                Screen.About -> AboutScreen()
                                Screen.Changelog -> ChangelogScreen()
                                Screen.Unknown -> UnknownScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

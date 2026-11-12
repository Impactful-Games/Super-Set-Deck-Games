package games.supersetdeck.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import games.supersetdeck.data.Documents
import games.supersetdeck.data.Settings
import games.supersetdeck.i18n.Lang
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.navigation.Routes
import games.supersetdeck.navigation.Router
import games.supersetdeck.ui.components.ErrorCard
import games.supersetdeck.ui.components.HtmlDocumentView
import games.supersetdeck.ui.components.LoadingArea
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.SsdButton
import games.supersetdeck.ui.components.StateMessage
import games.supersetdeck.ui.components.rememberPageScroll
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.layout.PoweredByBgg
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme

/** A documentary page: [url] in the current language (`about-fr.html`), or not translated. */
@Composable
private fun DocumentScreen(title: String, url: (Lang) -> String, mascot: Boolean) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val lang = Settings.lang
    val page = rememberPageScroll()
    val result by produceState(Documents.cached(url(lang))?.let { Result.success(it) }, lang) {
        value = runCatching {
            try {
                Documents.load(url(lang))
            } catch (error: Throwable) {
                if (lang == Lang.default) throw error else Documents.load(url(Lang.default))
            }
        }
    }
    ScreenColumn(page, spacing = if (layout.wide) 40.dp else 20.dp) {
        ScreenHeader {
            if (mascot) {
                ShapeBadge(Expressive.Cookie12, SsdTheme.colors.primaryContainer, if (layout.wide) 84.dp else 56.dp, rotation = -6f) {
                    Icon(Icons.Info, null, Modifier.size(if (layout.wide) 36.dp else 26.dp), tint = SsdTheme.colors.onPrimaryContainer)
                }
            }
            ScreenTitle(title)
        }
        when (val r = result) {
            null -> LoadingArea()
            else -> r.fold(
                onSuccess = { html ->
                    HtmlDocumentView(html, page, wide = layout.wide, documentKey = url(lang), onAppLink = ::openAppLink)
                },
                onFailure = { ErrorCard(strings.documentError) },
            )
        }
        if (!layout.wide && mascot) {
            Spacer(Modifier.height(8.dp))
            PoweredByBgg()
        }
    }
}

@Composable
fun AboutScreen() {
    DocumentScreen(LocalStrings.current.navAbout, { "docs/about-${it.code}.html" }, mascot = true)
}

@Composable
fun ChangelogScreen() {
    // The changelog is not translated.
    DocumentScreen(LocalStrings.current.navChangelog, { "docs/changelog.html" }, mascot = false)
}

@Composable
fun UnknownScreen() {
    val strings = LocalStrings.current
    val page = rememberPageScroll()
    ScreenColumn(page) {
        ScreenHeader {}
        StateMessage(
            glyph = "?!",
            title = strings.unknownPage,
            text = strings.howDidYouEndUpHere,
            action = { SsdButton(strings.seeGames, { Router.navigate(Routes.catalogue()) }, height = 52.dp) },
        )
    }
}

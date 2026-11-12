package games.supersetdeck

import androidx.compose.ui.window.ComposeViewport
import games.supersetdeck.data.Games
import games.supersetdeck.data.Lists
import games.supersetdeck.navigation.Router
import kotlinx.browser.document
import kotlinx.coroutines.MainScope

private fun removeSplash(): Unit = js("""
    (function () { var s = document.getElementById("ssd-splash"); if (s) s.remove(); })()
""")

fun main() {
    val scope = MainScope()
    Router.start()
    Games.load(scope)
    Lists.load(scope)
    ComposeViewport(document.body!!) {
        App()
    }
    removeSplash()
}

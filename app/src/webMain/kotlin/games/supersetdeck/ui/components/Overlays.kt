package games.supersetdeck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * HTML documents are laid over the canvas, so a Compose dialog, menu or sheet would be drawn under
 * them: documents hide while any overlay is shown.
 */
object Overlays {
    var count by mutableIntStateOf(0)
        private set

    val anyShown: Boolean get() = count > 0

    internal fun show() { ++count }
    internal fun hide() { --count }
}

/** Put inside the content of any overlay (dialog, menu, sheet, popup). */
@Composable
fun OverlayMarker() {
    DisposableEffect(Unit) {
        Overlays.show()
        onDispose { Overlays.hide() }
    }
}

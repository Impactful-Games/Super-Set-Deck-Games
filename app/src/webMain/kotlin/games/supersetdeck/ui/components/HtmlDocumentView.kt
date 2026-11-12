package games.supersetdeck.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import games.supersetdeck.model.Suit
import games.supersetdeck.navigation.Router
import games.supersetdeck.navigation.rememberScreenScrollState
import games.supersetdeck.platform.Disposer
import games.supersetdeck.platform.createDocumentHost
import games.supersetdeck.platform.installDocumentListeners
import games.supersetdeck.platform.observeDocumentHeight
import games.supersetdeck.platform.sectionOffset
import games.supersetdeck.platform.setActiveSection
import games.supersetdeck.platform.setDocumentHidden
import games.supersetdeck.platform.setDocumentHtml
import games.supersetdeck.platform.setDocumentStyle
import games.supersetdeck.platform.setDocumentWide
import games.supersetdeck.platform.setDocumentWidth
import games.supersetdeck.ui.theme.SsdColors
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.ui.theme.toHex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.HTMLElement

/** A screen's vertical scroll, with the position of its viewport in the window. */
class PageScroll(val state: ScrollState) {
    var viewportTop by mutableFloatStateOf(0f)
    var viewportHeight by mutableFloatStateOf(0f)
}

@Composable
fun rememberPageScroll(): PageScroll {
    val state = rememberScreenScrollState()
    return remember(state) { PageScroll(state) }
}

/** Scrolls vertically with [page], recording where its viewport is. */
fun Modifier.pageScroll(page: PageScroll): Modifier =
    onGloballyPositioned {
        page.viewportTop = it.positionInWindow().y
        page.viewportHeight = it.size.height.toFloat()
    }.verticalScroll(page.state)

/**
 * The last section scrolled to, per history entry: the same section is not scrolled to again, so
 * that coming back to an entry keeps its restored scroll position. A new visit is a new entry.
 */
private val lastScrolledSections = HashMap<Int, String>()

private class DocumentHost {
    var host: HTMLElement? = null
    var html: String? = null
    var wide: Boolean? = null
    var style: String? = null
    var hidden: Boolean? = null
    var section: String? = "\u0000"
    val disposers = ArrayList<Disposer>()
}

/**
 * Rules and documents, rendered by Asciidoctor at build time, shown as HTML over the canvas.
 *
 * It is as tall as its content and scrolls with the page: wheel and touch gestures over it are
 * forwarded to [page]. [scrollTo] brings a section into view when it changes.
 */
@Composable
fun HtmlDocumentView(
    html: String,
    page: PageScroll,
    wide: Boolean,
    modifier: Modifier = Modifier,
    documentKey: String = "",
    activeSection: String? = null,
    scrollTo: String? = null,
    onSection: (String) -> Unit = {},
    onAppLink: (String) -> Unit = {},
    onShowCards: () -> Unit = {},
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val colors = SsdTheme.colors
    val fling: FlingBehavior = ScrollableDefaults.flingBehavior()
    val holder = remember { DocumentHost() }
    val entry = Router.entry
    var height by remember { mutableDoubleStateOf(0.0) }
    var topInWindow by remember { mutableFloatStateOf(0f) }
    var measuredHtml by remember { mutableStateOf<String?>(null) }

    val onSectionState by rememberUpdatedState(onSection)
    val onAppLinkState by rememberUpdatedState(onAppLink)
    val onShowCardsState by rememberUpdatedState(onShowCards)
    val flingState by rememberUpdatedState(fling)

    val hidden = Overlays.anyShown
    val style = remember(colors) { colors.toCssVariables() }

    HtmlElementView(
        factory = {
            createDocumentHost().also { host ->
                holder.host = host
                holder.disposers += observeDocumentHeight(host) { height = it }
                holder.disposers += installDocumentListeners(
                    host,
                    onSection = { onSectionState(it) },
                    onAppLink = { onAppLinkState(it) },
                    onShowCards = { onShowCardsState() },
                    onScroll = { delta -> page.state.dispatchRawDelta((delta * density).toFloat()) },
                    onFling = { velocity ->
                        scope.launch { page.state.scroll { with(flingState) { performFling((velocity * density).toFloat()) } } }
                    },
                    onTouchStart = { scope.launch { page.state.stopScroll() } },
                    onFocus = { top, elementHeight ->
                        // Keyboard navigation: the focused element is brought into view by the page.
                        val margin = 24 * density
                        val elementTop = page.state.value + (topInWindow - page.viewportTop) + top.toFloat() * density
                        val elementBottom = elementTop + elementHeight.toFloat() * density
                        val viewTop = page.state.value
                        val viewBottom = viewTop + page.viewportHeight
                        val target = when {
                            elementTop < viewTop + margin -> elementTop - margin
                            elementBottom > viewBottom - margin -> elementBottom - page.viewportHeight + margin
                            else -> null
                        }
                        if (target != null) scope.launch { page.state.animateScrollTo(target.toInt().coerceIn(0, page.state.maxValue)) }
                    },
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height.coerceAtLeast(1.0).dp)
            .onSizeChanged { holder.host?.let { host -> setDocumentWidth(host, it.width / density.toDouble()) } }
            .onGloballyPositioned { topInWindow = it.positionInWindow().y },
        update = { host ->
            if (holder.html != html) {
                holder.html = html
                setDocumentHtml(host, html)
                holder.wide = null
                holder.section = "\u0000"
                measuredHtml = html
            }
            if (holder.wide != wide) {
                holder.wide = wide
                setDocumentWide(host, wide)
            }
            if (holder.style != style) {
                holder.style = style
                setDocumentStyle(host, style)
            }
            if (holder.hidden != hidden) {
                holder.hidden = hidden
                setDocumentHidden(host, hidden)
            }
            if (holder.section != activeSection) {
                holder.section = activeSection
                setActiveSection(host, activeSection)
            }
        },
        onRelease = {
            holder.disposers.forEach { it.dispose() }
            holder.disposers.clear()
            holder.host = null
        },
    )

    // Brings the requested section into view, once the document is in place and measured. The
    // section is only recorded as scrolled to once the scroll is done: this effect can be restarted
    // while it waits for the layout, and the new run must then do the scroll.
    LaunchedEffect(scrollTo, measuredHtml) {
        val section = scrollTo ?: return@LaunchedEffect
        val key = "$documentKey#$section"
        if (measuredHtml != html || key == lastScrolledSections[entry]) return@LaunchedEffect
        val host = holder.host ?: return@LaunchedEffect
        withTimeoutOrNull(3000) { snapshotFlow { height }.first { it > 1.0 } }
        if (sectionOffset(host, section) < 0) return@LaunchedEffect
        fun target(): Int {
            val documentTop = page.state.value + (topInWindow - page.viewportTop)
            return (documentTop + sectionOffset(host, section) * density - 16 * density).toInt().coerceAtLeast(0)
        }
        // The page is laid out with the document's full height a frame after it is measured.
        withTimeoutOrNull(3000) { snapshotFlow { page.state.maxValue }.first { it != Int.MAX_VALUE && it >= target() } }
        page.state.animateScrollTo(target().coerceAtMost(page.state.maxValue))
        lastScrolledSections[entry] = key
    }
}

/** The theme, as the CSS custom properties the documents' stylesheet uses. */
private fun SsdColors.toCssVariables(): String = buildString {
    fun v(name: String, color: androidx.compose.ui.graphics.Color) = append("--ssd-$name: ${color.toHex()}; ")
    v("primary", primary)
    v("on-primary", onPrimary)
    v("primary-text", primaryText)
    v("primary-container", primaryContainer)
    v("on-primary-container", onPrimaryContainer)
    v("secondary-container", secondaryContainer)
    v("on-secondary-container", onSecondaryContainer)
    v("tertiary", tertiary)
    v("tertiary-container", tertiaryContainer)
    v("on-tertiary-container", onTertiaryContainer)
    v("error", error)
    v("error-container", errorContainer)
    v("on-error-container", onErrorContainer)
    v("surface", surface)
    v("surface-low", surfaceContainerLow)
    v("surface-container", surfaceContainer)
    v("surface-high", surfaceContainerHigh)
    v("surface-highest", surfaceContainerHighest)
    v("on-surface", onSurface)
    v("on-surface-variant", onSurfaceVariant)
    v("outline", outline)
    v("outline-variant", outlineVariant)
    Suit.entries.forEach { suit ->
        val s = suit(suit)
        v("${suit.key}-ink", s.ink)
        v("${suit.key}-container", s.container)
        v("${suit.key}-on", s.onContainer)
    }
    append("color-scheme: ${if (isDark) "dark" else "light"};")
}

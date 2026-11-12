package games.supersetdeck.ui.screens.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.supersetdeck.data.rememberImage
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.model.Game
import games.supersetdeck.platform.exitFullscreen
import games.supersetdeck.platform.isFullscreen
import games.supersetdeck.platform.ScreenAwake
import games.supersetdeck.platform.onFullscreenChange
import games.supersetdeck.platform.onKeyDown
import games.supersetdeck.platform.requestFullscreen
import games.supersetdeck.ui.components.AdaptiveGrid
import games.supersetdeck.ui.components.ConnectedRow
import games.supersetdeck.ui.components.ConnectedToggle
import games.supersetdeck.ui.components.LocalWindowLayout
import games.supersetdeck.ui.components.OverlayMarker
import games.supersetdeck.ui.components.RemoteImage
import games.supersetdeck.ui.components.ScrollingRow
import games.supersetdeck.ui.components.SsdIconButton
import games.supersetdeck.ui.components.scrollbarStyle
import games.supersetdeck.ui.components.handCursor
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.GalleryColors
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.SsdTheme
import kotlinx.coroutines.launch

/** The game aids, in their order; choosing one opens it full screen. */
@Composable
fun References(game: Game, onOpen: (Int) -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val wide = LocalWindowLayout.current.wide
    AdaptiveGrid((0 until game.referenceCount).toList(), minItemWidth = if (wide) 380.dp else 280.dp, spacing = if (wide) 24.dp else 12.dp, key = { it }) { index ->
        Surface(
            onClick = {
                // Full screen and the screen wake (its video fallback) must start during the click itself.
                requestFullscreen()
                ScreenAwake.start()
                onOpen(index)
            },
            modifier = Modifier.fillMaxWidth().handCursor().semantics { contentDescription = strings.openFullScreen(index + 1) },
            shape = RoundedCornerShape(if (wide) 32.dp else 28.dp),
            color = c.surfaceContainerLow,
            contentColor = c.onSurface,
        ) {
            Box(Modifier.padding(12.dp)) {
                RemoteImage(
                    game.referenceUrl(index),
                    contentDescription = strings.reference(index + 1),
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(22.dp))
                        .background(GalleryColors.paper).border(1.dp, c.outlineVariant, RoundedCornerShape(22.dp)).padding(8.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = GalleryColors.paper,
                )
                // The references are portrait in a landscape box: the button stays clear of them, in the corner.
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp).clip(Pill).background(c.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Fullscreen, null, Modifier.size(20.dp), tint = c.onPrimary)
                }
            }
        }
    }
}

/**
 * The references full screen: native full screen when the browser allows it (leaving it closes
 * the viewer), screen kept awake, swipe between images, an overview to jump to any image, and in
 * landscape a dual view of two independently navigable references.
 */
@Composable
fun ReferenceGallery(game: Game, lang: String, start: Int, onClose: () -> Unit) {
    val strings = LocalStrings.current
    val layout = LocalWindowLayout.current
    val scope = rememberCoroutineScope()
    val count = game.referenceCount
    val onCloseState by rememberUpdatedState(onClose)

    var dualWanted by remember { mutableStateOf(true) }
    var overview by remember { mutableStateOf(true) }
    var activePane by remember { mutableIntStateOf(0) }
    val canDual = layout.landscape && count >= 2
    val dual = canDual && dualWanted
    val first = rememberPagerState(start) { count }
    val second = rememberPagerState((start + 1) % count) { count }

    fun pane(index: Int): PagerState = if (index == 1 && dual) second else first
    fun move(by: Int) {
        val pager = pane(activePane)
        scope.launch { pager.animateScrollToPage((pager.currentPage + by).mod(count)) }
    }

    DisposableEffect(Unit) {
        // Already started by the click that opened the gallery; started here otherwise.
        ScreenAwake.start()
        var wasFullscreen = isFullscreen()
        val fullscreen = onFullscreenChange {
            val now = isFullscreen()
            // Leaving native full screen (Escape, system gesture) ends the viewing.
            if (wasFullscreen && !now) onCloseState()
            wasFullscreen = now
        }
        val keys = onKeyDown { key ->
            when (key) {
                "ArrowLeft" -> move(-1)
                "ArrowRight" -> move(1)
                "Escape" -> onCloseState()
            }
        }
        onDispose {
            keys.dispose()
            fullscreen.dispose()
            ScreenAwake.stop()
            exitFullscreen()
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        OverlayMarker()
        Column(Modifier.fillMaxSize().background(GalleryColors.background)) {
            Row(
                Modifier.fillMaxWidth().height(if (layout.landscape) 56.dp else 64.dp).padding(start = 12.dp, end = 12.dp, top = if (layout.landscape) 0.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SsdIconButton(Icons.Close, strings.closeReferences, onClose, container = GalleryColors.control, content = GalleryColors.onBackground, size = 44.dp)
                Text(game.name(lang), Modifier.weight(1f), style = SsdTheme.type.labelLarge.copy(fontSize = 18.sp), color = GalleryColors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (count >= 2) {
                    ConnectedRow {
                        if (canDual) {
                            ConnectedToggle(
                                selected = dualWanted, onClick = { dualWanted = !dualWanted }, first = true, last = false,
                                selectedColors = GalleryColors.firstPane to SsdTheme.colors.onPrimaryContainer,
                                unselectedColors = GalleryColors.control to GalleryColors.onBackground,
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.semantics { contentDescription = strings.dualView },
                            ) { Icon(Icons.DualView, null, Modifier.size(22.dp)) }
                        }
                        ConnectedToggle(
                            selected = overview, onClick = { overview = !overview }, first = !canDual, last = true,
                            selectedColors = SsdTheme.colors.primary to SsdTheme.colors.onPrimary,
                            unselectedColors = GalleryColors.control to GalleryColors.onBackground,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.semantics { contentDescription = strings.referencesOverview },
                        ) { Icon(Icons.Grid, null, Modifier.size(22.dp)) }
                    }
                }
            }

            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GalleryPane(game, first, Modifier.weight(1f).fillMaxHeight(), showIndex = dual || !overview) { activePane = 0 }
                if (dual) GalleryPane(game, second, Modifier.weight(1f).fillMaxHeight(), showIndex = true) { activePane = 1 }
            }

            if (count >= 2 && overview) {
                Column(
                    Modifier.fillMaxWidth().padding(top = if (layout.landscape) 4.dp else 16.dp, bottom = if (layout.landscape) 10.dp else 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (!dual && !layout.landscape) IndexBadge(first.currentPage, count)
                    ScrollingRow(
                        contentModifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        style = scrollbarStyle(GalleryColors.onBackground, thickness = 4.dp),
                    ) {
                        repeat(count) { index ->
                            val ring = when {
                                first.currentPage == index -> GalleryColors.firstPane
                                dual && second.currentPage == index -> GalleryColors.secondPane
                                else -> null
                            }
                            Thumbnail(game, index, ring, if (layout.landscape) 64.dp to 44.dp else 64.dp to 84.dp) {
                                scope.launch { pane(activePane).animateScrollToPage(index) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexBadge(page: Int, count: Int, modifier: Modifier = Modifier) {
    Box(modifier.clip(Pill).background(GalleryColors.control).padding(horizontal = 12.dp, vertical = 3.dp)) {
        Text("${page + 1} / $count", style = SsdTheme.type.labelLarge.copy(fontSize = 13.sp), color = Color.White)
    }
}

@Composable
private fun GalleryPane(game: Game, pager: PagerState, modifier: Modifier, showIndex: Boolean, onInteract: () -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val count = game.referenceCount
    Box(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                    onInteract()
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            GalleryImage(game, page)
        }
        if (count >= 2) {
            SsdIconButton(
                Icons.ChevronLeft, strings.previousReference,
                { onInteract(); scope.launch { pager.animateScrollToPage((pager.currentPage - 1).mod(count)) } },
                Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                container = GalleryColors.overlay, content = Color.White, size = 44.dp,
            )
            SsdIconButton(
                Icons.ChevronRight, strings.nextReference,
                { onInteract(); scope.launch { pager.animateScrollToPage((pager.currentPage + 1).mod(count)) } },
                Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                container = GalleryColors.overlay, content = Color.White, size = 44.dp,
            )
            if (showIndex) IndexBadge(pager.currentPage, count, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
    }
}

/** A reference at its own aspect ratio, as large as fits, on paper. */
@Composable
private fun GalleryImage(game: Game, index: Int) {
    val strings = LocalStrings.current
    val image by rememberImage(game.referenceUrl(index))
    Box(Modifier.fillMaxSize().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = strings.reference(index + 1),
                modifier = Modifier.aspectRatio(bitmap.width.toFloat() / bitmap.height).clip(RoundedCornerShape(16.dp)).background(GalleryColors.paper),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun Thumbnail(game: Game, index: Int, ring: Color?, size: Pair<Dp, Dp>, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Box(
        Modifier.width(size.first).height(size.second)
            .then(if (ring != null) Modifier.border(BorderStroke(3.dp, ring), RoundedCornerShape(12.dp)) else Modifier)
            .padding(3.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(role = Role.Button, onClick = onClick).handCursor()
            .semantics {
                contentDescription = strings.showReference(index + 1)
                selected = ring != null
            }
            .alpha(if (ring != null) 1f else 0.7f),
    ) {
        RemoteImage(game.referenceUrl(index), null, Modifier.fillMaxSize().background(GalleryColors.paper), ContentScale.Fit, GalleryColors.paper)
    }
}

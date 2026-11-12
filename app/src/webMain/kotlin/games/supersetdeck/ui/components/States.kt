package games.supersetdeck.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.w3c.dom.HTMLElement
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.HtmlElementView
import games.supersetdeck.data.AppErrors
import games.supersetdeck.data.rememberImage
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.platform.createDocumentHost
import games.supersetdeck.platform.observeDocumentHeight
import games.supersetdeck.platform.setDocumentWidth
import games.supersetdeck.platform.setErrorCard
import games.supersetdeck.platform.setSnackbar
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.Expressive
import games.supersetdeck.ui.theme.ShapeBadge
import games.supersetdeck.ui.theme.SsdTheme
import games.supersetdeck.ui.theme.toHex
import kotlinx.coroutines.delay

/** The expressive loading indicator, in a primary container. */
@Composable
fun LoadingIndicatorBadge(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val strings = LocalStrings.current
    ContainedLoadingIndicator(
        modifier = modifier.size(size).semantics { contentDescription = strings.loading },
        containerColor = SsdTheme.colors.primaryContainer,
        indicatorColor = SsdTheme.colors.onPrimaryContainer,
    )
}

/** A loading indicator centered in a tall area. */
@Composable
fun LoadingArea(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { LoadingIndicatorBadge() }
}

/** An error message card; with [onDismiss], it can be closed. */
@Composable
fun ErrorCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Warning,
    onDismiss: (() -> Unit)? = null,
) {
    val c = SsdTheme.colors
    Surface(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        shape = RoundedCornerShape(24.dp),
        color = c.errorContainer,
        contentColor = c.onErrorContainer,
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = if (onDismiss != null) 8.dp else 20.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, null, Modifier.padding(top = 1.dp).size(22.dp))
            Text(text, Modifier.weight(1f).padding(vertical = 1.dp), style = SsdTheme.type.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp))
            if (onDismiss != null) {
                SsdIconButton(Icons.Close, LocalStrings.current.dismiss, onDismiss, container = Color.Transparent, content = c.onErrorContainer, size = 36.dp, iconSize = 18.dp)
            }
        }
    }
}

/** An expressive shape with a big glyph: empty results, unknown pages, missing games. */
@Composable
fun ShapeGlyph(
    glyph: String,
    shape: Expressive = Expressive.Sunny,
    size: Dp = 150.dp,
    container: Color = SsdTheme.colors.surfaceContainerHighest,
    content: Color = SsdTheme.colors.outline,
    rotation: Float = 10f,
    modifier: Modifier = Modifier,
) {
    ShapeBadge(shape, container, size, modifier, rotation) {
        Text(glyph, style = SsdTheme.type.displayLarge.copy(fontSize = (size.value * 0.42f).sp, lineHeight = (size.value * 0.42f).sp), color = content)
    }
}

/** A centered state: shape, title and optional text and action. */
@Composable
fun StateMessage(
    glyph: String,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShapeGlyph(glyph)
        Text(title, style = SsdTheme.type.headlineMedium.copy(fontWeight = SsdTheme.type.displayLarge.fontWeight), textAlign = TextAlign.Center)
        if (text != null) Text(text, style = SsdTheme.type.bodyLarge, color = SsdTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) action()
    }
}

/**
 * The global errors, one at a time, stacked to show that more are waiting. Drawn in HTML, laid over
 * the canvas after the documents: a Compose card would be hidden by them, or have to hide them.
 */
@Composable
fun AppErrorHost(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val current = AppErrors.queue.firstOrNull() ?: return
    val more = AppErrors.queue.size > 1
    val density = LocalDensity.current.density
    var height by remember { mutableDoubleStateOf(0.0) }
    var host by remember { mutableStateOf<HTMLElement?>(null) }
    val text = current.message(strings)
    HtmlElementView(
        factory = {
            createDocumentHost().also { element ->
                host = element
                observeDocumentHeight(element) { height = it }
            }
        },
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().height(height.coerceAtLeast(1.0).dp)
            .onSizeChanged { size -> host?.let { setDocumentWidth(it, size.width / density.toDouble()) } },
        update = {
            setErrorCard(
                host = it,
                iconSvg = Icons.ERROR_SVG,
                closeSvg = Icons.CLOSE_SVG,
                text = text,
                dismissLabel = strings.dismiss,
                more = more,
                background = c.errorContainer.toHex(),
                color = c.onErrorContainer.toHex(),
                onDismiss = { AppErrors.dismissCurrent() },
            )
        },
    )
}

/** The transient messages ("Link copied"). */
object Snackbars {
    data class Message(val text: String, val error: Boolean, val id: Long)

    var current: Message? by mutableStateOf(null)
        private set

    private var counter = 0L

    fun show(text: String, error: Boolean = false) {
        current = Message(text, error, ++counter)
    }

    internal fun clear(message: Message) {
        if (current == message) current = null
    }
}

/**
 * Shows [Snackbars.current] for a few seconds. It is drawn in HTML, laid over the canvas after the
 * documents, so that it stays visible above them.
 */
@Composable
fun SnackbarHost(modifier: Modifier = Modifier) {
    val message = Snackbars.current
    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            Snackbars.clear(message)
        }
    }
    val c = SsdTheme.colors
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        val shown = message ?: return@AnimatedVisibility
        HtmlElementView(
            factory = { createDocumentHost() },
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().height(52.dp),
            update = {
                setSnackbar(
                    element = it,
                    iconSvg = if (shown.error) Icons.ERROR_SVG else Icons.CHECK_SVG,
                    text = shown.text,
                    background = c.inverseSurface.toHex(),
                    color = c.inverseOnSurface.toHex(),
                    iconColor = (if (shown.error) c.errorContainer else c.inversePrimary).toHex(),
                )
            },
        )
    }
}

/** An image from the site, on a [placeholder] background while it loads. */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Color = SsdTheme.colors.surfaceContainerHigh,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val image by rememberImage(url)
    Box(modifier.background(if (image == null) placeholder else Color.Transparent), contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription, Modifier.matchParentSize(), contentScale = contentScale)
        } else {
            Spacer(Modifier.matchParentSize().semantics { if (contentDescription != null) this.contentDescription = contentDescription })
        }
        overlay()
    }
}

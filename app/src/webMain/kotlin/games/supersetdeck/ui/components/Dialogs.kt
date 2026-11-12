package games.supersetdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import games.supersetdeck.platform.onKeyDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.ui.theme.SsdTheme

/**
 * A full-window dialog: a click on the dimmed area around its content, or Escape, dismisses it.
 *
 * Escape is watched on the window rather than through Compose: the keyboard focus may be in an
 * HTML document (after its "Show cards" button, say), where Compose does not see the keys.
 */
@Composable
fun DialogFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        OverlayMarker()
        DisposableEffect(Unit) {
            val keys = onKeyDown { if (it == "Escape") dismiss() }
            onDispose { keys.dispose() }
        }
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { dismiss() } },
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Keeps the taps inside a dialog's card from reaching the dimmed area, which would dismiss it. */
fun Modifier.dialogSurface(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/**
 * The design's dialog card. On small screens it sits near the top, so that a text field stays
 * visible above the virtual keyboard.
 */
@Composable
fun SsdDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 400.dp,
    alignTop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogFrame(onDismiss) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
            val top = alignTop && maxHeight < 700.dp
            val available = maxHeight
            Box(Modifier.fillMaxSize(), contentAlignment = if (top) Alignment.TopCenter else Alignment.Center) {
                Surface(
                    modifier = modifier.widthIn(max = maxWidth).fillMaxWidth().padding(top = if (top) 24.dp else 0.dp)
                        .heightIn(max = available - if (top) 24.dp else 0.dp)
                        .dialogSurface()
                        .semantics { paneTitle = title },
                    shape = RoundedCornerShape(32.dp),
                    color = SsdTheme.colors.surfaceContainerLow,
                    contentColor = SsdTheme.colors.onSurface,
                    shadowElevation = 12.dp,
                ) {
                    ScrollingColumn(
                        contentPadding = PaddingValues(28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        scrollbarPadding = PaddingValues(vertical = 28.dp, horizontal = 6.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

/** The actions row, at the end of a dialog. */
@Composable
fun DialogActions(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

/** A confirmation: a tinted icon, a centered title, an explanation, cancel or confirm. */
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    textAlign: TextAlign = TextAlign.Start,
) {
    val c = SsdTheme.colors
    val strings = LocalStrings.current
    val focus = remember { FocusRequester() }
    SsdDialog(onDismiss, title, maxWidth = 400.dp) {
        Surface(Modifier.align(Alignment.CenterHorizontally).size(56.dp), shape = RoundedCornerShape(20.dp), color = c.errorContainer, contentColor = c.error) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(26.dp)) }
        }
        Text(title, Modifier.fillMaxWidth(), style = SsdTheme.type.headlineMedium, textAlign = TextAlign.Center)
        if (text.isNotEmpty()) {
            Text(text, Modifier.fillMaxWidth(), style = SsdTheme.type.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.onSurfaceVariant, textAlign = textAlign)
        }
        DialogActions {
            SsdButton(strings.cancel, onDismiss, kind = ButtonKind.Text, height = 44.dp)
            // The confirmation is the default choice.
            SsdButton(confirmLabel, onConfirm, kind = ButtonKind.Danger, height = 44.dp, modifier = Modifier.focusRequester(focus))
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** Asks for a non-empty text; Enter validates. */
@Composable
fun TextFieldDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    val valid = value.text.isNotBlank()
    fun confirm() {
        if (valid) onConfirm(value.text.trim())
    }
    SsdDialog(onDismiss, title, maxWidth = 400.dp, alignTop = true) {
        Text(title, style = SsdTheme.type.headlineMedium)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = SsdTheme.type.bodyLarge.copy(fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirm() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.primaryText,
                focusedLabelColor = c.primaryText,
                cursorColor = c.primaryText,
                unfocusedBorderColor = c.outline,
            ),
        )
        DialogActions {
            SsdButton(strings.cancel, onDismiss, kind = ButtonKind.Text, height = 44.dp)
            SsdButton(confirmLabel, { confirm() }, enabled = valid, height = 44.dp)
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

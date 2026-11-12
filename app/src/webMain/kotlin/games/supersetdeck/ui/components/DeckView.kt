package games.supersetdeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.i18n.Strings
import games.supersetdeck.model.Card
import games.supersetdeck.model.Deck
import games.supersetdeck.model.Pack
import games.supersetdeck.model.Suit
import games.supersetdeck.ui.theme.Pill
import games.supersetdeck.ui.theme.SsdTheme

/** A dashed rounded outline, which Compose's `border` cannot draw. */
fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

/** A card's label: its value, translated for the specials. */
fun Strings.cardValue(card: Card): String = if (card.suit == Suit.Specials) special(card.value) else card.value

/** The summary line of a suit: `2-10, J, Q, 3×K, A`. */
fun Strings.summary(pack: Pack, suit: Suit): String =
    pack.summary(suit).joinToString(", ") { group ->
        val label = if (suit == Suit.Specials) special(group.label) else group.label
        if (group.copies > 1) "${group.copies}×$label" else label
    }

/**
 * The pack view: a textual summary of the needed cards, then the whole deck, suit by suit, the
 * needed cards highlighted. Selecting a needed card tells which games use it; with [showCounts]
 * (the packer), each needed card also shows how many games of the pack use it.
 */
@Composable
fun DeckView(pack: Pack, showCounts: Boolean, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    var picked by remember(pack) { mutableStateOf<Card?>(null) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = c.surfaceContainer) {
            FlowRow(
                Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(pack.size.toString(), style = SsdTheme.type.displayLarge.copy(fontSize = 64.sp, lineHeight = 64.sp), color = c.primaryText)
                    Text(strings.cardsToPack, style = SsdTheme.type.labelMedium, color = c.onSurfaceVariant)
                }
                Column(Modifier.widthIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pack.usedSuits().forEach { suit ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(suit.symbol, Modifier.width(26.dp), style = SsdTheme.type.titleLarge.copy(fontSize = 22.sp, lineHeight = 24.sp), color = c.suit(suit).ink)
                            Text(strings.summary(pack, suit), style = SsdTheme.type.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp))
                        }
                    }
                }
            }
        }

        val selection = picked?.let { card -> pack.cards[card]?.let { card to it } }
        if (selection != null) {
            val (card, games) = selection
            Surface(shape = RoundedCornerShape(16.dp), color = c.inverseSurface, contentColor = c.inverseOnSurface) {
                Row(Modifier.padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.defaultMinSize(minWidth = 32.dp).height(32.dp).background(c.inverseOnSurface, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        val copies = Deck.copies(card.suit, card.value)
                        Text(
                            listOf(card.suit.symbol, strings.cardValue(card)).filter { it.isNotEmpty() }.joinToString(" ") + if (copies > 1) " #${card.copy}" else "",
                            style = SsdTheme.type.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = c.inverseSurface,
                        )
                    }
                    Text(
                        buildAnnotatedString {
                            append(strings.usedBy + " ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(games.map { it.name(strings.languageCode()) }.sorted().joinToString(", "))
                            }
                        },
                        style = SsdTheme.type.bodyMedium,
                    )
                }
            }
        } else if (pack.size > 0) {
            Text(strings.selectCardHint, style = SsdTheme.type.bodyMedium, color = c.onSurfaceVariant)
        }

        Suit.entries.forEach { suit ->
            SuitRow(suit, pack, showCounts, picked) { picked = it }
        }
    }
}

/** The language code matching these strings, for game names. */
fun Strings.languageCode(): String = games.supersetdeck.i18n.Lang.entries.first { it.strings === this }.code

@Composable
private fun SuitRow(suit: Suit, pack: Pack, showCounts: Boolean, picked: Card?, onPick: (Card) -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val colors = c.suit(suit)
    val cards = Deck.cards(suit)
    val needed = cards.count { it in pack.cards }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.semantics { heading() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (suit.symbol.isNotEmpty()) {
                Box(Modifier.size(32.dp).background(colors.container, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(suit.symbol, style = SsdTheme.type.titleMedium.copy(fontSize = 18.sp), color = colors.ink)
                }
            }
            Text(strings.suit(suit), style = SsdTheme.type.labelLarge)
            Text("$needed / ${cards.size}", style = SsdTheme.type.bodySmall.copy(fontSize = 13.sp), color = c.onSurfaceVariant)
            if (needed == 0) {
                Box(Modifier.dashedBorder(1.dp, c.outline, 999.dp).padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text(strings.notUsed, style = SsdTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.onSurfaceVariant)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.forEach { card ->
                CardChip(card, pack.cards[card]?.size ?: 0, pack.cards[card]?.map { it.name(strings.languageCode()) }?.sorted().orEmpty(), showCounts, card == picked) { onPick(card) }
            }
        }
    }
}

@Composable
private fun CardChip(card: Card, games: Int, gameNames: List<String>, showCount: Boolean, selected: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    val colors = c.suit(card.suit)
    val needed = games > 0
    val special = card.suit == Suit.Specials
    val multi = Deck.copies(card.suit, card.value) > 1
    val shape = RoundedCornerShape(9.dp)
    val description = strings.card(strings.suit(card.suit), strings.cardValue(card), if (multi) card.copy else null, gameNames)
    Box(
        Modifier
            .border(2.dp, if (selected) c.onSurface else Color.Transparent, RoundedCornerShape(13.dp))
            .padding(3.dp)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Box(
            Modifier
                .height(52.dp)
                .then(if (special) Modifier.defaultMinSize(minWidth = 84.dp) else Modifier.width(38.dp))
                .then(
                    if (needed) Modifier.background(colors.container, shape).border(2.dp, colors.ink, shape)
                    else Modifier.dashedBorder(1.5.dp, c.outlineVariant, 9.dp)
                )
                .then(if (needed) Modifier.clickable(role = Role.Button, onClick = onClick).handCursor() else Modifier)
                .padding(horizontal = if (special) 10.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    strings.cardValue(card),
                    style = SsdTheme.type.labelLarge.copy(fontSize = if (special) 13.sp else 16.sp, lineHeight = if (special) 15.sp else 18.sp),
                    color = if (needed) colors.onContainer else c.outline,
                )
                if (multi) {
                    Text("#${card.copy}", style = SsdTheme.type.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold), color = if (needed) colors.onContainer else c.outline)
                }
            }
        }
        if (needed && showCount) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-7).dp)
                    .defaultMinSize(minWidth = 18.dp).height(18.dp)
                    .background(c.surface, Pill).padding(2.dp)
                    .background(c.primary, Pill).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(games.toString(), style = SsdTheme.type.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold), color = c.onPrimary)
            }
        }
    }
}

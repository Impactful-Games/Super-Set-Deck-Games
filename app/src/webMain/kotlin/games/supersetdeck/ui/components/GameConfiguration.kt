package games.supersetdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import games.supersetdeck.i18n.LocalStrings
import games.supersetdeck.model.Game
import games.supersetdeck.model.abbreviatedText
import games.supersetdeck.ui.icons.Icons
import games.supersetdeck.ui.theme.SsdTheme

/**
 * A game's configuration, shared by the packer and the cards needed by a game: its player counts
 * (one at least) and, if it has some, its optional variants (any number).
 */
@Composable
fun GameConfiguration(
    game: Game,
    players: Set<Int>,
    variants: Set<String>,
    onChange: (players: Set<Int>, variants: Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
) {
    val strings = LocalStrings.current
    val c = SsdTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (inline) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Players, null, Modifier.size(20.dp), tint = c.primaryText)
                    Text(strings.players(players.abbreviatedText()), style = SsdTheme.type.labelLarge.copy(fontSize = SsdTheme.type.titleMedium.fontSize))
                }
                PlayerToggles(game, players) { onChange(it, variants) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.players(players.abbreviatedText()), style = SsdTheme.type.labelMedium.copy(fontWeight = SsdTheme.type.labelLarge.fontWeight))
                PlayerToggles(game, players) { onChange(it, variants) }
            }
        }
        val gameVariants = game.variants
        if (gameVariants.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.variants(variants.count { it in gameVariants }), style = SsdTheme.type.labelMedium.copy(fontWeight = SsdTheme.type.labelLarge.fontWeight))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gameVariants.forEach { variant ->
                        val selected = variant in variants
                        SelectableChip(variant, selected, onClick = {
                            onChange(players, if (selected) variants - variant else variants + variant)
                        })
                    }
                }
            }
        }
    }
}

/** The player counts of [game], as a connected group; the last selected one cannot be removed. */
@Composable
private fun PlayerToggles(game: Game, players: Set<Int>, onChange: (Set<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val counts = game.playerCount.sorted()
        counts.forEachIndexed { index, count ->
            val selected = count in players
            ConnectedToggle(
                selected = selected,
                onClick = {
                    if (!selected) onChange(players + count)
                    else if (players.size > 1) onChange(players - count)
                },
                first = index == 0,
                last = index == counts.lastIndex,
                contentPadding = PaddingValues(start = if (selected) 12.dp else 16.dp, end = 16.dp),
            ) {
                if (selected) Icon(Icons.Check, null, Modifier.size(16.dp))
                Text(count.toString(), style = SsdTheme.type.labelLarge.copy(fontSize = SsdTheme.type.titleMedium.fontSize))
            }
        }
    }
}

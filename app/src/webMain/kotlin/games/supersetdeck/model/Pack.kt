package games.supersetdeck.model

/** A game of a pack, with the player counts and variants it is played with. */
data class PackedGame(val game: Game, val players: Set<Int>, val variants: Set<String>) {

    /** Whether every player count and every variant is selected. */
    val isFull: Boolean get() = players.containsAll(game.playerCount) && variants.containsAll(game.variants)

    companion object {
        fun full(game: Game) = PackedGame(game, game.playerCount.toSet(), game.variants.toSet())
    }
}

/**
 * The minimal set of cards needed to play [games]: for each card required k times by a game
 * configuration, copies #1 to #k are needed. Copies are a maximum across games, never a sum.
 */
class Pack(val games: List<PackedGame>) {

    /** Every needed card, with the games using it. */
    val cards: Map<Card, Set<Game>> = buildMap<Card, MutableSet<Game>> {
        games.forEach { (game, players, variants) ->
            (variants + Game.BASE_VARIANT).forEach { variant ->
                game.cards[variant].orEmpty().values
                    .filter { configuration -> players.any { it in configuration.players } }
                    .forEach { configuration ->
                        configuration.cards.forEach { (suitKey, values) ->
                            val suit = Suit.of(suitKey) ?: return@forEach
                            values.forEach { (value, count) ->
                                repeat(count) { index ->
                                    getOrPut(Card(suit, value, index + 1)) { LinkedHashSet() }.add(game)
                                }
                            }
                        }
                    }
            }
        }
    }

    val size: Int get() = cards.size

    fun usedSuits(): List<Suit> = Suit.entries.filter { suit -> cards.keys.any { it.suit == suit } }

    /** A group of the summary: a range of single numeric values (`2-10`), or a value with its copies (`3×K`). */
    data class Group(val copies: Int, val label: String)

    /** The textual summary of a suit, groups in canonical value order. */
    fun summary(suit: Suit): List<Group> {
        val counts = cards.keys.filter { it.suit == suit }.groupingBy { it.value }.eachCount()
        val groups = ArrayList<Group>()
        val singles = ArrayList<Int>()
        fun flush() {
            if (singles.isNotEmpty()) {
                groups += singles.abbreviated().map { Group(1, it) }
                singles.clear()
            }
        }
        counts.keys.sortedWith(Deck.valueComparator).forEach { value ->
            val count = counts.getValue(value)
            val number = value.toIntOrNull()
            if (count == 1 && number != null) {
                singles += number
            } else {
                flush()
                groups += Group(count, value)
            }
        }
        flush()
        return groups
    }
}

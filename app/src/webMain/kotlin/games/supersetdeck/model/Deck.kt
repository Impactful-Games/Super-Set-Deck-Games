package games.supersetdeck.model

/** The 8 suits of the Super-Set Deck, in canonical order. */
enum class Suit(val key: String, val symbol: String) {
    Stars("stars", "★"),
    Spades("spades", "♠"),
    Hearts("hearts", "♥"),
    Clubs("clubs", "♣"),
    Diamonds("diamonds", "♦"),
    Florettes("florettes", "✿"),
    Wheels("wheels", "⎈"),
    Specials("specials", ""),
    ;

    companion object {
        fun of(key: String): Suit? = entries.firstOrNull { it.key == key }
    }
}

/** One physical card: a value of a suit, and which copy of that value it is (1 for a unique value). */
data class Card(val suit: Suit, val value: String, val copy: Int)

object Deck {

    private fun range(from: Int, to: Int) = (from..to).map { it.toString() }

    private val classic = range(0, 18) + listOf("J", "Q", "K", "K", "K", "S", "A")

    /** Each suit's values, one entry per copy, in the canonical value order. */
    private val values: Map<Suit, List<String>> = mapOf(
        Suit.Stars to range(0, 21) + listOf("J", "Q", "K", "K", "K", "S", "C", "A", "-‽", "×2"),
        Suit.Spades to classic,
        Suit.Hearts to classic,
        Suit.Clubs to classic,
        Suit.Diamonds to classic,
        Suit.Florettes to range(0, 15) + listOf("J", "Q", "K", "K", "K", "A"),
        Suit.Wheels to range(0, 8) + listOf("J", "Q", "K", "K", "A", "►"),
        Suit.Specials to List(6) { "Butterfly" } + List(6) { "Wolf" } + List(4) { "Phoenix" } +
            List(4) { "Snake" } + List(2) { "Dragon" } + List(2) { "Monkey" } + listOf("Ø"),
    )

    /** The canonical order of values, used to sort the cards of a suit. */
    val valueOrder: List<String> = buildList {
        add("0"); add("Butterfly")
        addAll(range(1, 6)); add("Wolf")
        addAll(range(7, 12)); add("Phoenix")
        addAll(range(13, 16)); add("Snake")
        addAll(range(17, 20)); add("Dragon")
        add("21"); add("J"); add("Monkey"); add("Q"); add("S"); add("Ø"); add("K"); add("C"); add("A")
        add("-‽"); add("►"); add("×2")
    }

    val valueComparator: Comparator<String> = compareBy { valueOrder.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }

    /** Every card of a suit, copies numbered, in canonical order. */
    fun cards(suit: Suit): List<Card> {
        val seen = HashMap<String, Int>()
        return values.getValue(suit)
            .sortedWith(valueComparator)
            .map { value ->
                val copy = (seen[value] ?: 0) + 1
                seen[value] = copy
                Card(suit, value, copy)
            }
    }

    /** How many copies of [value] the [suit] holds. */
    fun copies(suit: Suit, value: String): Int = values.getValue(suit).count { it == value }

    /** 198. */
    val size: Int get() = values.values.sumOf { it.size }
}

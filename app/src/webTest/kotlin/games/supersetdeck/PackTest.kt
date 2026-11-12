package games.supersetdeck

import games.supersetdeck.model.Card
import games.supersetdeck.model.Pack
import games.supersetdeck.model.PackedGame
import games.supersetdeck.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackTest {
    private fun Pack.text(suit: Suit) = summary(suit).joinToString(", ") { if (it.copies > 1) "${it.copies}×${it.label}" else it.label }

    @Test
    fun tichuSummary() {
        val pack = Pack(listOf(PackedGame.full(tichu)))
        assertEquals(listOf(Suit.Stars, Suit.Spades, Suit.Hearts, Suit.Clubs, Suit.Diamonds, Suit.Specials), pack.usedSuits())
        assertEquals("1", pack.text(Suit.Stars))
        assertEquals("2-10, J, Q, K, A", pack.text(Suit.Spades))
        assertEquals("Wolf, Phoenix, Dragon", pack.text(Suit.Specials))
        assertEquals(56, pack.size)
    }

    @Test
    fun playerConfigurationsApplyToTheSelectedCounts() {
        assertEquals(54, Pack(listOf(PackedGame(chimera, setOf(3), emptySet()))).size)
        val five = Pack(listOf(PackedGame(chimera, setOf(5), emptySet())))
        assertEquals(58, five.size)
        assertEquals("4×Butterfly, Phoenix, Dragon", five.text(Suit.Specials))
    }

    @Test
    fun copiesAreAMaximumNotASum() {
        val pack = Pack(listOf(PackedGame.full(tichu), PackedGame.full(chimera)))
        assertEquals(setOf(tichu, chimera), pack.cards[Card(Suit.Spades, "7", 1)])
        assertEquals(setOf(tichu), pack.cards[Card(Suit.Spades, "K", 1)])
        assertTrue(Card(Suit.Specials, "Phoenix", 2) !in pack.cards)
    }

    @Test
    fun variantsAddCards() {
        assertEquals("3×K", Pack(listOf(PackedGame(lostCities, setOf(2), emptySet()))).text(Suit.Stars))
        assertEquals(listOf(Suit.Stars), Pack(listOf(PackedGame(lostCities, setOf(2), emptySet()))).usedSuits())
        assertEquals(listOf(Suit.Stars, Suit.Florettes), Pack(listOf(PackedGame.full(lostCities))).usedSuits())
    }
}

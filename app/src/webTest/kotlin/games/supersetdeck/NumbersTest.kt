package games.supersetdeck

import games.supersetdeck.model.abbreviatedText
import kotlin.test.Test
import kotlin.test.assertEquals

class NumbersTest {
    @Test
    fun abbreviatesRunsOfThreeOrMore() {
        assertEquals("2-6", listOf(2, 3, 4, 5, 6).abbreviatedText())
        assertEquals("3, 4, 6", listOf(3, 4, 6).abbreviatedText())
        assertEquals("1, 2, 4", listOf(1, 2, 4).abbreviatedText())
        assertEquals("2-4, 7-10", listOf(2, 3, 4, 7, 8, 9, 10).abbreviatedText())
    }

    @Test
    fun sortsAndDeduplicates() {
        assertEquals("1-3, 8", listOf(8, 3, 1, 2, 3).abbreviatedText())
        assertEquals("", emptyList<Int>().abbreviatedText())
    }
}

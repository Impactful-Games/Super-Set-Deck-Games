package games.supersetdeck

import games.supersetdeck.model.PackedGame
import games.supersetdeck.navigation.Location
import games.supersetdeck.navigation.Routes
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {
    private val games = listOf(tichu, chimera, lostCities)

    @Test
    fun encodesThePackOrderedById() {
        val location = Routes.packer(
            listOf(
                PackedGame.full(tichu),
                PackedGame(chimera, setOf(3), emptySet()),
                PackedGame(lostCities, setOf(2), emptySet()),
            )
        )
        assertEquals(listOf("Chimera*3*", "Lost_Cities**-", "Tichu**"), location.all("g"))
    }

    @Test
    fun decodesThePack() {
        val location = Location("/packer", listOf("g" to "Chimera*3*", "g" to "Tichu**", "g" to "Unknown*2*", "g" to "Lost_Cities*x*-"))
        val packed = Routes.packedGames(location, games)
        assertEquals(listOf("Chimera", "Tichu", "Lost_Cities"), packed.map { it.game.id })
        assertEquals(setOf(3), packed[0].players)
        assertEquals(setOf(3, 4, 6), packed[1].players)
        // Invalid player counts mean all of them; "-" means no variant.
        assertEquals(setOf(2), packed[2].players)
        assertEquals(emptySet(), packed[2].variants)
    }

    @Test
    fun missingVariantsMeanAll() {
        val packed = Routes.packedGames(Location("/packer", listOf("g" to "Lost_Cities")), games)
        assertEquals(setOf("Advanced"), packed.single().variants)
    }

    @Test
    fun allReplacesThePack() {
        val packed = Routes.packedGames(Location("/packer", listOf("all" to "", "g" to "Tichu*3*")), games)
        assertEquals(games.map { it.id }, packed.map { it.game.id })
        assertEquals(setOf(3, 4, 6), packed.first().players)
    }
}

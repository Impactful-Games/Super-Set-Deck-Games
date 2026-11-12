package games.supersetdeck.navigation

import games.supersetdeck.model.Game
import games.supersetdeck.model.PackedGame

enum class SortOrder(val param: String?) {
    Name(null), Rating("rating"), Year("year");

    companion object {
        fun of(param: String?): SortOrder = entries.firstOrNull { it.param != null && it.param.equals(param, ignoreCase = true) } ?: Name
    }
}

/** The catalogue's filters, entirely carried by the URL; only non-default values are written. */
data class CatalogueFilters(
    val types: Set<String> = emptySet(),
    val listId: String? = null,
    val listName: String? = null,
    val playerCounts: Set<Int> = emptySet(),
    val search: String = "",
    val sort: SortOrder = SortOrder.Name,
) {
    val isEmpty: Boolean get() = this == CatalogueFilters()

    fun toParams(): List<Pair<String, String>> = buildList {
        if (types.isNotEmpty()) add("types" to types.sorted().joinToString(","))
        if (listId != null) add("list" to "$listId.${listName.orEmpty()}")
        if (playerCounts.isNotEmpty()) add("playerCounts" to playerCounts.sorted().joinToString(","))
        if (search.isNotEmpty()) add("search" to search)
        sort.param?.let { add("sort" to it) }
    }

    companion object {
        val names = setOf("types", "list", "playerCounts", "search", "sort")

        fun from(location: Location): CatalogueFilters {
            val list = location["list"]?.takeIf { it.isNotEmpty() }
            return CatalogueFilters(
                types = location["types"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty(),
                // Split on the first dot only: a list name may contain dots.
                listId = list?.substringBefore('.'),
                listName = list?.substringAfter('.', ""),
                playerCounts = location["playerCounts"]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty(),
                search = location["search"].orEmpty(),
                sort = SortOrder.of(location["sort"]),
            )
        }
    }
}

enum class GameTab { Rules, References }

/** Every screen of the site, read from the URL. */
sealed interface Screen {
    data class Catalogue(val filters: CatalogueFilters) : Screen
    data class GamePage(val id: String, val filters: CatalogueFilters, val tab: GameTab, val section: String?) : Screen
    data object MyLists : Screen
    data class Packer(val location: Location) : Screen
    data object About : Screen
    data object Changelog : Screen
    data object Unknown : Screen

    companion object {
        fun of(location: Location): Screen {
            val segments = location.segments
            return when {
                segments == listOf("games") -> Catalogue(CatalogueFilters.from(location))
                segments.size == 2 && segments[0] == "games" -> GamePage(
                    id = segments[1],
                    filters = CatalogueFilters.from(location),
                    tab = if (location["page"] == "references") GameTab.References else GameTab.Rules,
                    section = location["section"]?.takeIf { it.isNotEmpty() },
                )
                segments == listOf("lists") -> MyLists
                segments == listOf("packer") -> Packer(location)
                segments == listOf("about") -> About
                segments == listOf("changelog") -> Changelog
                else -> Unknown
            }
        }
    }
}

object Routes {
    fun catalogue(filters: CatalogueFilters = CatalogueFilters()) = Location("/games", filters.toParams())

    fun game(id: String, filters: CatalogueFilters = CatalogueFilters()) = Location("/games/$id", filters.toParams())

    /** The canonical production URL of a game page, whatever the domain the site is browsed on. */
    fun shareUrl(id: String) = "https://super-set-deck.games/#/games/$id"

    // region Packer

    /**
     * One `g` parameter per game, ordered by id: `{id}*{players}*{variants}`, players joined with dots
     * (empty when all are selected), variants sorted and joined with dots (empty when all are, `-` when none).
     */
    fun packer(games: List<PackedGame>) = Location("/packer", games.sortedBy { it.game.id }.map { packed ->
        val players = if (packed.players.containsAll(packed.game.playerCount)) "" else packed.players.sorted().joinToString(".")
        val variants = when {
            packed.variants.containsAll(packed.game.variants) -> ""
            packed.variants.isEmpty() -> "-"
            else -> packed.variants.sorted().joinToString(".")
        }
        "g" to "${packed.game.id}*$players*$variants"
    })

    /** Reads a packer URL: unknown games are ignored, missing or invalid players and variants mean all. */
    fun packedGames(location: Location, games: List<Game>): List<PackedGame> {
        if (location.has("all")) return games.map { PackedGame.full(it) }
        val byId = games.associateBy { it.id }
        return location.all("g").mapNotNull { value ->
            val parts = value.split('*')
            val game = byId[parts[0]] ?: return@mapNotNull null
            val players = parts.getOrNull(1).orEmpty().split('.').mapNotNull { it.toIntOrNull() }
                .filter { it in game.playerCount }.toSet()
                .ifEmpty { game.playerCount.toSet() }
            val variants = when (val v = parts.getOrNull(2)) {
                null, "" -> game.variants.toSet()
                "-" -> emptySet()
                else -> v.split('.').filter { it in game.variants }.toSet()
            }
            PackedGame(game, players, variants)
        }.distinctBy { it.game.id }
    }

    // endregion
}

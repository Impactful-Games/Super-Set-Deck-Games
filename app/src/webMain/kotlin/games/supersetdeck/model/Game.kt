package games.supersetdeck.model

import games.supersetdeck.platform.normalizeForSearch
import kotlinx.serialization.Serializable

/** Suit → value → number of copies needed. */
typealias SuitCards = Map<String, Map<String, Int>>

@Serializable
data class Game(
    val id: String,
    val names: Map<String, String>,
    val designers: Designers,
    val types: List<String>,
    val playerCount: List<Int>,
    /** Variant (`Base`, `Advanced`…) → player configuration (`all`, `5`, `4,5`…) → cards. */
    val cards: Map<String, Map<String, PlayerCards>>,
    /** The game aids, `R-1.png`, `R-2.png`… (0 when an older `games.json` from the cache has none). */
    val referenceCount: Int = 0,
    val thumbnailExt: String,
    val imageExt: String,
    val bgg: Bgg,
) {
    @Serializable
    data class Designers(
        val authors: List<String>? = null,
        val adaptedBy: List<String>? = null,
        val tradition: String? = null,
    )

    @Serializable
    data class PlayerCards(val players: List<Int>, val cards: SuitCards)

    @Serializable
    data class Bgg(
        val bggId: Long,
        val names: List<String>,
        val yearPublished: Int,
        val playingTime: Int,
        val minAge: Int,
        val rating: Double,
    )

    /** The name in [lang], else in English, else the first one available. */
    fun name(lang: String): String = names[lang] ?: names["en"] ?: names.values.first()

    /** The optional variants: every variant but Base, sorted by name. */
    val variants: List<String> get() = cards.keys.filter { it != BASE_VARIANT }.sorted()

    val thumbnailUrl: String get() = "games/$id/thumbnail.$thumbnailExt"
    val imageUrl: String get() = "games/$id/image.$imageExt"
    fun ruleUrl(lang: String): String = "games/$id/$lang.html"
    /** The URL of the game aid at [index], from 0. */
    fun referenceUrl(index: Int): String = "games/$id/R-${index + 1}.png"

    /** Every value a textual search looks into, normalized. */
    val searchValues: List<String> by lazy {
        (names.values + designers.authors.orEmpty() + designers.adaptedBy.orEmpty() + listOfNotNull(designers.tradition) + bgg.names)
            .map { normalizeForSearch(it) }
    }

    companion object {
        const val BASE_VARIANT = "Base"
    }
}

/** Games matching every word of [text], in any name, author, adapter, tradition or BGG name. */
fun List<Game>.searched(text: String): List<Game> {
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.map { normalizeForSearch(it) }
    if (words.isEmpty()) return this
    return filter { game -> words.all { word -> game.searchValues.any { word in it } } }
}

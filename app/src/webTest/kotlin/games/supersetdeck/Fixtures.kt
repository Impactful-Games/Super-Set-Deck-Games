package games.supersetdeck

import games.supersetdeck.model.Game

fun game(
    id: String,
    players: List<Int>,
    cards: Map<String, Map<String, Game.PlayerCards>>,
    names: Map<String, String> = mapOf("en" to id),
) = Game(
    id = id,
    names = names,
    designers = Game.Designers(),
    types = emptyList(),
    playerCount = players,
    cards = cards,
    referenceCount = 0,
    thumbnailExt = "jpg",
    imageExt = "jpg",
    bgg = Game.Bgg(1, listOf(id), 2000, 30, 8, 7.0),
)

/** Tichu, as its game.yaml describes it. */
val tichu = game(
    "Tichu", listOf(3, 4, 6),
    mapOf(
        "Base" to mapOf(
            "all" to Game.PlayerCards(
                listOf(3, 4, 6),
                mapOf("stars" to mapOf("1" to 1)) +
                    listOf("spades", "hearts", "clubs", "diamonds").associateWith { ((2..10).map { it.toString() } + listOf("J", "Q", "K", "A")).associateWith { 1 } } +
                    mapOf("specials" to mapOf("Wolf" to 1, "Phoenix" to 1, "Dragon" to 1)),
            ),
        ),
    ),
)

/** Chimera: more cards at 5 players. */
val chimera = game(
    "Chimera", listOf(3, 5),
    mapOf(
        "Base" to mapOf(
            "all" to Game.PlayerCards(
                listOf(3, 5),
                listOf("spades", "hearts", "clubs", "diamonds").associateWith { ((1..12).map { it.toString() } + "A").associateWith { 1 } } +
                    mapOf("specials" to mapOf("Phoenix" to 1, "Dragon" to 1)),
            ),
            "5" to Game.PlayerCards(listOf(5), mapOf("specials" to mapOf("Butterfly" to 4))),
        ),
    ),
)

/** A game with optional variants. */
val lostCities = game(
    "Lost_Cities", listOf(2),
    mapOf(
        "Base" to mapOf("all" to Game.PlayerCards(listOf(2), mapOf("stars" to mapOf("K" to 3)))),
        "Advanced" to mapOf("all" to Game.PlayerCards(listOf(2), mapOf("florettes" to mapOf("A" to 1)))),
    ),
)

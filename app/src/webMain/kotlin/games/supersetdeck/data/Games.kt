package games.supersetdeck.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import games.supersetdeck.model.Game
import games.supersetdeck.platform.fetchText
import games.supersetdeck.platform.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface GamesState {
    data object Loading : GamesState
    data class Loaded(val games: List<Game>) : GamesState {
        private val byId = games.associateBy { it.id }
        operator fun get(id: String): Game? = byId[id]
    }
}

object Games {
    private val json = Json { ignoreUnknownKeys = true }

    var state: GamesState by mutableStateOf(GamesState.Loading)
        private set

    /** The catalogue, empty while loading or if it could not be loaded. */
    val all: List<Game> get() = (state as? GamesState.Loaded)?.games.orEmpty()

    operator fun get(id: String): Game? = (state as? GamesState.Loaded)?.get(id)

    fun load(scope: CoroutineScope) {
        scope.launch {
            state = try {
                GamesState.Loaded(json.decodeFromString<List<Game>>(fetchText("games/games.json")))
            } catch (error: Throwable) {
                AppErrors.report(AppError.Games(error.userMessage()))
                GamesState.Loaded(emptyList())
            }
        }
    }
}

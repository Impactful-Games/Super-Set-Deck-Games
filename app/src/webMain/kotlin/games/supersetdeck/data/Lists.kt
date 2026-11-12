package games.supersetdeck.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import games.supersetdeck.i18n.Strings
import games.supersetdeck.platform.Database
import games.supersetdeck.platform.nowMillis
import games.supersetdeck.platform.randomUuid
import games.supersetdeck.platform.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A game list of the user; the favourites list has the [FAVOURITES] id and an empty name. */
data class UserList(val id: String, val name: String) {
    val isFavourites: Boolean get() = id == FAVOURITES

    fun displayName(strings: Strings): String = if (isFavourites) strings.favourites else name

    companion object {
        const val FAVOURITES = "favourites"
    }
}

/** Every list of the device, tombstones included (an id with an update time and no list). */
data class ListsData(
    val names: Map<String, String>,
    val games: Map<String, Set<String>>,
    val updates: Map<String, Double>,
) {
    /** The favourites first, then the personal lists sorted by name. */
    val lists: List<UserList> =
        listOf(UserList(UserList.FAVOURITES, "")) +
            names.map { (id, name) -> UserList(id, name) }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun list(id: String): UserList? = if (id == UserList.FAVOURITES) lists.first() else names[id]?.let { UserList(id, it) }

    fun gamesOf(id: String): Set<String> = games[id].orEmpty()

    fun listsOf(gameId: String): List<UserList> = lists.filter { gameId in gamesOf(it.id) }
}

sealed interface ListsState {
    data object Loading : ListsState
    data class Unavailable(val message: String) : ListsState
    data class Ready(val data: ListsData) : ListsState
}

object Lists {
    @Serializable
    private class Stored(val lists: List<StoredList>, val entries: List<StoredEntry>, val updates: List<StoredUpdate>)

    @Serializable
    private class StoredList(val id: String, val name: String)

    @Serializable
    private class StoredEntry(val listId: String, val gameId: String)

    @Serializable
    private class StoredUpdate(val id: String, val date: Double)

    @Serializable
    private class Write(val name: String?, val games: List<String>, val date: Double)

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var scope: CoroutineScope
    private var database: Database? = null

    var state: ListsState by mutableStateOf(ListsState.Loading)
        private set

    /** The lists, or null while loading or in degraded mode. */
    val data: ListsData? get() = (state as? ListsState.Ready)?.data

    fun load(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            state = try {
                val db = Database.open()
                val stored = json.decodeFromString<Stored>(db.loadAll())
                database = db
                ListsState.Ready(
                    ListsData(
                        names = stored.lists.associate { it.id to it.name },
                        games = stored.entries.groupBy({ it.listId }, { it.gameId }).mapValues { it.value.toSet() },
                        updates = stored.updates.associate { it.id to it.date },
                    )
                )
            } catch (error: Throwable) {
                val message = error.userMessage()
                AppErrors.report(AppError.Database(message))
                ListsState.Unavailable(message)
            }
        }
    }

    private fun update(id: String, date: Double = nowMillis(), transform: (ListsData) -> ListsData) {
        val current = data ?: return
        val next = transform(current).let { it.copy(updates = it.updates + (id to date)) }
        state = ListsState.Ready(next)
        val write = Write(
            name = if (id == UserList.FAVOURITES) "" else next.names[id],
            games = next.gamesOf(id).sorted(),
            date = date,
        )
        val db = database ?: return
        scope.launch {
            try {
                db.writeList(id, json.encodeToString(Write.serializer(), write))
            } catch (error: Throwable) {
                AppErrors.report(AppError.Other(error.userMessage()))
            }
        }
    }

    /** Creates a list and returns its new id. */
    fun create(name: String): String {
        val id = randomUuid()
        update(id) { it.copy(names = it.names + (id to name), games = it.games + (id to emptySet())) }
        return id
    }

    fun rename(id: String, name: String) {
        if (id == UserList.FAVOURITES) return
        update(id) { it.copy(names = it.names + (id to name)) }
    }

    /** Deletes a list, leaving a tombstone (its id and the deletion time) for the synchronization. */
    fun delete(id: String) {
        if (id == UserList.FAVOURITES) return
        update(id) { it.copy(names = it.names - id, games = it.games - id) }
    }

    fun addGame(id: String, gameId: String) {
        val current = data ?: return
        if (current.list(id) == null || gameId in current.gamesOf(id)) return
        update(id) { it.copy(games = it.games + (id to (it.gamesOf(id) + gameId))) }
    }

    fun removeGame(id: String, gameId: String) {
        val current = data ?: return
        if (gameId !in current.gamesOf(id)) return
        update(id) { it.copy(games = it.games + (id to (it.gamesOf(id) - gameId))) }
    }

    /**
     * Replaces a list by a backup's version, keeping the backup's date: [name] null means the
     * backup holds a tombstone, and the list is deleted locally.
     */
    fun restore(id: String, name: String?, games: List<String>?, date: Double) {
        update(id, date) {
            if (name == null || games == null) it.copy(names = it.names - id, games = it.games - id)
            else it.copy(
                names = if (id == UserList.FAVOURITES) it.names else it.names + (id to name),
                games = it.games + (id to games.toSet()),
            )
        }
    }
}

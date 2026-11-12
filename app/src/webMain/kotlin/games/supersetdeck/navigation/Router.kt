package games.supersetdeck.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import games.supersetdeck.platform.currentHash
import games.supersetdeck.platform.decodeQueryComponent
import games.supersetdeck.platform.encodeQueryComponent
import games.supersetdeck.platform.historyBack
import games.supersetdeck.platform.historyState
import games.supersetdeck.platform.onPopState
import games.supersetdeck.platform.pushHistory
import games.supersetdeck.platform.randomUuid
import games.supersetdeck.platform.replaceHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A screen path (`/games/Tichu`) and its query parameters, decoded, in order (`g` repeats). */
data class Location(val path: String, val params: List<Pair<String, String>> = emptyList()) {

    operator fun get(name: String): String? = params.firstOrNull { it.first == name }?.second

    fun all(name: String): List<String> = params.filter { it.first == name }.map { it.second }

    fun has(name: String): Boolean = params.any { it.first == name }

    /** The URL hash: `#/games/Tichu?page=references`. */
    fun toHash(): String = "#" + path + if (params.isEmpty()) "" else
        "?" + params.joinToString("&") { (k, v) -> encodeQueryComponent(k) + "=" + encodeQueryComponent(v) }

    val segments: List<String> get() = path.split('/').filter { it.isNotEmpty() }

    companion object {
        fun parse(hash: String): Location {
            val raw = hash.removePrefix("#")
            val path = raw.substringBefore('?').ifEmpty { "/" }
            val query = raw.substringAfter('?', "")
            val params = query.split('&').filter { it.isNotEmpty() }.map {
                decodeQueryComponent(it.substringBefore('=')) to decodeQueryComponent(it.substringAfter('=', ""))
            }
            val decodedPath = path.split('/').joinToString("/") { decodeQueryComponent(it) }
            return Location(if (decodedPath.startsWith("/")) decodedPath else "/$decodedPath", params)
        }
    }
}

/**
 * Hash-based navigation (`https://super-set-deck.games/#/games/Tichu`), so that every existing URL
 * keeps working. Each history entry gets an index, to restore its scroll position when going back.
 */
object Router {

    @Serializable
    private class EntryState(val s: String, val i: Int, val f: Boolean)

    private val session = randomUuid()
    private var nextIndex = 0
    private val savedScrolls = HashMap<Int, Int>()
    private var activeScroll: Pair<Int, ScrollState>? = null

    var location: Location by mutableStateOf(Location("/"))
        private set

    /** The index of the current history entry. */
    var entry: Int by mutableStateOf(0)
        private set

    /** Whether the current entry was reached by navigating inside the app (rather than by a link or a reload). */
    var reachedInApp: Boolean by mutableStateOf(false)
        private set

    fun start() {
        adoptCurrentEntry()
        onPopState {
            saveScroll()
            adoptCurrentEntry()
        }
    }

    private fun adoptCurrentEntry() {
        val state = historyState()?.let { runCatching { Json.decodeFromString<EntryState>(it) }.getOrNull() }
        val parsed = Location.parse(currentHash())
        if (state != null && state.s == session) {
            entry = state.i
            reachedInApp = state.f
        } else {
            // A direct arrival (link, QR code, reload, typed address): a new entry of this session.
            entry = nextIndex++
            reachedInApp = false
            replaceHistory(Json.encodeToString(EntryState.serializer(), EntryState(session, entry, false)), parsed.toHash())
        }
        location = parsed
        if (parsed.path == "/") navigate(Location("/games"), replace = true)
    }

    private fun saveScroll() {
        activeScroll?.let { (index, state) -> savedScrolls[index] = state.value }
    }

    /** Opens [target] in a new history entry, or in place of the current one with [replace]. */
    fun navigate(target: Location, replace: Boolean = false) {
        if (replace) {
            location = target
            replaceHistory(Json.encodeToString(EntryState.serializer(), EntryState(session, entry, reachedInApp)), target.toHash())
        } else {
            saveScroll()
            val index = nextIndex++
            savedScrolls.remove(index)
            pushHistory(Json.encodeToString(EntryState.serializer(), EntryState(session, index, true)), target.toHash())
            entry = index
            reachedInApp = true
            location = target
        }
    }

    fun navigate(path: String, params: List<Pair<String, String>> = emptyList(), replace: Boolean = false) =
        navigate(Location(path, params), replace)

    /** Changes the current screen's parameters, without a new history entry. */
    fun replaceParams(params: List<Pair<String, String>>) = navigate(location.copy(params = params), replace = true)

    /** Goes back to the previous entry when it belongs to the app, else opens [fallback]. */
    fun back(fallback: Location) {
        if (reachedInApp) historyBack()
        else navigate(fallback)
    }

    internal fun register(index: Int, state: ScrollState) {
        activeScroll = index to state
    }

    internal fun unregister(index: Int, state: ScrollState) {
        if (activeScroll?.second === state) {
            savedScrolls[index] = state.value
            activeScroll = null
        }
    }

    internal fun savedScroll(index: Int): Int = savedScrolls[index] ?: 0
}

/**
 * The scroll state of the current screen: back at the top on a new page, restored when coming back
 * (once the content is tall enough again, as it may still be loading).
 */
@Composable
fun rememberScreenScrollState(): ScrollState {
    val entry = Router.entry
    val state = remember(entry) { ScrollState(0) }
    val saved = remember(entry) { Router.savedScroll(entry) }
    LaunchedEffect(state) {
        if (saved > 0) {
            withTimeoutOrNull(5000) { snapshotFlow { state.maxValue }.first { it >= saved } }
            state.scrollTo(saved)
        }
    }
    DisposableEffect(state) {
        Router.register(entry, state)
        onDispose { Router.unregister(entry, state) }
    }
    return state
}

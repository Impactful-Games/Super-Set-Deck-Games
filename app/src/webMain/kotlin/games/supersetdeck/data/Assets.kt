package games.supersetdeck.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import games.supersetdeck.platform.fetchBytes
import games.supersetdeck.platform.fetchText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jetbrains.compose.resources.decodeToImageBitmap

/** A small least-recently-used memory cache. */
private class LruCache<V>(private val capacity: Int) {
    private val map = LinkedHashMap<String, V>()

    operator fun get(key: String): V? = map.remove(key)?.also { map[key] = it }

    operator fun set(key: String, value: V) {
        map.remove(key)
        map[key] = value
        while (map.size > capacity) map.remove(map.keys.first())
    }
}

/** Loads a value once per key, sharing the loading between concurrent callers. */
private class Loader<V : Any>(capacity: Int, private val load: suspend (String) -> V) {
    private val cache = LruCache<V>(capacity)
    private val loading = HashMap<String, Deferred<Result<V>>>()

    fun cached(key: String): V? = cache[key]

    suspend fun get(key: String): V {
        cache[key]?.let { return it }
        loading[key]?.let { return it.await().getOrThrow() }
        val deferred = CompletableDeferred<Result<V>>()
        loading[key] = deferred
        val result = runCatching { load(key) }
        result.onSuccess { cache[key] = it }
        loading.remove(key)
        deferred.complete(result)
        return result.getOrThrow()
    }
}

object Images {
    // The thumbnails of the whole catalogue stay in memory, with a few large images.
    private val loader = Loader(capacity = 120) { url -> fetchBytes(url).decodeToImageBitmap() }

    fun cached(url: String): ImageBitmap? = loader.cached(url)

    suspend fun load(url: String): ImageBitmap = loader.get(url)
}

/** The image at [url]: null while loading or if it cannot be loaded. */
@Composable
fun rememberImage(url: String): State<ImageBitmap?> =
    produceState(Images.cached(url), url) {
        if (value == null) value = runCatching { Images.load(url) }.getOrNull()
    }

object Documents {
    // The last rules and documents read stay in memory, to show again without waiting.
    private val loader = Loader(capacity = 8) { url -> fetchText(url) }

    fun cached(url: String): String? = loader.cached(url)

    suspend fun load(url: String): String = loader.get(url)
}

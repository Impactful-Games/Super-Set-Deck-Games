package games.supersetdeck.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The backup document, identical in `.ssdbak` files and in Google Drive's
 * `Super-Set-Deck-Collections.json`, so that existing backups stay usable.
 */
@Serializable
data class Backup(
    val version: Int = 1,
    /** List id → list state. */
    val lists: Map<String, BackupList> = emptyMap(),
) {
    @Serializable
    data class BackupList(
        /** Last modification, in milliseconds since the Unix epoch. */
        val update: Long,
        /** Null for a deleted list (a tombstone). */
        val content: Content?,
    )

    @Serializable
    data class Content(
        /** Empty for the favourites. */
        val name: String,
        val games: List<String>,
    )

    fun encode(): String = json.encodeToString(serializer(), this)

    /** Same lists, same dates, same names and same games (in any order). */
    fun sameAs(other: Backup): Boolean = normalized() == other.normalized()

    private fun normalized() = lists.mapValues { (_, list) ->
        Triple(list.update, list.content?.name, list.content?.games?.toSet())
    }

    companion object {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            prettyPrintIndent = "    "
            encodeDefaults = true
            explicitNulls = true
        }

        /** An empty (or blank) file is an empty backup. */
        fun decode(text: String): Backup = if (text.isBlank()) Backup() else json.decodeFromString(serializer(), text)

        /** The application's lists in the backup format: every list with a modification time. */
        fun of(data: ListsData): Backup = Backup(
            lists = data.updates.mapValues { (id, date) ->
                val list = data.list(id)
                BackupList(
                    update = date.toLong(),
                    content = list?.let { Content(if (it.isFavourites) "" else it.name, data.gamesOf(id).sorted()) },
                )
            }
        )
    }
}

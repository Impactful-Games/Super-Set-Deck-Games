package games.supersetdeck.data

import games.supersetdeck.platform.OpenedFile
import games.supersetdeck.platform.driveFetch
import games.supersetdeck.platform.encodeQueryComponent
import games.supersetdeck.platform.loadScript
import games.supersetdeck.platform.parseDate
import games.supersetdeck.platform.requestGoogleToken
import games.supersetdeck.platform.saveTextFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Where a backup is read from and written to. */
sealed interface BackupStorage {
    /** The backup's text, empty when there is none yet. */
    suspend fun read(): String

    /** Writes the backup; false if the user cancelled. */
    suspend fun write(content: String): Boolean
}

/** A local `.ssdbak` file: [opened] for an existing file, null for a new one. */
class LocalFileStorage(private val opened: OpenedFile?, private val fileDescription: String) : BackupStorage {
    override suspend fun read(): String = opened?.text ?: ""

    override suspend fun write(content: String): Boolean =
        saveTextFile(
            suggestedName = opened?.name ?: "MyCollection.ssdbak",
            description = fileDescription,
            mimeType = "application/ssd-back",
            extension = ".ssdbak",
            text = content,
        )
}

/**
 * The `Super-Set-Deck-Collections.json` file of the private Drive data folder (appDataFolder) of
 * the site's OAuth client, where the previous version kept its backups too.
 */
class GoogleDriveStorage private constructor(private val token: String) : BackupStorage {

    @Serializable
    private class FileList(val files: List<File> = emptyList())

    @Serializable
    private class File(val id: String, val modifiedTime: String? = null)

    private var file: File? = null

    /** The last backup's time (ms since epoch), null when there is no backup yet. Known after [read]. */
    val lastBackup: Double? get() = file?.modifiedTime?.let { parseDate(it) }

    override suspend fun read(): String {
        val query = "name = '$FILE_NAME' and 'appDataFolder' in parents and trashed = false"
        val list = json.decodeFromString<FileList>(
            driveFetch(token, "GET", "$API/files?spaces=appDataFolder&fields=files(id,modifiedTime)&q=${encodeQueryComponent(query)}")
        )
        // With several files, the first one is used.
        file = list.files.firstOrNull()
        return file?.let { driveFetch(token, "GET", "$API/files/${it.id}?alt=media") } ?: ""
    }

    override suspend fun write(content: String): Boolean {
        val existing = file
        if (existing == null) {
            val boundary = "ssd-backup-boundary"
            val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}"""
            val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
                "--$boundary\r\nContent-Type: application/json\r\n\r\n$content\r\n--$boundary--"
            file = json.decodeFromString<File>(
                driveFetch(token, "POST", "$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime", "multipart/related; boundary=$boundary", body)
            )
        } else {
            file = json.decodeFromString<File>(
                driveFetch(token, "PATCH", "$UPLOAD/files/${existing.id}?uploadType=media&fields=id,modifiedTime", "application/json", content)
            )
        }
        return true
    }

    suspend fun delete() {
        file?.let { driveFetch(token, "DELETE", "$API/files/${it.id}") }
        file = null
    }

    companion object {
        private const val CLIENT_ID = "269700352625-hqcmod7k8527ennfnb040jk6ef3s5m8t.apps.googleusercontent.com"
        private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val FILE_NAME = "Super-Set-Deck-Collections.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val SCRIPT = "https://accounts.google.com/gsi/client"

        private val json = Json { ignoreUnknownKeys = true }

        /** Loads Google's script ahead of time, so that the consent popup opens right on click. */
        suspend fun preload() {
            try {
                loadScript(SCRIPT)
            } catch (_: Throwable) {
                // Reported when the user actually asks for Google Drive.
            }
        }

        /** Asks for the user's consent; must start from a user gesture. */
        suspend fun connect(): GoogleDriveStorage {
            loadScript(SCRIPT)
            return GoogleDriveStorage(requestGoogleToken(CLIENT_ID, SCOPE))
        }
    }
}

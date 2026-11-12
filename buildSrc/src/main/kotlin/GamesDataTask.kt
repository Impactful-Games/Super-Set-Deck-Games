import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Generates `games/games.json` from every `games/<id>/game.yaml`, enriched with BoardGameGeek data,
 * and copies each game's BGG thumbnail & image.
 * Its references and printables are exported from their DesignCraft files by [DesignExportTask], into [designsDir].
 *
 * BGG answers are cached in [cacheDir] forever: a game only hits the BGG API (and needs a token)
 * the first time it is built on a machine.
 */
@DisableCachingByDefault(because = "Depends on the BGG answers stored in the local cache directory")
abstract class GamesDataTask : DefaultTask() {

    init {
        group = "build"
        description = "Generates the games data (games.json and BGG images)."
    }

    @get:Inject
    protected abstract val fileSystem: FileSystemOperations

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gamesDir: DirectoryProperty

    /** The output of [DesignExportTask]: the references (`games/<id>/R-<n>.png`) are counted there. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val designsDir: DirectoryProperty

    /** Where BGG answers and images are kept between builds. Not an input: it is a local cache. */
    @get:Internal
    abstract val cacheDir: DirectoryProperty

    /** Only needed for games that are not in [cacheDir] yet. Not an input: it does not change the output. */
    @get:Internal
    abstract val bggToken: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // Created on first use, during execution: an HTTP client cannot be stored in the configuration cache.
    @Transient
    private var httpClient: HttpClient? = null

    private class BggGame(
        val names: List<String>,
        val yearPublished: Int,
        val playingTime: Int,
        val minAge: Int,
        val rating: Double,
        val thumbnail: String,
        val image: String,
    )

    @TaskAction
    fun run() {
        val out = outputDir.get().asFile
        fileSystem.delete { delete(out) }
        val gamesOut = out.resolve("games").apply { mkdirs() }

        val gameDirs = gamesDir.get().asFile.listFiles()!!
            .filter { it.isDirectory && it.resolve("game.yaml").isFile }
            .sortedBy { it.name }

        val games = gameDirs.map { dir ->
            try {
                processGame(dir, gamesOut.resolve(dir.name))
            } catch (ex: Exception) {
                throw RuntimeException("Could not process game ${dir.name}: ${ex.message}", ex)
            }
        }

        gamesOut.resolve("games.json").writeText(toJson(games))
    }

    @Suppress("UNCHECKED_CAST")
    private fun processGame(dir: File, out: File): Map<String, Any?> {
        logger.info("Game ${dir.name}")
        out.mkdirs()

        val yaml = Load(LoadSettings.builder().setAllowDuplicateKeys(false).build())
            .loadFromString(dir.resolve("game.yaml").readText()) as Map<String, Any?>

        val bggId = yaml["bggId"].toString().trim().toLong()
        val bgg = getBgg(bggId, dir.name)

        val thumbnailExt = bgg.thumbnail.substringAfterLast('.')
        val imageExt = bgg.image.substringAfterLast('.')
        cached(bgg.thumbnail, "${dir.name}-thumbnail.$thumbnailExt").copyTo(out.resolve("thumbnail.$thumbnailExt"), true)
        // Older caches stored the image under the thumbnail's extension.
        val imageCache = cacheDir.get().asFile.resolve("${dir.name}-image.$thumbnailExt")
            .takeIf { it.isFile && thumbnailExt != imageExt }
            ?: cached(bgg.image, "${dir.name}-image.$imageExt")
        imageCache.copyTo(out.resolve("image.$imageExt"), true)

        val playerCount = when (val players = yaml["players"]) {
            is String -> players.split(",").map { it.trim().toInt() }
            is Number -> listOf(players.toInt())
            else -> error("Bad players")
        }

        // R-1.png, R-2.png…
        val referenceCount = designsDir.get().asFile.resolve("games/${dir.name}").listFiles().orEmpty()
            .count { Regex("R-[0-9]+\\.png").matches(it.name) }

        fun names(key: String) = (yaml[key] as String?)?.split(",")?.map { it.trim() }

        return linkedMapOf(
            "id" to dir.name,
            "names" to dir.listFiles()!!
                .filter { it.extension == "adoc" }
                .sortedBy { it.name }
                .associate { it.nameWithoutExtension to it.useLines { lines -> lines.first().removePrefix("= ").trim() } },
            "designers" to linkedMapOf(
                "authors" to names("authors"),
                "adaptedBy" to names("adaptedBy"),
                "tradition" to (yaml["tradition"] as String?)?.trim(),
            ).filterValues { it != null },
            "types" to (yaml["type"] as? List<String> ?: error("Bad type")),
            "playerCount" to playerCount,
            "cards" to (yaml["cards"] as Map<String, Map<String, Map<String, Any?>>>).mapValues { (_, configs) ->
                configs.entries.associate { (key, cards) ->
                    require(key.endsWith("-players")) { "Bad card players key $key" }
                    val players = key.removeSuffix("-players")
                    players to linkedMapOf(
                        "players" to (if (players == "all") playerCount else players.expandPlayers()),
                        "cards" to cards.mapValues { (_, values) ->
                            values.toString().split(",").map { it.trim() }
                                .flatMap { it.expandCardValues() }
                                .groupingBy { it }
                                .eachCount()
                        },
                    )
                }
            },
            "referenceCount" to referenceCount,
            "thumbnailExt" to thumbnailExt,
            "imageExt" to imageExt,
            "bgg" to linkedMapOf(
                "bggId" to bggId,
                "names" to bgg.names,
                "yearPublished" to bgg.yearPublished,
                "playingTime" to bgg.playingTime,
                "minAge" to bgg.minAge,
                "rating" to bgg.rating,
            ),
        )
    }

    private fun String.expandCardValues(): List<String> =
        Regex("([0-9]+)-([0-9]+)").matchEntire(this)?.let { match ->
            (match.groupValues[1].toInt()..match.groupValues[2].toInt()).map { it.toString() }
        } ?: Regex("([0-9a-zA-Z]+)\\*([0-9]+)").matchEntire(this)?.let { match ->
            List(match.groupValues[2].toInt()) { match.groupValues[1] }
        } ?: listOf(this)

    private fun String.expandPlayers(): List<Int> =
        Regex("([0-9]+)-([0-9]+)").matchEntire(this)?.let { match ->
            (match.groupValues[1].toInt()..match.groupValues[2].toInt()).toList()
        } ?: split(",").map { it.trim().toInt() }

    private fun getBgg(bggId: Long, gameId: String): BggGame {
        val file = cached("https://boardgamegeek.com/xmlapi2/thing?id=$bggId&stats=1", "$gameId.xml")
        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(file.readBytes()))
            val item = document.getElementsByTagName("item").elements().first()
            val names = item.elements("name")
            return BggGame(
                names = (names.filter { it.getAttribute("type") == "primary" } + names.filter { it.getAttribute("type") == "alternate" })
                    .map { it.getAttribute("value") },
                yearPublished = item.elements("yearpublished").first().getAttribute("value").toInt(),
                playingTime = item.elements("playingtime").first().getAttribute("value").toInt(),
                minAge = item.elements("minage").first().getAttribute("value").toInt(),
                rating = item.elements("statistics").first().elements("ratings").first()
                    .elements("average").first().getAttribute("value").toDouble(),
                thumbnail = item.elements("thumbnail").first().textContent.trim(),
                image = item.elements("image").first().textContent.trim(),
            )
        } catch (ex: Exception) {
            // A broken answer must not stay in the cache, or the build would fail forever.
            file.delete()
            throw ex
        }
    }

    private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) }.filterIsInstance<Element>()
    private fun Element.elements(tag: String): List<Element> = getElementsByTagName(tag).elements()

    /** Returns the cached file for [url], downloading it first (with retries) if needed. */
    private fun cached(url: String, fileName: String): File {
        val file = cacheDir.get().asFile.resolve(fileName)
        if (file.isFile) return file

        val token = bggToken.orNull ?: throw IllegalStateException(
            "$fileName is not in the BGG cache, and no BGG API token was found. " +
                "Set the BGG_TOKEN environment variable or pass -Pbgg.token=<token> to Gradle. " +
                "Register for a token at https://boardgamegeek.com/applications"
        )

        var attempt = 0
        while (true) {
            ++attempt
            val request = HttpRequest.newBuilder(URI(url))
                .header("Authorization", "Bearer $token")
                .GET()
                .build()
            val client = httpClient ?: HttpClient.newHttpClient().also { httpClient = it }
            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
            when (response.statusCode()) {
                200 -> {
                    file.parentFile.mkdirs()
                    file.writeBytes(response.body())
                    return file
                }
                401 -> throw IllegalStateException("BGG API authentication failed (HTTP 401): check BGG_TOKEN.")
                // BGG answers 202 while it prepares the answer, and 429 when called too often.
                202, 429, 500, 502, 503 -> {
                    if (attempt >= 30) throw IllegalStateException("BGG API keeps answering HTTP ${response.statusCode()} for $url")
                    Thread.sleep(2000)
                }
                else -> throw IllegalStateException("BGG API request failed with HTTP ${response.statusCode()} for $url")
            }
        }
    }
}

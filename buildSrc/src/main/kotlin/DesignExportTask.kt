import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Exports the DesignCraft files of the games with `designcraft-cli`, into `games/<id>/` of [outputDir]:
 *
 * - `board.designcraft`, the printable board or sheet: all its pages in one `<id>-Board.pdf`, a name that stays clear once downloaded.
 * - `reminder.designcraft`, the game aids: each page as `R-<n>.png` (`R-1.png` for the first page), at [PPI],
 *   without the [BLEED] around the page, which is only there for printing.
 *
 * Every font the files use must be installed: the task fails rather than export with a substitute font.
 */
@DisableCachingByDefault(because = "Depends on the designcraft-cli and the fonts installed on the machine")
abstract class DesignExportTask : DefaultTask() {

    init {
        group = "build"
        description = "Exports the games' boards (PDF) and references (PNG) from their DesignCraft files."
    }

    @get:Inject
    protected abstract val exec: ExecOperations

    @get:Inject
    protected abstract val fileSystem: FileSystemOperations

    /** The `board.designcraft` and `reminder.designcraft` files, each in its game's directory. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val designFiles: ConfigurableFileCollection

    /** The `designcraft-cli` executable; by default, the one found in the PATH or in `~/.local/bin`. Not an input: it does not change the output. */
    @get:Internal
    abstract val cli: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val out = outputDir.get().asFile
        fileSystem.delete { delete(out) }
        val cli = findCli()

        designFiles.files.sortedBy { it.path }.forEach { file ->
            val gameOut = out.resolve("games/${file.parentFile.name}").apply { mkdirs() }
            try {
                val pages = checkAndGetPageSizes(cli, file)
                when (file.name) {
                    "board.designcraft" -> exportBoard(cli, file, gameOut.resolve("${file.parentFile.name}-Board.pdf"))
                    "reminder.designcraft" -> exportReferences(cli, file, pages, gameOut)
                    else -> error("Unexpected file")
                }
            } catch (ex: Exception) {
                throw GradleException("Could not export ${file.parentFile.name}/${file.name}: ${ex.message}", ex)
            }
        }
    }

    private fun findCli(): String {
        cli.orNull?.let { return it }
        val dirs = System.getenv("PATH").orEmpty().split(File.pathSeparator) + File(System.getProperty("user.home"), ".local/bin").path
        return dirs.filter { it.isNotEmpty() }.map { File(it, "designcraft-cli") }.firstOrNull { it.canExecute() }?.path
            ?: throw GradleException(
                "designcraft-cli was not found in the PATH nor in ~/.local/bin. " +
                    "Install it (https://github.com/storytold/designcraft/releases), " +
                    "or give its path with the DESIGNCRAFT_CLI environment variable or -Pdesigncraft.cli=<path>."
            )
    }

    /** Fails if a font of [file] is missing, and returns the size of each of its pages, in points. */
    @Suppress("UNCHECKED_CAST")
    private fun checkAndGetPageSizes(cli: String, file: File): List<Pair<Double, Double>> {
        // One JSON result per line, in the order of the commands.
        val (inspect, fonts) = designcraft(cli, "--in", file.path, "--cmd", "document.inspect", "--cmd", "font.list")
            .lines().filter { it.isNotBlank() }
            .map { Load(LoadSettings.builder().setCodePointLimit(Int.MAX_VALUE).build()).loadFromString(it) }

        val substituted = (fonts as List<Map<String, Any?>>)
            .filter { it["missing"] == true || it["styleMissing"] == true || it["matchStatus"] != "exact" }
            .map { "${it["family"]} ${it["style"]}" }
        if (substituted.isNotEmpty()) throw GradleException("Missing fonts: ${substituted.joinToString()}. Install them first.")

        return ((inspect as Map<String, Any?>)["spreads"] as List<Map<String, Any?>>)
            .flatMap { it["pages"] as List<Map<String, Any?>> }
            .map { page ->
                val (x0, y0, x1, y1) = (page["bounds"] as List<Number>).map { it.toDouble() }
                (x1 - x0) to (y1 - y0)
            }
    }

    private fun exportBoard(cli: String, file: File, pdf: File) {
        // A fixed creation date makes the same file give the same PDF, which the service worker then does not download again.
        val result = designcraft(cli, "--in", file.path, "--cmd", "file.exportPdf=" + toJson(linkedMapOf("path" to pdf.path, "created" to 0)))
        logger.info(result)
        val warnings = (Load(LoadSettings.builder().build()).loadFromString(result.lines().first { it.isNotBlank() }) as Map<*, *>)["warnings"] as List<*>?
        warnings.orEmpty().forEach { logger.warn("${file.parentFile.name}/${file.name}: $it") }
    }

    private fun exportReferences(cli: String, file: File, pages: List<Pair<Double, Double>>, out: File) {
        // Each page shrinks by the bleed on every side, around its centre, which keeps the objects in place.
        val resizes = pages.withIndex()
            .groupBy({ it.value }, { it.index + 1 })
            .flatMap { (size, numbers) ->
                val resize = linkedMapOf("pages" to numbers, "width" to size.first - 2 * BLEED, "height" to size.second - 2 * BLEED, "anchor" to 4)
                listOf("--cmd", "layout.pageSize=" + toJson(resize))
            }

        val tmp = temporaryDir.resolve(file.parentFile.name)
        fileSystem.delete { delete(tmp) }
        tmp.mkdirs()
        logger.info(designcraft(cli, "--in", file.path, *resizes.toTypedArray(), "--scale", (PPI / 72.0).toString(), "--all-pages", tmp.path))

        // page-001.png, page-002.png…
        val images = tmp.listFiles()!!.filter { it.extension == "png" }.sortedBy { it.name }
        check(images.size == pages.size) { "${pages.size} pages, but ${images.size} images exported" }
        images.forEachIndexed { index, image -> image.copyTo(out.resolve("R-${index + 1}.png"), true) }
    }

    /** Runs `designcraft-cli run` with [args], and returns what it prints. */
    private fun designcraft(cli: String, vararg args: String): String {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine(cli, "run", *args)
            standardOutput = stdout
            errorOutput = stderr
            isIgnoreExitValue = true
        }
        val output = stdout.toString(Charsets.UTF_8)
        if (result.exitValue != 0) {
            throw GradleException("designcraft-cli failed (exit ${result.exitValue}): ${stderr.toString(Charsets.UTF_8).trim().ifEmpty { output.trim() }}")
        }
        return output
    }

    companion object {
        /** The print bleed around each page of `reminder.designcraft`: 3.5 mm, in points. */
        const val BLEED = 3.5 / 25.4 * 72

        /** The resolution of the references. */
        const val PPI = 300.0
    }
}

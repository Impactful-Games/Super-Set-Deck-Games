import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

/**
 * Lists every file of the site for the service worker, which downloads them all in the background so the
 * whole site (rules, references, printables, images) works offline without visiting each page.
 *
 * The first line is a hash of the whole site, then each file comes with a hash of its content: the
 * service worker only downloads the files that are new or changed since it last cached them.
 */
@CacheableTask
abstract class PrecacheListTask : DefaultTask() {

    init {
        group = "build"
        description = "Generates the list of files the service worker caches for offline use."
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val siteDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val root = siteDir.get().asFile
        val files = root.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath to it }
            // Source maps are only for debugging, and CNAME only for GitHub Pages.
            .filterNot { (path, _) -> path.endsWith(".map") || path == "CNAME" || path == "precache.txt" || path.endsWith(".DS_Store") }
            .sortedBy { it.first }
            .toList()

        fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }.take(16)
        val entries = files.map { (path, file) -> path to hash(file.readBytes()) }
        val version = hash(entries.joinToString("\n") { (path, fileHash) -> "$path $fileHash" }.toByteArray())

        outputFile.get().asFile.writeText(
            (listOf(version) + entries.map { (path, fileHash) -> "$path $fileHash" }).joinToString("\n", postfix = "\n")
        )
    }
}

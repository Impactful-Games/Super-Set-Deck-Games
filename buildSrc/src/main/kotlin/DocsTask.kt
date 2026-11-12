import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.Options
import org.asciidoctor.SafeMode
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import java.io.File
import javax.inject.Inject

/**
 * Renders every rule (`games/<id>/<lang>.adoc`) and document (`docs/<name>.adoc`) to an HTML fragment,
 * then reshapes it for the site: table of contents in its own block, section anchors, the
 * "Show cards" button of the components block, admonitions, scrollable tables, MathML formulas,
 * links to printables, and the changelog timeline.
 *
 * The section ids are Asciidoctor's defaults, which shared links (`?section=_bombs`) rely on.
 */
@CacheableTask
abstract class DocsTask : DefaultTask() {

    init {
        group = "build"
        description = "Renders the rules and documents from AsciiDoc to HTML fragments."
    }

    @get:Inject
    protected abstract val fileSystem: FileSystemOperations

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gamesDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val out = outputDir.get().asFile
        fileSystem.delete { delete(out) }

        Asciidoctor.Factory.create().use { asciidoctor ->
            gamesDir.get().asFile.listFiles()!!.filter { it.isDirectory }.sortedBy { it.name }.forEach { dir ->
                dir.listFiles()!!.filter { it.extension == "adoc" }.forEach { file ->
                    val lang = file.nameWithoutExtension
                    val html = asciidoctor.render(file, lang)
                    val target = out.resolve("games/${dir.name}/$lang.html")
                    target.parentFile.mkdirs()
                    target.writeText(reshapeRule(html, dir.name, lang))
                }
            }
            docsDir.get().asFile.listFiles()!!.filter { it.extension == "adoc" }.forEach { file ->
                val lang = file.nameWithoutExtension.substringAfterLast('-', "en")
                val html = asciidoctor.render(file, lang)
                val target = out.resolve("docs/${file.nameWithoutExtension}.html")
                target.parentFile.mkdirs()
                target.writeText(
                    if (file.nameWithoutExtension == "changelog") reshapeChangelog(html)
                    else reshapeDocument(html, lang)
                )
            }
        }
    }

    private fun Asciidoctor.render(file: File, lang: String): String =
        convert(
            file.readText(),
            Options.builder()
                .safe(SafeMode.UNSAFE)
                .backend("html5")
                .standalone(false)
                .baseDir(file.parentFile)
                .attributes(
                    Attributes.builder()
                        .icons("font")
                        .attribute("stem", "asciimath")
                        // A trailing @ lets the document override the value (the French rules set "Sommaire").
                        .attribute("toc-title", Labels.contents(lang) + "@")
                        .attribute("lang", lang)
                        .build()
                )
                .build()
        )

    private fun parse(html: String): Element =
        Jsoup.parseBodyFragment(html)
            // Pretty-printing would add whitespace between inline elements, which shows in the text.
            .apply { outputSettings().prettyPrint(false) }
            .body()

    private fun reshapeRule(html: String, gameId: String, lang: String): String {
        val body = parse(html)
        convertMath(body)
        reshapeAdmonitions(body)
        wrapTables(body)
        reshapeComponents(body, lang)
        addSectionAnchors(body, lang)
        rewriteLinks(body) { href -> "games/$gameId/$href" }
        return layoutWithToc(body, "ssd-rules", lang)
    }

    private fun reshapeDocument(html: String, lang: String): String {
        val body = parse(html)
        convertMath(body)
        reshapeAdmonitions(body)
        wrapTables(body)
        body.select("div.openblock.ssd-suits").forEach { it.replaceWith(suitsGrid(lang)) }
        body.select("a.ssd-button").forEach { link ->
            link.prepend(Icons.svg(Icons.file))
            if (link.attr("href").contains("://")) {
                link.attr("target", "_blank").attr("rel", "noopener")
                link.append(Icons.svg(Icons.external, size = 16))
            }
        }
        rewriteLinks(body) { href -> "docs/$href" }
        body.select("#toc").remove()
        return "<div class=\"ssd-doc ssd-document\"><article class=\"ssd-content\">${body.html()}</article></div>"
    }

    /** Replaces Asciidoctor's `\$...\$` AsciiMath passthroughs with MathML. */
    private fun convertMath(body: Element) {
        val regex = Regex("""\\\$(.+?)\\\$""", RegexOption.DOT_MATCHES_ALL)
        body.select("*").flatMap { it.textNodes() }
            .filter { regex.containsMatchIn(it.wholeText) }
            .forEach { node ->
                val text = node.wholeText
                val html = buildString {
                    var last = 0
                    regex.findAll(text).forEach { match ->
                        append(Entities.escape(text.substring(last, match.range.first)))
                        append(AsciiMath.toMathML(match.groupValues[1]))
                        last = match.range.last + 1
                    }
                    append(Entities.escape(text.substring(last)))
                }
                node.before(html)
                node.remove()
            }
    }

    private fun reshapeAdmonitions(body: Element) {
        body.select("div.admonitionblock").forEach { block ->
            val kind = listOf("note", "tip", "important", "warning", "caution").firstOrNull { block.hasClass(it) } ?: "note"
            val label = block.selectFirst("td.icon i")?.attr("title")?.takeIf { it.isNotBlank() }
                ?: block.selectFirst("td.icon .title")?.text()
                ?: kind.uppercase()
            val content = block.selectFirst("td.content")?.html() ?: ""
            val id = block.id().takeIf { it.isNotEmpty() }?.let { " id=\"$it\"" } ?: ""
            block.replaceWith(
                parse(
                    """<div class="ssd-admonition ssd-admonition-$kind" role="note"$id>""" +
                        Icons.svg(Icons.admonition(kind), size = 24, cssClass = "ssd-admonition-icon") +
                        """<div class="ssd-admonition-body"><strong class="ssd-admonition-label">${Entities.escape(label)}</strong>""" +
                        """<div class="ssd-admonition-content">$content</div></div></div>"""
                ).child(0)
            )
        }
    }

    private fun wrapTables(body: Element) {
        body.select("table.tableblock").forEach { table ->
            // The frame is drawn by the scrolling wrapper, so that it scrolls with the table.
            val frame = table.classNames().firstOrNull { it.startsWith("frame-") } ?: "frame-all"
            table.wrap("<div class=\"ssd-table-scroll ssd-$frame\"></div>")
        }
    }

    private fun reshapeComponents(body: Element, lang: String) {
        body.select("div.sidebarblock.ssd-components").forEach { block ->
            val content = block.selectFirst("> div.content") ?: block
            val title = content.selectFirst("> div.title")
            val titleText = title?.text() ?: ""
            title?.remove()
            val id = block.id().takeIf { it.isNotEmpty() }?.let { " id=\"$it\"" } ?: ""
            block.replaceWith(
                parse(
                    """<aside class="ssd-components"$id aria-label="${Entities.escape(titleText)}">""" +
                        """<div class="ssd-components-header"><span class="ssd-components-title">${Entities.escape(titleText)}</span>""" +
                        """<button type="button" class="ssd-show-cards">${Icons.svg(Icons.cards)}<span>${Labels.showCards(lang)}</span></button></div>""" +
                        """<div class="ssd-components-body">${content.html()}</div></aside>"""
                ).child(0)
            )
        }
        body.select("div.sidebarblock").addClass("ssd-sidebar")
    }

    private fun addSectionAnchors(body: Element, lang: String) {
        body.select("h2[id], h3[id], h4[id]").forEach { heading ->
            heading.addClass("ssd-section-title")
            heading.attr("data-section", heading.id())
            heading.appendElement("a")
                .addClass("ssd-anchor")
                .attr("href", "#${heading.id()}")
                .attr("aria-label", Labels.linkToSection(lang, heading.text()))
                .append(Icons.svg(Icons.link, size = 18))
        }
    }

    /**
     * App links (`#/games/X`, `/#/games/X`) get the `ssd-app-link` class for in-app navigation; relative
     * links point to printable material and open it in a new tab; other sites' links stay unchanged.
     */
    private fun rewriteLinks(body: Element, relative: (String) -> String) {
        body.select("a[href]").forEach { link ->
            val href = link.attr("href")
            when {
                href.startsWith("/#/") -> link.attr("href", href.removePrefix("/")).addClass("ssd-app-link")
                href.startsWith("#/") -> link.addClass("ssd-app-link")
                href.startsWith("#") -> link.addClass("ssd-section-link")
                href.startsWith("/") || "://" in href || href.startsWith("mailto:") -> {}
                else -> link.attr("href", relative(href)).attr("target", "_blank").attr("rel", "noopener")
            }
        }
    }

    private fun layoutWithToc(body: Element, kind: String, lang: String): String {
        val toc = body.selectFirst("#toc")
        val tocHtml = toc?.let {
            val title = it.selectFirst("#toctitle")?.text() ?: Labels.contents(lang)
            it.select("a[href^=#]").forEach { a -> a.attr("data-section", a.attr("href").removePrefix("#")) }
            val list = it.selectFirst("> ul")?.outerHtml() ?: ""
            """<aside class="ssd-side"><details class="ssd-toc"><summary><span class="ssd-toc-title">${Entities.escape(title)}</span>""" +
                Icons.svg(Icons.chevronDown, cssClass = "ssd-toc-chevron") +
                """</summary><nav aria-label="${Entities.escape(title)}">$list</nav></details></aside>"""
        } ?: ""
        toc?.remove()
        // The preamble can be left empty once the table of contents is out of it.
        body.select("#preamble").filter { it.text().isBlank() && it.select("aside, table, img").isEmpty() }.forEach { it.remove() }
        return """<div class="ssd-doc $kind">$tocHtml<article class="ssd-content">${body.html()}</article></div>"""
    }

    private fun suitsGrid(lang: String): Element {
        val tiles = Labels.suits.joinToString("") { suit ->
            """<span class="ssd-suit ssd-suit-${suit.key}"><span class="ssd-suit-symbol" aria-hidden="true">${suit.symbol}</span>""" +
                """<span class="ssd-suit-count">${suit.count}</span><span class="ssd-suit-name">${suit.name(lang)}</span></span>"""
        }
        return parse("""<div class="ssd-suits" role="list" aria-label="${Labels.suitsTitle(lang)}">$tiles</div>""").child(0)
    }

    private fun reshapeChangelog(html: String): String {
        val body = parse(html)
        rewriteLinks(body) { it }
        val months = body.select("div.sect1").mapIndexed { index, section ->
            val title = section.selectFirst("> h2")?.text() ?: ""
            val label = Regex("(\\d{1,2})/(\\d{4})").matchEntire(title.trim())?.let { m ->
                "${Labels.months[m.groupValues[1].toInt() - 1]} ${m.groupValues[2]}"
            } ?: title
            val games = ArrayList<String>()
            val site = ArrayList<String>()
            section.select("> div.sectionbody > div.ulist > ul > li").forEach { item ->
                val nested = item.selectFirst("> div.ulist")
                val text = item.selectFirst("> p")?.text()?.trim() ?: ""
                if (nested != null && text.removeSuffix(":").trim().lowercase() in setOf("games", "jeux")) {
                    nested.select("a").forEach { a ->
                        games += """<a class="ssd-game-chip ssd-app-link" href="${a.attr("href")}">${Icons.svg(Icons.cards, size = 18)}<span>${a.html()}</span></a>"""
                    }
                } else {
                    site += "<li>${item.html()}</li>"
                }
            }
            buildString {
                append("""<section class="ssd-month"><div class="ssd-month-rail" aria-hidden="true">""")
                append("""<svg class="ssd-month-dot ssd-month-dot-${index % 2}" viewBox="0 0 100 100" width="44" height="44"><path d="${Icons.cookie9}"/></svg>""")
                append("""<span class="ssd-month-line"></span></div><div class="ssd-month-body"><h2>${Entities.escape(label)}</h2><div class="ssd-month-card">""")
                if (games.isNotEmpty()) {
                    append("""<span class="ssd-month-label">New games</span><div class="ssd-month-games">${games.joinToString("")}</div>""")
                }
                if (site.isNotEmpty()) {
                    append("""<span class="ssd-month-label">Site</span><ul>${site.joinToString("")}</ul>""")
                }
                append("</div></div></section>")
            }
        }
        return """<div class="ssd-doc ssd-changelog"><div class="ssd-months">${months.joinToString("")}</div></div>"""
    }
}

private object Labels {
    fun contents(lang: String) = if (lang == "fr") "Sommaire" else "Contents"
    fun showCards(lang: String) = if (lang == "fr") "Voir les cartes" else "Show cards"
    fun linkToSection(lang: String, title: String) =
        Entities.escape(if (lang == "fr") "Lien vers « $title »" else "Link to \"$title\"")
    fun suitsTitle(lang: String) = if (lang == "fr") "Les 198 cartes par couleur" else "The 198 cards by suit"

    class Suit(val key: String, val symbol: String, val count: Int, val en: String, val fr: String) {
        fun name(lang: String) = if (lang == "fr") fr else en
    }

    val suits = listOf(
        Suit("stars", "★", 32, "Stars", "Étoiles"),
        Suit("spades", "♠", 26, "Spades", "Piques"),
        Suit("hearts", "♥", 26, "Hearts", "Cœurs"),
        Suit("clubs", "♣", 26, "Clubs", "Trèfles"),
        Suit("diamonds", "♦", 26, "Diamonds", "Carreaux"),
        Suit("florettes", "✿", 22, "Florettes", "Fleurettes"),
        Suit("wheels", "⎈", 15, "Wheels", "Roues"),
        Suit("specials", "Ø", 25, "Specials", "Spéciales"),
    )

    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
}

/** Stroke icons drawn on a 24×24 grid, the same set the app uses. */
private object Icons {
    const val link = """<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>"""
    const val cards = """<rect x="4" y="3" width="11" height="16" rx="2"/><path d="M18 6.5l1.6.4a2 2 0 0 1 1.4 2.5l-2.8 10.4a2 2 0 0 1-2.5 1.4L9 19"/>"""
    const val chevronDown = """<path d="M6 9l6 6 6-6"/>"""
    const val file = """<path d="M6 3h8l4 4v14H6z"/><path d="M14 3v4h4"/>"""
    const val external = """<path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/>"""
    private const val note = """<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>"""
    private const val tip = """<path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"/>"""
    private const val important = """<circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16.5h.01"/>"""
    private const val warning = """<path d="M12 3l9.5 17h-19z"/><path d="M12 10v4M12 17.5h.01"/>"""
    private const val caution = """<path d="M8.5 3h7L21 8.5v7L15.5 21h-7L3 15.5v-7z"/><path d="M12 8v5M12 16.5h.01"/>"""

    const val cookie9 = "M50.0 5.0 53.1 2.9 56.4 1.7 59.5 2.0 62.4 3.8 64.7 6.6 66.6 9.8 68.4 12.6 70.5 14.5 73.2 15.3 76.5 15.5 80.2 15.6 83.8 16.2 86.8 17.7 88.6 20.4 89.3 23.8 89.0 27.5 88.4 31.1 88.2 34.2 88.9 36.8 90.7 39.1 93.4 41.4 96.1 43.9 98.2 46.8 99.0 50.0 98.2 53.2 96.1 56.1 93.4 58.6 90.7 60.9 88.9 63.2 88.2 65.8 88.4 68.9 89.0 72.5 89.3 76.2 88.6 79.6 86.8 82.3 83.8 83.8 80.2 84.4 76.5 84.5 73.2 84.7 70.5 85.5 68.4 87.4 66.6 90.2 64.7 93.4 62.4 96.2 59.5 98.0 56.4 98.3 53.1 97.1 50.0 95.0 47.2 92.7 44.6 91.0 42.0 90.3 39.1 90.7 35.8 91.9 32.2 93.0 28.6 93.3 25.5 92.4 23.2 90.2 21.7 86.9 20.8 83.2 20.2 79.8 19.1 77.1 17.2 75.1 14.4 73.8 11.0 72.5 7.6 70.9 5.0 68.6 3.7 65.7 3.8 62.4 5.1 58.9 6.9 55.7 8.4 52.7 9.0 50.0 8.4 47.3 6.9 44.3 5.1 41.1 3.8 37.6 3.7 34.3 5.0 31.4 7.6 29.1 11.0 27.5 14.4 26.2 17.2 24.9 19.1 22.9 20.2 20.2 20.8 16.8 21.7 13.1 23.2 9.8 25.5 7.6 28.6 6.7 32.2 7.0 35.8 8.1 39.1 9.3 42.0 9.7 44.6 9.0 47.2 7.3Z"

    fun admonition(kind: String) = when (kind) {
        "tip" -> tip
        "important" -> important
        "warning" -> warning
        "caution" -> caution
        else -> note
    }

    fun svg(content: String, size: Int = 20, cssClass: String = "ssd-icon") =
        """<svg class="$cssClass" viewBox="0 0 24 24" width="$size" height="$size" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">$content</svg>"""
}

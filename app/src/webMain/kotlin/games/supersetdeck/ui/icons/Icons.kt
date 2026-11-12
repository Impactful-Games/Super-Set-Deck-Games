package games.supersetdeck.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The design's icons: 24×24 strokes of width 2 with round caps, plus a few filled glyphs. */
object Icons {

    private fun circle(cx: Double, cy: Double, r: Double) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun rect(x: Double, y: Double, w: Double, h: Double, r: Double) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    private fun stroke(name: String, vararg paths: String, filled: List<String> = emptyList()) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            filled.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()

    private fun fill(name: String, vararg paths: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()

    val Games = stroke("games", rect(4.0, 3.0, 11.0, 16.0, 2.0), "M18 6.5l1.6.4a2 2 0 0 1 1.4 2.5l-2.8 10.4a2 2 0 0 1-2.5 1.4L9 19")
    val Bookmark = stroke("bookmark", "M7 3h10a1 1 0 0 1 1 1v17l-6-4-6 4V4a1 1 0 0 1 1-1z")
    val Luggage = stroke("luggage", rect(3.0, 7.0, 18.0, 13.0, 3.0), "M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M3 13h18")
    val Info = stroke("info", circle(12.0, 12.0, 9.0), "M12 11v5M12 8h.01")
    val History = stroke("history", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5M12 7v5l3 2")
    val Globe = stroke("globe", circle(12.0, 12.0, 9.0), "M3 12h18M12 3c2.5 2.7 3.8 5.7 3.8 9s-1.3 6.3-3.8 9c-2.5-2.7-3.8-5.7-3.8-9S9.5 5.7 12 3z")
    val ChevronDown = stroke("chevronDown", "M6 9l6 6 6-6")
    val ChevronUp = stroke("chevronUp", "M6 15l6-6 6 6")
    val ChevronLeft = stroke("chevronLeft", "M15 6l-6 6 6 6")
    val ChevronRight = stroke("chevronRight", "M9 6l6 6-6 6")
    val External = stroke("external", "M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5")
    val Search = stroke("search", circle(11.0, 11.0, 7.0), "M20 20l-3.5-3.5")
    val Sort = stroke("sort", "M4 6h16M7 12h10M10 18h4")
    val Players = stroke("players", circle(9.0, 8.0, 3.5), "M2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 7M18 14.5a6.5 6.5 0 0 1 3.5 5.5")
    val Tag = stroke("tag", "M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9-9-9z", circle(8.0, 8.0, 1.5))
    val Close = stroke("close", "M6 6l12 12M18 6L6 18")
    val Check = stroke("check", "M5 12l5 5L20 7")
    val Plus = stroke("plus", "M12 5v14M5 12h14")
    val ArrowBack = stroke("arrowBack", "M19 12H5M11 6l-6 6 6 6")
    val ArrowForward = stroke("arrowForward", "M5 12h14M13 6l6 6-6 6")
    val Share = stroke("share", circle(18.0, 5.0, 2.5), circle(6.0, 12.0, 2.5), circle(18.0, 19.0, 2.5), "M8.2 10.8l7.6-4.4M8.2 13.2l7.6 4.4")
    val Message = stroke("message", "M4 5h16v11H8l-4 4z")
    val QrCode = stroke("qrCode", rect(3.0, 3.0, 7.0, 7.0, 1.0), rect(14.0, 3.0, 7.0, 7.0, 1.0), rect(3.0, 14.0, 7.0, 7.0, 1.0), "M14 14h3v3M21 14v.01M14 21h7v-4")
    val Link = stroke("link", "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1", "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val Calendar = stroke("calendar", rect(3.0, 5.0, 18.0, 16.0, 2.0), "M3 10h18M8 3v4M16 3v4")
    val Person = stroke("person", circle(12.0, 7.0, 4.0), "M5 21a7 7 0 0 1 14 0")
    val Clock = stroke("clock", circle(12.0, 12.0, 9.0), "M12 7v5l3 2")
    val Star = fill("star", "M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z")
    val Heart = fill("heart", "M12 20s-7-4.4-9-9a4.8 4.8 0 0 1 9-3 4.8 4.8 0 0 1 9 3c-2 4.6-9 9-9 9z")
    val Book = stroke("book", "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z", "M4 19V5")
    val Image = stroke("image", rect(3.0, 4.0, 18.0, 16.0, 2.0), circle(9.0, 10.0, 2.0), "M21 16l-5-5-9 9")
    val Edit = stroke("edit", "M4 20h4L19 9l-4-4L4 16v4z", "M13.5 6.5l4 4")
    val Delete = stroke("delete", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Cloud = stroke("cloud", "M7 18a5 5 0 1 1 1-9.9A6 6 0 0 1 19.5 10 4 4 0 0 1 18 18H7z")
    val CloudUpload = stroke("cloudUpload", "M7 18a5 5 0 1 1 1-9.9A6 6 0 0 1 19.5 10 4 4 0 0 1 18 18H7z", "M12 15V10M9.5 12.5 12 10l2.5 2.5")
    val CloudOff = stroke("cloudOff", "M7 18a5 5 0 1 1 1-9.9A6 6 0 0 1 19.5 10 4 4 0 0 1 18 18H7z", "M10 11l4 4M14 11l-4 4")
    val File = stroke("file", "M6 3h8l4 4v14H6z", "M14 3v4h4")
    val Folder = stroke("folder", "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val Sync = stroke("sync", "M4 12a8 8 0 0 1 14-5.3L20 9M20 4v5h-5M20 12a8 8 0 0 1-14 5.3L4 15M4 20v-5h5")
    val Pause = stroke("pause", "M9 6v12M15 6v12")
    val Fullscreen = stroke("fullscreen", "M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5")
    val DualView = stroke("dualView", rect(3.0, 5.0, 8.0, 14.0, 1.5), rect(13.0, 5.0, 8.0, 14.0, 1.5))
    val Grid = stroke("grid", rect(3.0, 3.0, 7.0, 7.0, 1.5), rect(14.0, 3.0, 7.0, 7.0, 1.5), rect(3.0, 14.0, 7.0, 7.0, 1.5), rect(14.0, 14.0, 7.0, 7.0, 1.5))
    val Warning = stroke("warning", "M12 3l9.5 17h-19z", "M12 10v4M12 17.5h.01")
    val Error = stroke("error", circle(12.0, 12.0, 9.0), "M12 7v6M12 16.5h.01")
    val Sun = stroke("sun", circle(12.0, 12.0, 4.0), "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4")
    val Moon = stroke("moon", "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")
    val Contrast = stroke("contrast", circle(12.0, 12.0, 9.0), filled = listOf("M12 3a9 9 0 0 0 0 18z"))

    /** The same strokes as SVG markup, for the few elements drawn in HTML. */
    const val CHECK_SVG = """<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12l5 5L20 7"/></svg>"""
    const val CLOSE_SVG = """<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6l12 12M18 6L6 18"/></svg>"""
    const val ERROR_SVG = """<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16.5h.01"/></svg>"""
}

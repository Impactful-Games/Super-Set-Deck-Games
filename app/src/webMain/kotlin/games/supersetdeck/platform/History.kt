package games.supersetdeck.platform

private fun jsHash(): String = js("window.location.hash")

/** The current location hash, `#` included (`#/games/Tichu?page=references`). */
fun currentHash(): String = jsHash()

private fun jsHistoryState(): String? = js("""
    (function () { var s = window.history.state; return s && s.ssd ? JSON.stringify(s.ssd) : null; })()
""")

/** The app's own JSON state attached to the current history entry, if any. */
fun historyState(): String? = jsHistoryState()

private fun jsPushState(state: String, hash: String): Unit = js("window.history.pushState({ ssd: JSON.parse(state) }, '', hash)")
private fun jsReplaceState(state: String, hash: String): Unit = js("window.history.replaceState({ ssd: JSON.parse(state) }, '', hash)")
private fun jsBack(): Unit = js("window.history.back()")

fun pushHistory(state: String, hash: String) = jsPushState(state, hash)
fun replaceHistory(state: String, hash: String) = jsReplaceState(state, hash)
fun historyBack() = jsBack()

private fun jsOnPopState(callback: () -> Unit): Unit = js("""
    window.addEventListener("popstate", function () { callback(); })
""")

/** Back / forward, and a hash typed in the address bar (which also fires `popstate`). */
fun onPopState(callback: () -> Unit) = jsOnPopState(callback)

private fun jsDecodeComponent(value: String): String = js("""
    (function () { try { return decodeURIComponent(value.replace(/\+/g, " ")); } catch (e) { return value; } })()
""")

/** Decodes a query string component, `+` meaning a space as in forms and `URLSearchParams`. */
fun decodeQueryComponent(value: String): String = jsDecodeComponent(value)

private fun jsEncodeComponent(value: String): String = js("""
    encodeURIComponent(value).replace(/%2C/g, ",").replace(/%2F/g, "/")
""")

/** Encodes a query string component, keeping `,` and `/` readable. */
fun encodeQueryComponent(value: String): String = jsEncodeComponent(value)

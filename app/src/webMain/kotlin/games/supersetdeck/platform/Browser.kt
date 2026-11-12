@file:Suppress("unused")

package games.supersetdeck.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

// Every function in this file is a single `js(...)` call: that is the shape both Kotlin/JS and
// Kotlin/Wasm accept for inline JavaScript, so the same code runs in both builds. Kotlin/Wasm makes
// it the body of an arrow function, so it must be an expression: statements go in an IIFE.
// Kotlin/JS re-prints the code: `new X(…).f()` must be written `(new X(…)).f()`, or it becomes `new (X(…).f)()`.

class HttpException(val status: Int, message: String) : Exception(message)

private fun jsFetchText(url: String): Promise<JsString> = js("""
    fetch(url).then(function (response) {
        if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
        return response.text();
    })
""")

private fun jsFetchBuffer(url: String): Promise<JsAny> = js("""
    fetch(url).then(function (response) {
        if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
        return response.arrayBuffer();
    })
""")

/** Downloads [url] as text, failing on any non-2xx status. */
suspend fun fetchText(url: String): String = jsFetchText(url).await<JsString>().toString()

/** Downloads [url] as bytes, failing on any non-2xx status. */
suspend fun fetchBytes(url: String): ByteArray = jsFetchBuffer(url).await<JsAny>().arrayBufferToByteArray()

/** Converts a JS `ArrayBuffer` to a Kotlin [ByteArray] (a view on JS, a copy into linear memory on Wasm). */
expect fun JsAny.arrayBufferToByteArray(): ByteArray

/**
 * Message of a JS or Kotlin error, as shown to the user. Kotlin/Wasm wraps JS errors as
 * "Non-Kotlin exception Error: … of type 'class Error'": only the JS message is kept.
 */
fun Throwable.userMessage(): String {
    val raw = message?.takeIf { it.isNotBlank() } ?: return this::class.simpleName ?: "Unknown error"
    return raw.removePrefix("Non-Kotlin exception ")
        .replace(Regex(" of type '[^']*'$"), "")
        .removePrefix("Error: ")
        .trim()
        .removeSuffix(".")
}

// region Storage

private fun jsStorageGet(key: String): String? = js("""
    (function () { try { return window.localStorage.getItem(key); } catch (e) { return null; } })()
""")

private fun jsStorageSet(key: String, value: String): Unit = js("""
    (function () { try { window.localStorage.setItem(key, value); } catch (e) {} })()
""")

object LocalStorage {
    operator fun get(key: String): String? = jsStorageGet(key)
    operator fun set(key: String, value: String) = jsStorageSet(key, value)
}

// endregion

// region Navigator

private fun jsPreferredLanguages(): String = js("""
    (navigator.languages && navigator.languages.length ? Array.from(navigator.languages) : [navigator.language || ""]).join(",")
""")

/** The browser's preferred languages, most preferred first (`fr-CA`, `en`, …). */
fun preferredLanguages(): List<String> = jsPreferredLanguages().split(",").filter { it.isNotBlank() }

private fun jsCanShare(): Boolean = js("typeof navigator.share === 'function'")

/** Whether the browser can open the system share sheet. */
fun canShareNatively(): Boolean = jsCanShare()

private fun jsShare(title: String, text: String, url: String): Unit = js("""
    navigator.share({ title: title, text: text, url: url }).catch(function () {})
""")

fun shareNatively(title: String, text: String, url: String) = jsShare(title, text, url)

private fun jsCopy(text: String): Promise<JsAny?> = js("""
    (function () {
        if (navigator.clipboard && navigator.clipboard.writeText) return navigator.clipboard.writeText(text);
        return new Promise(function (resolve, reject) {
            var area = document.createElement("textarea");
            area.value = text;
            area.style.position = "fixed";
            area.style.opacity = "0";
            document.body.appendChild(area);
            area.select();
            var ok = document.execCommand("copy");
            document.body.removeChild(area);
            if (ok) resolve(null); else reject(new Error("Copy refused"));
        });
    })()
""")

/** Copies [text] to the clipboard; returns whether it worked. */
suspend fun copyToClipboard(text: String): Boolean =
    try {
        jsCopy(text).await<JsAny?>()
        true
    } catch (_: Throwable) {
        false
    }

// endregion

// region Formatting

private fun jsFormatDateTime(epochMillis: Double, lang: String): String = js("""
    (new Intl.DateTimeFormat(lang, { dateStyle: "medium", timeStyle: "short" })).format(new Date(epochMillis))
""")

/** Medium date and short time, in [lang] (`Nov 3, 2026, 9:12 PM`). */
fun formatDateTime(epochMillis: Double, lang: String): String = jsFormatDateTime(epochMillis, lang)

private fun jsFormatOneDecimal(value: Double, lang: String): String = js("""
    (new Intl.NumberFormat(lang, { minimumFractionDigits: 1, maximumFractionDigits: 1 })).format(value)
""")

/** [value] with exactly one decimal, in [lang] (`7.6` / `7,6`). */
fun formatOneDecimal(value: Double, lang: String): String = jsFormatOneDecimal(value, lang)

private fun jsNormalize(text: String): String = js("""
    text.normalize("NFKD").replace(/[̀-ͯ]/g, "").toLowerCase()
""")

/** NFKD decomposition, diacritics removed, lower case: the form searches compare. */
fun normalizeForSearch(text: String): String = jsNormalize(text)

private fun jsNow(): Double = js("Date.now()")

/** Milliseconds since the Unix epoch. */
fun nowMillis(): Double = jsNow()

private fun jsRandomUuid(): String = js("""
    (typeof crypto !== "undefined" && crypto.randomUUID) ? crypto.randomUUID() :
    "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0;
        return (c === "x" ? r : (r & 0x3 | 0x8)).toString(16);
    })
""")

fun randomUuid(): String = jsRandomUuid()

// endregion

// region Theme

private fun jsPrefersDark(): Boolean = js("window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches")

fun prefersDarkScheme(): Boolean = jsPrefersDark()

private fun jsOnPrefersDarkChange(callback: (Boolean) -> Unit): Unit = js("""
    (function () {
        if (window.matchMedia) window.matchMedia('(prefers-color-scheme: dark)').addEventListener("change", function (e) { callback(e.matches); });
    })()
""")

fun onPrefersDarkSchemeChange(callback: (Boolean) -> Unit) = jsOnPrefersDarkChange(callback)

private fun jsSetThemeColor(color: String): Unit = js("""
    (function () {
        var meta = document.querySelector('meta[name="theme-color"]');
        if (meta) meta.setAttribute("content", color);
        document.documentElement.style.backgroundColor = color;
    })()
""")

/** Colors the browser's chrome and the page behind the canvas. */
fun setThemeColor(color: String) = jsSetThemeColor(color)

private fun jsSetDocumentLang(lang: String): Unit = js("document.documentElement.setAttribute('lang', lang)")

fun setDocumentLang(lang: String) = jsSetDocumentLang(lang)

// endregion

// region Fullscreen & wake lock

private fun jsRequestFullscreen(): Unit = js("""
    (function () {
        var e = document.documentElement;
        var request = e.requestFullscreen || e.webkitRequestFullscreen;
        if (request) { try { var p = request.call(e); if (p && p.catch) p.catch(function () {}); } catch (err) {} }
    })()
""")

/** Asks the browser for native full screen, if it can. */
fun requestFullscreen() = jsRequestFullscreen()

private fun jsExitFullscreen(): Unit = js("""
    (function () {
        if (!(document.fullscreenElement || document.webkitFullscreenElement)) return;
        var exit = document.exitFullscreen || document.webkitExitFullscreen;
        if (exit) { try { var p = exit.call(document); if (p && p.catch) p.catch(function () {}); } catch (err) {} }
    })()
""")

fun exitFullscreen() = jsExitFullscreen()

private fun jsIsFullscreen(): Boolean = js("!!(document.fullscreenElement || document.webkitFullscreenElement)")

fun isFullscreen(): Boolean = jsIsFullscreen()

private fun jsOnFullscreenChange(callback: () -> Unit): JsAny = js("""
    (function () {
        var listener = function () { callback(); };
        document.addEventListener("fullscreenchange", listener);
        document.addEventListener("webkitfullscreenchange", listener);
        return function () {
            document.removeEventListener("fullscreenchange", listener);
            document.removeEventListener("webkitfullscreenchange", listener);
        };
    })()
""")

private fun jsCallDisposer(disposer: JsAny): Unit = js("disposer()")

/** A JS-side registration (listener, observer…) that can be removed. */
class Disposer internal constructor(private val disposer: JsAny) {
    fun dispose() = jsCallDisposer(disposer)
}

fun onFullscreenChange(callback: () -> Unit): Disposer = Disposer(jsOnFullscreenChange(callback))

// Behaves as NoSleep.js (https://github.com/richtr/NoSleep.js, MIT), which the previous version
// used: the native Wake Lock API where it exists, except on Samsung Internet, which exposes it
// without honouring it; elsewhere a silent looping video, whose files come from NoSleep.js
// (nosleep/LICENSE.txt). The lock is taken again whenever the page becomes visible again, enters
// or leaves full screen, or the system releases it.
private fun jsKeepScreenOn(): JsAny = js("""
    (function () {
        var released = false;
        var sentinel = null;
        var video = null;
        var native = ("wakeLock" in navigator) && navigator.userAgent.indexOf("Samsung") === -1;
        var playVideo = function () {
            if (!video) {
                video = document.createElement("video");
                video.setAttribute("title", "No Sleep");
                video.setAttribute("playsinline", "");
                [["webm", "nosleep/nosleep.webm"], ["mp4", "nosleep/nosleep.mp4"]].forEach(function (s) {
                    var source = document.createElement("source");
                    source.src = s[1];
                    source.type = "video/" + s[0];
                    video.appendChild(source);
                });
                // iOS only counts a video that is in the document.
                video.style.position = "absolute";
                video.style.left = "-100%";
                video.style.top = "-100%";
                video.addEventListener("loadedmetadata", function () {
                    if (video.duration <= 1) video.setAttribute("loop", "");
                    else video.addEventListener("timeupdate", function () { if (video.currentTime > 0.5) video.currentTime = Math.random(); });
                });
                document.body.appendChild(video);
            }
            var playing = video.play();
            if (playing && playing.catch) playing.catch(function () {});
        };
        var acquire = function () {
            if (released || document.visibilityState !== "visible") return;
            if (!native) { playVideo(); return; }
            if (sentinel) return;
            navigator.wakeLock.request("screen").then(function (s) {
                if (released) { s.release(); return; }
                sentinel = s;
                s.addEventListener("release", function () {
                    sentinel = null;
                    if (!released && document.visibilityState === "visible") acquire();
                });
            }).catch(function () { playVideo(); });
        };
        acquire();
        document.addEventListener("visibilitychange", acquire);
        document.addEventListener("fullscreenchange", acquire);
        document.addEventListener("webkitfullscreenchange", acquire);
        return function () {
            released = true;
            document.removeEventListener("visibilitychange", acquire);
            document.removeEventListener("fullscreenchange", acquire);
            document.removeEventListener("webkitfullscreenchange", acquire);
            if (sentinel) { var s = sentinel; sentinel = null; s.release().catch(function () {}); }
            if (video) { video.pause(); if (video.parentNode) video.parentNode.removeChild(video); video = null; }
        };
    })()
""")

/**
 * Keeps the screen awake while the references are shown. Started during the click that opens them:
 * the video fallback can only start playing from a user gesture.
 */
object ScreenAwake {
    private var disposer: Disposer? = null

    fun start() {
        if (disposer == null) disposer = Disposer(jsKeepScreenOn())
    }

    fun stop() {
        disposer?.dispose()
        disposer = null
    }
}

// endregion

// region Window

private fun jsOnKeyDown(callback: (String) -> Unit): JsAny = js("""
    (function () {
        var listener = function (e) { callback(e.key); };
        window.addEventListener("keydown", listener);
        return function () { window.removeEventListener("keydown", listener); };
    })()
""")

fun onKeyDown(callback: (String) -> Unit): Disposer = Disposer(jsOnKeyDown(callback))

private fun jsOpenUrl(url: String): Unit = js("window.open(url, '_blank', 'noopener')")

fun openInNewTab(url: String) = jsOpenUrl(url)

private fun jsLoadScript(url: String): Promise<JsAny?> = js("""
    new Promise(function (resolve, reject) {
        var existing = document.querySelector('script[src="' + url + '"]');
        if (existing && existing.dataset.loaded) { resolve(null); return; }
        var script = existing || document.createElement("script");
        script.addEventListener("load", function () { script.dataset.loaded = "1"; resolve(null); });
        script.addEventListener("error", function () {
            // Removed, so that a later attempt (back online, say) loads it again instead of waiting forever.
            if (script.parentNode) script.parentNode.removeChild(script);
            reject(new Error("Could not load " + url));
        });
        if (!existing) { script.src = url; script.async = true; document.head.appendChild(script); }
    })
""")

suspend fun loadScript(url: String) {
    jsLoadScript(url).await<JsAny?>()
}

// endregion

private fun jsParseDate(iso: String): Double = js("Date.parse(iso)")

/** Milliseconds since the Unix epoch of an ISO 8601 date (as Google Drive returns them). */
fun parseDate(iso: String): Double = jsParseDate(iso)

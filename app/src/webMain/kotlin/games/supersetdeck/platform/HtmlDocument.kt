package games.supersetdeck.platform

import org.w3c.dom.HTMLElement

// The rendered rules and documents are real HTML, laid over the Compose canvas by HtmlElementView.
// The page itself scrolls in Compose, so the HTML is given its full height and the wheel and touch
// gestures it receives are handed back to the Compose scroll.

private fun jsCreateDocumentHost(): HTMLElement = js("""
    (function () {
        var host = document.createElement("div");
        host.className = "ssd-host";
        var root = document.createElement("div");
        root.className = "ssd-root";
        host.appendChild(root);
        return host;
    })()
""")

/** A host element (sized by Compose) holding the document root (sized by its content). */
fun createDocumentHost(): HTMLElement = jsCreateDocumentHost()

private fun jsSetDocumentHtml(host: HTMLElement, html: String): Unit = js("""
    host.firstChild.innerHTML = html
""")

fun setDocumentHtml(host: HTMLElement, html: String) = jsSetDocumentHtml(host, html)

private fun jsSetDocumentWidth(host: HTMLElement, width: Double): Unit = js("""
    host.firstChild.style.width = width + "px"
""")

/** The root is laid out at Compose's width even before the host is placed, so its height can be measured. */
fun setDocumentWidth(host: HTMLElement, width: Double) = jsSetDocumentWidth(host, width)

private fun jsObserveDocumentHeight(host: HTMLElement, callback: (Double) -> Unit): JsAny = js("""
    (function () {
        var root = host.firstChild;
        var observer = new ResizeObserver(function () { callback(root.offsetHeight); });
        observer.observe(root);
        callback(root.offsetHeight);
        return function () { observer.disconnect(); };
    })()
""")

fun observeDocumentHeight(host: HTMLElement, callback: (Double) -> Unit): Disposer =
    Disposer(jsObserveDocumentHeight(host, callback))

private fun jsSetDocumentStyle(host: HTMLElement, css: String): Unit = js("""
    host.style.cssText = host.style.cssText.replace(/--ssd-[^;]*;?/g, "") + css
""")

/** Sets the theme's CSS custom properties (`--ssd-primary: #FFA726;` …) on the host. */
fun setDocumentStyle(host: HTMLElement, css: String) = jsSetDocumentStyle(host, css)

private fun jsSetDocumentHidden(host: HTMLElement, hidden: Boolean): Unit = js("""
    (function () {
        host.style.visibility = hidden ? "hidden" : "";
        // Compose's wrapper around the host stays laid over the canvas, invisible but still in the
        // way of the pointer: without this, the dialog under it would get neither clicks nor wheel.
        if (host.parentElement) host.parentElement.style.pointerEvents = hidden ? "none" : "";
    })()
""")

/**
 * Hides the document while a Compose dialog or menu is open: the canvas is drawn below the HTML.
 * The pointer then goes through it, to the dialog.
 */
fun setDocumentHidden(host: HTMLElement, hidden: Boolean) = jsSetDocumentHidden(host, hidden)

private fun jsSetDocumentWide(host: HTMLElement, wide: Boolean): Unit = js("""
    (function () {
        var root = host.firstChild;
        var changed = root.classList.contains("ssd-wide") !== wide;
        root.classList.toggle("ssd-wide", wide);
        var toc = root.querySelector("details.ssd-toc");
        if (toc && (changed || wide)) toc.open = wide;
    })()
""")

/** Wide layout: the table of contents becomes an always open side column. */
fun setDocumentWide(host: HTMLElement, wide: Boolean) = jsSetDocumentWide(host, wide)

private fun jsSetActiveSection(host: HTMLElement, section: String?): Unit = js("""
    (function () {
        var root = host.firstChild;
        root.querySelectorAll(".ssd-toc a.ssd-active").forEach(function (a) { a.classList.remove("ssd-active"); a.removeAttribute("aria-current"); });
        if (section) {
            root.querySelectorAll(".ssd-toc a").forEach(function (a) {
                if (a.dataset.section === section) { a.classList.add("ssd-active"); a.setAttribute("aria-current", "location"); }
            });
        }
    })()
""")

fun setActiveSection(host: HTMLElement, section: String?) = jsSetActiveSection(host, section)

private fun jsSectionOffset(host: HTMLElement, section: String): Double = js("""
    (function () {
        var root = host.firstChild;
        var target = null;
        try { target = root.querySelector("#" + CSS.escape(section)); } catch (e) {}
        if (!target) return -1;
        return target.getBoundingClientRect().top - root.getBoundingClientRect().top;
    })()
""")

/** The section's top, relative to the document's top; negative if it does not exist. */
fun sectionOffset(host: HTMLElement, section: String): Double = jsSectionOffset(host, section)

private fun jsSectionHeight(host: HTMLElement, section: String): Double = js("""
    (function () {
        var root = host.firstChild;
        var target = null;
        try { target = root.querySelector("#" + CSS.escape(section)); } catch (e) {}
        if (!target) return 0;
        var block = target.closest(".sect1, .sect2, .sect3, .sect4, .sect5") || target;
        if (block.querySelector("#" + CSS.escape(section)) !== target && block !== target) block = target;
        return block.getBoundingClientRect().bottom - target.getBoundingClientRect().top;
    })()
""")

/** The height of the section block whose title has the [section] id. */
fun sectionHeight(host: HTMLElement, section: String): Double = jsSectionHeight(host, section)

private fun jsInstallDocumentListeners(
    host: HTMLElement,
    onSection: (String) -> Unit,
    onAppLink: (String) -> Unit,
    onShowCards: () -> Unit,
    onScroll: (Double) -> Unit,
    onFling: (Double) -> Unit,
    onTouchStart: () -> Unit,
    onFocus: (Double, Double) -> Unit,
): JsAny = js("""
    (function () {
        var focus = function (e) {
            var target = e.target;
            if (!(target instanceof Element)) return;
            var box = target.getBoundingClientRect();
            onFocus(box.top - host.firstChild.getBoundingClientRect().top, box.height);
        };
        var click = function (e) {
            var target = e.target;
            if (!(target instanceof Element)) return;
            if (target.closest(".ssd-show-cards")) { e.preventDefault(); onShowCards(); return; }
            var link = target.closest("a");
            if (link) {
                var href = link.getAttribute("href") || "";
                if (href.indexOf("#/") === 0) { e.preventDefault(); onAppLink(href.substring(1)); return; }
                if (href.indexOf("#") === 0) { e.preventDefault(); onSection(decodeURIComponent(href.substring(1))); return; }
                return;
            }
            var heading = target.closest(".ssd-section-title");
            if (heading && !String(window.getSelection() || "")) { onSection(heading.dataset.section); }
        };
        var wheel = function (e) {
            if (e.ctrlKey) return;
            if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return;
            var factor = e.deltaMode === 1 ? 16 : (e.deltaMode === 2 ? window.innerHeight : 1);
            e.preventDefault();
            onScroll(e.deltaY * factor);
        };
        var startX = 0, startY = 0, lastY = 0, lastT = 0, mode = null, velocity = 0;
        var touchStart = function (e) {
            if (e.touches.length !== 1) { mode = "none"; return; }
            var t = e.touches[0];
            startX = t.clientX; startY = lastY = t.clientY; lastT = e.timeStamp;
            mode = null; velocity = 0;
            onTouchStart();
        };
        var touchMove = function (e) {
            if (mode === "none" || e.touches.length !== 1) return;
            var t = e.touches[0];
            if (mode === null) {
                var dx = Math.abs(t.clientX - startX), dy = Math.abs(t.clientY - startY);
                if (dx < 8 && dy < 8) return;
                mode = dy >= dx ? "v" : "h";
                lastY = t.clientY; lastT = e.timeStamp;
            }
            if (mode !== "v") return;
            e.preventDefault();
            var delta = lastY - t.clientY;
            var dt = Math.max(1, e.timeStamp - lastT);
            velocity = 0.7 * (delta / dt * 1000) + 0.3 * velocity;
            lastY = t.clientY; lastT = e.timeStamp;
            onScroll(delta);
        };
        var touchEnd = function () {
            if (mode === "v") onFling(velocity);
            mode = null;
        };
        host.addEventListener("click", click);
        host.addEventListener("wheel", wheel, { passive: false });
        host.addEventListener("touchstart", touchStart, { passive: true });
        host.addEventListener("touchmove", touchMove, { passive: false });
        host.addEventListener("touchend", touchEnd, { passive: true });
        host.addEventListener("touchcancel", touchEnd, { passive: true });
        host.addEventListener("focusin", focus);
        return function () {
            host.removeEventListener("focusin", focus);
            host.removeEventListener("click", click);
            host.removeEventListener("wheel", wheel);
            host.removeEventListener("touchstart", touchStart);
            host.removeEventListener("touchmove", touchMove);
            host.removeEventListener("touchend", touchEnd);
            host.removeEventListener("touchcancel", touchEnd);
        };
    })()
""")

/**
 * Section links, app links (`#/games/X`) and the "Show cards" button call back into the app;
 * vertical wheel and touch gestures are forwarded as scroll deltas and fling velocities (CSS px);
 * a focused element (keyboard navigation) is reported by its top and height in the document, to be
 * brought into view by the page's scroll.
 */
fun installDocumentListeners(
    host: HTMLElement,
    onSection: (String) -> Unit,
    onAppLink: (String) -> Unit,
    onShowCards: () -> Unit,
    onScroll: (Double) -> Unit,
    onFling: (Double) -> Unit,
    onTouchStart: () -> Unit,
    onFocus: (top: Double, height: Double) -> Unit,
): Disposer = Disposer(jsInstallDocumentListeners(host, onSection, onAppLink, onShowCards, onScroll, onFling, onTouchStart, onFocus))

private fun jsSetSnackbar(element: HTMLElement, iconSvg: String, text: String, background: String, color: String, iconColor: String): Unit = js("""
    (function () {
        var inner = element.firstChild;
        if (!inner) { inner = document.createElement("div"); inner.className = "ssd-snackbar"; inner.setAttribute("role", "status"); element.appendChild(inner); }
        inner.innerHTML = iconSvg;
        var span = document.createElement("span");
        span.textContent = text;
        inner.appendChild(span);
        inner.style.background = background;
        inner.style.color = color;
        inner.firstChild.style.color = iconColor;
    })()
""")

/** Fills an HTML snackbar: being HTML, it shows above the HTML documents too. */
fun setSnackbar(element: HTMLElement, iconSvg: String, text: String, background: String, color: String, iconColor: String) =
    jsSetSnackbar(element, iconSvg, text, background, color, iconColor)

private fun jsSetErrorCard(
    host: HTMLElement,
    iconSvg: String,
    closeSvg: String,
    text: String,
    dismissLabel: String,
    more: Boolean,
    background: String,
    color: String,
    onDismiss: () -> Unit,
): Unit = js("""
    (function () {
        var root = host.firstChild;
        root.innerHTML = "";
        var card = document.createElement("div");
        card.className = "ssd-error" + (more ? " ssd-error-more" : "");
        card.setAttribute("role", "alert");
        card.style.background = background;
        card.style.color = color;
        card.style.setProperty("--ssd-error-stack", background);
        card.insertAdjacentHTML("beforeend", iconSvg);
        var span = document.createElement("span");
        span.textContent = text;
        card.appendChild(span);
        var button = document.createElement("button");
        button.type = "button";
        button.setAttribute("aria-label", dismissLabel);
        button.innerHTML = closeSvg;
        button.onclick = function () { onDismiss(); };
        card.appendChild(button);
        root.appendChild(card);
    })()
""")

/** Fills an HTML error card: being HTML, it shows above the HTML documents instead of hiding them. */
fun setErrorCard(
    host: HTMLElement,
    iconSvg: String,
    closeSvg: String,
    text: String,
    dismissLabel: String,
    more: Boolean,
    background: String,
    color: String,
    onDismiss: () -> Unit,
) = jsSetErrorCard(host, iconSvg, closeSvg, text, dismissLabel, more, background, color, onDismiss)

package games.supersetdeck.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

// Opening goes through an <input type="file">, which every browser supports. Saving uses the
// File System Access save picker where it exists (the user picks the destination), and falls back
// to a download elsewhere (Firefox, Safari).

private fun jsOpenTextFile(accept: String): Promise<JsAny?> = js("""
    new Promise(function (resolve) {
        var input = document.createElement("input");
        input.type = "file";
        input.accept = accept;
        input.style.display = "none";
        var done = false;
        var finish = function (value) {
            if (done) return;
            done = true;
            if (input.parentNode) input.parentNode.removeChild(input);
            resolve(value);
        };
        input.addEventListener("change", function () {
            var file = input.files && input.files[0];
            if (!file) { finish(null); return; }
            file.text().then(function (text) { finish({ name: file.name, text: text }); }, function () { finish(null); });
        });
        input.addEventListener("cancel", function () { finish(null); });
        document.body.appendChild(input);
        input.click();
    })
""")

private fun jsFileName(file: JsAny): String = js("file.name")
private fun jsFileText(file: JsAny): String = js("file.text")

class OpenedFile(val name: String, val text: String)

/** Lets the user pick a file; null if they cancel. */
suspend fun openTextFile(accept: String): OpenedFile? =
    jsOpenTextFile(accept).await<JsAny?>()?.let { OpenedFile(jsFileName(it), jsFileText(it)) }

private fun jsSaveTextFile(suggestedName: String, description: String, mimeType: String, extension: String, text: String): Promise<JsBoolean> = js("""
    (function () {
        if (typeof window.showSaveFilePicker === "function") {
            var accept = {};
            accept[mimeType] = [extension];
            return window.showSaveFilePicker({
                suggestedName: suggestedName,
                excludeAcceptAllOption: true,
                types: [{ description: description, accept: accept }]
            }).then(function (handle) {
                return handle.createWritable().then(function (writable) {
                    return writable.write(text).then(function () { return writable.close(); }).then(function () { return true; });
                });
            }, function (error) {
                if (error && error.name === "AbortError") return false;
                throw error;
            });
        }
        var blob = new Blob([text], { type: "application/octet-stream" });
        var url = URL.createObjectURL(blob);
        var link = document.createElement("a");
        link.href = url;
        link.download = suggestedName;
        link.style.display = "none";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
        return Promise.resolve(true);
    })()
""")

/** Writes [text] to a file the user chooses; false if they cancel. */
suspend fun saveTextFile(suggestedName: String, description: String, mimeType: String, extension: String, text: String): Boolean =
    jsSaveTextFile(suggestedName, description, mimeType, extension, text).await<JsBoolean>().toBoolean()

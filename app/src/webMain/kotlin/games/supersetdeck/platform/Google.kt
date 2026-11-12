package games.supersetdeck.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

// Google Identity Services gives an OAuth token for the Drive appDataFolder scope; Drive itself is
// called through its REST API with that token.

private fun jsRequestGoogleToken(clientId: String, scope: String): Promise<JsString> = js("""
    new Promise(function (resolve, reject) {
        if (!window.google || !google.accounts || !google.accounts.oauth2) {
            reject(new Error("Google services are not available"));
            return;
        }
        var client = google.accounts.oauth2.initTokenClient({
            client_id: clientId,
            scope: scope,
            callback: function (response) {
                if (response && response.access_token) resolve(response.access_token);
                else reject(new Error((response && (response.error_description || response.error)) || "Google access was not granted"));
            },
            error_callback: function (error) {
                reject(new Error((error && (error.message || error.type)) || "Google access was not granted"));
            }
        });
        client.requestAccessToken();
    })
""")

/** Opens Google's consent popup and returns an access token. Must be called from a user gesture. */
suspend fun requestGoogleToken(clientId: String, scope: String): String =
    jsRequestGoogleToken(clientId, scope).await<JsString>().toString()

private fun jsDriveFetch(token: String, method: String, url: String, contentType: String?, body: String?): Promise<JsString> = js("""
    (function () {
        var headers = { "Authorization": "Bearer " + token };
        if (contentType) headers["Content-Type"] = contentType;
        var init = { method: method, headers: headers };
        if (body !== null && body !== undefined) init.body = body;
        return fetch(url, init).then(function (response) {
            return response.text().then(function (text) {
                if (!response.ok) {
                    var message = "Google Drive error " + response.status;
                    try { var json = JSON.parse(text); if (json.error && json.error.message) message += ": " + json.error.message; } catch (e) {}
                    throw new Error(message);
                }
                return text;
            });
        });
    })()
""")

/** One Drive REST call; returns the response body. */
suspend fun driveFetch(token: String, method: String, url: String, contentType: String? = null, body: String? = null): String =
    jsDriveFetch(token, method, url, contentType, body).await<JsString>().toString()

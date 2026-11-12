package games.supersetdeck.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

// The database of the previous site version, reused as is so that existing lists are kept:
// - "List": { id, name }, keyed by "id" (personal lists only, never the favourites);
// - "ListEntry": { listId, gameId }, keyed by ["listId", "gameId"], indexed by "gameId";
// - "ListUpdate": last modification time (ms since epoch), keyed by list id, tombstones included.
// Data crosses the JS boundary as JSON strings, the simplest type both Kotlin/JS and Kotlin/Wasm share.

private fun jsOpenDatabase(): Promise<JsAny> = js("""
    new Promise(function (resolve, reject) {
        if (typeof indexedDB === "undefined") { reject(new Error("IndexedDB is not available")); return; }
        var request;
        try { request = indexedDB.open("Super-Set-Deck", 1); } catch (e) { reject(e); return; }
        request.onupgradeneeded = function () {
            var db = request.result;
            if (!db.objectStoreNames.contains("List")) db.createObjectStore("List", { keyPath: "id" });
            if (!db.objectStoreNames.contains("ListEntry")) {
                var entries = db.createObjectStore("ListEntry", { keyPath: ["listId", "gameId"] });
                entries.createIndex("gameId", "gameId");
            }
            if (!db.objectStoreNames.contains("ListUpdate")) db.createObjectStore("ListUpdate");
        };
        request.onsuccess = function () { resolve(request.result); };
        request.onerror = function () { reject(request.error || new Error("Could not open the database")); };
        request.onblocked = function () { reject(new Error("The database is blocked by another tab")); };
    })
""")

private fun jsLoadAll(db: JsAny): Promise<JsString> = js("""
    new Promise(function (resolve, reject) {
        var tx = db.transaction(["List", "ListEntry", "ListUpdate"], "readonly");
        var result = { lists: [], entries: [], updates: [] };
        tx.objectStore("List").getAll().onsuccess = function (e) {
            result.lists = e.target.result.map(function (l) { return { id: String(l.id), name: String(l.name) }; });
        };
        tx.objectStore("ListEntry").getAll().onsuccess = function (e) {
            result.entries = e.target.result.map(function (l) { return { listId: String(l.listId), gameId: String(l.gameId) }; });
        };
        var cursorRequest = tx.objectStore("ListUpdate").openCursor();
        cursorRequest.onsuccess = function (e) {
            var cursor = e.target.result;
            if (cursor) {
                result.updates.push({ id: String(cursor.key), date: Number(cursor.value) });
                cursor.continue();
            }
        };
        tx.oncomplete = function () { resolve(JSON.stringify(result)); };
        tx.onerror = function () { reject(tx.error || new Error("Could not read the database")); };
        tx.onabort = function () { reject(tx.error || new Error("Database read aborted")); };
    })
""")

/**
 * Replaces the whole state of one list in a single transaction.
 * [name] null with [games] null deletes the list (a tombstone remains through its update time);
 * for the favourites, only the entries and the update time are written.
 */
private fun jsWriteList(db: JsAny, id: String, json: String): Promise<JsAny?> = js("""
    new Promise(function (resolve, reject) {
        var data = JSON.parse(json);
        var tx = db.transaction(["List", "ListEntry", "ListUpdate"], "readwrite");
        var lists = tx.objectStore("List");
        var entries = tx.objectStore("ListEntry");
        if (id !== "favourites") {
            if (data.name === null) lists.delete(id);
            else lists.put({ id: id, name: data.name });
        }
        entries.delete(IDBKeyRange.bound([id, ""], [id, "￿"]));
        (data.games || []).forEach(function (gameId) { entries.put({ listId: id, gameId: gameId }); });
        tx.objectStore("ListUpdate").put(data.date, id);
        tx.oncomplete = function () { resolve(null); };
        tx.onerror = function () { reject(tx.error || new Error("Could not write the database")); };
        tx.onabort = function () { reject(tx.error || new Error("Database write aborted")); };
    })
""")

class Database internal constructor(private val db: JsAny) {

    /** The raw content: `{ lists: [{id, name}], entries: [{listId, gameId}], updates: [{id, date}] }`. */
    suspend fun loadAll(): String = jsLoadAll(db).await<JsString>().toString()

    /** Writes a list's whole state; see the store layout at the top of this file. */
    suspend fun writeList(id: String, json: String) {
        jsWriteList(db, id, json).await<JsAny?>()
    }

    companion object {
        suspend fun open(): Database = Database(jsOpenDatabase().await<JsAny>())
    }
}

// Offline support.
//
// precache.txt (generated at build time) lists every file of the site with a hash of its content,
// after a version line. Every listed file is downloaded in the background, so the whole site (rules,
// references, printables, images) is available offline without visiting each page. Only new or
// changed files are downloaded, and progress is kept file by file: an interrupted download resumes,
// and a file that fails does not prevent the others from being cached.
//
// While online, every request goes to the network first, so the latest version is always shown;
// the cache is only a fallback.

const CACHE = "ssd-site";
const MANIFEST_KEY = "__precache_manifest__";
const PARALLEL_DOWNLOADS = 6;
let lastCheck = 0;
let running = null;

async function readManifest(cache) {
    try {
        const response = await cache.match(MANIFEST_KEY);
        return response ? await response.json() : {};
    } catch (e) {
        return {};
    }
}

async function precache() {
    lastCheck = Date.now();
    let lines;
    try {
        const response = await fetch("precache.txt", { cache: "no-store" });
        if (!response.ok) return;
        lines = (await response.text()).split("\n").map((l) => l.trim()).filter((l) => l.length > 0);
    } catch (e) {
        return;
    }
    lines.shift(); // The version line: the per-file hashes are what matter.
    const cache = await caches.open(CACHE);
    const manifest = await readManifest(cache);
    const wanted = lines.map((line) => {
        const space = line.lastIndexOf(" ");
        return space < 0 ? { path: line, hash: "" } : { path: line.substring(0, space), hash: line.substring(space + 1) };
    });
    const missing = wanted.filter((file) => manifest[file.path] !== file.hash || !file.hash);
    let saving = Promise.resolve();
    const save = () => {
        saving = saving.then(() => cache.put(MANIFEST_KEY, new Response(JSON.stringify(manifest), { headers: { "Content-Type": "application/json" } })));
        return saving;
    };
    let next = 0;
    const worker = async () => {
        while (next < missing.length) {
            const file = missing[next++];
            try {
                const response = await fetch(file.path, { cache: "no-cache" });
                if (!response.ok) continue;
                await cache.put(file.path, response);
                manifest[file.path] = file.hash;
                await save();
            } catch (e) {
                // Retried at the next check.
            }
        }
    };
    await Promise.all(Array.from({ length: PARALLEL_DOWNLOADS }, worker));
    // Files no longer part of the site are dropped.
    const paths = new Set(wanted.map((file) => file.path));
    for (const path of Object.keys(manifest)) {
        if (!paths.has(path)) {
            delete manifest[path];
            await cache.delete(path);
        }
    }
    await save();
}

function startPrecache(event) {
    if (!running) running = precache().finally(() => { running = null; });
    if (event) event.waitUntil(running);
}

self.addEventListener("install", () => {
    // Installing must not wait for the whole site: the download goes on once active.
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(self.clients.claim());
    startPrecache(event);
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET" || new URL(request.url).origin !== self.location.origin) return;
    event.respondWith((async () => {
        const cache = await caches.open(CACHE);
        try {
            const response = await fetch(request);
            if (response.ok && response.type === "basic") cache.put(request, response.clone());
            // A new deployment may come with new files, and an interrupted download should resume:
            // checked at most every 10 minutes.
            if (request.mode === "navigate" && Date.now() - lastCheck > 600000) startPrecache(event);
            return response;
        } catch (error) {
            const cached = await cache.match(request, { ignoreSearch: true });
            if (cached) return cached;
            if (request.mode === "navigate") {
                const index = await cache.match("index.html");
                if (index) return index;
            }
            throw error;
        }
    })());
});

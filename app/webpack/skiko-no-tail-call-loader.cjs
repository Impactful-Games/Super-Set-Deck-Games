// Works around WebKit bug 329421 (Safari 27.2, macOS and iOS): when JavaScript calls a Wasm function
// in tail position, f32 arguments holding small integers (0, 1…) reach Wasm as NaN.
// Every export of skiko.mjs is such a wrapper, and Kotlin calls Skia through them: Compose draws
// nothing, a white page that still takes clicks and scrolling.
// See https://youtrack.jetbrains.com/issue/CMP-10986 and https://bugs.webkit.org/show_bug.cgi?id=329421
//
// A call inside a try block is never in tail position (ECMA-262, HasCallInTailPosition), and terser
// keeps the try block, unlike a `const r = f(); return r` that it folds back into a tail call.
// Remove this loader once the fixed WebKit has reached Safari users.

const WRAPPER = /\(\.\.\.a\) => \((\w+) = loadedWasm\._\["\1"\]\)\(\.\.\.a\)/g;

module.exports = function (source) {
    let patched = 0;
    const result = source.replace(WRAPPER, (_, name) => {
        patched++;
        return `(...a) => { try { return (${name} = loadedWasm._["${name}"])(...a) } catch (e) { throw e } }`;
    });
    if (patched === 0) throw new Error("The skiko.mjs export wrappers changed: the WebKit 329421 workaround cannot be applied");
    return result;
};

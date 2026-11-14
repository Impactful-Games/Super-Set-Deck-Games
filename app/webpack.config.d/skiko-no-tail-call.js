// Rewrites the Skia calls of skiko.mjs out of tail position, which Safari 27.2 breaks: see the loader.
// __dirname is build/<js|wasm>/packages/<package> of the root project.
config.module.rules.push({
    test: /[\\/]skiko\.mjs$/,
    loader: require("path").resolve(__dirname, "../../../../app/webpack/skiko-no-tail-call-loader.cjs"),
});

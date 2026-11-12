import org.jetbrains.compose.web.tasks.WebCompatibilityTask

plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
}

val designExport = tasks.register<DesignExportTask>("designExport") {
    designFiles.from(fileTree(layout.settingsDirectory.dir("games")) { include("*/board.designcraft", "*/reminder.designcraft") })
    cli = providers.gradleProperty("designcraft.cli").orElse(providers.environmentVariable("DESIGNCRAFT_CLI"))
    outputDir = layout.buildDirectory.dir("generated/site/designs")
}

val gamesData = tasks.register<GamesDataTask>("gamesData") {
    gamesDir = layout.settingsDirectory.dir("games")
    designsDir = designExport.flatMap { it.outputDir }
    // In the root build directory, which no clean task deletes: the BGG cache survives `clean`.
    cacheDir = layout.settingsDirectory.dir("build/bgg-cache")
    bggToken = providers.gradleProperty("bgg.token").orElse(providers.environmentVariable("BGG_TOKEN"))
    outputDir = layout.buildDirectory.dir("generated/site/games-data")
}

val docs = tasks.register<DocsTask>("docs") {
    gamesDir = layout.settingsDirectory.dir("games")
    docsDir = layout.settingsDirectory.dir("docs")
    outputDir = layout.buildDirectory.dir("generated/site/docs")
}

kotlin {
    jvmToolchain(findProperty("jvm.toolchain")!!.toString().toInt())

    // Both bundles are named app.js: the compatibility distribution then makes app.js the loader
    // that picks the Wasm build, or the JS one on browsers lacking the required Wasm features.
    js {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
            testTask { useKarma { useChromeHeadless() } }
        }
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
            testTask { useKarma { useChromeHeadless() } }
        }
        binaries.executable()
    }

    sourceSets {
        webMain {
            // The generated site content (games data, boards and references, rules and documents) is served next to the app.
            resources.srcDir(gamesData.flatMap { it.outputDir })
            resources.srcDir(designExport.flatMap { it.outputDir })
            resources.srcDir(docs.flatMap { it.outputDir })
        }
        // The fonts are Compose resources of commonMain, whose generated accessors need the library there.
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
        }
        webMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)

            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.qrose)
        }
        webTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    compilerOptions {
        optIn.addAll(
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "kotlin.js.ExperimentalWasmJsInterop",
            "kotlin.uuid.ExperimentalUuidApi",
        )
    }
}

compose.resources {
    packageOfResClass = "games.supersetdeck.resources"
}

// The Wasm optimizer (Binaryen) copies the JS glue (import-object.mjs, where every js("…") snippet
// lives) next to the optimized binary, but only declares the binary as input. A change to js("…")
// code alone leaves the binary as it was and the task up to date: the site would ship the previous
// glue. Declaring the glue as an input reruns the optimizer whenever it changes.
tasks.matching { it.name == "compileProductionExecutableKotlinWasmJsOptimize" }.configureEach {
    inputs.files(layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/kotlin").map { dir -> dir.asFileTree.matching { include("*.mjs") } })
        .withPropertyName("jsGlue")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

val compatibilityDistribution = tasks.named<WebCompatibilityTask>("composeCompatibilityBrowserDistribution")

val precacheList = tasks.register<PrecacheListTask>("precacheList") {
    siteDir = compatibilityDistribution.flatMap { it.outputDir }
    outputFile = layout.buildDirectory.file("generated/precache/precache.txt")
}

// The deployable site: the Wasm application with its JS fallback, plus the service worker's file list.
val site = tasks.register<Sync>("site") {
    group = "build"
    description = "Assembles the deployable site in build/site."
    from(compatibilityDistribution)
    from(precacheList)
    into(layout.buildDirectory.dir("site"))
}

tasks.named("assemble") { dependsOn(site) }

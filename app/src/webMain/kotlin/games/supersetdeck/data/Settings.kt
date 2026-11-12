package games.supersetdeck.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import games.supersetdeck.i18n.Lang
import games.supersetdeck.platform.LocalStorage
import games.supersetdeck.platform.onPrefersDarkSchemeChange
import games.supersetdeck.platform.prefersDarkScheme
import games.supersetdeck.platform.setDocumentLang

enum class ThemeMode(val key: String) {
    System("system"), Light("light"), Dark("dark");

    companion object {
        fun of(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}

/** The preferences kept on the device: language and theme. */
object Settings {
    private const val THEME_KEY = "theme"

    var lang: Lang by mutableStateOf(Lang.initial().also { setDocumentLang(it.code) })
        private set

    var themeMode: ThemeMode by mutableStateOf(ThemeMode.of(LocalStorage[THEME_KEY]))
        private set

    private var systemDark: Boolean by mutableStateOf(prefersDarkScheme())

    val isDark: Boolean get() = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    init {
        onPrefersDarkSchemeChange { systemDark = it }
    }

    fun selectLang(lang: Lang) {
        this.lang = lang
        Lang.remember(lang)
        setDocumentLang(lang.code)
    }

    fun selectTheme(mode: ThemeMode) {
        themeMode = mode
        LocalStorage[THEME_KEY] = mode.key
    }
}

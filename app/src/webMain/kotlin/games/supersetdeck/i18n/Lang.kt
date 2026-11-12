package games.supersetdeck.i18n

import games.supersetdeck.platform.LocalStorage
import games.supersetdeck.platform.preferredLanguages

enum class Lang(val code: String, val strings: Strings) {
    En("en", EnStrings),
    Fr("fr", FrStrings),
    ;

    companion object {
        val default = En

        private const val STORAGE_KEY = "lang"

        fun of(code: String): Lang? = entries.firstOrNull { it.code == code }

        /**
         * The remembered language, else the browser's first preferred language that is supported
         * (`fr-CA` → `fr`), else English. The result is remembered at once.
         */
        fun initial(): Lang {
            val lang = LocalStorage[STORAGE_KEY]?.let { of(it) }
                ?: preferredLanguages().map { it.substringBefore('-').lowercase() }.firstNotNullOfOrNull { of(it) }
                ?: default
            remember(lang)
            return lang
        }

        fun remember(lang: Lang) {
            LocalStorage[STORAGE_KEY] = lang.code
        }
    }
}

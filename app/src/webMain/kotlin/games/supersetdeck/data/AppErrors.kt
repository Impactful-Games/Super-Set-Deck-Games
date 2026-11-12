package games.supersetdeck.data

import androidx.compose.runtime.mutableStateListOf
import games.supersetdeck.i18n.Strings

/** A global error, shown to the user one at a time until dismissed. */
sealed interface AppError {
    fun message(strings: Strings): String

    data class Database(val cause: String) : AppError {
        override fun message(strings: Strings) = strings.databaseError(cause)
    }

    data class Games(val cause: String) : AppError {
        override fun message(strings: Strings) = strings.gamesError(cause)
    }

    data class Other(val cause: String?) : AppError {
        override fun message(strings: Strings) = cause ?: strings.unknownError
    }
}

object AppErrors {
    /** The errors waiting to be seen, the first one being displayed. */
    val queue = mutableStateListOf<AppError>()

    fun report(error: AppError) {
        queue += error
    }

    fun dismissCurrent() {
        if (queue.isNotEmpty()) queue.removeAt(0)
    }
}

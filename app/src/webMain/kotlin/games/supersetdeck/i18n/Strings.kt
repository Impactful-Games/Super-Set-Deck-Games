package games.supersetdeck.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import games.supersetdeck.model.Suit

/**
 * Every label of the interface. Adding a language means adding an implementation of this
 * interface to [Lang], plus its rules and About page.
 */
interface Strings {
    val languageName: String

    // Navigation
    val navGames: String
    val navLists: String
    val navPacker: String
    val navPackerShort: String
    val navAbout: String
    val navChangelog: String
    val poweredBy: String
    val language: String
    val theme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val back: String
    val close: String
    val dismiss: String
    val loading: String

    // Catalogue
    val sortByName: String
    val sortByRating: String
    val sortByYear: String
    val searchPlaceholder: String
    val searchLabel: String
    val clearSearch: String
    val clearPlayers: String
    val clearTypesAndList: String
    val filterTypesAndList: String
    val filterPlayers: String
    val sort: String
    val typesSection: String

    /** [players] is already abbreviated (`2-6, 8`), hence the literal handling of "0" and "1". */
    fun players(players: String): String
    fun games(count: Int): String
    fun types(count: Int): String
    fun variants(count: Int): String

    // Game page
    val rules: String
    val references: String
    val gameBy: String
    val adaptedBy: String
    fun traditional(tradition: String): String
    val share: String
    val shareOptions: String
    val shareByMessage: String
    val shareByQrCode: String
    val copyGameLink: String
    val linkCopied: String
    val linkNotCopied: String
    fun shareByQrCodeOf(game: String): String
    val otherBggNames: String
    fun bggDescription(game: String, year: Int, age: Int, minutes: Int, rating: String): String
    fun minutes(minutes: Int): String
    fun minAge(age: Int): String
    val favourites: String
    val newList: String
    val addToList: String
    val alreadyInList: String
    fun removeFrom(list: String): String
    fun filterBy(name: String): String
    fun cardsNeededFor(game: String): String
    val referencesCount: String

    // References gallery
    val closeReferences: String
    val dualView: String
    val referencesOverview: String
    val previousReference: String
    val nextReference: String
    /** A game aid, by its [number], from 1. */
    fun reference(number: Int): String
    fun showReference(number: Int): String
    fun openFullScreen(number: Int): String

    // Lists
    val backupRestore: String
    val newGameList: String
    fun renameTitle(name: String): String
    val listName: String
    val create: String
    val rename: String
    fun deleteTitle(name: String): String
    val delete: String
    val cancel: String
    fun renameList(name: String): String
    fun deleteList(name: String): String

    // Backup / restore
    val syncWithLocalFiles: String
    val createNewFile: String
    val openExistingFile: String
    val backupFileType: String
    val syncWithGoogleDrive: String
    val driveNewFile: String
    fun driveLastBackup(date: String): String
    val deleteDriveBackup: String
    val columnApplication: String
    val columnBackup: String
    val synchronize: String
    val upToDate: String
    val loadingBackup: String
    val badgeNew: String
    val badgeDelete: String
    val deletedList: String
    val absentList: String
    fun directionFor(list: String): String
    val directionToApp: String
    val directionPause: String
    val directionToBackup: String
    val backToLists: String

    // Packer
    val addGameToPack: String
    val game: String
    val addToPack: String
    val edit: String
    fun openPage(game: String): String
    fun editConfiguration(game: String): String
    fun removeFromPack(game: String): String
    val cardsToPack: String
    val usedBy: String
    val selectCardHint: String
    val notUsed: String
    fun card(suit: String, value: String, copy: Int?, games: List<String>): String

    // Errors & states
    fun databaseError(message: String): String
    fun gamesError(message: String): String
    val ruleError: String
    val documentError: String
    val unknownError: String
    val unknownPage: String
    val howDidYouEndUpHere: String
    val seeGames: String
    val gameNotFound: String
    val backToGames: String

    // Data
    fun suit(suit: Suit): String
    fun special(value: String): String
    fun type(type: String): String
    fun tradition(tradition: String): String
}

/** `A, B & C`. */
fun List<String>.joinedWithAnd(): String =
    if (size <= 1) joinToString() else dropLast(1).joinToString(", ") + " & " + last()

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }

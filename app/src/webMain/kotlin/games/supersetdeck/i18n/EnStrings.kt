package games.supersetdeck.i18n

import games.supersetdeck.model.Suit

object EnStrings : Strings {
    override val languageName = "English"

    override val navGames = "Games"
    override val navLists = "My Lists"
    override val navPacker = "Travel Packer"
    override val navPackerShort = "Packer"
    override val navAbout = "About"
    override val navChangelog = "Changelog"
    override val poweredBy = "Powered by"
    override val language = "Language"
    override val theme = "Theme"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val back = "Back"
    override val close = "Close"
    override val dismiss = "Dismiss"
    override val loading = "Loading"

    override val sortByName = "by Name"
    override val sortByRating = "by BGG Rating"
    override val sortByYear = "by Publication Year"
    override val searchPlaceholder = "Name / Author"
    override val searchLabel = "Search games"
    override val clearSearch = "Clear search"
    override val clearPlayers = "Clear players"
    override val clearTypesAndList = "Clear types and list"
    override val filterTypesAndList = "Filter by types and list"
    override val filterPlayers = "Filter by player count"
    override val sort = "Sort"
    override val typesSection = "Types"

    override fun players(players: String) = when (players) {
        "0", "" -> "nobody"
        "1" -> "1 player"
        else -> "$players players"
    }

    override fun games(count: Int) = when (count) {
        0 -> "no game"
        1 -> "1 game"
        else -> "$count games"
    }

    override fun types(count: Int) = when (count) {
        0 -> "no type"
        1 -> "1 type"
        else -> "$count types"
    }

    override fun variants(count: Int) = when (count) {
        0 -> "Without variant"
        1 -> "With 1 variant"
        else -> "With $count variants"
    }

    override val rules = "Rules"
    override val references = "References"
    override val gameBy = "A game by"
    override val adaptedBy = "adapted by"
    override fun traditional(tradition: String) = "A traditional $tradition game"
    override val share = "Share"
    override val shareOptions = "Share options"
    override val shareByMessage = "Share by Message"
    override val shareByQrCode = "Share by QR-Code"
    override val copyGameLink = "Copy Game page link"
    override val linkCopied = "Link copied"
    override val linkNotCopied = "Could not copy the link"
    override fun shareByQrCodeOf(game: String) = "Share $game by QR-Code"
    override val otherBggNames = "Other BGG names"
    override fun bggDescription(game: String, year: Int, age: Int, minutes: Int, rating: String) =
        "$game on BoardGameGeek: $year, age $age+, $minutes minutes, rated $rating"
    override fun minutes(minutes: Int) = "$minutes min"
    override fun minAge(age: Int) = "$age+"
    override val favourites = "Favourites"
    override val newList = "New list"
    override val addToList = "Add to list"
    override val alreadyInList = "Already in this list"
    override fun removeFrom(list: String) = "Remove from $list"
    override fun filterBy(name: String) = "Show the games of \"$name\""
    override fun cardsNeededFor(game: String) = "Cards needed for $game"
    override val referencesCount = "references"

    override val closeReferences = "Close references"
    override val dualView = "Dual view"
    override val referencesOverview = "All references overview"
    override val previousReference = "Previous reference"
    override val nextReference = "Next reference"
    override fun reference(number: Int) = "Reference $number"
    override fun showReference(number: Int) = "Show reference $number"
    override fun openFullScreen(number: Int) = "Open reference $number full screen"

    override val backupRestore = "Backup / Restore"
    override val newGameList = "New Game List"
    override fun renameTitle(name: String) = "Rename \"$name\""
    override val listName = "List name"
    override val create = "Create"
    override val rename = "Rename"
    override fun deleteTitle(name: String) = "Delete \"$name\"?"
    override val delete = "Delete"
    override val cancel = "Cancel"
    override fun renameList(name: String) = "Rename $name"
    override fun deleteList(name: String) = "Delete $name"

    override val syncWithLocalFiles = "Synchronize with Local Files"
    override val createNewFile = "Create new file"
    override val openExistingFile = "Open existing file"
    override val backupFileType = "Super-Set-Deck backup file"
    override val syncWithGoogleDrive = "Synchronize with Google Drive"
    override val driveNewFile = "A new file will be created in your Drive AppData."
    override fun driveLastBackup(date: String) = "Last Google Drive backup: $date."
    override val deleteDriveBackup = "Delete Drive backup?"
    override val columnApplication = "Application"
    override val columnBackup = "Backup"
    override val synchronize = "Synchronize"
    override val upToDate = "Everything is up to date!"
    override val loadingBackup = "Loading backup"
    override val badgeNew = "New"
    override val badgeDelete = "Delete"
    override val deletedList = "Deleted"
    override val absentList = "Not there"
    override fun directionFor(list: String) = "Direction for $list"
    override val directionToApp = "From backup to application"
    override val directionPause = "Pause, do nothing"
    override val directionToBackup = "From application to backup"
    override val backToLists = "Back to My Lists"

    override val addGameToPack = "Add game to pack"
    override val game = "Game"
    override val addToPack = "Add to pack"
    override val edit = "Edit"
    override fun openPage(game: String) = "Open $game page"
    override fun editConfiguration(game: String) = "Edit $game configuration"
    override fun removeFromPack(game: String) = "Remove $game from pack"
    override val cardsToPack = "cards to pack"
    override val usedBy = "Used by"
    override val selectCardHint = "Select a highlighted card to see which games use it."
    override val notUsed = "Not used"
    override fun card(suit: String, value: String, copy: Int?, games: List<String>) =
        "$suit $value" + (copy?.let { " number $it" } ?: "") +
            (if (games.isEmpty()) ", not needed" else ", needed by ${games.joinToString(", ")}")

    override fun databaseError(message: String) =
        "Could not open database: $message. The app is in degraded mode, without any persistance."
    override fun gamesError(message: String) = "Could not load games list: $message."
    override val ruleError = "Could not load game documentation."
    override val documentError = "Could not load document."
    override val unknownError = "Unknown error"
    override val unknownPage = "Unknown page"
    override val howDidYouEndUpHere = "How did you end up here?!?"
    override val seeGames = "See Games!"
    override val gameNotFound = "GAME NOT FOUND"
    override val backToGames = "Back to Games"

    override fun suit(suit: Suit) = suit.name
    override fun special(value: String) = value
    override fun type(type: String) = type
    override fun tradition(tradition: String) = tradition
}

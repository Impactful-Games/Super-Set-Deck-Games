package games.supersetdeck.i18n

import games.supersetdeck.model.Suit

object FrStrings : Strings {
    override val languageName = "Français"

    override val navGames = "Jeux"
    override val navLists = "Mes Listes"
    override val navPacker = "Packer"
    override val navPackerShort = "Packer"
    override val navAbout = "À propos"
    override val navChangelog = "Historique"
    override val poweredBy = "Propulsé par"
    override val language = "Langue"
    override val theme = "Thème"
    override val themeSystem = "Système"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val back = "Retour"
    override val close = "Fermer"
    override val dismiss = "Fermer"
    override val loading = "Chargement"

    override val sortByName = "par Nom"
    override val sortByRating = "par Note BGG"
    override val sortByYear = "par Année de Publication"
    override val searchPlaceholder = "Nom / Auteur·Autrice"
    override val searchLabel = "Rechercher des jeux"
    override val clearSearch = "Effacer la recherche"
    override val clearPlayers = "Effacer les joueur·euse·s"
    override val clearTypesAndList = "Effacer les types et la liste"
    override val filterTypesAndList = "Filtrer par types et par liste"
    override val filterPlayers = "Filtrer par nombre de joueur·euse·s"
    override val sort = "Tri"
    override val typesSection = "Types"

    override fun players(players: String) = when (players) {
        "0", "" -> "personne"
        "1" -> "1 joueur·euse"
        else -> "$players joueur·euse·s"
    }

    override fun games(count: Int) = when (count) {
        0 -> "pas de jeux"
        1 -> "1 jeu"
        else -> "$count jeux"
    }

    override fun types(count: Int) = when (count) {
        0 -> "aucun type"
        1 -> "1 type"
        else -> "$count types"
    }

    override fun variants(count: Int) = when (count) {
        0 -> "Sans variante"
        1 -> "Avec 1 variante"
        else -> "Avec $count variantes"
    }

    override val rules = "Règles"
    override val references = "Références"
    override val gameBy = "Un jeu de"
    override val adaptedBy = "adapté par"
    override fun traditional(tradition: String) = "Un jeu traditionnel $tradition"
    override val share = "Partager"
    override val shareOptions = "Options de partage"
    override val shareByMessage = "Partager par Message"
    override val shareByQrCode = "Partager par QR-Code"
    override val copyGameLink = "Copier le lien de la Page du jeu"
    override val linkCopied = "Lien copié"
    override val linkNotCopied = "Impossible de copier le lien"
    override fun shareByQrCodeOf(game: String) = "Partager $game par QR-Code"
    override val otherBggNames = "Autres noms BGG"
    override fun bggDescription(game: String, year: Int, age: Int, minutes: Int, rating: String) =
        "$game sur BoardGameGeek : $year, dès $age ans, $minutes minutes, noté $rating"
    override fun minutes(minutes: Int) = "$minutes min"
    override fun minAge(age: Int) = "$age+"
    override val favourites = "Favoris"
    override val newList = "Nouvelle liste"
    override val addToList = "Ajouter à une liste"
    override val alreadyInList = "Déjà dans cette liste"
    override fun removeFrom(list: String) = "Retirer de $list"
    override fun filterBy(name: String) = "Voir les jeux de « $name »"
    override fun cardsNeededFor(game: String) = "Cartes nécessaires pour $game"
    override val referencesCount = "références"

    override val closeReferences = "Fermer les références"
    override val dualView = "Double vue"
    override val referencesOverview = "Aperçu de toutes les références"
    override val previousReference = "Référence précédente"
    override val nextReference = "Référence suivante"
    override fun reference(number: Int) = "Référence $number"
    override fun showReference(number: Int) = "Afficher la référence $number"
    override fun openFullScreen(number: Int) = "Ouvrir la référence $number en plein écran"

    override val backupRestore = "Sauvegarder / Restaurer"
    override val newGameList = "Nouvelle Liste de Jeux"
    override fun renameTitle(name: String) = "Renommer \"$name\""
    override val listName = "Nom de la liste"
    override val create = "Créer"
    override val rename = "Renommer"
    override fun deleteTitle(name: String) = "Supprimer \"$name\" ?"
    override val delete = "Supprimer"
    override val cancel = "Annuler"
    override fun renameList(name: String) = "Renommer $name"
    override fun deleteList(name: String) = "Supprimer $name"

    override val syncWithLocalFiles = "Synchroniser avec un fichier local"
    override val createNewFile = "Créer un nouveau fichier"
    override val openExistingFile = "Ouvrir un fichier existant"
    override val backupFileType = "Fichier de backup Super-Set-Deck"
    override val syncWithGoogleDrive = "Synchroniser avec Google Drive"
    override val driveNewFile = "Un nouveau fichier sera créé dans votre Drive AppData."
    override fun driveLastBackup(date: String) = "Dernier backup sur Google Drive : $date."
    override val deleteDriveBackup = "Supprimer le backup sur Drive ?"
    override val columnApplication = "Application"
    override val columnBackup = "Backup"
    override val synchronize = "Synchroniser"
    override val upToDate = "Tout est à jour!"
    override val loadingBackup = "Chargement de la sauvegarde"
    override val badgeNew = "Nouvelle"
    override val badgeDelete = "Suppression"
    override val deletedList = "Supprimée"
    override val absentList = "Absente"
    override fun directionFor(list: String) = "Sens pour $list"
    override val directionToApp = "De la sauvegarde vers l'application"
    override val directionPause = "Pause, ne rien faire"
    override val directionToBackup = "De l'application vers la sauvegarde"
    override val backToLists = "Retour à Mes Listes"

    override val addGameToPack = "Ajouter un jeu au pack"
    override val game = "Jeu"
    override val addToPack = "Ajouter au pack"
    override val edit = "Éditer"
    override fun openPage(game: String) = "Ouvrir la page de $game"
    override fun editConfiguration(game: String) = "Modifier la configuration de $game"
    override fun removeFromPack(game: String) = "Retirer $game du pack"
    override val cardsToPack = "cartes à emporter"
    override val usedBy = "Utilisée par"
    override val selectCardHint = "Choisissez une carte en couleur pour voir les jeux qui l'utilisent."
    override val notUsed = "Non utilisée"
    override fun card(suit: String, value: String, copy: Int?, games: List<String>) =
        "$suit $value" + (copy?.let { " numéro $it" } ?: "") +
            (if (games.isEmpty()) ", non nécessaire" else ", nécessaire pour ${games.joinToString(", ")}")

    override fun databaseError(message: String) =
        "Impossible d'accéder à la base de données : ($message). L'application est en mode dégradé sans persistance."
    override fun gamesError(message: String) = "Impossible de charger la liste des jeux : $message."
    override val ruleError = "Impossible de charger la documentation du jeu."
    override val documentError = "Impossible de charger le document."
    override val unknownError = "Erreur inconnue"
    override val unknownPage = "Page inconnue"
    override val howDidYouEndUpHere = "Comment êtes-vous arrivé·e ici ?!?"
    override val seeGames = "Voir les Jeux !"
    override val gameNotFound = "JEU INTROUVABLE"
    override val backToGames = "Retour aux Jeux"

    override fun suit(suit: Suit) = when (suit) {
        Suit.Stars -> "Étoiles"
        Suit.Spades -> "Piques"
        Suit.Hearts -> "Cœurs"
        Suit.Clubs -> "Trèfles"
        Suit.Diamonds -> "Carreaux"
        Suit.Florettes -> "Fleurettes"
        Suit.Wheels -> "Roues"
        Suit.Specials -> "Spéciales"
    }

    override fun special(value: String) = when (value) {
        "Butterfly" -> "Papillon"
        "Wolf" -> "Loup"
        "Phoenix" -> "Phénix"
        "Snake" -> "Serpent"
        "Dragon" -> "Dragon"
        "Monkey" -> "Singe"
        else -> value
    }

    override fun type(type: String) = when (type) {
        "Auction" -> "Enchères"
        "Bluffing" -> "Bluff"
        "Cooperative" -> "Coopératif"
        "Deduction" -> "Déduction"
        "Draw & Write" -> "Pioche & Écriture"
        "Drafting" -> "Draft"
        "Ladder Climbing" -> "Montée en Puissance"
        "Memory" -> "Mémoire"
        "Player Elimination" -> "Élimination de joueur·euse·s"
        "Predictive Bid" -> "Pari Prédictif"
        "Push Your Luck" -> "Stop ou Encore"
        "Set Collection" -> "Collection d'ensembles"
        "Single Loser" -> "Perdant Unique"
        "Team Based" -> "En Équipe"
        "Trick Taking" -> "Prise de pli"
        else -> type
    }

    override fun tradition(tradition: String) = when (tradition) {
        "Chinese" -> "Chinois"
        "French" -> "Français"
        else -> tradition
    }
}

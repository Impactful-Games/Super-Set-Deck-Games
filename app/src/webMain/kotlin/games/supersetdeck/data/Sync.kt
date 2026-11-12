package games.supersetdeck.data

import games.supersetdeck.data.Backup.BackupList

enum class SyncDirection { ToApp, Pause, ToBackup }

/** A list shown in the synchronization configuration, with the directions it allows. */
data class SyncRow(
    val id: String,
    val app: BackupList?,
    val backup: BackupList?,
    val allowed: Set<SyncDirection>,
    val default: SyncDirection,
) {
    /** The most recent modification on either side, to sort the rows. */
    val latest: Long get() = maxOf(app?.update ?: 0, backup?.update ?: 0)
}

/** What a synchronization proposes: the rows to configure, and the deletions propagated automatically. */
data class SyncPlan(
    val rows: List<SyncRow>,
    val automatic: Map<String, SyncDirection>,
)

object Sync {

    /** The pre-configuration of every list present in the application or in the backup (spec § 8.4). */
    fun plan(app: Backup, backup: Backup): SyncPlan {
        val rows = ArrayList<SyncRow>()
        val automatic = LinkedHashMap<String, SyncDirection>()
        (app.lists.keys + backup.lists.keys).forEach { id ->
            val a = app.lists[id]
            val b = backup.lists[id]
            val aDate = a?.update ?: 0
            val bDate = b?.update ?: 0
            val newer = if (aDate >= bDate) SyncDirection.ToBackup else SyncDirection.ToApp
            when {
                // No content on either side: a deletion, propagated from the most recent side.
                a?.content == null && b?.content == null -> if (aDate != bDate) automatic[id] = newer
                a != null && b != null && aDate == bDate && a.content.sameContent(b.content) -> {}
                b == null -> rows += SyncRow(id, a, null, setOf(SyncDirection.ToBackup, SyncDirection.Pause), SyncDirection.ToBackup)
                a == null -> rows += SyncRow(id, null, b, setOf(SyncDirection.ToApp, SyncDirection.Pause), SyncDirection.ToApp)
                else -> rows += SyncRow(id, a, b, SyncDirection.entries.toSet(), newer)
            }
        }
        rows.sortBy { it.latest }
        return SyncPlan(rows, automatic)
    }

    private fun Backup.Content?.sameContent(other: Backup.Content?) =
        this?.name == other?.name && this?.games?.toSet() == other?.games?.toSet()

    /** One change to apply to the application: a list taking the backup's state. */
    data class AppChange(val id: String, val list: BackupList)

    data class Result(val backup: Backup, val appChanges: List<AppChange>)

    /** Applies the chosen directions (spec § 8.6); lists of the backup left alone are kept as they are. */
    fun apply(app: Backup, backup: Backup, plan: SyncPlan, directions: Map<String, SyncDirection>): Result {
        val lists = LinkedHashMap(backup.lists)
        val changes = ArrayList<AppChange>()
        val chosen = plan.automatic + plan.rows.associate { it.id to (directions[it.id] ?: it.default) }
        chosen.forEach { (id, direction) ->
            when (direction) {
                SyncDirection.ToBackup -> app.lists[id]?.let { lists[id] = it }
                SyncDirection.ToApp -> backup.lists[id]?.let { changes += AppChange(id, it) }
                SyncDirection.Pause -> {}
            }
        }
        return Result(backup.copy(version = 1, lists = lists), changes)
    }
}

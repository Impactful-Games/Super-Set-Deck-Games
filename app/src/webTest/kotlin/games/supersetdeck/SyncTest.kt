package games.supersetdeck

import games.supersetdeck.data.Backup
import games.supersetdeck.data.Backup.BackupList
import games.supersetdeck.data.Backup.Content
import games.supersetdeck.data.Sync
import games.supersetdeck.data.SyncDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncTest {
    private fun list(update: Long, name: String? = "List", vararg games: String) =
        BackupList(update, if (name == null) null else Content(name, games.toList()))

    @Test
    fun identicalSidesNeedNothing() {
        val side = Backup(lists = mapOf("a" to list(10, "A", "Tichu")))
        val plan = Sync.plan(side, Backup(lists = mapOf("a" to list(10, "A", "Tichu"))))
        assertTrue(plan.rows.isEmpty())
        assertTrue(plan.automatic.isEmpty())
        assertTrue(side.sameAs(Backup(lists = mapOf("a" to list(10, "A", "Tichu")))))
    }

    @Test
    fun preConfiguration() {
        val app = Backup(
            lists = mapOf(
                "onlyApp" to list(5, "Only app"),
                "newerApp" to list(20, "Newer", "Tichu"),
                "sameDate" to list(30, "Same", "Tichu"),
                "deletedApp" to list(40, null),
                "tombstones" to list(50, null),
            )
        )
        val backup = Backup(
            lists = mapOf(
                "onlyBackup" to list(6, "Only backup"),
                "newerApp" to list(10, "Older"),
                "sameDate" to list(30, "Same", "Chimera"),
                "deletedApp" to list(35, "Kept in backup", "Tichu"),
                "tombstones" to list(45, null),
            )
        )
        val plan = Sync.plan(app, backup)
        val rows = plan.rows.associateBy { it.id }
        assertEquals(SyncDirection.ToBackup, rows.getValue("onlyApp").default)
        assertEquals(setOf(SyncDirection.ToBackup, SyncDirection.Pause), rows.getValue("onlyApp").allowed)
        assertEquals(SyncDirection.ToApp, rows.getValue("onlyBackup").default)
        assertEquals(setOf(SyncDirection.ToApp, SyncDirection.Pause), rows.getValue("onlyBackup").allowed)
        assertEquals(SyncDirection.ToBackup, rows.getValue("newerApp").default)
        // Equal dates, different content: from the application.
        assertEquals(SyncDirection.ToBackup, rows.getValue("sameDate").default)
        // Deleted on one side, existing on the other: both directions offered.
        assertEquals(SyncDirection.entries.toSet(), rows.getValue("deletedApp").allowed)
        // Tombstones on both sides: propagated automatically, from the most recent.
        assertEquals(mapOf("tombstones" to SyncDirection.ToBackup), plan.automatic)
        // Sorted by most recent modification, oldest first.
        assertEquals(listOf("onlyApp", "onlyBackup", "newerApp", "sameDate", "deletedApp"), plan.rows.map { it.id })
    }

    @Test
    fun applyKeepsDatesAndUntouchedLists() {
        val app = Backup(lists = mapOf("a" to list(20, "A", "Tichu"), "b" to list(5, "B")))
        val backup = Backup(lists = mapOf("a" to list(10, "A"), "b" to list(30, null), "c" to list(1, "C")))
        val plan = Sync.plan(app, backup)
        // "c", only in the backup, would be restored by default: paused, it stays as it is.
        val result = Sync.apply(app, backup, plan, mapOf("c" to SyncDirection.Pause))
        assertEquals(list(20, "A", "Tichu"), result.backup.lists["a"])
        assertEquals(list(1, "C"), result.backup.lists["c"])
        // The backup's tombstone, more recent, deletes the list locally with the backup's date.
        assertEquals(listOf(Sync.AppChange("b", list(30, null))), result.appChanges)
    }

    @Test
    fun pauseDoesNothing() {
        val app = Backup(lists = mapOf("a" to list(20, "A", "Tichu")))
        val backup = Backup()
        val plan = Sync.plan(app, backup)
        val result = Sync.apply(app, backup, plan, mapOf("a" to SyncDirection.Pause))
        assertTrue(result.backup.sameAs(backup))
        assertTrue(result.appChanges.isEmpty())
    }

    @Test
    fun theFileFormatIsKept() {
        val text = """
            {
                "version": 1,
                "lists": {
                    "favourites": { "update": 1730283723000, "content": { "name": "", "games": ["Tichu", "Lost_Cities"] } },
                    "c1": { "update": 1730284000000, "content": null, "unknown": true }
                }
            }
        """.trimIndent()
        val backup = Backup.decode(text)
        assertEquals(listOf("Tichu", "Lost_Cities"), backup.lists.getValue("favourites").content?.games)
        assertEquals(null, backup.lists.getValue("c1").content)
        assertTrue(Backup.decode(backup.encode()).sameAs(backup))
        assertTrue(backup.encode().contains("\"content\": null"))
        assertTrue(Backup.decode("  \n").lists.isEmpty())
    }
}

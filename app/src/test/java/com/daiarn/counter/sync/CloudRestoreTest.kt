package com.daiarn.counter.sync

import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CloudRestoreTest {
    @Test fun interruptedGoogleSwitchCanRebindOnlyAnEmptyDatabase() {
        assertTrue(canRebindEmptyGoogleAccount("old-anonymous", "google", true, false))
        assertTrue(canRebindEmptyGoogleAccount(null, "google", true, false))
        assertFalse(canRebindEmptyGoogleAccount("old-anonymous", "google", true, true))
        assertFalse(canRebindEmptyGoogleAccount("old-anonymous", "another-anonymous", false, false))
        assertFalse(canRebindEmptyGoogleAccount("google", "google", true, false))
    }
    private fun visit(id: String, revision: Long = 1, deleted: Boolean = false) =
        Visit(id = id, type = VisitType.BOTH, occurredAt = 100, revision = revision, deleted = deleted, pendingSync = false)

    @Test fun downloadsEveryPageIncludingDeletionMarkers() = runBlocking {
        val rows = listOf(visit("a"), visit("b", deleted = true), visit("c"), visit("d"))
        val offsets = mutableListOf<Int>()
        assertEquals(rows, downloadHistory(2) { offset -> offsets += offset; rows.drop(offset).take(2) })
        assertEquals(listOf(0, 2, 4), offsets)
    }

    @Test fun emptyHistoryReturnsNoRows() = runBlocking {
        assertTrue(downloadHistory { emptyList() }.isEmpty())
    }

    @Test fun laterPageFailureDoesNotReturnPartialHistory() = runBlocking {
        var returned = false
        try {
            downloadHistory(1) { offset -> if (offset == 0) listOf(visit("a")) else error("offline") }
            returned = true
        } catch (_: IllegalStateException) { }
        assertFalse(returned)
    }

    @Test fun overlappingPagesKeepHighestRevision() = runBlocking {
        val result = downloadHistory(2) { offset -> when (offset) {
            0 -> listOf(visit("a", 3, true), visit("b"))
            2 -> listOf(visit("a", 1))
            else -> emptyList()
        } }
        assertEquals(listOf(visit("a", 3, true), visit("b")), result)
    }
}

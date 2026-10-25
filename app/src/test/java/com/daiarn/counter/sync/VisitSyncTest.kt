package com.daiarn.counter.sync

import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VisitSyncTest {
    @Test fun rejectedRevisionIsNeverAcknowledged() = runBlocking {
        val store = Store(Visit(type = VisitType.POOP, occurredAt = 123, revision = 2))
        try {
            VisitSync(store, VisitCloud { throw CloudRevisionConflict() }).drain()
            fail("Expected conflict")
        } catch (_: CloudRevisionConflict) { }
        assertTrue(store.row.pendingSync)
        assertEquals(2L, store.row.revision)
    }
    private class Store(var row: Visit) : PendingVisitStore {
        override suspend fun pending() = if (row.pendingSync) listOf(row) else emptyList()
        override suspend fun acknowledge(id: String, revision: Long) {
            if (row.id == id && row.revision == revision) row = row.copy(pendingSync = false)
        }
    }

    @Test fun networkFailureLeavesTheVisitPendingForRetry() = runBlocking {
        val store = Store(Visit(type = VisitType.BOTH, occurredAt = 123))
        var attempts = 0
        val sync = VisitSync(store, VisitCloud {
            attempts++
            if (attempts == 1) error("Connection lost")
        })
        try { sync.drain(); fail("Expected network failure") } catch (_: IllegalStateException) { }
        assertTrue(store.row.pendingSync)
        sync.drain()
        assertFalse(store.row.pendingSync)
        assertEquals(2, attempts)
    }

    @Test fun editDuringUploadIsUploadedBeforeItIsAcknowledged() = runBlocking {
        val store = Store(Visit(type = VisitType.POOP, occurredAt = 123))
        val uploaded = mutableListOf<Visit>()
        VisitSync(store, VisitCloud { visit ->
            uploaded.add(visit)
            if (visit.revision == 1L) store.row = store.row.copy(type = VisitType.BOTH, revision = 2)
        }).drain()
        assertEquals(listOf(1L, 2L), uploaded.map { it.revision })
        assertEquals(VisitType.BOTH, uploaded.last().type)
        assertFalse(store.row.pendingSync)
    }

    @Test fun deletionDuringUploadCannotBeClearedByTheOldAcknowledgement() = runBlocking {
        val store = Store(Visit(type = VisitType.PEE, occurredAt = 123))
        val uploaded = mutableListOf<Visit>()
        VisitSync(store, VisitCloud { visit ->
            uploaded.add(visit)
            if (visit.revision == 1L) store.row = store.row.copy(deleted = true, revision = 2)
        }).drain()
        assertFalse(uploaded.first().deleted)
        assertTrue(uploaded.last().deleted)
        assertTrue(store.row.deleted)
        assertFalse(store.row.pendingSync)
    }

    @Test fun acknowledgedVisitIsNotUploadedAgain() = runBlocking {
        val store = Store(Visit(type = VisitType.PEE, occurredAt = 123))
        var uploads = 0
        val sync = VisitSync(store, VisitCloud { uploads++ })
        sync.drain()
        sync.drain()
        assertEquals(1, uploads)
    }
}

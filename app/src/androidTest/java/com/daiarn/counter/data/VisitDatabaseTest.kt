package com.daiarn.counter.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitDatabaseTest {
    private lateinit var db: CounterDatabase
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val now = Instant.parse("2026-10-10T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before fun open() {
        context.deleteDatabase("counter-test.db")
        db = Room.databaseBuilder(context, CounterDatabase::class.java, "counter-test.db").build()
    }

    @After fun close() {
        db.close()
        context.deleteDatabase("counter-test.db")
    }

    @Test fun rapidLoggingPersistsAcrossReopening() = runBlocking {
        val repository = VisitRepository(db.visits(), clock)
        repeat(25) { repository.log(VisitType.BOTH) }
        db.close()
        db = Room.databaseBuilder(context, CounterDatabase::class.java, "counter-test.db").build()
        val entries = db.visits().observeVisits().first()
        assertEquals(25, entries.size)
        assertEquals(25, entries.map { it.id }.toSet().size)
        assertEquals(25, db.visits().observePendingCount().first())
    }

    @Test fun editsAndDeletesStayPendingAndOldAcknowledgementCannotEraseNewChange() = runBlocking {
        val dao = db.visits()
        val repository = VisitRepository(dao, clock)
        repository.log(VisitType.POOP)
        val saved = dao.observeVisits().first().single()
        repository.save(saved.id, VisitType.BOTH, now.minusSeconds(60), "forgotten note")
        assertEquals(0, dao.acknowledge(saved.id, saved.revision))
        val edited = dao.observeVisits().first().single()
        assertEquals(VisitType.BOTH, edited.type)
        assertEquals("forgotten note", edited.note)
        assertEquals(2L, edited.revision)
        repository.delete(saved.id)
        assertTrue(dao.observeVisits().first().isEmpty())
        assertTrue(dao.pendingVisits().single().deleted)
        assertEquals(0, dao.acknowledge(edited.id, edited.revision))
        assertEquals(1, dao.acknowledge(edited.id, 3))
        assertEquals(0, dao.observePendingCount().first())
    }

    @Test fun futureManualVisitIsRejectedWithoutWriting() = runBlocking {
        val repository = VisitRepository(db.visits(), clock)
        try {
            repository.save(null, VisitType.PEE, now.plusSeconds(1), "")
            fail("Future visit was accepted")
        } catch (_: IllegalArgumentException) {
            assertTrue(db.visits().observeVisits().first().isEmpty())
        }
    }

    @Test fun restoreIsIdempotentAndPreservesPendingEditsAndDeletions() = runBlocking {
        val dao = db.visits()
        val original = Visit(type = VisitType.BOTH, occurredAt = now.toEpochMilli(), pendingSync = false)
        val deleted = original.copy(id = "deleted")
        dao.insert(original)
        dao.insert(deleted)
        dao.edit(original.id, VisitType.PEE, original.occurredAt, "local edit")
        dao.delete(deleted.id)
        val new = original.copy(id = "new", revision = 5)
        assertEquals(1, dao.restore(listOf(original.copy(revision = 9), deleted.copy(revision = 9), new)))
        assertEquals(0, dao.restore(listOf(new)))
        assertEquals(2, dao.observeVisits().first().size)
        val edited = dao.observeVisits().first().first { it.id == original.id }
        assertEquals("local edit", edited.note)
        assertEquals(10L, edited.revision)
        assertTrue(dao.pendingVisits().first { it.id == deleted.id }.deleted)
        assertEquals(2, dao.observePendingCount().first())
    }

    @Test fun restoreAppliesNewerCloudDeletionButDoesNotRollBackNewerLocalRows() = runBlocking {
        val dao = db.visits()
        val original = Visit(type = VisitType.BOTH, occurredAt = now.toEpochMilli(), pendingSync = false, revision = 5)
        dao.insert(original)
        dao.restore(listOf(original.copy(revision = 3, note = "old")))
        assertEquals(original, dao.observeVisits().first().single())
        dao.restore(listOf(original.copy(revision = 6, deleted = true)))
        assertTrue(dao.observeVisits().first().isEmpty())
        assertEquals(0, dao.observePendingCount().first())
    }

    @Test fun invalidCloudBatchRollsBackEveryInsert() = runBlocking {
        val dao = db.visits()
        val valid = Visit(type = VisitType.BOTH, occurredAt = now.toEpochMilli(), pendingSync = false)
        try {
            dao.restore(listOf(valid, valid.copy(id = "invalid", revision = 0)))
            fail("Invalid batch accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, dao.storedCount())
    }

    @Test fun restoreThenSyncThenRestoreRetainsLocalEditAndDeletion() = runBlocking {
        val dao = db.visits()
        val edited = Visit(type = VisitType.PEE, occurredAt = now.toEpochMilli(), note = "local edit", revision = 2)
        val deleted = edited.copy(id = "deleted", deleted = true)
        dao.insert(edited)
        dao.insert(deleted)
        val remote = mutableMapOf(
            edited.id to edited.copy(type = VisitType.POOP, note = "remote", revision = 9, pendingSync = false),
            deleted.id to deleted.copy(deleted = false, revision = 9, pendingSync = false)
        )
        dao.restore(remote.values.toList())
        com.daiarn.counter.sync.VisitSync(object : com.daiarn.counter.sync.PendingVisitStore {
            override suspend fun pending() = dao.pendingVisits()
            override suspend fun acknowledge(id: String, revision: Long) { dao.acknowledge(id, revision) }
        }, com.daiarn.counter.sync.VisitCloud { visit ->
            assertTrue(visit.revision > remote.getValue(visit.id).revision)
            remote[visit.id] = visit.copy(pendingSync = false)
        }).drain()
        dao.restore(remote.values.toList())
        val restored = dao.observeVisits().first().single()
        assertEquals(VisitType.PEE, restored.type)
        assertEquals("local edit", restored.note)
        assertEquals(10L, restored.revision)
        assertEquals(0, dao.observePendingCount().first())
        assertTrue(remote.getValue(deleted.id).deleted)
    }
}

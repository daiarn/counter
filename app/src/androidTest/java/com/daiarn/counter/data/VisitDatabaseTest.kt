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
}

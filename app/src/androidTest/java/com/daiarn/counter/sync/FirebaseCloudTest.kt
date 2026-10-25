package com.daiarn.counter.sync

import androidx.test.platform.app.InstrumentationRegistry
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.daiarn.counter.generated.CounterConnector
import com.daiarn.counter.generated.GetVisitQuery
import com.daiarn.counter.generated.execute
import com.daiarn.counter.generated.getInstance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class FirebaseCloudTest {
    @Test fun anonymousSdkUploadsAreOwnerScopedAndStaleChangesCannotRestoreDeletion() = runBlocking {
        // Live test is opt-in; it creates synthetic rows and disposable anonymous users.
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseLive") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = FirebaseApp.getInstance().options
        val suffix = UUID.randomUUID().toString()
        val first = FirebaseApp.initializeApp(context, options, "test-a-$suffix")
        val second = FirebaseApp.initializeApp(context, options, "test-b-$suffix")
        val firstCloud = FirebaseVisitCloud(context, first)
        val secondCloud = FirebaseVisitCloud(context, second)
        val firstConnector = CounterConnector.getInstance(first)
        val secondConnector = CounterConnector.getInstance(second)
        val visit = Visit(type = VisitType.BOTH, occurredAt = Instant.now().minusSeconds(60).toEpochMilli(),
            note = "Synthetic integration test")
        suspend fun read(connector: CounterConnector): GetVisitQuery.Data.Visit? =
            connector.getVisit.execute(UUID.fromString(visit.id)).data.visit
        try {
            firstCloud.authenticate()
            secondCloud.authenticate()
            assertTrue(FirebaseAuth.getInstance(first).currentUser!!.isAnonymous)
            assertNotEquals(FirebaseAuth.getInstance(first).uid, FirebaseAuth.getInstance(second).uid)
            firstCloud.save(visit)
            assertEquals("BOTH", read(firstConnector)!!.type)
            assertNull(read(secondConnector))
            val edited = visit.copy(type = VisitType.POOP, note = "Edited synthetic test", revision = 2)
            firstCloud.save(edited)
            assertEquals("POOP", read(firstConnector)!!.type)
            val deleted = edited.copy(deleted = true, revision = 3)
            firstCloud.save(deleted)
            // Simulate an old request arriving after the deletion has been accepted.
            try {
                firstCloud.save(visit)
                fail("Stale upload must not be acknowledged")
            } catch (_: CloudRevisionConflict) { }
            firstCloud.save(deleted)
            val final = read(firstConnector)!!
            assertEquals(3L, final.revision)
            assertTrue(final.deleted)
            assertEquals("", final.note)
        } finally {
            // Keep only a synthetic deletion marker; never touch the app's real user/data.
            runCatching { firstCloud.save(visit.copy(deleted = true, revision = 4)) }
            for (app in listOf(first, second)) {
                runCatching { FirebaseAuth.getInstance(app).currentUser?.delete()?.await() }
                context.getSharedPreferences("sync_identity_${app.name}", 0).edit().clear().commit()
            }
            firstConnector.dataConnect.close()
            secondConnector.dataConnect.close()
            first.delete()
            second.delete()
        }
    }
}

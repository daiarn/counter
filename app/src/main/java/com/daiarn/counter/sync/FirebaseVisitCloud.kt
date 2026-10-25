package com.daiarn.counter.sync

import android.content.Context
import com.daiarn.counter.data.Visit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.daiarn.counter.generated.CounterConnector
import com.daiarn.counter.generated.execute
import com.daiarn.counter.generated.getInstance
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

class SyncIdentityChanged : Exception()
class CloudRevisionConflict : Exception("Cloud history has a newer change. Restore from cloud to keep and resync your local changes.")

class FirebaseVisitCloud(context: Context, app: FirebaseApp = FirebaseApp.getInstance()) : VisitCloud {
    private val identity = context.getSharedPreferences(
        if (app.name == FirebaseApp.DEFAULT_APP_NAME) "sync_identity" else "sync_identity_${app.name}",
        Context.MODE_PRIVATE
    )
    private val auth = FirebaseAuth.getInstance(app)
    private val connector by lazy {
        CounterConnector.getInstance(app)
    }

    suspend fun authenticate() {
        val boundUid = identity.getString("owner_uid", null)
        // Never reassign already-owned records if anonymous credentials disappear.
        if (boundUid != null && auth.currentUser?.uid != boundUid) throw SyncIdentityChanged()
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Anonymous authentication failed")
        if (boundUid == null) check(identity.edit().putString("owner_uid", user.uid).commit())
    }

    override suspend fun save(visit: Visit) {
        val id = UUID.fromString(visit.id)
        val current = connector.getVisit.execute(id).data.visit
        val instant = Instant.ofEpochMilli(visit.occurredAt)
        val timestamp = Timestamp(instant.epochSecond, instant.nano)
        val note = if (visit.deleted) "" else visit.note
        // Insert-only creation plus revision-filtered updates prevents late retries from
        // overwriting newer changes. A concurrent initial insert fails and is retried.
        if (current == null) {
            connector.createVisit.execute(id, visit.type.name, timestamp, note, visit.deleted, visit.revision)
        } else {
            val updated = connector.updateVisit.execute(id, visit.type.name, timestamp, note, visit.deleted, visit.revision)
            if (updated.data.visit_updateMany == 0) {
                val remote = connector.getVisit.execute(id).data.visit
                if (remote == null || remote.revision != visit.revision || remote.type != visit.type.name ||
                    remote.occurredAt != timestamp || remote.note != note || remote.deleted != visit.deleted) {
                    throw CloudRevisionConflict()
                }
            }
        }
    }
}

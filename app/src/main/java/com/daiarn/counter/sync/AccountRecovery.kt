package com.daiarn.counter.sync

import android.content.Context
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitDao
import com.daiarn.counter.data.VisitType
import com.daiarn.counter.generated.CounterConnector
import com.daiarn.counter.generated.getInstance
import com.daiarn.counter.generated.execute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Instant

/** Serializes authentication changes, local writes, uploads, and restoration. */
object AccountAccess { val lock = Mutex() }

class AccountRecovery(private val context: Context, private val dao: VisitDao) {
    private val auth = FirebaseAuth.getInstance()
    private val identity = context.getSharedPreferences("sync_identity", Context.MODE_PRIVATE)

    // Caller holds AccountAccess.lock. Never move an existing local database to another UID.
    suspend fun signInWithGoogle(idToken: String) = withContext(NonCancellable) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val bound = identity.getString("owner_uid", null)
        val hasData = dao.storedCount() != 0
        check(!hasData || (current != null && (bound == null || bound == current.uid))) {
            "The saved visits belong to another installation identity. Contact the project administrator before signing in."
        }
        if (current?.isAnonymous == true) {
            try {
                current.linkWithCredential(credential).await()
            } catch (collision: FirebaseAuthUserCollisionException) {
                check(!hasData) {
                    "This Google account already has a history. Your local visits are safe; administrator-assisted merging is needed before switching accounts."
                }
                auth.signInWithCredential(credential).await()
            }
        } else if (current == null || !hasData) {
            auth.signInWithCredential(credential).await()
        } else {
            current.reauthenticate(credential).await()
        }
        val user = checkNotNull(auth.currentUser)
        check(user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID })
        check(identity.edit().putString("owner_uid", user.uid).commit()) { "Could not save account identity. Try signing in again." }
    }

    suspend fun reconcileIdentity() {
        val user = auth.currentUser ?: return
        val bound = identity.getString("owner_uid", null)
        if (canRebindEmptyGoogleAccount(bound, user.uid,
                user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }, dao.storedCount() != 0)) {
            check(identity.edit().putString("owner_uid", user.uid).commit()) { "Could not save account identity. Try again." }
        }
    }

    suspend fun restore(): Int {
        reconcileIdentity()
        val user = checkNotNull(auth.currentUser) { "Sign in with Google first." }
        check(user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) { "Sign in with Google first." }
        FirebaseVisitCloud(context).authenticate()
        val connector = CounterConnector.getInstance()
        val visits = downloadHistory { offset ->
            val page = connector.listVisits.execute(offset).data.visits
            page.map {
                require(it.revision in 1 until Long.MAX_VALUE) { "Invalid cloud revision" }
                Visit(id = it.id.toString(), type = VisitType.valueOf(it.type),
                    occurredAt = Instant.ofEpochSecond(it.occurredAt.seconds, it.occurredAt.nanoseconds.toLong()).toEpochMilli(),
                    note = it.note, deleted = it.deleted, revision = it.revision, pendingSync = false)
            }
        }
        check(auth.currentUser?.uid == user.uid) { "Account changed. Restore cancelled." }
        // No database changes until every page has downloaded and validated successfully.
        return dao.restore(visits)
    }
}


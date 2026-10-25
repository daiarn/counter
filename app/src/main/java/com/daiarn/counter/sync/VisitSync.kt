package com.daiarn.counter.sync

import com.daiarn.counter.data.Visit

interface PendingVisitStore {
    suspend fun pending(): List<Visit>
    suspend fun acknowledge(id: String, revision: Long)
}

fun interface VisitCloud {
    suspend fun save(visit: Visit)
}

/** A server failure leaves the current change pending. A stale acknowledgement cannot clear an edit. */
class VisitSync(private val store: PendingVisitStore, private val cloud: VisitCloud) {
    suspend fun drain() {
        while (true) {
            val pending = store.pending()
            if (pending.isEmpty()) return
            for (visit in pending) {
                cloud.save(visit)
                store.acknowledge(visit.id, visit.revision)
            }
        }
    }
}

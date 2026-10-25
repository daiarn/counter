package com.daiarn.counter.sync

import com.daiarn.counter.data.Visit

fun canRebindEmptyGoogleAccount(boundUid: String?, currentUid: String, google: Boolean, hasData: Boolean): Boolean =
    google && !hasData && boundUid != currentUid

/** Downloads the complete history before the caller commits any local changes. */
suspend fun downloadHistory(pageSize: Int = 500, fetch: suspend (Int) -> List<Visit>): List<Visit> {
    require(pageSize > 0)
    val visits = linkedMapOf<String, Visit>()
    var offset = 0
    do {
        val page = fetch(offset)
        page.forEach { visit ->
            require(!visit.pendingSync && visit.revision in 1 until Long.MAX_VALUE) { "Invalid cloud record" }
            val previous = visits[visit.id]
            if (previous == null || visit.revision >= previous.revision) visits[visit.id] = visit
        }
        offset += page.size
    } while (page.size == pageSize)
    return visits.values.toList()
}

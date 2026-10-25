package com.daiarn.counter.data

import java.time.Clock
import java.time.Instant

class VisitRepository(private val dao: VisitDao, private val clock: Clock = Clock.systemUTC()) {
    val visits = dao.observeVisits()
    val pendingCount = dao.observePendingCount()

    suspend fun log(type: VisitType) = dao.insert(Visit(type = type, occurredAt = clock.millis()))

    suspend fun save(id: String?, type: VisitType, instant: Instant, note: String) {
        require(!instant.isAfter(clock.instant())) { "Future times are not allowed." }
        if (id == null) {
            dao.insert(Visit(type = type, occurredAt = instant.toEpochMilli(), note = note.trim()))
        } else {
            check(dao.edit(id, type, instant.toEpochMilli(), note.trim()) == 1) {
                "This visit no longer exists."
            }
        }
    }

    suspend fun delete(id: String) {
        check(dao.delete(id) == 1) { "This visit no longer exists." }
    }
}

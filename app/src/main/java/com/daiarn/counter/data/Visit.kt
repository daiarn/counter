package com.daiarn.counter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class VisitType(val label: String, val includesPoop: Boolean, val includesPee: Boolean) {
    POOP("Poop", true, false),
    PEE("Pee", false, true),
    BOTH("Both", true, true)
}

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: VisitType,
    val occurredAt: Long,
    val note: String = "",
    // Retain pending deletions so a later sync can remove their cloud counterparts.
    val deleted: Boolean = false,
    val pendingSync: Boolean = true,
    val revision: Long = 1
)

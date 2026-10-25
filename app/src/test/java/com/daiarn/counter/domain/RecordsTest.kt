package com.daiarn.counter.domain

import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class RecordsTest {
    private fun visit(type: VisitType, time: String, deleted: Boolean = false) =
        Visit(type = type, occurredAt = Instant.parse(time).toEpochMilli(), deleted = deleted)

    @Test fun dailyAndMonthlyRecordsCountBothAndKeepAllTiesNewestFirst() {
        val records = visitRecords(listOf(
            visit(VisitType.BOTH, "2026-01-01T12:00:00Z"),
            visit(VisitType.POOP, "2026-01-01T13:00:00Z"),
            visit(VisitType.PEE, "2026-01-02T12:00:00Z"),
            visit(VisitType.BOTH, "2026-02-01T12:00:00Z"),
            visit(VisitType.POOP, "2026-02-01T13:00:00Z"),
            visit(VisitType.PEE, "2026-02-01T14:00:00Z"),
            visit(VisitType.BOTH, "2026-02-01T15:00:00Z", deleted = true)
        ), ZoneOffset.UTC)
        assertEquals(VisitRecord(2, listOf(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-01"))), records.poopDay)
        assertEquals(VisitRecord(2, listOf(LocalDate.parse("2026-02-01"))), records.peeDay)
        val months = VisitRecord(2, listOf(YearMonth.parse("2026-02"), YearMonth.parse("2026-01")))
        assertEquals(months, records.poopMonth)
        assertEquals(months, records.peeMonth)
    }

    @Test fun monthsSumAcrossDaysAndDoNotMergeYears() {
        val records = visitRecords(listOf(
            visit(VisitType.POOP, "2025-01-01T12:00:00Z"),
            visit(VisitType.POOP, "2025-01-02T12:00:00Z"),
            visit(VisitType.POOP, "2026-01-01T12:00:00Z")
        ), ZoneOffset.UTC)
        assertEquals(VisitRecord(2, listOf(YearMonth.parse("2025-01"))), records.poopMonth)
        assertNull(records.peeDay)
        assertNull(records.peeMonth)
    }

    @Test fun emptyAndDeletedOnlyHistoryHaveNoRecords() {
        val empty = VisitRecords(null, null, null, null)
        assertEquals(empty, visitRecords(emptyList(), ZoneOffset.UTC))
        assertEquals(empty, visitRecords(listOf(visit(VisitType.BOTH, "2026-01-01T12:00:00Z", true)), ZoneOffset.UTC))
    }

    @Test fun recordsFollowLocalDayAndMonthBoundariesAndTimezoneChanges() {
        val visits = listOf(
            visit(VisitType.BOTH, "2026-01-31T21:59:59Z"),
            visit(VisitType.BOTH, "2026-01-31T22:00:00Z")
        )
        val local = visitRecords(visits, ZoneId.of("Europe/Vilnius"))
        assertEquals(VisitRecord(1, listOf(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-31"))), local.poopDay)
        assertEquals(VisitRecord(1, listOf(YearMonth.parse("2026-02"), YearMonth.parse("2026-01"))), local.peeMonth)
        val utc = visitRecords(visits, ZoneOffset.UTC)
        assertEquals(VisitRecord(2, listOf(LocalDate.parse("2026-01-31"))), utc.poopDay)
        assertEquals(VisitRecord(2, listOf(YearMonth.parse("2026-01"))), utc.peeMonth)
    }
}

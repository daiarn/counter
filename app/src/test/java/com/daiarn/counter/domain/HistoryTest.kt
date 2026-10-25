package com.daiarn.counter.domain

import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class HistoryTest {
    @Test fun hourlyChartUsesLocalDayAndCountsBothAndExcludesDeleted() {
        val entries = listOf(
            visit(VisitType.POOP, "2026-10-09T20:59:59Z"),
            visit(VisitType.BOTH, "2026-10-09T21:00:00Z"),
            visit(VisitType.PEE, "2026-10-09T21:59:59Z"),
            visit(VisitType.POOP, "2026-10-09T22:00:00Z"),
            visit(VisitType.BOTH, "2026-10-09T22:30:00Z", deleted = true)
        )
        val hours = totalsByHour(entries, LocalDate.parse("2026-10-10"), ZoneId.of("Europe/Vilnius"))
        assertEquals(24, hours.size)
        assertEquals(Totals(1, 2), hours[0])
        assertEquals(Totals(1, 0), hours[1])
        assertTrue(hours.drop(2).all { it == Totals() })
        assertEquals(List(24) { Totals() }, totalsByHour(entries, LocalDate.parse("2026-10-11"), ZoneOffset.UTC))
    }

    @Test fun repeatedClockHourCombinesBothOffsetsWithoutLosingVisits() {
        val entries = listOf(
            visit(VisitType.POOP, "2026-10-25T00:30:00Z"),
            visit(VisitType.PEE, "2026-10-25T01:30:00Z")
        )
        val hours = totalsByHour(entries, LocalDate.parse("2026-10-25"), ZoneId.of("Europe/Vilnius"))
        assertEquals(Totals(1, 1), hours[3])
        assertEquals(1, hours.sumOf { it.poop })
        assertEquals(1, hours.sumOf { it.pee })
    }
    private fun visit(type: VisitType, time: String, deleted: Boolean = false) =
        Visit(type = type, occurredAt = Instant.parse(time).toEpochMilli(), deleted = deleted)

    @Test fun bothIsOneVisitButContributesToBothTotals() {
        val visits = listOf(visit(VisitType.BOTH, "2026-10-10T09:00:00Z"))
        assertEquals(1, visits.size)
        assertEquals(Totals(1, 1), totals(visits))
    }

    @Test fun deletedVisitsDoNotAffectTotalsOrLastVisit() {
        val entries = listOf(
            visit(VisitType.PEE, "2026-10-10T09:00:00Z"),
            visit(VisitType.BOTH, "2026-10-10T10:00:00Z", deleted = true)
        )
        assertEquals(Totals(0, 1), totals(entries))
        assertNull(lastVisit(entries, true))
        assertEquals(Instant.parse("2026-10-10T09:00:00Z"), lastVisit(entries, false))
    }

    @Test fun midnightSeparatesLocalDays() {
        val entries = listOf(
            visit(VisitType.POOP, "2026-10-09T20:59:59Z"),
            visit(VisitType.PEE, "2026-10-09T21:00:00Z")
        )
        val zone = ZoneId.of("Europe/Vilnius")
        assertEquals(Totals(1, 0), totals(visitsOn(entries, LocalDate.parse("2026-10-09"), zone)))
        assertEquals(Totals(0, 1), totals(visitsOn(entries, LocalDate.parse("2026-10-10"), zone)))
    }

    @Test fun timezoneChangeRegroupsExistingInstants() {
        val entries = listOf(visit(VisitType.BOTH, "2026-10-09T23:30:00Z"))
        assertEquals(Totals(1, 1), totalsByDay(entries, ZoneId.of("Europe/Vilnius"))[LocalDate.parse("2026-10-10")])
        assertEquals(Totals(1, 1), totalsByDay(entries, ZoneId.of("America/New_York"))[LocalDate.parse("2026-10-09")])
    }

    @Test fun latestVisitsAreIndependentOfSelectedDay() {
        val entries = listOf(
            visit(VisitType.BOTH, "2026-10-09T09:00:00Z"),
            visit(VisitType.PEE, "2026-10-10T10:00:00Z")
        )
        assertEquals(Instant.parse("2026-10-09T09:00:00Z"), lastVisit(entries, true))
        assertEquals(Instant.parse("2026-10-10T10:00:00Z"), lastVisit(entries, false))
    }

    @Test fun emptyHistoryStartsToday() {
        val today = LocalDate.parse("2026-10-10")
        assertEquals(today, firstHistoryDay(emptyList(), today, ZoneOffset.UTC))
        assertEquals("Not recorded yet", elapsedLabel(null, Instant.now()))
    }

    @Test fun earliestEntryDefinesRangeIncludingEmptyDays() {
        val entries = listOf(visit(VisitType.PEE, "2026-10-07T09:00:00Z"))
        assertEquals(LocalDate.parse("2026-10-07"), firstHistoryDay(entries, LocalDate.parse("2026-10-10"), ZoneOffset.UTC))
    }

    @Test(expected = IllegalArgumentException::class)
    fun daylightSavingGapIsRejected() {
        resolveLocalTime(LocalDateTime.parse("2026-03-29T03:30"), ZoneId.of("Europe/Vilnius"), null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun daylightSavingOverlapRequiresOffset() {
        resolveLocalTime(LocalDateTime.parse("2026-10-25T03:30"), ZoneId.of("Europe/Vilnius"), null)
    }

    @Test fun daylightSavingOverlapSupportsBothInstants() {
        val time = LocalDateTime.parse("2026-10-25T03:30")
        val zone = ZoneId.of("Europe/Vilnius")
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), resolveLocalTime(time, zone, ZoneOffset.ofHours(3)))
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), resolveLocalTime(time, zone, ZoneOffset.ofHours(2)))
    }
}

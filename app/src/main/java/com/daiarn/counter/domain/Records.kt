package com.daiarn.counter.domain

import com.daiarn.counter.data.Visit
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class VisitRecord<T>(val count: Int, val periods: List<T>)

data class VisitRecords(
    val poopDay: VisitRecord<LocalDate>?,
    val peeDay: VisitRecord<LocalDate>?,
    val poopMonth: VisitRecord<YearMonth>?,
    val peeMonth: VisitRecord<YearMonth>?
)

fun visitRecords(visits: List<Visit>, zone: ZoneId): VisitRecords {
    val daily = totalsByDay(visits, zone)
    val monthly = daily.entries.groupBy { YearMonth.from(it.key) }.mapValues { (_, days) ->
        Totals(days.sumOf { it.value.poop }, days.sumOf { it.value.pee })
    }
    return VisitRecords(
        record(daily) { it.poop }, record(daily) { it.pee },
        record(monthly) { it.poop }, record(monthly) { it.pee }
    )
}

private fun <T : Comparable<T>> record(totals: Map<T, Totals>, count: (Totals) -> Int): VisitRecord<T>? {
    val maximum = totals.values.maxOfOrNull(count) ?: return null
    if (maximum == 0) return null
    return VisitRecord(maximum, totals.filterValues { count(it) == maximum }.keys.sortedDescending())
}

package com.daiarn.counter.domain

import com.daiarn.counter.data.Visit
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

data class Totals(val poop: Int = 0, val pee: Int = 0)

/** Local clock-hour buckets; a repeated daylight-saving hour shares one bucket. */
fun totalsByHour(visits: List<Visit>, date: LocalDate, zone: ZoneId): List<Totals> {
    val grouped = visitsOn(visits, date, zone)
        .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).hour }
    return List(24) { hour -> totals(grouped[hour].orEmpty()) }
}

fun visitsOn(visits: List<Visit>, date: LocalDate, zone: ZoneId): List<Visit> =
    visits.filter { !it.deleted && Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() == date }

fun totals(visits: List<Visit>): Totals = Totals(
    poop = visits.count { !it.deleted && it.type.includesPoop },
    pee = visits.count { !it.deleted && it.type.includesPee }
)

fun totalsByDay(visits: List<Visit>, zone: ZoneId): Map<LocalDate, Totals> = visits
    .filterNot { it.deleted }
    .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }
    .mapValues { totals(it.value) }

fun firstHistoryDay(visits: List<Visit>, today: LocalDate, zone: ZoneId): LocalDate =
    visits.filterNot { it.deleted }.minOfOrNull {
        Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate()
    }?.coerceAtMost(today) ?: today

fun lastVisit(visits: List<Visit>, poop: Boolean): Instant? = visits
    .filter { !it.deleted && if (poop) it.type.includesPoop else it.type.includesPee }
    .maxOfOrNull { it.occurredAt }?.let(Instant::ofEpochMilli)

fun elapsedLabel(last: Instant?, now: Instant): String {
    if (last == null) return "Not recorded yet"
    val minutes = Duration.between(last, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes == 0L -> "Less than a minute ago"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} h ${minutes % 60} min ago"
        else -> "${minutes / 1440} d ${(minutes % 1440) / 60} h ago"
    }
}

// Reject daylight-saving gaps. Repeated local times require an explicit offset.
fun resolveLocalTime(local: LocalDateTime, zone: ZoneId, offset: ZoneOffset?): Instant {
    val offsets = zone.rules.getValidOffsets(local)
    require(offsets.isNotEmpty()) { "This time does not exist because the clocks change. Choose another time." }
    val chosen = if (offsets.size == 1) offsets.first() else {
        require(offset != null && offset in offsets) { "This time occurs twice. Choose a UTC offset." }
        offset
    }
    return local.toInstant(chosen)
}

package com.daiarn.counter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.daiarn.counter.data.Visit
import com.daiarn.counter.domain.VisitRecord
import com.daiarn.counter.domain.visitRecords
import com.daiarn.counter.ui.theme.Gold
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val recordDayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val recordMonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
fun RecordsScreen(visits: List<Visit>, zone: ZoneId) {
    val records = remember(visits, zone) { visitRecords(visits, zone) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("records-list"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Records", style = MaterialTheme.typography.headlineSmall)
            Text("Your all-time highs. Both visits count toward poop and pee.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        item { Text("Daily records", style = MaterialTheme.typography.titleLarge) }
        item { RecordCard("Most poop in a day", "POOP", records.poopDay, "days") { it.format(recordDayFormat) } }
        item { RecordCard("Most pee in a day", "PEE", records.peeDay, "days") { it.format(recordDayFormat) } }
        item { Text("Monthly records", style = MaterialTheme.typography.titleLarge) }
        item { RecordCard("Most poop in a month", "POOP", records.poopMonth, "months") { it.format(recordMonthFormat) } }
        item { RecordCard("Most pee in a month", "PEE", records.peeMonth, "months") { it.format(recordMonthFormat) } }
    }
}

@Composable
private fun <T> RecordCard(title: String, glyph: String, record: VisitRecord<T>?, periods: String, format: (T) -> String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CounterGlyph(glyph, Gold, Modifier.size(26.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            if (record == null) {
                Text("Not recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${record.count} ${if (record.count == 1) "time" else "times"}",
                    color = Gold, style = MaterialTheme.typography.headlineMedium)
                if (record.periods.size == 1) {
                    Text(format(record.periods.first()))
                } else {
                    Text("Record shared by ${record.periods.size} $periods")
                    Text("Most recent: ${format(record.periods.first())}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

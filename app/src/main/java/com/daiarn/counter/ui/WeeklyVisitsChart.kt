package com.daiarn.counter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.daiarn.counter.domain.Totals
import com.daiarn.counter.ui.theme.Gold
import com.daiarn.counter.ui.theme.Mist
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weeklyDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val accessibleDateFormat = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH)
private val PeeBlue = Color(0xFFA5DCEF)

@Composable
fun WeeklyVisitsChart(daily: Map<LocalDate, Totals>, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val currentMonday = today.minusDays(today.dayOfWeek.value - 1L)
    // A null selection follows the current week through midnight and timezone changes.
    var selectedWeek by rememberSaveable { mutableStateOf<String?>(null) }
    val monday = selectedWeek?.let(LocalDate::parse)?.coerceAtMost(currentMonday) ?: currentMonday
    val dates = List(7) { monday.plusDays(it.toLong()) }
    val counts = dates.map { if (it <= today) daily[it] ?: Totals() else null }
    val maximum = counts.filterNotNull().maxOf { maxOf(it.poop, it.pee) }.coerceAtLeast(1)
    val poop = counts.filterNotNull().sumOf { it.poop }
    val pee = counts.filterNotNull().sumOf { it.pee }
    val textMeasurer = rememberTextMeasurer()
    val valueStyle = MaterialTheme.typography.labelSmall

    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Weekly trends", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedWeek = monday.minusWeeks(1).toString() },
                    modifier = Modifier.semantics { contentDescription = "Previous week" }) {
                    CounterGlyph("Previous", Gold, Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (monday == currentMonday) "This week" else "Week of ${monday.format(weeklyDateFormat)}",
                        style = MaterialTheme.typography.titleSmall)
                    Text("${monday.format(weeklyDateFormat)} – ${dates.last().format(weeklyDateFormat)}",
                        style = MaterialTheme.typography.bodySmall, color = Mist)
                }
                IconButton(enabled = monday < currentMonday,
                    onClick = { selectedWeek = monday.plusWeeks(1).takeIf { it < currentMonday }?.toString() },
                    modifier = Modifier.semantics { contentDescription = "Next week" }) {
                    CounterGlyph("Next", if (monday < currentMonday) Gold else Mist.copy(alpha = .35f), Modifier.size(22.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("● Poop", color = Gold, style = MaterialTheme.typography.bodyMedium)
                Text("○ Pee · dashed", color = PeeBlue, style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.clearAndSetSemantics {}) {
                    Box(Modifier.fillMaxWidth().height(168.dp)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val labelHeight = textMeasurer.measure("0", valueStyle).size.height
                            val inset = labelHeight + 10.dp.toPx()
                            val plotHeight = size.height - inset * 2
                            val dayWidth = size.width / 7
                            fun point(index: Int, value: Int) = Offset(
                                dayWidth * (index + .5f), size.height - inset - plotHeight * value / maximum)
                            repeat(3) { row ->
                                val y = inset + plotHeight * row / 2
                                drawLine(Mist.copy(alpha = .18f), Offset(0f, y), Offset(size.width, y))
                            }
                            listOf(false, true).forEach { isPee ->
                                val color = if (isPee) PeeBlue else Gold
                                val dash = if (isPee) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
                                counts.forEachIndexed { index, count ->
                                    if (count != null) {
                                        val value = if (isPee) count.pee else count.poop
                                        val position = point(index, value)
                                        if (index > 0) {
                                            counts[index - 1]?.let { previous ->
                                                drawLine(color, point(index - 1, if (isPee) previous.pee else previous.poop),
                                                    position, strokeWidth = 2.dp.toPx(), pathEffect = dash)
                                            }
                                        }
                                        if (isPee) {
                                            drawCircle(color, radius = 6.dp.toPx(), center = position,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                                        } else drawCircle(color, radius = 3.dp.toPx(), center = position)
                                    }
                                }
                            }
                            // Put the higher count above and the lower count below the points.
                            // Equal counts remain readable as two separate, colored labels.
                            counts.forEachIndexed { index, count ->
                                if (count != null) {
                                    listOf(false, true).forEach { isPee ->
                                        val value = if (isPee) count.pee else count.poop
                                        val position = point(index, value)
                                        val above = if (isPee) count.pee > count.poop else count.poop >= count.pee
                                        val label = textMeasurer.measure(value.toString(), valueStyle)
                                        drawText(label, color = if (isPee) PeeBlue else Gold,
                                            topLeft = Offset(position.x - label.size.width / 2f,
                                                if (above) position.y - label.size.height - 8.dp.toPx()
                                                else position.y + 8.dp.toPx()))
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        dates.forEach { date ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall, color = if (date == today) Gold else Mist)
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = Mist)
                            }
                        }
                    }
                }
                Row(Modifier.matchParentSize()) {
                    dates.forEachIndexed { index, date ->
                        val count = counts[index]
                        Box(Modifier.weight(1f).fillMaxHeight()
                            .clickable(enabled = count != null, role = Role.Button,
                                onClickLabel = "Open day’s visits") { onSelect(date) }
                            .semantics {
                                contentDescription = if (count == null) "${date.format(accessibleDateFormat)}, future day"
                                else "${date.format(accessibleDateFormat)}, ${count.poop} poop, ${count.pee} pee" +
                                    if (date == today) ", today, ongoing" else ""
                            })
                    }
                }
            }
            Text("Week totals: $poop poop · $pee pee", style = MaterialTheme.typography.titleSmall)
            if (monday == currentMonday) Text("Today is ongoing. Future days are blank.",
                style = MaterialTheme.typography.bodySmall, color = Mist)
            Text(if (poop == 0 && pee == 0) "No visits recorded this week. Tap a day to view or add visits."
                else "Tap a day to see its visits. Both counts toward poop and pee.",
                style = MaterialTheme.typography.bodySmall, color = Mist)
        }
    }
}

package com.daiarn.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.daiarn.counter.domain.Totals
import com.daiarn.counter.ui.theme.Gold
import com.daiarn.counter.ui.theme.Mist

private val PeeBlue = Color(0xFFA5DCEF)

@Composable
fun HourlyVisitsChart(hours: List<Totals>) {
    val maximum = hours.maxOf { maxOf(it.poop, it.pee) }.coerceAtLeast(1)
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hourly visits", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Legend("POOP", "Poop", Gold)
                Legend("PEE", "Pee", PeeBlue)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.height(140.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(maximum.toString(), style = MaterialTheme.typography.labelSmall, color = Mist)
                    Text("0", style = MaterialTheme.typography.labelSmall, color = Mist)
                }
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().height(140.dp)) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            repeat(3) { HorizontalDivider(color = Mist.copy(alpha = .18f)) }
                        }
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                            hours.forEachIndexed { hour, count ->
                                Row(Modifier.weight(1f).fillMaxHeight().padding(horizontal = .5.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "%02d:00–%02d:00, %d poop, %d pee".format(
                                            hour, hour + 1, count.poop, count.pee)
                                    }, verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(.5.dp)) {
                                    Box(Modifier.weight(1f).height(140.dp * count.poop / maximum).background(Gold))
                                    Box(Modifier.weight(1f).height(140.dp * count.pee / maximum).background(PeeBlue))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("00", "06", "12", "18", "23").forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = Mist)
                        }
                    }
                }
            }
            Text(if (hours.all { it == Totals() }) "No visits recorded for this day."
                else "Hour of day · Both counts toward poop and pee.",
                style = MaterialTheme.typography.bodySmall, color = Mist)
        }
    }
}

@Composable
private fun Legend(kind: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CounterGlyph(kind, color, Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

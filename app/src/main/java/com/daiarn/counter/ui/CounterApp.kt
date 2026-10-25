package com.daiarn.counter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.daiarn.counter.CounterState
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import com.daiarn.counter.domain.*
import com.daiarn.counter.ui.theme.Navy
import com.daiarn.counter.ui.theme.Gold
import com.daiarn.counter.ui.theme.CardBlue
import com.daiarn.counter.ui.theme.Cream
import com.daiarn.counter.ui.theme.Mist
import com.daiarn.counter.ui.theme.DeepInk
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
fun CounterApp(
    state: CounterState,
    snackbar: SnackbarHostState,
    onLog: (VisitType) -> Unit,
    onRetry: () -> Unit,
    onSave: (String?, VisitType, Instant, String, (String?) -> Unit) -> Unit,
    onDelete: (Visit, (String?) -> Unit) -> Unit,
    signingIn: Boolean = false,
    onSignIn: () -> Unit = {},
    onRestore: () -> Unit = {}
) {
    var screen by rememberSaveable { mutableStateOf("Home") }
    var now by remember { mutableStateOf(Instant.now()) }
    var zone by remember { mutableStateOf(ZoneId.systemDefault()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = Instant.now()
                zone = ZoneId.systemDefault()
                delay(1_000)
            }
        }
    }
    val today = now.atZone(zone).toLocalDate()
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var followsToday by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(today) { if (followsToday) selectedText = today.toString() }
    val selected = if (followsToday) today else LocalDate.parse(selectedText)
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Navy,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                containerColor = CardBlue, contentColor = Cream) {
                listOf("Home", "Today", "History", "Records").forEach { destination ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        icon = { CounterGlyph(destination,
                            if (screen == destination) Gold else Mist, Modifier.size(24.dp)) },
                        label = { Text(destination) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold, selectedTextColor = Gold,
                            unselectedIconColor = Mist,
                            unselectedTextColor = Mist,
                            indicatorColor = Gold.copy(alpha = .16f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("counter", color = Gold, style = MaterialTheme.typography.headlineLarge)
                    Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)),
                        color = Mist, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { screen = "Account" }, modifier = Modifier.size(48.dp)
                    .background(Gold.copy(alpha = .12f), CircleShape)
                    .semantics { contentDescription = "Account and recovery" }) {
                    CounterGlyph("Account", Gold, Modifier.size(28.dp))
                }
            }
            Surface(Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.background) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.loadError -> Column(Modifier.padding(24.dp)) {
                    Text("Could not load your visits. Your saved data has not been reset.")
                    Button(onClick = onRetry) { Text("Retry") }
                }
                screen == "Home" -> HomeScreen(onLog)
                screen == "Today" -> DayScreen(
                    state, selected, today, now, zone,
                    onSelect = { selectedText = it.toString(); followsToday = it == today },
                    onAdd = { editorId = "new" },
                    onEdit = { editorId = it.id },
                    onDelete = { deleteId = it.id }
                )
                screen == "Records" -> RecordsScreen(state.visits, zone)
                screen == "Account" -> AccountScreen(state, signingIn, onSignIn, onRestore)
                else -> HistoryScreen(state, selected, today, zone, onSelect = {
                    selectedText = it.toString()
                    followsToday = it == today
                    screen = "Today"
                })
            }
            }
        }
    }

    if (editorId != null) {
        val visit = state.visits.find { it.id == editorId }
        key(editorId) {
            VisitEditor(visit, selected, zone, onDismiss = { editorId = null }, onSave = onSave)
        }
    }
    state.visits.find { it.id == deleteId }?.let { visit ->
        var saving by remember(visit.id) { mutableStateOf(false) }
        var error by remember(visit.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!saving) deleteId = null },
            title = { Text("Delete visit?") },
            text = {
                Column {
                    Text("${visit.type.label} on ${Instant.ofEpochMilli(visit.occurredAt).atZone(zone).format(dayFormat)} " +
                        "at ${Instant.ofEpochMilli(visit.occurredAt).atZone(zone).format(timeFormat)}")
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    saving = true
                    onDelete(visit) { message ->
                        saving = false
                        error = message
                        if (message == null) deleteId = null
                    }
                }) { Text(if (saving) "Deleting…" else "Delete") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleteId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomeScreen(onLog: (VisitType) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(88.dp)
            .background(Gold.copy(alpha = .10f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            CounterGlyph("BOTH", Gold, Modifier.size(56.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Log a visit", style = MaterialTheme.typography.headlineSmall)
        Text("Tap once to record it now.", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        VisitType.entries.forEach { type ->
            Button(
                onClick = { onLog(type) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == VisitType.BOTH) Gold else CardBlue,
                    contentColor = if (type == VisitType.BOTH) DeepInk else Cream),
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp)
                    .semantics { contentDescription = "Log ${type.label.lowercase()}" }
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val iconColor = if (type == VisitType.BOTH) DeepInk else Gold
                    Box(Modifier.size(42.dp).background(iconColor.copy(alpha = .10f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        CounterGlyph(type.name, iconColor, Modifier.size(28.dp))
                    }
                    Text(type.label, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    CounterGlyph("Add", iconColor, Modifier.size(20.dp))
                }
            }
        }
        Text("Saved with the current time. You can make changes in Today.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DayScreen(
    state: CounterState,
    selected: LocalDate,
    today: LocalDate,
    now: Instant,
    zone: ZoneId,
    onSelect: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Visit) -> Unit,
    onDelete: (Visit) -> Unit
) {
    val context = LocalContext.current
    val daily = remember(state.visits, zone) { totalsByDay(state.visits, zone) }
    val dayVisits = remember(state.visits, selected, zone) { visitsOn(state.visits, selected, zone) }
    val total = daily[selected] ?: Totals()
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("day-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected == today) "Today's visits" else "Daily visits", style = MaterialTheme.typography.headlineSmall)
                if (selected != today) TextButton(onClick = { onSelect(today) }) { Text("Go to today") }
            }
            if (state.pendingCount > 0) {
                Text("Saved locally · waiting to sync", style = MaterialTheme.typography.bodySmall)
                if (state.syncIssue) {
                    Text("Cloud sync is unavailable. Your visits are saved on this phone.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(selected.minusDays(1)) },
                modifier = Modifier.semantics { contentDescription = "Previous day" }) {
                CounterGlyph("Previous", Gold, Modifier.size(22.dp))
            }
            TextButton(onClick = { pickDate(context, selected, onSelect) }, modifier = Modifier.weight(1f)
                .semantics { contentDescription = "Choose date" }) {
                CounterGlyph("History", Gold, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(selected.format(dayFormat))
            }
            IconButton(enabled = selected < today, onClick = { onSelect(selected.plusDays(1)) },
                modifier = Modifier.semantics { contentDescription = "Next day" }) {
                CounterGlyph("Next", if (selected < today) Gold else Mist.copy(alpha = .35f), Modifier.size(22.dp))
            }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterGlyph("POOP", Gold, Modifier.size(28.dp))
                    Text("Poop: ${total.poop}", style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterGlyph("PEE", Gold, Modifier.size(28.dp))
                    Text("Pee: ${total.pee}", style = MaterialTheme.typography.titleLarge)
                }
            }
            }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
            CounterGlyph("Clock", Gold, Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selected == today) {
                Text("Last poop: ${elapsedLabel(lastVisit(state.visits, true), now)}")
                Text("Last pee: ${elapsedLabel(lastVisit(state.visits, false), now)}")
            } else {
                Text("Last poop that day: ${lastVisit(dayVisits, true)?.atZone(zone)?.format(timeFormat) ?: "None"}")
                Text("Last pee that day: ${lastVisit(dayVisits, false)?.atZone(zone)?.format(timeFormat) ?: "None"}")
            }
            }
            }
            }
        }
        item { HourlyVisitsChart(totalsByHour(state.visits, selected, zone)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Visits", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAdd) {
                    CounterGlyph("Add", Gold, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add visit")
                }
            }
            if (dayVisits.isEmpty()) Text("No visits recorded")
        }
        items(dayVisits, key = { it.id }) { visit ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CounterGlyph(visit.type.name, Gold, Modifier.size(26.dp))
                        Column {
                            Text(visit.type.label, style = MaterialTheme.typography.titleMedium)
                            Text(Instant.ofEpochMilli(visit.occurredAt).atZone(zone).format(timeFormat),
                                style = MaterialTheme.typography.bodySmall, color = Mist)
                        }
                    }
                    Row {
                        IconButton(onClick = { onEdit(visit) }, modifier = Modifier.semantics {
                            contentDescription = "Edit ${visit.type.label.lowercase()} visit"
                        }) { CounterGlyph("Edit", Gold, Modifier.size(20.dp)) }
                        IconButton(onClick = { onDelete(visit) }, modifier = Modifier.semantics {
                            contentDescription = "Delete ${visit.type.label.lowercase()} visit"
                        }) { CounterGlyph("Delete", Mist, Modifier.size(20.dp)) }
                    }
                }
                if (visit.note.isNotBlank()) Text(visit.note)
            }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    state: CounterState, selected: LocalDate, today: LocalDate, zone: ZoneId,
    onSelect: (LocalDate) -> Unit
) {
    val daily = remember(state.visits, zone) { totalsByDay(state.visits, zone) }
    val first = firstHistoryDay(state.visits, today, zone)
    val numberOfDays = ChronoUnit.DAYS.between(first, today).toInt() + 1
    LazyColumn(Modifier.fillMaxSize().testTag("history-list"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("History", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a day to see its visits and hourly chart.", color = Mist,
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Card {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CalendarView(selected, today, daily, onSelect)
                }
            }
        }
        item { Text("Daily totals", style = MaterialTheme.typography.titleLarge) }
        items(count = numberOfDays, key = { today.minusDays(it.toLong()).toString() }) { index ->
            val date = today.minusDays(index.toLong())
            val count = daily[date] ?: Totals()
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface).clickable {
                onSelect(date)
            }.padding(16.dp)) {
                Text(date.format(dayFormat), style = MaterialTheme.typography.titleMedium)
                Text("Poop: ${count.poop}    Pee: ${count.pee}")
            }
        }
    }
}

@Composable
private fun CalendarView(
    selected: LocalDate,
    today: LocalDate,
    daily: Map<LocalDate, Totals>,
    onSelect: (LocalDate) -> Unit
) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(selected).toString()) }
    LaunchedEffect(selected) { monthText = YearMonth.from(selected).toString() }
    val month = YearMonth.parse(monthText)
    Column(Modifier.horizontalScroll(rememberScrollState()).width(336.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthText = month.minusMonths(1).toString() },
                modifier = Modifier.semantics { contentDescription = "Previous month" }) {
                CounterGlyph("Previous", Gold, Modifier.size(22.dp))
            }
            Text(month.format(monthFormat), style = MaterialTheme.typography.titleMedium)
            IconButton(enabled = month < YearMonth.from(today),
                modifier = Modifier.semantics { contentDescription = "Next month" },
                onClick = { monthText = month.plusMonths(1).toString() }) {
                CounterGlyph("Next", if (month < YearMonth.from(today)) Gold else Mist.copy(alpha = .35f), Modifier.size(22.dp))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it) }
            }
        }
        val leading = month.atDay(1).dayOfWeek.value - 1
        val weeks = (leading + month.lengthOfMonth() + 6) / 7
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = week * 7 + column - leading + 1
                    if (day !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).height(64.dp))
                    } else {
                        val date = month.atDay(day)
                        val count = daily[date] ?: Totals()
                        val background = if (date == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        Column(
                            Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(14.dp)).background(background)
                                .clickable(enabled = date <= today) { onSelect(date) }
                                .semantics {
                                    contentDescription = "${date.format(dayFormat)}, ${count.poop} poop, ${count.pee} pee"
                                }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(day.toString(), color = if (date > today) MaterialTheme.colorScheme.outline
                                else if (date == selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface)
                            if (date <= today) Text("${count.poop}/${count.pee}",
                                color = if (date == selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Text("Daily totals: poop / pee", style = MaterialTheme.typography.bodySmall)
    }
}

private fun pickDate(context: android.content.Context, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day -> onSelect(LocalDate.of(year, month + 1, day)) },
        selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}

@Composable
private fun VisitEditor(
    visit: Visit?,
    selected: LocalDate,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onSave: (String?, VisitType, Instant, String, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val initial = remember { visit?.let { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDateTime() }
        ?: LocalDateTime.of(selected, LocalTime.now(zone).withSecond(0).withNano(0)) }
    var typeName by rememberSaveable { mutableStateOf(visit?.type?.name ?: VisitType.POOP.name) }
    var dateText by rememberSaveable { mutableStateOf(initial.toLocalDate().toString()) }
    var timeText by rememberSaveable { mutableStateOf(initial.toLocalTime().toString()) }
    var note by rememberSaveable { mutableStateOf(visit?.note.orEmpty()) }
    var offsetText by rememberSaveable { mutableStateOf(visit?.let {
        Instant.ofEpochMilli(it.occurredAt).atZone(zone).offset.id
    }) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val local = LocalDateTime.of(LocalDate.parse(dateText), LocalTime.parse(timeText))
    val offsets = zone.rules.getValidOffsets(local)
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (visit == null) "Add visit" else "Edit visit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VisitType.entries.forEach { type ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !saving) { typeName = type.name },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type.name == typeName, enabled = !saving,
                            onClick = { typeName = type.name })
                        CounterGlyph(type.name, Gold, Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(type.label)
                    }
                }
                TextButton(enabled = !saving, onClick = {
                    pickDate(context, local.toLocalDate()) { dateText = it.toString(); offsetText = null }
                }) { Text("Date: ${local.format(dayFormat)}") }
                TextButton(enabled = !saving, onClick = {
                    TimePickerDialog(context, { _, hour, minute ->
                        timeText = LocalTime.of(hour, minute).toString(); offsetText = null
                    }, local.hour, local.minute, true).show()
                }) { Text("Time: ${local.format(timeFormat)}") }
                if (offsets.size > 1) {
                    Text("This time occurs twice. Choose a UTC offset:")
                    offsets.forEach { offset ->
                        Row(Modifier.clickable(enabled = !saving) { offsetText = offset.id },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(offsetText == offset.id, enabled = !saving,
                                onClick = { offsetText = offset.id })
                            Text("UTC${offset.id}")
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, enabled = !saving,
                    label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                try {
                    val instant = resolveLocalTime(local, zone, offsetText?.let(ZoneOffset::of))
                    require(!instant.isAfter(Instant.now())) { "Future times are not allowed." }
                    saving = true
                    onSave(visit?.id, VisitType.valueOf(typeName), instant, note) { message ->
                        saving = false
                        error = message
                        if (message == null) onDismiss()
                    }
                } catch (exception: IllegalArgumentException) {
                    error = exception.message
                }
            }) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}

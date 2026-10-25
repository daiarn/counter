package com.daiarn.counter.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.daiarn.counter.CounterState
import com.daiarn.counter.data.Visit
import com.daiarn.counter.data.VisitType
import com.daiarn.counter.ui.theme.CounterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CounterUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickLoggingAndDeletionConfirmation() {
        val state = mutableStateOf(CounterState(loading = false))
        compose.setContent {
            CounterTheme {
                CounterApp(state.value, SnackbarHostState(),
                    onLog = { type -> state.value = state.value.copy(
                        visits = state.value.visits + Visit(type = type, occurredAt = System.currentTimeMillis()),
                        pendingCount = state.value.pendingCount + 1
                    ) }, onRetry = {}, onSave = { _, _, _, _, callback -> callback(null) },
                    onDelete = { visit, callback ->
                        state.value = state.value.copy(visits = state.value.visits.filterNot { it.id == visit.id })
                        callback(null)
                    })
            }
        }
        compose.onNodeWithContentDescription("Log both").performClick()
        compose.runOnIdle { assertEquals(VisitType.BOTH, state.value.visits.single().type) }
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("Poop: 1").assertIsDisplayed()
        compose.onNodeWithText("Pee: 1").assertIsDisplayed()
        compose.onNodeWithText("Saved locally · waiting to sync").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous day").performClick()
        compose.onNodeWithText("Poop: 0").assertIsDisplayed()
        compose.onNodeWithText("Pee: 0").assertIsDisplayed()
        compose.onNodeWithText("Go to today").performClick()
        compose.onNodeWithText("Poop: 1").assertIsDisplayed()
        compose.onNodeWithText("History").performClick()
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
        compose.onNodeWithContentDescription("$date, 1 poop, 1 pee").assertWidthIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("$date, 1 poop, 1 pee").performClick()
        compose.onNodeWithText("Poop: 1").assertIsDisplayed()
        compose.onNodeWithTag("day-list").performScrollToNode(hasText("Hourly visits"))
        compose.onNodeWithText("Hourly visits").assertIsDisplayed()
        compose.onNodeWithTag("day-list").performScrollToNode(hasContentDescription("Delete both visit"))
        compose.onNodeWithContentDescription("Delete both visit").performClick()
        compose.onNodeWithText("Delete visit?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(1, state.value.visits.size) }
        compose.onNodeWithContentDescription("Delete both visit").performClick()
        compose.onAllNodesWithText("Delete", useUnmergedTree = true).onLast().performClick()
        compose.runOnIdle { assertEquals(0, state.value.visits.size) }
    }
}

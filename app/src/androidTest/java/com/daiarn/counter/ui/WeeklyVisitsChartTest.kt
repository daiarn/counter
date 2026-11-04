package com.daiarn.counter.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.daiarn.counter.domain.Totals
import com.daiarn.counter.ui.theme.CounterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class WeeklyVisitsChartTest {
    @get:Rule val compose = createComposeRule()

    @Test fun weekCrossesYearBoundaryAndFutureDaysAreDisabled() {
        val today = LocalDate.parse("2027-01-01")
        var selected: LocalDate? = null
        val daily = mutableStateOf(mapOf(
            LocalDate.parse("2026-12-28") to Totals(2, 3),
            today to Totals(1, 1),
            today.plusDays(1) to Totals(99, 99)
        ))
        compose.setContent { CounterTheme { WeeklyVisitsChart(daily.value, today) { selected = it } } }
        compose.onNodeWithText("Week totals: 3 poop · 4 pee").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next week").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Saturday, 2 Jan 2027, future day").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Friday, 1 Jan 2027, 1 poop, 1 pee, today, ongoing").performClick()
        compose.runOnIdle { assertEquals(today, selected) }
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.onNodeWithText("Week totals: 0 poop · 0 pee").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sunday, 27 Dec 2026, 0 poop, 0 pee").assertIsEnabled()
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.onNodeWithText("This week").assertIsDisplayed()
        compose.runOnIdle { daily.value = emptyMap() }
        compose.onNodeWithText("Week totals: 0 poop · 0 pee").assertIsDisplayed()
    }

    @Test fun currentWeekFollowsMondayRollover() {
        val today = mutableStateOf(LocalDate.parse("2026-11-01"))
        compose.setContent { CounterTheme { WeeklyVisitsChart(emptyMap(), today.value) {} } }
        compose.onNodeWithContentDescription("Sunday, 1 Nov 2026, 0 poop, 0 pee, today, ongoing").assertIsEnabled()
        compose.runOnIdle { today.value = today.value.plusDays(1) }
        compose.onNodeWithContentDescription("Monday, 2 Nov 2026, 0 poop, 0 pee, today, ongoing").assertIsEnabled()
        compose.onNodeWithContentDescription("Tuesday, 3 Nov 2026, future day").assertIsNotEnabled()
    }
}

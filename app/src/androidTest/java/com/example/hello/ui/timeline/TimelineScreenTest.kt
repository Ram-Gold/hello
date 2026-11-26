package com.example.hello.ui.timeline

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.hello.ui.theme.HelloTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun calendarHeader_displaysSelectedDate_stepsDays_andShowsTodayReturnPill() {
        val viewModel = TimelineViewModel()
        val today = viewModel.uiState.value.today

        composeTestRule.setContent {
            HelloTheme(darkTheme = false) {
                val uiState by viewModel.uiState.collectAsState()
                TimelineScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onContactClick = {},
                    onLogEncounterForContact = { _, _ -> },
                    onLogEncounterClick = {}
                )
            }
        }

        val initialTitle = TimelineViewModel.formatCalendarHeaderTitle(today, today)
        composeTestRule.onNodeWithText(initialTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertDoesNotExist()

        val otherDayInWeek = if (today.dayOfWeek == java.time.DayOfWeek.SATURDAY) {
            today.minusDays(1)
        } else {
            today.plusDays(1)
        }
        composeTestRule.onNodeWithText(otherDayInWeek.dayOfMonth.toString()).performClick()
        composeTestRule.waitForIdle()
        assertEquals(otherDayInWeek, viewModel.uiState.value.selectedDate)
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()

        composeTestRule.onNodeWithText("Today").performClick()
        composeTestRule.waitForIdle()
        assertEquals(today, viewModel.uiState.value.selectedDate)
    }
}

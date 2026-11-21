package com.example.hello.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.hello.ui.theme.HelloTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HelloDatePickerDialogUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modeToggle_switchesBetweenCalendarAndTextInput_andConfirms() {
        var confirmedDate: LocalDate? = null
        var dismissed = false

        composeTestRule.setContent {
            HelloTheme {
                HelloDatePickerDialog(
                    initialDate = LocalDate.of(1995, 6, 15),
                    onConfirm = { confirmedDate = it },
                    onDismissRequest = { dismissed = true },
                    restrictToPastDates = true
                )
            }
        }

        // Confirm and Cancel buttons are visible in Calendar mode
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Switch to Text Input mode via Material 3 DatePicker's mode toggle
        composeTestRule.onNodeWithContentDescription("Switch to text input mode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()

        // Switch back to Calendar mode and verify Confirm/Cancel remain visible and unclipped
        composeTestRule.onNodeWithContentDescription("Switch to calendar input mode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()

        composeTestRule.onNodeWithText("Confirm").performClick()
        composeTestRule.waitForIdle()

        assertEquals(LocalDate.of(1995, 6, 15), confirmedDate)
        assertTrue(dismissed)
    }
}

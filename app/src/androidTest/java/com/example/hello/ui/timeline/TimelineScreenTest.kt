package com.example.hello.ui.timeline

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import com.example.hello.ui.theme.HelloTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun filterPills_updateSelectionStateOnTap_andMatchDesignSystemGeometryAndSpacing() {
        val viewModel = TimelineViewModel()

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

        val allPill = composeTestRule.onNodeWithText("All")
        val futurePill = composeTestRule.onNodeWithText("Future")
        val pastPill = composeTestRule.onNodeWithText("Past")

        allPill.assertIsDisplayed().assertIsSelected()
        futurePill.assertIsDisplayed().assertIsNotSelected()
        pastPill.assertIsDisplayed().assertIsNotSelected()

        val allBounds = allPill.getUnclippedBoundsInRoot()
        val futureBounds = futurePill.getUnclippedBoundsInRoot()
        val pastBounds = pastPill.getUnclippedBoundsInRoot()

        assertEquals(28f, allBounds.height.value, 0.5f)
        assertEquals(28f, futureBounds.height.value, 0.5f)
        assertEquals(28f, pastBounds.height.value, 0.5f)

        val gapAllToFuture = (futureBounds.left - allBounds.right).value
        val gapFutureToPast = (pastBounds.left - futureBounds.right).value
        assertEquals(8f, gapAllToFuture, 0.5f)
        assertEquals(8f, gapFutureToPast, 0.5f)

        futurePill.performClick()
        composeTestRule.waitForIdle()
        futurePill.assertIsSelected()
        allPill.assertIsNotSelected()
        pastPill.assertIsNotSelected()

        pastPill.performClick()
        composeTestRule.waitForIdle()
        pastPill.assertIsSelected()
        allPill.assertIsNotSelected()
        futurePill.assertIsNotSelected()
    }
}

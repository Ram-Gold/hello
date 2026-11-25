package com.example.hello.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.height
import com.example.hello.ui.theme.HelloTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HelloToastPillUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun actionableToast_rendersCompactCapsuleWithCountdownBar() {
        composeTestRule.setContent {
            HelloTheme(darkTheme = false) {
                HelloToastPill(
                    message = "Contact archived",
                    actionLabel = "Undo",
                    progressOverride = 0.6f
                )
            }
        }

        val pillNode = composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG)
        pillNode.assertIsDisplayed()
        composeTestRule.onNodeWithTag(TOAST_COUNTDOWN_BAR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()

        val bounds = pillNode.getUnclippedBoundsInRoot()
        assertTrue("Expected medium-tall toast height in 44..56.dp, got ${bounds.height}", bounds.height.value in 44f..56f)
    }

    @Test
    fun informationalToast_rendersCompactCapsuleWithoutCountdownBar() {
        composeTestRule.setContent {
            HelloTheme(darkTheme = false) {
                HelloToastPill(
                    message = "Preferences saved"
                )
            }
        }

        val pillNode = composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG)
        pillNode.assertIsDisplayed()
        composeTestRule.onNodeWithTag(TOAST_COUNTDOWN_BAR_TEST_TAG).assertDoesNotExist()

        val bounds = pillNode.getUnclippedBoundsInRoot()
        assertTrue("Expected medium-tall toast height in 40..52.dp, got ${bounds.height}", bounds.height.value in 40f..52f)
    }

    private fun showToastAndWaitForPill(
        message: String,
        actionLabel: String? = null
    ): SnackbarHostState {
        val hostState = SnackbarHostState()
        lateinit var scope: CoroutineScope

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            HelloTheme(darkTheme = false) {
                HelloToastHost(hostState = hostState)
            }
        }

        scope.launch {
            hostState.showSnackbar(
                HelloToastVisuals(
                    message = message,
                    actionLabel = actionLabel
                )
            )
        }
        composeTestRule.waitUntil(3_000) {
            hostState.currentSnackbarData != null &&
                composeTestRule.onAllNodesWithTag(TOAST_PILL_TEST_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).assertIsDisplayed()
        return hostState
    }

    @Test
    fun toastHost_swipeLeft_dismissesToast() {
        val hostState = showToastAndWaitForPill(message = "Contact archived", actionLabel = "Undo")

        composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitUntil(3_000) { hostState.currentSnackbarData == null }
        assertNull(hostState.currentSnackbarData)
    }

    @Test
    fun toastHost_swipeRight_dismissesToast() {
        val hostState = showToastAndWaitForPill(message = "Preferences saved")

        composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).performTouchInput {
            swipeRight()
        }
        composeTestRule.waitUntil(3_000) { hostState.currentSnackbarData == null }
        assertNull(hostState.currentSnackbarData)
    }

    @Test
    fun toastHost_shortDrag_snapsBackWithoutDismissing() {
        val hostState = showToastAndWaitForPill(message = "Contact archived", actionLabel = "Undo")
        val initialLeftDp = composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).getUnclippedBoundsInRoot().left.value

        composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).performTouchInput {
            down(center)
            moveBy(Offset(24f, 0f), delayMillis = 80L)
            up()
        }
        composeTestRule.waitForIdle()
        assertNotNull("Short drag below threshold should snap back without dismissing", hostState.currentSnackbarData)
        composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).assertIsDisplayed()
        val finalLeftDp = composeTestRule.onNodeWithTag(TOAST_PILL_TEST_TAG).getUnclippedBoundsInRoot().left.value
        assertTrue(
            "Expected pill to snap back near resting X ($initialLeftDp dp), got $finalLeftDp dp",
            abs(finalLeftDp - initialLeftDp) <= 2f
        )
    }
}

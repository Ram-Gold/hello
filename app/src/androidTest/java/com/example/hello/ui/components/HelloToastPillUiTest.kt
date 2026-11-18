package com.example.hello.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.height
import com.example.hello.ui.theme.HelloTheme
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
        assertTrue("Expected compact toast height <= 48.dp, got ${bounds.height}", bounds.height.value <= 48f)
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
        assertTrue("Expected compact toast height <= 44.dp, got ${bounds.height}", bounds.height.value <= 44f)
    }
}

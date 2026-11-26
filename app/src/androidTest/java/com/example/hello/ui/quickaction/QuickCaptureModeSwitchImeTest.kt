package com.example.hello.ui.quickaction

import android.view.View
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.hello.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickCaptureModeSwitchImeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private data class ImeSample(
        val elapsedMs: Long,
        val visible: Boolean,
        val bottomPx: Int,
        val focused: Boolean
    )

    private fun readImeState(): Pair<Boolean, Int> {
        var visible = false
        var bottom = 0
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val rootView: View = activity.window.decorView
            val insets = ViewCompat.getRootWindowInsets(rootView)
            visible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
            bottom = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        }
        return visible to bottom
    }

    private fun hasFocusedEditableText(): Boolean {
        val focusedEditable = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
            .and(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        return composeTestRule.onAllNodes(focusedEditable, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun assertImeStaysUpAcrossSwap(
        label: String,
        toggleContentDescription: String,
        baselineBottomPx: Int
    ) {
        val samples = mutableListOf<ImeSample>()
        val startMs = System.currentTimeMillis()
        composeTestRule.onNodeWithContentDescription(toggleContentDescription).performClick()

        // Sample continuously for 900ms to catch both permanent dismissal and any IME drop/bounce or focus loss
        while (System.currentTimeMillis() - startMs < 900L) {
            val (vis, bot) = readImeState()
            val foc = hasFocusedEditableText()
            samples += ImeSample(
                elapsedMs = System.currentTimeMillis() - startMs,
                visible = vis,
                bottomPx = bot,
                focused = foc
            )
            Thread.sleep(20)
        }
        composeTestRule.waitForIdle()

        val minBottom = samples.minOf { it.bottomPx }
        val everHidden = samples.any { !it.visible }
        val everUnfocused = samples.any { !it.focused }
        val finalSample = samples.last()
        val trace = samples.joinToString(" | ") { "${it.elapsedMs}ms:v=${it.visible},b=${it.bottomPx},f=${it.focused}" }

        assertTrue(
            "$label: IME dropped/closed or focus lost! baseline=${baselineBottomPx}px, minBottom=${minBottom}px, everHidden=$everHidden, everUnfocused=$everUnfocused, final=$finalSample\nTrace: $trace",
            !everHidden && !everUnfocused && minBottom >= (baselineBottomPx * 0.9f).toInt() && finalSample.visible && finalSample.focused
        )
    }

    @Test
    fun inAppQuickAdd_modeSwitch_neverDropsKeyboardOrFocus() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Quick capture")),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Quick capture").performClick()
        composeTestRule.waitForIdle()

        // Open Met Again first (terminology-independent label), then test both directions
        composeTestRule.onNodeWithText("Met Again").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val (vis, bot) = readImeState()
            vis && bot > 200 && hasFocusedEditableText()
        }
        Thread.sleep(300)
        val (_, baselineBottom) = readImeState()
        assertTrue("Expected baseline IME bottom > 200px, got $baselineBottom", baselineBottom > 200)

        // Swap 1: Met Again -> Add Contact
        assertImeStaysUpAcrossSwap(
            label = "Met Again -> Add Contact",
            toggleContentDescription = "Switch to Add Contact",
            baselineBottomPx = baselineBottom
        )

        // Type in Add Contact primary input, then swap Add Contact -> Met Again
        val focusedEditable = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
            .and(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        composeTestRule.onNode(focusedEditable, useUnmergedTree = true).performTextInput("Alex")
        composeTestRule.waitForIdle()

        // Swap 2: Add Contact (with text) -> Met Again
        assertImeStaysUpAcrossSwap(
            label = "Add Contact -> Met Again",
            toggleContentDescription = "Switch to Met Again",
            baselineBottomPx = baselineBottom
        )
    }
}

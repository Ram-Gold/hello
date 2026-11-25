package com.example.hello.ui.quickaction

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImePlacementTest {

    @Test
    fun resolveInitialBottomPaddingPx_usesLearnedHeightWhenValid_elseFallsBackTo40Percent() {
        // First launch (saved == 0) -> 40% of 2400px = 960px
        assertEquals(
            960,
            WidgetImePlacement.resolveInitialBottomPaddingPx(
                savedImeHeightPx = 0,
                screenHeightPx = 2400,
                navBarBottomPx = 120
            )
        )

        // Learned height valid (> navBarBottomPx) -> returns exact learned height
        assertEquals(
            912,
            WidgetImePlacement.resolveInitialBottomPaddingPx(
                savedImeHeightPx = 912,
                screenHeightPx = 2400,
                navBarBottomPx = 120
            )
        )

        // Implausibly small saved height (<= navBarBottomPx) -> falls back to 40%
        assertEquals(
            960,
            WidgetImePlacement.resolveInitialBottomPaddingPx(
                savedImeHeightPx = 80,
                screenHeightPx = 2400,
                navBarBottomPx = 120
            )
        )

        // Implausibly large saved height (> 65% screen height) -> clamped cleanly
        val clamped = WidgetImePlacement.resolveInitialBottomPaddingPx(
            savedImeHeightPx = 2000,
            screenHeightPx = 2400,
            navBarBottomPx = 120
        )
        assertTrue("Saved height must be clamped below 65% of screen height", clamped <= (2400 * 0.65f).toInt())
    }

    @Test
    fun resolveTargetBottomPaddingPx_locksImmediatelyToImeAnimationTargetSoCardIsNeverBehindKeyboard() {
        // Keyboard taller than initial guess -> immediately snaps to exact target height
        assertEquals(
            1040,
            WidgetImePlacement.resolveTargetBottomPaddingPx(
                currentBottomPx = 960,
                imeAnimationTargetBottomPx = 1040,
                navBarBottomPx = 120
            )
        )
        // Before IME target is available (<= navBarBottomPx) -> keeps pre-positioned height
        assertEquals(
            960,
            WidgetImePlacement.resolveTargetBottomPaddingPx(
                currentBottomPx = 960,
                imeAnimationTargetBottomPx = 0,
                navBarBottomPx = 120
            )
        )
    }

    @Test
    fun shouldApplySettledCorrection_onlyTriggersWhenDeltaExceedsThreshold() {
        val thresholdPx = 12 // ~4dp at 3x density

        assertFalse(
            "Delta within 4dp threshold should not trigger relayout",
            WidgetImePlacement.shouldApplySettledCorrection(
                currentBottomPx = 840,
                settledImeBottomPx = 848,
                thresholdPx = thresholdPx
            )
        )
        assertFalse(
            "Zero settled height must not trigger correction",
            WidgetImePlacement.shouldApplySettledCorrection(
                currentBottomPx = 840,
                settledImeBottomPx = 0,
                thresholdPx = thresholdPx
            )
        )
        assertTrue(
            "Delta exceeding 4dp threshold must trigger single correction",
            WidgetImePlacement.shouldApplySettledCorrection(
                currentBottomPx = 840,
                settledImeBottomPx = 900,
                thresholdPx = thresholdPx
            )
        )
    }

    @Test
    fun shouldDismissOnImeChange_dismissesOnlyAfterImeWasObservedVisibleAndThenCloses() {
        // Before keyboard finishes rising, IME is not visible yet -> never dismiss
        assertFalse(WidgetImePlacement.shouldDismissOnImeChange(hasSeenImeVisible = false, isImeVisibleNow = false))
        assertFalse(WidgetImePlacement.shouldDismissOnImeChange(hasSeenImeVisible = false, isImeVisibleNow = true))
        // Still visible -> keep open
        assertFalse(WidgetImePlacement.shouldDismissOnImeChange(hasSeenImeVisible = true, isImeVisibleNow = true))
        // Was visible and now closed -> always cancel & dismiss
        assertTrue(WidgetImePlacement.shouldDismissOnImeChange(hasSeenImeVisible = true, isImeVisibleNow = false))
    }

    @Test
    fun preferencesRepository_persistsPortraitAndLandscapeWidgetImeHeightsIndependently() = runTest {
        val repo = FakeUserPreferencesRepository()

        assertEquals(0, repo.userPreferencesFlow.first().widgetImeHeightPortraitPx)
        assertEquals(0, repo.userPreferencesFlow.first().widgetImeHeightLandscapePx)

        repo.setWidgetImeHeightPx(isLandscape = false, heightPx = 880)
        repo.setWidgetImeHeightPx(isLandscape = true, heightPx = 540)

        val prefs = repo.userPreferencesFlow.first()
        assertEquals(880, prefs.widgetImeHeightPortraitPx)
        assertEquals(540, prefs.widgetImeHeightLandscapePx)
    }
}

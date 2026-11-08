package com.example.hello.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seam 1 & 2: Pull-to-action resistance math, progress normalization, and threshold triggering.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PullToActionStateTest {

    @Test
    fun pullToAction_initialState_hasZeroOffsetAndProgress() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)

        assertEquals(0f, state.pullOffsetPx, 0.001f)
        assertEquals(0f, state.progress, 0.001f)
        assertFalse(state.isTriggered)
    }

    @Test
    fun calculateResistanceDelta_appliesDiminishingReturnsAsPullIncreases() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)

        // At 0 offset, factor is maximum (0.5)
        val initialDelta = state.calculateResistanceDelta(rawDelta = 50f)
        assertEquals(25f, initialDelta, 0.01f)

        // Simulate pull to 200px (half of maxPullPx)
        state.applyDelta(200f)
        val midDelta = state.calculateResistanceDelta(rawDelta = 50f)

        // Resistance should be greater, so delta should be smaller than initialDelta
        assertTrue("Delta with resistance must be smaller as offset increases", midDelta < initialDelta)
        assertEquals(12.5f, midDelta, 0.01f)
    }

    @Test
    fun progressAndTrigger_correctlyReflectsThresholdBoundary() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)

        state.applyDelta(100f)
        assertEquals(0.5f, state.progress, 0.01f)
        assertFalse(state.isTriggered)

        state.applyDelta(100f)
        assertEquals(1.0f, state.progress, 0.01f)
        assertTrue(state.isTriggered)

        // Further pull clamps progress to 1.0f, but remains triggered
        state.applyDelta(50f)
        assertEquals(1.0f, state.progress, 0.01f)
        assertTrue(state.isTriggered)
    }

    @Test
    fun reset_restoresZeroOffsetAndUntriggeredState() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)
        state.applyDelta(250f)
        assertTrue(state.isTriggered)

        state.reset()
        assertEquals(0f, state.pullOffsetPx, 0.001f)
        assertEquals(0f, state.progress, 0.001f)
        assertFalse(state.isTriggered)
    }

    @Test
    fun nestedScrollConnection_onPostScroll_increasesOffsetWithResistance() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)
        val scope = TestScope()
        var triggered = false
        val connection = PullToActionNestedScrollConnection(
            state = state,
            coroutineScope = scope,
            enabled = true,
            onTrigger = { triggered = true }
        )

        val consumed = connection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 100f),
            source = NestedScrollSource.UserInput
        )

        // rawDelta 100 with zero initial offset: 100 * (1 - 0) * 0.5 = 50
        assertEquals(50f, state.pullOffsetPx, 0.01f)
        assertEquals(100f, consumed.y, 0.01f)
        assertFalse(triggered)
    }

    @Test
    fun nestedScrollConnection_onPreScroll_collapsesOffsetWhenScrollingUp() {
        val state = PullToActionState(thresholdPx = 200f, maxPullPx = 400f)
        val scope = TestScope()
        val connection = PullToActionNestedScrollConnection(
            state = state,
            coroutineScope = scope,
            enabled = true,
            onTrigger = {}
        )

        state.applyDelta(100f)
        assertEquals(100f, state.pullOffsetPx, 0.01f)

        // User drags up by 40px
        val consumed = connection.onPreScroll(
            available = Offset(0f, -40f),
            source = NestedScrollSource.UserInput
        )

        assertEquals(-40f, consumed.y, 0.01f)
        assertEquals(60f, state.pullOffsetPx, 0.01f)
    }

    @Test
    fun nestedScrollConnection_onPreFling_triggersCallbackWhenThresholdMet() = runTest {
        val state = PullToActionState(thresholdPx = 100f, maxPullPx = 300f)
        var triggered = false
        val connection = PullToActionNestedScrollConnection(
            state = state,
            coroutineScope = this,
            enabled = true,
            onTrigger = { triggered = true }
        )

        // Below threshold
        state.applyDelta(80f)
        connection.onPreFling(Velocity.Zero)
        assertFalse(triggered)

        // Above threshold
        state.applyDelta(120f)
        assertTrue(state.isTriggered)
        connection.onPreFling(Velocity.Zero)
        assertTrue(triggered)
        assertEquals(0f, state.pullOffsetPx, 0.001f)
    }
}

package com.example.hello.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectableAvatarBadgeTest {

    @Test
    fun motionTokens_matchAvatarSelectionSpecs() {
        assertEquals(180, MotionTokens.AVATAR_SELECTION_ENTER_DURATION_MS)
        assertEquals(140, MotionTokens.AVATAR_SELECTION_EXIT_DURATION_MS)
        assertEquals(0.45f, MotionTokens.AVATAR_SELECTION_INITIAL_SCALE, 0.001f)
        assertEquals(0.45f, MotionTokens.AVATAR_SELECTION_EXIT_SCALE, 0.001f)
        assertSame(FastOutSlowInEasing, MotionTokens.AvatarSelectionEasing)
    }

    @Test
    fun selectableAvatarBadgeDefaults_exposeConsistentTokens() {
        assertEquals(MotionTokens.AVATAR_SELECTION_ENTER_DURATION_MS, SelectableAvatarBadgeDefaults.ENTER_DURATION_MS)
        assertEquals(MotionTokens.AVATAR_SELECTION_EXIT_DURATION_MS, SelectableAvatarBadgeDefaults.EXIT_DURATION_MS)
        assertEquals(MotionTokens.AVATAR_SELECTION_INITIAL_SCALE, SelectableAvatarBadgeDefaults.INITIAL_SCALE, 0.001f)
        assertEquals(MotionTokens.AVATAR_SELECTION_EXIT_SCALE, SelectableAvatarBadgeDefaults.TARGET_EXIT_SCALE, 0.001f)
        assertSame(FastOutSlowInEasing, SelectableAvatarBadgeDefaults.EasingCurve)
    }

    @Test
    fun transitions_areProperlyConfiguredAndNonEmpty() {
        assertNotEquals(EnterTransition.None, SelectableAvatarBadgeDefaults.enterTransition)
        assertNotEquals(ExitTransition.None, SelectableAvatarBadgeDefaults.exitTransition)
    }

    @Test
    fun easeOutCurve_demonstratesEaseOutFrontLoadedDeceleration() {
        val easing = SelectableAvatarBadgeDefaults.EasingCurve
        // At start progress is 0.0, at end progress is 1.0
        assertEquals(0.0f, easing.transform(0.0f), 0.001f)
        assertEquals(1.0f, easing.transform(1.0f), 0.001f)

        // Ease-out (FastOutSlowIn / decelerating) reaches > 0.5 progress by half-time
        val midpoint = easing.transform(0.5f)
        assertTrue("Ease-out curve must be front-loaded: midpoint $midpoint > 0.5", midpoint > 0.5f)
    }

    @Test
    fun animationDurations_stayWithinPerceptionBudget() {
        assertTrue(
            "Enter duration (${SelectableAvatarBadgeDefaults.ENTER_DURATION_MS}ms) must be <= 200ms for instant touch feedback",
            SelectableAvatarBadgeDefaults.ENTER_DURATION_MS <= 200
        )
        assertTrue(
            "Exit duration (${SelectableAvatarBadgeDefaults.EXIT_DURATION_MS}ms) must be faster than enter for crisp dismissal",
            SelectableAvatarBadgeDefaults.EXIT_DURATION_MS < SelectableAvatarBadgeDefaults.ENTER_DURATION_MS
        )
        assertTrue(
            "Initial scale (${SelectableAvatarBadgeDefaults.INITIAL_SCALE}) must be > 0f (physical world non-zero scale)",
            SelectableAvatarBadgeDefaults.INITIAL_SCALE > 0.0f
        )
    }
}

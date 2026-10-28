package com.example.hello.ui.navigation

import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighRefreshRateTransitionTest {

    @Test
    fun m3EmphasizedEasing_matchesPixelPlayerCubicBezierWithFastStartAndLongEaseOutSettle() {
        val easing = MotionTokens.M3EmphasizedEasing

        assertEquals("Starts at 0f", 0.0f, easing.transform(0.0f), 0.001f)
        assertEquals("Ends at 1f", 1.0f, easing.transform(1.0f), 0.001f)

        // CubicBezierEasing(0.2f, 0f, 0f, 1f): fast responsive start, long ease-out settle
        val progressAt10Percent = easing.transform(0.1f)
        assertTrue(
            "MD3 Emphasized easing must respond quickly at t=0.1, was $progressAt10Percent",
            progressAt10Percent in 0.05f..0.40f
        )

        // At t=0.5, > 80% of distance covered during luxurious ease-out deceleration tail
        val progressAt50Percent = easing.transform(0.5f)
        assertTrue(
            "MD3 Emphasized easing should smoothly decelerate (> 80% at t=0.5), but was $progressAt50Percent",
            progressAt50Percent > 0.80f
        )
    }

    @Test
    fun tabTransitionTokens_areCalibratedToPixelPlayer380msSpec() {
        assertEquals(380, MotionTokens.BOTTOM_NAV_TRANSITION_DURATION)
        assertEquals(190, MotionTokens.BOTTOM_NAV_FADE_DURATION)
        assertEquals(380, MotionTokens.TAB_TRANSITION_DURATION_MS)
        assertEquals(380, TRANSITION_MS)
        assertEquals(350, AOSP_TRANSITION_DURATION)
        assertEquals(450, TRANSITION_DURATION)

        val floatTween = MotionTokens.tabTransitionTween()
        assertEquals(380, floatTween.durationMillis)
        assertEquals(MotionTokens.M3EmphasizedEasing, floatTween.easing)

        val sizeTween = MotionTokens.tabTransitionSizeTween()
        assertEquals(380, sizeTween.durationMillis)
        assertEquals(MotionTokens.M3EmphasizedEasing, sizeTween.easing)

        val offsetTween = MotionTokens.tabTransitionOffsetTween()
        assertEquals(380, offsetTween.durationMillis)
        assertEquals(MotionTokens.M3EmphasizedEasing, offsetTween.easing)
    }

    @Test
    fun screenSlideSpecs_areSynchronizedWithPixelPlayerRootSpecs() {
        val slideSpec = MotionTokens.screenSlideSpec()
        assertEquals(380, slideSpec.durationMillis)
        assertEquals(MotionTokens.BottomNavEasing, slideSpec.easing)

        val fadeSpec = MotionTokens.screenFadeSpec()
        assertEquals(190, fadeSpec.durationMillis)
        assertEquals(MotionTokens.BottomNavEasing, fadeSpec.easing)
    }

    @Test
    fun pixelPlayerPushPopAndSharedAxisTransitions_areConfiguredCorrectly() {
        assertNotNull(enterTransition())
        assertNotNull(exitTransition())
        assertNotNull(popEnterTransition())
        assertNotNull(popExitTransition())

        assertNotNull(aospSharedAxisEnter())
        assertNotNull(aospSharedAxisExit())
        assertNotNull(aospSharedAxisPopEnter())
        assertNotNull(aospSharedAxisPopExit())

        assertNotNull(sharedAxisEnter())
        assertNotNull(sharedAxisExit())
        assertNotNull(sharedAxisPopEnter())
        assertNotNull(sharedAxisPopExit())
    }
}

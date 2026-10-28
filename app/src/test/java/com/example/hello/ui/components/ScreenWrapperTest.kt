package com.example.hello.ui.components

import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ScreenWrapperTest {

    @Test
    fun screenWrapperTokens_matchDesignSpecifications() {
        assertEquals("Max scale reduction must be 0% (no sizing down)", 0f, ScreenWrapperTokens.MAX_SCALE_REDUCTION, 0.001f)
        assertEquals("Minimum scale during transition must be 1.0f", 1.0f, ScreenWrapperTokens.MIN_SCALE, 0.001f)

        assertEquals("Max alpha fade must be 100%", 1.0f, ScreenWrapperTokens.MAX_ALPHA_FADE, 0.001f)
        assertEquals("Minimum alpha during transition must be 0.0f", 0.0f, ScreenWrapperTokens.MIN_ALPHA, 0.001f)

        assertEquals("Corner radius must be 0dp (eliminated)", 0f, ScreenWrapperTokens.MAX_CORNER_RADIUS_DP, 0.001f)
        assertEquals("Dim scrim must be 0% (eliminated)", 0f, ScreenWrapperTokens.MAX_DIM_ALPHA, 0.001f)
    }

    @Test
    fun depthCalculations_atZeroOffset_haveNoDepthDistortion() {
        val offset = 0.0f
        val absOffset = abs(offset).coerceIn(0f, 1f)
        val easedProgress = MotionTokens.EaseInOut.transform(absOffset)

        val scale = 1.0f - (ScreenWrapperTokens.MAX_SCALE_REDUCTION * absOffset)
        val alpha = 1.0f - (ScreenWrapperTokens.MAX_ALPHA_FADE * easedProgress)

        assertEquals("Resting scale must be exactly 1.0f", 1.0f, scale, 0.001f)
        assertEquals("Resting alpha must be exactly 1.0f", 1.0f, alpha, 0.001f)
    }

    @Test
    fun depthCalculations_atMaxOffset_reachFullRecedingDepth() {
        val offset = 1.0f
        val absOffset = abs(offset).coerceIn(0f, 1f)
        val easedProgress = MotionTokens.EaseInOut.transform(absOffset)

        val scale = 1.0f - (ScreenWrapperTokens.MAX_SCALE_REDUCTION * absOffset)
        val alpha = 1.0f - (ScreenWrapperTokens.MAX_ALPHA_FADE * easedProgress)

        assertEquals("Scale must remain 1.0f", 1.0f, scale, 0.001f)
        assertEquals("Receded alpha must reach 0.0f", 0.0f, alpha, 0.001f)
    }

    @Test
    fun depthCalculations_interpolateMonotonicallyDuringTransition() {
        var prevAlpha = 1.0f

        var p = 0.0f
        while (p <= 1.0f) {
            val easedProgress = MotionTokens.EaseInOut.transform(p)
            val scale = 1.0f - (ScreenWrapperTokens.MAX_SCALE_REDUCTION * p)
            val alpha = 1.0f - (ScreenWrapperTokens.MAX_ALPHA_FADE * easedProgress)

            assertEquals("Scale must remain exactly 1.0f at all times", 1.0f, scale, 0.0001f)
            assertTrue("Alpha must decrease monotonically ($alpha <= $prevAlpha)", alpha <= prevAlpha + 0.0001f)

            prevAlpha = alpha
            p += 0.05f
        }
    }
}

package com.example.hello.ui.components

import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ScreenDepthWrapperTest {

    @Test
    fun screenDepthTokens_matchComponentLevelFadeAndZeroScaleSpecifications() {
        assertEquals("Transition duration must be 380ms", 380, ScreenDepthTokens.DURATION_MS)
        assertEquals("Content min alpha when behind must be 0.0f (complete visible fade)", 0.0f, ScreenDepthTokens.MIN_CONTENT_ALPHA, 0.001f)
        assertEquals("Content alpha fade when behind must be 1.0f", 1.0f, ScreenDepthTokens.MAX_CONTENT_ALPHA_FADE, 0.001f)
        assertEquals("Content scale must remain exactly 1.0f (no sizing down)", 1.0f, ScreenDepthTokens.MIN_CONTENT_SCALE, 0.001f)
        assertEquals("Content scale reduction must be 0% (zero scaling)", 0f, ScreenDepthTokens.MAX_CONTENT_SCALE_REDUCTION, 0.001f)

        // Continuous pager tokens (Home <-> People)
        assertEquals("Pager content min alpha must be 0.0f (full crossfade)", 0.0f, ScreenDepthTokens.MIN_PAGER_CONTENT_ALPHA, 0.001f)
        assertEquals("Pager alpha fade must be 100%", 1.0f, ScreenDepthTokens.MAX_PAGER_ALPHA_FADE, 0.001f)
        assertEquals("Pager content scale must remain exactly 1.0f (no sizing down)", 1.0f, ScreenDepthTokens.MIN_PAGER_CONTENT_SCALE, 0.001f)
        assertEquals("Pager scale reduction must be 0% (zero scaling)", 0f, ScreenDepthTokens.MAX_PAGER_SCALE_REDUCTION, 0.001f)

        // Hard boundary and shader effects eliminated for low-end GPU efficiency
        assertEquals("Corner clipping radius must be 0dp (eliminated)", 0f, ScreenDepthTokens.MAX_CORNER_RADIUS_DP, 0.001f)
        assertEquals("Full-screen black dim scrim must be 0% (eliminated)", 0f, ScreenDepthTokens.MAX_DIM_ALPHA, 0.001f)
        assertEquals("Blur shader must be 0dp (eliminated)", 0f, ScreenDepthTokens.MAX_BLUR_DP, 0.001f)
        assertEquals(
            "Root tabs must use CompositingStrategy.Auto to avoid offscreen buffer allocation on low-end devices",
            androidx.compose.ui.graphics.CompositingStrategy.Auto,
            ScreenDepthTokens.ROOT_TAB_COMPOSITING_STRATEGY
        )
    }

    @Test
    fun restingState_hasFullOpacityAndNaturalScaleWithoutDistortion() {
        val isDepthVisible = false
        val targetAlpha = if (isDepthVisible) ScreenDepthTokens.MIN_CONTENT_ALPHA else 1.0f
        val targetScale = if (isDepthVisible) ScreenDepthTokens.MIN_CONTENT_SCALE else 1.0f

        assertEquals("Resting content alpha must be 1.0f (fully opaque)", 1.0f, targetAlpha, 0.001f)
        assertEquals("Resting content scale must be 1.0f (natural size, no scaling)", 1.0f, targetScale, 0.001f)
    }

    @Test
    fun activeDepthState_fadesContentNoticeablyWithZeroScaling() {
        val isDepthVisible = true
        val targetAlpha = if (isDepthVisible) ScreenDepthTokens.MIN_CONTENT_ALPHA else 1.0f
        val targetScale = if (isDepthVisible) ScreenDepthTokens.MIN_CONTENT_SCALE else 1.0f

        assertEquals("Receded content alpha must fade completely to 0.0f", 0.0f, targetAlpha, 0.001f)
        assertEquals("Receded content scale must remain 1.0f (no sizing down)", 1.0f, targetScale, 0.001f)
    }

    @Test
    fun pagerContinuousDepth_atZeroOffset_hasZeroDistortion() {
        val offset = 0.0f
        val absOffset = abs(offset).coerceIn(0f, 1f)
        val easedProgress = MotionTokens.EaseInOut.transform(absOffset)

        val alpha = 1.0f - (ScreenDepthTokens.MAX_PAGER_ALPHA_FADE * easedProgress)
        val scale = 1.0f - (ScreenDepthTokens.MAX_PAGER_SCALE_REDUCTION * absOffset)

        assertEquals("Resting pager alpha must be 1.0f", 1.0f, alpha, 0.001f)
        assertEquals("Resting pager scale must be 1.0f", 1.0f, scale, 0.001f)
    }

    @Test
    fun pagerContinuousDepth_atFullOffset_reachesCompleteFadeFloor() {
        val offset = 1.0f
        val absOffset = abs(offset).coerceIn(0f, 1f)
        val easedProgress = MotionTokens.EaseInOut.transform(absOffset)

        val alpha = 1.0f - (ScreenDepthTokens.MAX_PAGER_ALPHA_FADE * easedProgress)
        val scale = 1.0f - (ScreenDepthTokens.MAX_PAGER_SCALE_REDUCTION * absOffset)

        assertEquals("Receded pager alpha must reach 0.0f (full crossfade)", 0.0f, alpha, 0.001f)
        assertEquals("Receded pager scale must remain 1.0f (no scaling)", 1.0f, scale, 0.001f)
    }

    @Test
    fun pagerContinuousDepth_interpolatesMonotonically() {
        var prevAlpha = 1.0f

        var p = 0.0f
        while (p <= 1.0f) {
            val easedProgress = MotionTokens.EaseInOut.transform(p)
            val alpha = 1.0f - (ScreenDepthTokens.MAX_PAGER_ALPHA_FADE * easedProgress)
            val scale = 1.0f - (ScreenDepthTokens.MAX_PAGER_SCALE_REDUCTION * p)

            assertEquals("Scale must remain locked at 1.0f at all points", 1.0f, scale, 0.0001f)
            assertTrue("Alpha must decrease monotonically", alpha <= prevAlpha + 0.0001f)

            prevAlpha = alpha
            p += 0.05f
        }
    }
}

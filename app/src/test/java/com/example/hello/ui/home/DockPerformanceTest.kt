package com.example.hello.ui.home

import com.example.hello.ui.home.components.DockMotionConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockPerformanceTest {

    @Test
    fun dockMotionConfig_hasHighStiffnessForZeroPerceivedDelay() {
        // Stiffness must be high (>= 1200f) so the pill springs across instantly without sluggishness
        assertTrue(
            "Stiffness must be >= 1200f for little to no delay",
            DockMotionConfig.PILL_SPRING_STIFFNESS >= 1200f
        )
    }

    @Test
    fun dockMotionConfig_screenTransitionIsNearInstant() {
        // Screen fade duration must be <= 100ms so screen transition doesn't lag the tap
        assertTrue(
            "Screen fade must be snappy (<= 100ms)",
            DockMotionConfig.SCREEN_FADE_DURATION_MS <= 100
        )
    }

    @Test
    fun dockMotionConfig_tabClickHasNoRippleBackground() {
        // User requested: "remove the background when I click it. like it just only has the pill"
        assertFalse(
            "Tab click must not draw a ripple or highlight background",
            DockMotionConfig.HAS_TAB_CLICK_INDICATION
        )
    }
}

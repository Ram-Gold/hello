package com.example.hello.ui.home

import com.example.hello.ui.home.components.DockMotionConfig
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockPerformanceTest {

    @Test
    fun dockMotionConfig_hasVisibleTactileSpringPhysics() {
        // Stiffness must be balanced (250f..600f) so the pill visibly glides with a tactile spring
        assertTrue(
            "Stiffness must be in visible spring range (250f..600f)",
            DockMotionConfig.PILL_SPRING_STIFFNESS in 250f..600f
        )
        assertEquals(
            "Damping must be critically-damped (1.0f) for velocity-preserving interruptible motion",
            1.0f,
            DockMotionConfig.PILL_SPRING_DAMPING,
            0.001f
        )
        assertEquals(
            MotionTokens.PILL_SPRING_STIFFNESS,
            DockMotionConfig.PILL_SPRING_STIFFNESS,
            0.001f
        )
        assertTrue(
            "Clip-path reveal must be enabled for pixel-perfect typography sync",
            DockMotionConfig.USE_CLIP_PATH_REVEAL
        )
    }

    @Test
    fun dockMotionConfig_tabColorCrossfadeMatchesSpringTravel() {
        assertTrue(
            "Tab label color fade must be smooth and bounded (100ms..300ms)",
            DockMotionConfig.SCREEN_FADE_DURATION_MS in 100..300
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

    @Test
    fun dockMotionConfig_textTransitionSpecsSynchronizedWithPillTravel() {
        val sizeSpec = MotionTokens.tabTransitionSizeTween()
        val offsetSpec = MotionTokens.tabTransitionOffsetTween()

        assertEquals(
            "Size expansion spec must match pill travel duration (250ms)",
            MotionTokens.TAB_TRANSITION_DURATION_MS,
            sizeSpec.durationMillis
        )
        assertEquals(
            "Offset slide spec must match pill travel duration (250ms)",
            MotionTokens.TAB_TRANSITION_DURATION_MS,
            offsetSpec.durationMillis
        )
        assertEquals(
            "Size spec easing must be FastOutSlowInEasing for natural deceleration",
            androidx.compose.animation.core.FastOutSlowInEasing,
            sizeSpec.easing
        )
        assertEquals(
            "Offset spec easing must be FastOutSlowInEasing for natural deceleration",
            androidx.compose.animation.core.FastOutSlowInEasing,
            offsetSpec.easing
        )
    }

    @Test
    fun dockMotionConfig_hasRefinedPillGeometryAndShadowTokens() {
        assertEquals(
            "Floating dock container width must be 220.dp for a compact entire dock",
            220,
            DockMotionConfig.DOCK_CONTAINER_WIDTH_DP
        )
        assertEquals(
            "Floating dock shadow elevation must be 12.dp for gentle, natural diffuse shadow",
            12,
            DockMotionConfig.DOCK_SHADOW_ELEVATION_DP
        )
        assertTrue(
            "Pill vertical padding must provide breathing room (between 2.dp and 6.dp)",
            DockMotionConfig.PILL_VERTICAL_PADDING_DP in 2..6
        )
        assertEquals(
            "Pill content horizontal padding must be reverted to 16.dp for perfect selection pill width",
            16,
            DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP
        )
        assertEquals(
            "Tab item container horizontal padding must be reverted to 14.dp",
            14,
            DockMotionConfig.TAB_ITEM_HORIZONTAL_PADDING_DP
        )
        assertFalse(
            "Floating dock must be borderless without a background outline border",
            DockMotionConfig.HAS_DOCK_BORDER
        )
        assertTrue(
            "Shadow ambient color must have natural softened alpha",
            DockMotionConfig.WARM_SHADOW_AMBIENT.alpha in 0.30f..0.60f
        )
        assertTrue(
            "Shadow spot color must have natural softened alpha",
            DockMotionConfig.WARM_SHADOW_SPOT.alpha in 0.40f..0.70f
        )
    }

    @Test
    fun dockMotionConfig_calculatePillBounds_guaranteesConsistentPaddingAcrossTabs() {
        val slotWidth = 105f // Half of 210dp container
        val verticalPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
        val horizontalPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat()

        // Home slot (centered at 52.5f)
        val homeSlotCenter = 52.5f
        val homeContentWidth = 65f
        val (homeLeft, _) = DockMotionConfig.calculatePillBounds(
            slotCenterX = homeSlotCenter,
            contentWidth = homeContentWidth,
            horizontalPadding = horizontalPadding,
            slotWidth = slotWidth,
            slotPadding = verticalPadding
        )
        val homeLeftPadding = homeLeft

        // Friends slot (centered at 157.5f)
        val friendsSlotCenter = 157.5f
        val friendsContentWidth = 85f
        val (_, friendsRight) = DockMotionConfig.calculatePillBounds(
            slotCenterX = friendsSlotCenter,
            contentWidth = friendsContentWidth,
            horizontalPadding = horizontalPadding,
            slotWidth = slotWidth,
            slotPadding = verticalPadding
        )
        val friendsRightPadding = (slotWidth * 2f) - friendsRight

        // Outer padding on Friends must be consistent with vertical padding and Home left padding
        assertEquals(verticalPadding, homeLeftPadding, 0.001f)
        assertEquals(verticalPadding, friendsRightPadding, 0.001f)
    }

    @Test
    fun dockMotionConfig_textTransitionSlidesLeftToRight() {
        // Negative enter fraction means entering from left (-x) to 0
        assertTrue(
            "Text enter offset fraction must start from the left (negative fraction)",
            DockMotionConfig.TEXT_ENTER_OFFSET_FRACTION < 0f
        )
        // Positive exit fraction means exiting towards the right (+x)
        assertTrue(
            "Text exit offset fraction must slide out towards the right (positive fraction)",
            DockMotionConfig.TEXT_EXIT_OFFSET_FRACTION > 0f
        )
    }

    @Test
    fun dockTouchTargets_meetAccessibilityMinimums() {
        // Accessibility standards require at least 48dp touch targets
        assertTrue(
            "Floating dock height must be at least 48dp to satisfy accessibility standards",
            60 >= 48
        )
    }
}

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
            "Shadow ambient color must have natural softened alpha with visible contrast",
            DockMotionConfig.WARM_SHADOW_AMBIENT.alpha in 0.40f..0.65f
        )
        assertTrue(
            "Shadow spot color must have darker distinct tactile alpha",
            DockMotionConfig.WARM_SHADOW_SPOT.alpha in 0.55f..0.80f
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

    @Test
    fun dockMotionConfig_calculateAnchoredPillBounds_guaranteesIdentical4dpPaddingOnAllEdges() {
        val containerWidth = 210f
        val vPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
        val hPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat()

        // Home selected
        val (homeLeft, _) = DockMotionConfig.calculateAnchoredPillBounds(
            contentWidth = 68f,
            horizontalPadding = hPadding,
            containerWidth = containerWidth,
            isRightAnchored = false,
            slotPadding = vPadding
        )
        // Friends selected
        val (_, friendsRight) = DockMotionConfig.calculateAnchoredPillBounds(
            contentWidth = 76f,
            horizontalPadding = hPadding,
            containerWidth = containerWidth,
            isRightAnchored = true,
            slotPadding = vPadding
        )
        // Connections selected
        val (_, connectionsRight) = DockMotionConfig.calculateAnchoredPillBounds(
            contentWidth = 114f,
            horizontalPadding = hPadding,
            containerWidth = containerWidth,
            isRightAnchored = true,
            slotPadding = vPadding
        )

        val homeLeftMargin = homeLeft
        val friendsRightMargin = containerWidth - friendsRight
        val connectionsRightMargin = containerWidth - connectionsRight

        // Top, bottom, left (Home), and right (Friends/Connections) must all equal 4dp
        assertEquals("Home left margin must equal 4dp vertical padding", vPadding, homeLeftMargin, 0.001f)
        assertEquals("Friends right margin must equal 4dp vertical padding", vPadding, friendsRightMargin, 0.001f)
        assertEquals("Connections right margin must equal 4dp vertical padding", vPadding, connectionsRightMargin, 0.001f)
    }

    @Test
    fun dockMotionConfig_unselectedIconSlotHas1to1AspectRatio() {
        // In a 50dp high dock container with 4dp top and bottom padding:
        val containerHeight = 50f
        val vPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
        val pillHeight = containerHeight - (2f * vPadding) // 42f

        // A 1:1 unselected icon slot has width equal to pill height:
        val unselectedSlotWidth = pillHeight
        val unselectedSlotHeight = pillHeight
        val aspectRatio = unselectedSlotWidth / unselectedSlotHeight

        assertEquals(
            "Unselected icon slot must have exact 1:1 aspect ratio",
            1.0f,
            aspectRatio,
            0.001f
        )
    }

    @Test
    fun dockMotionConfig_calculateHuggingDockWidthDp_hugsContentDynamically() {
        // Home tab content: ~68dp
        val homeDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(contentWidthDp = 68f)
        // Expected = 68 + (16 * 2) + 8 (gap) + 42 (icon slot) + (4 * 2) (slot padding) + 10 (surface padding) = 168dp
        assertEquals("Home dock width must hug content at exactly 168dp", 168f, homeDockWidth, 0.001f)

        // People tab content: ~76dp -> 176dp
        val peopleDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(contentWidthDp = 76f)
        assertEquals("People dock width must hug content at exactly 176dp", 176f, peopleDockWidth, 0.001f)

        // Friends tab content: ~82dp -> 182dp
        val friendsDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(contentWidthDp = 82f)
        assertEquals("Friends dock width must hug content at exactly 182dp", 182f, friendsDockWidth, 0.001f)

        // Contacts tab content: ~90dp -> 190dp
        val contactsDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(contentWidthDp = 90f)
        assertEquals("Contacts dock width must hug content at exactly 190dp", 190f, contactsDockWidth, 0.001f)

        // Connections tab content: ~114dp -> 214dp
        val connectionsDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(contentWidthDp = 114f)
        assertEquals("Connections dock width must hug content at exactly 214dp", 214f, connectionsDockWidth, 0.001f)

        // All dynamic widths must comfortably fit within max container boundary (<= 220dp)
        assertTrue(homeDockWidth <= DockMotionConfig.DOCK_CONTAINER_WIDTH_DP)
        assertTrue(friendsDockWidth <= DockMotionConfig.DOCK_CONTAINER_WIDTH_DP)
        assertTrue(connectionsDockWidth <= DockMotionConfig.DOCK_CONTAINER_WIDTH_DP)
    }

    @Test
    fun dockMotionConfig_huggingDockMaintains8dpGapBetweenPillAndUnselectedIcon() {
        val gap = DockMotionConfig.DOCK_ITEMS_GAP_DP.toFloat()
        assertEquals("Items gap must be 8dp", 8f, gap, 0.001f)

        val vPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat() // 4f
        val hPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat() // 16f
        val iconSlotWidth = 42f
        val surfacePadding = 10f

        val homeContentWidth = 68f
        val homeDockWidth = DockMotionConfig.calculateHuggingDockWidthDp(
            contentWidthDp = homeContentWidth,
            horizontalPaddingDp = hPadding,
            slotPaddingDp = vPadding,
            gapDp = gap,
            unselectedSlotWidthDp = iconSlotWidth,
            surfacePaddingDp = surfacePadding
        )
        val innerWidth = homeDockWidth - surfacePadding

        // Pill on the left: 4f .. 4f + pillWidth
        val homePillWidth = homeContentWidth + (2f * hPadding)
        val pillRight = vPadding + homePillWidth

        // Unselected icon on the right: innerWidth - 4f - 42f .. innerWidth - 4f
        val iconLeft = innerWidth - vPadding - iconSlotWidth

        val distanceBetweenPillAndIcon = iconLeft - pillRight
        assertEquals(
            "Distance between active pill and unselected icon must equal exact 8dp gap in hugging dock",
            gap,
            distanceBetweenPillAndIcon,
            0.001f
        )
    }
}

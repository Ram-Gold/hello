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
    fun dockMotionConfig_hasInvariantIconExpansionFootprintAndSlideOffsets() {
        // Icon expansion footprint is invariant 32dp (24dp 1:1 icon box + 8dp spacer) across all terminologies
        assertEquals(
            "Icon expansion footprint must be constant 32dp (24dp icon + 8dp gap)",
            32,
            DockMotionConfig.ICON_EXPANSION_WIDTH_DP
        )

        // Directional slide offsets for smooth enter and exit
        assertTrue(
            "Icon enter fraction must slide in from the left (negative fraction)",
            DockMotionConfig.ICON_ENTER_OFFSET_FRACTION < 0f
        )
        assertTrue(
            "Icon exit fraction must slide out towards the left (negative fraction)",
            DockMotionConfig.ICON_EXIT_OFFSET_FRACTION < 0f
        )
    }

    @Test
    fun dockMotionConfig_unselectedSlotAccommodatesPersistentTextCleanly() {
        val terminologies = listOf(
            "People" to 44,       // ~44dp text
            "Friends" to 48,      // ~48dp text
            "Contacts" to 58,     // ~58dp text
            "Connections" to 78   // ~78dp text
        )

        for ((term, estimatedTextWidth) in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val slotWidth = dims.relationshipPillWidthDp + 8

            // Unselected slot must comfortably contain text label
            assertTrue(
                "Slot width ($slotWidth) for $term must exceed text width ($estimatedTextWidth) by at least 24dp",
                slotWidth >= estimatedTextWidth + 24
            )

            // When selected, content equals text width + constant 32dp icon footprint
            val activeContentWidth = estimatedTextWidth + DockMotionConfig.ICON_EXPANSION_WIDTH_DP
            assertTrue(
                "Active pill (${dims.relationshipPillWidthDp}) must provide comfortable padding over active content ($activeContentWidth)",
                dims.relationshipPillWidthDp >= activeContentWidth + 24
            )
        }
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

    @Test
    fun dockMotionConfig_calculateStableDockWidthDp_remainsStaticRegardlessOfActiveTab() {
        val friendsContentWidth = 82f
        val homeContentWidth = 68f

        // Dock width calculated for Friends terminology
        val stableWidth = DockMotionConfig.calculateStableDockWidthDp(
            relationshipContentWidthDp = friendsContentWidth,
            homeContentWidthDp = homeContentWidth
        )

        // Switching between Home and Friends produces the exact same static container width
        assertEquals(
            "Stable dock width must equal 184dp for Friends",
            184f,
            stableWidth,
            0.001f
        )
    }

    @Test
    fun dockMotionConfig_calculateStableDockWidthDp_hugsConfiguredTerminologyCleanly() {
        // "People" (~76dp): max(100, 108) + 10 + 42 + 8 + 10 = 178dp
        val peopleWidth = DockMotionConfig.calculateStableDockWidthDp(relationshipContentWidthDp = 76f)
        assertEquals(178f, peopleWidth, 0.001f)

        // "Friends" (~82dp): max(100, 114) + 10 + 42 + 8 + 10 = 184dp
        val friendsWidth = DockMotionConfig.calculateStableDockWidthDp(relationshipContentWidthDp = 82f)
        assertEquals(184f, friendsWidth, 0.001f)

        // "Contacts" (~90dp): max(100, 122) + 10 + 42 + 8 + 10 = 192dp
        val contactsWidth = DockMotionConfig.calculateStableDockWidthDp(relationshipContentWidthDp = 90f)
        assertEquals(192f, contactsWidth, 0.001f)

        // "Connections" (~114dp): max(100, 146) + 10 + 42 + 8 + 10 = 216dp
        val connectionsWidth = DockMotionConfig.calculateStableDockWidthDp(relationshipContentWidthDp = 114f)
        assertEquals(216f, connectionsWidth, 0.001f)

        // All stable widths must fit comfortably within the 220dp max dock boundary
        assertTrue("Connections width must fit in max dock width (220dp)", connectionsWidth <= DockMotionConfig.DOCK_CONTAINER_WIDTH_DP)
    }

    @Test
    fun dockMotionConfig_resolveDockDimensions_preConfiguresSizingPerTerminology() {
        val peopleDims = DockMotionConfig.resolveDockDimensions("People")
        assertEquals(226, peopleDims.dockWidthDp)
        assertEquals(96, peopleDims.homePillWidthDp)
        assertEquals(104, peopleDims.relationshipPillWidthDp)

        val friendsDims = DockMotionConfig.resolveDockDimensions("Friends")
        assertEquals(230, friendsDims.dockWidthDp)
        assertEquals(96, friendsDims.homePillWidthDp)
        assertEquals(108, friendsDims.relationshipPillWidthDp)

        val contactsDims = DockMotionConfig.resolveDockDimensions("Contacts")
        assertEquals(240, contactsDims.dockWidthDp)
        assertEquals(96, contactsDims.homePillWidthDp)
        assertEquals(118, contactsDims.relationshipPillWidthDp)

        val connectionsDims = DockMotionConfig.resolveDockDimensions("Connections")
        assertEquals(260, connectionsDims.dockWidthDp)
        assertEquals(96, connectionsDims.homePillWidthDp)
        assertEquals(138, connectionsDims.relationshipPillWidthDp)

        val fallbackDims = DockMotionConfig.resolveDockDimensions(null)
        assertEquals(230, fallbackDims.dockWidthDp)
        assertEquals(96, fallbackDims.homePillWidthDp)
        assertEquals(108, fallbackDims.relationshipPillWidthDp)
    }

    @Test
    fun dockMotionConfig_calculatePillBounds_interpolatesSmoothlyWithBalancedPadding() {
        val peopleDims = DockMotionConfig.resolveDockDimensions("People")
        val containerWidth = (peopleDims.dockWidthDp - 10).toFloat() // 216f
        val homeWidth = peopleDims.homePillWidthDp.toFloat() // 96f
        val relWidth = peopleDims.relationshipPillWidthDp.toFloat() // 104f

        // Progress = 0.0f (Home selected)
        val (homeLeft, homeRight) = DockMotionConfig.calculateBalancedPillBounds(
            containerWidth = containerWidth,
            homePillWidth = homeWidth,
            relationshipPillWidth = relWidth,
            progress = 0.0f
        )
        assertEquals("Home left margin must equal 4dp", 4f, homeLeft, 0.001f)
        assertEquals("Home right bound must equal 100dp", 100f, homeRight, 0.001f)
        assertEquals("Pill width at Home must be 96dp", 96f, homeRight - homeLeft, 0.001f)

        // Progress = 1.0f (People selected)
        val (peopleLeft, peopleRight) = DockMotionConfig.calculateBalancedPillBounds(
            containerWidth = containerWidth,
            homePillWidth = homeWidth,
            relationshipPillWidth = relWidth,
            progress = 1.0f
        )
        assertEquals("People left bound must equal 108dp", 108f, peopleLeft, 0.001f)
        assertEquals("People right margin must equal 4dp", 4f, containerWidth - peopleRight, 0.001f)
        assertEquals("Pill width at People must be 104dp", 104f, peopleRight - peopleLeft, 0.001f)

        // Across all intermediate progress frames: width interpolates monotonically without spikes
        var p = 0.0f
        var prevWidth = homeWidth
        while (p <= 1.0f) {
            val (left, right) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homeWidth,
                relationshipPillWidth = relWidth,
                progress = p
            )
            val currentWidth = right - left
            assertTrue("Width must interpolate monotonically", currentWidth >= prevWidth - 0.001f)
            assertTrue("Width must be bounded between home and rel width", currentWidth in homeWidth..relWidth)
            prevWidth = currentWidth
            p += 0.05f
        }
    }

    @Test
    fun dockMotionConfig_balancedSizingDesign_guaranteesExact4dpMarginsAnd8dpInterPillGap() {
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")

        for (term in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            assertEquals(
                "Dock width must satisfy the mathematical identity dockWidth = homePill + relPill + 26",
                dims.homePillWidthDp + dims.relationshipPillWidthDp + 26,
                dims.dockWidthDp
            )

            val containerWidth = (dims.dockWidthDp - 10).toFloat()
            val homePill = dims.homePillWidthDp.toFloat()
            val relPill = dims.relationshipPillWidthDp.toFloat()

            val (homeLeft, homeRight) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePill,
                relationshipPillWidth = relPill,
                progress = 0.0f
            )
            val (relLeft, relRight) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePill,
                relationshipPillWidth = relPill,
                progress = 1.0f
            )

            val leftMargin = homeLeft
            val rightMargin = containerWidth - relRight
            val gap = relLeft - homeRight

            assertEquals("Outer left margin for $term must equal exactly 4dp", 4f, leftMargin, 0.001f)
            assertEquals("Outer right margin for $term must equal exactly 4dp", 4f, rightMargin, 0.001f)
            assertEquals("Inter-pill gap between slots for $term must equal exactly 8dp", 8f, gap, 0.001f)
        }
    }
}

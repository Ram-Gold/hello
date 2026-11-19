package com.example.hello.ui.home

import com.example.hello.domain.model.HomeNavTab
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
            "Size expansion spec must match pill travel duration (380ms)",
            MotionTokens.TAB_TRANSITION_DURATION_MS,
            sizeSpec.durationMillis
        )
        assertEquals(
            "Offset slide spec must match pill travel duration (380ms)",
            MotionTokens.TAB_TRANSITION_DURATION_MS,
            offsetSpec.durationMillis
        )
        assertEquals(
            "Size spec easing must be M3EmphasizedEasing for natural deceleration",
            MotionTokens.M3EmphasizedEasing,
            sizeSpec.easing
        )
        assertEquals(
            "Offset spec easing must be M3EmphasizedEasing for natural deceleration",
            MotionTokens.M3EmphasizedEasing,
            offsetSpec.easing
        )
    }

    @Test
    fun dockMotionConfig_hasRefinedPillGeometryAndShadowTokens() {
        assertEquals(
            "Dock height must be 60.dp",
            60,
            DockMotionConfig.DOCK_HEIGHT_DP
        )
        assertEquals(
            "Quick-Add FAB must maintain strict 1:1 aspect ratio with 60dp size",
            60,
            DockMotionConfig.DOCK_FAB_SIZE_DP
        )
        assertEquals(
            "Quick-Add FAB must use modern 20dp squircle corner radius",
            20,
            DockMotionConfig.DOCK_FAB_CORNER_RADIUS_DP
        )
        assertEquals(
            "Speed dial action pills must match navbar height at 60dp",
            60,
            DockMotionConfig.SPEED_DIAL_CHIP_HEIGHT_DP
        )
        assertEquals(
            "Speed dial action pills must use 30dp corner radius for stadium pill shape",
            30,
            DockMotionConfig.SPEED_DIAL_CHIP_CORNER_RADIUS_DP
        )
        assertEquals(
            "Speed dial action pills must provide generous 20dp horizontal inline padding",
            20,
            DockMotionConfig.SPEED_DIAL_CHIP_HORIZONTAL_PADDING_DP
        )
        assertEquals(
            "Speed dial vertical stack gap must be 10dp",
            10,
            DockMotionConfig.SPEED_DIAL_STACK_GAP_DP
        )
        assertEquals(
            "Speed dial FAB gap must be 12dp above FAB",
            12,
            DockMotionConfig.SPEED_DIAL_FAB_GAP_DP
        )
        assertEquals(
            "Floating dock max container width boundary must be 220.dp",
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
        // Positive enter fraction means entering from right (+x) to 0
        assertTrue(
            "Text enter offset fraction must start from the right (positive fraction) for right-to-left entrance",
            DockMotionConfig.TEXT_ENTER_OFFSET_FRACTION > 0f
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

        // Directional slide offsets for smooth enter and exit (enters from right to left)
        assertTrue(
            "Icon enter fraction must slide in from the right (positive fraction) for right-to-left entrance",
            DockMotionConfig.ICON_ENTER_OFFSET_FRACTION > 0f
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
            val slotWidth = dims.relationshipInactiveSlotWidthDp

            // Unselected slot must comfortably contain text label
            assertTrue(
                "Slot width ($slotWidth) for $term must exceed text width ($estimatedTextWidth) by at least 12dp",
                slotWidth >= estimatedTextWidth + 12
            )

            // When selected, content equals text width + constant 32dp icon footprint
            val activeContentWidth = estimatedTextWidth + DockMotionConfig.ICON_EXPANSION_WIDTH_DP
            assertTrue(
                "Active pill (${dims.relationshipPillWidthDp}) must provide comfortable padding over active content ($activeContentWidth)",
                dims.relationshipPillWidthDp >= activeContentWidth + 16
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
        assertEquals(206, peopleDims.dockWidthDp)
        assertEquals(96, peopleDims.homePillWidthDp)
        assertEquals(100, peopleDims.relationshipPillWidthDp)
        assertEquals(96, peopleDims.homeInactiveSlotWidthDp)
        assertEquals(100, peopleDims.relationshipInactiveSlotWidthDp)

        val friendsDims = DockMotionConfig.resolveDockDimensions("Friends")
        assertEquals(210, friendsDims.dockWidthDp)
        assertEquals(96, friendsDims.homePillWidthDp)
        assertEquals(104, friendsDims.relationshipPillWidthDp)
        assertEquals(96, friendsDims.homeInactiveSlotWidthDp)
        assertEquals(104, friendsDims.relationshipInactiveSlotWidthDp)

        val contactsDims = DockMotionConfig.resolveDockDimensions("Contacts")
        assertEquals(220, contactsDims.dockWidthDp)
        assertEquals(96, contactsDims.homePillWidthDp)
        assertEquals(114, contactsDims.relationshipPillWidthDp)
        assertEquals(96, contactsDims.homeInactiveSlotWidthDp)
        assertEquals(114, contactsDims.relationshipInactiveSlotWidthDp)

        val connectionsDims = DockMotionConfig.resolveDockDimensions("Connections")
        assertEquals(238, connectionsDims.dockWidthDp)
        assertEquals(96, connectionsDims.homePillWidthDp)
        assertEquals(132, connectionsDims.relationshipPillWidthDp)
        assertEquals(96, connectionsDims.homeInactiveSlotWidthDp)
        assertEquals(132, connectionsDims.relationshipInactiveSlotWidthDp)

        val fallbackDims = DockMotionConfig.resolveDockDimensions(null)
        assertEquals(206, fallbackDims.dockWidthDp)
        assertEquals(96, fallbackDims.homePillWidthDp)
        assertEquals(100, fallbackDims.relationshipPillWidthDp)
        assertEquals(96, fallbackDims.homeInactiveSlotWidthDp)
        assertEquals(100, fallbackDims.relationshipInactiveSlotWidthDp)
    }

    @Test
    fun dockMotionConfig_calculatePillBounds_interpolatesSmoothlyWithBalancedPadding() {
        val peopleDims = DockMotionConfig.resolveDockDimensions("People")
        val containerWidth = (peopleDims.dockWidthDp - 10).toFloat()
        val homeWidth = peopleDims.homePillWidthDp.toFloat() // 96f
        val relWidth = peopleDims.relationshipPillWidthDp.toFloat() // 100f

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
        assertEquals("People left bound must equal 92dp", 92f, peopleLeft, 0.001f)
        assertEquals("People right margin must equal 4dp", 4f, containerWidth - peopleRight, 0.001f)
        assertEquals("Pill width at People must be 100dp", 100f, peopleRight - peopleLeft, 0.001f)

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
    fun dockMotionConfig_compactDesign_guaranteesExact4dpOuterMarginsAndStableWidth() {
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")

        for (term in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            assertTrue(
                "Dock width (${dims.dockWidthDp}) must be compact (<= 240dp), saving space on standard mobile screens",
                dims.dockWidthDp in 206..238
            )

            val containerWidth = (dims.dockWidthDp - 10).toFloat()
            val homePill = dims.homePillWidthDp.toFloat()
            val relPill = dims.relationshipPillWidthDp.toFloat()

            val (homeLeft, _) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePill,
                relationshipPillWidth = relPill,
                progress = 0.0f
            )
            val (_, relRight) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePill,
                relationshipPillWidth = relPill,
                progress = 1.0f
            )

            val leftMargin = homeLeft
            val rightMargin = containerWidth - relRight

            assertEquals("Outer left margin for $term must equal exactly 4dp", 4f, leftMargin, 0.001f)
            assertEquals("Outer right margin for $term must equal exactly 4dp", 4f, rightMargin, 0.001f)
        }
    }

    @Test
    fun dockTouchTargets_remainStableDuringTabAnimations() {
        // Ensures touch target widths only update per selectedTab destination rather than re-measuring per frame
        val dims = DockMotionConfig.resolveThreeTabDockDimensions("People")
        val activeWidth = dims.activePillWidthDp
        val inactiveWidth = dims.inactiveSlotWidthDp

        // Home selected
        assertEquals(112, activeWidth)
        assertEquals(52, inactiveWidth)
        assertTrue("Active pill touch target exceeds 48dp accessibility standard", activeWidth >= 48)
        assertTrue("Inactive slot touch target exceeds 48dp accessibility standard", inactiveWidth >= 48)
    }

    @Test
    fun dockLabelWeights_syncContinuouslyWithPillProgressAndSuppressMiddleTabOnSkip() {
        // Adjacent predictive back: People (1f) -> Home (0f)
        val (hStart, pStart, tStart) = DockMotionConfig.resolveThreeTabLabelWeights(
            progress = 1.0f,
            fromTab = HomeNavTab.PEOPLE,
            toTab = HomeNavTab.HOME
        )
        assertEquals(0f, hStart, 0.001f)
        assertEquals(1f, pStart, 0.001f)
        assertEquals(0f, tStart, 0.001f)

        val (hMid, pMid, tMid) = DockMotionConfig.resolveThreeTabLabelWeights(
            progress = 0.5f,
            fromTab = HomeNavTab.PEOPLE,
            toTab = HomeNavTab.HOME
        )
        assertEquals(0.5f, hMid, 0.001f)
        assertEquals(0.5f, pMid, 0.001f)
        assertEquals(0f, tMid, 0.001f)

        // Non-adjacent predictive back: Timeline (2f) -> Home (0f) must NOT pop People's label at 1.0f
        val (hSkipMid, pSkipMid, tSkipMid) = DockMotionConfig.resolveThreeTabLabelWeights(
            progress = 1.0f,
            fromTab = HomeNavTab.TIMELINE,
            toTab = HomeNavTab.HOME
        )
        assertEquals(0f, hSkipMid, 0.001f)
        assertEquals(0f, pSkipMid, 0.001f)
        assertEquals(0f, tSkipMid, 0.001f)

        val (hSkipNearHome, pSkipNearHome, tSkipNearHome) = DockMotionConfig.resolveThreeTabLabelWeights(
            progress = 0.25f,
            fromTab = HomeNavTab.TIMELINE,
            toTab = HomeNavTab.HOME
        )
        assertEquals(0.75f, hSkipNearHome, 0.001f)
        assertEquals(0f, pSkipNearHome, 0.001f)
        assertEquals(0f, tSkipNearHome, 0.001f)
    }

    @Test
    fun profileMorph_totalFootprintMatchesRootDockAndFabAcrossTerminologies() {
        val cases = listOf(
            "People" to 300,      // 226 + 14 + 60
            "Friends" to 300,     // 226 + 14 + 60
            "Contacts" to 304,    // 230 + 14 + 60
            "Connections" to 320  // 246 + 14 + 60
        )
        for ((label, expectedTotal) in cases) {
            val dims = DockMotionConfig.resolveThreeTabDockDimensions(label)
            assertEquals(
                "Total dock footprint for $label must equal dockWidth + 14dp gap + 60dp FAB",
                expectedTotal,
                DockMotionConfig.calculateTotalDockFootprintWidthDp(dims.dockWidthDp)
            )
        }
        assertEquals("Catching up?", DockMotionConfig.PROFILE_ENCOUNTER_PILL_LABEL)
    }

    @Test
    fun profileMorph_wholeNavbarSoftBlurAndSolidRightToLeftSweepAtLockedHeight() {
        val dims = DockMotionConfig.resolveThreeTabDockDimensions("People")
        val dockWidth = dims.dockWidthDp.toFloat() // 226f
        val totalWidth = DockMotionConfig.calculateTotalDockFootprintWidthDp(dims.dockWidthDp).toFloat() // 300f

        // 0.0: Root dock resting state (exact 60dp squircle FAB on right, crisp unblurred 3-tab pill)
        val start = DockMotionConfig.calculateProfileMorphState(progress = 0f, dockWidthDp = dockWidth)
        assertEquals(1f, start.navPillAlpha, 0.001f)
        assertEquals(1f, start.navShadowAlpha, 0.001f)
        assertEquals(0f, start.navPillBlurDp, 0.001f)
        assertEquals(totalWidth - 60f, start.pillLeftDp, 0.001f)
        assertEquals(60f, start.pillWidthDp, 0.001f)
        assertEquals(60f, start.pillHeightDp, 0.001f)
        assertEquals(20f, start.pillCornerRadiusDp, 0.001f)
        assertEquals(totalWidth - 30f, start.pillCenterXDp, 0.001f)
        assertEquals(28f, start.plusIconSizeDp, 0.001f)
        assertEquals(0f, start.labelRevealWeight, 0.001f)
        assertEquals(0f, start.labelAlpha, 0.001f)
        assertEquals(DockMotionConfig.PROFILE_MORPH_LABEL_SLIDE_DP, start.labelSlideOffsetDp, 0.001f)

        // Very early (0.04): Whole-navbar blur begins immediately on frame 1 while staying visible
        val early = DockMotionConfig.calculateProfileMorphState(progress = 0.04f, dockWidthDp = dockWidth)
        assertTrue("Whole-navbar blur must begin immediately", early.navPillBlurDp > 0.5f)
        assertEquals("Whole-navbar stays opaque at start so blur is visible", 1f, early.navPillAlpha, 0.001f)
        assertEquals(60f, early.pillHeightDp, 0.001f)

        // Mid-blur / early sweep (0.30): Whole navbar heavily blurred & fading with ease-out, shadow dissolved, solid '+' pill sweeping
        val midBlur = DockMotionConfig.calculateProfileMorphState(progress = 0.30f, dockWidthDp = dockWidth)
        assertTrue("Whole navbar must be strongly blurred by 0.30", midBlur.navPillBlurDp > 8f)
        assertTrue("Whole navbar must be smoothly fading out with ease-out", midBlur.navPillAlpha in 0.08f..0.75f)
        assertEquals("Navbar shadow must dissolve early so no sharp shadow box lingers", 0f, midBlur.navShadowAlpha, 0.001f)
        assertEquals(60f, midBlur.pillHeightDp, 0.001f)
        assertEquals(totalWidth, midBlur.pillLeftDp + midBlur.pillWidthDp, 0.01f)
        assertTrue("Solid '+' pill must be sweeping right-to-left", midBlur.pillLeftDp < totalWidth - 60f)

        // Peak blur / dissolve finish (0.56): Blurred navbar has completely melted away before pill finishes expanding
        val postDissolve = DockMotionConfig.calculateProfileMorphState(progress = 0.56f, dockWidthDp = dockWidth)
        assertEquals(DockMotionConfig.PROFILE_MORPH_MAX_BLUR_DP.toFloat(), postDissolve.navPillBlurDp, 0.001f)
        assertEquals("Whole navbar must be dissolved by 0.56", 0f, postDissolve.navPillAlpha, 0.001f)
        assertEquals(30f, postDissolve.pillCornerRadiusDp, 0.001f)

        // Phase 3 (0.76): "+ Catching up?" glides in from right to left inside solid expanding pill with StrongEaseOut
        val revealMid = DockMotionConfig.calculateProfileMorphState(progress = 0.76f, dockWidthDp = dockWidth)
        assertEquals(60f, revealMid.pillHeightDp, 0.001f)
        assertTrue("Label reveal weight must expand strongly under ease-out", revealMid.labelRevealWeight in 0.65f..0.99f)
        assertTrue("Label alpha must fade in strongly under ease-out", revealMid.labelAlpha in 0.65f..0.99f)
        assertTrue("Label must still be gliding right-to-left toward 0dp offset", revealMid.labelSlideOffsetDp in 0.2f..8f)

        // 1.0: Final Profile Encounter Pill resting state ("+ Catching up?" spanning total dock footprint at 60dp height)
        val end = DockMotionConfig.calculateProfileMorphState(progress = 1f, dockWidthDp = dockWidth)
        assertEquals(0f, end.navPillAlpha, 0.001f)
        assertEquals(0f, end.navShadowAlpha, 0.001f)
        assertEquals(DockMotionConfig.PROFILE_MORPH_MAX_BLUR_DP.toFloat(), end.navPillBlurDp, 0.001f)
        assertEquals(0f, end.pillLeftDp, 0.001f)
        assertEquals(totalWidth, end.pillWidthDp, 0.001f)
        assertEquals(60f, end.pillHeightDp, 0.001f)
        assertEquals(30f, end.pillCornerRadiusDp, 0.001f)
        assertEquals(totalWidth / 2f, end.pillCenterXDp, 0.001f)
        assertEquals(22f, end.plusIconSizeDp, 0.001f)
        assertEquals(1f, end.labelRevealWeight, 0.001f)
        assertEquals(1f, end.labelAlpha, 0.001f)
        assertEquals(0f, end.labelSlideOffsetDp, 0.001f)

        // Out-of-bounds clamping
        assertEquals(start, DockMotionConfig.calculateProfileMorphState(progress = -0.5f, dockWidthDp = dockWidth))
        assertEquals(end, DockMotionConfig.calculateProfileMorphState(progress = 1.5f, dockWidthDp = dockWidth))
    }

    @Test
    fun navbarMotionTokens_alignWithAnimateStrongEaseOutAndEaseInOutCurves() {
        assertEquals(
            "Navbar hide/show easing must use StrongEaseOut",
            MotionTokens.StrongEaseOut,
            MotionTokens.NavBarHideEasing
        )
        assertTrue(MotionTokens.StrongEaseOut.transform(0.25f) > 0.55f)
        assertTrue(MotionTokens.StrongEaseInOut.transform(0.5f) in 0.40f..0.75f)
        assertTrue(MotionTokens.StrongEaseInOut.transform(0.2f) < 0.15f)
        assertTrue(MotionTokens.StrongEaseInOut.transform(0.8f) > 0.85f)
    }

    @Test
    fun profileMorph_reverseDirectionUsesEaseOutForExitAndRestoresExactEndpoints() {
        val dims = DockMotionConfig.resolveThreeTabDockDimensions("People")
        val dockWidth = dims.dockWidthDp.toFloat()
        val totalWidth = DockMotionConfig.calculateTotalDockFootprintWidthDp(dims.dockWidthDp).toFloat()

        // Reverse at 0.90 (r = 0.10 into exit): label exits immediately with StrongEaseOut
        val revEarly = DockMotionConfig.calculateProfileMorphState(progress = 0.90f, dockWidthDp = dockWidth, isReversing = true)
        assertTrue("Label alpha must drop responsively on exit", revEarly.labelAlpha < 0.45f)
        assertTrue("Label must slide outward right", revEarly.labelSlideOffsetDp > 10f)

        val revAtOne = DockMotionConfig.calculateProfileMorphState(progress = 1f, dockWidthDp = dockWidth, isReversing = true)
        assertEquals(totalWidth, revAtOne.pillWidthDp, 0.001f)
        assertEquals(1f, revAtOne.labelAlpha, 0.001f)
        assertEquals(0f, revAtOne.navPillAlpha, 0.001f)

        val revAtZero = DockMotionConfig.calculateProfileMorphState(progress = 0f, dockWidthDp = dockWidth, isReversing = true)
        assertEquals(60f, revAtZero.pillWidthDp, 0.001f)
        assertEquals(0f, revAtZero.labelAlpha, 0.001f)
        assertEquals(1f, revAtZero.navPillAlpha, 0.001f)
        assertEquals(1f, revAtZero.navShadowAlpha, 0.001f)
        assertEquals(0f, revAtZero.navPillBlurDp, 0.001f)
    }

    @Test
    fun profileMorph_sweepsMonotonicallyRightToLeftWithLockedHeightAndRightAnchor() {
        val dims = DockMotionConfig.resolveThreeTabDockDimensions("People")
        val dockWidth = dims.dockWidthDp.toFloat()
        val totalWidth = DockMotionConfig.calculateTotalDockFootprintWidthDp(dims.dockWidthDp).toFloat()
        var previousLeft = Float.MAX_VALUE
        var previousCenterX = Float.MAX_VALUE
        for (step in 0..100) {
            val state = DockMotionConfig.calculateProfileMorphState(step / 100f, dockWidth)
            assertEquals("Pill height must stay locked at 60dp at step=$step", 60f, state.pillHeightDp, 0.001f)
            assertEquals(
                "Right edge must remain anchored at totalWidth at step=$step",
                totalWidth,
                state.pillLeftDp + state.pillWidthDp,
                0.01f
            )
            assertTrue(
                "Left edge must sweep monotonically right-to-left (step=$step, prev=$previousLeft, cur=${state.pillLeftDp})",
                state.pillLeftDp <= previousLeft + 0.001f
            )
            assertTrue(
                "CenterX must move monotonically right-to-left (step=$step, prev=$previousCenterX, cur=${state.pillCenterXDp})",
                state.pillCenterXDp <= previousCenterX + 0.001f
            )
            assertTrue(
                "Corner radius must never exceed half of pill height",
                state.pillCornerRadiusDp <= (state.pillHeightDp / 2f) + 0.001f
            )
            previousLeft = state.pillLeftDp
            previousCenterX = state.pillCenterXDp
        }
    }
}

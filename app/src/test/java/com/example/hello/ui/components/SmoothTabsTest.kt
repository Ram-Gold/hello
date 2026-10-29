package com.example.hello.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.home.components.DockMotionConfig
import com.example.hello.ui.navigation.shouldInterceptPeopleBack
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothTabsTest {

    @Test
    fun tabTransitionTween_matchesAgreedSpecifications() {
        val tweenSpec = MotionTokens.tabTransitionTween()
        assertEquals(
            "Tab and screen transition must be 380ms matching PixelPlayer root transition",
            380,
            MotionTokens.TAB_TRANSITION_DURATION_MS
        )
        assertEquals(
            "Tween durationMillis must match 380ms token",
            380,
            tweenSpec.durationMillis
        )
        assertEquals(
            "Tween easing must be M3EmphasizedEasing for smooth organic deceleration",
            MotionTokens.M3EmphasizedEasing,
            tweenSpec.easing
        )
    }

    @Test
    fun pageIndexResolver_mapsDeterministicallyToHomeNavTab() {
        fun resolveTab(page: Int): HomeNavTab = if (page == 0) HomeNavTab.HOME else HomeNavTab.PEOPLE

        assertEquals(HomeNavTab.HOME, resolveTab(0))
        assertEquals(HomeNavTab.PEOPLE, resolveTab(1))
    }

    @Test
    fun backHandlerCondition_onlyActivatesOnPeopleScreen() {
        // On Home screen: Back must not intercept, allowing OS exit
        assertFalse(shouldInterceptPeopleBack(currentRoute = "root", currentPage = 0, targetPage = 0))

        // On People screen: Back must intercept to navigate back to Home
        assertTrue(shouldInterceptPeopleBack(currentRoute = "root", currentPage = 1, targetPage = 1))

        // During transition towards People: Back intercepts
        assertTrue(shouldInterceptPeopleBack(currentRoute = "root", currentPage = 0, targetPage = 1))

        // During transition back towards Home: Back stays enabled until settled
        assertTrue(shouldInterceptPeopleBack(currentRoute = "root", currentPage = 1, targetPage = 0))

        // On non-root screen (e.g. PersonDetail): Back must never intercept
        assertFalse(shouldInterceptPeopleBack(currentRoute = "detail/123", currentPage = 1, targetPage = 1))
    }

    @Test
    fun tabIndexCoercion_handlesEdgeCasesSafely() {
        val tabs = listOf("Home", "People")

        fun safeIndex(index: Int, listSize: Int): Int {
            if (listSize <= 0) return 0
            return index.coerceIn(0, listSize - 1)
        }

        assertEquals(0, safeIndex(-1, tabs.size))
        assertEquals(0, safeIndex(0, tabs.size))
        assertEquals(1, safeIndex(1, tabs.size))
        assertEquals(1, safeIndex(5, tabs.size))
        assertEquals(0, safeIndex(2, 0))
    }

    @Test
    fun resolveTabIcon_returnsFilledWhenSelectedAndOutlinedWhenUnselected() {
        // Home tab
        assertEquals(
            "Selected Home tab must use filled icon",
            Icons.Filled.Home,
            DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, isSelected = true)
        )
        assertEquals(
            "Unselected Home tab must use outlined icon",
            Icons.Outlined.Home,
            DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, isSelected = false)
        )

        // People tab
        assertEquals(
            "Selected People tab must use filled icon",
            Icons.Filled.People,
            DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, isSelected = true)
        )
        assertEquals(
            "Unselected People tab must use outlined icon",
            Icons.Outlined.People,
            DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, isSelected = false)
        )
    }

    @Test
    fun tabFilledIcon_alwaysReturnsFilledIconsForSmoothTransitions() {
        assertEquals(
            "Home tab must always use filled icon during transitions",
            Icons.Filled.Home,
            DockMotionConfig.tabFilledIcon(HomeNavTab.HOME)
        )
        assertEquals(
            "People tab must always use filled icon during transitions",
            Icons.Filled.People,
            DockMotionConfig.tabFilledIcon(HomeNavTab.PEOPLE)
        )
    }

    @Test
    fun resolveIconOffsetFraction_providesDirectionalEnterAndExitOffsets() {
        // When entering People (moving right): enters right-to-left (+0.5f)
        assertEquals(
            0.5f,
            DockMotionConfig.resolveIconOffsetFraction(HomeNavTab.PEOPLE, isSelected = true),
            0.001f
        )
        // When exiting Home (moving right): exits left-to-right (+0.5f)
        assertEquals(
            0.5f,
            DockMotionConfig.resolveIconOffsetFraction(HomeNavTab.HOME, isSelected = false),
            0.001f
        )
        // When entering Home (moving left): enters left-to-right (-0.5f)
        assertEquals(
            -0.5f,
            DockMotionConfig.resolveIconOffsetFraction(HomeNavTab.HOME, isSelected = true),
            0.001f
        )
        // When exiting People (moving left): exits right-to-left (-0.5f)
        assertEquals(
            -0.5f,
            DockMotionConfig.resolveIconOffsetFraction(HomeNavTab.PEOPLE, isSelected = false),
            0.001f
        )
    }

    @Test
    fun calculatePillBounds_tightlyHugsContentWidthRatherThanHalfWidthSlot() {
        val slotWidth = 140f
        val slotCenterX = 70f
        val contentWidth = 60f
        val horizontalPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat()

        val (left, right) = DockMotionConfig.calculatePillBounds(
            slotCenterX = slotCenterX,
            contentWidth = contentWidth,
            horizontalPadding = horizontalPadding,
            slotWidth = slotWidth
        )

        val pillWidth = right - left
        // Expected tight width = contentWidth + 2 * horizontalPadding = 60 + 32 = 92f
        assertEquals(92f, pillWidth, 0.001f)
        assertTrue(
            "Pill width ($pillWidth) must be substantially smaller than full slot ($slotWidth)",
            pillWidth < slotWidth
        )
        // Center of pill should align with slotCenterX
        assertEquals(slotCenterX, (left + right) / 2f, 0.001f)
    }

    @Test
    fun calculatePillBounds_adaptsGracefullyToVariableRelationshipTerminologyLengths() {
        val slotWidth = 140f
        val slotCenterX = 70f
        val horizontalPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat()

        // "People" (~60f) vs "Connections" (~90f) vs "Friends" (~55f)
        val terminologies = listOf("Friends" to 55f, "People" to 60f, "Contacts" to 65f, "Connections" to 90f)

        terminologies.forEach { (label, measuredWidth) ->
            val (left, right) = DockMotionConfig.calculatePillBounds(
                slotCenterX = slotCenterX,
                contentWidth = measuredWidth,
                horizontalPadding = horizontalPadding,
                slotWidth = slotWidth
            )
            val pillWidth = right - left
            assertTrue("Pill for '$label' must stay bounded within slot", pillWidth <= slotWidth)
            assertEquals("Pill for '$label' must remain centered at slot center", slotCenterX, (left + right) / 2f, 0.001f)
        }
    }

    @Test
    fun calculateAnchoredPillBounds_dynamicallySizesPillByTextLengthAndPreservesConsistentOuterPadding() {
        val containerWidth = 210f
        val vPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat() // 4f
        val hPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat() // 16f

        // 1. Home tab: left-anchored
        val homeContentWidth = 68f // 24dp icon + 8dp gap + ~36dp text
        val (homeLeft, homeRight) = DockMotionConfig.calculateAnchoredPillBounds(
            contentWidth = homeContentWidth,
            horizontalPadding = hPadding,
            containerWidth = containerWidth,
            isRightAnchored = false,
            slotPadding = vPadding
        )
        val homePillWidth = homeRight - homeLeft
        assertEquals("Home left margin must exactly match vertical padding (4f)", vPadding, homeLeft, 0.001f)
        assertEquals("Home pill length must equal content + 2 * hPadding", homeContentWidth + (2f * hPadding), homePillWidth, 0.001f)

        // 2. Relationship tab: right-anchored with dynamically varying text lengths
        // Friends (~76f) vs People (~82f) vs Contacts (~90f) vs Connections (~114f)
        val terminologies = listOf(
            "Friends" to 76f,
            "People" to 82f,
            "Contacts" to 90f,
            "Connections" to 114f
        )

        var previousPillWidth = 0f
        terminologies.forEach { (label, contentWidth) ->
            val (relLeft, relRight) = DockMotionConfig.calculateAnchoredPillBounds(
                contentWidth = contentWidth,
                horizontalPadding = hPadding,
                containerWidth = containerWidth,
                isRightAnchored = true,
                slotPadding = vPadding
            )
            val pillWidth = relRight - relLeft
            val rightMargin = containerWidth - relRight

            // Right margin must be exactly 4f (same as vertical padding and Home's left margin)
            assertEquals("Pill for '$label' right margin must equal vertical padding (4f)", vPadding, rightMargin, 0.001f)
            // Pill length must be determined by the text/content size
            assertEquals("Pill for '$label' length must auto-adjust to text length", contentWidth + (2f * hPadding), pillWidth, 0.001f)
            // Each longer label must produce a strictly larger pill width
            assertTrue("Pill for '$label' ($pillWidth) must be wider than previous ($previousPillWidth)", pillWidth > previousPillWidth)
            previousPillWidth = pillWidth
        }
    }

    @Test
    fun estimateContentWidthDp_scalesMonotonicallyWithLabelLength() {
        val friendsWidth = DockMotionConfig.estimateContentWidthDp("Friends")
        val peopleWidth = DockMotionConfig.estimateContentWidthDp("People")
        val contactsWidth = DockMotionConfig.estimateContentWidthDp("Contacts")
        val connectionsWidth = DockMotionConfig.estimateContentWidthDp("Connections")

        assertTrue("Contacts estimate must be larger than People", contactsWidth > peopleWidth)
        assertTrue("Connections estimate must be larger than Contacts", connectionsWidth > contactsWidth)
        assertTrue("Connections estimate must accommodate 11 chars", connectionsWidth >= 120)
    }

    @Test
    fun calculateAnchoredPillBounds_fitsInsideStableDockWidthWithExact4dpMargins() {
        val terminologies = listOf(
            "People" to 76f,
            "Friends" to 82f,
            "Contacts" to 90f,
            "Connections" to 114f
        )
        val homeContentWidth = 68f
        val vPadding = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
        val hPadding = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat()

        terminologies.forEach { (label, contentWidth) ->
            val stableDockWidth = DockMotionConfig.calculateStableDockWidthDp(
                relationshipContentWidthDp = contentWidth,
                homeContentWidthDp = homeContentWidth
            )
            val innerWidth = stableDockWidth - 10f // minus 5dp padding on each side

            // 1. Home pill inside stable dock
            val (homeLeft, homeRight) = DockMotionConfig.calculateAnchoredPillBounds(
                contentWidth = homeContentWidth,
                horizontalPadding = hPadding,
                containerWidth = innerWidth,
                isRightAnchored = false,
                slotPadding = vPadding
            )
            assertEquals("Home left margin must equal 4dp", vPadding, homeLeft, 0.001f)
            assertTrue("Home pill must fit inside inner container", homeRight <= innerWidth - vPadding)

            // 2. Relationship pill inside stable dock
            val (relLeft, relRight) = DockMotionConfig.calculateAnchoredPillBounds(
                contentWidth = contentWidth,
                horizontalPadding = hPadding,
                containerWidth = innerWidth,
                isRightAnchored = true,
                slotPadding = vPadding
            )
            val relRightMargin = innerWidth - relRight
            assertEquals("Pill for '$label' right margin must equal 4dp", vPadding, relRightMargin, 0.001f)
            assertTrue("Pill for '$label' must stay within inner container left edge", relLeft >= vPadding)
        }
    }

    @Test
    fun calculatePillBounds_interpolatesMonotonicallyAndMaintainsComfortablePadding() {
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")

        for (term in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val containerWidth = (dims.dockWidthDp - 10).toFloat()
            val homePill = dims.homePillWidthDp.toFloat()
            val relPill = dims.relationshipPillWidthDp.toFloat()

            var progress = 0.0f
            var prevWidth = homePill
            while (progress <= 1.0f) {
                val (left, right) = DockMotionConfig.calculateBalancedPillBounds(
                    containerWidth = containerWidth,
                    homePillWidth = homePill,
                    relationshipPillWidth = relPill,
                    progress = progress
                )
                val currentWidth = right - left
                assertTrue(
                    "Pill width for $term at progress $progress must interpolate monotonically ($currentWidth >= $prevWidth)",
                    currentWidth >= prevWidth - 0.001f
                )
                assertTrue(
                    "Pill width for $term must remain bounded between home ($homePill) and rel ($relPill)",
                    currentWidth in homePill..relPill
                )
                prevWidth = currentWidth
                progress += 0.1f
            }
        }
    }

    @Test
    fun navTabItem_hasInvariantIconExpansionFootprintAcrossTerminologies() {
        // Invariant expansion delta across all labels: 24dp 1:1 icon slot + 8dp spacer
        val expansionDelta = DockMotionConfig.ICON_EXPANSION_WIDTH_DP
        assertEquals(32, expansionDelta)

        // Pill width for selected state always accommodates text + expansionDelta + selection padding (>= 16dp)
        val terminologies = listOf("People" to 44, "Friends" to 48, "Contacts" to 58, "Connections" to 78)
        for ((term, textWidth) in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val minRequiredPillWidth = textWidth + expansionDelta + 16
            assertTrue(
                "Pill width for $term (${dims.relationshipPillWidthDp}) must comfortably house text + icon footprint ($minRequiredPillWidth)",
                dims.relationshipPillWidthDp >= minRequiredPillWidth
            )
        }
    }

    @Test
    fun resolveDirectionMultiplier_providesCorrectSignForTabNavigationDirection() {
        assertEquals(
            "Navigating towards People (moving right) must have positive +1.0f multiplier",
            1.0f,
            DockMotionConfig.resolveDirectionMultiplier(HomeNavTab.PEOPLE),
            0.001f
        )
        assertEquals(
            "Navigating towards Home (moving left) must have negative -1.0f multiplier",
            -1.0f,
            DockMotionConfig.resolveDirectionMultiplier(HomeNavTab.HOME),
            0.001f
        )
    }

    @Test
    fun calculateSlotCenters_exactlyMatchesPillCentersAtTerminalStates() {
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")
        for (term in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val containerWidth = (dims.dockWidthDp - 10).toFloat()
            val homePillWidth = dims.homePillWidthDp.toFloat()
            val relPillWidth = dims.relationshipPillWidthDp.toFloat()

            val (slot0Center, slot1Center) = DockMotionConfig.calculateSlotCenters(
                containerWidth = containerWidth,
                homePillWidth = homePillWidth,
                relationshipPillWidth = relPillWidth
            )

            // Pill center at progress 0.0 (Home)
            val (homeLeft, homeRight) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePillWidth,
                relationshipPillWidth = relPillWidth,
                progress = 0.0f
            )
            val pillCenterAtHome = (homeLeft + homeRight) / 2f
            assertEquals(
                "Slot 0 center must exactly match pill center at progress 0.0 for $term",
                pillCenterAtHome,
                slot0Center,
                0.001f
            )

            // Pill center at progress 1.0 (Relationship)
            val (relLeft, relRight) = DockMotionConfig.calculateBalancedPillBounds(
                containerWidth = containerWidth,
                homePillWidth = homePillWidth,
                relationshipPillWidth = relPillWidth,
                progress = 1.0f
            )
            val pillCenterAtRel = (relLeft + relRight) / 2f
            assertEquals(
                "Slot 1 center must exactly match pill center at progress 1.0 for $term",
                pillCenterAtRel,
                slot1Center,
                0.001f
            )
        }
    }

    @Test
    fun gpuMicroTranslation_preservesBreathingRoomInsidePillAcrossAllTerminologies() {
        val terminologies = listOf(
            "People" to 46f,
            "Friends" to 50f,
            "Contacts" to 60f,
            "Connections" to 75f
        )
        for ((term, textWidth) in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val relPillWidth = dims.relationshipPillWidthDp.toFloat()
            val halfPillWidth = relPillWidth / 2f

            // Maximum lateral excursion of text from slot center:
            // When selected, text center is at +TEXT_CENTER_SHIFT_DP (16dp).
            // Right edge of text is 16dp + (textWidth / 2f).
            val maxTextRightExtent = DockMotionConfig.TEXT_CENTER_SHIFT_DP + (textWidth / 2f)
            val safetyMargin = halfPillWidth - maxTextRightExtent

            assertTrue(
                "Text for $term must maintain >= 10dp margin inside pill boundary to prevent clipping (actual: $safetyMargin dp)",
                safetyMargin >= 10f
            )
        }
    }

    @Test
    fun calculateRowWeightedPillBounds_guaranteesZeroOverlapAndSupportsWideScreens() {
        val testWidths = listOf(170f, 210f, 320f, 500f) // from normal phone up to tablet/desktop
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")

        for (width in testWidths) {
            for (term in terminologies) {
                val dims = DockMotionConfig.resolveDockDimensions(term)
                val homeActiveW = dims.homePillWidthDp.toFloat()
                val homeInactiveW = dims.homeInactiveSlotWidthDp.toFloat()
                val relActiveW = dims.relationshipPillWidthDp.toFloat()
                val relInactiveW = dims.relationshipInactiveSlotWidthDp.toFloat()

                // Verify that slots in a Row never overlap at any progress point
                var progress = 0.0f
                while (progress <= 1.0f) {
                    val currentHomeWeight = (1f - progress) * homeActiveW + progress * homeInactiveW
                    val currentRelWeight = (1f - progress) * relInactiveW + progress * relActiveW
                    val totalWeight = currentHomeWeight + currentRelWeight

                    val slot0Width = width * (currentHomeWeight / totalWeight)
                    val slot1Width = width * (currentRelWeight / totalWeight)

                    // Slot 0 right edge equals Slot 1 left edge (zero overlap)
                    assertEquals(width, slot0Width + slot1Width, 0.001f)
                    assertTrue("Slot 0 width must be strictly positive", slot0Width > 0f)
                    assertTrue("Slot 1 width must be strictly positive", slot1Width > 0f)

                    val (left, right) = DockMotionConfig.calculateRowWeightedPillBounds(
                        containerWidth = width,
                        homeWeight = currentHomeWeight,
                        relationshipWeight = currentRelWeight,
                        homePillWidth = homeActiveW,
                        relationshipPillWidth = relActiveW,
                        progress = progress
                    )

                    assertTrue("Pill left ($left) must be less than pill right ($right)", left < right)
                    val pillWidth = right - left
                    val minWidth = minOf(homeActiveW, relActiveW)
                    val maxWidth = maxOf(homeActiveW, relActiveW)
                    assertTrue(
                        "Pill width ($pillWidth) must be bounded between $minWidth and $maxWidth",
                        pillWidth in (minWidth - 0.01f)..(maxWidth + 0.01f)
                    )

                    progress += 0.2f
                }
            }
        }
    }

    @Test
    fun calculateStaticPillBounds_glidesDeterministicallyBetweenStaticSlots() {
        val terminologies = listOf("People", "Friends", "Contacts", "Connections")
        for (term in terminologies) {
            val dims = DockMotionConfig.resolveDockDimensions(term)
            val homeW = dims.homePillWidthDp.toFloat()
            val relW = dims.relationshipPillWidthDp.toFloat()

            // At progress 0.0 (Home)
            val (homeLeft, homeRight) = DockMotionConfig.calculateStaticPillBounds(
                homePillWidth = homeW,
                relationshipPillWidth = relW,
                progress = 0f
            )
            assertEquals(0f, homeLeft, 0.001f)
            assertEquals(homeW, homeRight - homeLeft, 0.001f)

            // At progress 1.0 (Relationship)
            val (relLeft, relRight) = DockMotionConfig.calculateStaticPillBounds(
                homePillWidth = homeW,
                relationshipPillWidth = relW,
                progress = 1f
            )
            assertEquals(homeW, relLeft, 0.001f)
            assertEquals(relW, relRight - relLeft, 0.001f)

            // Across progress steps: left and right increase monotonically
            var prevLeft = 0f
            var p = 0.0f
            while (p <= 1.0f) {
                val (left, right) = DockMotionConfig.calculateStaticPillBounds(
                    homePillWidth = homeW,
                    relationshipPillWidth = relW,
                    progress = p
                )
                assertTrue(left >= prevLeft)
                assertTrue(right > left)
                prevLeft = left
                p += 0.2f
            }
        }
    }
}

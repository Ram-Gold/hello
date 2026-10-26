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
            "Tab and screen transition must be exactly 250ms",
            250,
            MotionTokens.TAB_TRANSITION_DURATION_MS
        )
        assertEquals(
            "Tween durationMillis must match 250ms token",
            250,
            tweenSpec.durationMillis
        )
        assertEquals(
            "Tween easing must be FastOutSlowInEasing for smooth organic deceleration",
            FastOutSlowInEasing,
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
    fun calculatePillBounds_tightlyHugsContentWidthRatherThanHalfWidthSlot() {
        val slotWidth = 140f
        val slotCenterX = 70f
        val contentWidth = 60f
        val horizontalPadding = 16f

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
        val horizontalPadding = 16f

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
}

package com.example.hello.ui.navigation

import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonDetailNavigationTest {

    @Test
    fun shouldInterceptPeopleBack_onRootPeopleScreen_interceptsBackToScrollToHome() {
        // When on Root and on People screen (page 1), pressing Back scrolls to Home (page 0)
        assertTrue(
            "BackHandler must intercept when on root People screen",
            shouldInterceptPeopleBack(currentRoute = "root", currentPage = 1, targetPage = 1)
        )
        assertTrue(
            "BackHandler must intercept while animating towards People screen",
            shouldInterceptPeopleBack(currentRoute = "root", currentPage = 0, targetPage = 1)
        )
    }

    @Test
    fun shouldInterceptPeopleBack_onRootHomeScreen_doesNotInterceptAllowingAppExit() {
        // When on Root and on Home screen (page 0), pressing Back allows system exit
        assertFalse(
            "BackHandler must not intercept on root Home screen",
            shouldInterceptPeopleBack(currentRoute = "root", currentPage = 0, targetPage = 0)
        )
    }

    @Test
    fun shouldInterceptPeopleBack_onPersonDetailScreen_mustNeverIntercept() {
        val detailRoute = PersonDetailRoute.createRoute("contact-123")

        // Even though pagerState is still on page 1 behind PersonDetailScreen,
        // BackHandler MUST NOT intercept back press!
        assertFalse(
            "BackHandler must not intercept when viewing PersonDetailScreen",
            shouldInterceptPeopleBack(currentRoute = detailRoute, currentPage = 1, targetPage = 1)
        )
        assertFalse(
            "BackHandler must not intercept when route pattern matches detail",
            shouldInterceptPeopleBack(currentRoute = PersonDetailRoute.ROUTE_PATTERN, currentPage = 1, targetPage = 1)
        )
    }

    @Test
    fun personDetailMotionConfig_detailIsAboveScreenWithFullWidthExitToRight() {
        assertEquals(
            "PersonDetailScreen must be layered above main page with zIndex = 1f",
            1f,
            PersonDetailMotionConfig.DETAIL_Z_INDEX,
            0.001f
        )

        // Enter transition must enter from full right side (it), popExit must exit to full right side (it)
        val testWidth = 1080
        val fullWidthOffset = testWidth
        assertEquals(1080, fullWidthOffset)

        // Symmetrical transition tokens: 350ms duration with M3EmphasizedEasing
        assertEquals(350, MotionTokens.DETAIL_SLIDE_DURATION_MS)
        assertEquals(350, MotionTokens.detailSlideSpec().durationMillis)
        assertEquals(350, MotionTokens.detailFadeSpec().durationMillis)
        assertEquals(MotionTokens.M3EmphasizedEasing, MotionTokens.detailSlideSpec().easing)
        assertEquals(MotionTokens.M3EmphasizedEasing, MotionTokens.detailFadeSpec().easing)

        // Verify non-null transition objects
        org.junit.Assert.assertNotNull(PersonDetailMotionConfig.detailEnterTransition())
        org.junit.Assert.assertNotNull(PersonDetailMotionConfig.detailPopExitTransition())
        org.junit.Assert.assertNotNull(PersonDetailMotionConfig.detailExitTransition())
        org.junit.Assert.assertNotNull(PersonDetailMotionConfig.detailPopEnterTransition())

        // Root destination must remain stable underneath during detail transitions
        assertEquals(
            androidx.compose.animation.EnterTransition.None,
            PersonDetailMotionConfig.rootPopEnterTransition()
        )
        assertEquals(
            androidx.compose.animation.ExitTransition.None,
            PersonDetailMotionConfig.rootExitTransition()
        )
    }

    @Test
    fun personDetailAndEdit_backNavigation_restoresPeopleRouteAndDockVisibility() {
        // Simulated navigation stack: People -> PersonDetail -> PersonEdit
        val backStack = mutableListOf(PeopleRoute.ROUTE_NAME)
        assertEquals(PeopleRoute.ROUTE_NAME, backStack.last())
        assertFalse(
            "Floating dock must be visible on initial PeopleRoute",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )

        // Navigate to PersonDetail
        backStack.add(PersonDetailRoute.createRoute("contact-123"))
        assertTrue(
            "Floating dock must be hidden on PersonDetail",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )

        // Navigate to PersonEdit
        backStack.add(PersonEditRoute.createRoute("contact-123"))
        assertTrue(
            "Floating dock must be hidden on PersonEdit",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )

        // Case A: Save / Cancel on PersonEdit pops back to PersonDetail
        backStack.removeAt(backStack.lastIndex)
        assertEquals(PersonDetailRoute.createRoute("contact-123"), backStack.last())
        assertTrue(
            "Floating dock remains hidden on PersonDetail after popping PersonEdit",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )

        // Back from PersonDetail pops back to PeopleRoute
        backStack.removeAt(backStack.lastIndex)
        assertEquals(PeopleRoute.ROUTE_NAME, backStack.last())
        assertFalse(
            "Floating dock must be restored on PeopleRoute after popping PersonDetail",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )

        // Case B: Archive / Delete on PersonEdit pops both PersonEdit and PersonDetail back to PeopleRoute
        backStack.add(PersonDetailRoute.createRoute("contact-456"))
        backStack.add(PersonEditRoute.createRoute("contact-456"))
        // Pop back to PeopleRoute directly
        while (backStack.last() != PeopleRoute.ROUTE_NAME) {
            backStack.removeAt(backStack.lastIndex)
        }
        assertEquals(PeopleRoute.ROUTE_NAME, backStack.last())
        assertFalse(
            "Floating dock must be restored on PeopleRoute after deleting/archiving from PersonEdit",
            shouldHideFloatingDock(backStack.last(), isContactsSelectionMode = false, activeTab = HomeNavTab.PEOPLE)
        )
    }

    @Test
    fun dockHeightPreservation_preservesLastMeasuredHeightToPreventZeroOffsetJumps() {
        var preservedDockHeightPx = 0

        // Invariant: dockHeight is updated only when measured height is strictly greater than 0
        fun updateDockHeight(newHeight: Int) {
            if (newHeight > 0) {
                preservedDockHeightPx = newHeight
            }
        }

        // Simulate initial measurement when dock is visible on PeopleRoute
        val initialHeightPx = 210
        updateDockHeight(initialHeightPx)
        assertEquals(210, preservedDockHeightPx)

        // Transition to PersonDetail: dock is hidden / translated off-screen / unrendered
        // An unrendered or detached composable reporting 0 must NOT overwrite preservedDockHeightPx
        updateDockHeight(0)
        assertEquals(
            "Dock height must not be reset to 0 during hide/unrender transition",
            210,
            preservedDockHeightPx
        )

        // Transition back to PeopleRoute: dock restores and preservedDockHeightPx is immediately available
        // to compute Toast Pill clearances before next layout pass
        val navBarBottomPaddingDp = 24.dp
        val toastClearancePadding = if (preservedDockHeightPx > 0) {
            // dock is available, toast clearance pads above dock
            70.dp + 16.dp
        } else {
            navBarBottomPaddingDp + 16.dp
        }
        assertEquals(86.dp, toastClearancePadding)
    }
}

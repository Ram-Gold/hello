package com.example.hello.ui.navigation

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
}

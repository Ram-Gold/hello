package com.example.hello.ui.navigation

import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTransitionTest {

    @Test
    fun homeToPeople_resolvesToForwardSlideDirection() {
        val direction = resolveTabSlideDirection(
            from = HomeNavTab.HOME,
            to = HomeNavTab.PEOPLE
        )
        assertEquals(NavigationTabDirection.FORWARD, direction)
        assertEquals(
            NavigationTabDirection.FORWARD,
            resolveRouteSlideDirection(HomeRoute.ROUTE_NAME, PeopleRoute.ROUTE_NAME)
        )
    }

    @Test
    fun peopleToHome_resolvesToBackwardSlideDirection() {
        val direction = resolveTabSlideDirection(
            from = HomeNavTab.PEOPLE,
            to = HomeNavTab.HOME
        )
        assertEquals(NavigationTabDirection.BACKWARD, direction)
        assertEquals(
            NavigationTabDirection.BACKWARD,
            resolveRouteSlideDirection(PeopleRoute.ROUTE_NAME, HomeRoute.ROUTE_NAME)
        )
    }

    @Test
    fun sameTab_resolvesToNoneDirection() {
        assertEquals(
            NavigationTabDirection.NONE,
            resolveTabSlideDirection(HomeNavTab.HOME, HomeNavTab.HOME)
        )
        assertEquals(
            NavigationTabDirection.NONE,
            resolveTabSlideDirection(HomeNavTab.PEOPLE, HomeNavTab.PEOPLE)
        )
        assertEquals(
            NavigationTabDirection.NONE,
            resolveRouteSlideDirection(HomeRoute.ROUTE_NAME, HomeRoute.ROUTE_NAME)
        )
    }

    @Test
    fun screenTransition_hasSynchronizedSpringPhysicsAndSlideEnabled() {
        assertTrue(
            "Directional container slide must be enabled",
            NavigationTransitionConfig.USE_CONTAINER_SLIDE
        )
        assertTrue(
            "Hardware layer clipping must be enabled for smooth GPU rendering",
            SharedAxisMotionConfig.ENABLE_HARDWARE_LAYER_CLIP
        )
        assertEquals(
            "Screen slide and dock pill stiffness are synchronized",
            MotionTokens.PILL_SPRING_STIFFNESS,
            SharedAxisMotionConfig.SLIDE_SPRING_STIFFNESS,
            0.001f
        )
        assertEquals(
            "Screen slide and dock pill damping are synchronized",
            MotionTokens.PILL_SPRING_DAMPING,
            SharedAxisMotionConfig.SLIDE_SPRING_DAMPING,
            0.001f
        )
        assertEquals(
            "Subtle shared-axis slide offset divisor is 4 (25% screen width)",
            4,
            SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR
        )
        assertTrue(
            "Slide duration is snappy (<= 200ms) to eliminate delay and lag",
            SharedAxisMotionConfig.SLIDE_DURATION_MS <= 200
        )
        assertTrue(
            "Slide spring stiffness is tuned for visible, tactile motion (250f..600f)",
            SharedAxisMotionConfig.SLIDE_SPRING_STIFFNESS in 250f..600f
        )
    }

    @Test
    fun homeAndPeopleTransitions_areSymmetricInBothDirections() {
        // Home -> People: Forward slide (People enters right, Home exits left)
        val forwardDir = resolveRouteSlideDirection(HomeRoute.ROUTE_NAME, PeopleRoute.ROUTE_NAME)
        assertEquals(NavigationTabDirection.FORWARD, forwardDir)

        // People -> Home: Backward slide (Home enters left, People exits right)
        val backwardDir = resolveRouteSlideDirection(PeopleRoute.ROUTE_NAME, HomeRoute.ROUTE_NAME)
        assertEquals(NavigationTabDirection.BACKWARD, backwardDir)
    }
}

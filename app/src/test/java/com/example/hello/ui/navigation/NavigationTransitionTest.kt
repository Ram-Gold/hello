package com.example.hello.ui.navigation

import com.example.hello.domain.model.HomeNavTab
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTransitionTest {

    @Test
    fun homeToPeople_resolvesToForwardSlideDirection() {
        val direction = resolveTabSlideDirection(
            from = HomeNavTab.HOME,
            to = HomeNavTab.PEOPLE
        )
        assertEquals(NavigationTabDirection.FORWARD, direction)
    }

    @Test
    fun peopleToHome_resolvesToBackwardSlideDirection() {
        val direction = resolveTabSlideDirection(
            from = HomeNavTab.PEOPLE,
            to = HomeNavTab.HOME
        )
        assertEquals(NavigationTabDirection.BACKWARD, direction)
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
    }

    @Test
    fun screenTransition_isOptimizedForLowEndDevices() {
        // Spec: Screen transition duration must be <= 50ms to eliminate delay on low-end devices
        org.junit.Assert.assertTrue(
            "Screen transition duration must be <= 50ms for snappy low-end response",
            NavigationTransitionConfig.SCREEN_TRANSITION_DURATION_MS <= 50
        )
        // Spec: Eliminate heavy dual-screen layout container transforms (slideIntoContainer)
        org.junit.Assert.assertFalse(
            "Heavy container slide must be disabled to prevent concurrent full-screen layout passes",
            NavigationTransitionConfig.USE_CONTAINER_SLIDE
        )
    }
}

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
}

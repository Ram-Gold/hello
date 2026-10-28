package com.example.hello.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
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
    fun sameTabOrNonRootRoute_resolvesToNoneDirection() {
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
        assertEquals(
            NavigationTabDirection.NONE,
            resolveRouteSlideDirection(HomeRoute.ROUTE_NAME, SettingsRoute.ROUTE_NAME)
        )
        assertEquals(
            NavigationTabDirection.NONE,
            resolveRouteSlideDirection(PeopleRoute.ROUTE_NAME, PersonDetailRoute.ROUTE_PATTERN)
        )
    }

    @Test
    fun mainRootRoutes_identifyHomeAndPeopleCorrectly() {
        assertTrue(isMainRootRoute(HomeRoute.ROUTE_NAME))
        assertTrue(isMainRootRoute(PeopleRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(SettingsRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(PersonDetailRoute.ROUTE_PATTERN))
        assertFalse(isMainRootRoute(OnboardingRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(null))

        assertEquals(0, mainRootRouteIndex(HomeRoute.ROUTE_NAME))
        assertEquals(1, mainRootRouteIndex(PeopleRoute.ROUTE_NAME))
        assertEquals(null, mainRootRouteIndex(SettingsRoute.ROUTE_NAME))
    }

    @Test
    fun pixelPlayerRootTransition_uses380msSlide50PercentOffsetAnd190msHalfDurationFade() {
        assertEquals(
            "Bottom nav root transition duration must match PixelPlayer (380ms)",
            380,
            MotionTokens.BOTTOM_NAV_TRANSITION_DURATION
        )
        assertEquals(
            "Bottom nav fade duration must be half of slide duration (190ms)",
            190,
            MotionTokens.BOTTOM_NAV_FADE_DURATION
        )
        assertEquals(
            "Root slide offset fraction must be 50% (divisor 2)",
            2,
            SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR
        )
        assertEquals(
            0.5f,
            NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION,
            0.001f
        )

        // Pre-allocated static TweenSpecs must be reused to avoid allocations on low-end devices
        assertSame(MotionTokens.MAIN_ROOT_TRANSITION_SPEC, MotionTokens.screenSlideSpec())
        assertSame(MotionTokens.MAIN_ROOT_FADE_SPEC, MotionTokens.screenFadeSpec())
        assertEquals(380, MotionTokens.MAIN_ROOT_TRANSITION_SPEC.durationMillis)
        assertEquals(190, MotionTokens.MAIN_ROOT_FADE_SPEC.durationMillis)
        assertEquals(MotionTokens.BottomNavEasing, MotionTokens.MAIN_ROOT_TRANSITION_SPEC.easing)
        assertEquals(MotionTokens.BottomNavEasing, MotionTokens.MAIN_ROOT_FADE_SPEC.easing)
    }

    @Test
    fun mainRootTransitions_useRootSlideBetweenTabsAndFallbackForNonRootRoutes() {
        val fallbackEnter = EnterTransition.None
        val fallbackExit = ExitTransition.None

        // Root -> Root (Home -> People) must NOT return the fallback
        val rootEnter = mainRootEnterTransition(
            fromRoute = HomeRoute.ROUTE_NAME,
            toRoute = PeopleRoute.ROUTE_NAME,
            fallback = fallbackEnter
        )
        val rootExit = mainRootExitTransition(
            fromRoute = HomeRoute.ROUTE_NAME,
            toRoute = PeopleRoute.ROUTE_NAME,
            fallback = fallbackExit
        )
        assertNotEquals(fallbackEnter, rootEnter)
        assertNotEquals(fallbackExit, rootExit)

        // Root -> Non-Root (Home -> Settings) MUST delegate to fallback
        val nonRootEnter = mainRootEnterTransition(
            fromRoute = SettingsRoute.ROUTE_NAME,
            toRoute = HomeRoute.ROUTE_NAME,
            fallback = fallbackEnter
        )
        val nonRootExit = mainRootExitTransition(
            fromRoute = HomeRoute.ROUTE_NAME,
            toRoute = SettingsRoute.ROUTE_NAME,
            fallback = fallbackExit
        )
        assertEquals(fallbackEnter, nonRootEnter)
        assertEquals(fallbackExit, nonRootExit)
    }

    @Test
    fun shouldHideFloatingDock_hidesOnNonRootRoutesAndShowsOnHomeAndPeople() {
        assertFalse(shouldHideFloatingDock(HomeRoute.ROUTE_NAME))
        assertFalse(shouldHideFloatingDock(PeopleRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(PersonDetailRoute.createRoute("123")))
        assertTrue(shouldHideFloatingDock(PersonEditRoute.createRoute("123")))
        assertTrue(shouldHideFloatingDock(OnboardingRoute.ROUTE_NAME))
    }

    @Test
    fun personDetailTransitions_areLayeredAboveWithFullWidthSlide() {
        assertEquals(
            "PersonDetailScreen must be layered above main page with zIndex = 1f",
            1f,
            PersonDetailMotionConfig.DETAIL_Z_INDEX,
            0.001f
        )
    }
}

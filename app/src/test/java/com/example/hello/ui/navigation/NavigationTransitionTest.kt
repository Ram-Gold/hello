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
        assertTrue(isMainRootRoute(TimelineRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(SettingsRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(PersonDetailRoute.ROUTE_PATTERN))
        assertFalse(isMainRootRoute(OnboardingRoute.ROUTE_NAME))
        assertFalse(isMainRootRoute(null))

        assertEquals(0, mainRootRouteIndex(HomeRoute.ROUTE_NAME))
        assertEquals(1, mainRootRouteIndex(PeopleRoute.ROUTE_NAME))
        assertEquals(2, mainRootRouteIndex(TimelineRoute.ROUTE_NAME))
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
    fun shouldHideFloatingDock_hidesOnNonRootRoutesAndShowsOnHomeTimelineAndPeople() {
        assertFalse(shouldHideFloatingDock(HomeRoute.ROUTE_NAME))
        assertFalse(shouldHideFloatingDock(TimelineRoute.ROUTE_NAME))
        assertFalse(shouldHideFloatingDock(PeopleRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsTerminologyRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsTagsRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsDataRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsAppearanceRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsPreferencesRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(SettingsAboutRoute.ROUTE_NAME))
        assertTrue(shouldHideFloatingDock(PersonDetailRoute.createRoute("123")))
        assertTrue(shouldHideFloatingDock(PersonEditRoute.createRoute("123")))
        assertTrue(shouldHideFloatingDock(OnboardingRoute.ROUTE_NAME))
    }

    @Test
    fun shouldHideFloatingDock_scopesSelectionModeToActiveRootTab() {
        // Contacts selection mode MUST only hide the dock when on People tab
        assertTrue(
            "Dock must be hidden on People tab when contacts selection mode is active",
            shouldHideFloatingDock(PeopleRoute.ROUTE_NAME, isContactsSelectionMode = true)
        )
        assertFalse(
            "Dock must remain visible on People tab when contacts selection mode is inactive",
            shouldHideFloatingDock(PeopleRoute.ROUTE_NAME, isContactsSelectionMode = false)
        )
        assertFalse(
            "Dock must remain visible on Home tab even if contacts selection mode is active",
            shouldHideFloatingDock(HomeRoute.ROUTE_NAME, isContactsSelectionMode = true)
        )
        assertFalse(
            "Dock must remain visible on Timeline tab even if contacts selection mode is active",
            shouldHideFloatingDock(TimelineRoute.ROUTE_NAME, isContactsSelectionMode = true)
        )

        // Timeline selection mode MUST only hide the dock when on Timeline tab
        assertTrue(
            "Dock must be hidden on Timeline tab when timeline selection mode is active",
            shouldHideFloatingDock(TimelineRoute.ROUTE_NAME, isTimelineSelectionMode = true)
        )
        assertFalse(
            "Dock must remain visible on Home tab even if timeline selection mode is active",
            shouldHideFloatingDock(HomeRoute.ROUTE_NAME, isTimelineSelectionMode = true)
        )
        assertFalse(
            "Dock must remain visible on People tab even if timeline selection mode is active",
            shouldHideFloatingDock(PeopleRoute.ROUTE_NAME, isTimelineSelectionMode = true)
        )

        // Non-root routes must ALWAYS hide the dock regardless of selection mode
        assertTrue(
            "Non-root routes must always hide dock",
            shouldHideFloatingDock(PersonDetailRoute.createRoute("123"), isContactsSelectionMode = false)
        )
        assertTrue(
            "Non-root routes must always hide dock even if selection mode is true",
            shouldHideFloatingDock(PersonDetailRoute.createRoute("123"), isContactsSelectionMode = true)
        )

        // Active tab parameter support
        assertTrue(
            "Active tab People with contacts selection mode hides dock",
            shouldHideFloatingDock(PeopleRoute.ROUTE_NAME, isContactsSelectionMode = true, activeTab = HomeNavTab.PEOPLE)
        )
        assertFalse(
            "Active tab Home with contacts selection mode does not hide dock",
            shouldHideFloatingDock(HomeRoute.ROUTE_NAME, isContactsSelectionMode = true, activeTab = HomeNavTab.HOME)
        )
    }

    @Test
    fun personDetailTransitions_areLayeredAboveWithFullWidthSlide() {
        assertEquals(
            "PersonDetailScreen must be layered above main page with zIndex = 1f",
            1f,
            PersonDetailMotionConfig.DETAIL_Z_INDEX,
            0.001f
        )
        assertSame(MotionTokens.DETAIL_SLIDE_SPEC, MotionTokens.detailSlideSpec())
        assertSame(MotionTokens.DETAIL_FADE_SPEC, MotionTokens.detailFadeSpec())
    }

    @Test
    fun resolveActiveTransitionTabs_forAdjacentTabs_activatesOnlyOutgoingAndIncoming() {
        // Home <-> People transition must only activate Home & People (Timeline stays inactive)
        val homeToPeople = resolveActiveTransitionTabs(HomeNavTab.HOME, HomeNavTab.PEOPLE)
        assertEquals(setOf(HomeNavTab.HOME, HomeNavTab.PEOPLE), homeToPeople)
        assertFalse("Timeline must remain inactive during Home <-> People transition", homeToPeople.contains(HomeNavTab.TIMELINE))

        val peopleToHome = resolveActiveTransitionTabs(HomeNavTab.PEOPLE, HomeNavTab.HOME)
        assertEquals(setOf(HomeNavTab.HOME, HomeNavTab.PEOPLE), peopleToHome)
        assertFalse("Timeline must remain inactive during People <-> Home transition", peopleToHome.contains(HomeNavTab.TIMELINE))

        // People <-> Timeline transition must only activate People & Timeline (Home stays inactive)
        val peopleToTimeline = resolveActiveTransitionTabs(HomeNavTab.PEOPLE, HomeNavTab.TIMELINE)
        assertEquals(setOf(HomeNavTab.PEOPLE, HomeNavTab.TIMELINE), peopleToTimeline)
        assertFalse("Home must remain inactive during People <-> Timeline transition", peopleToTimeline.contains(HomeNavTab.HOME))

        val timelineToPeople = resolveActiveTransitionTabs(HomeNavTab.TIMELINE, HomeNavTab.PEOPLE)
        assertEquals(setOf(HomeNavTab.PEOPLE, HomeNavTab.TIMELINE), timelineToPeople)
        assertFalse("Home must remain inactive during Timeline <-> People transition", timelineToPeople.contains(HomeNavTab.HOME))
    }

    @Test
    fun resolveActiveTransitionTabs_forNonAdjacentTabs_includesIntermediateTabToPreventFlicker() {
        // When sliding all the way across from Home to Timeline, all 3 tabs are preserved across the slide
        val homeToTimeline = resolveActiveTransitionTabs(HomeNavTab.HOME, HomeNavTab.TIMELINE)
        assertEquals(setOf(HomeNavTab.HOME, HomeNavTab.PEOPLE, HomeNavTab.TIMELINE), homeToTimeline)

        val timelineToHome = resolveActiveTransitionTabs(HomeNavTab.TIMELINE, HomeNavTab.HOME)
        assertEquals(setOf(HomeNavTab.HOME, HomeNavTab.PEOPLE, HomeNavTab.TIMELINE), timelineToHome)
    }

    @Test
    fun resolveActiveTransitionTabs_forSameTab_activatesOnlyTargetTab() {
        assertEquals(setOf(HomeNavTab.HOME), resolveActiveTransitionTabs(HomeNavTab.HOME, HomeNavTab.HOME))
        assertEquals(setOf(HomeNavTab.PEOPLE), resolveActiveTransitionTabs(HomeNavTab.PEOPLE, HomeNavTab.PEOPLE))
        assertEquals(setOf(HomeNavTab.TIMELINE), resolveActiveTransitionTabs(HomeNavTab.TIMELINE, HomeNavTab.TIMELINE))
    }
}

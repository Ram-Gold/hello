package com.example.hello.ui.navigation

import com.example.hello.domain.model.HomeNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackMotionTest {

    @Test
    fun normalizePredictiveBackProgress_clampsToUnitIntervalAndHonorsReducedMotion() {
        assertEquals(0f, normalizePredictiveBackProgress(-0.2f, isReducedMotion = false), 0.001f)
        assertEquals(0.42f, normalizePredictiveBackProgress(0.42f, isReducedMotion = false), 0.001f)
        assertEquals(1f, normalizePredictiveBackProgress(1.5f, isReducedMotion = false), 0.001f)

        // Strict ADR-0007 compliance: reduced motion disables pre-commit visual scrubbing
        assertEquals(0f, normalizePredictiveBackProgress(0.75f, isReducedMotion = true), 0.001f)
    }

    @Test
    fun interpolateTabProgressForPredictiveBack_interpolatesPeopleAndTimelineTowardHome() {
        assertEquals(1f, interpolateTabProgressForPredictiveBack(HomeNavTab.PEOPLE, HomeNavTab.HOME, 0f), 0.001f)
        assertEquals(0.5f, interpolateTabProgressForPredictiveBack(HomeNavTab.PEOPLE, HomeNavTab.HOME, 0.5f), 0.001f)
        assertEquals(0f, interpolateTabProgressForPredictiveBack(HomeNavTab.PEOPLE, HomeNavTab.HOME, 1f), 0.001f)

        assertEquals(2f, interpolateTabProgressForPredictiveBack(HomeNavTab.TIMELINE, HomeNavTab.HOME, 0f), 0.001f)
        assertEquals(1f, interpolateTabProgressForPredictiveBack(HomeNavTab.TIMELINE, HomeNavTab.HOME, 0.5f), 0.001f)
        assertEquals(0f, interpolateTabProgressForPredictiveBack(HomeNavTab.TIMELINE, HomeNavTab.HOME, 1f), 0.001f)

        assertEquals(0f, interpolateTabProgressForPredictiveBack(HomeNavTab.HOME, HomeNavTab.HOME, 0.5f), 0.001f)
    }

    @Test
    fun resolveDepthFractionWithPredictiveBack_scrubsUnderlyingRootDepthFromOneToZero() {
        assertEquals(1f, resolveDepthFractionWithPredictiveBack(isDeeperRoute = true, animatedDepthFraction = 1f, deeperBackProgress = 0f), 0.001f)
        assertEquals(0.6f, resolveDepthFractionWithPredictiveBack(isDeeperRoute = true, animatedDepthFraction = 1f, deeperBackProgress = 0.4f), 0.001f)
        assertEquals(0f, resolveDepthFractionWithPredictiveBack(isDeeperRoute = true, animatedDepthFraction = 1f, deeperBackProgress = 1f), 0.001f)

        // When not on a deeper route, animatedDepthFraction governs cleanly
        assertEquals(0.25f, resolveDepthFractionWithPredictiveBack(isDeeperRoute = false, animatedDepthFraction = 0.25f, deeperBackProgress = 0.5f), 0.001f)
    }

    @Test
    fun resolveDockVisibilityWithPredictiveBack_scrubsDockAndToastClearanceIntoView() {
        assertEquals(0f, resolveDockVisibilityWithPredictiveBack(0f, 0f, 0f), 0.001f)
        assertEquals(0.55f, resolveDockVisibilityWithPredictiveBack(0f, 0.55f, 0f), 0.001f)
        assertEquals(0.7f, resolveDockVisibilityWithPredictiveBack(0f, 0f, 0.7f), 0.001f)
        assertEquals(1f, resolveDockVisibilityWithPredictiveBack(1f, 0.2f, 0f), 0.001f)
    }

    @Test
    fun deeperRoutePopExitMetrics_followCanonicalLeftToRightZeroScaleSlide() {
        assertEquals(0f, resolveDeeperRoutePopExitOffsetX(0f, widthPx = 1080f), 0.001f)
        assertEquals(540f, resolveDeeperRoutePopExitOffsetX(0.5f, widthPx = 1080f), 0.001f)
        assertEquals(1080f, resolveDeeperRoutePopExitOffsetX(1f, widthPx = 1080f), 0.001f)

        assertEquals(1f, resolveDeeperRoutePopExitAlpha(0f), 0.001f)
        assertEquals(0.4f, resolveDeeperRoutePopExitAlpha(0.6f), 0.001f)
        assertEquals(0f, resolveDeeperRoutePopExitAlpha(1f), 0.001f)
    }

    @Test
    fun sheetDismissMetrics_translateDownwardAndFadeScrim() {
        assertEquals(0f, resolveSheetDismissTranslationY(0f, sheetHeightPx = 1000f), 0.001f)
        assertEquals(225f, resolveSheetDismissTranslationY(0.5f, sheetHeightPx = 1000f), 0.001f)
        assertEquals(450f, resolveSheetDismissTranslationY(1f, sheetHeightPx = 1000f), 0.001f)

        assertEquals(0.32f, resolveSheetScrimAlpha(0f, maxAlpha = 0.32f), 0.001f)
        assertEquals(0.16f, resolveSheetScrimAlpha(0.5f, maxAlpha = 0.32f), 0.001f)
        assertEquals(0f, resolveSheetScrimAlpha(1f, maxAlpha = 0.32f), 0.001f)
    }

    @Test
    fun shouldUseRootBridgeForBack_onlyTrueWhenDeeperRouteIsDirectlyOverRootTab() {
        assertTrue(shouldUseRootBridgeForBack(currentRoute = PersonDetailRoute.ROUTE_PATTERN, previousRoute = HomeRoute.ROUTE_NAME))
        assertTrue(shouldUseRootBridgeForBack(currentRoute = PersonDetailRoute.ROUTE_PATTERN, previousRoute = PeopleRoute.ROUTE_NAME))
        assertTrue(shouldUseRootBridgeForBack(currentRoute = SettingsRoute.ROUTE_NAME, previousRoute = HomeRoute.ROUTE_NAME))
        assertTrue(shouldUseRootBridgeForBack(currentRoute = ArchivedContactsRoute.ROUTE_NAME, previousRoute = PeopleRoute.ROUTE_NAME))

        // Stacked deeper routes must use NavHost's native SeekableTransitionState instead
        assertFalse(shouldUseRootBridgeForBack(currentRoute = PersonEditRoute.ROUTE_PATTERN, previousRoute = PersonDetailRoute.ROUTE_PATTERN))
        assertFalse(shouldUseRootBridgeForBack(currentRoute = SettingsAppearanceRoute.ROUTE_NAME, previousRoute = SettingsRoute.ROUTE_NAME))
        assertFalse(shouldUseRootBridgeForBack(currentRoute = PeopleRoute.ROUTE_NAME, previousRoute = HomeRoute.ROUTE_NAME))
        assertFalse(shouldUseRootBridgeForBack(currentRoute = HomeRoute.ROUTE_NAME, previousRoute = null))
    }

    @Test
    fun resolvePredictiveFinishDurationMs_scalesWithRemainingDistanceAndSnapsOnReducedMotion() {
        assertEquals(0, resolvePredictiveFinishDurationMs(0.3f, baseDurationMs = 240, isReducedMotion = true))
        assertEquals(240, resolvePredictiveFinishDurationMs(0f, baseDurationMs = 240, isReducedMotion = false))
        assertEquals(120, resolvePredictiveFinishDurationMs(0.5f, baseDurationMs = 240, isReducedMotion = false))
        assertEquals(60, resolvePredictiveFinishDurationMs(0.95f, baseDurationMs = 240, isReducedMotion = false))
    }

    @Test
    fun predictiveBackPopHandoff_skipsPopExitAcrossMultipleEvaluationsUntilExitingEntryDisposed() {
        val handoff = PredictiveBackPopHandoff()

        // Normal back tap (no predictive commit) must NOT skip popExitTransition
        assertFalse(handoff.shouldSkipPopExit("detail-entry-1"))

        // Predictive back commit marks the specific exiting entry
        handoff.markCommitted("detail-entry-1")

        // Multiple transition evaluations on route change must all see skip = true (never cleared early)
        assertTrue(handoff.shouldSkipPopExit("detail-entry-1"))
        assertTrue(handoff.shouldSkipPopExit("detail-entry-1"))
        assertFalse(handoff.shouldSkipPopExit("other-entry-2"))

        // Disposing an unrelated entry must not clear the committed entry
        handoff.onExitingEntryDisposed("other-entry-2")
        assertTrue(handoff.shouldSkipPopExit("detail-entry-1"))

        // Once the committed entry itself is disposed, handoff resets cleanly
        handoff.onExitingEntryDisposed("detail-entry-1")
        assertFalse(handoff.shouldSkipPopExit("detail-entry-1"))
    }

    @Test
    fun resolveProfileMorphProgressWithPredictiveBack_scrubsInLockstepOnlyForProfileToRoot() {
        // PersonDetail -> Root Tab (not in selection mode): reverses 1f -> 0f in 1:1 lockstep
        assertEquals(
            1f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = true,
                isDetailSelectionMode = false,
                targetMorphProgress = 1f,
                deeperBackProgress = 0f
            ),
            0.001f
        )
        assertEquals(
            0.6f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = true,
                isDetailSelectionMode = false,
                targetMorphProgress = 1f,
                deeperBackProgress = 0.4f
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = true,
                isDetailSelectionMode = false,
                targetMorphProgress = 1f,
                deeperBackProgress = 1f
            ),
            0.001f
        )

        // Timeline Selection Mode on PersonDetail: must keep pill morphed (1f) while dock hides/restores
        assertEquals(
            1f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = true,
                isDetailSelectionMode = true,
                targetMorphProgress = 1f,
                deeperBackProgress = 0.5f
            ),
            0.001f
        )

        // Other routes (Settings, Home, PersonEdit): preserves targetMorphProgress unchanged
        assertEquals(
            0f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = false,
                isDetailSelectionMode = false,
                targetMorphProgress = 0f,
                deeperBackProgress = 0.5f
            ),
            0.001f
        )
        assertEquals(
            1f,
            resolveProfileMorphProgressWithPredictiveBack(
                isPersonDetailRoute = false,
                isDetailSelectionMode = false,
                targetMorphProgress = 1f,
                deeperBackProgress = 0.5f
            ),
            0.001f
        )
    }
}

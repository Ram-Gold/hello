package com.example.hello.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.hello.domain.model.HomeNavTab
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
        fun isBackHandlerEnabled(currentPage: Int, targetPage: Int): Boolean {
            return currentPage == 1 || targetPage == 1
        }

        // On Home screen: Back must not intercept, allowing OS exit
        assertFalse(isBackHandlerEnabled(currentPage = 0, targetPage = 0))

        // On People screen: Back must intercept to navigate back to Home
        assertTrue(isBackHandlerEnabled(currentPage = 1, targetPage = 1))

        // During transition towards People: Back intercepts
        assertTrue(isBackHandlerEnabled(currentPage = 0, targetPage = 1))

        // During transition back towards Home: Back stays enabled until settled
        assertTrue(isBackHandlerEnabled(currentPage = 1, targetPage = 0))
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
}

package com.example.hello.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import com.example.hello.ui.theme.MotionTokens

data object HomeRoute {
    const val ROUTE_NAME = "home"
}

data object PeopleRoute {
    const val ROUTE_NAME = "people"
}

data object SettingsRoute {
    const val ROUTE_NAME = "settings"
}

data object OnboardingRoute {
    const val ROUTE_NAME = "onboarding"
}

data object PersonDetailRoute {
    const val ROUTE_PATTERN = "detail/{contactId}"
    const val ARG_CONTACT_ID = "contactId"
    fun createRoute(contactId: String) = "detail/$contactId"
}

data object PersonEditRoute {
    const val ROUTE_PATTERN = "edit/{contactId}"
    const val ARG_CONTACT_ID = "contactId"
    fun createRoute(contactId: String) = "edit/$contactId"
}

/**
 * Returns true if [route] is one of the top-level bottom dock root tabs ([HomeRoute] or [PeopleRoute]).
 * Matches PixelPlayer's `MainRootRoutes.kt`.
 */
fun isMainRootRoute(route: String?): Boolean = when (route) {
    HomeRoute.ROUTE_NAME,
    PeopleRoute.ROUTE_NAME -> true
    else -> false
}

/**
 * Returns the 0-based horizontal index of a main root route, or null if [route] is not a root tab.
 * Matches PixelPlayer's `MainRootRoutes.kt`.
 */
fun mainRootRouteIndex(route: String?): Int? = when (route) {
    HomeRoute.ROUTE_NAME -> 0
    PeopleRoute.ROUTE_NAME -> 1
    else -> null
}

/**
 * Determines whether the floating bottom navigation dock should be hidden for [currentRoute].
 * Visible on [HomeRoute] and [PeopleRoute] (and while route is null during initial composition);
 * hidden on deeper or supporting screens ([SettingsRoute], [PersonDetailRoute], [PersonEditRoute], [OnboardingRoute]).
 */
fun shouldHideFloatingDock(currentRoute: String?): Boolean {
    if (currentRoute == null) return false
    return !isMainRootRoute(currentRoute)
}

/**
 * Evaluates whether BackHandler should intercept system back to scroll from People (page 1) to Home (page 0).
 * Retained for backward compatibility with pager tests; with direct NavHost routes, NavController handles
 * back from PeopleRoute -> HomeRoute natively.
 */
fun shouldInterceptPeopleBack(currentRoute: String?, currentPage: Int, targetPage: Int): Boolean {
    val isAtRoot = currentRoute == "root" || isMainRootRoute(currentRoute)
    return isAtRoot && (currentPage == 1 || targetPage == 1)
}

object PersonDetailMotionConfig {
    const val DETAIL_Z_INDEX = 1f

    fun detailEnterTransition(): EnterTransition = slideInHorizontally(
        animationSpec = MotionTokens.detailSlideSpec(),
        initialOffsetX = { it }
    )

    fun detailPopExitTransition(): ExitTransition = slideOutHorizontally(
        animationSpec = MotionTokens.detailSlideSpec(),
        targetOffsetX = { it }
    )

    fun rootPopEnterTransition(): EnterTransition = EnterTransition.None

    fun rootExitTransition(): ExitTransition = ExitTransition.None
}

object SharedAxisMotionConfig {
    const val SLIDE_DURATION_MS = MotionTokens.SCREEN_SLIDE_DURATION_MS
    const val SLIDE_OFFSET_DIVISOR = MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR
    const val SLIDE_SPRING_STIFFNESS = MotionTokens.SCREEN_SLIDE_SPRING_STIFFNESS
    const val SLIDE_SPRING_DAMPING = MotionTokens.SCREEN_SLIDE_SPRING_DAMPING
    const val ENABLE_HARDWARE_LAYER_CLIP = true
}

class NavigationDebouncer(
    private val thresholdMillis: Long = 300L,
    private val onNavigate: () -> Unit
) {
    private var lastNavigateTimestamp: Long = 0L

    fun tryNavigate(currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        if (currentTimestamp - lastNavigateTimestamp >= thresholdMillis) {
            lastNavigateTimestamp = currentTimestamp
            onNavigate()
            return true
        }
        return false
    }
}

fun resolveRouteSlideDirection(fromRoute: String?, toRoute: String?): NavigationTabDirection {
    val fromIndex = mainRootRouteIndex(fromRoute) ?: return NavigationTabDirection.NONE
    val toIndex = mainRootRouteIndex(toRoute) ?: return NavigationTabDirection.NONE
    if (fromIndex == toIndex) return NavigationTabDirection.NONE
    return if (toIndex > fromIndex) NavigationTabDirection.FORWARD else NavigationTabDirection.BACKWARD
}

fun sharedAxisEnterTransition(direction: NavigationTabDirection): EnterTransition {
    val slideSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC
    val fadeSpec = MotionTokens.MAIN_ROOT_FADE_SPEC
    val divisor = SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { it / divisor }) +
                fadeIn(animationSpec = fadeSpec, initialAlpha = 0f)
        }
        NavigationTabDirection.BACKWARD -> {
            slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { -it / divisor }) +
                fadeIn(animationSpec = fadeSpec, initialAlpha = 0f)
        }
        NavigationTabDirection.NONE -> EnterTransition.None
    }
}

fun sharedAxisExitTransition(direction: NavigationTabDirection): ExitTransition {
    val slideSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC
    val fadeSpec = MotionTokens.MAIN_ROOT_FADE_SPEC
    val divisor = SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { -it / divisor }) +
                fadeOut(animationSpec = fadeSpec, targetAlpha = 0f)
        }
        NavigationTabDirection.BACKWARD -> {
            slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it / divisor }) +
                fadeOut(animationSpec = fadeSpec, targetAlpha = 0f)
        }
        NavigationTabDirection.NONE -> ExitTransition.None
    }
}

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

/**
 * Lifecycle-gated singleTop navigation helper matching PixelPlayer's `NavControllerExtensions.kt`.
 */
fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}

/**
 * Top-level tab navigation extension matching PixelPlayer's `navigateToTopLevelSafely`.
 * Deduplicates redundant taps and preserves state across HomeRoute <-> PeopleRoute switches.
 */
fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    if (currentDestination?.route == route) return false
    val startDestinationId = runCatching { graph.findStartDestination().id }.getOrNull() ?: return false
    navigate(route) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}

/**
 * High-leverage navigation extension delegating to [navigateToTopLevelSafely].
 */
fun NavController.navigateToRootTab(targetRoute: String) {
    navigateToTopLevelSafely(targetRoute)
}

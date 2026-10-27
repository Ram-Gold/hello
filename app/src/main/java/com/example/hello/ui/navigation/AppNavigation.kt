package com.example.hello.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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

/**
 * Evaluates whether BackHandler should intercept system back to scroll from People (page 1) to Home (page 0).
 * MUST only intercept when the user is actively on the "root" screen so it never delays exiting PersonDetailScreen.
 */
fun shouldInterceptPeopleBack(currentRoute: String?, currentPage: Int, targetPage: Int): Boolean {
    val isAtRoot = currentRoute == "root"
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

data object PersonEditRoute {
    const val ROUTE_PATTERN = "edit/{contactId}"
    const val ARG_CONTACT_ID = "contactId"
    fun createRoute(contactId: String) = "edit/$contactId"
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
    if (fromRoute == toRoute || fromRoute == null || toRoute == null) {
        return NavigationTabDirection.NONE
    }
    val isFromRoot = fromRoute == HomeRoute.ROUTE_NAME || fromRoute == PeopleRoute.ROUTE_NAME
    val isToRoot = toRoute == HomeRoute.ROUTE_NAME || toRoute == PeopleRoute.ROUTE_NAME
    if (!isFromRoot || !isToRoot) {
        return NavigationTabDirection.NONE
    }
    return if (fromRoute == HomeRoute.ROUTE_NAME && toRoute == PeopleRoute.ROUTE_NAME) {
        NavigationTabDirection.FORWARD
    } else if (fromRoute == PeopleRoute.ROUTE_NAME && toRoute == HomeRoute.ROUTE_NAME) {
        NavigationTabDirection.BACKWARD
    } else {
        NavigationTabDirection.NONE
    }
}

fun sharedAxisEnterTransition(direction: NavigationTabDirection): EnterTransition {
    val slideSpec = MotionTokens.screenSlideSpec()
    val fadeSpec = MotionTokens.screenFadeSpec()
    val divisor = SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            // Home -> People: People enters from subtle 25% right offset to center
            slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { it / divisor }) +
                fadeIn(animationSpec = fadeSpec, initialAlpha = 0f)
        }
        NavigationTabDirection.BACKWARD -> {
            // People -> Home: Home enters from subtle 25% left offset to center
            slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { -it / divisor }) +
                fadeIn(animationSpec = fadeSpec, initialAlpha = 0f)
        }
        NavigationTabDirection.NONE -> EnterTransition.None
    }
}

fun sharedAxisExitTransition(direction: NavigationTabDirection): ExitTransition {
    val slideSpec = MotionTokens.screenSlideSpec()
    val fadeSpec = MotionTokens.screenFadeSpec()
    val divisor = SharedAxisMotionConfig.SLIDE_OFFSET_DIVISOR

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            // Home -> People: Home exits from center to subtle 25% left offset
            slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { -it / divisor }) +
                fadeOut(animationSpec = fadeSpec, targetAlpha = 0f)
        }
        NavigationTabDirection.BACKWARD -> {
            // People -> Home: People exits from center to subtle 25% right offset
            slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it / divisor }) +
                fadeOut(animationSpec = fadeSpec, targetAlpha = 0f)
        }
        NavigationTabDirection.NONE -> ExitTransition.None
    }
}

/**
 * High-leverage navigation extension ensuring debounced, singleTop, and state-preserving navigation.
 */
fun NavController.navigateToRootTab(targetRoute: String) {
    if (currentDestination?.route == targetRoute) {
        return // deduplicate redundant tap
    }
    navigate(targetRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

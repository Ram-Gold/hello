package com.example.hello.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

data object HomeRoute {
    const val ROUTE_NAME = "home"
}

data object PeopleRoute {
    const val ROUTE_NAME = "people"
}

object SharedAxisMotionConfig {
    const val SLIDE_DURATION_MS = 120
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
    val tweenSpec = tween<IntOffset>(
        durationMillis = SharedAxisMotionConfig.SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    val fadeSpec = tween<Float>(durationMillis = SharedAxisMotionConfig.SLIDE_DURATION_MS)

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            slideInHorizontally(animationSpec = tweenSpec, initialOffsetX = { it }) +
                fadeIn(animationSpec = fadeSpec)
        }
        NavigationTabDirection.BACKWARD -> {
            slideInHorizontally(animationSpec = tweenSpec, initialOffsetX = { -it }) +
                fadeIn(animationSpec = fadeSpec)
        }
        NavigationTabDirection.NONE -> EnterTransition.None
    }
}

fun sharedAxisExitTransition(direction: NavigationTabDirection): ExitTransition {
    val tweenSpec = tween<IntOffset>(
        durationMillis = SharedAxisMotionConfig.SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    val fadeSpec = tween<Float>(durationMillis = SharedAxisMotionConfig.SLIDE_DURATION_MS)

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            slideOutHorizontally(animationSpec = tweenSpec, targetOffsetX = { -it }) +
                fadeOut(animationSpec = fadeSpec)
        }
        NavigationTabDirection.BACKWARD -> {
            slideOutHorizontally(animationSpec = tweenSpec, targetOffsetX = { it }) +
                fadeOut(animationSpec = fadeSpec)
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

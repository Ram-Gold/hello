package com.example.hello.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import com.example.hello.domain.model.HomeNavTab

object NavigationTransitionConfig {
    const val SCREEN_TRANSITION_DURATION_MS = 50
    const val USE_CONTAINER_SLIDE = false
}

enum class NavigationTabDirection {
    FORWARD,  // Target tab is to the right in dock order (enters from right edge to center)
    BACKWARD, // Target tab is to the left in dock order (enters from left edge to center)
    NONE      // Same tab re-selection (no-op)
}

fun resolveTabSlideDirection(from: HomeNavTab, to: HomeNavTab): NavigationTabDirection {
    return when {
        from.ordinal < to.ordinal -> NavigationTabDirection.FORWARD
        from.ordinal > to.ordinal -> NavigationTabDirection.BACKWARD
        else -> NavigationTabDirection.NONE
    }
}

// Snappy spring physics complying with AGENTS.md §3 and ultra-fast capture speed
private val TabSlideSpringSpec = spring<IntOffset>(
    dampingRatio = 0.82f,
    stiffness = 1400f
)

private val TabFadeSpringSpec = spring<Float>(
    dampingRatio = 0.9f,
    stiffness = 1500f
)

fun AnimatedContentTransitionScope<HomeNavTab>.tabSlideTransitionSpec(): ContentTransform {
    val direction = resolveTabSlideDirection(initialState, targetState)

    if (direction == NavigationTabDirection.NONE) {
        return EnterTransition.None togetherWith ExitTransition.None
    }

    if (!NavigationTransitionConfig.USE_CONTAINER_SLIDE) {
        return createSnappyCrossfade()
    }

    return when (direction) {
        NavigationTabDirection.FORWARD -> {
            // Home -> People: Target enters from right moving left; initial exits to left
            createDirectionalSlide(AnimatedContentTransitionScope.SlideDirection.Left)
        }
        NavigationTabDirection.BACKWARD -> {
            // People -> Home: Target enters from left moving right; initial exits to right
            createDirectionalSlide(AnimatedContentTransitionScope.SlideDirection.Right)
        }
        NavigationTabDirection.NONE -> {
            // No-op for same-tab re-selection: zero unnecessary recomposition/fade
            EnterTransition.None togetherWith ExitTransition.None
        }
    }
}

private fun createSnappyCrossfade(): ContentTransform {
    val fadeSpec = tween<Float>(
        durationMillis = NavigationTransitionConfig.SCREEN_TRANSITION_DURATION_MS
    )
    return fadeIn(animationSpec = fadeSpec).togetherWith(
        fadeOut(animationSpec = fadeSpec)
    )
}

private fun AnimatedContentTransitionScope<HomeNavTab>.createDirectionalSlide(
    slideDirection: AnimatedContentTransitionScope.SlideDirection
): ContentTransform {
    return (slideIntoContainer(
        towards = slideDirection,
        animationSpec = TabSlideSpringSpec
    ) + fadeIn(
        animationSpec = TabFadeSpringSpec,
        initialAlpha = 0.85f // Gentle softening rather than stark 0f fade
    )).togetherWith(
        slideOutOfContainer(
            towards = slideDirection,
            animationSpec = TabSlideSpringSpec
        ) + fadeOut(
            animationSpec = TabFadeSpringSpec,
            targetAlpha = 0.85f
        )
    )
}

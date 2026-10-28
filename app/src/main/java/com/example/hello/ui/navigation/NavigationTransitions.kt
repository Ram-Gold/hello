package com.example.hello.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens

object NavigationTransitionConfig {
    const val SCREEN_TRANSITION_DURATION_MS = MotionTokens.BOTTOM_NAV_TRANSITION_DURATION
    const val MAIN_ROOT_SLIDE_OFFSET_FRACTION = 0.5f
    const val USE_CONTAINER_SLIDE = true
}

// Official Material 3 Emphasized curve (used by contemporary Pixel devices & PixelPlayer)
val M3EmphasizedEasing = MotionTokens.M3EmphasizedEasing
val M3Easing = M3EmphasizedEasing
val MotionEasing = M3EmphasizedEasing
val FadeEasing = MotionTokens.BottomNavEasing

const val BOTTOM_NAV_TRANSITION_DURATION = MotionTokens.BOTTOM_NAV_TRANSITION_DURATION
const val TRANSITION_MS = MotionTokens.TRANSITION_MS
const val MotionDuration = TRANSITION_MS
const val AOSP_TRANSITION_DURATION = MotionTokens.AOSP_TRANSITION_DURATION
const val TRANSITION_DURATION = MotionTokens.TRANSITION_DURATION

private val EmphasizedDecelerateEasing = MotionTokens.EmphasizedDecelerateEasing
private val EmphasizedAccelerateEasing = MotionTokens.EmphasizedAccelerateEasing

// Pre-allocated specs for AOSP 350ms shared-axis and 450ms MD3 Expressive push/pop transitions
private val AOSP_SLIDE_SPEC = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = AOSP_TRANSITION_DURATION,
    easing = M3EmphasizedEasing
)
private val AOSP_FADE_SPEC = tween<Float>(
    durationMillis = AOSP_TRANSITION_DURATION,
    easing = M3EmphasizedEasing
)
private val AOSP_POP_EXIT_SLIDE_SPEC = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = AOSP_TRANSITION_DURATION,
    easing = Easing { f -> f * f * f }
)

private val PUSH_DECELERATE_SLIDE_SPEC = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = TRANSITION_DURATION,
    easing = EmphasizedDecelerateEasing
)
private val PUSH_DECELERATE_FADE_SPEC = tween<Float>(
    durationMillis = TRANSITION_DURATION,
    easing = EmphasizedDecelerateEasing
)
private val PUSH_DECELERATE_HALF_FADE_SPEC = tween<Float>(
    durationMillis = TRANSITION_DURATION / 2,
    easing = EmphasizedDecelerateEasing
)
private val PUSH_ACCELERATE_SLIDE_SPEC = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = TRANSITION_DURATION,
    easing = EmphasizedAccelerateEasing
)
private val PUSH_ACCELERATE_FADE_SPEC = tween<Float>(
    durationMillis = TRANSITION_DURATION,
    easing = EmphasizedAccelerateEasing
)
private val PUSH_ACCELERATE_HALF_FADE_SPEC = tween<Float>(
    durationMillis = TRANSITION_DURATION / 2,
    easing = EmphasizedAccelerateEasing
)

enum class NavigationTabDirection {
    FORWARD,  // Target tab is to the right in dock order (enters from right edge to center)
    BACKWARD, // Target tab is to the left in dock order (enters from left edge to center)
    NONE      // Same tab re-selection or non-root route
}

fun resolveTabSlideDirection(from: HomeNavTab, to: HomeNavTab): NavigationTabDirection {
    return when {
        from.ordinal < to.ordinal -> NavigationTabDirection.FORWARD
        from.ordinal > to.ordinal -> NavigationTabDirection.BACKWARD
        else -> NavigationTabDirection.NONE
    }
}

private val TabSlideSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC
private val TabFadeSpec = MotionTokens.MAIN_ROOT_FADE_SPEC

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
            createDirectionalSlide(AnimatedContentTransitionScope.SlideDirection.Left)
        }
        NavigationTabDirection.BACKWARD -> {
            createDirectionalSlide(AnimatedContentTransitionScope.SlideDirection.Right)
        }
        NavigationTabDirection.NONE -> {
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
    val divisor = MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR
    return (slideIntoContainer(
        towards = slideDirection,
        animationSpec = TabSlideSpec,
        initialOffset = { it / divisor }
    ) + fadeIn(
        animationSpec = TabFadeSpec,
        initialAlpha = 0f
    )).togetherWith(
        slideOutOfContainer(
            towards = slideDirection,
            animationSpec = TabSlideSpec,
            targetOffset = { it / divisor }
        ) + fadeOut(
            animationSpec = TabFadeSpec,
            targetAlpha = 0f
        )
    )
}

/**
 * PixelPlayer Root Tab Transitions (HomeRoute <-> PeopleRoute):
 * - 380ms slide duration with 50% width offset (`±(it * 0.5f).toInt()`)
 * - 190ms (duration / 2) fadeIn / fadeOut
 * - Material 3 Expressive Emphasized easing `CubicBezierEasing(0.2f, 0f, 0f, 1f)`
 * - Falls back to [fallback] when transitioning to/from non-root destinations
 */
fun mainRootEnterTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: EnterTransition = enterTransition()
): EnterTransition = when (resolveRouteSlideDirection(fromRoute, toRoute)) {
    NavigationTabDirection.FORWARD -> {
        slideInHorizontally(
            animationSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC,
            initialOffsetX = { (it * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION).toInt() }
        ) + fadeIn(animationSpec = MotionTokens.MAIN_ROOT_FADE_SPEC)
    }
    NavigationTabDirection.BACKWARD -> {
        slideInHorizontally(
            animationSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC,
            initialOffsetX = { -(it * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION).toInt() }
        ) + fadeIn(animationSpec = MotionTokens.MAIN_ROOT_FADE_SPEC)
    }
    NavigationTabDirection.NONE -> fallback
}

fun mainRootExitTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: ExitTransition = exitTransition()
): ExitTransition = when (resolveRouteSlideDirection(fromRoute, toRoute)) {
    NavigationTabDirection.FORWARD -> {
        slideOutHorizontally(
            animationSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC,
            targetOffsetX = { -(it * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION).toInt() }
        ) + fadeOut(animationSpec = MotionTokens.MAIN_ROOT_FADE_SPEC)
    }
    NavigationTabDirection.BACKWARD -> {
        slideOutHorizontally(
            animationSpec = MotionTokens.MAIN_ROOT_TRANSITION_SPEC,
            targetOffsetX = { (it * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION).toInt() }
        ) + fadeOut(animationSpec = MotionTokens.MAIN_ROOT_FADE_SPEC)
    }
    NavigationTabDirection.NONE -> fallback
}

// =========================================================================
// PixelPlayer AOSP 350ms Shared-Axis Transitions (NavHost Defaults)
// =========================================================================

// Equivalent to sud_slide_next_in
fun aospSharedAxisEnter(
    durationMillis: Int = AOSP_TRANSITION_DURATION,
    offsetDivisor: Int = 3
): EnterTransition {
    val slideSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_SLIDE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    val fadeSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_FADE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    return slideInHorizontally(
        initialOffsetX = { it / offsetDivisor },
        animationSpec = slideSpec
    ) + fadeIn(
        animationSpec = fadeSpec
    )
}

// Equivalent to sud_slide_next_out
fun aospSharedAxisExit(
    durationMillis: Int = AOSP_TRANSITION_DURATION,
    offsetDivisor: Int = 3
): ExitTransition {
    val slideSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_SLIDE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    val scaleSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_FADE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    return slideOutHorizontally(
        targetOffsetX = { -it / offsetDivisor },
        animationSpec = slideSpec
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = scaleSpec
    )
}

// Equivalent to sud_slide_back_in
fun aospSharedAxisPopEnter(
    durationMillis: Int = AOSP_TRANSITION_DURATION,
    offsetDivisor: Int = 3
): EnterTransition {
    val slideSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_SLIDE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    val fadeSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_FADE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    return slideInHorizontally(
        initialOffsetX = { -it / offsetDivisor },
        animationSpec = slideSpec
    ) + fadeIn(
        animationSpec = fadeSpec
    )
}

// Equivalent to sud_slide_back_out
@Suppress("UNUSED_PARAMETER")
fun aospSharedAxisPopExit(
    durationMillis: Int = AOSP_TRANSITION_DURATION,
    offsetDivisor: Int = 3
): ExitTransition {
    val slideSpec = if (durationMillis == AOSP_TRANSITION_DURATION) {
        AOSP_POP_EXIT_SLIDE_SPEC
    } else {
        tween(durationMillis = durationMillis, easing = Easing { f -> f * f * f })
    }
    val scaleSpec = if (durationMillis == AOSP_TRANSITION_DURATION) AOSP_FADE_SPEC else tween(durationMillis = durationMillis, easing = M3EmphasizedEasing)
    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = slideSpec
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = scaleSpec
    )
}

// =========================================================================
// PixelPlayer 450ms MD3 Expressive Push/Pop Transitions (Root Tab Fallbacks)
// =========================================================================

// Push: Enter from Right — slides in 50% of screen width + slight scale up
fun enterTransition(): EnterTransition = slideInHorizontally(
    animationSpec = PUSH_DECELERATE_SLIDE_SPEC,
    initialOffsetX = { (it * 0.5f).toInt() }
) + scaleIn(
    animationSpec = PUSH_DECELERATE_FADE_SPEC,
    initialScale = 0.92f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeIn(
    animationSpec = PUSH_ACCELERATE_FADE_SPEC
)

// Push: Exit to Left — recedes 25% (parallax)
fun exitTransition(): ExitTransition = slideOutHorizontally(
    animationSpec = PUSH_ACCELERATE_SLIDE_SPEC,
    targetOffsetX = { -(it * 0.25f).toInt() }
) + fadeOut(
    animationSpec = PUSH_ACCELERATE_HALF_FADE_SPEC
)

// Pop: Enter from Left — parallax slide-in 25% + subtle scale up
fun popEnterTransition(): EnterTransition = slideInHorizontally(
    animationSpec = PUSH_DECELERATE_SLIDE_SPEC,
    initialOffsetX = { -(it * 0.25f).toInt() }
) + scaleIn(
    animationSpec = PUSH_DECELERATE_FADE_SPEC,
    initialScale = 0.95f
) + fadeIn(
    animationSpec = PUSH_DECELERATE_HALF_FADE_SPEC
)

// Pop: Exit to Right — slides out 50% + slight scale down
fun popExitTransition(): ExitTransition = slideOutHorizontally(
    animationSpec = PUSH_ACCELERATE_SLIDE_SPEC,
    targetOffsetX = { (it * 0.5f).toInt() }
) + scaleOut(
    animationSpec = PUSH_ACCELERATE_FADE_SPEC,
    targetScale = 0.92f,
    transformOrigin = TransformOrigin(0.5f, 0.5f)
) + fadeOut(
    animationSpec = PUSH_ACCELERATE_HALF_FADE_SPEC
)

// Backward-compatible aliases delegating to AOSP Shared-Axis transitions
fun sharedAxisEnter(): EnterTransition = aospSharedAxisEnter()
fun sharedAxisExit(): ExitTransition = aospSharedAxisExit()
fun sharedAxisPopEnter(): EnterTransition = aospSharedAxisPopEnter()
fun sharedAxisPopExit(): ExitTransition = aospSharedAxisPopExit()

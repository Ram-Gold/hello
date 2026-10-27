package com.example.hello.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Centralized Animation Vocabulary & Motion Tokens for "Hello".
 *
 * Defines synchronized, tactile motion for the floating bottom dock indicator pill,
 * directional horizontal screen transitions (Home <-> People), and interactive shortcut badges.
 */
object MotionTokens {
    // 1. Floating Dock Sliding Indicator Pill
    // Critically-damped spring (1.0f) with snappy 500f stiffness (~180ms perceptual duration)
    // for velocity-preserving interruptible translation with zero overshoot/bounce.
    const val PILL_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val PILL_SPRING_STIFFNESS = 500f

    // 2. Directional Shared-Axis Screen Slide (Home <-> People)
    // Snappy 160ms FastOutSlowIn transition provides instant visual response (<16ms) with zero jank
    const val SCREEN_SLIDE_DURATION_MS = 160
    const val SCREEN_SLIDE_OFFSET_DIVISOR = 4

    // Fallback spring constants for screen slide if spring physics are chosen
    const val SCREEN_SLIDE_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val SCREEN_SLIDE_SPRING_STIFFNESS = 500f
    const val SCREEN_FADE_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val SCREEN_FADE_SPRING_STIFFNESS = 500f
    const val SCREEN_FADE_MIN_ALPHA = 0.0f

    // 3. Tab Label / Icon Color Crossfade
    const val TAB_COLOR_TWEEN_DURATION_MS = 160

    // 4. Interactive Shortcut Badge Press Feedback ("X contacts ->", "<- Home")
    const val BADGE_PRESS_SPRING_DAMPING = Spring.DampingRatioMediumBouncy
    const val BADGE_PRESS_SPRING_STIFFNESS = Spring.StiffnessLow
    const val BADGE_PRESSED_SCALE = 0.88f

    // 5. Smooth Tab & Screen Transition (250ms FastOutSlowIn)
    const val TAB_TRANSITION_DURATION_MS = 250

    // 6. Person Detail Screen Transitions (Full width slide, layered above)
    const val DETAIL_SLIDE_DURATION_MS = 250

    fun tabTransitionTween(): TweenSpec<Float> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun tabTransitionSizeTween(): TweenSpec<IntSize> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun tabTransitionOffsetTween(): TweenSpec<IntOffset> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun detailSlideSpec(): TweenSpec<IntOffset> = tween(
        durationMillis = DETAIL_SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun dockPillProgressSpring(): SpringSpec<Float> = spring(
        dampingRatio = PILL_SPRING_DAMPING,
        stiffness = PILL_SPRING_STIFFNESS
    )

    fun dockPillSpring(): SpringSpec<Dp> = spring(
        dampingRatio = PILL_SPRING_DAMPING,
        stiffness = PILL_SPRING_STIFFNESS,
        visibilityThreshold = Dp.VisibilityThreshold
    )

    fun screenSlideSpec(): TweenSpec<IntOffset> = tween(
        durationMillis = SCREEN_SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun screenFadeSpec(): TweenSpec<Float> = tween(
        durationMillis = SCREEN_SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    fun screenSlideSpring(): SpringSpec<IntOffset> = spring(
        dampingRatio = SCREEN_SLIDE_SPRING_DAMPING,
        stiffness = SCREEN_SLIDE_SPRING_STIFFNESS,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    fun screenFadeSpring(): SpringSpec<Float> = spring(
        dampingRatio = SCREEN_FADE_SPRING_DAMPING,
        stiffness = SCREEN_FADE_SPRING_STIFFNESS
    )

    fun badgePressSpring(): SpringSpec<Float> = spring(
        dampingRatio = BADGE_PRESS_SPRING_DAMPING,
        stiffness = BADGE_PRESS_SPRING_STIFFNESS
    )
}

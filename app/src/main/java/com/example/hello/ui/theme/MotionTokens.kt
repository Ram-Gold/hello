package com.example.hello.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
 * Centralized Animation Vocabulary & Motion Tokens for "Hello", aligned with PixelPlayer's
 * Material 3 Expressive motion specs and optimized with pre-allocated static TweenSpecs
 * for zero-allocation execution on low-end devices.
 */
object MotionTokens {
    // 1. Floating Dock Sliding Indicator Pill fallback spring tokens
    const val PILL_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val PILL_SPRING_STIFFNESS = 500f

    // 2. PixelPlayer Bottom-Nav Root Transition (Home <-> Relationship Terminology tab)
    // Base duration for bottom-nav switches at 1x — at 0.5x system scale = ~190ms.
    const val BOTTOM_NAV_TRANSITION_DURATION = 380
    const val BOTTOM_NAV_FADE_DURATION = BOTTOM_NAV_TRANSITION_DURATION / 2 // 190ms
    const val SCREEN_SLIDE_DURATION_MS = BOTTOM_NAV_TRANSITION_DURATION
    const val SCREEN_SLIDE_OFFSET_DIVISOR = 2 // 50% horizontal offset (it * 0.5f)

    // Fallback spring constants for screen slide if spring physics are chosen
    const val SCREEN_SLIDE_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val SCREEN_SLIDE_SPRING_STIFFNESS = 500f
    const val SCREEN_FADE_SPRING_DAMPING = Spring.DampingRatioNoBouncy
    const val SCREEN_FADE_SPRING_STIFFNESS = 500f
    const val SCREEN_FADE_MIN_ALPHA = 0.0f

    // 3. Tab Label / Icon Color Crossfade & Bar Hide
    const val TAB_COLOR_TWEEN_DURATION_MS = 160
    const val NAV_BAR_HIDE_DURATION_MS = 220

    // 4. Interactive Shortcut Badge Press Feedback ("X contacts ->", "<- Home")
    const val BADGE_PRESS_SPRING_DAMPING = Spring.DampingRatioMediumBouncy
    const val BADGE_PRESS_SPRING_STIFFNESS = Spring.StiffnessLow
    const val BADGE_PRESSED_SCALE = 0.88f

    // 5. Synchronized Tab & Root Screen Transition Duration (380ms MD3 Expressive)
    const val TAB_TRANSITION_DURATION_MS = BOTTOM_NAV_TRANSITION_DURATION
    const val TRANSITION_MS = BOTTOM_NAV_TRANSITION_DURATION

    // 6. Hierarchical Push/Pop Durations (matching PixelPlayer Transitions.kt)
    const val AOSP_TRANSITION_DURATION = 350
    const val TRANSITION_DURATION = 450
    const val DETAIL_SLIDE_DURATION_MS = AOSP_TRANSITION_DURATION

    // Material 3 Expressive – Emphasized easing (matches Material Motion spec & PixelPlayer)
    // cubic-bezier(0.2, 0.0, 0.0, 1.0) — fast responsive start, long smooth ease-out settle
    val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val BottomNavEasing = M3EmphasizedEasing
    val M3Easing = M3EmphasizedEasing
    val EaseInOut = M3EmphasizedEasing
    val NavBarHideEasing = LinearOutSlowInEasing

    // Decelerate for elements entering the screen — mirror of EmphasizedAccelerateEasing
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.2f, 0.85f, 0.7f, 1f)

    // Accelerate for elements leaving the screen
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Emil Kowalski Smooth Deceleration curve
    val EaseOutDecelerate = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

    // Pre-allocated static TweenSpec instances to prevent per-navigation allocations on low-end hardware
    val MAIN_ROOT_TRANSITION_SPEC: TweenSpec<IntOffset> = tween(
        durationMillis = BOTTOM_NAV_TRANSITION_DURATION,
        easing = BottomNavEasing
    )

    val MAIN_ROOT_FADE_SPEC: TweenSpec<Float> = tween(
        durationMillis = BOTTOM_NAV_FADE_DURATION,
        easing = BottomNavEasing
    )

    val TAB_TRANSITION_TWEEN: TweenSpec<Float> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = M3EmphasizedEasing
    )

    val TAB_TRANSITION_SIZE_TWEEN: TweenSpec<IntSize> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = M3EmphasizedEasing
    )

    val TAB_TRANSITION_OFFSET_TWEEN: TweenSpec<IntOffset> = tween(
        durationMillis = TAB_TRANSITION_DURATION_MS,
        easing = M3EmphasizedEasing
    )

    fun tabTransitionTween(): TweenSpec<Float> = TAB_TRANSITION_TWEEN

    fun tabTransitionSizeTween(): TweenSpec<IntSize> = TAB_TRANSITION_SIZE_TWEEN

    fun tabTransitionOffsetTween(): TweenSpec<IntOffset> = TAB_TRANSITION_OFFSET_TWEEN

    fun detailSlideSpec(): TweenSpec<IntOffset> = tween(
        durationMillis = DETAIL_SLIDE_DURATION_MS,
        easing = M3EmphasizedEasing
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

    fun screenSlideSpec(): TweenSpec<IntOffset> = MAIN_ROOT_TRANSITION_SPEC

    fun screenFadeSpec(): TweenSpec<Float> = MAIN_ROOT_FADE_SPEC

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

package com.example.hello.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * Motion specifications for contact departure animations across Hello CRM.
 *
 * Implements Emil Kowalski's guidelines:
 * - Decelerating ease-out departure curves (front-loaded velocity).
 * - Total duration bounded under 250ms for instant perceived responsiveness.
 * - Non-zero scale boundaries (0.92x dissolution rather than collapsing to 0).
 * - Graceful zero-duration degradation when reduced motion is requested.
 */
object ContactExitMotionTokens {

    // 1. Archive Spatial Tuck Exit Tokens
    const val ARCHIVE_EXIT_FADE_DURATION_MS: Int = 180
    const val ARCHIVE_EXIT_SLIDE_FRACTION_X: Float = -0.20f
    const val ARCHIVE_EXIT_SLIDE_FRACTION_Y: Float = -0.15f
    val ArchiveExitEasing: Easing = FastOutSlowInEasing

    // 2. Delete Physical Dissolution Exit Tokens
    const val DELETE_EXIT_FADE_DURATION_MS: Int = 150
    const val DELETE_EXIT_TARGET_SCALE: Float = 0.92f
    val DeleteExitEasing: Easing = LinearOutSlowInEasing

    // 3. Restore Exit Tokens (from Archive Screen)
    const val RESTORE_EXIT_FADE_DURATION_MS: Int = 160
    const val RESTORE_EXIT_SLIDE_FRACTION_X: Float = 0.25f
    val RestoreExitEasing: Easing = FastOutSlowInEasing

    // 4. Spring Physics for List Reflow & Row Collapse
    const val PLACEMENT_SPRING_DAMPING: Float = 0.82f
    const val PLACEMENT_SPRING_STIFFNESS: Float = Spring.StiffnessMediumLow

    fun <T> placementSpringSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = PLACEMENT_SPRING_DAMPING,
        stiffness = PLACEMENT_SPRING_STIFFNESS
    )

    fun archiveExitTransition(isReducedMotion: Boolean = false): ExitTransition {
        if (isReducedMotion) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = ARCHIVE_EXIT_FADE_DURATION_MS,
                easing = ArchiveExitEasing
            ),
            targetOffsetX = { (it * ARCHIVE_EXIT_SLIDE_FRACTION_X).toInt() }
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = ARCHIVE_EXIT_FADE_DURATION_MS,
                easing = ArchiveExitEasing
            ),
            targetOffsetY = { (it * ARCHIVE_EXIT_SLIDE_FRACTION_Y).toInt() }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ARCHIVE_EXIT_FADE_DURATION_MS,
                easing = ArchiveExitEasing
            )
        ) + shrinkVertically(
            animationSpec = placementSpringSpec()
        )
    }

    fun deleteExitTransition(isReducedMotion: Boolean = false): ExitTransition {
        if (isReducedMotion) return ExitTransition.None
        return scaleOut(
            animationSpec = tween(
                durationMillis = DELETE_EXIT_FADE_DURATION_MS,
                easing = DeleteExitEasing
            ),
            targetScale = DELETE_EXIT_TARGET_SCALE
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DELETE_EXIT_FADE_DURATION_MS,
                easing = DeleteExitEasing
            )
        ) + shrinkVertically(
            animationSpec = placementSpringSpec()
        )
    }

    fun restoreExitTransition(isReducedMotion: Boolean = false): ExitTransition {
        if (isReducedMotion) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = RESTORE_EXIT_FADE_DURATION_MS,
                easing = RestoreExitEasing
            ),
            targetOffsetX = { (it * RESTORE_EXIT_SLIDE_FRACTION_X).toInt() }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = RESTORE_EXIT_FADE_DURATION_MS,
                easing = RestoreExitEasing
            )
        ) + shrinkVertically(
            animationSpec = placementSpringSpec()
        )
    }
}


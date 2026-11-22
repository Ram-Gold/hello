package com.example.hello.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Normalizes a system predictive back [rawProgress] into `[0f, 1f]`, or returns `0f` when
 * [isReducedMotion] is enabled per ADR-0007 (strict 0ms cut).
 */
fun normalizePredictiveBackProgress(
    rawProgress: Float,
    isReducedMotion: Boolean
): Float {
    if (isReducedMotion) return 0f
    return rawProgress.coerceIn(0f, 1f)
}

/**
 * Interpolates the continuous root-tab progress (`0f = Home`, `1f = People`, `2f = Timeline`)
 * from [currentTab] toward [targetTab] during a predictive back gesture.
 */
fun interpolateTabProgressForPredictiveBack(
    currentTab: HomeNavTab,
    targetTab: HomeNavTab = HomeNavTab.HOME,
    backProgress: Float
): Float {
    val fromIndex = currentTab.ordinal.toFloat()
    val toIndex = targetTab.ordinal.toFloat()
    val clamped = backProgress.coerceIn(0f, 1f)
    return fromIndex + (toIndex - fromIndex) * clamped
}

/**
 * Bridges `WarmRootTabsHost` depth parallax (`1f` = occluded behind deeper route, `0f` = at rest)
 * with live predictive back progress when popping a deeper route back to a root tab.
 */
fun resolveDepthFractionWithPredictiveBack(
    isDeeperRoute: Boolean,
    animatedDepthFraction: Float,
    deeperBackProgress: Float
): Float {
    if (!isDeeperRoute) return animatedDepthFraction.coerceIn(0f, 1f)
    val clampedBack = deeperBackProgress.coerceIn(0f, 1f)
    return (animatedDepthFraction * (1f - clampedBack)).coerceIn(0f, 1f)
}

/**
 * Computes effective Floating Bottom Dock visibility (`0f..1f`) by combining the normal animated
 * visibility state with live predictive back progress from deeper screens or selection mode.
 */
fun resolveDockVisibilityWithPredictiveBack(
    animatedDockVisibility: Float,
    deeperBackProgress: Float,
    selectionBackProgress: Float
): Float {
    return maxOf(
        animatedDockVisibility,
        deeperBackProgress.coerceIn(0f, 1f),
        selectionBackProgress.coerceIn(0f, 1f)
    ).coerceIn(0f, 1f)
}

/**
 * Computes effective Floating Bottom Dock -> Profile Encounter Pill morph progress (`0f..1f`)
 * during predictive back from [PersonDetailRoute] to a root tab (ADR-0034).
 *
 * When swiping back from Contact Profile (outside Timeline Selection Mode), reverses the morph
 * (`1f -> 0f`) in 1:1 lockstep with [deeperBackProgress] while keeping the dock vertically stationary.
 */
fun resolveProfileMorphProgressWithPredictiveBack(
    isPersonDetailRoute: Boolean,
    isDetailSelectionMode: Boolean,
    targetMorphProgress: Float,
    deeperBackProgress: Float
): Float {
    val clampedTarget = targetMorphProgress.coerceIn(0f, 1f)
    if (!isPersonDetailRoute || isDetailSelectionMode) return clampedTarget
    val clampedBack = deeperBackProgress.coerceIn(0f, 1f)
    return (clampedTarget * (1f - clampedBack)).coerceIn(0f, 1f)
}

/**
 * Computes canonical left-to-right hierarchical horizontal translation (`+X`) for a deeper
 * route popping off-screen during predictive back.
 */
fun resolveDeeperRoutePopExitOffsetX(
    progress: Float,
    widthPx: Float
): Float = progress.coerceIn(0f, 1f) * widthPx

/**
 * Computes outgoing alpha for a deeper route popping off-screen during predictive back.
 */
fun resolveDeeperRoutePopExitAlpha(progress: Float): Float =
    (1f - progress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

/**
 * Computes downward vertical translation for bottom-anchored sheets during predictive dismiss.
 */
fun resolveSheetDismissTranslationY(
    progress: Float,
    sheetHeightPx: Float
): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val travelPx = (sheetHeightPx * 0.45f).coerceAtLeast(160f)
    return clamped * travelPx
}

/**
 * Computes backdrop scrim alpha for bottom-anchored sheets during predictive dismiss.
 */
fun resolveSheetScrimAlpha(
    progress: Float,
    maxAlpha: Float
): Float = (maxAlpha * (1f - progress.coerceIn(0f, 1f))).coerceIn(0f, maxAlpha)

/**
 * Computes remaining alpha for contextual selection overlays during predictive back exit.
 */
fun resolveSelectionExitAlpha(progress: Float): Float =
    (1f - progress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

/**
 * Returns `true` only when [currentRoute] is a deeper non-root destination stacked directly above
 * a root tab (`HomeRoute`, `PeopleRoute`, or `TimelineRoute`), where `WarmRootTabsHost` and
 * `FloatingDockOverlay` must scrub in lockstep with the outgoing screen.
 *
 * Stacked deeper-to-deeper transitions (e.g. `PersonEditRoute -> PersonDetailRoute` or
 * `SettingsAppearanceRoute -> SettingsRoute`) return `false` so `NavHost`'s native
 * `SeekableTransitionState` handles both screens directly.
 */
fun shouldUseRootBridgeForBack(
    currentRoute: String?,
    previousRoute: String?
): Boolean {
    if (currentRoute == null || previousRoute == null) return false
    if (isMainRootRoute(currentRoute) || currentRoute == OnboardingRoute.ROUTE_NAME || currentRoute == OnboardingRoute.REPLAY_ROUTE_NAME) return false
    return isMainRootRoute(previousRoute)
}

/**
 * Computes the remaining finish-animation duration (in ms) when a predictive back gesture is
 * committed partway through, or `0` when [isReducedMotion] is enabled.
 */
fun resolvePredictiveFinishDurationMs(
    progress: Float,
    baseDurationMs: Int = 240,
    isReducedMotion: Boolean = false
): Int {
    if (isReducedMotion || baseDurationMs <= 0) return 0
    val remaining = (1f - progress.coerceIn(0f, 1f))
    return (remaining * baseDurationMs).toInt().coerceIn(60, baseDurationMs)
}

/**
 * Tracks a deeper route's `NavBackStackEntry.id` across a predictive-back commit so `NavHost`
 * skips replaying `popExitTransition` for that already-exited entry until its composition is
 * actually disposed.
 */
@Stable
class PredictiveBackPopHandoff {
    private var committedEntryId by mutableStateOf<String?>(null)

    fun markCommitted(entryId: String) {
        committedEntryId = entryId
    }

    fun hasActiveCommit(): Boolean = committedEntryId != null

    fun shouldSkipPopExit(exitingEntryId: String): Boolean =
        committedEntryId != null && committedEntryId == exitingEntryId

    fun onExitingEntryDisposed(entryId: String): Boolean {
        if (committedEntryId == entryId) {
            committedEntryId = null
            return true
        }
        return false
    }

    fun clear() {
        committedEntryId = null
    }
}

/**
 * Draw-phase modifier applying canonical `+X` pop-exit slide and fade without recomposition.
 */
fun Modifier.predictiveDeeperRoutePopExit(
    progressProvider: () -> Float
): Modifier = graphicsLayer {
    val progress = progressProvider().coerceIn(0f, 1f)
    if (progress > 0f) {
        translationX = resolveDeeperRoutePopExitOffsetX(progress, size.width)
        alpha = resolveDeeperRoutePopExitAlpha(progress)
    }
}

/**
 * Draw-phase modifier applying vertical downward translation and subtle fade for bottom-anchored
 * sheets/cards during predictive back dismissal.
 */
fun Modifier.predictiveSheetDismiss(
    progressProvider: () -> Float
): Modifier = graphicsLayer {
    val progress = progressProvider().coerceIn(0f, 1f)
    if (progress > 0f) {
        translationY = resolveSheetDismissTranslationY(progress, size.height)
        alpha = (1f - progress * 0.65f).coerceIn(0f, 1f)
    }
}

/**
 * Draw-phase modifier applying subtle horizontal fade-out to floating selection action trays
 * during predictive back exit from selection mode.
 */
fun Modifier.predictiveSelectionTrayExit(
    progressProvider: () -> Float
): Modifier = graphicsLayer {
    val progress = progressProvider().coerceIn(0f, 1f)
    if (progress > 0f) {
        alpha = resolveSelectionExitAlpha(progress)
        translationX = progress * 48.dp.toPx()
    }
}

/**
 * Reusable predictive back handler that drives a smooth `0f..1f` [Animatable] progress value,
 * animates gracefully back to `0f` if cancelled, and optionally finishes animating to `1f`
 * before invoking [onCommit].
 */
@Composable
fun ScrubbablePredictiveBackHandler(
    enabled: Boolean,
    isReducedMotion: Boolean,
    onProgressChanged: (Float) -> Unit,
    finishOnCommit: Boolean = false,
    resetOnCommit: Boolean = !finishOnCommit,
    baseFinishDurationMs: Int = 220,
    onCommit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(enabled) {
        if (!enabled && animatable.value != 0f && resetOnCommit) {
            animatable.snapTo(0f)
            onProgressChanged(0f)
        }
    }

    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                val normalized = normalizePredictiveBackProgress(backEvent.progress, isReducedMotion)
                animatable.snapTo(normalized)
                onProgressChanged(normalized)
            }

            if (finishOnCommit && !isReducedMotion && animatable.value < 0.999f) {
                val durationMs = resolvePredictiveFinishDurationMs(
                    progress = animatable.value,
                    baseDurationMs = baseFinishDurationMs,
                    isReducedMotion = false
                )
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = MotionTokens.M3EmphasizedEasing)
                ) {
                    onProgressChanged(value)
                }
            }
            onCommit()
            if (resetOnCommit) {
                animatable.snapTo(0f)
                onProgressChanged(0f)
            }
        } catch (e: CancellationException) {
            scope.launch {
                if (isReducedMotion) {
                    animatable.snapTo(0f)
                    onProgressChanged(0f)
                } else {
                    animatable.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180, easing = MotionTokens.M3EmphasizedEasing)
                    ) {
                        onProgressChanged(value)
                    }
                }
            }
            throw e
        }
    }
}

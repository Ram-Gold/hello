package com.example.hello.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State tracking pull-to-action motion physics:
 * - Diminishing returns rubber-band resistance
 * - Normalized progress [0.0f, 1.0f] toward thresholdPx
 * - Triggered state when pull exceeds thresholdPx
 * - Spring snap-back on gesture release
 */
@Stable
class PullToActionState(
    val thresholdPx: Float = 216f,
    val maxPullPx: Float = 432f
) {
    var pullOffsetPx by mutableFloatStateOf(0f)
        internal set

    val progress: Float
        get() = if (thresholdPx > 0f) (pullOffsetPx / thresholdPx).coerceIn(0f, 1f) else 0f

    val isTriggered: Boolean
        get() = pullOffsetPx >= thresholdPx

    fun calculateResistanceDelta(rawDelta: Float): Float {
        if (rawDelta <= 0f) return rawDelta
        val pullFraction = (pullOffsetPx / maxPullPx).coerceIn(0f, 1f)
        return rawDelta * (1f - pullFraction) * 0.5f
    }

    fun applyDelta(delta: Float) {
        pullOffsetPx = (pullOffsetPx + delta).coerceIn(0f, maxPullPx)
    }

    fun reset() {
        pullOffsetPx = 0f
    }

    suspend fun animateSnapBack() {
        if (pullOffsetPx == 0f) return
        try {
            Animatable(pullOffsetPx).animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                pullOffsetPx = value
            }
        } catch (_: IllegalStateException) {
            // Graceful fallback for non-Compose unit test environments lacking MonotonicFrameClock
            pullOffsetPx = 0f
        }
    }
}

/**
 * Remember a [PullToActionState] instance across recompositions.
 */
@Composable
fun rememberPullToActionState(
    thresholdPx: Float = 216f,
    maxPullPx: Float = 432f
): PullToActionState {
    return remember {
        PullToActionState(thresholdPx = thresholdPx, maxPullPx = maxPullPx)
    }
}

/**
 * Compose [NestedScrollConnection] coordinating pull-down gestures at the top boundary.
 */
class PullToActionNestedScrollConnection(
    private val state: PullToActionState,
    private val coroutineScope: CoroutineScope,
    private val enabled: Boolean = true,
    private val isAtTop: () -> Boolean = { true },
    private val onTrigger: () -> Unit
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!enabled) return Offset.Zero
        // If pulled down and dragging back up, collapse pull offset before scrolling list content
        if (available.y < 0f && state.pullOffsetPx > 0f) {
            val consumedY = available.y.coerceAtLeast(-state.pullOffsetPx)
            state.applyDelta(consumedY)
            return Offset(0f, consumedY)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (!enabled || !isAtTop()) return Offset.Zero
        // If pulling down at the top boundary (unconsumed downward delta)
        if (available.y > 0f && source == NestedScrollSource.UserInput) {
            val resistedDelta = state.calculateResistanceDelta(available.y)
            state.applyDelta(resistedDelta)
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.pullOffsetPx > 0f) {
            val wasTriggered = state.isTriggered
            if (wasTriggered) {
                onTrigger()
            }
            state.animateSnapBack()
            return Velocity(0f, available.y)
        }
        return Velocity.Zero
    }
}

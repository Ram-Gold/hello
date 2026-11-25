package com.example.hello.ui.quickaction

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure placement & lifecycle calculations for QuickCaptureActivity (widget entry only, ADR-0040).
 * Pre-positions the floating Capture Flow card at the learned per-orientation IME height on frame 0
 * so the keyboard opens without per-frame Compose relayout contention.
 */
object WidgetImePlacement {
    const val DEFAULT_IME_HEIGHT_FRACTION: Float = 0.40f
    const val MAX_IME_HEIGHT_FRACTION: Float = 0.65f
    const val SETTLED_CORRECTION_THRESHOLD_DP: Int = 4

    fun resolveInitialBottomPaddingPx(
        savedImeHeightPx: Int,
        screenHeightPx: Int,
        navBarBottomPx: Int
    ): Int {
        val safeNav = navBarBottomPx.coerceAtLeast(0)
        val safeScreen = screenHeightPx.coerceAtLeast(safeNav)
        val fallback = (safeScreen * DEFAULT_IME_HEIGHT_FRACTION).roundToInt().coerceAtLeast(safeNav)
        val maxAllowed = (safeScreen * MAX_IME_HEIGHT_FRACTION).roundToInt().coerceAtLeast(safeNav)
        return if (savedImeHeightPx > safeNav) {
            savedImeHeightPx.coerceIn(safeNav, maxAllowed)
        } else {
            fallback.coerceIn(safeNav, maxAllowed)
        }
    }

    fun resolveTargetBottomPaddingPx(
        currentBottomPx: Int,
        imeAnimationTargetBottomPx: Int,
        navBarBottomPx: Int
    ): Int {
        val safeNav = navBarBottomPx.coerceAtLeast(0)
        return if (imeAnimationTargetBottomPx > safeNav) {
            imeAnimationTargetBottomPx
        } else {
            currentBottomPx.coerceAtLeast(safeNav)
        }
    }

    fun shouldApplySettledCorrection(
        currentBottomPx: Int,
        settledImeBottomPx: Int,
        thresholdPx: Int
    ): Boolean {
        if (settledImeBottomPx <= 0) return false
        return abs(settledImeBottomPx - currentBottomPx) > thresholdPx.coerceAtLeast(0)
    }

    fun shouldDismissOnImeChange(
        hasSeenImeVisible: Boolean,
        isImeVisibleNow: Boolean
    ): Boolean = hasSeenImeVisible && !isImeVisibleNow
}

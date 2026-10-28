package com.example.hello.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.example.hello.ui.navigation.TRANSITION_MS
import com.example.hello.ui.theme.MotionTokens
import kotlin.math.abs

/**
 * Depth tokens for low-end-optimized transitions matching PixelPlayer:
 * - 380ms duration with M3EmphasizedEasing (`CubicBezierEasing(0.2f, 0f, 0f, 1f)`)
 * - Root tab switches (`HomeRoute` <-> `PeopleRoute`) use `CompositingStrategy.Auto` with
 *   zero offscreen buffer allocation, zero blur shader, zero dim overlay, and zero path clipping
 * - Zero scaling / size distortion at rest (pure 1.0f scale)
 */
object ScreenDepthTokens {
    const val DURATION_MS = TRANSITION_MS

    // Stack receding tokens (visible depth fade on content layer)
    const val MIN_CONTENT_ALPHA = 0.0f
    const val MAX_CONTENT_ALPHA_FADE = 1.0f - MIN_CONTENT_ALPHA // 1.0f

    // Pager continuous transition tokens (retained for backward compatibility)
    const val MIN_PAGER_CONTENT_ALPHA = 0.0f
    const val MAX_PAGER_ALPHA_FADE = 1.0f - MIN_PAGER_CONTENT_ALPHA // 1.0f

    // Strictly zero scaling / sizing down in wrapper
    const val MIN_CONTENT_SCALE = 1.0f
    const val MAX_CONTENT_SCALE_REDUCTION = 0f
    const val MIN_PAGER_CONTENT_SCALE = 1.0f
    const val MAX_PAGER_SCALE_REDUCTION = 0f

    // Low-end GPU optimization: skip offscreen buffers, corner clipping, dim scrims, and blur shaders
    val ROOT_TAB_COMPOSITING_STRATEGY = CompositingStrategy.Auto
    const val MAX_CORNER_RADIUS_DP = 0f
    const val MAX_DIM_ALPHA = 0f
    const val MAX_BLUR_DP = 0f
    const val MAX_SCALE_REDUCTION = 0f
    const val MIN_SCALE = 1.0f
    const val MAX_PAGER_DIM_ALPHA = 0f
    const val MIN_PAGER_SCALE = 1.0f
    const val MIN_PAGER_ALPHA = MIN_PAGER_CONTENT_ALPHA
    const val ENABLE_DEPTH_BLUR = false
}

/**
 * Low-end-optimized screen wrapper for NavHost destinations.
 *
 * Keeps `compositingStrategy = CompositingStrategy.Auto` and `clip = false` so root tab
 * switches (`HomeRoute` <-> `PeopleRoute`) execute with zero offscreen framebuffer allocation
 * or shader passes on entry-level GPUs.
 */
@Composable
fun ScreenDepthWrapper(
    isDepthVisible: Boolean,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") enableBlur: Boolean = false,
    content: @Composable () -> Unit
) {
    if (!isDepthVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
        return
    }

    val animatedAlphaState = animateFloatAsState(
        targetValue = ScreenDepthTokens.MIN_CONTENT_ALPHA,
        animationSpec = tween(ScreenDepthTokens.DURATION_MS, easing = MotionTokens.EaseInOut),
        label = "contentDepthAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = ScreenDepthTokens.ROOT_TAB_COMPOSITING_STRATEGY
                clip = false
                alpha = animatedAlphaState.value
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

/**
 * Continuous physics-driven depth wrapper overload retained for backward compatibility.
 */
@Composable
fun ScreenDepthWrapper(
    pageOffset: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val absOffset = abs(pageOffset).coerceIn(0f, 1f)
    val easedProgress = MotionTokens.EaseInOut.transform(absOffset)
    val alpha = (1.0f - (ScreenDepthTokens.MAX_PAGER_ALPHA_FADE * easedProgress)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.compositingStrategy = ScreenDepthTokens.ROOT_TAB_COMPOSITING_STRATEGY
                    this.alpha = alpha
                    this.translationX = -(pageOffset * size.width)
                }
        ) {
            content()
        }
    }
}

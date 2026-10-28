package com.example.hello.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Backward compatibility facade for [ScreenDepthTokens].
 */
object ScreenWrapperTokens {
    const val MAX_CORNER_RADIUS_DP = ScreenDepthTokens.MAX_CORNER_RADIUS_DP
    const val MAX_DIM_ALPHA = ScreenDepthTokens.MAX_DIM_ALPHA
    const val MAX_SCALE_REDUCTION = ScreenDepthTokens.MAX_SCALE_REDUCTION
    const val MIN_SCALE = ScreenDepthTokens.MIN_SCALE
    const val MAX_ALPHA_FADE = ScreenDepthTokens.MAX_PAGER_ALPHA_FADE
    const val MIN_ALPHA = ScreenDepthTokens.MIN_PAGER_ALPHA
}

/**
 * ScreenWrapper overload delegating to production-safe [ScreenDepthWrapper].
 */
@Composable
fun ScreenWrapper(
    isDepthVisible: Boolean,
    modifier: Modifier = Modifier,
    enableBlur: Boolean = ScreenDepthTokens.ENABLE_DEPTH_BLUR,
    content: @Composable () -> Unit
) {
    ScreenDepthWrapper(
        isDepthVisible = isDepthVisible,
        modifier = modifier,
        enableBlur = enableBlur,
        content = content
    )
}

/**
 * ScreenWrapper overload taking [NavController] to automatically gate depth visibility
 * based on whether this destination is currently behind another screen.
 */
@Composable
fun ScreenWrapper(
    navController: NavController,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    enableBlur: Boolean = ScreenDepthTokens.ENABLE_DEPTH_BLUR,
    content: @Composable () -> Unit
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val thisRoute = animatedVisibilityScope?.let {
        // If animatedVisibilityScope is provided, use back stack check
        currentRoute
    }
    val isBehind = currentRoute != null && thisRoute != null && currentRoute != thisRoute

    ScreenDepthWrapper(
        isDepthVisible = isBehind,
        modifier = modifier,
        enableBlur = enableBlur,
        content = content
    )
}

/**
 * ScreenWrapper overload for AnimatedVisibilityScope extension usage.
 */
@Composable
fun AnimatedVisibilityScope.ScreenWrapper(
    modifier: Modifier = Modifier,
    isDepthVisible: Boolean = false,
    enableBlur: Boolean = ScreenDepthTokens.ENABLE_DEPTH_BLUR,
    content: @Composable () -> Unit
) {
    ScreenDepthWrapper(
        isDepthVisible = isDepthVisible,
        modifier = modifier,
        enableBlur = enableBlur,
        content = content
    )
}

/**
 * Continuous pager transition wrapper delegating to [ScreenDepthWrapper].
 */
@Composable
fun ScreenWrapper(
    pageOffset: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ScreenDepthWrapper(
        pageOffset = pageOffset,
        modifier = modifier,
        content = content
    )
}

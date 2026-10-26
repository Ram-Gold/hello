package com.example.hello.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.theme.MotionTokens

object DockMotionConfig {
    // Snappy critically-damped spring stiffness (500f) produces a visibly smooth, tactile spring glide across tabs
    const val PILL_SPRING_STIFFNESS = MotionTokens.PILL_SPRING_STIFFNESS
    // Critically-damped (1.0f) for velocity-preserving interruptible translation with zero overshoot
    const val PILL_SPRING_DAMPING = MotionTokens.PILL_SPRING_DAMPING
    // Smooth tab label/icon crossfade aligned with the pill travel
    const val SCREEN_FADE_DURATION_MS = MotionTokens.TAB_COLOR_TWEEN_DURATION_MS
    // Disable default click indication / ripple background on tabs ("only the pill")
    const val HAS_TAB_CLICK_INDICATION = false
    // Clip-path reveal technique enabled for pixel-perfect typography and edge reveal
    const val USE_CLIP_PATH_REVEAL = true

    // Visual geometry and drop shadow elevation
    const val DOCK_CONTAINER_WIDTH_DP = 220
    const val DOCK_SHADOW_ELEVATION_DP = 12
    const val DOCK_PRESSED_SHADOW_ELEVATION_DP = 14
    const val PILL_VERTICAL_PADDING_DP = 4
    const val PILL_CONTENT_HORIZONTAL_PADDING_DP = 16
    const val TAB_ITEM_HORIZONTAL_PADDING_DP = 14
    const val HAS_DOCK_BORDER = false

    // Warm tactile shadow tokens for light theme
    val WARM_SHADOW_AMBIENT = Color(0x382B1F14)
    val WARM_SHADOW_SPOT = Color(0x592B1F14)

    // Unified left-to-right text transition offsets
    const val TEXT_ENTER_OFFSET_FRACTION = -0.5f
    const val TEXT_EXIT_OFFSET_FRACTION = 0.5f

    /**
     * Resolves the navigation tab icon based on destination and active selection state.
     * When selected: Solid Filled icon vector.
     * When unselected: Outlined icon vector.
     */
    fun resolveTabIcon(tab: HomeNavTab, isSelected: Boolean): ImageVector = when (tab) {
        HomeNavTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
        HomeNavTab.PEOPLE -> if (isSelected) Icons.Filled.People else Icons.Outlined.People
    }

    /**
     * Calculates tight content-hugging pill bounds (left and right coordinates) centered within
     * the tab slot.
     */
    fun calculatePillBounds(
        slotCenterX: Float,
        contentWidth: Float,
        horizontalPadding: Float,
        slotWidth: Float
    ): Pair<Float, Float> {
        val tightWidth = (contentWidth + (horizontalPadding * 2f)).coerceAtMost(slotWidth)
        val left = slotCenterX - (tightWidth / 2f)
        val right = slotCenterX + (tightWidth / 2f)
        return Pair(left, right)
    }
}

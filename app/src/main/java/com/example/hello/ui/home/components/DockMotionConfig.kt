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
    const val DOCK_PRESSED_SHADOW_ELEVATION_DP = 16
    const val PILL_VERTICAL_PADDING_DP = 4
    const val PILL_CONTENT_HORIZONTAL_PADDING_DP = 16
    const val TAB_ITEM_HORIZONTAL_PADDING_DP = 14
    const val DOCK_ITEMS_GAP_DP = 8
    const val HAS_DOCK_BORDER = false

    // Warm tactile shadow tokens for light theme (natural diffuse single shadow, darker contrast)
    val WARM_SHADOW_AMBIENT = Color(0x802B1F14)
    val WARM_SHADOW_SPOT = Color(0xA6140D07)

    // Invariant 1:1 icon expansion footprint: 24dp square icon slot + 8dp spacer
    const val ICON_EXPANSION_WIDTH_DP = 32

    // Directional icon horizontal slide fractions
    const val ICON_ENTER_OFFSET_FRACTION = -0.5f
    const val ICON_EXIT_OFFSET_FRACTION = -0.5f

    // Backward-compatible aliases
    const val TEXT_ENTER_OFFSET_FRACTION = ICON_ENTER_OFFSET_FRACTION
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
     * the tab slot, preserving uniform vertical and outer margin constraints.
     */
    fun calculatePillBounds(
        slotCenterX: Float,
        contentWidth: Float,
        horizontalPadding: Float,
        slotWidth: Float,
        slotPadding: Float = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
    ): Pair<Float, Float> {
        val maxPillWidth = (slotWidth - (slotPadding * 2f)).coerceAtLeast(0f)
        val tightWidth = (contentWidth + (horizontalPadding * 2f)).coerceIn(0f, maxPillWidth)
        val left = slotCenterX - (tightWidth / 2f)
        val right = slotCenterX + (tightWidth / 2f)
        return Pair(left, right)
    }

    /**
     * Estimates the content width in DP for a tab item with an icon (24dp) + gap (8dp) + label text.
     */
    fun estimateContentWidthDp(label: String): Int = 32 + (label.length * 8)

    /**
     * Calculates the dynamic hugging dock container width in DP based on the active tab content width.
     * Hugs the active pill (content + 2 * horizontalPadding) + gap (8dp) + unselected 1:1 icon slot (42dp)
     * + outer slot padding (2 * 4dp) + surface inner padding (10dp).
     */
    fun calculateHuggingDockWidthDp(
        contentWidthDp: Float,
        horizontalPaddingDp: Float = PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat(),
        slotPaddingDp: Float = PILL_VERTICAL_PADDING_DP.toFloat(),
        gapDp: Float = DOCK_ITEMS_GAP_DP.toFloat(),
        unselectedSlotWidthDp: Float = 42f,
        surfacePaddingDp: Float = 10f
    ): Float {
        val pillWidth = contentWidthDp + (horizontalPaddingDp * 2f)
        return pillWidth + gapDp + unselectedSlotWidthDp + (slotPaddingDp * 2f) + surfacePaddingDp
    }

    /**
     * Calculates a stable, non-animating dock container width in DP based on the configured terminology.
     * Uses the maximum of Home content and the relationship tab content so the dock remains completely
     * static during tab switching, eliminating jitter and resizing bugs while snugly fitting both states.
     */
    fun calculateStableDockWidthDp(
        relationshipContentWidthDp: Float,
        homeContentWidthDp: Float = 68f,
        horizontalPaddingDp: Float = PILL_CONTENT_HORIZONTAL_PADDING_DP.toFloat(),
        slotPaddingDp: Float = PILL_VERTICAL_PADDING_DP.toFloat(),
        gapDp: Float = 10f,
        unselectedSlotWidthDp: Float = 42f,
        surfacePaddingDp: Float = 10f
    ): Float {
        val homePillWidth = homeContentWidthDp + (horizontalPaddingDp * 2f)
        val relationshipPillWidth = relationshipContentWidthDp + (horizontalPaddingDp * 2f)
        val maxPillWidth = maxOf(homePillWidth, relationshipPillWidth)
        return maxPillWidth + gapDp + unselectedSlotWidthDp + (slotPaddingDp * 2f) + surfacePaddingDp
    }

    /**
     * Calculates dynamically sized pill coordinates anchored to the container edge:
     * - Home (isRightAnchored = false): left = slotPadding, right = left + pillWidth
     * - People/Friends/Connections (isRightAnchored = true): right = containerWidth - slotPadding, left = right - pillWidth
     *
     * Ensures consistent padding across all edges (left/right, top, bottom = slotPadding).
     */
    fun calculateAnchoredPillBounds(
        contentWidth: Float,
        horizontalPadding: Float,
        containerWidth: Float,
        isRightAnchored: Boolean,
        slotPadding: Float = DockMotionConfig.PILL_VERTICAL_PADDING_DP.toFloat()
    ): Pair<Float, Float> {
        val maxAvailableWidth = (containerWidth - (slotPadding * 2f)).coerceAtLeast(0f)
        val pillWidth = (contentWidth + (horizontalPadding * 2f)).coerceIn(0f, maxAvailableWidth)
        return if (isRightAnchored) {
            val right = containerWidth - slotPadding
            val left = right - pillWidth
            Pair(left, right)
        } else {
            val left = slotPadding
            val right = left + pillWidth
            Pair(left, right)
        }
    }

    // Default home selection pill width providing ~15dp balanced inline padding
    const val HOME_PILL_WIDTH_DP = 96

    /**
     * Resolves pre-configured, deterministic dock and tab-specific selection pill dimensions per relationship terminology.
     * Guarantees 100% static container geometry during tab switching (zero jitter) while ensuring both
     * Home and the relationship tab enjoy consistent, generous ~15dp inline padding.
     */
    fun resolveDockDimensions(label: String?): DockDimensions {
        val homePill = HOME_PILL_WIDTH_DP
        val relPill = when (label?.lowercase()) {
            "connections" -> 138
            "contacts" -> 118
            "friends" -> 108
            "people" -> 104
            else -> 108
        }
        val dockWidth = homePill + relPill + 26
        return DockDimensions(
            dockWidthDp = dockWidth,
            homePillWidthDp = homePill,
            relationshipPillWidthDp = relPill
        )
    }

    /**
     * Calculates deterministic position and width coordinates for the selection pill:
     * - At progress = 0f (Home): pill center = homeCenter, width = homePillWidth
     * - At progress = 1f (Relationship): pill center = relCenter, width = relationshipPillWidth
     * Guarantees exact 4dp outer margins and ~15dp balanced inline padding on both tabs.
     */
    fun calculateBalancedPillBounds(
        containerWidth: Float,
        homePillWidth: Float,
        relationshipPillWidth: Float,
        progress: Float,
        slotPadding: Float = PILL_VERTICAL_PADDING_DP.toFloat()
    ): Pair<Float, Float> {
        val homeCenter = slotPadding + (homePillWidth / 2f)
        val relCenter = containerWidth - slotPadding - (relationshipPillWidth / 2f)
        val currentCenter = homeCenter + (relCenter - homeCenter) * progress
        val currentWidth = homePillWidth + (relationshipPillWidth - homePillWidth) * progress
        val left = currentCenter - (currentWidth / 2f)
        val right = currentCenter + (currentWidth / 2f)
        return Pair(left, right)
    }

    /**
     * Backward-compatible overload for symmetrical pill widths.
     */
    fun calculateCenteredPillBounds(
        containerWidth: Float,
        pillWidth: Float,
        progress: Float
    ): Pair<Float, Float> = calculateBalancedPillBounds(
        containerWidth = containerWidth,
        homePillWidth = pillWidth,
        relationshipPillWidth = pillWidth,
        progress = progress
    )
}

/**
 * Pre-configured dimensions for a dock with balanced, tab-specific pill widths.
 */
data class DockDimensions(
    val dockWidthDp: Int,
    val homePillWidthDp: Int,
    val relationshipPillWidthDp: Int
) {
    // Backward-compatibility property
    val pillWidthDp: Int get() = relationshipPillWidthDp
}

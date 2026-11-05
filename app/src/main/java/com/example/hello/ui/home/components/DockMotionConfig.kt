package com.example.hello.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
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
    const val DOCK_HEIGHT_DP = 60
    const val DOCK_FAB_SIZE_DP = 60
    const val DOCK_FAB_CORNER_RADIUS_DP = 20
    const val SPEED_DIAL_CHIP_HEIGHT_DP = DOCK_HEIGHT_DP
    const val SPEED_DIAL_CHIP_CORNER_RADIUS_DP = SPEED_DIAL_CHIP_HEIGHT_DP / 2
    const val SPEED_DIAL_CHIP_HORIZONTAL_PADDING_DP = 20
    const val SPEED_DIAL_FAB_GAP_DP = 12
    const val SPEED_DIAL_STACK_GAP_DP = 10
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

    // Bounded micro-translation constants for GPU motion (zero layout re-measurements)
    const val ICON_SLIDE_DISTANCE_DP = 16
    const val TEXT_CENTER_SHIFT_DP = 16

    // Directional icon horizontal slide fractions (positive enters from right to left)
    const val ICON_ENTER_OFFSET_FRACTION = 0.5f
    const val ICON_EXIT_OFFSET_FRACTION = -0.5f

    // Backward-compatible aliases
    const val TEXT_ENTER_OFFSET_FRACTION = ICON_ENTER_OFFSET_FRACTION
    const val TEXT_EXIT_OFFSET_FRACTION = 0.5f

    /**
     * Resolves the directional multiplier for tab icon translation during transitions.
     * When navigating towards People (moving right): multiplier is +1.0f (enters from right, exits to right).
     * When navigating towards Home (moving left): multiplier is -1.0f (enters from left, exits to left).
     */
    fun resolveDirectionMultiplier(selectedTab: HomeNavTab): Float = when (selectedTab) {
        HomeNavTab.TIMELINE -> 1.0f
        HomeNavTab.PEOPLE -> 1.0f
        HomeNavTab.HOME -> -1.0f
    }

    /**
     * Calculates the stationary slot centers for Home (Slot 0) and Relationship (Slot 1)
     * within the dock container. These centers match where the selection pill lands at progress 0.0
     * and 1.0, ensuring zero layout jumping or clipping.
     */
    fun calculateSlotCenters(
        containerWidth: Float,
        homePillWidth: Float,
        relationshipPillWidth: Float,
        slotPadding: Float = PILL_VERTICAL_PADDING_DP.toFloat()
    ): Pair<Float, Float> {
        val homeCenter = slotPadding + (homePillWidth / 2f)
        val relCenter = containerWidth - slotPadding - (relationshipPillWidth / 2f)
        return Pair(homeCenter, relCenter)
    }

    /**
     * Resolves the navigation tab icon based on destination and active selection state.
     * Uses Outlined icons when inactive and Filled icons when active (ADR-0014).
     */
    fun resolveTabIcon(tab: HomeNavTab, isSelected: Boolean): ImageVector = if (isSelected) {
        when (tab) {
            HomeNavTab.HOME -> Icons.Filled.Home
            HomeNavTab.PEOPLE -> Icons.Filled.People
            HomeNavTab.TIMELINE -> Icons.Filled.CalendarMonth
        }
    } else {
        when (tab) {
            HomeNavTab.HOME -> Icons.Outlined.Home
            HomeNavTab.PEOPLE -> Icons.Outlined.People
            HomeNavTab.TIMELINE -> Icons.Outlined.CalendarMonth
        }
    }

    /**
     * Resolves the Outlined navigation tab icon for unselected states.
     */
    fun tabOutlinedIcon(tab: HomeNavTab): ImageVector = resolveTabIcon(tab, isSelected = false)

    /**
     * Resolves the Filled navigation tab icon for the active state.
     */
    fun tabFilledIcon(tab: HomeNavTab): ImageVector = resolveTabIcon(tab, isSelected = true)

    /**
     * Resolves directional horizontal slide offset fraction for tab icons during transitions.
     * When entering People or exiting Home (moving right): enters right-to-left (+0.5f) and exits left-to-right (+0.5f).
     * When entering Home or exiting People (moving left): enters left-to-right (-0.5f) and exits right-to-left (-0.5f).
     */
    fun resolveIconOffsetFraction(tab: HomeNavTab, isSelected: Boolean): Float = when (tab) {
        HomeNavTab.TIMELINE -> if (isSelected) 0.5f else -0.5f
        HomeNavTab.PEOPLE -> if (isSelected) 0.5f else -0.5f
        HomeNavTab.HOME -> if (isSelected) -0.5f else 0.5f
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

    // Default home selection pill width providing comfortable ~15dp balanced inline padding with 1:1 icon
    const val HOME_PILL_WIDTH_DP = 96
    const val HOME_INACTIVE_SLOT_WIDTH_DP = 96
    const val TIMELINE_PILL_WIDTH_DP = 104

    /**
     * Resolves pre-configured, deterministic 3-tab dock dimensions (`Home` | `Timeline` | `People`)
     * per relationship terminology (ADR-0014).
     * Active tab expands to `activePillWidthDp` (icon + label), while the 2 inactive tabs use
     * compact icon-only slots (`inactiveSlotWidthDp`), keeping total dock width and selection pill width 100% constant.
     */
    fun resolveThreeTabDockDimensions(label: String?): ThreeTabDockDimensions {
        val activePill = when (label?.lowercase()) {
            "connections" -> 132
            "contacts" -> 116
            "friends" -> 112
            "people" -> 112
            else -> 112
        }
        val inactiveSlot = 52
        val dockWidth = activePill + (inactiveSlot * 2) + 10
        return ThreeTabDockDimensions(
            dockWidthDp = dockWidth,
            activePillWidthDp = activePill,
            inactiveSlotWidthDp = inactiveSlot
        )
    }

    /**
     * Calculates constant-width selection pill bounds `(left, right)` across the 3-slot dock
     * where `progress` is in `0f..2f` (`0f` = Home, `1f` = Timeline, `2f` = People).
     * Because the active slot has width `activePillWidth` and each inactive slot has width `inactiveSlotWidth`,
     * the pill's left edge translates cleanly by `progress * inactiveSlotWidth` while maintaining a strictly
     * constant width of `activePillWidth`.
     */
    fun calculateConstantWidthThreeTabPillBounds(
        activePillWidth: Float,
        inactiveSlotWidth: Float,
        progress: Float
    ): Pair<Float, Float> {
        val p = progress.coerceIn(0f, 2f)
        val left = p * inactiveSlotWidth
        val right = left + activePillWidth
        return Pair(left, right)
    }

    /**
     * Calculates the 3 tab slot widths `(slot0Width, slot1Width, slot2Width)` at any transition `progress` in `0f..2f`.
     * The sum of the three slot widths is invariant (`activePillWidth + 2 * inactiveSlotWidth`), and the active slot's
     * bounds match `calculateConstantWidthThreeTabPillBounds` at every frame.
     */
    fun calculateThreeTabSlotWidths(
        activePillWidth: Float,
        inactiveSlotWidth: Float,
        progress: Float
    ): Triple<Float, Float, Float> {
        val p = progress.coerceIn(0f, 2f)
        val (w0, w1, w2) = if (p <= 1f) {
            Triple(1f - p, p, 0f)
        } else {
            Triple(0f, 2f - p, p - 1f)
        }
        val delta = activePillWidth - inactiveSlotWidth
        return Triple(
            inactiveSlotWidth + delta * w0,
            inactiveSlotWidth + delta * w1,
            inactiveSlotWidth + delta * w2
        )
    }

    /**
     * Calculates deterministic position (left) and width for the 3-slot sliding pill
     * where `progress` is in `0f..2f` (`0f` = Home, `1f` = Timeline, `2f` = People).
     */
    fun calculateThreeTabPillBounds(
        homeWidth: Float,
        timelineWidth: Float,
        relationshipWidth: Float,
        progress: Float
    ): Pair<Float, Float> {
        val p = progress.coerceIn(0f, 2f)
        return if (p <= 1f) {
            val t = p
            val left = homeWidth * t
            val width = homeWidth + (timelineWidth - homeWidth) * t
            Pair(left, left + width)
        } else {
            val t = p - 1f
            val left = homeWidth + (timelineWidth * t)
            val width = timelineWidth + (relationshipWidth - timelineWidth) * t
            Pair(left, left + width)
        }
    }

    /**
     * Resolves pre-configured, deterministic dock and tab-specific selection pill dimensions per relationship terminology.
     * Guarantees 100% static container geometry during tab switching (zero jitter) while providing comfortable
     * breathing room for both active and unselected tabs.
     */
    fun resolveDockDimensions(label: String?): DockDimensions {
        val (dockWidth, relPill, relInactive) = when (label?.lowercase()) {
            "connections" -> Triple(238, 132, 132)
            "contacts" -> Triple(220, 114, 114)
            "friends" -> Triple(210, 104, 104)
            "people" -> Triple(206, 100, 100)
            else -> Triple(206, 100, 100)
        }
        return DockDimensions(
            dockWidthDp = dockWidth,
            homePillWidthDp = HOME_PILL_WIDTH_DP,
            relationshipPillWidthDp = relPill,
            homeInactiveSlotWidthDp = HOME_INACTIVE_SLOT_WIDTH_DP,
            relationshipInactiveSlotWidthDp = relInactive
        )
    }

    /**
     * Calculates deterministic position and width coordinates for the sliding selection pill
     * over static asymmetric slots.
     * At progress = 0f (Home): left = slotPadding, width = homePillWidth
     * At progress = 1f (Relationship): left = slotPadding + homePillWidth, width = relationshipPillWidth
     */
    fun calculateStaticPillBounds(
        homePillWidth: Float,
        relationshipPillWidth: Float,
        progress: Float,
        slotPadding: Float = 0f
    ): Pair<Float, Float> {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val left = slotPadding + (homePillWidth * clampedProgress)
        val width = homePillWidth + (relationshipPillWidth - homePillWidth) * clampedProgress
        val right = left + width
        return Pair(left, right)
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
     * Calculates the exact bounds of the selection pill centered over the dynamically-weighted
     * slots of the Row. Supports any container width (including wide screens and tablets) with zero overlap.
     */
    fun calculateRowWeightedPillBounds(
        containerWidth: Float,
        homeWeight: Float,
        relationshipWeight: Float,
        homePillWidth: Float,
        relationshipPillWidth: Float,
        progress: Float
    ): Pair<Float, Float> {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val totalWeight = (homeWeight + relationshipWeight).coerceAtLeast(1f)
        val slot0Width = containerWidth * (homeWeight / totalWeight)
        val slot1Width = containerWidth * (relationshipWeight / totalWeight)
        val slot0Center = slot0Width / 2f
        val slot1Center = slot0Width + (slot1Width / 2f)
        val currentCenter = (1f - clampedProgress) * slot0Center + clampedProgress * slot1Center
        val currentWidth = (1f - clampedProgress) * homePillWidth + clampedProgress * relationshipPillWidth
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
    val relationshipPillWidthDp: Int,
    val homeInactiveSlotWidthDp: Int = 64,
    val relationshipInactiveSlotWidthDp: Int = 72
) {
    // Backward-compatibility property
    val pillWidthDp: Int get() = relationshipPillWidthDp
}

data class ThreeTabDockDimensions(
    val dockWidthDp: Int,
    val activePillWidthDp: Int,
    val inactiveSlotWidthDp: Int = 52,
    val homePillWidthDp: Int = activePillWidthDp,
    val timelinePillWidthDp: Int = activePillWidthDp,
    val relationshipPillWidthDp: Int = activePillWidthDp
)

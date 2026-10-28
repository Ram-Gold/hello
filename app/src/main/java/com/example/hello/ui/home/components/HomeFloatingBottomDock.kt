package com.example.hello.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.ui.theme.MotionTokens
import kotlin.math.roundToInt

@Composable
fun HomeFloatingBottomDock(
    selectedTab: HomeNavTab,
    onTabSelect: (HomeNavTab) -> Unit,
    isSpeedDialOpen: Boolean,
    onToggleSpeedDial: () -> Unit,
    onSpeedDialAction: (SpeedDialAction) -> Unit,
    modifier: Modifier = Modifier,
    peopleTabLabel: String = "People",
    addContactLabel: String = "Add Contact",
    pillAnimationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween()
) {
    // 1. Organic Floating Pill Navigation Dock with Draw-Phase Clip-Path Reveal
    val isDark = isSystemInDarkTheme()
    val warmShadowAmbient = if (isDark) Color(0x80000000) else DockMotionConfig.WARM_SHADOW_AMBIENT
    val warmShadowSpot = if (isDark) Color(0xB3000000) else DockMotionConfig.WARM_SHADOW_SPOT

    val dockBorderModifier = if (DockMotionConfig.HAS_DOCK_BORDER) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(30.dp)
        )
    } else {
        Modifier
    }

    // Pre-configured deterministic dock and selection pill dimensions per relationship terminology
    val dimensions = remember(peopleTabLabel) {
        DockMotionConfig.resolveDockDimensions(peopleTabLabel)
    }

    // Separate Rounded-Square Quick-Add FAB with matching elevation & spring rotation
    val fabRotation by animateFloatAsState(
        targetValue = if (isSpeedDialOpen) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "fabRotate"
    )

    val fabBorderModifier = if (DockMotionConfig.HAS_DOCK_BORDER) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(DockMotionConfig.DOCK_FAB_CORNER_RADIUS_DP.dp)
        )
    } else {
        Modifier
    }

    // Bottom Row: [ Floating Navigation Dock ] + [ 1:1 Squircle Quick-Add FAB ]
    // with right-aligned Speed Dial Chips stacked directly above the FAB
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Upper Chip: Met Again (Log Encounter Action)
            AnimatedVisibility(
                visible = isSpeedDialOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)
                ) + fadeIn(animationSpec = spring(stiffness = 400f)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f))
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    SpeedDialChip(
                        label = "Met Again",
                        icon = Icons.Outlined.EditNote,
                        onClick = { onSpeedDialAction(SpeedDialAction.LOG_ENCOUNTER) }
                    )
                    Spacer(modifier = Modifier.height(DockMotionConfig.SPEED_DIAL_STACK_GAP_DP.dp))
                }
            }

            // Lower Chip: Add Contact / Friend / Connection (New Contact Action)
            AnimatedVisibility(
                visible = isSpeedDialOpen,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                ) + fadeIn(animationSpec = spring(stiffness = 500f)),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
                ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f))
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    SpeedDialChip(
                        label = addContactLabel,
                        icon = Icons.Outlined.PersonAdd,
                        onClick = { onSpeedDialAction(SpeedDialAction.NEW_CONTACT) }
                    )
                    Spacer(modifier = Modifier.height(DockMotionConfig.SPEED_DIAL_FAB_GAP_DP.dp))
                }
            }

            // Bottom Unit: [ Floating Navigation Dock Surface ] [ 14dp Spacer ] [ Quick-Add FAB ]
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Floating Navigation Dock Surface
                Surface(
                    modifier = Modifier
                        .width(dimensions.dockWidthDp.dp)
                        .height(DockMotionConfig.DOCK_HEIGHT_DP.dp)
                        .shadow(
                            elevation = DockMotionConfig.DOCK_SHADOW_ELEVATION_DP.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = warmShadowAmbient,
                            spotColor = warmShadowSpot
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .then(dockBorderModifier),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    DockTabsLayer(
                        selectedTab = selectedTab,
                        peopleTabLabel = peopleTabLabel,
                        dimensions = dimensions,
                        pillAnimationSpec = pillAnimationSpec,
                        isSpeedDialOpen = isSpeedDialOpen,
                        onToggleSpeedDial = onToggleSpeedDial,
                        onTabSelect = onTabSelect
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 1:1 Squircle Quick-Add FAB (stationary beside dock at bottom right)
                FloatingActionButton(
                    onClick = onToggleSpeedDial,
                    modifier = Modifier
                        .size(DockMotionConfig.DOCK_FAB_SIZE_DP.dp)
                        .shadow(
                            elevation = DockMotionConfig.DOCK_SHADOW_ELEVATION_DP.dp,
                            shape = RoundedCornerShape(DockMotionConfig.DOCK_FAB_CORNER_RADIUS_DP.dp),
                            ambientColor = warmShadowAmbient,
                            spotColor = warmShadowSpot
                        )
                        .then(fabBorderModifier),
                    shape = RoundedCornerShape(DockMotionConfig.DOCK_FAB_CORNER_RADIUS_DP.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = (DockMotionConfig.DOCK_PRESSED_SHADOW_ELEVATION_DP - DockMotionConfig.DOCK_SHADOW_ELEVATION_DP).dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = if (isSpeedDialOpen) "Close speed dial" else "Quick capture",
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTabsLayer(
    selectedTab: HomeNavTab,
    peopleTabLabel: String,
    dimensions: DockDimensions,
    pillAnimationSpec: AnimationSpec<Float>,
    isSpeedDialOpen: Boolean,
    onToggleSpeedDial: () -> Unit,
    onTabSelect: (HomeNavTab) -> Unit
) {
    val targetProgress = if (selectedTab == HomeNavTab.HOME) 0f else 1f
    val pillProgressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = pillAnimationSpec,
        label = "dockSlidingPillProgress"
    )

    val homeIconFractionProvider = remember {
        { (1f - pillProgressState.value).coerceIn(0f, 1f) }
    }
    val relIconFractionProvider = remember {
        { pillProgressState.value.coerceIn(0f, 1f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
    ) {
        // Base Layer: Inactive styled tabs (onSurfaceVariant color, identical typography)
        TabsRow(
            selectedTab = selectedTab,
            peopleLabel = peopleTabLabel,
            dimensions = dimensions,
            homeIconFractionProvider = homeIconFractionProvider,
            relationshipIconFractionProvider = relIconFractionProvider,
            iconColor = Color.Transparent,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Active Layer: SecondaryContainer pill background + active styled tabs (onSecondaryContainer),
        // clipped dynamically in the GPU draw phase to the moving centered pill shape.
        val pillShape = remember(dimensions.homePillWidthDp, dimensions.relationshipPillWidthDp) {
            CenteredPillClipShape(
                progressProvider = { pillProgressState.value },
                homePillWidthDp = dimensions.homePillWidthDp,
                relationshipPillWidthDp = dimensions.relationshipPillWidthDp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = pillShape
                }
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            TabsRow(
                selectedTab = selectedTab,
                peopleLabel = peopleTabLabel,
                dimensions = dimensions,
                homeIconFractionProvider = homeIconFractionProvider,
                relationshipIconFractionProvider = relIconFractionProvider,
                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Interactive Touch Layer on top (48dp+ accessible touch target)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .semantics {
                        role = Role.Tab
                        selected = selectedTab == HomeNavTab.HOME
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (isSpeedDialOpen) {
                                onToggleSpeedDial()
                            }
                            onTabSelect(HomeNavTab.HOME)
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .semantics {
                        role = Role.Tab
                        selected = selectedTab == HomeNavTab.PEOPLE
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (isSpeedDialOpen) {
                                onToggleSpeedDial()
                            }
                            onTabSelect(HomeNavTab.PEOPLE)
                        }
                    )
            )
        }
    }
}

@Composable
private fun TabsRow(
    selectedTab: HomeNavTab,
    peopleLabel: String = "People",
    dimensions: DockDimensions,
    homeIconFractionProvider: () -> Float,
    relationshipIconFractionProvider: () -> Float,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Slot 0 (Home): anchored at start with exact 4dp padding, width = homePillWidthDp
        // Permanently centered at homeCenter = 4dp + (homePillWidthDp / 2)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp)
                .width(dimensions.homePillWidthDp.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            NavTabItemContent(
                tab = HomeNavTab.HOME,
                customLabel = null,
                selectedTab = selectedTab,
                iconFractionProvider = homeIconFractionProvider,
                icon = DockMotionConfig.tabFilledIcon(HomeNavTab.HOME),
                iconColor = iconColor,
                textColor = textColor
            )
        }

        // Slot 1 (Relationship): anchored at end with exact 4dp padding, width = relationshipPillWidthDp
        // Permanently centered at relCenter = containerWidth - 4dp - (relationshipPillWidthDp / 2)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp)
                .width(dimensions.relationshipPillWidthDp.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            NavTabItemContent(
                tab = HomeNavTab.PEOPLE,
                customLabel = peopleLabel,
                selectedTab = selectedTab,
                iconFractionProvider = relationshipIconFractionProvider,
                icon = DockMotionConfig.tabFilledIcon(HomeNavTab.PEOPLE),
                iconColor = iconColor,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun NavTabItemContent(
    tab: HomeNavTab,
    customLabel: String?,
    selectedTab: HomeNavTab,
    iconFractionProvider: () -> Float,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val displayLabel = customLabel ?: tab.label
    val directionMultiplier = DockMotionConfig.resolveDirectionMultiplier(selectedTab)
    val density = LocalDensity.current
    val slideDistancePx = with(density) { DockMotionConfig.ICON_SLIDE_DISTANCE_DP.dp.toPx() }
    val textShiftPx = with(density) { DockMotionConfig.TEXT_CENTER_SHIFT_DP.dp.toPx() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon slot: fixed 24dp size, motion applied 100% via GPU graphicsLayer
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        val fraction = iconFractionProvider().coerceIn(0f, 1f)
                        alpha = fraction
                        val scale = 0.85f + (0.15f * fraction)
                        scaleX = scale
                        scaleY = scale
                        translationX = (1f - fraction) * slideDistancePx * directionMultiplier
                    },
                contentAlignment = Alignment.Center
            ) {
                if (iconColor != Color.Transparent) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(22.dp)
                            .aspectRatio(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Label text: stable layout, micro-translated on GPU to remain centered when unselected
            Text(
                text = displayLabel,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor,
                modifier = Modifier.graphicsLayer {
                    val fraction = iconFractionProvider().coerceIn(0f, 1f)
                    // At fraction = 1 (selected): translationX = 0 (centered together with icon)
                    // At fraction = 0 (unselected): translationX = -textShiftPx (shifts text by -16dp, centering it in the slot)
                    translationX = -textShiftPx * (1f - fraction)
                }
            )
        }
    }
}

@Composable
private fun SpeedDialChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .wrapContentWidth(Alignment.End)
            .height(DockMotionConfig.SPEED_DIAL_CHIP_HEIGHT_DP.dp),
        shape = RoundedCornerShape(DockMotionConfig.SPEED_DIAL_CHIP_CORNER_RADIUS_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = DockMotionConfig.SPEED_DIAL_CHIP_HORIZONTAL_PADDING_DP.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(22.dp)
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Reusable zero-allocation shape that dynamically outlines the centered sliding pill in the draw phase.
 * Translates between Slot 0 center and Slot 1 center with deterministic width interpolation (zero jitter).
 */
private class CenteredPillClipShape(
    private val progressProvider: () -> Float,
    private val homePillWidthDp: Int,
    private val relationshipPillWidthDp: Int,
    private val verticalPaddingDp: Dp = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val progress = progressProvider()
        val homeWidthPx = with(density) { homePillWidthDp.dp.toPx() }
        val relWidthPx = with(density) { relationshipPillWidthDp.dp.toPx() }
        val vPaddingPx = with(density) { verticalPaddingDp.toPx() }

        val (left, right) = DockMotionConfig.calculateBalancedPillBounds(
            containerWidth = size.width,
            homePillWidth = homeWidthPx,
            relationshipPillWidth = relWidthPx,
            progress = progress,
            slotPadding = vPaddingPx
        )

        val top = vPaddingPx
        val bottom = size.height - vPaddingPx
        val pillHeight = (bottom - top).coerceAtLeast(0f)
        val radius = pillHeight / 2f

        return Outline.Rounded(
            RoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerRadius = CornerRadius(radius, radius)
            )
        )
    }
}

@Preview(name = "Speed Dial Chip Preview", showBackground = true)
@Composable
private fun SpeedDialChipPreview() {
    SpeedDialChip(
        label = "Add Contact",
        icon = Icons.Outlined.PersonAdd,
        onClick = {}
    )
}

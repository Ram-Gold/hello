package com.example.hello.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PersonAdd
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
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

    // Pre-configured deterministic 3-tab dock and selection pill dimensions per relationship terminology
    val dimensions = remember(peopleTabLabel) {
        DockMotionConfig.resolveThreeTabDockDimensions(peopleTabLabel)
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
                        icon = Icons.Rounded.EditNote,
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
                        icon = Icons.Rounded.PersonAdd,
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
                        imageVector = Icons.Rounded.Add,
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
    dimensions: ThreeTabDockDimensions,
    pillAnimationSpec: AnimationSpec<Float>,
    isSpeedDialOpen: Boolean,
    onToggleSpeedDial: () -> Unit,
    onTabSelect: (HomeNavTab) -> Unit
) {
    val targetProgress = when (selectedTab) {
        HomeNavTab.HOME -> 0f
        HomeNavTab.PEOPLE -> 1f
        HomeNavTab.TIMELINE -> 2f
    }
    val pillProgressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = pillAnimationSpec,
        label = "dockSlidingPillProgress"
    )

    val touchSlot0WidthDp = if (selectedTab == HomeNavTab.HOME) dimensions.activePillWidthDp else dimensions.inactiveSlotWidthDp
    val touchSlot1WidthDp = if (selectedTab == HomeNavTab.PEOPLE) dimensions.activePillWidthDp else dimensions.inactiveSlotWidthDp
    val touchSlot2WidthDp = if (selectedTab == HomeNavTab.TIMELINE) dimensions.activePillWidthDp else dimensions.inactiveSlotWidthDp
    val pillColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp)
            .drawBehind {
                val progress = pillProgressState.value.coerceIn(0f, 2f)
                val (left, right) = DockMotionConfig.calculateConstantWidthThreeTabPillBounds(
                    activePillWidth = dimensions.activePillWidthDp.dp.toPx(),
                    inactiveSlotWidth = dimensions.inactiveSlotWidthDp.dp.toPx(),
                    progress = progress
                )
                val currentWidth = (right - left).coerceAtLeast(0f)
                val vPaddingPx = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp.toPx()
                val top = vPaddingPx
                val height = size.height - (vPaddingPx * 2f)
                val radius = height / 2f
                drawRoundRect(
                    color = pillColor,
                    topLeft = Offset(left, top),
                    size = Size(currentWidth, height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
    ) {
        // Foreground Single TabsRow (constant-width active pill + icon-only inactive slots)
        TabsRow(
            selectedTab = selectedTab,
            peopleLabel = peopleTabLabel,
            dimensions = dimensions,
            pillProgressProvider = { pillProgressState.value }
        )

        // Interactive Touch Layer on top (48dp+ accessible touch target)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(touchSlot0WidthDp.dp)
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
                    .width(touchSlot1WidthDp.dp)
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
            Box(
                modifier = Modifier
                    .width(touchSlot2WidthDp.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(25.dp))
                    .semantics {
                        role = Role.Tab
                        selected = selectedTab == HomeNavTab.TIMELINE
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (isSpeedDialOpen) {
                                onToggleSpeedDial()
                            }
                            onTabSelect(HomeNavTab.TIMELINE)
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
    dimensions: ThreeTabDockDimensions,
    pillProgressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val activePillWidthPx = with(density) { dimensions.activePillWidthDp.dp.toPx() }
    val inactiveSlotWidthPx = with(density) { dimensions.inactiveSlotWidthDp.dp.toPx() }

    Layout(
        content = {
            // Slot 0 (Home)
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                NavTabItemContent(
                    tab = HomeNavTab.HOME,
                    customLabel = null,
                    isSelected = selectedTab == HomeNavTab.HOME,
                    icon = DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, isSelected = selectedTab == HomeNavTab.HOME)
                )
            }

            // Slot 1 (Relationship / People Tab)
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                NavTabItemContent(
                    tab = HomeNavTab.PEOPLE,
                    customLabel = peopleLabel,
                    isSelected = selectedTab == HomeNavTab.PEOPLE,
                    icon = DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, isSelected = selectedTab == HomeNavTab.PEOPLE)
                )
            }

            // Slot 2 (Timeline)
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                NavTabItemContent(
                    tab = HomeNavTab.TIMELINE,
                    customLabel = null,
                    isSelected = selectedTab == HomeNavTab.TIMELINE,
                    icon = DockMotionConfig.resolveTabIcon(HomeNavTab.TIMELINE, isSelected = selectedTab == HomeNavTab.TIMELINE)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { measurables, constraints ->
        val progress = pillProgressProvider()
        val p = progress.coerceIn(0f, 2f)
        val delta = activePillWidthPx - inactiveSlotWidthPx
        val w0 = if (p <= 1f) 1f - p else 0f
        val w1 = if (p <= 1f) p else 2f - p
        val w2 = if (p <= 1f) 0f else p - 1f

        val width0 = (inactiveSlotWidthPx + delta * w0).roundToInt()
        val width1 = (inactiveSlotWidthPx + delta * w1).roundToInt()
        val width2 = (inactiveSlotWidthPx + delta * w2).roundToInt()

        val placeable0 = measurables[0].measure(
            Constraints.fixed(width0, constraints.maxHeight)
        )
        val placeable1 = measurables[1].measure(
            Constraints.fixed(width1, constraints.maxHeight)
        )
        val placeable2 = measurables[2].measure(
            Constraints.fixed(width2, constraints.maxHeight)
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable0.place(0, 0)
            placeable1.place(width0, 0)
            placeable2.place(width0 + width1, 0)
        }
    }
}

@Composable
private fun NavTabItemContent(
    tab: HomeNavTab,
    customLabel: String?,
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val displayLabel = customLabel ?: tab.label
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = DockMotionConfig.SCREEN_FADE_DURATION_MS,
            easing = MotionTokens.M3EmphasizedEasing
        ),
        label = "navTabContentColor"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayLabel,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .aspectRatio(1f)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = MotionTokens.tabTransitionTween()) +
                        expandHorizontally(
                            animationSpec = MotionTokens.tabTransitionSizeTween(),
                            expandFrom = Alignment.Start
                        ),
                exit = fadeOut(animationSpec = MotionTokens.tabTransitionTween()) +
                       shrinkHorizontally(
                           animationSpec = MotionTokens.tabTransitionSizeTween(),
                           shrinkTowards = Alignment.Start
                       )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayLabel,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = contentColor
                    )
                }
            }
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

@Preview(name = "Speed Dial Chip Preview", showBackground = true)
@Composable
private fun SpeedDialChipPreview() {
    SpeedDialChip(
        label = "Add Contact",
        icon = Icons.Rounded.PersonAdd,
        onClick = {}
    )
}

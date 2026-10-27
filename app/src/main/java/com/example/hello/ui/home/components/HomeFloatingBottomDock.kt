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
    pillAnimationSpec: AnimationSpec<Float> = MotionTokens.dockPillProgressSpring()
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
            shape = RoundedCornerShape(18.dp)
        )
    } else {
        Modifier
    }

    // Bottom Row: [ Floating Pill Dock ] + [ Quick-Add FAB with stacked Speed Dial ]
    // Centered horizontally on the screen as a cohesive, balanced unit
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            modifier = Modifier
                .width(dimensions.dockWidthDp.dp)
                .height(60.dp)
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            ) {
                val targetProgress = if (selectedTab == HomeNavTab.HOME) 0f else 1f
                val pillProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = pillAnimationSpec,
                    label = "dockSlidingPillProgress"
                )

                val homeWeight = dimensions.homePillWidthDp + 8f
                val relWeight = dimensions.relationshipPillWidthDp + 8f

                // Base Layer: Inactive styled tabs (onSurfaceVariant color, identical typography)
                TabsRow(
                    selectedTab = selectedTab,
                    peopleLabel = peopleTabLabel,
                    homeWeight = homeWeight,
                    relationshipWeight = relWeight,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Active Layer: SecondaryContainer pill background + active styled tabs (onSecondaryContainer),
                // clipped dynamically in the GPU draw phase to the moving centered pill shape.
                val pillShape = remember(dimensions.homePillWidthDp, dimensions.relationshipPillWidthDp) {
                    CenteredPillClipShape(
                        progressProvider = { pillProgress },
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
                        homeWeight = homeWeight,
                        relationshipWeight = relWeight,
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
                            .weight(homeWeight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(25.dp))
                            .semantics {
                                role = Role.Tab
                                selected = selectedTab == HomeNavTab.HOME
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // Only the sliding pill appears; no click ripple/background
                                onClick = { onTabSelect(HomeNavTab.HOME) }
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(relWeight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(25.dp))
                            .semantics {
                                role = Role.Tab
                                selected = selectedTab == HomeNavTab.PEOPLE
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelect(HomeNavTab.PEOPLE) }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Quick-Add FAB + Speed Dial Popups directly anchored above it
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = isSpeedDialOpen,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
                ) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedDialItem(
                        label = "Met Again",
                        icon = Icons.Outlined.EditNote,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onSpeedDialAction(SpeedDialAction.LOG_ENCOUNTER) }
                    )
                    SpeedDialItem(
                        label = addContactLabel,
                        icon = Icons.Outlined.PersonAdd,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { onSpeedDialAction(SpeedDialAction.NEW_CONTACT) }
                    )
                }
            }

            FloatingActionButton(
                onClick = onToggleSpeedDial,
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = DockMotionConfig.DOCK_SHADOW_ELEVATION_DP.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = warmShadowAmbient,
                        spotColor = warmShadowSpot
                    )
                    .then(fabBorderModifier),
                shape = RoundedCornerShape(18.dp),
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

@Composable
private fun TabsRow(
    selectedTab: HomeNavTab,
    peopleLabel: String = "People",
    homeWeight: Float,
    relationshipWeight: Float,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slot 0 (Home): weighted slot, permanently centered at homeCenter
        Box(
            modifier = Modifier
                .weight(homeWeight)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            NavTabItemContent(
                tab = HomeNavTab.HOME,
                customLabel = null,
                isSelected = selectedTab == HomeNavTab.HOME,
                icon = DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, selectedTab == HomeNavTab.HOME),
                iconColor = iconColor,
                textColor = textColor
            )
        }

        // Slot 1 (Relationship): weighted slot, permanently centered at relCenter
        Box(
            modifier = Modifier
                .weight(relationshipWeight)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            NavTabItemContent(
                tab = HomeNavTab.PEOPLE,
                customLabel = peopleLabel,
                isSelected = selectedTab == HomeNavTab.PEOPLE,
                icon = DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, selectedTab == HomeNavTab.PEOPLE),
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
    isSelected: Boolean,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val displayLabel = customLabel ?: tab.label

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = MotionTokens.tabTransitionTween()) +
                        expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = MotionTokens.tabTransitionSizeTween()
                        ) +
                        slideInHorizontally(
                            initialOffsetX = { (it * DockMotionConfig.ICON_ENTER_OFFSET_FRACTION).toInt() },
                            animationSpec = MotionTokens.tabTransitionOffsetTween()
                        ),
                exit = fadeOut(animationSpec = MotionTokens.tabTransitionTween()) +
                       shrinkHorizontally(
                           shrinkTowards = Alignment.Start,
                           animationSpec = MotionTokens.tabTransitionSizeTween()
                       ) +
                       slideOutHorizontally(
                           targetOffsetX = { (it * DockMotionConfig.ICON_EXIT_OFFSET_FRACTION).toInt() },
                           animationSpec = MotionTokens.tabTransitionOffsetTween()
                       )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null, // Label is displayed by adjacent Text to avoid duplicate TalkBack readout
                            tint = iconColor,
                            modifier = Modifier
                                .size(22.dp)
                                .aspectRatio(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Text(
                text = displayLabel,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
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

package com.example.hello.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    pillAnimationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // Speed Dial Popups (Animated above the FAB)
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
                modifier = Modifier
                    .padding(end = 20.dp, bottom = 12.dp),
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
                    label = "Add Contact",
                    icon = Icons.Outlined.PersonAdd,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onSpeedDialAction(SpeedDialAction.NEW_CONTACT) }
                )
            }
        }

        // Bottom Row: [ Floating Pill Dock ] + [ Rounded-Square Quick-Add FAB ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Organic Floating Pill Navigation Dock with Draw-Phase Clip-Path Reveal
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(30.dp)
                    )
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(30.dp)),
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

                    // Base Layer: Inactive styled tabs (onSurfaceVariant color, identical typography)
                    TabsRow(
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Active Layer: SecondaryContainer pill background + active styled tabs (onSecondaryContainer),
                    // clipped dynamically in the GPU draw phase to the moving pill shape.
                    val pillShape = remember { PillClipShape({ pillProgress }) }
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
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Interactive Touch Layer on top (zero visual decoration, indication = null)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(25.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null, // Only the sliding pill appears; no click ripple/background
                                    onClick = { onTabSelect(HomeNavTab.HOME) }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(25.dp))
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

            // 2. Separate Rounded-Square Quick-Add FAB with Primary Terracotta color
            val fabRotation by animateFloatAsState(
                targetValue = if (isSpeedDialOpen) 45f else 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                label = "fabRotate"
            )

            FloatingActionButton(
                onClick = onToggleSpeedDial,
                modifier = Modifier
                    .size(60.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
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
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTabItemContent(
            tab = HomeNavTab.HOME,
            icon = Icons.Outlined.Home,
            iconColor = iconColor,
            textColor = textColor,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        NavTabItemContent(
            tab = HomeNavTab.PEOPLE,
            icon = Icons.Outlined.People,
            iconColor = iconColor,
            textColor = textColor,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun NavTabItemContent(
    tab: HomeNavTab,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = tab.label,
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
 * Reusable zero-allocation shape that dynamically outlines the sliding pill in the draw phase.
 */
private class PillClipShape(
    private val progressProvider: () -> Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val halfWidth = size.width / 2f
        val left = progressProvider() * halfWidth
        val radius = with(density) { 25.dp.toPx() }
        return Outline.Rounded(
            RoundRect(
                left = left,
                top = 0f,
                right = left + halfWidth,
                bottom = size.height,
                cornerRadius = CornerRadius(radius, radius)
            )
        )
    }
}

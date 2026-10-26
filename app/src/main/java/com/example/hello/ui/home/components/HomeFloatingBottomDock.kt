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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
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

    val density = LocalDensity.current
    var homeContentWidthPx by remember { mutableFloatStateOf(0f) }
    var peopleContentWidthPx by remember(peopleTabLabel) { mutableFloatStateOf(0f) }

    val defaultHomeContentWidthDp = 68f
    val defaultPeopleContentWidthDp = DockMotionConfig.estimateContentWidthDp(peopleTabLabel).toFloat()

    val homeWidthDp = if (homeContentWidthPx > 0f) with(density) { homeContentWidthPx.toDp().value } else defaultHomeContentWidthDp
    val peopleWidthDp = if (peopleContentWidthPx > 0f) with(density) { peopleContentWidthPx.toDp().value } else defaultPeopleContentWidthDp

    val stableDockWidthDp = DockMotionConfig.calculateStableDockWidthDp(
        relationshipContentWidthDp = peopleWidthDp,
        homeContentWidthDp = homeWidthDp
    ).dp

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
                .width(stableDockWidthDp)
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

                // Base Layer: Inactive styled tabs (onSurfaceVariant color, identical typography)
                TabsRow(
                    selectedTab = selectedTab,
                    pillProgress = pillProgress,
                    peopleLabel = peopleTabLabel,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    homeContentWidthPx = homeContentWidthPx,
                    peopleContentWidthPx = peopleContentWidthPx,
                    onHomeContentWidth = { homeContentWidthPx = it },
                    onPeopleContentWidth = { peopleContentWidthPx = it }
                )

                // Active Layer: SecondaryContainer pill background + active styled tabs (onSecondaryContainer),
                // clipped dynamically in the GPU draw phase to the moving content-hugging pill shape.
                val pillShape = remember {
                    PillClipShape(
                        progressProvider = { pillProgress },
                        homeWidthProvider = { homeContentWidthPx },
                        peopleWidthProvider = { peopleContentWidthPx },
                        peopleLabelProvider = { peopleTabLabel }
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
                        pillProgress = pillProgress,
                        peopleLabel = peopleTabLabel,
                        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        homeContentWidthPx = homeContentWidthPx,
                        peopleContentWidthPx = peopleContentWidthPx
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
                                indication = null, // Only the sliding pill appears; no click ripple/background
                                onClick = { onTabSelect(HomeNavTab.HOME) }
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
    pillProgress: Float,
    peopleLabel: String = "People",
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    homeContentWidthPx: Float = 0f,
    peopleContentWidthPx: Float = 0f,
    onHomeContentWidth: ((Float) -> Unit)? = null,
    onPeopleContentWidth: ((Float) -> Unit)? = null
) {
    Layout(
        content = {
            NavTabItemContent(
                tab = HomeNavTab.HOME,
                customLabel = null,
                isSelected = selectedTab == HomeNavTab.HOME,
                icon = DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, selectedTab == HomeNavTab.HOME),
                iconColor = iconColor,
                textColor = textColor,
                onContentWidthMeasured = onHomeContentWidth
            )
            NavTabItemContent(
                tab = HomeNavTab.PEOPLE,
                customLabel = peopleLabel,
                isSelected = selectedTab == HomeNavTab.PEOPLE,
                icon = DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, selectedTab == HomeNavTab.PEOPLE),
                iconColor = iconColor,
                textColor = textColor,
                onContentWidthMeasured = onPeopleContentWidth
            )
        },
        modifier = modifier.fillMaxSize()
    ) { measurables, constraints ->
        val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
        val homePlaceable = measurables[0].measure(unconstrained)
        val peoplePlaceable = measurables[1].measure(unconstrained)

        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val vPaddingPx = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp.toPx()
        val hPaddingPx = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.dp.toPx()
        val pillHeight = (containerHeight - (2f * vPaddingPx)).coerceAtLeast(0f)

        // Estimated content widths if not yet measured
        val defaultHomeWidth = 68.dp.toPx()
        val defaultPeopleWidth = DockMotionConfig.estimateContentWidthDp(peopleLabel).dp.toPx()

        val effectiveHomeWidth = homeContentWidthPx.takeIf { it > 0f }
            ?: (if (selectedTab == HomeNavTab.HOME && homePlaceable.width > 0) homePlaceable.width.toFloat() else defaultHomeWidth)
        val effectivePeopleWidth = peopleContentWidthPx.takeIf { it > 0f }
            ?: (if (selectedTab == HomeNavTab.PEOPLE && peoplePlaceable.width > 0) peoplePlaceable.width.toFloat() else defaultPeopleWidth)

        val homePillWidth = effectiveHomeWidth + (2f * hPaddingPx)
        val peoplePillWidth = effectivePeopleWidth + (2f * hPaddingPx)

        // Center coordinates for Home tab:
        // When active: centered in Home pill (vPaddingPx .. vPaddingPx + homePillWidth)
        // When inactive: centered in 1:1 icon slot (vPaddingPx .. vPaddingPx + pillHeight)
        val homeActiveCenterX = vPaddingPx + (homePillWidth / 2f)
        val homeInactiveCenterX = vPaddingPx + (pillHeight / 2f)
        val homeCenterX = homeActiveCenterX + (homeInactiveCenterX - homeActiveCenterX) * pillProgress

        // Center coordinates for Relationship tab:
        // When inactive: centered in 1:1 icon slot (containerWidth - vPaddingPx - pillHeight .. containerWidth - vPaddingPx)
        // When active: centered in Relationship pill (containerWidth - vPaddingPx - peoplePillWidth .. containerWidth - vPaddingPx)
        val peopleInactiveCenterX = containerWidth - vPaddingPx - (pillHeight / 2f)
        val peopleActiveCenterX = containerWidth - vPaddingPx - (peoplePillWidth / 2f)
        val peopleCenterX = peopleInactiveCenterX + (peopleActiveCenterX - peopleInactiveCenterX) * pillProgress

        val homeX = (homeCenterX - (homePlaceable.width / 2f)).toInt()
        val homeY = ((containerHeight - homePlaceable.height) / 2f).toInt()

        val peopleX = (peopleCenterX - (peoplePlaceable.width / 2f)).toInt()
        val peopleY = ((containerHeight - peoplePlaceable.height) / 2f).toInt()

        layout(constraints.maxWidth, constraints.maxHeight) {
            homePlaceable.placeRelative(homeX, homeY)
            peoplePlaceable.placeRelative(peopleX, peopleY)
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
    modifier: Modifier = Modifier,
    onContentWidthMeasured: ((Float) -> Unit)? = null
) {
    val displayLabel = customLabel ?: tab.label
    var lastExpandedWidthPx by remember(displayLabel) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.onSizeChanged { size ->
                if (isSelected && size.width > 0) {
                    lastExpandedWidthPx = size.width.toFloat()
                    onContentWidthMeasured?.invoke(lastExpandedWidthPx)
                } else if (lastExpandedWidthPx > 0f) {
                    onContentWidthMeasured?.invoke(lastExpandedWidthPx)
                }
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = displayLabel,
                    tint = iconColor,
                    modifier = Modifier
                        .size(22.dp)
                        .aspectRatio(1f)
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = MotionTokens.tabTransitionTween()) +
                        expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = MotionTokens.tabTransitionSizeTween()
                        ) +
                        slideInHorizontally(
                            initialOffsetX = { (it * DockMotionConfig.TEXT_ENTER_OFFSET_FRACTION).toInt() },
                            animationSpec = MotionTokens.tabTransitionOffsetTween()
                        ),
                exit = fadeOut(animationSpec = MotionTokens.tabTransitionTween()) +
                       shrinkHorizontally(
                           shrinkTowards = Alignment.End,
                           animationSpec = MotionTokens.tabTransitionSizeTween()
                       ) +
                       slideOutHorizontally(
                           targetOffsetX = { (it * DockMotionConfig.TEXT_EXIT_OFFSET_FRACTION).toInt() },
                           animationSpec = MotionTokens.tabTransitionOffsetTween()
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
                        color = textColor
                    )
                }
            }
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
 * Reusable zero-allocation shape that dynamically outlines the content-hugging sliding pill in the draw phase.
 */
private class PillClipShape(
    private val progressProvider: () -> Float,
    private val homeWidthProvider: () -> Float,
    private val peopleWidthProvider: () -> Float,
    private val peopleLabelProvider: () -> String,
    private val verticalPaddingDp: Dp = DockMotionConfig.PILL_VERTICAL_PADDING_DP.dp,
    private val horizontalPaddingDp: Dp = DockMotionConfig.PILL_CONTENT_HORIZONTAL_PADDING_DP.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val progress = progressProvider()

        val vPaddingPx = with(density) { verticalPaddingDp.toPx() }
        val hPaddingPx = with(density) { horizontalPaddingDp.toPx() }

        val defaultHomeWidth = with(density) { 68.dp.toPx() }
        val defaultPeopleWidth = with(density) {
            DockMotionConfig.estimateContentWidthDp(peopleLabelProvider()).dp.toPx()
        }

        val measuredHome = homeWidthProvider().takeIf { it > 0f } ?: defaultHomeWidth
        val measuredPeople = peopleWidthProvider().takeIf { it > 0f } ?: defaultPeopleWidth

        val homePillWidth = measuredHome + (hPaddingPx * 2f)
        val peoplePillWidth = measuredPeople + (hPaddingPx * 2f)

        // Anchored positioning ensuring exact consistent margins:
        // When Home is selected: left padding == vPaddingPx, top == vPaddingPx, bottom == vPaddingPx
        val homeLeft = vPaddingPx
        val homeRight = (homeLeft + homePillWidth).coerceAtMost(size.width - vPaddingPx)

        // When Relationship tab is selected: right padding == vPaddingPx, top == vPaddingPx, bottom == vPaddingPx
        val peopleRight = (size.width - vPaddingPx).coerceAtLeast(vPaddingPx)
        val peopleLeft = (peopleRight - peoplePillWidth).coerceAtLeast(vPaddingPx)

        // Interpolate smoothly between tab 0 and tab 1 bounds
        val left = homeLeft + (peopleLeft - homeLeft) * progress
        val right = homeRight + (peopleRight - homeRight) * progress

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

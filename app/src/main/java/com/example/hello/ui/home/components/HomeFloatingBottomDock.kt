package com.example.hello.ui.home.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BLUR_QUANTIZATION_STEP_PX = 0.25f
private const val BLUR_CACHE_MAX_ENTRIES = 128

private val decalBlurEffectCache = object : LinkedHashMap<Int, BlurEffect>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, BlurEffect>?): Boolean {
        return size > BLUR_CACHE_MAX_ENTRIES
    }
}

private fun cachedDecalBlurEffect(blurPx: Float): BlurEffect {
    val bucket = (blurPx / BLUR_QUANTIZATION_STEP_PX).roundToInt().coerceAtLeast(1)
    synchronized(decalBlurEffectCache) {
        return decalBlurEffectCache.getOrPut(bucket) {
            val quantizedPx = bucket * BLUR_QUANTIZATION_STEP_PX
            BlurEffect(quantizedPx, quantizedPx, TileMode.Decal)
        }
    }
}

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
    pillAnimationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween(),
    overridePillProgress: Float? = null,
    profileMorphProgressProvider: () -> Float = { 0f },
    isProfileMorphReversing: () -> Boolean = { false },
    isProfileEncounterMode: Boolean = false,
    onProfileEncounterClick: () -> Unit = {},
    onProfileReminderClick: () -> Unit = {},
    pendingReminderCount: Int = 0,
    reminderSavedEventId: Long = 0L,
    profileContextKey: String? = null
) {
    // 1. Organic Floating Pill Navigation Dock with Draw-Phase Clip-Path Reveal & In-Place Profile Morph (ADR-0034)
    val haptics = LocalHelloHaptics.current
    val isReducedMotion = LocalReducedMotion.current
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
    val totalFootprintWidthDp = remember(dimensions.dockWidthDp) {
        DockMotionConfig.calculateTotalDockFootprintWidthDp(dimensions.dockWidthDp)
    }

    // Separate Rounded-Square Quick-Add FAB with matching elevation & ease-out rotation (snaps under reduced motion)
    val fabRotation by animateFloatAsState(
        targetValue = if (isSpeedDialOpen && !isProfileEncounterMode) 45f else 0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = 200, easing = MotionTokens.StrongEaseOut),
        label = "fabRotate"
    )

    val morphPillShape = remember(dimensions.dockWidthDp, profileMorphProgressProvider, isProfileMorphReversing) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val state = DockMotionConfig.calculateProfileMorphState(
                    progress = profileMorphProgressProvider(),
                    dockWidthDp = dimensions.dockWidthDp.toFloat(),
                    isReversing = isProfileMorphReversing()
                )
                val radiusPx = with(density) { state.pillCornerRadiusDp.dp.toPx() }
                    .coerceAtMost(minOf(size.width, size.height) / 2f)
                return Outline.Rounded(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                )
            }
        }
    }

    val fabBorderModifier = if (DockMotionConfig.HAS_DOCK_BORDER) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            shape = morphPillShape
        )
    } else {
        Modifier
    }

    val actionInteractionSource = remember { MutableInteractionSource() }
    val isActionPressed by actionInteractionSource.collectIsPressedAsState()
    val actionPressScale by animateFloatAsState(
        targetValue = if (isActionPressed && !isReducedMotion) 0.96f else 1f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = 140, easing = MotionTokens.StrongEaseOut),
        label = "actionPillPressScale"
    )
    val reminderInteractionSource = remember { MutableInteractionSource() }
    val isReminderPressed by reminderInteractionSource.collectIsPressedAsState()
    val reminderPressScale by animateFloatAsState(
        targetValue = if (isReminderPressed && !isReducedMotion) DockMotionConfig.PROFILE_REMINDER_PRESS_SCALE else 1f,
        animationSpec = if (isReducedMotion) {
            snap()
        } else if (isReminderPressed) {
            tween(durationMillis = 120, easing = MotionTokens.StrongEaseOut)
        } else {
            tween(durationMillis = 140, easing = MotionTokens.StrongEaseOut)
        },
        label = "reminderPillPressScale"
    )
    val reminderScope = rememberCoroutineScope()
    val bellRotation = remember { Animatable(0f) }
    var bellAnimJob by remember { mutableStateOf<Job?>(null) }
    var isBellSwingActive by remember { mutableStateOf(false) }

    var isShowingSavedConfirmation by remember { mutableStateOf(false) }
    val savedTransition = remember { Animatable(0f) }
    var lastHandledSavedEventId by remember { mutableLongStateOf(reminderSavedEventId) }
    var savedConfirmJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isReminderPressed, isReducedMotion) {
        if (isReducedMotion) {
            bellAnimJob?.cancel()
            isBellSwingActive = false
            bellRotation.snapTo(0f)
            return@LaunchedEffect
        }
        if (isReminderPressed) {
            isBellSwingActive = false
            bellAnimJob?.cancel()
            bellAnimJob = reminderScope.launch {
                bellRotation.animateTo(
                    targetValue = DockMotionConfig.PROFILE_REMINDER_PRESS_BELL_TILT_DEG,
                    animationSpec = tween(durationMillis = 120, easing = MotionTokens.StrongEaseOut)
                )
            }
        } else {
            delay(24L)
            if (!isBellSwingActive && bellRotation.value != 0f) {
                bellAnimJob?.cancel()
                bellAnimJob = reminderScope.launch {
                    bellRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 140, easing = MotionTokens.StrongEaseOut)
                    )
                }
            }
        }
    }

    LaunchedEffect(isProfileEncounterMode, profileContextKey) {
        if (!isProfileEncounterMode) {
            savedConfirmJob?.cancel()
            savedConfirmJob = null
            isShowingSavedConfirmation = false
            savedTransition.snapTo(0f)
            lastHandledSavedEventId = reminderSavedEventId
            bellAnimJob?.cancel()
            isBellSwingActive = false
            bellRotation.snapTo(0f)
        } else {
            savedConfirmJob?.cancel()
            savedConfirmJob = null
            isShowingSavedConfirmation = false
            savedTransition.snapTo(0f)
            lastHandledSavedEventId = reminderSavedEventId
        }
    }

    LaunchedEffect(reminderSavedEventId, isProfileEncounterMode, isReducedMotion) {
        if (isProfileEncounterMode && reminderSavedEventId > lastHandledSavedEventId) {
            lastHandledSavedEventId = reminderSavedEventId
            savedConfirmJob?.cancel()
            savedConfirmJob = reminderScope.launch {
                delay(DockMotionConfig.PROFILE_REMINDER_POST_SHEET_DELAY_MS)
                isShowingSavedConfirmation = true
                haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                savedTransition.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 120 else 180,
                        easing = MotionTokens.StrongEaseOut
                    )
                )
                delay(DockMotionConfig.PROFILE_REMINDER_SAVED_HOLD_MS)
                isShowingSavedConfirmation = false
                savedTransition.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = if (isReducedMotion) 120 else 220,
                        easing = MotionTokens.StrongEaseOut
                    )
                )
                if (!isReducedMotion) {
                    isBellSwingActive = true
                    bellAnimJob?.cancel()
                    bellAnimJob = reminderScope.launch {
                        bellRotation.animateTo(
                            targetValue = DockMotionConfig.PROFILE_REMINDER_SETTLE_BELL_NOD_DEG,
                            animationSpec = tween(durationMillis = 80, easing = MotionTokens.StrongEaseOut)
                        )
                        bellRotation.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f)
                        )
                        isBellSwingActive = false
                    }
                }
            }
        }
    }

    val showSpeedDialChips = isSpeedDialOpen && !isProfileEncounterMode

    // Bottom Row: [ Floating Navigation Dock ] + [ 1:1 Squircle Quick-Add FAB ]
    // morphing in place into the centered "+ Catching up?" Profile Encounter Pill on Contact Profile
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
                visible = showSpeedDialChips,
                enter = if (isReducedMotion) {
                    fadeIn(animationSpec = tween(durationMillis = 120, easing = MotionTokens.StrongEaseOut))
                } else {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 220, easing = MotionTokens.StrongEaseOut)
                    ) + fadeIn(animationSpec = tween(durationMillis = 180, easing = MotionTokens.StrongEaseOut))
                },
                exit = if (isReducedMotion) {
                    fadeOut(animationSpec = tween(durationMillis = 100, easing = MotionTokens.StrongEaseOut))
                } else {
                    slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 160, easing = MotionTokens.StrongEaseOut)
                    ) + fadeOut(animationSpec = tween(durationMillis = 140, easing = MotionTokens.StrongEaseOut))
                }
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    SpeedDialChip(
                        label = "Met Again",
                        icon = Icons.Rounded.EditNote,
                        onClick = {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onSpeedDialAction(SpeedDialAction.LOG_ENCOUNTER)
                        }
                    )
                    Spacer(modifier = Modifier.height(DockMotionConfig.SPEED_DIAL_STACK_GAP_DP.dp))
                }
            }

            // Lower Chip: Add Contact / Friend / Connection (New Contact Action)
            AnimatedVisibility(
                visible = showSpeedDialChips,
                enter = if (isReducedMotion) {
                    fadeIn(animationSpec = tween(durationMillis = 120, easing = MotionTokens.StrongEaseOut))
                } else {
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 190, easing = MotionTokens.StrongEaseOut)
                    ) + fadeIn(animationSpec = tween(durationMillis = 150, easing = MotionTokens.StrongEaseOut))
                },
                exit = if (isReducedMotion) {
                    fadeOut(animationSpec = tween(durationMillis = 100, easing = MotionTokens.StrongEaseOut))
                } else {
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(durationMillis = 150, easing = MotionTokens.StrongEaseOut)
                    ) + fadeOut(animationSpec = tween(durationMillis = 130, easing = MotionTokens.StrongEaseOut))
                }
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    SpeedDialChip(
                        label = addContactLabel,
                        icon = Icons.Rounded.PersonAdd,
                        onClick = {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onSpeedDialAction(SpeedDialAction.NEW_CONTACT)
                        }
                    )
                    Spacer(modifier = Modifier.height(DockMotionConfig.SPEED_DIAL_FAB_GAP_DP.dp))
                }
            }

            // Bottom Unit: Invariant total footprint (dockWidth + 14dp + 60dp) x 60dp
            // Dual-capsule in-place morph: continuous left capsule contracts 1:1 with interior blur
            // cross-morph while the solid opaque '+' -> '+ Catching up?' right pill sweeps left (ADR-0034, RAM-63)
            val resolvedNavSurfaceColor = DockMotionConfig.resolveDockSurfaceColor(MaterialTheme.colorScheme)
            val primaryPillColor = MaterialTheme.colorScheme.primary
            val reminderSurfaceColor by animateColorAsState(
                targetValue = if (isShowingSavedConfirmation && isProfileEncounterMode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    resolvedNavSurfaceColor
                },
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 120 else if (isShowingSavedConfirmation) 200 else 220,
                    easing = MotionTokens.StrongEaseOut
                ),
                label = "reminderPillSurfaceColor"
            )
            val reminderContentColor by animateColorAsState(
                targetValue = if (isShowingSavedConfirmation && isProfileEncounterMode) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                animationSpec = tween(
                    durationMillis = if (isReducedMotion) 120 else if (isShowingSavedConfirmation) 200 else 220,
                    easing = MotionTokens.StrongEaseOut
                ),
                label = "reminderPillContentColor"
            )
            val reminderAccessibilityDescription = when {
                isShowingSavedConfirmation -> "Reminder saved"
                pendingReminderCount == 1 -> "${DockMotionConfig.PROFILE_REMINDER_PILL_LABEL}, 1 active reminder. Set reminder"
                pendingReminderCount > 1 -> "${DockMotionConfig.PROFILE_REMINDER_PILL_LABEL}, $pendingReminderCount active reminders. Set reminder"
                else -> "${DockMotionConfig.PROFILE_REMINDER_PILL_LABEL}. Set reminder"
            }

            Layout(
                content = {
                    // Child 0: Single Continuous Left Capsule (3-Tab Navigation Pill <-> Profile Reminder Pill)
                    // Stays 100% solid & sharp while contracting 1:1 with the right pill (locked 14dp gap)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val pressScale = if (isProfileEncounterMode || profileMorphProgressProvider() >= 0.5f) {
                                    reminderPressScale
                                } else {
                                    1f
                                }
                                scaleX = pressScale
                                scaleY = pressScale
                                shadowElevation = DockMotionConfig.DOCK_SHADOW_ELEVATION_DP.dp.toPx()
                                ambientShadowColor = warmShadowAmbient
                                spotShadowColor = warmShadowSpot
                                shape = RoundedCornerShape(30.dp)
                                clip = true
                            }
                            .background(reminderSurfaceColor)
                            .then(dockBorderModifier),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Layer A: Outgoing/Returning 3-Tab Navigation Interior (blurs, slides, and feathers directionally)
                        Box(
                            modifier = Modifier
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .width(dimensions.dockWidthDp.dp)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    val morphState = DockMotionConfig.calculateProfileMorphState(
                                        progress = profileMorphProgressProvider(),
                                        dockWidthDp = dimensions.dockWidthDp.toFloat(),
                                        isReversing = isProfileMorphReversing()
                                    )
                                    alpha = morphState.navPillAlpha
                                    translationX = if (isReducedMotion) 0f else morphState.navPillSlideOffsetDp.dp.toPx()
                                    compositingStrategy = if (morphState.navPillAlpha in 0.001f..0.999f) {
                                        CompositingStrategy.Offscreen
                                    } else {
                                        CompositingStrategy.Auto
                                    }
                                    clip = false
                                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val blurPx = morphState.navPillBlurDp.dp.toPx()
                                        renderEffect = if (blurPx > 0.25f) {
                                            cachedDecalBlurEffect(blurPx)
                                        } else {
                                            null
                                        }
                                    }
                                }
                                .drawWithContent {
                                    drawContent()
                                    val morphState = DockMotionConfig.calculateProfileMorphState(
                                        progress = profileMorphProgressProvider(),
                                        dockWidthDp = dimensions.dockWidthDp.toFloat(),
                                        isReversing = isProfileMorphReversing()
                                    )
                                    if (morphState.navPillAlpha in 0.001f..0.999f && size.width > 1f) {
                                        val featherPx = DockMotionConfig.PROFILE_MORPH_LABEL_FEATHER_DP.dp.toPx()
                                            .coerceAtMost(size.width * 0.45f)
                                        val colorStops = if (isProfileMorphReversing()) {
                                            val fadeEnd = (featherPx / size.width).coerceIn(0f, 1f)
                                            arrayOf(
                                                0.0f to Color.Transparent,
                                                fadeEnd to Color.Black,
                                                1.0f to Color.Black
                                            )
                                        } else {
                                            val fadeStart = ((size.width - featherPx) / size.width).coerceIn(0f, 1f)
                                            arrayOf(
                                                0.0f to Color.Black,
                                                fadeStart to Color.Black,
                                                1.0f to Color.Transparent
                                            )
                                        }
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colorStops = colorStops,
                                                startX = 0f,
                                                endX = size.width
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                        ) {
                            DockTabsLayer(
                                selectedTab = selectedTab,
                                peopleTabLabel = peopleTabLabel,
                                dimensions = dimensions,
                                pillAnimationSpec = if (isReducedMotion) snap() else pillAnimationSpec,
                                overridePillProgress = overridePillProgress,
                                isSpeedDialOpen = isSpeedDialOpen,
                                onToggleSpeedDial = onToggleSpeedDial,
                                onTabSelect = onTabSelect,
                                isInteractive = !isProfileEncounterMode
                            )
                        }

                        // Layer B: Incoming Profile Reminder Interior (center-aligned inside contracting capsule, blurs into focus)
                        val reminderClickModifier = if (isProfileEncounterMode) {
                            Modifier
                                .semantics {
                                    role = Role.Button
                                    contentDescription = reminderAccessibilityDescription
                                }
                                .clickable(
                                    interactionSource = reminderInteractionSource,
                                    indication = LocalIndication.current,
                                    onClick = {
                                        haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                        if (isShowingSavedConfirmation || savedTransition.value > 0.01f) {
                                            savedConfirmJob?.cancel()
                                            savedConfirmJob = null
                                            isShowingSavedConfirmation = false
                                            reminderScope.launch {
                                                savedTransition.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(
                                                        durationMillis = 120,
                                                        easing = MotionTokens.StrongEaseOut
                                                    )
                                                )
                                            }
                                        }
                                        if (!isReducedMotion) {
                                            isBellSwingActive = true
                                            bellAnimJob?.cancel()
                                            bellAnimJob = reminderScope.launch {
                                                bellRotation.animateTo(
                                                    targetValue = DockMotionConfig.PROFILE_REMINDER_RELEASE_BELL_SWING_DEG,
                                                    animationSpec = tween(
                                                        durationMillis = 90,
                                                        easing = MotionTokens.StrongEaseOut
                                                    )
                                                )
                                                bellRotation.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.55f,
                                                        stiffness = 480f
                                                    )
                                                )
                                                isBellSwingActive = false
                                            }
                                        }
                                        onProfileReminderClick()
                                    }
                                )
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(reminderClickModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    val morphState = DockMotionConfig.calculateProfileMorphState(
                                        progress = profileMorphProgressProvider(),
                                        dockWidthDp = dimensions.dockWidthDp.toFloat(),
                                        isReversing = isProfileMorphReversing()
                                    )
                                    val contentScale = if (isReducedMotion) 1f else morphState.reminderPillScale
                                    alpha = morphState.reminderPillAlpha
                                    translationX = if (isReducedMotion) 0f else morphState.reminderPillSlideOffsetDp.dp.toPx()
                                    scaleX = contentScale
                                    scaleY = contentScale
                                    clip = false
                                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val blurPx = morphState.reminderPillBlurDp.dp.toPx()
                                        renderEffect = if (blurPx > 0.25f) {
                                            cachedDecalBlurEffect(blurPx)
                                        } else {
                                            null
                                        }
                                    }
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileReminderPillContent(
                                    hasActiveReminders = pendingReminderCount > 0,
                                    savedTransitionProvider = { savedTransition.value },
                                    bellRotationProvider = { bellRotation.value },
                                    contentColor = reminderContentColor,
                                    surfaceCutoutColor = reminderSurfaceColor,
                                    activeDotColor = MaterialTheme.colorScheme.primary,
                                    isReducedMotion = isReducedMotion,
                                    labelStateProvider = {
                                        val s = DockMotionConfig.calculateProfileMorphState(
                                            progress = profileMorphProgressProvider(),
                                            dockWidthDp = dimensions.dockWidthDp.toFloat(),
                                            isReversing = isProfileMorphReversing()
                                        )
                                        Triple(s.reminderLabelRevealWeight, s.reminderLabelAlpha, s.reminderLabelSlideOffsetDp)
                                    }
                                )
                            }
                        }
                    }

                    // Child 1: 100% Solid Opaque Right-Anchored '+' Pill (sweeps right-to-left at locked 60dp height, unchanged)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val morphState = DockMotionConfig.calculateProfileMorphState(
                                    progress = profileMorphProgressProvider(),
                                    dockWidthDp = dimensions.dockWidthDp.toFloat(),
                                    isReversing = isProfileMorphReversing()
                                )
                                val maxRadiusDp = minOf(morphState.pillWidthDp, morphState.pillHeightDp) / 2f
                                val radiusDp = morphState.pillCornerRadiusDp.coerceAtMost(maxRadiusDp)
                                scaleX = actionPressScale
                                scaleY = actionPressScale
                                shadowElevation = DockMotionConfig.DOCK_SHADOW_ELEVATION_DP.dp.toPx()
                                ambientShadowColor = warmShadowAmbient
                                spotShadowColor = warmShadowSpot
                                shape = RoundedCornerShape(radiusDp.dp)
                                clip = true
                            }
                            .background(primaryPillColor)
                            .then(fabBorderModifier)
                            .semantics {
                                role = Role.Button
                                contentDescription = when {
                                    isProfileEncounterMode -> "${DockMotionConfig.PROFILE_ENCOUNTER_PILL_LABEL} Log encounter"
                                    isSpeedDialOpen -> "Close speed dial"
                                    else -> "Quick capture"
                                }
                            }
                            .clickable(
                                interactionSource = actionInteractionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isProfileEncounterMode || profileMorphProgressProvider() >= 0.5f) {
                                        haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                        onProfileEncounterClick()
                                    } else {
                                        onToggleSpeedDial()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MorphActionPillContent(
                            fabRotation = fabRotation,
                            profileMorphProgressProvider = profileMorphProgressProvider,
                            isProfileMorphReversing = isProfileMorphReversing,
                            dockWidthDp = dimensions.dockWidthDp.toFloat(),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                modifier = Modifier
                    .width(totalFootprintWidthDp.dp)
                    .height(DockMotionConfig.DOCK_HEIGHT_DP.dp)
            ) { measurables, constraints ->
                val morphState = DockMotionConfig.calculateProfileMorphState(
                    progress = profileMorphProgressProvider(),
                    dockWidthDp = dimensions.dockWidthDp.toFloat(),
                    isReversing = isProfileMorphReversing()
                )
                val gapPx = DockMotionConfig.DOCK_FAB_GAP_DP.dp.roundToPx()
                val pillWidthPx = morphState.pillWidthDp.dp.roundToPx().coerceIn(1, constraints.maxWidth)
                val pillHeightPx = morphState.pillHeightDp.dp.roundToPx().coerceIn(1, constraints.maxHeight)
                val pillLeftPx = (constraints.maxWidth - pillWidthPx).coerceAtLeast(0)
                val leftCapsuleWidthPx = (pillLeftPx - gapPx).coerceIn(1, constraints.maxWidth)

                val leftCapsulePlaceable = measurables[0].measure(
                    Constraints.fixed(width = leftCapsuleWidthPx, height = pillHeightPx)
                )
                val actionPillPlaceable = measurables[1].measure(
                    Constraints.fixed(width = pillWidthPx, height = pillHeightPx)
                )
                val pillTopPx = ((constraints.maxHeight - pillHeightPx) / 2f).roundToInt()

                layout(constraints.maxWidth, constraints.maxHeight) {
                    leftCapsulePlaceable.placeRelative(0, pillTopPx)
                    actionPillPlaceable.placeRelative(pillLeftPx, pillTopPx)
                }
            }
        }
    }
}

@Composable
private fun MorphActionPillContent(
    fabRotation: Float,
    profileMorphProgressProvider: () -> Float,
    isProfileMorphReversing: () -> Boolean,
    dockWidthDp: Float,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    val progress = profileMorphProgressProvider().coerceIn(0f, 1f)
                    val state = DockMotionConfig.calculateProfileMorphState(
                        progress = progress,
                        dockWidthDp = dockWidthDp,
                        isReversing = isProfileMorphReversing()
                    )
                    val scale = state.plusIconSizeDp / 24f
                    scaleX = scale
                    scaleY = scale
                    rotationZ = fabRotation * (1f - progress)
                }
        )

        FeatheredRevealLabel(
            text = DockMotionConfig.PROFILE_ENCOUNTER_PILL_LABEL,
            spacerWidthDp = 8f,
            contentColor = contentColor,
            stateProvider = {
                val state = DockMotionConfig.calculateProfileMorphState(
                    progress = profileMorphProgressProvider(),
                    dockWidthDp = dockWidthDp,
                    isReversing = isProfileMorphReversing()
                )
                Triple(state.labelRevealWeight, state.labelAlpha, state.labelSlideOffsetDp)
            }
        )
    }
}

@Composable
private fun FeatheredRevealLabel(
    text: String,
    spacerWidthDp: Float,
    contentColor: Color,
    stateProvider: () -> Triple<Float, Float, Float>
) {
    Layout(
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    val (_, labelAlpha, slideOffsetDp) = stateProvider()
                    alpha = labelAlpha
                    translationX = slideOffsetDp.dp.toPx()
                }
            ) {
                Spacer(modifier = Modifier.width(spacerWidthDp.dp))
                Text(
                    text = text,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor
                )
            }
        },
        modifier = Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
            }
            .drawWithContent {
                drawContent()
                val revealWeight = stateProvider().first
                if (revealWeight in 0.001f..0.999f && size.width > 1f) {
                    val featherPx = DockMotionConfig.PROFILE_MORPH_LABEL_FEATHER_DP.dp.toPx()
                        .coerceAtMost(size.width * 0.6f)
                    val fadeStart = ((size.width - featherPx) / size.width).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black,
                                fadeStart to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            startX = 0f,
                            endX = size.width
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) { measurables, constraints ->
        val placeable = measurables[0].measure(
            constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        )
        val revealWeight = stateProvider().first
        val revealedWidth = (placeable.width * revealWeight).roundToInt()
        layout(revealedWidth, placeable.height) {
            if (revealedWidth > 0) {
                placeable.place(0, 0)
            }
        }
    }
}

@Composable
private fun ProfileReminderPillContent(
    hasActiveReminders: Boolean,
    savedTransitionProvider: () -> Float,
    bellRotationProvider: () -> Float,
    contentColor: Color,
    surfaceCutoutColor: Color,
    activeDotColor: Color,
    isReducedMotion: Boolean,
    labelStateProvider: () -> Triple<Float, Float, Float> = { Triple(1f, 1f, 0f) }
) {
    val activeProgressState = animateFloatAsState(
        targetValue = if (hasActiveReminders) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 120 else 180,
            easing = MotionTokens.StrongEaseOut
        ),
        label = "profileReminderActiveProgress"
    )
    val showRemind by remember(savedTransitionProvider) {
        derivedStateOf {
            val t = savedTransitionProvider().coerceIn(0f, 1f)
            (1f - (t * 1.25f)).coerceIn(0f, 1f) > 0.001f
        }
    }
    val showSaved by remember(savedTransitionProvider) {
        derivedStateOf {
            val t = savedTransitionProvider().coerceIn(0f, 1f)
            ((t - 0.15f) / 0.85f).coerceIn(0f, 1f) > 0.001f
        }
    }
    val showOutlineBell by remember {
        derivedStateOf { activeProgressState.value < 0.999f }
    }
    val showFilledBell by remember {
        derivedStateOf { activeProgressState.value > 0.001f }
    }

    Box(contentAlignment = Alignment.Center) {
        if (showRemind) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    val t = savedTransitionProvider().coerceIn(0f, 1f)
                    val remindAlpha = (1f - (t * 1.25f)).coerceIn(0f, 1f)
                    val remindScale = if (isReducedMotion) 1f else (1f - 0.06f * t)
                    alpha = remindAlpha
                    scaleX = remindScale
                    scaleY = remindScale
                    clip = false
                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val swapBlurDp = DockMotionConfig.PROFILE_REMINDER_CONTENT_SWAP_BLUR_DP * sin(t * PI).toFloat()
                        val blurPx = swapBlurDp.dp.toPx()
                        renderEffect = if (blurPx > 0.2f) {
                            cachedDecalBlurEffect(blurPx)
                        } else {
                            null
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.15f)
                            rotationZ = if (isReducedMotion) 0f else bellRotationProvider()
                            clip = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (showOutlineBell) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    val activeProgress = activeProgressState.value
                                    val outlineScale = if (isReducedMotion) 1f else (1f - 0.06f * activeProgress)
                                    alpha = (1f - activeProgress).coerceIn(0f, 1f)
                                    scaleX = outlineScale
                                    scaleY = outlineScale
                                    clip = false
                                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val bellSwapBlurDp = DockMotionConfig.PROFILE_REMINDER_CONTENT_SWAP_BLUR_DP * sin(activeProgress * PI).toFloat()
                                        val blurPx = bellSwapBlurDp.dp.toPx()
                                        renderEffect = if (blurPx > 0.2f) {
                                            cachedDecalBlurEffect(blurPx)
                                        } else {
                                            null
                                        }
                                    }
                                }
                        )
                    }
                    if (showFilledBell) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    val activeProgress = activeProgressState.value
                                    val filledScale = if (isReducedMotion) 1f else (0.94f + 0.06f * activeProgress)
                                    alpha = activeProgress.coerceIn(0f, 1f)
                                    scaleX = filledScale
                                    scaleY = filledScale
                                    clip = false
                                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val bellSwapBlurDp = DockMotionConfig.PROFILE_REMINDER_CONTENT_SWAP_BLUR_DP * sin(activeProgress * PI).toFloat()
                                        val blurPx = bellSwapBlurDp.dp.toPx()
                                        renderEffect = if (blurPx > 0.2f) {
                                            cachedDecalBlurEffect(blurPx)
                                        } else {
                                            null
                                        }
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                                .size(9.dp)
                                .graphicsLayer {
                                    val activeProgress = activeProgressState.value
                                    val dotScale = if (isReducedMotion) 1f else (0.85f + 0.15f * activeProgress)
                                    alpha = activeProgress.coerceIn(0f, 1f)
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .background(surfaceCutoutColor, CircleShape)
                                .padding(1.5.dp)
                                .background(activeDotColor, CircleShape)
                        )
                    }
                }
                FeatheredRevealLabel(
                    text = DockMotionConfig.PROFILE_REMINDER_PILL_LABEL,
                    spacerWidthDp = 6f,
                    contentColor = contentColor,
                    stateProvider = labelStateProvider
                )
            }
        }

        if (showSaved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    val t = savedTransitionProvider().coerceIn(0f, 1f)
                    val savedAlpha = ((t - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    val savedScale = if (isReducedMotion) 1f else (0.94f + 0.06f * t)
                    alpha = savedAlpha
                    scaleX = savedScale
                    scaleY = savedScale
                    clip = false
                    if (!isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val swapBlurDp = DockMotionConfig.PROFILE_REMINDER_CONTENT_SWAP_BLUR_DP * sin(t * PI).toFloat()
                        val blurPx = swapBlurDp.dp.toPx()
                        renderEffect = if (blurPx > 0.2f) {
                            cachedDecalBlurEffect(blurPx)
                        } else {
                            null
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = DockMotionConfig.PROFILE_REMINDER_SAVED_LABEL,
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

@Composable
private fun DockTabsLayer(
    selectedTab: HomeNavTab,
    peopleTabLabel: String,
    dimensions: ThreeTabDockDimensions,
    pillAnimationSpec: AnimationSpec<Float>,
    overridePillProgress: Float?,
    isSpeedDialOpen: Boolean,
    onToggleSpeedDial: () -> Unit,
    onTabSelect: (HomeNavTab) -> Unit,
    isInteractive: Boolean = true
) {
    val targetProgress = when (selectedTab) {
        HomeNavTab.HOME -> 0f
        HomeNavTab.PEOPLE -> 1f
        HomeNavTab.TIMELINE -> 2f
    }
    var lastStableTab by remember { mutableStateOf(selectedTab) }
    var previousTab by remember { mutableStateOf(selectedTab) }
    if (lastStableTab != selectedTab) {
        previousTab = lastStableTab
        lastStableTab = selectedTab
    }
    val pillAnimatable = remember { Animatable(targetProgress) }
    LaunchedEffect(overridePillProgress) {
        if (overridePillProgress != null) {
            pillAnimatable.snapTo(overridePillProgress.coerceIn(0f, 2f))
        }
    }
    LaunchedEffect(targetProgress, overridePillProgress, pillAnimationSpec) {
        if (overridePillProgress == null && pillAnimatable.targetValue != targetProgress) {
            pillAnimatable.animateTo(targetProgress, animationSpec = pillAnimationSpec)
            previousTab = selectedTab
        }
    }
    val pillProgressState = pillAnimatable.asState()
    val transitionFromTab = if (overridePillProgress != null) selectedTab else previousTab
    val transitionToTab = if (overridePillProgress != null) HomeNavTab.HOME else selectedTab

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
            peopleLabel = peopleTabLabel,
            dimensions = dimensions,
            pillProgressProvider = { pillProgressState.value },
            fromTab = transitionFromTab,
            toTab = transitionToTab
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
                        enabled = isInteractive,
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
                        enabled = isInteractive,
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
                        enabled = isInteractive,
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
    peopleLabel: String = "People",
    dimensions: ThreeTabDockDimensions,
    pillProgressProvider: () -> Float,
    fromTab: HomeNavTab,
    toTab: HomeNavTab,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val activePillWidthPx = with(density) { dimensions.activePillWidthDp.dp.toPx() }
    val inactiveSlotWidthPx = with(density) { dimensions.inactiveSlotWidthDp.dp.toPx() }
    val weightsProvider: () -> Triple<Float, Float, Float> = {
        DockMotionConfig.resolveThreeTabLabelWeights(
            progress = pillProgressProvider(),
            fromTab = fromTab,
            toTab = toTab
        )
    }

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
                    selectionWeightProvider = { weightsProvider().first }
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
                    selectionWeightProvider = { weightsProvider().second }
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
                    selectionWeightProvider = { weightsProvider().third }
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
    selectionWeightProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val displayLabel = customLabel ?: tab.label
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.onSecondaryContainer
    val outlinedIcon = remember(tab) { DockMotionConfig.tabOutlinedIcon(tab) }
    val filledIcon = remember(tab) { DockMotionConfig.tabFilledIcon(tab) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Draw-phase crossfade between Outlined & Filled icons (zero per-frame recompositions)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = outlinedIcon,
                    contentDescription = displayLabel,
                    tint = inactiveColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (1f - selectionWeightProvider().coerceIn(0f, 1f))
                        }
                )
                Icon(
                    imageVector = filledIcon,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = selectionWeightProvider().coerceIn(0f, 1f)
                        }
                )
            }

            Layout(
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer {
                            val w = selectionWeightProvider().coerceIn(0f, 1f)
                            alpha = ((w - 0.12f) / 0.88f).coerceIn(0f, 1f)
                        }
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayLabel,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = activeColor
                        )
                    }
                },
                modifier = Modifier.clipToBounds()
            ) { measurables, constraints ->
                val placeable = measurables[0].measure(
                    constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                )
                val w = selectionWeightProvider().coerceIn(0f, 1f)
                val revealedWidth = (placeable.width * w).roundToInt()
                layout(revealedWidth, placeable.height) {
                    if (revealedWidth > 0) {
                        placeable.place(0, 0)
                    }
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
        color = DockMotionConfig.resolveDockSurfaceColor(MaterialTheme.colorScheme),
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
    HelloTheme {
        SpeedDialChip(
            label = "Add Contact",
            icon = Icons.Rounded.PersonAdd,
            onClick = {}
        )
    }
}

@Preview(name = "Profile Morph: 0.0 Root Dock", showBackground = true, widthDp = 390)
@Composable
private fun ProfileMorphRootDockPreview() {
    HelloTheme {
        HomeFloatingBottomDock(
            selectedTab = HomeNavTab.PEOPLE,
            onTabSelect = {},
            isSpeedDialOpen = false,
            onToggleSpeedDial = {},
            onSpeedDialAction = {},
            profileMorphProgressProvider = { 0f },
            isProfileEncounterMode = false
        )
    }
}

@Preview(name = "Profile Morph: 0.16 Liquid Bridge", showBackground = true, widthDp = 390)
@Composable
private fun ProfileMorphLiquidBridgePreview() {
    HelloTheme {
        HomeFloatingBottomDock(
            selectedTab = HomeNavTab.PEOPLE,
            onTabSelect = {},
            isSpeedDialOpen = false,
            onToggleSpeedDial = {},
            onSpeedDialAction = {},
            profileMorphProgressProvider = { 0.16f },
            isProfileEncounterMode = true
        )
    }
}

@Preview(name = "Profile Morph: 0.52 Right-to-Left Swallow", showBackground = true, widthDp = 390)
@Composable
private fun ProfileMorphSwallowPreview() {
    HelloTheme {
        HomeFloatingBottomDock(
            selectedTab = HomeNavTab.PEOPLE,
            onTabSelect = {},
            isSpeedDialOpen = false,
            onToggleSpeedDial = {},
            onSpeedDialAction = {},
            profileMorphProgressProvider = { 0.52f },
            isProfileEncounterMode = true
        )
    }
}

@Preview(name = "Profile Morph: 1.0 Catching Up Pill", showBackground = true, widthDp = 390)
@Composable
private fun ProfileMorphPillPreview() {
    HelloTheme {
        HomeFloatingBottomDock(
            selectedTab = HomeNavTab.PEOPLE,
            onTabSelect = {},
            isSpeedDialOpen = false,
            onToggleSpeedDial = {},
            onSpeedDialAction = {},
            profileMorphProgressProvider = { 1f },
            isProfileEncounterMode = true
        )
    }
}

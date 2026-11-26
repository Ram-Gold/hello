package com.example.hello.ui.quickaction

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.predictiveSheetDismiss
import com.example.hello.ui.navigation.resolveSheetScrimAlpha
import com.example.hello.ui.quickaction.components.FlowAAddContactCard
import com.example.hello.ui.quickaction.components.FlowBLogEncounterSheet
import com.example.hello.ui.quickaction.components.LocationExplainerDialog
import com.example.hello.ui.quickaction.components.QuickCaptureComposerCard
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens

internal fun hasForegroundLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

@Composable
internal fun ForegroundLocationPermissionHandler(
    showExplainer: Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onDismissExplainer: () -> Unit
) {
    val context = LocalContext.current
    val currentOnPermissionResult by rememberUpdatedState(onPermissionResult)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        currentOnPermissionResult(granted)
    }

    val hasLocationPermission = showExplainer && hasForegroundLocationPermission(context)

    LaunchedEffect(showExplainer) {
        if (showExplainer && hasForegroundLocationPermission(context)) {
            currentOnPermissionResult(true)
        }
    }

    if (showExplainer && !hasLocationPermission) {
        LocationExplainerDialog(
            onAllowClick = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismissClick = onDismissExplainer
        )
    }
}

@Composable
fun QuickActionSheetHost(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Foreground Location In-App Explainer Dialog & Runtime Permission Launcher
    ForegroundLocationPermissionHandler(
        showExplainer = uiState.showLocationExplainer,
        onPermissionResult = { granted ->
            onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = granted))
        },
        onDismissExplainer = {
            onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
        }
    )

    val isReducedMotion = LocalReducedMotion.current
    var innerStepBackProgress by remember { mutableFloatStateOf(0f) }
    var sheetDismissProgress by remember { mutableFloatStateOf(0f) }
    var isPredictiveDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isOpen) {
        if (uiState.isOpen) {
            isPredictiveDismissing = false
            sheetDismissProgress = 0f
            innerStepBackProgress = 0f
        }
    }

    val hasOpenComposerSubOverlay = uiState.isCustomizeQuickAddOpen ||
        uiState.isPlusPopoverOpen ||
        uiState.activeDetailTray != null

    // Collapse sub-overlay (Customize -> '+' popover -> inline detail tray) before dismissing sheet
    ScrubbablePredictiveBackHandler(
        enabled = uiState.isOpen && hasOpenComposerSubOverlay,
        isReducedMotion = isReducedMotion,
        onProgressChanged = { innerStepBackProgress = it },
        finishOnCommit = false,
        onCommit = {
            when {
                uiState.isCustomizeQuickAddOpen -> onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false))
                uiState.isPlusPopoverOpen -> onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
                uiState.activeDetailTray != null -> onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
            }
        }
    )

    ScrubbablePredictiveBackHandler(
        enabled = uiState.isOpen && !hasOpenComposerSubOverlay,
        isReducedMotion = isReducedMotion,
        onProgressChanged = { sheetDismissProgress = it },
        finishOnCommit = true,
        baseFinishDurationMs = 140,
        onCommit = {
            isPredictiveDismissing = true
            onEvent(QuickActionUiEvent.OnDismiss)
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop
        AnimatedVisibility(
            visible = uiState.isOpen && uiState.currentMode != QuickActionMode.CHOICE,
            enter = fadeIn(tween(if (isReducedMotion) 0 else 140)),
            exit = if (isPredictiveDismissing || isReducedMotion) ExitTransition.None else fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = resolveSheetScrimAlpha(sheetDismissProgress, maxAlpha = 0.36f)))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        when {
                            uiState.isCustomizeQuickAddOpen -> onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false))
                            uiState.isPlusPopoverOpen -> onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
                            uiState.activeDetailTray != null -> onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                            else -> onEvent(QuickActionUiEvent.OnDismiss)
                        }
                    }
            )
        }

        // Unified Single-Surface Floating Capture Card (Add Contact & Met Again, ADR-0040, Emil Kowalski StrongEaseOut)
        AnimatedVisibility(
            visible = uiState.isOpen && uiState.currentMode != QuickActionMode.CHOICE,
            enter = if (isReducedMotion) {
                fadeIn(tween(120))
            } else {
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(190, easing = MotionTokens.StrongEaseOut)
                ) + fadeIn(tween(150, easing = MotionTokens.StrongEaseOut))
            },
            exit = if (isPredictiveDismissing) {
                ExitTransition.None
            } else if (isReducedMotion) {
                fadeOut(tween(90))
            } else {
                slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(130, easing = MotionTokens.StrongEaseOut)
                ) + fadeOut(tween(110, easing = MotionTokens.StrongEaseOut))
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .predictiveSheetDismiss { sheetDismissProgress }
                .navigationBarsPadding()
                .imePadding()
        ) {
            QuickCaptureComposerCard(
                uiState = uiState,
                onEvent = onEvent
            )
        }
    }
}

// -------------------------------------------------------------
// Previews
// -------------------------------------------------------------

@Preview(name = "Flow A: Step 1 Name", showBackground = true, widthDp = 390)
@Composable
fun FlowAStep1Preview() {
    val vm = QuickActionViewModel()
    val state = vm.uiState.value.copy(
        isOpen = true,
        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
        flowAStep = FlowAStep.NAME,
        newContactName = "Maya Patel"
    )
    HelloTheme(darkTheme = false) {
        FlowAAddContactCard(uiState = state, onEvent = {})
    }
}

@Preview(name = "Flow A: Step 3 Tags", showBackground = true, widthDp = 390)
@Composable
fun FlowAStep3Preview() {
    val vm = QuickActionViewModel()
    val state = vm.uiState.value.copy(
        isOpen = true,
        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
        flowAStep = FlowAStep.TAGS,
        selectedTagIds = setOf("tag-tech")
    )
    HelloTheme(darkTheme = false) {
        FlowAAddContactCard(uiState = state, onEvent = {})
    }
}

@Preview(name = "Flow B: Encounter Sheet", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390)
@Composable
fun FlowBEncounterSheetPreview() {
    val vm = QuickActionViewModel()
    val sampleContact = vm.uiState.value.allContacts.firstOrNull()
    val state = vm.uiState.value.copy(
        isOpen = true,
        currentMode = QuickActionMode.FLOW_B_LOG_ENCOUNTER,
        selectedContact = sampleContact,
        encounterNoteFlowB = "Caught up about her new job at Figma"
    )
    HelloTheme(darkTheme = true) {
        FlowBLogEncounterSheet(uiState = state, onEvent = {})
    }
}

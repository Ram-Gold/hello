package com.example.hello.ui.quickaction

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.ui.quickaction.components.FlowAAddContactCard
import com.example.hello.ui.quickaction.components.FlowBLogEncounterSheet
import com.example.hello.ui.quickaction.components.LocationExplainerDialog
import com.example.hello.ui.quickaction.components.QuickActionChoiceContent
import com.example.hello.ui.theme.HelloTheme

@Composable
fun QuickActionSheetHost(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Foreground Location In-App Explainer Dialog
    if (uiState.showLocationExplainer) {
        LocationExplainerDialog(
            onAllowClick = {
                onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))
            },
            onDismissClick = {
                onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
            }
        )
    }

    // Intercept system back when sheet is open
    BackHandler(enabled = uiState.isOpen) {
        when {
            uiState.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT && uiState.flowAStep != FlowAStep.NAME -> {
                onEvent(QuickActionUiEvent.OnPreviousStep)
            }
            uiState.currentMode == QuickActionMode.FLOW_B_LOG_ENCOUNTER && uiState.selectedContact != null -> {
                onEvent(QuickActionUiEvent.OnClearSelectedContact)
            }
            else -> {
                onEvent(QuickActionUiEvent.OnDismiss)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop
        AnimatedVisibility(
            visible = uiState.isOpen,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onEvent(QuickActionUiEvent.OnDismiss)
                    }
            )
        }

        // In-hierarchy bottom-anchored sheet (zero DialogWindow lag, synchronized with IME)
        AnimatedVisibility(
            visible = uiState.isOpen,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f)
            ) + fadeIn(tween(90)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(120)
            ) + fadeOut(tween(90)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Consume taps on sheet surface */ }
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                when (uiState.currentMode) {
                    QuickActionMode.CHOICE -> {
                        QuickActionChoiceContent(
                            onSelectAddContact = {
                                onEvent(QuickActionUiEvent.OnSelectAddContact)
                            },
                            onSelectLogEncounter = {
                                onEvent(QuickActionUiEvent.OnSelectLogEncounter)
                            },
                            onDismiss = {
                                onEvent(QuickActionUiEvent.OnDismiss)
                            }
                        )
                    }

                    QuickActionMode.FLOW_A_ADD_CONTACT -> {
                        FlowAAddContactCard(
                            uiState = uiState,
                            onEvent = onEvent
                        )
                    }

                    QuickActionMode.FLOW_B_LOG_ENCOUNTER -> {
                        FlowBLogEncounterSheet(
                            uiState = uiState,
                            onEvent = onEvent
                        )
                    }
                }
            }
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

@Preview(name = "Flow A: Step 3 Tags & Circles", showBackground = true, widthDp = 390)
@Composable
fun FlowAStep3Preview() {
    val vm = QuickActionViewModel()
    val state = vm.uiState.value.copy(
        isOpen = true,
        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
        flowAStep = FlowAStep.TAGS_AND_CIRCLES,
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

package com.example.hello.ui.quickaction.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.example.hello.ui.components.LocalReducedMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.ui.components.ContactParticipantsStrip
import com.example.hello.ui.components.HelloDatePickerDialog
import com.example.hello.ui.components.MultiContactPickerDialog
import com.example.hello.ui.components.SelectableAvatarBadge
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ENCOUNTER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val ENCOUNTER_CUSTOM_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowBLogEncounterSheet(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    predictiveBackProgress: Float = 0f
) {
    val isReducedMotion = LocalReducedMotion.current
    val haptics = LocalHelloHaptics.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val stageTransitionState = remember { SeekableTransitionState(uiState.selectedContact) }
    LaunchedEffect(uiState.selectedContact, predictiveBackProgress, isReducedMotion) {
        if (predictiveBackProgress > 0f && uiState.selectedContact != null && !isReducedMotion) {
            stageTransitionState.seekTo(fraction = predictiveBackProgress.coerceIn(0f, 1f), targetState = null)
        } else if (stageTransitionState.currentState != uiState.selectedContact || stageTransitionState.targetState != uiState.selectedContact) {
            if (isReducedMotion) {
                stageTransitionState.snapTo(uiState.selectedContact)
            } else {
                stageTransitionState.animateTo(uiState.selectedContact)
            }
        }
    }
    val stageTransition = rememberTransition(stageTransitionState, label = "flowBStageTransition")

    if (showDatePicker) {
        HelloDatePickerDialog(
            initialDate = uiState.encounterDate,
            onConfirm = { picked ->
                onEvent(QuickActionUiEvent.OnEncounterDateChange(picked))
            },
            onDismissRequest = { showDatePicker = false },
            restrictToPastDates = false
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.encounterTime.hour,
            initialMinute = uiState.encounterTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick catch-up time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(
                            QuickActionUiEvent.OnEncounterTimeChange(
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    val isEncounterStage = uiState.selectedContact != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Minimal Top Header: [ Circular 'x' / '<-' ] + [ Centered Minimal Dots ] + [ Circular '->' ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circular Close / Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (isEncounterStage) {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(QuickActionUiEvent.OnClearSelectedContact)
                        } else {
                            onEvent(QuickActionUiEvent.OnDismiss)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEncounterStage) {
                        Icons.AutoMirrored.Rounded.ArrowBack
                    } else {
                        Icons.Rounded.Close
                    },
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered Minimal 2-Dot Progression
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(false, true).forEach { stageTwo ->
                    val isCurrent = isEncounterStage == stageTwo
                    val isCompleted = isEncounterStage && !stageTwo
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 18.dp else 6.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                        label = "flowBDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = tween(150),
                        label = "flowBDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // Right Circular '->' Arrow Button (shown in Encounter stage or when multi-selecting contacts)
            val canAdvanceFromSelector = !isEncounterStage && uiState.isFlowBMultiSelectMode && uiState.selectedContacts.isNotEmpty()
            if (isEncounterStage || canAdvanceFromSelector) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isEncounterStage) {
                                onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)
                            } else {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(QuickActionUiEvent.OnConfirmMultiSelectContactsFlowB)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = if (isEncounterStage) "Save encounter" else "Next",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        stageTransition.AnimatedContent(
            transitionSpec = {
                if (isReducedMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    fadeIn(tween(110)) togetherWith fadeOut(tween(80))
                }
            }
        ) { selectedContact ->
            if (selectedContact == null) {
                ContactSelectorContent(
                    uiState = uiState,
                    onEvent = onEvent
                )
            } else {
                EncounterFormContent(
                    contact = selectedContact,
                    uiState = uiState,
                    onEvent = onEvent,
                    onOpenCustomDatePicker = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showDatePicker = true
                    },
                    onOpenTimePicker = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showTimePicker = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactSelectorContent(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit
) {
    val haptics = LocalHelloHaptics.current
    val focusRequester = remember { FocusRequester() }
    val selectedIds = remember(uiState.selectedContacts) {
        uiState.selectedContacts.map { it.id }.toSet()
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(45)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (uiState.isFlowBMultiSelectMode) {
                    "Select people (${uiState.selectedContacts.size})"
                } else {
                    "Who did you meet?"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!uiState.isFlowBMultiSelectMode) {
                Text(
                    text = "Tap to log · Long-press to select multiple",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MinimalFilledTextField(
            value = uiState.searchQuery,
            onValueChange = { onEvent(QuickActionUiEvent.OnSearchQueryChange(it)) },
            placeholder = "Search name...",
            focusRequester = focusRequester,
            imeAction = ImeAction.Search,
            capitalization = KeyboardCapitalization.Words
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.filteredContacts, key = { it.id }) { contact ->
                val isSelected = contact.id in selectedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                if (uiState.isFlowBMultiSelectMode) {
                                    onEvent(QuickActionUiEvent.OnToggleContactFlowB(contact))
                                } else {
                                    onEvent(QuickActionUiEvent.OnSelectExistingContact(contact))
                                }
                            },
                            onLongClick = {
                                haptics.perform(SemanticHapticType.EDIT_ENTRY)
                                onEvent(QuickActionUiEvent.OnLongPressContactFlowB(contact))
                            }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectableAvatarBadge(
                        isSelected = isSelected,
                        initials = contact.avatarInitials,
                        bgIndex = contact.avatarBgIndex,
                        size = 34.dp,
                        photoUri = contact.photoUri
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = contact.contextSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    contact.lastEncounterText?.let { recency ->
                        Text(
                            text = recency,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncounterFormContent(
    contact: ContactSummary,
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    onOpenCustomDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isToday = uiState.encounterDate == today
    val isYesterday = uiState.encounterDate == yesterday
    val isCustomDate = !isToday && !isYesterday
    val timeLabel = remember(uiState.encounterTime) {
        uiState.encounterTime.format(ENCOUNTER_TIME_FORMATTER)
    }
    val focusRequester = remember { FocusRequester() }
    val selectedContacts = uiState.selectedContacts.ifEmpty { listOf(contact) }

    LaunchedEffect(contact.id) {
        withFrameNanos { }
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    if (uiState.isFlowBParticipantPickerOpen) {
        MultiContactPickerDialog(
            allContacts = uiState.allContacts,
            initiallySelectedIds = selectedContacts.map { it.id }.toSet(),
            onDismiss = { onEvent(QuickActionUiEvent.OnToggleFlowBParticipantPicker(false)) },
            onConfirm = { picked -> onEvent(QuickActionUiEvent.OnSetFlowBParticipants(picked)) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ContactParticipantsStrip(
            selectedContacts = selectedContacts,
            onRemoveContact = { id -> onEvent(QuickActionUiEvent.OnRemoveSelectedContactFlowB(id)) },
            onOpenMultiPicker = { onEvent(QuickActionUiEvent.OnToggleFlowBParticipantPicker(true)) }
        )

        // When: Date & Time Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            MinimalSelectableChip(
                label = "Today",
                selected = isToday,
                onClick = { onEvent(QuickActionUiEvent.OnEncounterDateChange(today)) }
            )
            MinimalSelectableChip(
                label = "Yesterday",
                selected = isYesterday,
                onClick = { onEvent(QuickActionUiEvent.OnEncounterDateChange(yesterday)) }
            )
            MinimalSelectableChip(
                label = if (isCustomDate) {
                    uiState.encounterDate.format(ENCOUNTER_CUSTOM_DATE_FORMATTER)
                } else {
                    "Custom..."
                },
                selected = isCustomDate,
                onClick = onOpenCustomDatePicker
            )
            TonalFilterPill(
                label = timeLabel,
                isSelected = uiState.isEncounterTimeCustomized,
                unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onOpenTimePicker,
                leadingIcon = { tintColor ->
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        // Where: Bordered & Selected Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MinimalSelectableChip(
                label = when {
                    uiState.isLocatingGps -> "Locating..."
                    uiState.capturedLocationLabel != null -> uiState.capturedLocationLabel
                    else -> "Current GPS"
                },
                leadingIconKey = HelloIconKey.MAP_PIN,
                selected = uiState.isLocatingGps || (uiState.capturedLatitude != null && uiState.capturedLongitude != null),
                onClick = { onEvent(QuickActionUiEvent.OnRequestLocation) }
            )

            uiState.suggestedPlaces.forEach { place ->
                val isSelected = uiState.encounterPlaceId == place.id
                MinimalSelectableChip(
                    label = place.label,
                    selected = isSelected,
                    onClick = {
                        val nextId = if (isSelected) null else place.id
                        onEvent(QuickActionUiEvent.OnEncounterPlaceChange(nextId, ""))
                    }
                )
            }
        }

        // Note: Filled input background
        MinimalFilledTextField(
            value = uiState.encounterNoteFlowB,
            onValueChange = { onEvent(QuickActionUiEvent.OnEncounterNoteChange(it)) },
            placeholder = "Caught up about...",
            focusRequester = focusRequester,
            singleLine = false,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences,
            onImeAction = { onEvent(QuickActionUiEvent.OnSaveFlowBEncounter) }
        )
    }
}

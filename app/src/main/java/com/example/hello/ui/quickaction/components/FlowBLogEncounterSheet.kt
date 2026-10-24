package com.example.hello.ui.quickaction.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowBLogEncounterSheet(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.encounterDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onEvent(QuickActionUiEvent.OnEncounterDateChange(picked))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                            onEvent(QuickActionUiEvent.OnClearSelectedContact)
                        } else {
                            onEvent(QuickActionUiEvent.OnDismiss)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEncounterStage) {
                        Icons.AutoMirrored.Outlined.ArrowBack
                    } else {
                        Icons.Outlined.Close
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

            // Right Circular '->' Arrow Button
            if (isEncounterStage) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onEvent(QuickActionUiEvent.OnSaveFlowBEncounter) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        AnimatedContent(
            targetState = uiState.selectedContact,
            transitionSpec = {
                fadeIn(tween(110)) togetherWith fadeOut(tween(80))
            },
            label = "flowBStageTransition"
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
                    onOpenCustomDatePicker = { showDatePicker = true }
                )
            }
        }
    }
}

@Composable
private fun ContactSelectorContent(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

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
        Text(
            text = "Who did you meet?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(QuickActionUiEvent.OnSelectExistingContact(contact)) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                        Spacer(modifier = Modifier.width(8.dp))
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
    onOpenCustomDatePicker: () -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isToday = uiState.encounterDate == today
    val isYesterday = uiState.encounterDate == yesterday
    val isCustomDate = !isToday && !isYesterday
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(contact.id) {
        withFrameNanos { }
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // When: Bordered & Selected Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
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
                    uiState.encounterDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                } else {
                    "Custom..."
                },
                selected = isCustomDate,
                onClick = onOpenCustomDatePicker
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
                label = "📍 Current GPS",
                selected = false,
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

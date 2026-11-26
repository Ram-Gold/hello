package com.example.hello.ui.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.FollowUp
import com.example.hello.ui.components.ContactParticipantsStrip
import com.example.hello.ui.components.HelloDatePickerDialog
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.MultiContactPickerDialog
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.predictiveSheetDismiss
import com.example.hello.ui.navigation.resolveSheetScrimAlpha
import com.example.hello.ui.quickaction.components.MinimalFilledTextField
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val REMINDER_QUICK_NOTES = listOf("Check in", "Send link", "Wish luck", "Follow up")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Hoisted state and callbacks for the in-hierarchy [ProfileReminderSheetOverlay] (RAM-61).
 */
data class ProfileReminderOverlayPayload(
    val contactName: String,
    val editingReminder: FollowUp?,
    val defaultHour: Int,
    val defaultMinute: Int,
    val onDismiss: () -> Unit,
    val onSave: (dueDate: LocalDate, note: String, hasExplicitTime: Boolean, explicitTime: LocalTime?, participantIds: List<String>) -> Unit,
    val onDelete: ((String) -> Unit)? = null,
    val primaryContact: ContactSummary? = null,
    val availableContacts: List<ContactSummary> = emptyList()
)

/**
 * In-hierarchy bottom-anchored Reminder sheet overlay matching QuickActionSheetHost ("+ Catching up?")
 * 1:1 — zero DialogWindow lag, synchronized with IME insets, and supporting scrubbable Predictive Back (RAM-61).
 */
@Composable
fun ProfileReminderSheetOverlay(
    payload: ProfileReminderOverlayPayload?,
    modifier: Modifier = Modifier
) {
    val isOpen = payload != null
    val isReducedMotion = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var lastPayload by remember { mutableStateOf<ProfileReminderOverlayPayload?>(null) }
    if (payload != null) {
        lastPayload = payload
    }

    var sheetDismissProgress by remember { mutableFloatStateOf(0f) }
    var isPredictiveDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            isPredictiveDismissing = false
            sheetDismissProgress = 0f
        }
    }

    val dismissWithKeyboardHide: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        lastPayload?.onDismiss?.invoke()
    }

    ScrubbablePredictiveBackHandler(
        enabled = isOpen,
        isReducedMotion = isReducedMotion,
        onProgressChanged = { sheetDismissProgress = it },
        finishOnCommit = true,
        baseFinishDurationMs = 140,
        onCommit = {
            isPredictiveDismissing = true
            dismissWithKeyboardHide()
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop matching QuickActionSheetHost
        AnimatedVisibility(
            visible = isOpen,
            enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(tween(120)),
            exit = if (isPredictiveDismissing || isReducedMotion) ExitTransition.None else fadeOut(tween(100))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = resolveSheetScrimAlpha(sheetDismissProgress, maxAlpha = 0.32f)))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        dismissWithKeyboardHide()
                    }
            )
        }

        // In-hierarchy bottom-anchored sheet (zero DialogWindow lag, synchronized 1:1 with IME)
        AnimatedVisibility(
            visible = isOpen,
            enter = if (isReducedMotion) {
                fadeIn(snap())
            } else {
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f)
                ) + fadeIn(tween(90))
            },
            exit = if (isPredictiveDismissing || isReducedMotion) {
                ExitTransition.None
            } else {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(120)
                ) + fadeOut(tween(90))
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val active = lastPayload ?: return@AnimatedVisibility
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .predictiveSheetDismiss { sheetDismissProgress }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Consume taps on sheet surface */ }
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                ReminderEditorSheetContent(
                    contactName = active.contactName,
                    editingReminder = active.editingReminder,
                    defaultHour = active.defaultHour,
                    defaultMinute = active.defaultMinute,
                    onDismiss = dismissWithKeyboardHide,
                    onSave = { dueDate, note, hasExplicitTime, explicitTime, participantIds ->
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        active.onSave(dueDate, note, hasExplicitTime, explicitTime, participantIds)
                    },
                    onDelete = active.onDelete?.let { deleteCallback ->
                        { followUpId ->
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            deleteCallback(followUpId)
                        }
                    },
                    primaryContact = active.primaryContact,
                    availableContacts = active.availableContacts
                )
            }
        }
    }
}

/**
 * Focused in-hierarchy content for creating or editing a Reminder on a Contact Profile,
 * matching the visual language, padding, and keyboard cadence of FlowBLogEncounterSheet ("+ Catching up?")
 * per RAM-60 and RAM-61.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheetContent(
    contactName: String,
    editingReminder: FollowUp?,
    defaultHour: Int,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onSave: (dueDate: LocalDate, note: String, hasExplicitTime: Boolean, explicitTime: LocalTime?, participantIds: List<String>) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    primaryContact: ContactSummary? = null,
    availableContacts: List<ContactSummary> = emptyList()
) {
    val zoneId = ZoneId.systemDefault()
    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }
    val inThreeDays = remember(today) { today.plusDays(3) }
    val nextWeek = remember(today) { today.plusDays(7) }

    val initialLocal = editingReminder?.dueAt?.atZone(zoneId)
    val initialDate = initialLocal?.toLocalDate() ?: inThreeDays
    val initialHasExplicitTime = editingReminder?.hasExplicitTime ?: false
    val defaultLocalTime = remember(defaultHour, defaultMinute) {
        LocalTime.of(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
    }
    val initialTime = if (initialLocal != null && initialHasExplicitTime) {
        initialLocal.toLocalTime()
    } else {
        defaultLocalTime
    }

    val contactsById = remember(availableContacts, primaryContact) {
        val map = availableContacts.associateBy { it.id }.toMutableMap()
        if (primaryContact != null) {
            map.putIfAbsent(primaryContact.id, primaryContact)
        }
        map
    }
    val initialParticipants = remember(editingReminder?.id, primaryContact?.id, availableContacts) {
        val fromReminder = editingReminder?.resolvedParticipantIds().orEmpty()
            .mapNotNull { contactsById[it] }
        fromReminder.ifEmpty { listOfNotNull(primaryContact) }
    }
    var selectedParticipants by remember(editingReminder?.id, primaryContact?.id) {
        mutableStateOf(initialParticipants)
    }
    var showParticipantPicker by remember { mutableStateOf(false) }

    var note by remember(editingReminder?.id) { mutableStateOf(editingReminder?.note.orEmpty()) }
    var selectedDate by remember(editingReminder?.id, initialDate) { mutableStateOf(initialDate) }
    var hasExplicitTime by remember(editingReminder?.id, initialHasExplicitTime) { mutableStateOf(initialHasExplicitTime) }
    var selectedTime by remember(editingReminder?.id, initialTime) { mutableStateOf(initialTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isCustomDate = selectedDate != tomorrow && selectedDate != inThreeDays && selectedDate != nextWeek
    val defaultTimeLabel = remember(defaultLocalTime) { defaultLocalTime.format(TIME_FORMATTER) }
    val activeTimeLabel = if (hasExplicitTime) {
        "At ${selectedTime.format(TIME_FORMATTER)}"
    } else {
        "At $defaultTimeLabel"
    }

    val submitReminder = {
        onSave(
            selectedDate,
            note,
            hasExplicitTime,
            if (hasExplicitTime) selectedTime else null,
            selectedParticipants.map { it.id }
        )
    }

    LaunchedEffect(editingReminder?.id) {
        withFrameNanos { }
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Minimal Action Row: Left 40dp Circular 'X' Close | Right Trash (if editing) & 40dp Circular '->' Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editingReminder != null && onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { onDelete(editingReminder.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete reminder",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { submitReminder() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Save reminder",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Contact Name / Multi-Participant Strip, Borderless Pill-to-Squircle Chips & Filled Note Input
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (availableContacts.isNotEmpty() && selectedParticipants.isNotEmpty()) {
                ContactParticipantsStrip(
                    selectedContacts = selectedParticipants,
                    onRemoveContact = { id ->
                        if (selectedParticipants.size > 1) {
                            selectedParticipants = selectedParticipants.filterNot { it.id == id }
                        }
                    },
                    onOpenMultiPicker = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showParticipantPicker = true
                    }
                )
            } else {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // When: Borderless Pill-to-Squircle Date Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "Tomorrow" to tomorrow,
                    "In 3 days" to inThreeDays,
                    "Next week" to nextWeek
                )
                presets.forEach { (label, date) ->
                    TonalFilterPill(
                        label = label,
                        isSelected = selectedDate == date,
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { selectedDate = date }
                    )
                }

                TonalFilterPill(
                    label = if (isCustomDate) selectedDate.format(DATE_FORMATTER) else "Custom...",
                    isSelected = isCustomDate,
                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showDatePicker = true
                    },
                    leadingIcon = if (isCustomDate) {
                        { tintColor ->
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null
                )
            }

            // Time & Quick-Note Suggestions: Borderless Pill-to-Squircle Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TonalFilterPill(
                    label = activeTimeLabel,
                    isSelected = hasExplicitTime,
                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        showTimePicker = true
                    },
                    leadingIcon = { tintColor ->
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )

                REMINDER_QUICK_NOTES.forEach { suggestion ->
                    val isSelected = note.equals(suggestion, ignoreCase = true)
                    TonalFilterPill(
                        label = suggestion,
                        isSelected = isSelected,
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            note = if (isSelected) "" else suggestion
                        }
                    )
                }
            }

            // Note: MinimalFilledTextField matching Catching up?
            MinimalFilledTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Remind me to...",
                focusRequester = focusRequester,
                singleLine = false,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences,
                onImeAction = { submitReminder() }
            )
        }
    }

    if (showParticipantPicker) {
        MultiContactPickerDialog(
            allContacts = availableContacts,
            initiallySelectedIds = selectedParticipants.map { it.id }.toSet(),
            onDismiss = { showParticipantPicker = false },
            onConfirm = { picked ->
                selectedParticipants = picked
                showParticipantPicker = false
            }
        )
    }

    if (showDatePicker) {
        HelloDatePickerDialog(
            initialDate = selectedDate,
            onConfirm = { picked ->
                selectedDate = picked
            },
            onDismissRequest = { showDatePicker = false },
            restrictToPastDates = false
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick reminder time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        hasExplicitTime = true
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hasExplicitTime) {
                        TextButton(
                            onClick = {
                                hasExplicitTime = false
                                selectedTime = defaultLocalTime
                                showTimePicker = false
                            }
                        ) { Text("Use default ($defaultTimeLabel)") }
                    }
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

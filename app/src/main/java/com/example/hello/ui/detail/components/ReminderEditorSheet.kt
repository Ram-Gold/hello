package com.example.hello.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.FollowUp
import com.example.hello.ui.components.TonalFilterPill
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val REMINDER_QUICK_NOTES = listOf("Check in", "Send link", "Wish luck", "Follow up")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Focused bottom sheet for creating or editing a Reminder on a Contact Profile,
 * matching the visual language and flow of FlowBLogEncounterSheet ("+ Catching up?") per RAM-60.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorBottomSheet(
    contactName: String,
    editingReminder: FollowUp?,
    defaultHour: Int,
    defaultMinute: Int,
    onDismiss: () -> Unit,
    onSave: (dueDate: LocalDate, note: String, hasExplicitTime: Boolean, explicitTime: LocalTime?) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            if (hasExplicitTime) selectedTime else null
        )
    }

    LaunchedEffect(editingReminder?.id) {
        withFrameNanos { }
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Minimal Action Row: Left 'X' Close | Right Trash (if editing) & Circular '->' Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
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

            // 2. Contact Name & Chip Rows (matching FlowBLogEncounterSheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

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

                // Note: Borderless MinimalFilledTextField matching Catching up?
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = false,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitReminder() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (note.isEmpty()) {
                                Text(
                                    text = "Remind me to...",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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

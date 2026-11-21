package com.example.hello.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure date-conversion and validation helpers for [HelloDatePickerDialog].
 */
object HelloDatePickerDefaults {

    fun localDateToUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun utcMillisToLocalDate(utcTimeMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun isSelectableBirthDate(
        utcTimeMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = utcTimeMillis <= nowMillis

    fun isSelectableBirthYear(
        year: Int,
        currentYear: Int = LocalDate.now().year
    ): Boolean = year <= currentYear

    fun shouldDismissImeForDisplayMode(displayMode: DisplayMode): Boolean =
        displayMode == DisplayMode.Picker

    fun createBirthDateSelectableDates(): SelectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
            isSelectableBirthDate(utcTimeMillis)

        override fun isSelectableYear(year: Int): Boolean =
            isSelectableBirthYear(year)
    }
}

/**
 * Shared Material 3 [DatePickerDialog] wrapper that:
 * 1. Clears focus and hides the soft keyboard when opened, when switching from
 *    text input ([DisplayMode.Input]) back to calendar ([DisplayMode.Picker]),
 *    and when confirmed/cancelled/dismissed.
 * 2. Uses `usePlatformDefaultWidth = false` + `decorFitsSystemWindows = false`
 *    with [imePadding] and [verticalScroll] so switching between text input and
 *    calendar never clips or breaks the calendar layout on smaller/older phones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelloDatePickerDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    restrictToPastDates: Boolean = false,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel"
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val clearFocusAndHideIme = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val initialMillis = remember(initialDate) {
        initialDate?.let(HelloDatePickerDefaults::localDateToUtcMillis)
            ?: System.currentTimeMillis()
    }
    val selectableDates = remember(restrictToPastDates) {
        if (restrictToPastDates) {
            HelloDatePickerDefaults.createBirthDateSelectableDates()
        } else {
            object : SelectableDates {}
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = selectableDates
    )

    // Ensure no lingering IME from the host screen stays open when the dialog appears.
    LaunchedEffect(Unit) {
        clearFocusAndHideIme()
    }

    // Dismiss IME immediately when toggling back from text-based input to calendar picker.
    LaunchedEffect(datePickerState.displayMode) {
        if (HelloDatePickerDefaults.shouldDismissImeForDisplayMode(datePickerState.displayMode)) {
            clearFocusAndHideIme()
        }
    }

    DatePickerDialog(
        onDismissRequest = {
            clearFocusAndHideIme()
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clearFocusAndHideIme()
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(HelloDatePickerDefaults.utcMillisToLocalDate(millis))
                    }
                    onDismissRequest()
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    clearFocusAndHideIme()
                    onDismissRequest()
                }
            ) {
                Text(dismissLabel)
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }
}

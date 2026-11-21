package com.example.hello.ui.components

import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
class HelloDatePickerDialogTest {

    @Test
    fun localDateToUtcMillis_andBack_roundTripsWithoutTimezoneShift() {
        val date = LocalDate.of(1994, 7, 19)
        val millis = HelloDatePickerDefaults.localDateToUtcMillis(date)
        val roundTripped = HelloDatePickerDefaults.utcMillisToLocalDate(millis)

        assertEquals(date, roundTripped)
    }

    @Test
    fun isSelectableBirthDate_allowsPastAndPresent_blocksFuture() {
        val nowMillis = HelloDatePickerDefaults.localDateToUtcMillis(LocalDate.of(2026, 11, 21))
        val pastMillis = HelloDatePickerDefaults.localDateToUtcMillis(LocalDate.of(1990, 5, 10))
        val futureMillis = HelloDatePickerDefaults.localDateToUtcMillis(LocalDate.of(2026, 11, 22))

        assertTrue(HelloDatePickerDefaults.isSelectableBirthDate(pastMillis, nowMillis))
        assertTrue(HelloDatePickerDefaults.isSelectableBirthDate(nowMillis, nowMillis))
        assertFalse(HelloDatePickerDefaults.isSelectableBirthDate(futureMillis, nowMillis))
    }

    @Test
    fun isSelectableBirthYear_allowsCurrentAndPast_blocksFuture() {
        assertTrue(HelloDatePickerDefaults.isSelectableBirthYear(1985, currentYear = 2026))
        assertTrue(HelloDatePickerDefaults.isSelectableBirthYear(2026, currentYear = 2026))
        assertFalse(HelloDatePickerDefaults.isSelectableBirthYear(2027, currentYear = 2026))
    }

    @Test
    fun shouldDismissImeForDisplayMode_returnsTrueForPicker_falseForInput() {
        assertTrue(HelloDatePickerDefaults.shouldDismissImeForDisplayMode(DisplayMode.Picker))
        assertFalse(HelloDatePickerDefaults.shouldDismissImeForDisplayMode(DisplayMode.Input))
    }
}

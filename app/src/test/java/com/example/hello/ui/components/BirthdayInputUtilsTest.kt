package com.example.hello.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BirthdayInputUtilsTest {

    @Test
    fun parseBirthdayInput_supportsMonthDayYearFormats() {
        // Hyphen format: MM-DD-YYYY
        val parsedHyphen = BirthdayInputUtils.parseBirthdayInput("11-06-1996")
        assertNotNull(parsedHyphen)
        assertEquals(LocalDate.of(1996, 11, 6), parsedHyphen)

        // Slash format: MM/DD/YYYY
        val parsedSlash = BirthdayInputUtils.parseBirthdayInput("05/18/1990")
        assertNotNull(parsedSlash)
        assertEquals(LocalDate.of(1990, 5, 18), parsedSlash)

        // Single digit month/day: M-D-YYYY
        val parsedSingle = BirthdayInputUtils.parseBirthdayInput("4-7-2002")
        assertNotNull(parsedSingle)
        assertEquals(LocalDate.of(2002, 4, 7), parsedSingle)

        // Fallback ISO: YYYY-MM-DD
        val parsedIso = BirthdayInputUtils.parseBirthdayInput("1988-12-25")
        assertNotNull(parsedIso)
        assertEquals(LocalDate.of(1988, 12, 25), parsedIso)
    }

    @Test
    fun parseBirthdayInput_supportsMonthDayOptionalYear() {
        val parsed = BirthdayInputUtils.parseBirthdayInput("08-15")
        assertNotNull(parsed)
        assertEquals(8, parsed?.monthValue)
        assertEquals(15, parsed?.dayOfMonth)

        val parsedSlash = BirthdayInputUtils.parseBirthdayInput("3/22")
        assertNotNull(parsedSlash)
        assertEquals(3, parsedSlash?.monthValue)
        assertEquals(22, parsedSlash?.dayOfMonth)
    }

    @Test
    fun parseBirthdayInput_incompleteOrInvalid_returnsNull() {
        assertNull(BirthdayInputUtils.parseBirthdayInput(""))
        assertNull(BirthdayInputUtils.parseBirthdayInput("   "))
        assertNull(BirthdayInputUtils.parseBirthdayInput("1"))
        assertNull(BirthdayInputUtils.parseBirthdayInput("11-"))
        assertNull(BirthdayInputUtils.parseBirthdayInput("invalid-date"))
        assertNull(BirthdayInputUtils.parseBirthdayInput("13-40-2020")) // Invalid month & day
    }

    @Test
    fun leapYearHandling_feb29SafelyAdjustsOnNonLeapYears() {
        // Feb 29 on non-leap year (e.g. 2025 or 2026) safely adjusts to Feb 28 without throwing DateTimeException
        val date2026 = BirthdayInputUtils.createSafeDate(2026, 2, 29)
        assertNotNull(date2026)
        assertEquals(LocalDate.of(2026, 2, 28), date2026)

        // Leap year 2024 keeps Feb 29
        val date2024 = BirthdayInputUtils.createSafeDate(2024, 2, 29)
        assertNotNull(date2024)
        assertEquals(LocalDate.of(2024, 2, 29), date2024)
    }

    @Test
    fun nextAnnualBirthday_computesCorrectUpcomingDate() {
        val today = LocalDate.of(2026, 11, 8)

        // Birthday tomorrow: Nov 9
        val bdayTomorrow = LocalDate.of(1995, 11, 9)
        val nextTomorrow = BirthdayInputUtils.nextAnnualBirthday(bdayTomorrow, today)
        assertEquals(LocalDate.of(2026, 11, 9), nextTomorrow)

        // Birthday today: Nov 8
        val bdayToday = LocalDate.of(1995, 11, 8)
        val nextToday = BirthdayInputUtils.nextAnnualBirthday(bdayToday, today)
        assertEquals(LocalDate.of(2026, 11, 8), nextToday)

        // Birthday passed this year: Nov 5 -> rolls to 2027
        val bdayPassed = LocalDate.of(1995, 11, 5)
        val nextPassed = BirthdayInputUtils.nextAnnualBirthday(bdayPassed, today)
        assertEquals(LocalDate.of(2027, 11, 5), nextPassed)
    }

    @Test
    fun formatToMonthDayYear_formatsCorrectly() {
        assertEquals("", BirthdayInputUtils.formatToMonthDayYear(null))
        assertEquals("11-06-1996", BirthdayInputUtils.formatToMonthDayYear(LocalDate.of(1996, 11, 6)))
    }

    @Test
    fun formatHumanReadable_formatsCorrectly() {
        assertEquals("", BirthdayInputUtils.formatHumanReadable(null))
        assertEquals("Nov 6, 1996", BirthdayInputUtils.formatHumanReadable(LocalDate.of(1996, 11, 6)))
        assertEquals("August 15", BirthdayInputUtils.formatHumanReadable(LocalDate.of(0, 8, 15)))
    }
}


package com.example.hello.ui.components

import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val BIRTHDAY_INPUT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")
private val BIRTHDAY_HUMAN_FULL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val BIRTHDAY_HUMAN_MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")
private val BIRTHDAY_SHORT_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val BIRTHDAY_SHORT_MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * Robust date formatting and parsing utility for contact birthdays.
 * Supports Month-Day-Year (MM-DD-YYYY) and Month-Day (MM-DD), with slash or hyphen separators,
 * and guards against leap-year crashes on February 29th.
 */
object BirthdayInputUtils {

    /**
     * Formats a LocalDate into a human-friendly MM-DD-YYYY string.
     */
    fun formatToMonthDayYear(date: LocalDate?): String {
        if (date == null) return ""
        return date.format(BIRTHDAY_INPUT_FORMATTER)
    }

    /**
     * Formats a LocalDate into a clean, human-readable display string (e.g. "Nov 6, 1996" or "November 6").
     */
    fun formatHumanReadable(date: LocalDate?): String {
        if (date == null) return ""
        return if (date.year > 1900) {
            date.format(BIRTHDAY_HUMAN_FULL_FORMATTER)
        } else {
            date.format(BIRTHDAY_HUMAN_MONTH_DAY_FORMATTER)
        }
    }

    /**
     * Safely attempts to parse user input string into a LocalDate.
     * Supports:
     * - MM-DD-YYYY / MM/DD/YYYY (e.g. "11-06-1996", "11/06/1996")
     * - M-D-YYYY / M/D/YYYY (e.g. "5-4-1998")
     * - MM-DD / MM/DD (defaults year to 2000 as a leap-year safe placeholder year)
     * - Standard ISO YYYY-MM-DD fallback for backwards compatibility
     *
     * Returns null if incomplete or invalid.
     */
    fun parseBirthdayInput(input: String): LocalDate? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val normalized = trimmed.replace('/', '-')
        val parts = normalized.split('-')

        return try {
            when (parts.size) {
                // MM-DD-YYYY or M-D-YYYY
                3 -> {
                    if (parts[0].length == 4) {
                        // YYYY-MM-DD fallback
                        val year = parts[0].toIntOrNull() ?: return null
                        val month = parts[1].toIntOrNull() ?: return null
                        val day = parts[2].toIntOrNull() ?: return null
                        createSafeDate(year, month, day)
                    } else {
                        // MM-DD-YYYY
                        val month = parts[0].toIntOrNull() ?: return null
                        val day = parts[1].toIntOrNull() ?: return null
                        val year = parts[2].toIntOrNull() ?: return null
                        if (year in 1900..2100) {
                            createSafeDate(year, month, day)
                        } else null
                    }
                }
                // MM-DD or M-D (year optional: stored with year 0 placeholder)
                2 -> {
                    val month = parts[0].toIntOrNull() ?: return null
                    val day = parts[1].toIntOrNull() ?: return null
                    createSafeDate(0, month, day)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a LocalDate safely handling Feb 29 in non-leap years by adjusting to Feb 28.
     */
    fun createSafeDate(year: Int, month: Int, day: Int): LocalDate? {
        if (month !in 1..12) return null
        if (day !in 1..31) return null

        val adjustedDay = if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) {
            28
        } else {
            day
        }

        return try {
            LocalDate.of(year, month, adjustedDay)
        } catch (_: DateTimeParseException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Calculates the next annual birthday date safely, handling Feb 29 on non-leap target years.
     */
    fun nextAnnualBirthday(bday: LocalDate, today: LocalDate = LocalDate.now()): LocalDate {
        var targetYear = today.year
        var nextBday = toYearSafely(bday, targetYear)
        if (nextBday.isBefore(today)) {
            targetYear = today.year + 1
            nextBday = toYearSafely(bday, targetYear)
        }
        return nextBday
    }

    /**
     * Returns true when the contact's next annual birthday falls within the upcoming
     * calendar month (inclusive of today through `today.plusMonths(1)`).
     */
    fun isUpcomingWithinOneMonth(bday: LocalDate?, today: LocalDate = LocalDate.now()): Boolean {
        if (bday == null) return false
        val nextBday = nextAnnualBirthday(bday, today)
        return !nextBday.isBefore(today) && !nextBday.isAfter(today.plusMonths(1))
    }

    /**
     * Formats a month/day with a standard 3-letter abbreviation and trailing period
     * (except May), e.g. "Nov. 21" or "May 5".
     */
    fun formatAbbreviatedMonthDay(date: LocalDate): String {
        val monthLabel = when (date.monthValue) {
            1 -> "Jan."
            2 -> "Feb."
            3 -> "Mar."
            4 -> "Apr."
            5 -> "May"
            6 -> "Jun."
            7 -> "Jul."
            8 -> "Aug."
            9 -> "Sep."
            10 -> "Oct."
            11 -> "Nov."
            12 -> "Dec."
            else -> date.format(BIRTHDAY_SHORT_MONTH_FORMATTER)
        }
        return "$monthLabel ${date.dayOfMonth}"
    }

    /**
     * Formats the relative countdown for an upcoming birthday ("Today!", "Tomorrow", or "In X days").
     */
    fun formatBirthdayCountdown(daysUntil: Long): String = when (daysUntil) {
        0L -> "Today!"
        1L -> "Tomorrow"
        else -> "In $daysUntil days"
    }

    /**
     * Moves a date to targetYear, adjusting Feb 29 to Feb 28 if targetYear is not a leap year.
     */
    fun toYearSafely(date: LocalDate, targetYear: Int): LocalDate {
        val isFeb29 = date.monthValue == 2 && date.dayOfMonth == 29
        return if (isFeb29 && !Year.isLeap(targetYear.toLong())) {
            LocalDate.of(targetYear, 2, 28)
        } else {
            LocalDate.of(targetYear, date.monthValue, date.dayOfMonth)
        }
    }

    /**
     * Formats birthday for display on profile/card:
     * - If year is placeholder (2000) or user chose optional year: "Nov 6"
     * - If actual year: "Nov 6, 1996 (Age X)"
     */
    fun formatDisplayBirthday(bday: LocalDate?, today: LocalDate = LocalDate.now()): String {
        if (bday == null) return ""
        val nextBday = nextAnnualBirthday(bday, today)
        val monthDayStr = nextBday.format(BIRTHDAY_SHORT_MONTH_DAY_FORMATTER)

        // If birth year is not the placeholder 0, or if we know turning age
        val age = (today.year - bday.year).takeIf { it in 1..130 && bday.year > 1900 }
        return if (age != null) {
            "$monthDayStr, ${bday.year} (Turning $age)"
        } else {
            monthDayStr
        }
    }
}


package com.example.hello.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Pure Kotlin domain model representing an actionable stay-in-touch reminder tied to a Contact.
 * Presented to users in the UI as a "Reminder" (GLOSSARY.md, ADR-0037).
 */
data class FollowUp(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val dueAt: Instant,
    val note: String = "",
    val doneAt: Instant? = null,
    val hasExplicitTime: Boolean = false
) {
    val isDone: Boolean
        get() = doneAt != null

    companion object {
        fun computeDueAt(
            date: LocalDate,
            hasExplicitTime: Boolean,
            explicitTime: LocalTime? = null,
            defaultHour: Int = 9,
            defaultMinute: Int = 0,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Instant {
            val resolvedTime = if (hasExplicitTime && explicitTime != null) {
                explicitTime
            } else {
                LocalTime.of(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
            }
            return date.atTime(resolvedTime).atZone(zoneId).toInstant()
        }

        fun computeSnoozedDueAt(
            followUp: FollowUp,
            days: Long = 3L,
            defaultHour: Int = 9,
            defaultMinute: Int = 0,
            now: Instant = Instant.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Instant {
            val today = now.atZone(zoneId).toLocalDate()
            val currentLocal = followUp.dueAt.atZone(zoneId)
            val baseDate = if (currentLocal.toLocalDate().isBefore(today)) today else currentLocal.toLocalDate()
            val targetDate = baseDate.plusDays(days.coerceAtLeast(1L))
            val time = if (followUp.hasExplicitTime) {
                currentLocal.toLocalTime()
            } else {
                LocalTime.of(defaultHour.coerceIn(0, 23), defaultMinute.coerceIn(0, 59))
            }
            return targetDate.atTime(time).atZone(zoneId).toInstant()
        }

        fun shiftDateOnlyDueAt(
            currentDueAt: Instant,
            newHour: Int,
            newMinute: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Instant {
            val date = currentDueAt.atZone(zoneId).toLocalDate()
            val time = LocalTime.of(newHour.coerceIn(0, 23), newMinute.coerceIn(0, 59))
            return date.atTime(time).atZone(zoneId).toInstant()
        }
    }
}

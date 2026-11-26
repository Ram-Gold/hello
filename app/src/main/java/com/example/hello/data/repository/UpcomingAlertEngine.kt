package com.example.hello.data.repository

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.SerendipityPicker
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.domain.model.formatParticipantNames
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Pure in-process engine computing UpcomingAlert projections (pending FollowUps,
 * 14-day Birthdays, and ADR-0036 weighted-random Serendipity Prompts with daily skip state).
 */
class UpcomingAlertEngine {

    private var skippedSerendipityDate: LocalDate = LocalDate.now()
    private val skippedSerendipityContactIds = mutableSetOf<String>()

    fun skipSerendipity(contactId: String, today: LocalDate = LocalDate.now()): Set<String> {
        rollSkipListIfNewDay(today)
        if (contactId.isNotBlank()) {
            skippedSerendipityContactIds += contactId
        }
        return skippedSerendipityContactIds.toSet()
    }

    fun skippedIdsFor(today: LocalDate = LocalDate.now()): Set<String> {
        rollSkipListIfNewDay(today)
        return skippedSerendipityContactIds.toSet()
    }

    private fun rollSkipListIfNewDay(today: LocalDate) {
        if (skippedSerendipityDate != today) {
            skippedSerendipityContactIds.clear()
            skippedSerendipityDate = today
        }
    }

    fun buildFollowUpAlerts(
        followUps: List<FollowUp>,
        contactsMap: Map<String, Contact>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<UpcomingAlert.FollowUpReminder> = followUps
        .mapNotNull { fu ->
            if (fu.isDone) return@mapNotNull null
            val activeParticipants = fu.resolvedParticipantIds()
                .mapNotNull { contactsMap[it] }
                .filterNot { it.isArchived }
            if (activeParticipants.isEmpty()) return@mapNotNull null
            fu to formatParticipantNames(activeParticipants.map { it.name })
        }
        .sortedBy { (fu, _) -> fu.dueAt }
        .map { (fu, contactName) ->
            val dueDate = fu.dueAt.atZone(zoneId).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
            val dueText = when {
                daysUntil < 0L -> "Overdue"
                daysUntil == 0L -> "Today"
                daysUntil == 1L -> "Tomorrow"
                else -> "In $daysUntil days"
            }
            UpcomingAlert.FollowUpReminder(
                id = fu.id,
                contactName = contactName,
                taskDescription = fu.note,
                dueText = dueText
            )
        }

    fun computeBirthdayAlerts(
        contacts: Collection<Contact>,
        today: LocalDate = LocalDate.now()
    ): List<UpcomingAlert.BirthdayAlert> = contacts
        .filterNot { it.isArchived }
        .mapNotNull { contact ->
            val bday = contact.birthday ?: return@mapNotNull null
            val nextBday = nextAnnualBirthday(bday, today)
            val daysUntil = ChronoUnit.DAYS.between(today, nextBday)
            if (daysUntil in 0..14) {
                val dateText = when (daysUntil) {
                    0L -> "Today!"
                    1L -> "Tomorrow"
                    else -> "In $daysUntil days"
                }
                UpcomingAlert.BirthdayAlert(
                    id = "bday-${contact.id}",
                    contactId = contact.id,
                    contactName = contact.name,
                    dateText = dateText
                )
            } else {
                null
            }
        }

    fun computeSerendipityPrompts(
        contacts: Collection<Contact>,
        encounters: List<Encounter>,
        pendingFollowUps: List<FollowUp>,
        skippedContactIds: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneOffset.UTC
    ): List<UpcomingAlert.SerendipityPrompt> {
        val pendingContactIds = pendingFollowUps
            .filter { it.doneAt == null }
            .flatMap { it.resolvedParticipantIds() }
            .toSet()
        val encounterCountByContact = mutableMapOf<String, Int>()
        encounters.forEach { enc ->
            enc.resolvedParticipantIds().forEach { pid ->
                encounterCountByContact[pid] = (encounterCountByContact[pid] ?: 0) + 1
            }
        }

        val candidates = contacts
            .filterNot { it.isArchived || it.id in pendingContactIds || it.id in skippedContactIds }
            .mapNotNull { contact ->
                val anchorInstant = contact.lastMetAt ?: contact.firstMetAt ?: return@mapNotNull null
                val anchorDate = anchorInstant.atZone(zoneId).toLocalDate()
                val daysSince = ChronoUnit.DAYS.between(anchorDate, today).toInt().coerceAtLeast(0)
                if (daysSince < SerendipityPicker.MIN_GAP_DAYS) return@mapNotNull null

                val hasMultiple = (encounterCountByContact[contact.id] ?: 0) >= 2 || contact.meetCount >= 2
                val contextText = SerendipityPicker.formatSerendipityContext(
                    daysSince = daysSince,
                    hasMultipleEncounters = hasMultiple
                )
                val prompt = UpcomingAlert.SerendipityPrompt(
                    id = "serendipity-${contact.id}",
                    contactName = contact.name,
                    daysSinceLastMet = daysSince,
                    contextText = contextText,
                    contactId = contact.id
                )
                SerendipityPicker.WeightedCandidate(
                    item = prompt,
                    stableId = contact.id,
                    daysSinceLastMet = daysSince
                )
            }

        return SerendipityPicker
            .pickWeightedRandom(
                candidates = candidates,
                todayEpochDay = today.toEpochDay(),
                maxCount = SerendipityPicker.MAX_SURFACED_PROMPTS
            )
            .map { it.item }
    }

    private fun nextAnnualBirthday(birthday: LocalDate, today: LocalDate): LocalDate {
        val thisYear = toYearSafely(birthday, today.year)
        return if (thisYear.isBefore(today)) toYearSafely(birthday, today.year + 1) else thisYear
    }

    private fun toYearSafely(birthday: LocalDate, year: Int): LocalDate {
        val day = if (birthday.monthValue == 2 && birthday.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            28
        } else {
            birthday.dayOfMonth
        }
        return LocalDate.of(year, birthday.monthValue, day)
    }
}

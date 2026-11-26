package com.example.hello.ui.timeline

import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.domain.model.Contact
import com.example.hello.ui.timeline.components.TimelineCalendarStripDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZonedDateTime

class TimelineDayGroupingTest {

    private val today = LocalDate.of(2026, 11, 21)
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun formatDayHeaderLabel_formatsRelativeAndCalendarDates() {
        assertEquals("Today, Nov 21", TimelineViewModel.formatDayHeaderLabel(today, today))
        assertEquals("Yesterday, Nov 20", TimelineViewModel.formatDayHeaderLabel(today.minusDays(1), today))
        assertEquals("Tomorrow, Nov 22", TimelineViewModel.formatDayHeaderLabel(today.plusDays(1), today))
        assertEquals("Nov 14", TimelineViewModel.formatDayHeaderLabel(LocalDate.of(2026, 11, 14), today))
        assertEquals("Nov 14, 2025", TimelineViewModel.formatDayHeaderLabel(LocalDate.of(2025, 11, 14), today))
        assertEquals("Jan 5, 2027", TimelineViewModel.formatDayHeaderLabel(LocalDate.of(2027, 1, 5), today))
    }

    @Test
    fun formatPastTimeLabel_formatsTimeOfDay() {
        val afternoon = ZonedDateTime.of(2026, 11, 21, 14, 30, 0, 0, zoneId).toInstant()
        assertEquals("2:30 PM", TimelineViewModel.formatPastTimeLabel(afternoon, zoneId))
    }

    @Test
    fun groupPastNodesByDay_groupsNewestFirstAndPreservesDayOrder() {
        val nov21Morning = ZonedDateTime.of(2026, 11, 21, 9, 15, 0, 0, zoneId).toInstant()
        val nov21Afternoon = ZonedDateTime.of(2026, 11, 21, 15, 45, 0, 0, zoneId).toInstant()
        val nov20Evening = ZonedDateTime.of(2026, 11, 20, 19, 0, 0, 0, zoneId).toInstant()

        val nodes = listOf(
            pastNode("p1", nov21Afternoon),
            pastNode("p2", nov21Morning),
            pastNode("p3", nov20Evening)
        )

        val groups = TimelineViewModel.groupPastNodesByDay(nodes, today, zoneId)
        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 11, 21), groups[0].date)
        assertEquals("Today, Nov 21", groups[0].headerLabel)
        assertEquals(listOf("p1", "p2"), groups[0].items.map { it.id })

        assertEquals(LocalDate.of(2026, 11, 20), groups[1].date)
        assertEquals("Yesterday, Nov 20", groups[1].headerLabel)
        assertEquals(listOf("p3"), groups[1].items.map { it.id })
    }

    @Test
    fun groupFutureNodesByDay_groupsMonotonicDescendingFurthestFutureFirst() {
        val nodes = listOf(
            FutureTimelineNode.BirthdayNode(
                id = "b1",
                contactId = "c1",
                contactName = "Alex",
                avatarInitials = "A",
                avatarBgIndex = 0,
                daysUntil = 0,
                dateLabel = "Today!",
                targetDate = today
            ),
            FutureTimelineNode.FollowUpNode(
                id = "f1",
                contactId = "c2",
                contactName = "Maya",
                avatarInitials = "M",
                avatarBgIndex = 1,
                daysUntil = 0,
                dateLabel = "Today",
                followUpId = "fu1",
                note = "Check in",
                targetDate = today
            ),
            FutureTimelineNode.FollowUpNode(
                id = "f2",
                contactId = "c3",
                contactName = "Sam",
                avatarInitials = "S",
                avatarBgIndex = 2,
                daysUntil = 1,
                dateLabel = "Tomorrow",
                followUpId = "fu2",
                note = "Send notes",
                targetDate = today.plusDays(1)
            )
        )

        val groups = TimelineViewModel.groupFutureNodesByDay(nodes, today)
        assertEquals(2, groups.size)
        assertEquals("Tomorrow, Nov 22", groups[0].headerLabel)
        assertEquals(listOf("f2"), groups[0].items.map { it.id })
        assertEquals("Today, Nov 21", groups[1].headerLabel)
        assertEquals(listOf("b1", "f1"), groups[1].items.map { it.id })
    }

    @Test
    fun calendarStripDefaults_respectMondayAndSundayWeekStart() {
        // 2026-11-24 is Tuesday
        val tuesday = LocalDate.of(2026, 11, 24)
        assertEquals(LocalDate.of(2026, 11, 23), TimelineCalendarStripDefaults.weekStartOf(tuesday, WeekStartDay.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 22), TimelineCalendarStripDefaults.weekStartOf(tuesday, WeekStartDay.SUNDAY))
    }

    @Test
    fun hasTimelineEntryOnDate_matchesActiveDatesAndLeapSafeRecurringBirthdays() {
        val activeDates = setOf(LocalDate.of(2026, 11, 24))
        val recurringBirthdays = setOf(MonthDay.of(7, 20), MonthDay.of(2, 29))

        assertTrue(TimelineViewModel.hasTimelineEntryOnDate(LocalDate.of(2026, 11, 24), activeDates, recurringBirthdays))
        assertTrue(TimelineViewModel.hasTimelineEntryOnDate(LocalDate.of(2028, 7, 20), activeDates, recurringBirthdays))
        // Non-leap year Feb 28 matches Feb 29 birthday
        assertTrue(TimelineViewModel.hasTimelineEntryOnDate(LocalDate.of(2026, 2, 28), activeDates, recurringBirthdays))
        // Leap year Feb 28 does NOT match Feb 29 birthday
        assertFalse(TimelineViewModel.hasTimelineEntryOnDate(LocalDate.of(2028, 2, 28), activeDates, recurringBirthdays))
        assertTrue(TimelineViewModel.hasTimelineEntryOnDate(LocalDate.of(2028, 2, 29), activeDates, recurringBirthdays))
    }

    @Test
    fun buildPastBirthdaysForWindow_materializesHistoricalBirthdaysInNavigatedMonth() {
        val alex = Contact(id = "c1", name = "Alex", birthday = LocalDate.of(1995, 5, 14))
        val result = TimelineViewModel.buildPastBirthdaysForWindow(
            contactsMap = mapOf("c1" to alex),
            anchorDate = LocalDate.of(2025, 5, 10),
            today = today
        )
        val may14 = result[LocalDate.of(2025, 5, 14)].orEmpty()
        assertEquals(1, may14.size)
        assertEquals(30, may14.first().turningAge)
    }

    private fun pastNode(id: String, occurredAt: Instant) = PastTimelineNode(
        id = id,
        contactId = "c-$id",
        contactName = "Contact $id",
        avatarInitials = "C",
        avatarBgIndex = 0,
        occurredAt = occurredAt,
        dateLabel = "Met today",
        placeLabel = null,
        notes = "",
        badge = PastEncounterBadge.MET_AGAIN,
        timeLabel = TimelineViewModel.formatPastTimeLabel(occurredAt, zoneId)
    )
}

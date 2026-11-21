package com.example.hello.ui.timeline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
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
    fun groupFutureNodesByDay_groupsSoonestFirst() {
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
        assertEquals("Today, Nov 21", groups[0].headerLabel)
        assertEquals(listOf("b1", "f1"), groups[0].items.map { it.id })
        assertEquals("Tomorrow, Nov 22", groups[1].headerLabel)
        assertEquals(listOf("f2"), groups[1].items.map { it.id })
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

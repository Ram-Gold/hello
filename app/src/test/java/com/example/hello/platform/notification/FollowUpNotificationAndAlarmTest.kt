package com.example.hello.platform.notification

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.formatParticipantNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class FollowUpNotificationAndAlarmTest {

    @Test
    fun constants_areCorrectlyConfigured() {
        assertEquals("followup_reminders", FollowUpNotificationHelper.CHANNEL_ID)
        assertEquals("com.example.hello.action.FOLLOW_UP_DONE", FollowUpActionReceiver.ACTION_DONE)
        assertEquals("com.example.hello.action.FOLLOW_UP_SNOOZE", FollowUpActionReceiver.ACTION_SNOOZE)
        assertEquals("com.example.hello.action.VIEW_CONTACT", FollowUpNotificationHelper.ACTION_VIEW_CONTACT)
    }

    @Test
    fun triggerCalculation_handlesFutureAndPastDates() {
        val now = System.currentTimeMillis()
        val future = Instant.ofEpochMilli(now + 100000)
        val past = Instant.ofEpochMilli(now - 100000)

        val triggerFuture = future.toEpochMilli().coerceAtLeast(now + 1000L)
        val triggerPast = past.toEpochMilli().coerceAtLeast(now + 1000L)

        assertEquals(now + 100000, triggerFuture)
        assertEquals(now + 1000L, triggerPast)
    }

    @Test
    fun formatParticipantNames_handlesAllCounts() {
        assertEquals("Contact", formatParticipantNames(emptyList()))
        assertEquals("Alice", formatParticipantNames(listOf("Alice")))
        assertEquals("Alice & Bob", formatParticipantNames(listOf("Alice", "Bob")))
        assertEquals("Alice, Bob & Charlie", formatParticipantNames(listOf("Alice", "Bob", "Charlie")))
        assertEquals("Alice, Bob & 2 others", formatParticipantNames(listOf("Alice", "Bob", "Charlie", "Dana")))
    }

    @Test
    fun checkInDigestPlanner_batchesQuietContactsAndSkipsPendingFollowUps() {
        val today = LocalDate.of(2026, 11, 23)
        val quietInstant = Instant.now().minus(30, ChronoUnit.DAYS)
        val alice = Contact(id = "a", name = "Alice", lastMetAt = quietInstant)
        val bob = Contact(id = "b", name = "Bob", lastMetAt = quietInstant)
        val carol = Contact(id = "c", name = "Carol", lastMetAt = quietInstant)
        val pendingFollowUp = FollowUp(
            id = "fu-1",
            contactId = "c",
            participantIds = listOf("c"),
            dueAt = Instant.now().plus(2, ChronoUnit.DAYS)
        )

        val payload = CheckInDigestPlanner.buildDigestPayload(
            contacts = listOf(alice, bob, carol),
            encounters = emptyList(),
            pendingFollowUps = listOf(pendingFollowUp),
            today = today
        )
        assertNotNull(payload)
        assertEquals(2, payload!!.contactIds.size)
        assertTrue("Carol with pending follow-up should be excluded", "c" !in payload.contactIds)
        assertTrue(payload.title.contains("Alice") && payload.title.contains("Bob"))

        val emptyPayload = CheckInDigestPlanner.buildDigestPayload(
            contacts = listOf(carol),
            encounters = emptyList(),
            pendingFollowUps = listOf(pendingFollowUp),
            today = today
        )
        assertNull(emptyPayload)
    }

    @Test
    fun checkInDigestPlanner_computeInitialDelayTargetsNextPreferredHour() {
        val zone = ZoneId.of("UTC")
        val before9am = ZonedDateTime.of(2026, 11, 23, 8, 0, 0, 0, zone)
        assertEquals(60 * 60_000L, CheckInDigestPlanner.computeInitialDelayMillis(before9am, 9, 0))

        val after9am = ZonedDateTime.of(2026, 11, 23, 10, 0, 0, 0, zone)
        assertEquals(23 * 60 * 60_000L, CheckInDigestPlanner.computeInitialDelayMillis(after9am, 9, 0))
    }
}

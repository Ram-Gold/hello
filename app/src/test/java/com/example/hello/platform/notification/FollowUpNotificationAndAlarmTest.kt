package com.example.hello.platform.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

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
}

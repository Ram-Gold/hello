package com.example.hello.platform.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.hello.domain.model.FollowUp
import java.time.Instant

/**
 * Manages scheduling and cancelling exact alarms for FollowUp stay-in-touch reminders.
 * Adheres to Android 12+ (API 31+) exact alarm permission regulations with graceful windowed fallback.
 */
class FollowUpAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Schedules an exact (or allowed windowed) wake-up alarm for a given FollowUp.
     */
    fun scheduleAlarm(
        followUpId: String,
        contactId: String,
        contactName: String,
        note: String,
        dueAt: Instant
    ) {
        val manager = alarmManager ?: return
        val triggerMillis = dueAt.toEpochMilli()

        // If scheduled in the past or immediately, trigger in 1 second
        val effectiveTrigger = triggerMillis.coerceAtLeast(System.currentTimeMillis() + 1000L)

        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            putExtra(EXTRA_FOLLOW_UP_ID, followUpId)
            putExtra(EXTRA_CONTACT_ID, contactId)
            putExtra(EXTRA_CONTACT_NAME, contactName)
            putExtra(EXTRA_NOTE, note)
        }

        val requestCode = followUpId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTrigger,
                        pendingIntent
                    )
                } else {
                    // Graceful fallback when exact alarm permission is revoked or not granted
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        effectiveTrigger,
                        pendingIntent
                    )
                }
            } else {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    effectiveTrigger,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("FollowUpAlarmScheduler", "Exact alarm denied, scheduling windowed fallback", e)
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                effectiveTrigger,
                pendingIntent
            )
        }
    }

    /**
     * Helper to schedule directly from a FollowUp domain model and contact name.
     */
    fun scheduleFollowUp(followUp: FollowUp, contactName: String) {
        scheduleAlarm(
            followUpId = followUp.id,
            contactId = followUp.contactId,
            contactName = contactName,
            note = followUp.note,
            dueAt = followUp.dueAt
        )
    }

    /**
     * Cancels an existing scheduled alarm for a given FollowUp ID.
     */
    fun cancelAlarm(followUpId: String) {
        val manager = alarmManager ?: return
        val intent = Intent(context, FollowUpAlarmReceiver::class.java)
        val requestCode = followUpId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        const val EXTRA_FOLLOW_UP_ID = "com.example.hello.extra.FOLLOW_UP_ID"
        const val EXTRA_CONTACT_ID = "com.example.hello.extra.CONTACT_ID"
        const val EXTRA_CONTACT_NAME = "com.example.hello.extra.CONTACT_NAME"
        const val EXTRA_NOTE = "com.example.hello.extra.NOTE"
    }
}

package com.example.hello.platform.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.formatParticipantNames
import java.time.Instant

/**
 * Manages scheduling and cancelling local alarms for FollowUp reminders (ADR-0037).
 * Defaults to zero-permission setAndAllowWhileIdle() and upgrades automatically to
 * setExactAndAllowWhileIdle() when SCHEDULE_EXACT_ALARM is granted.
 */
class FollowUpAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules an exact (when permitted) or inexact Doze-safe wake-up alarm for a given FollowUp.
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
            data = Uri.parse("$URI_SCHEME_PREFIX$followUpId")
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
        if (followUp.isDone) return
        scheduleAlarm(
            followUpId = followUp.id,
            contactId = followUp.resolvedParticipantIds().firstOrNull() ?: followUp.contactId,
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
        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            data = Uri.parse("$URI_SCHEME_PREFIX$followUpId")
        }
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

        private const val URI_SCHEME_PREFIX = "hello://followup/alarm/"

        fun createExactAlarmSettingsIntent(context: Context): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else {
                FollowUpNotificationHelper.createAppNotificationSettingsIntent(context)
            }
        }

        /**
         * Reconciles and re-arms alarms for all pending reminders belonging to active (non-archived) contacts.
         * Safe to call after reboot, app update, clock/timezone changes, exact-alarm grant, or app startup.
         */
        suspend fun rescheduleAllPendingFromDatabase(context: Context) {
            val appContext = context.applicationContext
            val db = HelloDatabase.getInstance(appContext)
            val scheduler = FollowUpAlarmScheduler(appContext)
            val pending = db.followUpDao().getPendingFollowUps()
            if (pending.isEmpty()) return

            val participantsByFollowUp = db.followUpDao()
                .getAllPersonFollowUps()
                .groupBy({ it.followUpId }, { it.contactId })
            val activeContactsById = db.contactDao()
                .getAllContacts()
                .asSequence()
                .filterNot { it.isArchived }
                .associateBy { it.id }

            val nowMillis = System.currentTimeMillis()
            for (entity in pending) {
                val participantIds = participantsByFollowUp[entity.id]
                    .orEmpty()
                    .ifEmpty { listOf(entity.contactId) }
                    .distinct()
                val activeContacts = participantIds.mapNotNull { activeContactsById[it] }
                if (activeContacts.isEmpty()) {
                    scheduler.cancelAlarm(entity.id)
                } else if (entity.dueAt.toEpochMilli() <= nowMillis) {
                    continue
                } else {
                    val activeParticipantIds = activeContacts.map { it.id }
                    scheduler.scheduleFollowUp(
                        entity.toDomain(activeParticipantIds),
                        formatParticipantNames(activeContacts.map { it.name })
                    )
                }
            }
        }
    }
}

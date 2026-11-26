package com.example.hello.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.formatParticipantNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Headless BroadcastReceiver handling inline 'Done' and 'Snooze (3d)' notification actions.
 * Executes background Room mutations and alarm rescheduling without launching any Activity.
 */
class FollowUpActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val followUpId = intent.getStringExtra(EXTRA_FOLLOW_UP_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, followUpId.hashCode())

        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {}

        val action = intent.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val appContext = context.applicationContext
                val db = HelloDatabase.getInstance(appContext)
                val scheduler = FollowUpAlarmScheduler(appContext)
                when (action) {
                    ACTION_DONE -> {
                        db.followUpDao().markDone(followUpId, Instant.now())
                        scheduler.cancelAlarm(followUpId)
                    }
                    ACTION_SNOOZE -> {
                        val existing = db.followUpDao().getFollowUpById(followUpId) ?: return@launch
                        if (existing.doneAt != null) return@launch
                        val activeContacts = FollowUpNotificationHelper.resolveActiveContactsForFollowUp(db, existing)
                        if (activeContacts.isEmpty()) return@launch

                        val prefs = DataStoreUserPreferencesRepository(appContext.helloDataStore)
                            .userPreferencesFlow.first()
                        val newDueAt = FollowUp.computeSnoozedDueAt(
                            followUp = existing.toDomain(activeContacts.map { it.id }),
                            days = 3L,
                            defaultHour = prefs.notificationHour,
                            defaultMinute = prefs.notificationMinute
                        )
                        db.followUpDao().snooze(followUpId, newDueAt)
                        scheduler.scheduleAlarm(
                            followUpId = followUpId,
                            contactId = activeContacts.first().id,
                            contactName = formatParticipantNames(activeContacts.map { it.name }),
                            note = existing.note,
                            dueAt = newDueAt
                        )
                    }
                }
            } catch (_: Exception) {
                // Failsafe on background action receiver
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.example.hello.action.FOLLOW_UP_DONE"
        const val ACTION_SNOOZE = "com.example.hello.action.FOLLOW_UP_SNOOZE"

        const val EXTRA_FOLLOW_UP_ID = "com.example.hello.extra.ACTION_FOLLOW_UP_ID"
        const val EXTRA_NOTIFICATION_ID = "com.example.hello.extra.ACTION_NOTIFICATION_ID"
        const val EXTRA_CONTACT_ID = "com.example.hello.extra.ACTION_CONTACT_ID"
        const val EXTRA_CONTACT_NAME = "com.example.hello.extra.ACTION_CONTACT_NAME"
        const val EXTRA_NOTE = "com.example.hello.extra.ACTION_NOTE"
    }
}

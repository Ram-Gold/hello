package com.example.hello.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.hello.data.local.HelloDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Headless BroadcastReceiver handling inline 'Done' and 'Snooze' notification actions.
 * Executes background Room mutations and alarm rescheduling in <10ms without launching any Activity.
 */
class FollowUpActionReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val followUpId = intent.getStringExtra(EXTRA_FOLLOW_UP_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, followUpId.hashCode())

        // Instantly dismiss notification for snappy UI response
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {}

        val action = intent.action ?: return
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val db = HelloDatabase.getInstance(context)
                when (action) {
                    ACTION_DONE -> {
                        db.followUpDao().markDone(followUpId, Instant.now())
                    }
                    ACTION_SNOOZE -> {
                        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID).orEmpty()
                        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Contact"
                        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

                        val newDueAt = Instant.now().plus(3, ChronoUnit.DAYS)
                        db.followUpDao().snooze(followUpId, newDueAt)

                        // Re-arm exact alarm for +3 days
                        val scheduler = FollowUpAlarmScheduler(context)
                        scheduler.scheduleAlarm(
                            followUpId = followUpId,
                            contactId = contactId,
                            contactName = contactName,
                            note = note,
                            dueAt = newDueAt
                        )
                    }
                }
            } catch (_: Exception) {
                // Failsafe logging if required
            } finally {
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

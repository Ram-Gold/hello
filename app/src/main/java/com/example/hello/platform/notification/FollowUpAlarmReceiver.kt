package com.example.hello.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled FollowUp due date is reached.
 * Immediately posts the NotificationCompat follow-up notification.
 */
class FollowUpAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val followUpId = intent.getStringExtra(FollowUpAlarmScheduler.EXTRA_FOLLOW_UP_ID) ?: return
        val contactId = intent.getStringExtra(FollowUpAlarmScheduler.EXTRA_CONTACT_ID).orEmpty()
        val contactName = intent.getStringExtra(FollowUpAlarmScheduler.EXTRA_CONTACT_NAME) ?: "Contact"
        val note = intent.getStringExtra(FollowUpAlarmScheduler.EXTRA_NOTE).orEmpty()

        FollowUpNotificationHelper.showFollowUpNotification(
            context = context,
            followUpId = followUpId,
            contactId = contactId,
            contactName = contactName,
            note = note
        )
    }
}

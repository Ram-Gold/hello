package com.example.hello.platform.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.hello.MainActivity

/**
 * Helper responsible for creating the FollowUp notification channel and posting NotificationCompat instances
 * with inline Done and Snooze (+3 days) action receivers.
 */
object FollowUpNotificationHelper {

    const val CHANNEL_ID = "followup_reminders"
    const val CHANNEL_NAME = "Follow-up Reminders"
    const val CHANNEL_DESCRIPTION = "Stay-in-touch cadences and actionable follow-up tasks"

    const val ACTION_VIEW_CONTACT = "com.example.hello.action.VIEW_CONTACT"
    const val EXTRA_CONTACT_ID = "com.example.hello.extra.CONTACT_ID"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if notification posting permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Posts a notification for the specified FollowUp reminder.
     */
    @SuppressLint("MissingPermission")
    fun showFollowUpNotification(
        context: Context,
        followUpId: String,
        contactId: String,
        contactName: String,
        note: String
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val notificationId = followUpId.hashCode()

        // 1. Content tap intent: Launches MainActivity with deep link routing to PersonDetailScreen
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_CONTACT
            putExtra(EXTRA_CONTACT_ID, contactId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Inline Action: Done (dismisses notification and marks done in DB via Headless Receiver)
        val doneIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = FollowUpActionReceiver.ACTION_DONE
            putExtra(FollowUpActionReceiver.EXTRA_FOLLOW_UP_ID, followUpId)
            putExtra(FollowUpActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 31 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Inline Action: Snooze (+3 days)
        val snoozeIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = FollowUpActionReceiver.ACTION_SNOOZE
            putExtra(FollowUpActionReceiver.EXTRA_FOLLOW_UP_ID, followUpId)
            putExtra(FollowUpActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(FollowUpActionReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(FollowUpActionReceiver.EXTRA_CONTACT_NAME, contactName)
            putExtra(FollowUpActionReceiver.EXTRA_NOTE, note)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 31 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanNote = note.ifBlank { "Stay-in-touch cadence due" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Follow up with $contactName")
            .setContentText(cleanNote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanNote))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Done",
                donePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze (3d)",
                snoozePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

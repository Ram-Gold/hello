package com.example.hello.platform.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.hello.MainActivity
import com.example.hello.R
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.FollowUpEntity

/**
 * Helper responsible for creating the FollowUp notification channel and posting NotificationCompat instances
 * with inline Done and Snooze (+3 days) action receivers and VISIBILITY_PRIVATE lock-screen redaction (ADR-0037).
 */
object FollowUpNotificationHelper {

    const val CHANNEL_ID = "followup_reminders"
    const val CHANNEL_NAME = "Reminders"
    const val CHANNEL_DESCRIPTION = "Actionable reminders you schedule for contacts"

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
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
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
     * Checks whether the Reminders notification channel is enabled (not IMPORTANCE_NONE).
     */
    fun isNotificationChannelEnabled(context: Context): Boolean {
        ensureNotificationChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = manager?.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return true
    }

    fun isFullyDeliverable(context: Context): Boolean =
        hasNotificationPermission(context) && isNotificationChannelEnabled(context)

    fun createAppNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun createChannelNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureNotificationChannel(context)
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            }
        } else {
            createAppNotificationSettingsIntent(context)
        }
    }

    suspend fun resolveActiveContactsForFollowUp(
        db: HelloDatabase,
        followUp: FollowUpEntity
    ): List<ContactEntity> {
        val participantIds = db.followUpDao().getParticipantIdsForFollowUp(followUp.id)
            .ifEmpty { listOf(followUp.contactId) }
            .distinct()
        return participantIds.mapNotNull { pid ->
            db.contactDao().getContactById(pid)?.takeIf { !it.isArchived }
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

        // 1. Content tap intent: Launches/reuses MainActivity with deep link routing to PersonDetailScreen
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_CONTACT
            data = Uri.parse("hello://contact/$contactId?followUpId=$followUpId")
            putExtra(EXTRA_CONTACT_ID, contactId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Inline Action: Done
        val doneIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = FollowUpActionReceiver.ACTION_DONE
            data = Uri.parse("hello://followup/done/$followUpId")
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
            data = Uri.parse("hello://followup/snooze/$followUpId")
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

        val cleanNote = note.ifBlank { "Follow up with $contactName" }

        // Sanitized lock-screen fallback when the user hides sensitive notification content
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hello_reminder)
            .setContentTitle("Hello")
            .setContentText("You have a reminder due")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hello_reminder)
            .setContentTitle("Follow up with $contactName")
            .setContentText(cleanNote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanNote))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                "Done",
                donePendingIntent
            )
            .addAction(
                0,
                "Snooze (3d)",
                snoozePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

package com.example.hello.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hello.data.local.HelloDatabase
import com.example.hello.domain.model.formatParticipantNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled FollowUp due time is reached.
 * Verifies live Room state via goAsync() so completed, deleted, or archived contacts never fire stale alerts (ADR-0037).
 */
class FollowUpAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val followUpId = intent.getStringExtra(FollowUpAlarmScheduler.EXTRA_FOLLOW_UP_ID) ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val appContext = context.applicationContext
                val db = HelloDatabase.getInstance(appContext)
                val followUp = db.followUpDao().getFollowUpById(followUpId) ?: return@launch
                if (followUp.doneAt != null) return@launch

                val activeContacts = FollowUpNotificationHelper.resolveActiveContactsForFollowUp(db, followUp)
                if (activeContacts.isEmpty()) return@launch

                FollowUpNotificationHelper.showFollowUpNotification(
                    context = appContext,
                    followUpId = followUp.id,
                    contactId = activeContacts.first().id,
                    contactName = formatParticipantNames(activeContacts.map { it.name }),
                    note = followUp.note
                )
            } catch (_: Exception) {
                // Fail silently on background receiver errors to prevent crashes
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }
}

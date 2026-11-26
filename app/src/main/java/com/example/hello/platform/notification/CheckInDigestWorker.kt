package com.example.hello.platform.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.hello.HelloApplication
import com.example.hello.MainActivity
import com.example.hello.R
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.preferences.CheckInDigestFrequency
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.SerendipityPicker
import com.example.hello.domain.model.formatParticipantNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

data class CheckInDigestPayload(
    val title: String,
    val body: String,
    val contactIds: List<String>
)

object CheckInDigestPlanner {
    const val DEFAULT_MAX_CONTACTS = 3

    fun buildDigestPayload(
        contacts: Collection<Contact>,
        encounters: List<Encounter>,
        pendingFollowUps: List<FollowUp>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneOffset.UTC,
        maxCount: Int = DEFAULT_MAX_CONTACTS
    ): CheckInDigestPayload? {
        val pendingContactIds = pendingFollowUps
            .filter { it.doneAt == null }
            .flatMap { it.resolvedParticipantIds() }
            .toSet()

        val encounterCountByContact = mutableMapOf<String, Int>()
        encounters.forEach { enc ->
            enc.resolvedParticipantIds().forEach { pid ->
                encounterCountByContact[pid] = (encounterCountByContact[pid] ?: 0) + 1
            }
        }

        val candidates = contacts
            .filterNot { it.isArchived || it.id in pendingContactIds }
            .mapNotNull { contact ->
                val anchor = contact.lastMetAt ?: contact.firstMetAt ?: return@mapNotNull null
                val anchorDate = anchor.atZone(zoneId).toLocalDate()
                val daysSince = ChronoUnit.DAYS.between(anchorDate, today).toInt().coerceAtLeast(0)
                if (daysSince < SerendipityPicker.MIN_GAP_DAYS) return@mapNotNull null
                val hasMultiple = (encounterCountByContact[contact.id] ?: 0) >= 2 || contact.meetCount >= 2
                val snippet = SerendipityPicker.formatSerendipityContext(daysSince, hasMultiple)
                SerendipityPicker.WeightedCandidate(
                    item = contact to snippet,
                    stableId = contact.id,
                    daysSinceLastMet = daysSince
                )
            }

        val picked = SerendipityPicker.pickWeightedRandom(
            candidates = candidates,
            todayEpochDay = today.toEpochDay(),
            maxCount = maxCount
        )
        if (picked.isEmpty()) return null

        val pickedContacts = picked.map { it.item.first }
        val namesSummary = formatParticipantNames(pickedContacts.map { it.name })
        val body = picked.joinToString(" · ") { (pair, _, _) ->
            val (contact, snippet) = pair
            "${contact.name} ($snippet)"
        }
        return CheckInDigestPayload(
            title = "Catch up with $namesSummary",
            body = body,
            contactIds = pickedContacts.map { it.id }
        )
    }

    fun computeInitialDelayMillis(
        now: ZonedDateTime,
        preferredHour: Int,
        preferredMinute: Int
    ): Long {
        val targetTime = LocalTime.of(preferredHour.coerceIn(0, 23), preferredMinute.coerceIn(0, 59))
        var target = now.toLocalDate().atTime(targetTime).atZone(now.zone)
        if (!target.isAfter(now)) {
            target = target.plusDays(1L)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(60_000L)
    }
}

object CheckInDigestScheduler {
    const val UNIQUE_WORK_NAME = "hello_checkin_digest"

    fun syncSchedule(
        workManager: WorkManager,
        frequency: CheckInDigestFrequency,
        preferredHour: Int,
        preferredMinute: Int,
        now: ZonedDateTime = ZonedDateTime.now(),
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
    ) {
        if (frequency == CheckInDigestFrequency.OFF) {
            if (existingPeriodicWorkPolicy != ExistingPeriodicWorkPolicy.KEEP) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            }
            return
        }
        val initialDelayMs = CheckInDigestPlanner.computeInitialDelayMillis(
            now = now,
            preferredHour = preferredHour,
            preferredMinute = preferredMinute
        )
        val request = PeriodicWorkRequestBuilder<CheckInDigestWorker>(
            frequency.intervalDays,
            TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            existingPeriodicWorkPolicy,
            request
        )
    }
}

/**
 * Periodic local-only WorkManager worker that batches 2-3 quiet Contacts via
 * [SerendipityPicker] into one low-pressure notification (ADR-0039).
 */
class CheckInDigestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = HelloApplication.from(applicationContext)
            val prefsRepository = app?.userPreferencesRepository
                ?: DataStoreUserPreferencesRepository(applicationContext.helloDataStore)
            val prefs = prefsRepository.userPreferencesFlow.first()
            if (prefs.checkInDigestFrequency == CheckInDigestFrequency.OFF) {
                return@withContext Result.success()
            }
            if (!FollowUpNotificationHelper.hasNotificationPermission(applicationContext)) {
                return@withContext Result.success()
            }

            val db = app?.database ?: HelloDatabase.getInstance(applicationContext)
            val contacts = db.contactDao().getAllContacts().map { it.toDomain() }
            val encRefs = db.encounterDao().getAllPersonEncounters()
                .groupBy({ it.encounterId }, { it.contactId })
            val encounters = db.encounterDao().getAllEncountersFlow().first()
                .map { it.toDomain(encRefs[it.id].orEmpty()) }
            val fuRefs = db.followUpDao().getAllPersonFollowUps()
                .groupBy({ it.followUpId }, { it.contactId })
            val pendingFollowUps = db.followUpDao().getPendingFollowUps()
                .map { it.toDomain(fuRefs[it.id].orEmpty()) }

            val payload = CheckInDigestPlanner.buildDigestPayload(
                contacts = contacts,
                encounters = encounters,
                pendingFollowUps = pendingFollowUps,
                zoneId = ZoneId.systemDefault()
            ) ?: return@withContext Result.success()

            ensureChannel(applicationContext)

            val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
                if (payload.contactIds.size == 1) {
                    val targetId = payload.contactIds.first()
                    action = FollowUpNotificationHelper.ACTION_VIEW_CONTACT
                    data = Uri.parse("hello://contact/$targetId")
                    putExtra(FollowUpNotificationHelper.EXTRA_CONTACT_ID, targetId)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val publicNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_hello_reminder)
                .setContentTitle("Hello")
                .setContentText("Time to catch up with friends")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_hello_reminder)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {
        const val CHANNEL_ID = "checkin_digest"
        const val CHANNEL_NAME = "Check-in Digest"
        const val CHANNEL_DESCRIPTION = "Optional periodic suggestions of quiet contacts to catch up with"
        const val NOTIFICATION_ID = 0x48454C4C

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }
    }
}

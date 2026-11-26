package com.example.hello.data.repository

import androidx.room.Transactor
import androidx.room.useWriterConnection
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.PersonFollowUpCrossRef
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.formatParticipantNames
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Deep FollowUp persistence module owning the reactive FollowUp stream, serialized ACID
 * Room mutations, alarm scheduling, and ADR-0027 FollowUp undo recovery.
 */
class FollowUpStore(
    private val database: HelloDatabase?,
    private val alarmScheduler: FollowUpAlarmScheduler?,
    private val coroutineScope: CoroutineScope,
    private val undoStore: UndoActionStore,
    private val resolveActiveParticipants: (List<String>) -> List<Contact>,
    private val resolveFallbackName: (String) -> String?,
    private val onFollowUpsChanged: (ZoneId) -> Unit,
    initialFollowUps: List<FollowUp> = MockDataProvider.followUps
) {
    private val _followUps = MutableStateFlow(initialFollowUps)
    val followUps: StateFlow<List<FollowUp>> = _followUps.asStateFlow()

    private var lastDbJob: Job? = null

    @Volatile
    private var defaultNotificationHour: Int = 9

    @Volatile
    private var defaultNotificationMinute: Int = 0

    fun setDefaultNotificationTime(hour: Int, minute: Int) {
        defaultNotificationHour = hour.coerceIn(0, 23)
        defaultNotificationMinute = minute.coerceIn(0, 59)
    }

    fun observeDatabase() {
        val db = database ?: return
        coroutineScope.launch {
            combine(
                db.followUpDao().getAllFollowUpsFlow(),
                db.followUpDao().getAllPersonFollowUpsFlow()
            ) { entities, crossRefs ->
                val refsByFollowUp = crossRefs.groupBy({ it.followUpId }, { it.contactId })
                entities.map { it.toDomain(refsByFollowUp[it.id].orEmpty()) }
            }.collect { domainFollowUps ->
                _followUps.value = domainFollowUps
                onFollowUpsChanged(ZoneId.systemDefault())
            }
        }
    }

    fun getFollowUpsForContact(contactId: String): Flow<List<FollowUp>> =
        _followUps.map { list ->
            list.filter { contactId in it.resolvedParticipantIds() }
                .sortedBy { it.dueAt }
        }

    fun prependInMemory(followUp: FollowUp) {
        _followUps.update { listOf(followUp) + it }
    }

    fun restoreInMemory(followUpsToRestore: List<FollowUp>) {
        if (followUpsToRestore.isEmpty()) return
        val restoredIds = followUpsToRestore.map { it.id }.toSet()
        _followUps.update { followUpsToRestore + it.filterNot { fu -> fu.id in restoredIds } }
    }

    fun removeParticipantsFromMemory(deletedContactIds: Set<String>) {
        _followUps.update { list ->
            list.mapNotNull { fu ->
                val remaining = fu.resolvedParticipantIds().filterNot { it in deletedContactIds }
                when {
                    remaining.isEmpty() -> null
                    remaining.size == fu.resolvedParticipantIds().size -> fu
                    else -> fu.copy(contactId = remaining.first(), participantIds = remaining)
                }
            }
        }
    }

    fun addFollowUp(
        contactId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean = false,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault(),
        participantIds: List<String> = listOf(contactId)
    ): FollowUp {
        val allIds = (participantIds.ifEmpty { listOf(contactId) }).distinct()
        val primaryId = allIds.first()
        val activeParticipants = resolveActiveParticipants(allIds)
        val displayNames = activeParticipants.map { it.name }.ifEmpty {
            allIds.mapNotNull(resolveFallbackName)
        }
        val formattedParticipants = formatParticipantNames(displayNames)
        val resolvedNote = note.trim().ifEmpty { "Follow up with $formattedParticipants" }
        val resolvedDueAt = FollowUp.computeDueAt(
            date = dueDate,
            hasExplicitTime = hasExplicitTime,
            explicitTime = explicitTime,
            defaultHour = defaultHour,
            defaultMinute = defaultMinute,
            zoneId = zoneId
        )
        val followUp = FollowUp(
            id = "fu-${UUID.randomUUID()}",
            contactId = primaryId,
            participantIds = allIds,
            dueAt = resolvedDueAt,
            note = resolvedNote,
            hasExplicitTime = hasExplicitTime
        )
        _followUps.update { (it + followUp).sortedBy { item -> item.dueAt } }
        onFollowUpsChanged(zoneId)
        if (activeParticipants.isNotEmpty()) {
            alarmScheduler?.scheduleFollowUp(followUp, formattedParticipants)
        }

        enqueueDbWrite { db ->
            db.withTransactionSafe {
                db.followUpDao().insertFollowUp(followUp.toEntity())
                db.followUpDao().insertPersonFollowUps(
                    allIds.map { pid -> PersonFollowUpCrossRef(followUp.id, pid) }
                )
            }
        }
        return followUp
    }

    fun updateFollowUp(
        followUpId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault(),
        participantIds: List<String>? = null
    ): FollowUp? {
        val existing = _followUps.value.firstOrNull { it.id == followUpId } ?: return null
        val allIds = (participantIds?.ifEmpty { existing.resolvedParticipantIds() }
            ?: existing.resolvedParticipantIds()).distinct()
        val activeParticipants = resolveActiveParticipants(allIds)
        val displayNames = activeParticipants.map { it.name }.ifEmpty {
            allIds.mapNotNull(resolveFallbackName)
        }
        val formattedParticipants = formatParticipantNames(displayNames)
        val resolvedNote = note.trim().ifEmpty { "Follow up with $formattedParticipants" }
        val resolvedDueAt = FollowUp.computeDueAt(
            date = dueDate,
            hasExplicitTime = hasExplicitTime,
            explicitTime = explicitTime,
            defaultHour = defaultHour,
            defaultMinute = defaultMinute,
            zoneId = zoneId
        )
        val updated = existing.copy(
            contactId = allIds.first(),
            participantIds = allIds,
            dueAt = resolvedDueAt,
            note = resolvedNote,
            hasExplicitTime = hasExplicitTime
        )
        _followUps.update { list ->
            list.map { if (it.id == followUpId) updated else it }.sortedBy { it.dueAt }
        }
        onFollowUpsChanged(zoneId)
        if (activeParticipants.isEmpty()) {
            alarmScheduler?.cancelAlarm(followUpId)
        } else {
            alarmScheduler?.scheduleFollowUp(updated, formattedParticipants)
        }

        enqueueDbWrite { db ->
            db.withTransactionSafe {
                db.followUpDao().updateFollowUp(updated.toEntity())
                db.followUpDao().deletePersonFollowUpsForFollowUp(followUpId)
                db.followUpDao().insertPersonFollowUps(
                    allIds.map { pid -> PersonFollowUpCrossRef(followUpId, pid) }
                )
            }
        }
        return updated
    }

    fun deleteFollowUps(followUpIds: Set<String>) {
        if (followUpIds.isEmpty()) return
        val deleted = _followUps.value.filter { it.id in followUpIds }
        undoStore.recordFollowUpMutation(deleted, wasCompleted = false)
        _followUps.update { followUps -> followUps.filterNot { it.id in followUpIds } }
        onFollowUpsChanged(ZoneId.systemDefault())
        followUpIds.forEach { alarmScheduler?.cancelAlarm(it) }

        enqueueDbWrite { db ->
            db.withTransactionSafe {
                followUpIds.forEach { followUpId ->
                    db.followUpDao().deleteFollowUpById(followUpId)
                    db.followUpDao().deletePersonFollowUpsForFollowUp(followUpId)
                }
            }
        }
    }

    fun markFollowUpDone(alertId: String, doneAt: Instant = Instant.now()) {
        val existing = _followUps.value.firstOrNull { it.id == alertId }
        if (existing != null) {
            undoStore.recordFollowUpMutation(listOf(existing), wasCompleted = true)
        }
        _followUps.update { followUps ->
            followUps.map { if (it.id == alertId) it.copy(doneAt = doneAt) else it }
        }
        onFollowUpsChanged(ZoneId.systemDefault())
        alarmScheduler?.cancelAlarm(alertId)

        enqueueDbWrite { db ->
            db.followUpDao().markDone(alertId, doneAt)
        }
    }

    fun reopenFollowUp(alertId: String) {
        val existing = _followUps.value.firstOrNull { it.id == alertId } ?: return
        val reopened = existing.copy(doneAt = null)
        _followUps.update { followUps ->
            followUps.map { if (it.id == alertId) reopened else it }.sortedBy { it.dueAt }
        }
        onFollowUpsChanged(ZoneId.systemDefault())

        val activeParticipants = resolveActiveParticipants(reopened.resolvedParticipantIds())
        if (activeParticipants.isNotEmpty()) {
            alarmScheduler?.scheduleFollowUp(
                reopened,
                formatParticipantNames(activeParticipants.map { it.name })
            )
        }

        enqueueDbWrite { db ->
            db.followUpDao().clearDone(alertId)
        }
    }

    fun undoLastMutation(): Boolean {
        val snapshot = undoStore.consumeFollowUpMutation() ?: return false
        val toRestore = snapshot.followUps
        if (toRestore.isEmpty()) return false

        val restoredIds = toRestore.map { it.id }.toSet()
        _followUps.update { current ->
            (current.filterNot { it.id in restoredIds } + toRestore.map { it.copy(doneAt = null) })
                .sortedBy { it.dueAt }
        }
        onFollowUpsChanged(ZoneId.systemDefault())

        for (fu in toRestore) {
            val activeParticipants = resolveActiveParticipants(fu.resolvedParticipantIds())
            if (activeParticipants.isNotEmpty()) {
                alarmScheduler?.scheduleFollowUp(
                    fu.copy(doneAt = null),
                    formatParticipantNames(activeParticipants.map { it.name })
                )
            }
        }

        enqueueDbWrite { db ->
            db.withTransactionSafe {
                toRestore.forEach { fu ->
                    if (snapshot.wasCompleted) {
                        db.followUpDao().clearDone(fu.id)
                    } else {
                        db.followUpDao().insertFollowUp(fu.copy(doneAt = null).toEntity())
                        db.followUpDao().insertPersonFollowUps(
                            fu.resolvedParticipantIds().map { pid -> PersonFollowUpCrossRef(fu.id, pid) }
                        )
                    }
                }
            }
        }
        return true
    }

    fun snoozeFollowUp(
        alertId: String,
        days: Long = 3,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val targetFollowUp = _followUps.value.firstOrNull { it.id == alertId }
        val newDueAt = if (targetFollowUp != null) {
            FollowUp.computeSnoozedDueAt(
                followUp = targetFollowUp,
                days = days,
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                zoneId = zoneId
            )
        } else {
            Instant.now().plus(days, ChronoUnit.DAYS)
        }
        if (targetFollowUp != null) {
            val activeParticipants = resolveActiveParticipants(targetFollowUp.resolvedParticipantIds())
            if (activeParticipants.isNotEmpty()) {
                alarmScheduler?.scheduleAlarm(
                    followUpId = alertId,
                    contactId = activeParticipants.first().id,
                    contactName = formatParticipantNames(activeParticipants.map { it.name }),
                    note = targetFollowUp.note,
                    dueAt = newDueAt
                )
            }
        }

        _followUps.update { followUps ->
            followUps.map {
                if (it.id == alertId) it.copy(dueAt = newDueAt) else it
            }.sortedBy { it.dueAt }
        }
        onFollowUpsChanged(zoneId)

        enqueueDbWrite { db ->
            db.followUpDao().snooze(alertId, newDueAt)
        }
    }

    fun rescheduleDateOnlyFollowUps(
        hour: Int,
        minute: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        setDefaultNotificationTime(hour, minute)
        val shifted = mutableListOf<FollowUp>()
        _followUps.update { current ->
            current.map { fu ->
                if (!fu.isDone && !fu.hasExplicitTime) {
                    val updated = fu.copy(
                        dueAt = FollowUp.shiftDateOnlyDueAt(
                            currentDueAt = fu.dueAt,
                            newHour = defaultNotificationHour,
                            newMinute = defaultNotificationMinute,
                            zoneId = zoneId
                        )
                    )
                    shifted.add(updated)
                    updated
                } else {
                    fu
                }
            }.sortedBy { it.dueAt }
        }
        onFollowUpsChanged(zoneId)

        for (fu in shifted) {
            val activeParticipants = resolveActiveParticipants(fu.resolvedParticipantIds())
            if (activeParticipants.isNotEmpty()) {
                alarmScheduler?.scheduleFollowUp(fu, formatParticipantNames(activeParticipants.map { it.name }))
            }
        }

        if (shifted.isNotEmpty()) {
            enqueueDbWrite { db ->
                db.withTransactionSafe {
                    shifted.forEach { fu ->
                        db.followUpDao().updateFollowUp(fu.toEntity())
                    }
                }
            }
        }
    }

    private fun enqueueDbWrite(block: suspend (HelloDatabase) -> Unit) {
        val db = database ?: return
        val previousJob = lastDbJob
        lastDbJob = coroutineScope.launch {
            previousJob?.join()
            block(db)
        }
    }

    private suspend fun <R> HelloDatabase.withTransactionSafe(block: suspend () -> R): R =
        this.useWriterConnection { transactor ->
            transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                block()
            }
        }
}

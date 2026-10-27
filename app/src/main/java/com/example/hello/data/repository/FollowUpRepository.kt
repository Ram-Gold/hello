package com.example.hello.data.repository

import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.model.FollowUp
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Repository interface for managing durable FollowUp persistence and stay-in-touch cadences.
 * Backed by Room with atomic ACID updates in <2ms (Ticket 05).
 */
interface FollowUpRepository {
    fun getPendingFollowUpsFlow(): Flow<List<FollowUp>>
    fun getFollowUpsForContactFlow(contactId: String): Flow<List<FollowUp>>
    suspend fun saveFollowUp(followUp: FollowUp)
    suspend fun markDone(id: String, doneAt: Instant = Instant.now())
    suspend fun snooze(id: String, newDueAt: Instant)
    suspend fun deleteFollowUp(id: String)
}

class RoomFollowUpRepository(
    private val database: HelloDatabase,
    private val alarmScheduler: FollowUpAlarmScheduler? = null
) : FollowUpRepository {

    override fun getPendingFollowUpsFlow(): Flow<List<FollowUp>> {
        return database.followUpDao().getPendingFollowUpsFlow()
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getFollowUpsForContactFlow(contactId: String): Flow<List<FollowUp>> {
        return database.followUpDao().getFollowUpsForContactFlow(contactId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveFollowUp(followUp: FollowUp) {
        database.followUpDao().insertFollowUp(followUp.toEntity())
    }

    override suspend fun markDone(id: String, doneAt: Instant) {
        database.followUpDao().markDone(id, doneAt)
        alarmScheduler?.cancelAlarm(id)
    }

    override suspend fun snooze(id: String, newDueAt: Instant) {
        database.followUpDao().snooze(id, newDueAt)
    }

    override suspend fun deleteFollowUp(id: String) {
        database.followUpDao().deleteFollowUpById(id)
        alarmScheduler?.cancelAlarm(id)
    }
}

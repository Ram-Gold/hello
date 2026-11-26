package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.hello.domain.model.FollowUp
import java.time.Instant

@Entity(
    tableName = "follow_ups",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["doneAt"]),
        Index(value = ["dueAt"])
    ]
)
data class FollowUpEntity(
    @PrimaryKey
    val id: String,
    val contactId: String,
    val dueAt: Instant,
    val note: String,
    val doneAt: Instant? = null,
    val hasExplicitTime: Boolean = false
)

fun FollowUp.toEntity(): FollowUpEntity = FollowUpEntity(
    id = id,
    contactId = contactId,
    dueAt = dueAt,
    note = note,
    doneAt = doneAt,
    hasExplicitTime = hasExplicitTime
)

fun FollowUpEntity.toDomain(participantIds: List<String> = emptyList()): FollowUp = FollowUp(
    id = id,
    contactId = contactId,
    participantIds = participantIds.distinct(),
    dueAt = dueAt,
    note = note,
    doneAt = doneAt,
    hasExplicitTime = hasExplicitTime
)

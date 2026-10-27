package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.FollowUp
import java.time.Instant

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey
    val id: String,
    val contactId: String,
    val dueAt: Instant,
    val note: String,
    val doneAt: Instant? = null
)

fun FollowUp.toEntity(): FollowUpEntity = FollowUpEntity(
    id = id,
    contactId = contactId,
    dueAt = dueAt,
    note = note,
    doneAt = null
)

fun FollowUpEntity.toDomain(): FollowUp = FollowUp(
    id = id,
    contactId = contactId,
    dueAt = dueAt,
    note = note
)

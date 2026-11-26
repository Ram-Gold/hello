package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Encounter
import java.time.Instant

@Entity(
    tableName = "encounters",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["placeId"]),
        Index(value = ["occurredAt"])
    ]
)
data class EncounterEntity(
    @PrimaryKey
    val id: String,
    val contactId: String,
    val occurredAt: Instant,
    val placeId: String? = null,
    val eventLabel: String? = null,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

fun Encounter.toEntity(): EncounterEntity = EncounterEntity(
    id = id,
    contactId = contactId,
    occurredAt = occurredAt,
    placeId = placeId,
    eventLabel = eventLabel,
    notes = notes,
    latitude = latitude,
    longitude = longitude
)

fun EncounterEntity.toDomain(participantIds: List<String> = emptyList()): Encounter = Encounter(
    id = id,
    contactId = contactId,
    participantIds = participantIds.distinct(),
    occurredAt = occurredAt,
    placeId = placeId,
    eventLabel = eventLabel,
    notes = notes,
    latitude = latitude,
    longitude = longitude
)

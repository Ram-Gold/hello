package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Encounter
import java.time.Instant

@Entity(tableName = "encounters")
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

fun EncounterEntity.toDomain(): Encounter = Encounter(
    id = id,
    contactId = contactId,
    occurredAt = occurredAt,
    placeId = placeId,
    eventLabel = eventLabel,
    notes = notes,
    latitude = latitude,
    longitude = longitude
)

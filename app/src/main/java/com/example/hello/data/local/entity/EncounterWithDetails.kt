package com.example.hello.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relational projection joining Encounter with its associated Contact and Place.
 * Used for timeline feed rendering (Ticket 04).
 */
data class EncounterWithDetails(
    @Embedded
    val encounter: EncounterEntity,
    @Relation(
        parentColumn = "contactId",
        entityColumn = "id"
    )
    val contact: ContactEntity? = null,
    @Relation(
        parentColumn = "placeId",
        entityColumn = "id"
    )
    val place: PlaceEntity? = null
)

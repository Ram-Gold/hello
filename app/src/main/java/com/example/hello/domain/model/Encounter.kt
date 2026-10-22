package com.example.hello.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure Kotlin domain model representing a recorded interaction or meeting between the user and a Contact.
 */
data class Encounter(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val occurredAt: Instant = Instant.now(),
    val placeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val eventLabel: String? = null,
    val notes: String = ""
)

package com.example.hello.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure Kotlin domain model representing a named physical venue or location where Encounters take place.
 */
data class Place(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val useCount: Int = 0,
    val lastUsedAt: Instant? = null
)

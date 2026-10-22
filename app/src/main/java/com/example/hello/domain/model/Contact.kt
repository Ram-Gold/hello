package com.example.hello.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure Kotlin domain model representing an individual with whom the user maintains
 * a personal or professional relationship.
 */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aliases: List<String> = emptyList(),
    val circles: List<Circle> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val notes: String = "",
    val photoUri: String? = null,
    val firstMetAt: Instant? = null,
    val lastMetAt: Instant? = null,
    val meetCount: Int = 0,
    val cadenceDays: Int? = null
)

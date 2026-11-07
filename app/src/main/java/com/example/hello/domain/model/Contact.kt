package com.example.hello.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Pure Kotlin domain model representing an individual with whom the user maintains
 * a personal or professional relationship.
 */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aliases: List<String> = emptyList(),
    val role: String? = null,
    val company: String? = null,
    val birthday: LocalDate? = null,
    val phone: String? = null,
    val email: String? = null,
    val tagIds: List<String> = emptyList(),
    val notes: String = "",
    val photoUri: String? = null,
    val firstMetAt: Instant? = null,
    val lastMetAt: Instant? = null,
    val meetCount: Int = 0,
    val cadenceDays: Int? = null,
    val isArchived: Boolean = false,
    val customProperties: Map<String, String> = emptyMap()
)

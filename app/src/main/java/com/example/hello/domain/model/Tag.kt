package com.example.hello.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure Kotlin domain model representing a lightweight freeform label attached to Contacts.
 */
data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String,
    val useCount: Int = 0,
    val lastUsedAt: Instant? = null
)

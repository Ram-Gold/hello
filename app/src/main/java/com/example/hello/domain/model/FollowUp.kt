package com.example.hello.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Pure Kotlin domain model representing an actionable stay-in-touch reminder or task tied to a Contact.
 */
data class FollowUp(
    val id: String = UUID.randomUUID().toString(),
    val contactId: String,
    val dueAt: Instant,
    val note: String = "",
    val doneAt: Instant? = null
) {
    val isDone: Boolean
        get() = doneAt != null
}

package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import java.time.Instant

/**
 * Room persistence entity for Contact.
 * Domain models remain pure and decoupled per ADR 0002.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val phone: String? = null,
    val email: String? = null,
    val circles: List<Circle> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val notes: String = "",
    val photoUri: String? = null,
    val firstMetAt: Instant? = null,
    val lastMetAt: Instant? = null,
    val meetCount: Int = 0,
    val cadenceDays: Int? = null,
    val isArchived: Boolean = false
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    name = name,
    aliases = aliases,
    phone = phone,
    email = email,
    circles = circles,
    tagIds = tagIds,
    notes = notes,
    photoUri = photoUri,
    firstMetAt = firstMetAt,
    lastMetAt = lastMetAt,
    meetCount = meetCount,
    cadenceDays = cadenceDays,
    isArchived = isArchived
)

fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    name = name,
    aliases = aliases,
    phone = phone,
    email = email,
    circles = circles,
    tagIds = tagIds,
    notes = notes,
    photoUri = photoUri,
    firstMetAt = firstMetAt,
    lastMetAt = lastMetAt,
    meetCount = meetCount,
    cadenceDays = cadenceDays,
    isArchived = isArchived
)

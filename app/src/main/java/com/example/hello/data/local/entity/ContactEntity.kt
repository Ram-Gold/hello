package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Contact
import java.time.Instant
import java.time.LocalDate

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
    val isFavorite: Boolean = false,
    val updatedAt: Instant? = null,
    val customProperties: Map<String, String> = emptyMap()
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    name = name,
    aliases = aliases,
    role = role,
    company = company,
    birthday = birthday,
    phone = phone,
    email = email,
    tagIds = tagIds,
    notes = notes,
    photoUri = photoUri,
    firstMetAt = firstMetAt,
    lastMetAt = lastMetAt,
    meetCount = meetCount,
    cadenceDays = cadenceDays,
    isArchived = isArchived,
    isFavorite = isFavorite,
    updatedAt = updatedAt,
    customProperties = customProperties
)

fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    name = name,
    aliases = aliases,
    role = role,
    company = company,
    birthday = birthday,
    phone = phone,
    email = email,
    tagIds = tagIds,
    notes = notes,
    photoUri = photoUri,
    firstMetAt = firstMetAt,
    lastMetAt = lastMetAt,
    meetCount = meetCount,
    cadenceDays = cadenceDays,
    isArchived = isArchived,
    isFavorite = isFavorite,
    updatedAt = updatedAt,
    customProperties = customProperties
)

package com.example.hello.data.repository

import androidx.room.Transactor
import androidx.room.useWriterConnection
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.EncounterWithDetails
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface for managing durable Encounter persistence and relational joins.
 * Backed by Room with atomic transaction updates in <2ms (Ticket 04).
 */
interface EncounterRepository {
    fun getAllEncountersFlow(): Flow<List<Encounter>>
    fun getEncountersForContactFlow(contactId: String): Flow<List<Encounter>>
    fun getEncountersWithDetailsFlow(): Flow<List<EncounterWithDetails>>
    suspend fun saveEncounterAtomic(
        encounter: Encounter,
        place: Place? = null,
        tagIds: List<String> = emptyList()
    )
}

class RoomEncounterRepository(
    private val database: HelloDatabase
) : EncounterRepository {

    override fun getAllEncountersFlow(): Flow<List<Encounter>> {
        return database.encounterDao().getAllEncountersFlow()
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getEncountersForContactFlow(contactId: String): Flow<List<Encounter>> {
        return database.encounterDao().getEncountersForContactFlow(contactId)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getEncountersWithDetailsFlow(): Flow<List<EncounterWithDetails>> {
        return database.encounterDao().getEncountersWithDetailsFlow()
    }

    override suspend fun saveEncounterAtomic(
        encounter: Encounter,
        place: Place?,
        tagIds: List<String>
    ) {
        val now = encounter.occurredAt
        database.useWriterConnection { transactor ->
            transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                // 1. Save encounter
                database.encounterDao().insertEncounter(encounter.toEntity())

                // 2. Update contact recency and counter
                val contact = database.contactDao().getContactById(encounter.contactId)
                if (contact != null) {
                    val updatedContact = contact.copy(
                        lastMetAt = now,
                        meetCount = contact.meetCount + 1
                    )
                    database.contactDao().updateContact(updatedContact)
                }

                // 3. Update or insert place
                if (place != null) {
                    val existingPlace = database.placeDao().getPlaceById(place.id)
                    if (existingPlace != null) {
                        val updated = existingPlace.copy(
                            useCount = existingPlace.useCount + 1,
                            lastUsedAt = now
                        )
                        database.placeDao().updatePlace(updated)
                    } else {
                        database.placeDao().insertPlace(place.toEntity().copy(useCount = 1, lastUsedAt = now))
                    }
                }

                // 4. Update tags use count and lastUsedAt
                tagIds.forEach { tagId ->
                    val existingTag = database.tagDao().getTagById(tagId)
                    if (existingTag != null) {
                        val updated = existingTag.copy(
                            useCount = existingTag.useCount + 1,
                            lastUsedAt = now
                        )
                        database.tagDao().updateTag(updated)
                    }
                }
            }
        }
    }
}

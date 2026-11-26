package com.example.hello.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.hello.data.local.entity.EncounterEntity
import com.example.hello.data.local.entity.EncounterWithDetails
import com.example.hello.data.local.entity.PersonEncounterCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {

    @Query("SELECT * FROM encounters ORDER BY occurredAt DESC")
    fun getAllEncountersFlow(): Flow<List<EncounterEntity>>

    @Query(
        """
        SELECT * FROM encounters WHERE contactId = :contactId
        UNION
        SELECT e.* FROM encounters e
        INNER JOIN person_encounters pe ON e.id = pe.encounterId
        WHERE pe.contactId = :contactId
        ORDER BY occurredAt DESC
        """
    )
    fun getEncountersForContactFlow(contactId: String): Flow<List<EncounterEntity>>

    @Transaction
    @Query("SELECT * FROM encounters ORDER BY occurredAt DESC")
    fun getEncountersWithDetailsFlow(): Flow<List<EncounterWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM encounters WHERE contactId = :contactId
        UNION
        SELECT e.* FROM encounters e
        INNER JOIN person_encounters pe ON e.id = pe.encounterId
        WHERE pe.contactId = :contactId
        ORDER BY occurredAt DESC
        """
    )
    fun getEncountersWithDetailsForContactFlow(contactId: String): Flow<List<EncounterWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonEncounters(refs: List<PersonEncounterCrossRef>)

    @Query("SELECT * FROM person_encounters")
    fun getAllPersonEncountersFlow(): Flow<List<PersonEncounterCrossRef>>

    @Query("SELECT * FROM person_encounters")
    suspend fun getAllPersonEncounters(): List<PersonEncounterCrossRef>

    @Query("SELECT contactId FROM person_encounters WHERE encounterId = :encounterId")
    suspend fun getParticipantIdsForEncounter(encounterId: String): List<String>

    @Query("DELETE FROM person_encounters WHERE encounterId = :encounterId")
    suspend fun deletePersonEncountersForEncounter(encounterId: String)

    @Query("DELETE FROM person_encounters WHERE contactId = :contactId")
    suspend fun deletePersonEncountersForContact(contactId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(encounter: EncounterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounters(encounters: List<EncounterEntity>)

    @Query("DELETE FROM encounters WHERE contactId = :contactId")
    suspend fun deleteEncountersForContact(contactId: String)

    @Query("DELETE FROM encounters WHERE id = :id")
    suspend fun deleteEncounterById(id: String)
}

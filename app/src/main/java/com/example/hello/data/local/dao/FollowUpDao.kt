package com.example.hello.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.hello.data.local.entity.FollowUpEntity
import com.example.hello.data.local.entity.PersonFollowUpCrossRef
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FollowUpDao {

    @Query("SELECT * FROM follow_ups WHERE doneAt IS NULL ORDER BY dueAt ASC")
    fun getPendingFollowUpsFlow(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups ORDER BY dueAt ASC")
    fun getAllFollowUpsFlow(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE doneAt IS NULL ORDER BY dueAt ASC")
    suspend fun getPendingFollowUps(): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups ORDER BY dueAt ASC")
    suspend fun getAllFollowUps(): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups WHERE id = :id LIMIT 1")
    suspend fun getFollowUpById(id: String): FollowUpEntity?

    @Query(
        """
        SELECT * FROM follow_ups WHERE contactId = :contactId
        UNION
        SELECT f.* FROM follow_ups f
        INNER JOIN person_follow_ups pf ON f.id = pf.followUpId
        WHERE pf.contactId = :contactId
        ORDER BY dueAt ASC
        """
    )
    fun getFollowUpsForContactFlow(contactId: String): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonFollowUps(refs: List<PersonFollowUpCrossRef>)

    @Query("SELECT * FROM person_follow_ups")
    fun getAllPersonFollowUpsFlow(): Flow<List<PersonFollowUpCrossRef>>

    @Query("SELECT * FROM person_follow_ups")
    suspend fun getAllPersonFollowUps(): List<PersonFollowUpCrossRef>

    @Query("SELECT contactId FROM person_follow_ups WHERE followUpId = :followUpId")
    suspend fun getParticipantIdsForFollowUp(followUpId: String): List<String>

    @Query("DELETE FROM person_follow_ups WHERE followUpId = :followUpId")
    suspend fun deletePersonFollowUpsForFollowUp(followUpId: String)

    @Query("DELETE FROM person_follow_ups WHERE contactId = :contactId")
    suspend fun deletePersonFollowUpsForContact(contactId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUps(followUps: List<FollowUpEntity>)

    @Update
    suspend fun updateFollowUp(followUp: FollowUpEntity)

    @Query("UPDATE follow_ups SET doneAt = :doneAt WHERE id = :id")
    suspend fun markDone(id: String, doneAt: Instant)

    @Query("UPDATE follow_ups SET doneAt = NULL WHERE id = :id")
    suspend fun clearDone(id: String)

    @Query("UPDATE follow_ups SET dueAt = :newDueAt WHERE id = :id")
    suspend fun snooze(id: String, newDueAt: Instant)

    @Query("DELETE FROM follow_ups WHERE id = :id")
    suspend fun deleteFollowUpById(id: String)

    @Query("DELETE FROM follow_ups WHERE contactId = :contactId")
    suspend fun deleteFollowUpsForContact(contactId: String)
}

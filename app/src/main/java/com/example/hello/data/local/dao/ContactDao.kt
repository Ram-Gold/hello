package com.example.hello.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getActiveContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun getContactFlow(id: String): Flow<ContactEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: String)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    // FTS full-text search
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactFts(fts: ContactFtsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactFtsList(ftsList: List<ContactFtsEntity>)

    @Query("DELETE FROM contacts_fts WHERE contactId = :contactId")
    suspend fun deleteContactFtsByContactId(contactId: String)

    @Query("""
        SELECT c.* FROM contacts c 
        JOIN contacts_fts fts ON c.id = fts.contactId 
        WHERE contacts_fts MATCH :query
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun searchContactsFts(query: String): Flow<List<ContactEntity>>

    @Query("""
        SELECT * FROM contacts 
        WHERE name LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%' 
           OR aliases LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchContactsLike(query: String): Flow<List<ContactEntity>>
}

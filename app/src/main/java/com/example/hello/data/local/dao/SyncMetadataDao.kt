package com.example.hello.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hello.data.local.entity.SyncMetadataEntity

/**
 * Data Access Object for local metadata and Markdown sync markers.
 */
@Dao
interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    suspend fun getByKey(key: String): SyncMetadataEntity?
}

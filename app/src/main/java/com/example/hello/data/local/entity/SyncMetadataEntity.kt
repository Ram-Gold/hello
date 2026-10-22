package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * SQLite entity for tracking local cache sync state and Markdown reconciliation timestamps.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Instant = Instant.now()
)

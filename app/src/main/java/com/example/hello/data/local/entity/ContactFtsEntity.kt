package com.example.hello.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table for sub-10ms full-text search across contacts (Ticket 03).
 */
@Entity(tableName = "contacts_fts")
@Fts4
data class ContactFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,
    val contactId: String,
    val name: String,
    val aliases: String,
    val notes: String
)

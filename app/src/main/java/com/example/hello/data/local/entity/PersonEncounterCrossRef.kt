package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "person_encounters",
    primaryKeys = ["encounterId", "contactId"],
    indices = [Index(value = ["contactId"])]
)
data class PersonEncounterCrossRef(
    val encounterId: String,
    val contactId: String
)

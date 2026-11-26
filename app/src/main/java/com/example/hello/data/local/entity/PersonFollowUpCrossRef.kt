package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "person_follow_ups",
    primaryKeys = ["followUpId", "contactId"],
    indices = [Index(value = ["contactId"])]
)
data class PersonFollowUpCrossRef(
    val followUpId: String,
    val contactId: String
)

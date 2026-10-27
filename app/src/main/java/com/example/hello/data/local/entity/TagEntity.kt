package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Tag
import java.time.Instant

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String,
    val useCount: Int = 0,
    val lastUsedAt: Instant? = null
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    useCount = useCount,
    lastUsedAt = lastUsedAt
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorHex = colorHex,
    useCount = useCount,
    lastUsedAt = lastUsedAt
)

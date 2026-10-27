package com.example.hello.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hello.domain.model.Place
import java.time.Instant

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val useCount: Int = 0,
    val lastUsedAt: Instant? = null
)

fun Place.toEntity(): PlaceEntity = PlaceEntity(
    id = id,
    label = label,
    latitude = latitude,
    longitude = longitude,
    useCount = useCount,
    lastUsedAt = lastUsedAt
)

fun PlaceEntity.toDomain(): Place = Place(
    id = id,
    label = label,
    latitude = latitude,
    longitude = longitude,
    useCount = useCount,
    lastUsedAt = lastUsedAt
)

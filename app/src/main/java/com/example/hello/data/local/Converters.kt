package com.example.hello.data.local

import androidx.room.TypeConverter
import com.example.hello.domain.model.Circle
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Room TypeConverters for mapping domain types to SQLite-compatible primitives.
 * Complies with ADR 0002 §3 enforcing ISO-8601 timestamp serialization.
 */
class Converters {

    @TypeConverter
    fun fromInstantString(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    @TypeConverter
    fun toInstantString(instant: Instant?): String? {
        return instant?.toString()
    }

    @TypeConverter
    fun fromUuidString(value: String?): UUID? {
        if (value.isNullOrBlank()) return null
        return try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @TypeConverter
    fun toUuidString(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun fromCircleListString(value: String?): List<Circle> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { name ->
            try {
                Circle.valueOf(name.trim())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun toCircleListString(circles: List<Circle>?): String? {
        if (circles == null) return null
        return circles.joinToString(",") { it.name }
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("\u001F").filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null) return null
        return list.joinToString("\u001F")
    }
}

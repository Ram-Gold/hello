package com.example.hello.data.local

import com.example.hello.domain.model.Circle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun instant_roundtrip_preservesIso8601String() {
        val now = Instant.parse("2026-10-22T15:30:00.123456Z")
        val isoStr = converters.toInstantString(now)
        val roundtrip = converters.fromInstantString(isoStr)

        assertEquals("2026-10-22T15:30:00.123456Z", isoStr)
        assertEquals(now, roundtrip)
    }

    @Test
    fun instant_handlesNullAndMalformedGracefully() {
        assertNull(converters.toInstantString(null))
        assertNull(converters.fromInstantString(null))
        assertNull(converters.fromInstantString(""))
        assertNull(converters.fromInstantString("not-a-date"))
    }

    @Test
    fun uuid_roundtrip_preservesUuid() {
        val original = UUID.randomUUID()
        val str = converters.toUuidString(original)
        val roundtrip = converters.fromUuidString(str)

        assertEquals(original.toString(), str)
        assertEquals(original, roundtrip)
    }

    @Test
    fun uuid_handlesNullAndMalformedGracefully() {
        assertNull(converters.toUuidString(null))
        assertNull(converters.fromUuidString(null))
        assertNull(converters.fromUuidString("invalid-uuid"))
    }

    @Test
    fun circleList_roundtrip_preservesCircles() {
        val circles = listOf(Circle.WORK, Circle.FRIENDS)
        val serialized = converters.toCircleListString(circles)
        val roundtrip = converters.fromCircleListString(serialized)

        assertEquals(circles, roundtrip)
    }

    @Test
    fun circleList_handlesEmptyAndNull() {
        assertEquals(emptyList<Circle>(), converters.fromCircleListString(null))
        assertEquals(emptyList<Circle>(), converters.fromCircleListString(""))
        assertEquals("", converters.toCircleListString(emptyList()))
        assertNull(converters.toCircleListString(null as List<Circle>?))
    }

    @Test
    fun stringList_roundtrip_preservesEntries() {
        val list = listOf("S-Lin", "Sarah", "Tech Lead")
        val serialized = converters.toStringList(list)
        val roundtrip = converters.fromStringList(serialized)

        assertEquals(list, roundtrip)
    }

    @Test
    fun stringList_handlesEmptyAndNull() {
        assertEquals(emptyList<String>(), converters.fromStringList(null))
        assertEquals(emptyList<String>(), converters.fromStringList(""))
        assertEquals("", converters.toStringList(emptyList()))
        assertNull(converters.toStringList(null as List<String>?))
    }
}

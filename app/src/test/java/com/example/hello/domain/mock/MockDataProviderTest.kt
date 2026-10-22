package com.example.hello.domain.mock

import com.example.hello.domain.model.Circle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MockDataProviderTest {

    @Test
    fun testMockContactsIntegrity() {
        val contacts = MockDataProvider.contacts
        assertTrue("Should have mock contacts", contacts.isNotEmpty())

        val sarah = contacts.find { it.name == "Sarah Lin" }
        assertNotNull("Sarah Lin should exist in mock contacts", sarah)
        assertEquals(3, sarah!!.meetCount)
        assertEquals(30, sarah.cadenceDays)
        assertTrue("Sarah should have aliases", sarah.aliases.contains("S-Lin"))
        assertTrue("Sarah should have WORK circle", sarah.circles.contains(Circle.WORK))

        val alex = contacts.find { it.name == "Alex Rivera" }
        assertNotNull("Alex Rivera should exist in mock contacts", alex)
        assertEquals(1, alex!!.meetCount)
        assertTrue("Alex should have FRIENDS circle", alex.circles.contains(Circle.FRIENDS))

        val elena = contacts.find { it.name == "Elena Gomez" }
        assertNotNull("Elena Gomez should exist in mock contacts", elena)
        assertTrue("Elena should have FAMILY circle", elena!!.circles.contains(Circle.FAMILY))
        assertEquals(7, elena.cadenceDays)
    }

    @Test
    fun testMockEncountersIntegrity() {
        val encounters = MockDataProvider.encounters
        assertTrue("Should have mock encounters", encounters.isNotEmpty())

        val sarah = MockDataProvider.contacts.first { it.name == "Sarah Lin" }
        val sarahEncounters = encounters.filter { it.contactId == sarah.id }
        assertEquals("Sarah Lin should have 3 encounters", 3, sarahEncounters.size)

        val firstEncounter = sarahEncounters.first()
        assertNotNull(firstEncounter.placeId)
        assertNotNull(firstEncounter.latitude)
        assertNotNull(firstEncounter.longitude)
        assertTrue(firstEncounter.notes.isNotBlank())
    }

    @Test
    fun testMockPlacesIntegrity() {
        val places = MockDataProvider.places
        assertTrue("Should have mock places", places.isNotEmpty())

        val coffeeShop = places.find { it.label.contains("Coffee") }
        assertNotNull("Should have coffee shop place", coffeeShop)
        assertTrue(coffeeShop!!.useCount > 0)
        assertNotNull(coffeeShop.latitude)
        assertNotNull(coffeeShop.longitude)

        val conference = places.find { it.label.contains("Conference") || it.label.contains("Center") }
        assertNotNull("Should have conference/convention place", conference)
    }

    @Test
    fun testMockTagsIntegrity() {
        val tags = MockDataProvider.tags
        assertTrue("Should have mock tags", tags.isNotEmpty())

        tags.forEach { tag ->
            assertTrue("ColorHex should start with #", tag.colorHex.startsWith("#"))
            assertTrue("Name should not be empty", tag.name.isNotBlank())
        }
    }

    @Test
    fun testMockFollowUpsIntegrity() {
        val followUps = MockDataProvider.followUps
        assertTrue("Should have mock follow-ups", followUps.isNotEmpty())

        val pending = followUps.filter { !it.isDone }
        val done = followUps.filter { it.isDone }

        assertTrue("Should have pending follow-ups", pending.isNotEmpty())
        assertTrue("Should have completed follow-ups", done.isNotEmpty())

        pending.forEach {
            assertNull(it.doneAt)
            assertFalse(it.isDone)
        }
        done.forEach {
            assertNotNull(it.doneAt)
            assertTrue(it.isDone)
        }
    }
}

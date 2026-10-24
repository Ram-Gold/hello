package com.example.hello.data.repository

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.UpcomingAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ContactRepositoryTest {

    private lateinit var repository: ContactRepository

    @Before
    fun setUp() {
        repository = ContactRepository()
    }

    @Test
    fun initialState_containsSeededContactsPlacesAndTags() {
        assertTrue(repository.contacts.value.isNotEmpty())
        assertEquals(4, repository.recentContacts.value.size)
        assertTrue(repository.places.value.isNotEmpty())
        assertTrue(repository.tags.value.isNotEmpty())
    }

    @Test
    fun addContactWithInitialEncounter_prependsContactCreatesEncounterAndFollowUp() {
        val initialContactCount = repository.contacts.value.size
        val initialRecentCount = repository.recentContacts.value.size
        val targetPlace = repository.places.value.first()
        val initialPlaceUseCount = targetPlace.useCount
        val targetTag = repository.tags.value.first()

        val addedContact = repository.addContactWithInitialEncounter(
            name = "Hannah Abbott",
            circles = setOf(Circle.FRIENDS, Circle.WORK),
            tagIds = setOf(targetTag.id),
            placeId = targetPlace.id,
            customPlaceName = null,
            note = "Met at herbology symposium",
            followUpDays = 3
        )

        assertEquals("Hannah Abbott", addedContact.name)
        assertEquals("HA", addedContact.avatarInitials)
        assertEquals(initialContactCount + 1, repository.contacts.value.size)
        assertEquals(initialRecentCount + 1, repository.recentContacts.value.size)
        assertEquals("Hannah Abbott", repository.recentContacts.value.first().name)
        assertTrue(addedContact.contextSubtitle.contains("Met at herbology symposium"))
        assertEquals("Met today", addedContact.lastEncounterText)

        // Verify Encounter created
        val encounter = repository.encounters.value.firstOrNull { it.contactId == addedContact.id }
        assertNotNull(encounter)
        assertEquals(targetPlace.id, encounter?.placeId)
        assertEquals("Met at herbology symposium", encounter?.notes)

        // Verify Place use count incremented
        val updatedPlace = repository.places.value.first { it.id == targetPlace.id }
        assertEquals(initialPlaceUseCount + 1, updatedPlace.useCount)

        // Verify FollowUp created
        val followUp = repository.followUps.value.firstOrNull { it.contactId == addedContact.id }
        assertNotNull(followUp)
    }

    @Test
    fun addContactWithCustomPlace_createsNewPlaceAndLinksEncounter() {
        val initialPlaceCount = repository.places.value.size

        val addedContact = repository.addContactWithInitialEncounter(
            name = "Oliver Wood",
            circles = setOf(Circle.UNI),
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = "Tartine Bakery",
            note = "Talked about strategy",
            followUpDays = null
        )

        assertEquals(initialPlaceCount + 1, repository.places.value.size)
        val createdPlace = repository.places.value.firstOrNull { it.label == "Tartine Bakery" }
        assertNotNull(createdPlace)
        assertEquals(1, createdPlace?.useCount)

        val encounter = repository.encounters.value.first { it.contactId == addedContact.id }
        assertEquals(createdPlace?.id, encounter.placeId)
    }

    @Test
    fun logEncounterForContact_updatesRecencyAndResetsCadence() {
        val existingContact = repository.contacts.value.first { it.name.startsWith("Sarah") }

        val encounter = repository.logEncounterForContact(
            contactId = existingContact.id,
            date = LocalDate.now(),
            placeId = repository.places.value.first().id,
            customPlaceName = null,
            note = "Caught up about her new job at Figma"
        )

        assertEquals(existingContact.id, encounter.contactId)
        assertEquals("Caught up about her new job at Figma", encounter.notes)

        // Verify Contact moved to top of recentContacts and updated
        val topRecent = repository.recentContacts.value.first()
        assertEquals(existingContact.id, topRecent.id)
        assertEquals("Met today", topRecent.lastEncounterText)
        assertTrue(topRecent.contextSubtitle.contains("Caught up about her new job at Figma"))

        // Verify CadenceAlert for Sarah is cleared/reset
        val remainingSarahCadence = repository.upcomingAlerts.value.filter {
            it is UpcomingAlert.CadenceAlert && it.contactName.startsWith("Sarah")
        }
        assertTrue(remainingSarahCadence.isEmpty())
    }
}

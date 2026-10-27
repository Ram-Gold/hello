package com.example.hello.data.repository

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.UpcomingAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun getContact_returnsFullContactDetails() {
        val contact = repository.getContact("c-1")
        assertNotNull(contact)
        assertEquals("Alex Rivera", contact?.name)
        assertEquals("+1 (555) 876-5432", contact?.phone)
        assertEquals("alex.rivera@example.com", contact?.email)
        assertEquals(14, contact?.cadenceDays)
    }

    @Test
    fun updateContact_updatesFullContactAndSummaries() {
        val initial = repository.getContact("c-1")!!
        val updated = initial.copy(
            name = "Alexander Rivera",
            phone = "+1 (555) 999-0000",
            notes = "Updated notes"
        )
        repository.updateContact(updated)

        val fetched = repository.getContact("c-1")
        assertEquals("Alexander Rivera", fetched?.name)
        assertEquals("+1 (555) 999-0000", fetched?.phone)

        val inContacts = repository.contacts.value.first { it.id == "c-1" }
        assertEquals("Alexander Rivera", inContacts.name)
        assertEquals("+1 (555) 999-0000", inContacts.phone)
    }

    @Test
    fun archiveContact_marksContactArchivedAndClearsCadenceAlert() {
        val initial = repository.getContact("c-1")!!
        assertFalse(initial.isArchived)

        repository.archiveContact("c-1", archived = true)

        val archived = repository.getContact("c-1")!!
        assertTrue(archived.isArchived)

        val inContacts = repository.contacts.value.first { it.id == "c-1" }
        assertTrue(inContacts.isArchived)
    }

    @Test
    fun deleteContact_removesContactAndCascadesEncounters() {
        val initialContact = repository.getContact("c-1")
        assertNotNull(initialContact)
        val initialEncounters = repository.encounters.value.filter { it.contactId == "c-1" }
        assertTrue(initialEncounters.isNotEmpty())

        repository.deleteContact("c-1")

        assertNull(repository.getContact("c-1"))
        assertTrue(repository.contacts.value.none { it.id == "c-1" })
        assertTrue(repository.recentContacts.value.none { it.id == "c-1" })
        assertTrue(repository.encounters.value.none { it.contactId == "c-1" })
    }

    @Test
    fun addTag_createsNewTagSuccessfully() {
        val newTag = repository.addTag("Mentorship", "#4A6572")
        assertEquals("Mentorship", newTag.name)
        assertEquals("#4A6572", newTag.colorHex)
        assertTrue(repository.tags.value.any { it.id == newTag.id })
    }

    @Test
    fun updateTag_modifiesNameAndColor() {
        val initial = repository.tags.value.first()
        val updated = initial.copy(name = "UpdatedTech", colorHex = "#7DAA82")
        repository.updateTag(updated)

        val retrieved = repository.tags.value.first { it.id == initial.id }
        assertEquals("UpdatedTech", retrieved.name)
        assertEquals("#7DAA82", retrieved.colorHex)
    }

    @Test
    fun mergeTags_migratesContactsCombinesUseCountsAndDeletesSource() {
        // Tag tech is on c-2 and c-8. Let's merge tag-tech into tag-design.
        val sourceTag = repository.tags.value.first { it.id == "tag-tech" }
        val targetTag = repository.tags.value.first { it.id == "tag-design" }
        val expectedCombinedCount = sourceTag.useCount + targetTag.useCount

        repository.mergeTags(sourceTagId = "tag-tech", targetTagId = "tag-design")

        // 1. sourceTag is deleted from tags
        assertNull(repository.tags.value.firstOrNull { it.id == "tag-tech" })

        // 2. targetTag has combined useCount
        val updatedTarget = repository.tags.value.first { it.id == "tag-design" }
        assertEquals(expectedCombinedCount, updatedTarget.useCount)

        // 3. Contacts that had tag-tech now have tag-design without duplicates
        val c2 = repository.getContact("c-2")!!
        assertFalse(c2.tagIds.contains("tag-tech"))
        assertTrue(c2.tagIds.contains("tag-design"))

        val c8 = repository.getContact("c-8")!!
        assertFalse(c8.tagIds.contains("tag-tech"))
        assertTrue(c8.tagIds.contains("tag-design"))
    }

    @Test
    fun deleteTag_unlinksCleanlyFromAllContacts() {
        val tagToDelete = "tag-design"
        val c1Before = repository.getContact("c-1")!!
        assertTrue(c1Before.tagIds.contains(tagToDelete))

        repository.deleteTag(tagToDelete)

        // Tag removed from tags
        assertNull(repository.tags.value.firstOrNull { it.id == tagToDelete })

        // Tag unlinked from contact
        val c1After = repository.getContact("c-1")!!
        assertFalse(c1After.tagIds.contains(tagToDelete))
    }
}

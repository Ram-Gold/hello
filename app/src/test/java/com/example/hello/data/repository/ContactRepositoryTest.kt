package com.example.hello.data.repository

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
    fun addTag_stripsHashAndAssignsDeterministicColorWhenOmitted() {
        val tagWithHash = repository.addTag("#hiking")
        assertEquals("hiking", tagWithHash.name)
        assertTrue(com.example.hello.domain.model.TagColorPalette.PALETTE_HEX.contains(tagWithHash.colorHex))
        assertEquals(com.example.hello.domain.model.TagColorPalette.getDeterministicColorHex("hiking"), tagWithHash.colorHex)
    }

    @Test
    fun updateTag_modifiesNameAndColor() {
        val initial = repository.tags.value.first()
        val updated = initial.copy(name = "UpdatedTech", colorHex = "#7DAA82")
        repository.updateTag(updated)

        val retrieved = repository.tags.value.first { it.id == initial.id }
        assertEquals("UpdatedTech", retrieved.name)
        assertEquals("#7DAA82", retrieved.colorHex)

        // Verify contacts carrying this tag are reactively updated
        val contactsWithTag = repository.contacts.value.filter { it.tags.any { t -> t.id == initial.id } }
        assertTrue(contactsWithTag.isNotEmpty())
        contactsWithTag.forEach { contact ->
            val tag = contact.tags.first { it.id == initial.id }
            assertEquals("UpdatedTech", tag.name)
            assertEquals("#7DAA82", tag.colorHex)
        }

        // Verify recentContacts carrying this tag are reactively updated
        val recentWithTag = repository.recentContacts.value.filter { it.tags.any { t -> t.id == initial.id } }
        recentWithTag.forEach { contact ->
            val tag = contact.tags.first { it.id == initial.id }
            assertEquals("UpdatedTech", tag.name)
            assertEquals("#7DAA82", tag.colorHex)
        }
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

        // 4. Reactive contacts and recentContacts summaries are updated
        val c2Summary = repository.contacts.value.first { it.id == "c-2" }
        assertFalse(c2Summary.tags.any { it.id == "tag-tech" })
        assertTrue(c2Summary.tags.any { it.id == "tag-design" })
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

        // Tag unlinked from reactive contacts summary
        val c1Summary = repository.contacts.value.first { it.id == "c-1" }
        assertFalse(c1Summary.tags.any { it.id == tagToDelete })
    }

    @Test
    fun updateContactPhotoUri_propagatesToContactSummariesAndTimelineNodes() {
        val original = repository.getContact("c-1")!!
        val customAvatarPath = "/data/user/0/com.example.hello/files/avatars/contact_c-1_1000.png"
        val updated = original.copy(
            photoUri = customAvatarPath,
            birthday = java.time.LocalDate.now().plusDays(5)
        )

        repository.updateContact(updated)

        // 1. ContactSummary on contacts and recentContacts reflects photoUri
        val summaryInContacts = repository.contacts.value.first { it.id == "c-1" }
        assertEquals(customAvatarPath, summaryInContacts.photoUri)

        val summaryInRecents = repository.recentContacts.value.firstOrNull { it.id == "c-1" }
        if (summaryInRecents != null) {
            assertEquals(customAvatarPath, summaryInRecents.photoUri)
        }

        // 2. Timeline nodes reflect photoUri for both past encounters and future nodes
        val (futureNodes, pastNodes) = com.example.hello.ui.timeline.TimelineViewModel.buildTimelineNodes(
            contactsMap = repository.fullContacts.value,
            encounters = repository.encounters.value,
            placesMap = repository.places.value.associateBy { it.id },
            followUps = repository.followUps.value
        )
        val c1PastNode = pastNodes.firstOrNull { it.contactId == "c-1" }
        assertNotNull(c1PastNode)
        assertEquals(customAvatarPath, c1PastNode?.photoUri)

        val c1FutureNode = futureNodes.firstOrNull { it.contactId == "c-1" }
        assertNotNull(c1FutureNode)
        assertEquals(customAvatarPath, c1FutureNode?.photoUri)
    }

    @Test
    fun deleteEncounters_recalculatesContactMeetCountAndRecencyText() {
        val contact = repository.addContactWithInitialEncounter(
            name = "Test Person",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "First met today",
            followUpDays = null
        )

        // Contact starts with 1 encounter
        val initialFullContact = repository.fullContacts.value[contact.id]
        assertNotNull(initialFullContact)
        assertEquals(1, initialFullContact?.meetCount)

        val firstEncounter = repository.encounters.value.first { it.contactId == contact.id }

        // Log a second encounter
        val secondEncounter = repository.logEncounterForContact(
            contactId = contact.id,
            date = LocalDate.now(),
            placeId = null,
            customPlaceName = null,
            note = "Met again"
        )
        val contactAfterSecond = repository.fullContacts.value[contact.id]
        assertEquals(2, contactAfterSecond?.meetCount)

        // Delete the second encounter
        repository.deleteEncounters(setOf(secondEncounter.id))
        val contactAfterDeleteSecond = repository.fullContacts.value[contact.id]
        assertEquals(1, contactAfterDeleteSecond?.meetCount)
        assertEquals(firstEncounter.occurredAt, contactAfterDeleteSecond?.lastMetAt)

        // Delete initial encounter (allowed per ADR-0023)
        repository.deleteEncounters(setOf(firstEncounter.id))
        val contactAfterDeleteAll = repository.fullContacts.value[contact.id]
        assertEquals(0, contactAfterDeleteAll?.meetCount)
        assertNull(contactAfterDeleteAll?.lastMetAt)

        val summaryAfterDeleteAll = repository.contacts.value.first { it.id == contact.id }
        assertNull(summaryAfterDeleteAll.lastEncounterText)
    }

    @Test
    fun deleteFollowUps_removesFromFollowUpsAndUpcomingAlerts() {
        val contact = repository.addContactWithInitialEncounter(
            name = "FollowUp Test",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Initial note",
            followUpDays = 3
        )

        val followUp = repository.followUps.value.first { it.contactId == contact.id }
        assertTrue(repository.upcomingAlerts.value.any { it.id == followUp.id })

        repository.deleteFollowUps(setOf(followUp.id))
        assertFalse(repository.followUps.value.any { it.id == followUp.id })
        assertFalse(repository.upcomingAlerts.value.any { it.id == followUp.id })
    }

    @Test
    fun birthdayAlerts_enforces14DayHorizonAndTodayCelebration() {
        val today = java.time.LocalDate.now()

        // 1. Contact with birthday today (0 days)
        val contactToday = repository.addContactWithInitialEncounter(
            name = "Bday Today",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Met today",
            followUpDays = null
        )
        val fullToday = repository.getContact(contactToday.id)!!
        repository.updateContact(fullToday.copy(birthday = today))

        // 2. Contact with birthday in 7 days (within 14 days)
        val contactSoon = repository.addContactWithInitialEncounter(
            name = "Bday In 7 Days",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Met recently",
            followUpDays = null
        )
        val fullSoon = repository.getContact(contactSoon.id)!!
        repository.updateContact(fullSoon.copy(birthday = today.plusDays(7)))

        // 3. Contact with birthday in 25 days (beyond 14 days)
        val contactFar = repository.addContactWithInitialEncounter(
            name = "Bday In 25 Days",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Met weeks ago",
            followUpDays = null
        )
        val fullFar = repository.getContact(contactFar.id)!!
        repository.updateContact(fullFar.copy(birthday = today.plusDays(25)))

        val alerts = repository.upcomingAlerts.value.filterIsInstance<com.example.hello.domain.model.UpcomingAlert.BirthdayAlert>()

        // Contact Today must be present with dateText = "Today!"
        val todayAlert = alerts.firstOrNull { it.contactId == contactToday.id }
        assertNotNull(todayAlert)
        assertEquals("Today!", todayAlert?.dateText)

        // Contact Soon (7 days) must be present
        val soonAlert = alerts.firstOrNull { it.contactId == contactSoon.id }
        assertNotNull(soonAlert)
        assertEquals("In 7 days", soonAlert?.dateText)

        // Contact Far (25 days) must NOT be in 14-day upcoming alerts
        val farAlert = alerts.firstOrNull { it.contactId == contactFar.id }
        assertNull(farAlert)
    }

    @Test
    fun toggleFavorite_togglesStatusAndUpdatesFavoritesStream() {
        val contact = repository.contacts.value.first()
        val initialFull = repository.getContact(contact.id)!!
        assertFalse(initialFull.isFavorite)

        repository.toggleFavorite(contact.id)
        val favorited = repository.getContact(contact.id)!!
        assertTrue(favorited.isFavorite)
        assertNotNull(favorited.updatedAt)
        assertTrue(repository.favoriteContacts.value.any { it.id == contact.id })

        repository.toggleFavorite(contact.id)
        val unfavorited = repository.getContact(contact.id)!!
        assertFalse(unfavorited.isFavorite)
        assertFalse(repository.favoriteContacts.value.any { it.id == contact.id })
    }

    @Test
    fun favoriteContacts_sortedByMostRecentlyMetOrUpdated() {
        val contact1 = repository.contacts.value[0]
        val contact2 = repository.contacts.value[1]

        repository.toggleFavorite(contact1.id)
        repository.toggleFavorite(contact2.id)

        // Log encounter for contact1, which updates its updatedAt / lastMetAt
        repository.logEncounterForContact(contact1.id, note = "Coffee chat")

        val favorites = repository.favoriteContacts.value
        assertEquals(contact1.id, favorites.first().id)
    }

    @Test
    fun deleteContact_removesFromFavorites() {
        val contact = repository.contacts.value.first()
        repository.toggleFavorite(contact.id)
        assertTrue(repository.favoriteContacts.value.any { it.id == contact.id })

        repository.deleteContact(contact.id)
        assertFalse(repository.favoriteContacts.value.any { it.id == contact.id })
    }

    @Test
    fun archiveContact_and_undoLastArchive_restoresContact() {
        val contact = repository.contacts.value.first()
        repository.archiveContact(contact.id, archived = true)
        assertTrue(repository.fullContacts.value[contact.id]?.isArchived == true)

        val undone = repository.undoLastArchive()
        assertTrue(undone)
        assertTrue(repository.fullContacts.value[contact.id]?.isArchived == false)
    }

    @Test
    fun deleteContact_and_undoLastDeleteContact_restoresContact() {
        val contact = repository.contacts.value.first()
        val contactId = contact.id
        val contactName = contact.name

        repository.deleteContact(contactId)
        assertNull(repository.fullContacts.value[contactId])
        assertFalse(repository.contacts.value.any { it.id == contactId })

        val undone = repository.undoLastDeleteContact()
        assertTrue(undone)
        assertNotNull(repository.fullContacts.value[contactId])
        assertEquals(contactName, repository.fullContacts.value[contactId]?.name)
        assertTrue(repository.contacts.value.any { it.id == contactId })
    }

    @Test
    fun deleteEncounters_and_undoLastDeleteEncounters_restoresEncounters() {
        val encounter = repository.encounters.value.first()
        val encounterId = encounter.id

        repository.deleteEncounters(setOf(encounterId))
        assertFalse(repository.encounters.value.any { it.id == encounterId })

        val undone = repository.undoLastDeleteEncounters()
        assertTrue(undone)
        assertTrue(repository.encounters.value.any { it.id == encounterId })
    }
}

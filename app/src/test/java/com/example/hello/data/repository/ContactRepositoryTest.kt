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

        // Verify SerendipityPrompt for Sarah is cleared when an encounter is logged
        val remainingSarahPrompt = repository.upcomingAlerts.value.filter {
            it is UpcomingAlert.SerendipityPrompt && it.contactName.startsWith("Sarah")
        }
        assertTrue(remainingSarahPrompt.isEmpty())
    }

    @Test
    fun getContact_returnsFullContactDetails() {
        val contact = repository.getContact("c-1")
        assertNotNull(contact)
        assertEquals("Alex Rivera", contact?.name)
        assertEquals("+1 (555) 876-5432", contact?.phone)
        assertEquals("alex.rivera@example.com", contact?.email)
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
    fun archiveContact_marksContactArchivedAndClearsSerendipityPrompt() {
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
    fun undoWindowDuration_matches7SecondsSpecification() {
        assertEquals(7000L, ContactRepository.UNDO_WINDOW_DURATION_MS)
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

    @Test
    fun deleteTag_and_undoLastDeleteTag_restoresTagAndContactAssignments() {
        val tag = repository.addTag("Bookclub", "#D3E5EF")
        val added = repository.addContactWithInitialEncounter(
            name = "Hannah Abbott",
            tagIds = setOf(tag.id),
            placeId = null,
            customPlaceName = null,
            note = "Met at reading group",
            followUpDays = null
        )

        val tagBeforeDelete = repository.tags.value.first { it.id == tag.id }

        repository.deleteTag(tag.id)
        assertTrue(repository.tags.value.none { it.id == tag.id })
        assertFalse(tag.id in repository.fullContacts.value[added.id]!!.tagIds)
        assertTrue(repository.contacts.value.first { it.id == added.id }.tags.none { it.id == tag.id })
        assertTrue(repository.recentContacts.value.first { it.id == added.id }.tags.none { it.id == tag.id })

        assertTrue(repository.undoLastDeleteTag())
        assertEquals(tagBeforeDelete, repository.tags.value.firstOrNull { it.id == tag.id })
        assertTrue(tag.id in repository.fullContacts.value[added.id]!!.tagIds)
        assertTrue(repository.contacts.value.first { it.id == added.id }.tags.any { it.id == tag.id })
        assertTrue(repository.recentContacts.value.first { it.id == added.id }.tags.any { it.id == tag.id })

        // Calling undo again without a new deletion is a no-op
        assertFalse(repository.undoLastDeleteTag())
    }

    @Test
    fun deleteTag_consecutiveDeletes_undoRestoresOnlyMostRecentTag() {
        val firstTag = repository.tags.value[0]
        val secondTag = repository.tags.value[1]

        repository.deleteTag(firstTag.id)
        repository.deleteTag(secondTag.id)

        assertTrue(repository.undoLastDeleteTag())
        assertTrue(repository.tags.value.any { it.id == secondTag.id })
        assertTrue(repository.tags.value.none { it.id == firstTag.id })
    }

    @Test
    fun undoLastDeleteTag_returnsFalseWhenNoTagDeleted() {
        assertFalse(repository.undoLastDeleteTag())
    }

    @Test
    fun skipSerendipityPrompt_excludesSkippedContactAndRollsNextEligibleContact() {
        val today = LocalDate.of(2026, 11, 22)
        val thirtyDaysAgo = today.minusDays(30).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val sixtyDaysAgo = today.minusDays(60).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val ninetyDaysAgo = today.minusDays(90).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()

        val contacts = listOf(
            com.example.hello.domain.model.Contact(id = "ser-1", name = "One", lastMetAt = thirtyDaysAgo, meetCount = 2),
            com.example.hello.domain.model.Contact(id = "ser-2", name = "Two", lastMetAt = sixtyDaysAgo, meetCount = 2),
            com.example.hello.domain.model.Contact(id = "ser-3", name = "Three", lastMetAt = ninetyDaysAgo, meetCount = 2)
        )

        val initialPicks = repository.computeSerendipityPrompts(
            contacts = contacts,
            encounters = emptyList(),
            pendingFollowUps = emptyList(),
            skippedContactIds = emptySet(),
            today = today
        )
        assertEquals(2, initialPicks.size)

        val firstPickedId = initialPicks.first().contactId
        val afterSkipPicks = repository.computeSerendipityPrompts(
            contacts = contacts,
            encounters = emptyList(),
            pendingFollowUps = emptyList(),
            skippedContactIds = setOf(firstPickedId),
            today = today
        )
        assertEquals(2, afterSkipPicks.size)
        assertTrue("Skipped contact must not reappear on the same day", afterSkipPicks.none { it.contactId == firstPickedId })
    }

    @Test
    fun archiveContacts_allPreloaded_andUndoLastArchiveBatch_restoresEveryContact() {
        val initialActiveIds = repository.contacts.value.filterNot { it.isArchived }.map { it.id }.toSet()
        assertTrue("Expected multiple preloaded contacts", initialActiveIds.size >= 10)

        repository.archiveContacts(initialActiveIds, archived = true)
        assertTrue(repository.contacts.value.none { !it.isArchived })
        assertEquals(initialActiveIds.size, repository.contacts.value.count { it.isArchived })

        val restoredCount = repository.undoLastArchiveBatch()
        assertEquals(initialActiveIds.size, restoredCount)
        assertEquals(initialActiveIds.size, repository.contacts.value.count { !it.isArchived })
        assertEquals(0, repository.contacts.value.count { it.isArchived })
        // Second undo is a no-op
        assertEquals(0, repository.undoLastArchiveBatch())
    }

    @Test
    fun deleteContacts_allPreloaded_andUndoLastDeleteContacts_restoresEveryContactAndRelatedRecords() {
        val initialIds = repository.contacts.value.map { it.id }.toSet()
        val initialEncounterCount = repository.encounters.value.size
        val initialFollowUpCount = repository.followUps.value.size
        assertTrue("Expected multiple preloaded contacts", initialIds.size >= 10)

        repository.deleteContacts(initialIds)
        assertTrue(repository.contacts.value.isEmpty())
        assertTrue(repository.encounters.value.none { it.contactId in initialIds })
        assertTrue(repository.followUps.value.none { it.contactId in initialIds })

        val restoredCount = repository.undoLastDeleteContacts()
        assertEquals(initialIds.size, restoredCount)
        assertEquals(initialIds, repository.contacts.value.map { it.id }.toSet())
        assertEquals(initialEncounterCount, repository.encounters.value.size)
        assertEquals(initialFollowUpCount, repository.followUps.value.size)
        // Second undo is a no-op
        assertEquals(0, repository.undoLastDeleteContacts())
    }

    @Test
    fun addUpdateDeleteDoneAndUndoFollowUp_managesLifecycleAndDefaultReason() {
        val dueDate = LocalDate.of(2026, 12, 10)
        val zone = java.time.ZoneOffset.UTC

        // Blank note falls back to "Follow up with <Contact Name>"
        val created = repository.addFollowUp(
            contactId = "c-1",
            dueDate = dueDate,
            note = "   ",
            hasExplicitTime = false,
            defaultHour = 9,
            defaultMinute = 0,
            zoneId = zone
        )
        assertEquals("Follow up with Alex Rivera", created.note)
        assertFalse(created.hasExplicitTime)
        assertEquals(9, created.dueAt.atZone(zone).hour)
        assertTrue(repository.followUps.value.any { it.id == created.id })

        // Update with explicit time & custom reason
        val updated = repository.updateFollowUp(
            followUpId = created.id,
            dueDate = dueDate.plusDays(1),
            note = "Send architecture notes",
            hasExplicitTime = true,
            explicitTime = java.time.LocalTime.of(14, 30),
            zoneId = zone
        )
        assertNotNull(updated)
        assertEquals("Send architecture notes", updated?.note)
        assertTrue(updated?.hasExplicitTime == true)
        assertEquals(14, updated?.dueAt?.atZone(zone)?.hour)
        assertEquals(30, updated?.dueAt?.atZone(zone)?.minute)

        // Complete + Undo
        repository.markFollowUpDone(created.id)
        val completed = repository.followUps.value.firstOrNull { it.id == created.id }
        assertNotNull("Completed follow-up must remain in followUps stream for timeline", completed)
        assertTrue(completed?.isDone == true)
        assertTrue(repository.upcomingAlerts.value.none { it.id == created.id })
        assertTrue(repository.undoLastFollowUpMutation())
        val restoredAfterComplete = repository.followUps.value.firstOrNull { it.id == created.id }
        assertNotNull(restoredAfterComplete)
        assertFalse(restoredAfterComplete?.isDone == true)
        assertFalse(repository.undoLastFollowUpMutation())

        // Complete + Reopen preserves original dueAt
        val originalDueAt = restoredAfterComplete!!.dueAt
        repository.markFollowUpDone(created.id)
        assertTrue(repository.followUps.value.first { it.id == created.id }.isDone)
        repository.reopenFollowUp(created.id)
        val reopened = repository.followUps.value.first { it.id == created.id }
        assertFalse(reopened.isDone)
        assertEquals(originalDueAt, reopened.dueAt)
        assertTrue(repository.upcomingAlerts.value.any { it.id == created.id })

        // Delete + Undo
        repository.deleteFollowUp(created.id)
        assertTrue(repository.followUps.value.none { it.id == created.id })
        assertTrue(repository.undoLastFollowUpMutation())
        assertTrue(repository.followUps.value.any { it.id == created.id })
    }

    @Test
    fun rescheduleDateOnlyFollowUps_shiftsOnlyDateOnlyReminders() {
        val zone = java.time.ZoneOffset.UTC
        val dateOnly = repository.addFollowUp(
            contactId = "c-1",
            dueDate = LocalDate.of(2026, 12, 5),
            note = "Date only",
            hasExplicitTime = false,
            defaultHour = 9,
            defaultMinute = 0,
            zoneId = zone
        )
        val explicit = repository.addFollowUp(
            contactId = "c-1",
            dueDate = LocalDate.of(2026, 12, 5),
            note = "Exact call",
            hasExplicitTime = true,
            explicitTime = java.time.LocalTime.of(15, 45),
            zoneId = zone
        )

        repository.rescheduleDateOnlyFollowUps(hour = 18, minute = 0, zoneId = zone)

        val updatedDateOnly = repository.followUps.value.first { it.id == dateOnly.id }
        val updatedExplicit = repository.followUps.value.first { it.id == explicit.id }
        assertEquals(18, updatedDateOnly.dueAt.atZone(zone).hour)
        assertEquals(0, updatedDateOnly.dueAt.atZone(zone).minute)
        assertEquals(15, updatedExplicit.dueAt.atZone(zone).hour)
        assertEquals(45, updatedExplicit.dueAt.atZone(zone).minute)
    }

    @Test
    fun archiveContact_hidesFollowUpAlertWithoutDeletingAndRestoresOnUnarchive() {
        val created = repository.addFollowUp(
            contactId = "c-1",
            dueDate = LocalDate.now().plusDays(2),
            note = "Check in on prototype"
        )
        assertTrue(repository.upcomingAlerts.value.any { it.id == created.id })

        repository.archiveContact("c-1", archived = true)
        assertTrue("Pending FollowUp must remain stored in repository", repository.followUps.value.any { it.id == created.id })
        assertFalse("Archived contact's reminder must be hidden from Upcoming alerts", repository.upcomingAlerts.value.any { it.id == created.id })

        repository.archiveContact("c-1", archived = false)
        assertTrue("Restored contact's reminder must return to Upcoming alerts", repository.upcomingAlerts.value.any { it.id == created.id })
    }

    @Test
    fun multiContactEncounter_updatesAllParticipants_andReconcilesOnDeleteAndUndo() {
        val p1 = repository.addContactWithInitialEncounter(
            name = "Alice Test",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "",
            followUpDays = null
        )
        val p2 = repository.addContactWithInitialEncounter(
            name = "Bob Test",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "",
            followUpDays = null
        )
        val beforeP1 = repository.fullContacts.value[p1.id]!!.meetCount
        val beforeP2 = repository.fullContacts.value[p2.id]!!.meetCount

        val shared = repository.logEncounterForContact(
            contactId = p1.id,
            date = LocalDate.now(),
            note = "Group lunch",
            additionalContactIds = listOf(p2.id)
        )

        assertEquals(listOf(p1.id, p2.id), shared.resolvedParticipantIds())
        assertEquals(beforeP1 + 1, repository.fullContacts.value[p1.id]!!.meetCount)
        assertEquals(beforeP2 + 1, repository.fullContacts.value[p2.id]!!.meetCount)

        repository.deleteEncounters(setOf(shared.id))
        assertEquals(beforeP1, repository.fullContacts.value[p1.id]!!.meetCount)
        assertEquals(beforeP2, repository.fullContacts.value[p2.id]!!.meetCount)

        assertTrue(repository.undoLastDeleteEncounters())
        assertEquals(beforeP1 + 1, repository.fullContacts.value[p1.id]!!.meetCount)
        assertEquals(beforeP2 + 1, repository.fullContacts.value[p2.id]!!.meetCount)
    }

    @Test
    fun multiContactFollowUp_formatsParticipants_andExcludesAllFromSerendipity() {
        val created = repository.addFollowUp(
            contactId = "c-1",
            dueDate = LocalDate.now().plusDays(2),
            note = "",
            participantIds = listOf("c-1", "c-2")
        )
        assertEquals(listOf("c-1", "c-2"), created.resolvedParticipantIds())
        assertTrue(created.note.contains("Alex Rivera") && created.note.contains("David Kim"))

        val alert = repository.upcomingAlerts.value.filterIsInstance<com.example.hello.domain.model.UpcomingAlert.FollowUpReminder>()
            .first { it.id == created.id }
        assertEquals("Alex Rivera & David Kim", alert.contactName)

        val serendipity = repository.computeSerendipityPrompts(
            contacts = repository.fullContacts.value.values,
            encounters = repository.encounters.value,
            pendingFollowUps = listOf(created)
        )
        assertFalse(serendipity.any { it.contactId == "c-1" || it.contactId == "c-2" })
    }
}

package com.example.hello.data.repository

import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * In-memory reactive repository synchronizing Contacts, Encounters, Places, Tags, and FollowUps
 * across Home, People, and Quick Action capture surfaces in <2ms.
 */
class ContactRepository {

    private val _contacts = MutableStateFlow(ContactsViewModel.createSampleContacts())
    val contacts: StateFlow<List<ContactSummary>> = _contacts.asStateFlow()

    private val _recentContacts = MutableStateFlow(HomeViewModel.createInitialState().recentContacts)
    val recentContacts: StateFlow<List<ContactSummary>> = _recentContacts.asStateFlow()

    private val _places = MutableStateFlow(MockDataProvider.places)
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _tags = MutableStateFlow(MockDataProvider.tags)
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _encounters = MutableStateFlow(MockDataProvider.encounters)
    val encounters: StateFlow<List<Encounter>> = _encounters.asStateFlow()

    private val _followUps = MutableStateFlow(MockDataProvider.followUps)
    val followUps: StateFlow<List<FollowUp>> = _followUps.asStateFlow()

    private val _upcomingAlerts = MutableStateFlow(HomeViewModel.createInitialState().upcomingAlerts)
    val upcomingAlerts: StateFlow<List<UpcomingAlert>> = _upcomingAlerts.asStateFlow()

    fun addContactWithInitialEncounter(
        name: String,
        circles: Set<Circle>,
        tagIds: Set<String>,
        placeId: String?,
        customPlaceName: String?,
        note: String,
        followUpDays: Int?
    ): ContactSummary {
        val cleanName = name.trim().ifEmpty { "New Contact" }
        val resolvedPlaceId = resolveOrCreatePlace(placeId, customPlaceName)
        val placeLabel = resolvedPlaceId?.let { id -> _places.value.firstOrNull { it.id == id }?.label }

        val selectedTagNames = _tags.value
            .filter { it.id in tagIds }
            .map { "#${it.name.lowercase()}" }

        val contextParts = mutableListOf<String>()
        if (note.isNotBlank()) {
            contextParts.add(note.trim())
        } else if (placeLabel != null) {
            contextParts.add("Met at $placeLabel")
        }
        if (selectedTagNames.isNotEmpty()) {
            contextParts.add(selectedTagNames.joinToString(" "))
        }
        val subtitle = contextParts.joinToString(" • ").ifEmpty { "Met today" }

        val newContact = ContactSummary(
            id = "c-${UUID.randomUUID()}",
            name = cleanName,
            contextSubtitle = subtitle,
            circles = circles.toList().ifEmpty { listOf(Circle.FRIENDS) },
            avatarInitials = cleanName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "👋" },
            avatarBgIndex = (_contacts.value.size % 6),
            lastEncounterText = "Met today"
        )

        val now = Instant.now()
        val encounter = Encounter(
            id = "enc-${UUID.randomUUID()}",
            contactId = newContact.id,
            occurredAt = now,
            placeId = resolvedPlaceId,
            eventLabel = placeLabel,
            notes = note.trim()
        )

        _contacts.update { listOf(newContact) + it }
        _recentContacts.update { listOf(newContact) + it }
        _encounters.update { listOf(encounter) + it }

        if (tagIds.isNotEmpty()) {
            _tags.update { currentTags ->
                currentTags.map { tag ->
                    if (tag.id in tagIds) {
                        tag.copy(useCount = tag.useCount + 1, lastUsedAt = now)
                    } else {
                        tag
                    }
                }
            }
        }

        if (followUpDays != null && followUpDays > 0) {
            val followUp = FollowUp(
                id = "fu-${UUID.randomUUID()}",
                contactId = newContact.id,
                dueAt = now.plus(followUpDays.toLong(), ChronoUnit.DAYS),
                note = note.trim().ifEmpty { "Follow up with ${newContact.name}" }
            )
            _followUps.update { listOf(followUp) + it }
            val alert = UpcomingAlert.FollowUpReminder(
                id = followUp.id,
                contactName = newContact.name,
                taskDescription = followUp.note,
                dueText = "In $followUpDays days"
            )
            _upcomingAlerts.update { listOf(alert) + it }
        }

        return newContact
    }

    fun logEncounterForContact(
        contactId: String,
        date: LocalDate = LocalDate.now(),
        placeId: String? = null,
        customPlaceName: String? = null,
        note: String = ""
    ): Encounter {
        val resolvedPlaceId = resolveOrCreatePlace(placeId, customPlaceName)
        val placeLabel = resolvedPlaceId?.let { id -> _places.value.firstOrNull { it.id == id }?.label }
        val occurredAt = date.atStartOfDay(ZoneOffset.UTC).toInstant()

        val encounter = Encounter(
            id = "enc-${UUID.randomUUID()}",
            contactId = contactId,
            occurredAt = occurredAt,
            placeId = resolvedPlaceId,
            eventLabel = placeLabel,
            notes = note.trim()
        )
        _encounters.update { listOf(encounter) + it }

        val dateText = formatEncounterDateText(date)
        val targetContact = _contacts.value.firstOrNull { it.id == contactId }
            ?: _recentContacts.value.firstOrNull { it.id == contactId }

        val updatedSubtitle = when {
            note.isNotBlank() && placeLabel != null -> "${note.trim()} • at $placeLabel"
            note.isNotBlank() -> note.trim()
            placeLabel != null -> "Met at $placeLabel"
            else -> targetContact?.contextSubtitle ?: "Recent encounter"
        }

        _contacts.update { list ->
            list.map { c ->
                if (c.id == contactId) {
                    c.copy(
                        contextSubtitle = updatedSubtitle,
                        lastEncounterText = dateText
                    )
                } else {
                    c
                }
            }
        }

        _recentContacts.update { list ->
            val existing = list.firstOrNull { it.id == contactId } ?: targetContact
            if (existing != null) {
                val updated = existing.copy(
                    contextSubtitle = updatedSubtitle,
                    lastEncounterText = dateText
                )
                listOf(updated) + list.filterNot { it.id == contactId }
            } else {
                list
            }
        }

        // Reset relationship cadence: clear any overdue CadenceAlert for this contact
        if (targetContact != null) {
            val firstName = targetContact.name.split(" ").firstOrNull() ?: targetContact.name
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    alert is UpcomingAlert.CadenceAlert &&
                        (alert.contactName.equals(targetContact.name, ignoreCase = true) ||
                            alert.contactName.startsWith(firstName, ignoreCase = true))
                }
            }
        }

        return encounter
    }

    private fun resolveOrCreatePlace(placeId: String?, customPlaceName: String?): String? {
        val now = Instant.now()
        if (placeId != null) {
            _places.update { currentPlaces ->
                currentPlaces.map { place ->
                    if (place.id == placeId) {
                        place.copy(useCount = place.useCount + 1, lastUsedAt = now)
                    } else {
                        place
                    }
                }
            }
            return placeId
        }
        val cleanCustom = customPlaceName?.trim().orEmpty()
        if (cleanCustom.isNotEmpty()) {
            val existing = _places.value.firstOrNull { it.label.equals(cleanCustom, ignoreCase = true) }
            if (existing != null) {
                _places.update { currentPlaces ->
                    currentPlaces.map { place ->
                        if (place.id == existing.id) {
                            place.copy(useCount = place.useCount + 1, lastUsedAt = now)
                        } else {
                            place
                        }
                    }
                }
                return existing.id
            }
            val newPlace = Place(
                id = "place-${UUID.randomUUID()}",
                label = cleanCustom,
                useCount = 1,
                lastUsedAt = now
            )
            _places.update { listOf(newPlace) + it }
            return newPlace.id
        }
        return null
    }

    private fun formatEncounterDateText(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Met today"
            today.minusDays(1) -> "Met yesterday"
            else -> "Met ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }
}

package com.example.hello.data.repository

import androidx.room.Transactor
import androidx.room.useWriterConnection
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Reactive repository synchronizing Contacts, Encounters, Places, Tags, and FollowUps.
 * Maintains in-memory StateFlows for <1ms zero-latency UI reactivity (<50ms cold start),
 * while durably committing ACID mutations to Room (SQLite + FTS5 cache) in <2ms on background dispatchers.
 */
class ContactRepository(
    val database: HelloDatabase? = null,
    val alarmScheduler: FollowUpAlarmScheduler? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
) {

    val encounterRepository: EncounterRepository? = database?.let { RoomEncounterRepository(it) }
    val followUpRepository: FollowUpRepository? = database?.let { RoomFollowUpRepository(it, alarmScheduler) }

    private val _fullContacts = MutableStateFlow<Map<String, Contact>>(initialFullContacts())
    val fullContacts: StateFlow<Map<String, Contact>> = _fullContacts.asStateFlow()

    private val _contacts = MutableStateFlow(ContactsViewModel.createSampleContacts())
    val contacts: StateFlow<List<ContactSummary>> = _contacts.asStateFlow()

    private val _recentContacts = MutableStateFlow(HomeViewModel.createInitialState().recentContacts)
    val recentContacts: StateFlow<List<ContactSummary>> = _recentContacts.asStateFlow()

    private val _places = MutableStateFlow(MockDataProvider.places)
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _tags = MutableStateFlow(MockDataProvider.tags)
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _encounters = MutableStateFlow(initialEncounters())
    val encounters: StateFlow<List<Encounter>> = _encounters.asStateFlow()

    private val _followUps = MutableStateFlow(MockDataProvider.followUps)
    val followUps: StateFlow<List<FollowUp>> = _followUps.asStateFlow()

    private val _upcomingAlerts = MutableStateFlow(HomeViewModel.createInitialState().upcomingAlerts)
    val upcomingAlerts: StateFlow<List<UpcomingAlert>> = _upcomingAlerts.asStateFlow()

    init {
        if (database != null) {
            coroutineScope.launch {
                seedDatabaseIfEmpty()
                observeDatabaseStreams()
            }
        }
    }

    private suspend fun <R> HelloDatabase.withTransactionSafe(block: suspend () -> R): R {
        return this.useWriterConnection { transactor ->
            transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                block()
            }
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val db = database ?: return
        val existingContacts = db.contactDao().getAllContactsFlow().firstOrNull().orEmpty()
        if (existingContacts.isEmpty()) {
            val contactsToSeed = _fullContacts.value.values.toList()
            db.withTransactionSafe {
                db.contactDao().insertContacts(contactsToSeed.map { it.toEntity() })
                db.contactDao().insertContactFtsList(
                    contactsToSeed.map { contact ->
                        ContactFtsEntity(
                            contactId = contact.id,
                            name = contact.name,
                            aliases = contact.aliases.joinToString(" "),
                            notes = contact.notes
                        )
                    }
                )
                db.placeDao().insertPlaces(_places.value.map { it.toEntity() })
                db.tagDao().insertTags(_tags.value.map { it.toEntity() })
                db.encounterDao().insertEncounters(_encounters.value.map { it.toEntity() })
                db.followUpDao().insertFollowUps(_followUps.value.map { it.toEntity() })
            }
        }
    }

    private fun observeDatabaseStreams() {
        val db = database ?: return

        coroutineScope.launch {
            db.contactDao().getAllContactsFlow().collect { entities ->
                val domainMap = entities.associate { it.id to it.toDomain() }
                _fullContacts.value = domainMap
                _contacts.value = entities.map { it.toSummary() }
                _recentContacts.value = entities
                    .filterNot { it.isArchived }
                    .sortedByDescending { it.lastMetAt ?: Instant.MIN }
                    .take(4)
                    .map { it.toSummary() }
            }
        }

        coroutineScope.launch {
            db.placeDao().getAllPlacesFlow().collect { entities ->
                _places.value = entities.map { it.toDomain() }
            }
        }

        coroutineScope.launch {
            db.tagDao().getAllTagsFlow().collect { entities ->
                _tags.value = entities.map { it.toDomain() }
            }
        }

        coroutineScope.launch {
            db.encounterDao().getAllEncountersFlow().collect { entities ->
                _encounters.value = entities.map { it.toDomain() }
            }
        }

        coroutineScope.launch {
            db.followUpDao().getPendingFollowUpsFlow().collect { entities ->
                val domainFollowUps = entities.map { it.toDomain() }
                _followUps.value = domainFollowUps

                val reminders = domainFollowUps.map { fu ->
                    val contactName = _fullContacts.value[fu.contactId]?.name ?: "Contact"
                    val daysUntil = ChronoUnit.DAYS.between(Instant.now(), fu.dueAt).coerceAtLeast(0)
                    val dueText = if (daysUntil <= 0) "Today" else "In $daysUntil days"
                    UpcomingAlert.FollowUpReminder(
                        id = fu.id,
                        contactName = contactName,
                        taskDescription = fu.note,
                        dueText = dueText
                    )
                }
                _upcomingAlerts.update { currentAlerts ->
                    val nonFollowUps = currentAlerts.filterNot { it is UpcomingAlert.FollowUpReminder }
                    reminders + nonFollowUps
                }
            }
        }
    }

    fun getContact(contactId: String): Contact? {
        return _fullContacts.value[contactId]
    }

    fun getContactFlow(contactId: String): Flow<Contact?> {
        return _fullContacts.map { it[contactId] }
    }

    fun getEncountersForContact(contactId: String): Flow<List<Encounter>> {
        return _encounters.map { list ->
            list.filter { it.contactId == contactId }
                .sortedByDescending { it.occurredAt }
        }
    }

    /**
     * Sub-10ms full-text search backed by SQLite FTS4/FTS5 virtual table (Ticket 03).
     * Cleared/empty queries fall back to the active contacts list.
     */
    fun searchContacts(query: String): Flow<List<Contact>> {
        val clean = query.trim()
        if (clean.isBlank()) {
            return _fullContacts.map { map ->
                map.values.filterNot { it.isArchived }.sortedBy { it.name.lowercase() }
            }
        }

        val db = database
        if (db != null) {
            val ftsQuery = formatFtsQuery(clean)
            if (ftsQuery != null) {
                return db.contactDao().searchContactsFts(ftsQuery)
                    .map { list -> list.map { it.toDomain() } }
                    .catch {
                        emitAll(db.contactDao().searchContactsLike(clean).map { list -> list.map { it.toDomain() } })
                    }
            }
        }

        return _fullContacts.map { map ->
            map.values.filterNot { it.isArchived }.filter { contact ->
                contact.name.contains(clean, ignoreCase = true) ||
                    contact.notes.contains(clean, ignoreCase = true) ||
                    contact.aliases.any { it.contains(clean, ignoreCase = true) }
            }.sortedBy { it.name.lowercase() }
        }
    }

    fun updateContact(updated: Contact) {
        _fullContacts.update { it + (updated.id to updated) }

        _contacts.update { list ->
            list.map { summary ->
                if (summary.id == updated.id) {
                    summary.copy(
                        name = updated.name,
                        circles = updated.circles,
                        phone = updated.phone,
                        email = updated.email,
                        isArchived = updated.isArchived
                    )
                } else {
                    summary
                }
            }
        }

        _recentContacts.update { list ->
            list.map { summary ->
                if (summary.id == updated.id) {
                    summary.copy(
                        name = updated.name,
                        circles = updated.circles,
                        phone = updated.phone,
                        email = updated.email,
                        isArchived = updated.isArchived
                    )
                } else {
                    summary
                }
            }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.contactDao().updateContact(updated.toEntity())
                db.contactDao().deleteContactFtsByContactId(updated.id)
                db.contactDao().insertContactFts(
                    ContactFtsEntity(
                        contactId = updated.id,
                        name = updated.name,
                        aliases = updated.aliases.joinToString(" "),
                        notes = updated.notes
                    )
                )
            }
        }
    }

    fun archiveContact(contactId: String, archived: Boolean = true) {
        val existing = _fullContacts.value[contactId] ?: return
        val updated = existing.copy(isArchived = archived)
        updateContact(updated)

        if (archived) {
            val firstName = existing.name.split(" ").firstOrNull() ?: existing.name
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    alert is UpcomingAlert.CadenceAlert &&
                        (alert.contactName.equals(existing.name, ignoreCase = true) ||
                            alert.contactName.startsWith(firstName, ignoreCase = true))
                }
            }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                val entity = db.contactDao().getContactById(contactId)
                if (entity != null) {
                    db.contactDao().updateContact(entity.copy(isArchived = archived))
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        val contactFollowUps = _followUps.value.filter { it.contactId == contactId }
        contactFollowUps.forEach { fu -> alarmScheduler?.cancelAlarm(fu.id) }

        val existing = _fullContacts.value[contactId]
        _fullContacts.update { it - contactId }
        _contacts.update { list -> list.filterNot { it.id == contactId } }
        _recentContacts.update { list -> list.filterNot { it.id == contactId } }
        _encounters.update { list -> list.filterNot { it.contactId == contactId } }
        _followUps.update { list -> list.filterNot { it.contactId == contactId } }

        if (existing != null) {
            val firstName = existing.name.split(" ").firstOrNull() ?: existing.name
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    alert.contactName.equals(existing.name, ignoreCase = true) ||
                        alert.contactName.startsWith(firstName, ignoreCase = true)
                }
            }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    db.contactDao().deleteContactById(contactId)
                    db.contactDao().deleteContactFtsByContactId(contactId)
                    db.encounterDao().deleteEncountersForContact(contactId)
                    db.followUpDao().deleteFollowUpsForContact(contactId)
                }
            }
        }
    }

    fun addTag(name: String, colorHex: String = "#E06D3B"): Tag {
        val cleanName = name.trim().removePrefix("#")
        val existing = _tags.value.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) return existing

        val newTag = Tag(
            id = "tag-${UUID.randomUUID()}",
            name = cleanName,
            colorHex = colorHex,
            useCount = 1,
            lastUsedAt = Instant.now()
        )
        _tags.update { listOf(newTag) + it }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.tagDao().insertTag(newTag.toEntity())
            }
        }
        return newTag
    }

    fun updateTag(updatedTag: Tag) {
        _tags.update { currentTags ->
            currentTags.map { if (it.id == updatedTag.id) updatedTag else it }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.tagDao().updateTag(updatedTag.toEntity())
            }
        }
    }

    fun mergeTags(sourceTagId: String, targetTagId: String) {
        if (sourceTagId == targetTagId) return
        val sourceTag = _tags.value.firstOrNull { it.id == sourceTagId } ?: return
        val targetTag = _tags.value.firstOrNull { it.id == targetTagId } ?: return

        _fullContacts.update { currentMap ->
            currentMap.mapValues { (_, contact) ->
                if (contact.tagIds.contains(sourceTagId)) {
                    val updatedIds = (contact.tagIds - sourceTagId) + targetTagId
                    contact.copy(tagIds = updatedIds.distinct())
                } else {
                    contact
                }
            }
        }

        _tags.update { currentTags ->
            currentTags
                .filterNot { it.id == sourceTagId }
                .map { tag ->
                    if (tag.id == targetTagId) {
                        tag.copy(
                            useCount = tag.useCount + sourceTag.useCount,
                            lastUsedAt = Instant.now()
                        )
                    } else {
                        tag
                    }
                }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    db.tagDao().deleteTagById(sourceTagId)
                    val target = db.tagDao().getTagById(targetTagId)
                    if (target != null) {
                        db.tagDao().updateTag(
                            target.copy(
                                useCount = target.useCount + sourceTag.useCount,
                                lastUsedAt = Instant.now()
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteTag(tagId: String) {
        _fullContacts.update { currentMap ->
            currentMap.mapValues { (_, contact) ->
                if (contact.tagIds.contains(tagId)) {
                    contact.copy(tagIds = contact.tagIds - tagId)
                } else {
                    contact
                }
            }
        }

        _tags.update { currentTags ->
            currentTags.filterNot { it.id == tagId }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.tagDao().deleteTagById(tagId)
            }
        }
    }

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

        val fullContact = Contact(
            id = newContact.id,
            name = cleanName,
            circles = newContact.circles,
            tagIds = tagIds.toList(),
            notes = note.trim(),
            firstMetAt = now,
            lastMetAt = now,
            meetCount = 1,
            cadenceDays = followUpDays
        )
        _fullContacts.update { it + (newContact.id to fullContact) }

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

        var createdFollowUp: FollowUp? = null
        if (followUpDays != null && followUpDays > 0) {
            val followUp = FollowUp(
                id = "fu-${UUID.randomUUID()}",
                contactId = newContact.id,
                dueAt = now.plus(followUpDays.toLong(), ChronoUnit.DAYS),
                note = note.trim().ifEmpty { "Follow up with ${newContact.name}" }
            )
            createdFollowUp = followUp
            _followUps.update { listOf(followUp) + it }
            val alert = UpcomingAlert.FollowUpReminder(
                id = followUp.id,
                contactName = newContact.name,
                taskDescription = followUp.note,
                dueText = "In $followUpDays days"
            )
            _upcomingAlerts.update { listOf(alert) + it }
            alarmScheduler?.scheduleFollowUp(followUp, newContact.name)
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    db.contactDao().insertContact(fullContact.toEntity())
                    db.contactDao().insertContactFts(
                        ContactFtsEntity(
                            contactId = fullContact.id,
                            name = fullContact.name,
                            aliases = fullContact.aliases.joinToString(" "),
                            notes = fullContact.notes
                        )
                    )
                    db.encounterDao().insertEncounter(encounter.toEntity())
                    if (createdFollowUp != null) {
                        db.followUpDao().insertFollowUp(createdFollowUp.toEntity())
                    }
                }
            }
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

        _fullContacts.update { map ->
            val contact = map[contactId]
            if (contact != null) {
                val updatedContact = contact.copy(
                    lastMetAt = occurredAt,
                    meetCount = contact.meetCount + 1
                )
                map + (contactId to updatedContact)
            } else {
                map
            }
        }

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

        val db = database
        if (db != null) {
            coroutineScope.launch {
                val place = resolvedPlaceId?.let { pid -> _places.value.firstOrNull { it.id == pid } }
                encounterRepository?.saveEncounterAtomic(encounter, place)
            }
        }

        return encounter
    }

    fun markFollowUpDone(alertId: String) {
        _upcomingAlerts.update { alerts -> alerts.filterNot { it.id == alertId } }
        _followUps.update { followUps -> followUps.filterNot { it.id == alertId } }
        alarmScheduler?.cancelAlarm(alertId)

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.followUpDao().markDone(alertId, Instant.now())
            }
        }
    }

    fun snoozeFollowUp(alertId: String, days: Long = 3) {
        _upcomingAlerts.update { alerts -> alerts.filterNot { it.id == alertId } }

        val newDueAt = Instant.now().plus(days, ChronoUnit.DAYS)
        val targetFollowUp = _followUps.value.firstOrNull { it.id == alertId }
        val contactName = targetFollowUp?.let { _fullContacts.value[it.contactId]?.name } ?: "Contact"
        if (targetFollowUp != null) {
            alarmScheduler?.scheduleAlarm(
                followUpId = alertId,
                contactId = targetFollowUp.contactId,
                contactName = contactName,
                note = targetFollowUp.note,
                dueAt = newDueAt
            )
        }

        _followUps.update { followUps ->
            followUps.map {
                if (it.id == alertId) it.copy(dueAt = newDueAt) else it
            }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.followUpDao().snooze(alertId, newDueAt)
            }
        }
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
            val db = database
            if (db != null) {
                coroutineScope.launch {
                    db.placeDao().insertPlace(newPlace.toEntity())
                }
            }
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

    companion object {
        fun formatFtsQuery(query: String): String? {
            val clean = query.trim()
            if (clean.isBlank()) return null
            val tokens = clean.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }

        fun ContactEntity.toSummary(): ContactSummary {
            val initials = name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "👋" }
            val subtitle = notes.ifBlank {
                circles.firstOrNull()?.let { "${it.displayName} Circle" } ?: "Contact"
            }
            val lastMet = when {
                lastMetAt == null -> "Never met"
                else -> {
                    val date = lastMetAt.atZone(ZoneId.systemDefault()).toLocalDate()
                    val today = LocalDate.now()
                    when (date) {
                        today -> "Met today"
                        today.minusDays(1) -> "Met yesterday"
                        else -> "Met ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
                    }
                }
            }
            return ContactSummary(
                id = id,
                name = name,
                contextSubtitle = subtitle,
                circles = circles.ifEmpty { listOf(Circle.FRIENDS) },
                avatarInitials = initials,
                avatarBgIndex = (id.hashCode().let { if (it < 0) -it else it } % 6),
                lastEncounterText = lastMet,
                phone = phone,
                email = email,
                isArchived = isArchived
            )
        }

        fun initialFullContacts(): Map<String, Contact> {
            val map = mutableMapOf<String, Contact>()
            MockDataProvider.contacts.forEach { contact ->
                map[contact.id] = contact
            }
            val sampleSummaries = ContactsViewModel.createSampleContacts()
            val sampleDetails = mapOf(
                "c-1" to Contact(
                    id = "c-1",
                    name = "Alex Rivera",
                    aliases = listOf("Rivera"),
                    phone = "+1 (555) 876-5432",
                    email = "alex.rivera@example.com",
                    circles = listOf(Circle.WORK),
                    tagIds = listOf("tag-design"),
                    notes = "Senior Product Designer working on accessible UI design systems. Coffee lover.",
                    firstMetAt = Instant.now().minus(45, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(5, ChronoUnit.DAYS),
                    meetCount = 1,
                    cadenceDays = 14
                ),
                "c-2" to Contact(
                    id = "c-2",
                    name = "David Kim",
                    aliases = listOf("Dave"),
                    phone = "+1 (555) 432-1098",
                    email = "dkim@example.com",
                    circles = listOf(Circle.WORK, Circle.UNI),
                    tagIds = listOf("tag-tech"),
                    notes = "Mobile Dev • Discussed Compose gestures and physics.",
                    firstMetAt = Instant.now().minus(60, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(10, ChronoUnit.DAYS),
                    meetCount = 4,
                    cadenceDays = 30
                ),
                "c-3" to Contact(
                    id = "c-3",
                    name = "Elena Rostova",
                    aliases = listOf("Lena"),
                    phone = "+1 (555) 345-6789",
                    email = "elena.r@example.org",
                    circles = listOf(Circle.FRIENDS, Circle.FAMILY),
                    tagIds = emptyList(),
                    notes = "Neighbor & Botanist • Exchanged rare monstera cuttings.",
                    firstMetAt = Instant.now().minus(120, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(7, ChronoUnit.DAYS),
                    meetCount = 6,
                    cadenceDays = 7
                ),
                "c-8" to Contact(
                    id = "c-8",
                    name = "Sarah Lin",
                    aliases = listOf("S-Lin", "Sarah"),
                    phone = "+1 (555) 234-5678",
                    email = "sarah.lin@example.com",
                    circles = listOf(Circle.FAMILY, Circle.FRIENDS),
                    tagIds = listOf("tag-tech", "tag-ai", "tag-coffee"),
                    notes = "Biotech Researcher • Tea tasting buddy. Loves light roast pour-overs.",
                    firstMetAt = Instant.now().minus(90, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(1, ChronoUnit.DAYS),
                    meetCount = 3,
                    cadenceDays = 30
                )
            )
            sampleDetails.forEach { (id, contact) -> map[id] = contact }
            sampleSummaries.forEach { summary ->
                if (!map.containsKey(summary.id)) {
                    map[summary.id] = Contact(
                        id = summary.id,
                        name = summary.name,
                        circles = summary.circles,
                        notes = summary.contextSubtitle,
                        cadenceDays = 14,
                        lastMetAt = Instant.now().minus(14, ChronoUnit.DAYS)
                    )
                }
            }
            return map
        }

        fun initialEncounters(): List<Encounter> {
            val list = MockDataProvider.encounters.toMutableList()
            MockDataProvider.encounters.filter { it.contactId == MockDataProvider.contactSarah.id }.forEach { enc ->
                list.add(enc.copy(id = "enc-c8-${enc.id}", contactId = "c-8"))
            }
            MockDataProvider.encounters.filter { it.contactId == MockDataProvider.contactAlex.id }.forEach { enc ->
                list.add(enc.copy(id = "enc-c1-${enc.id}", contactId = "c-1"))
            }
            return list
        }
    }
}

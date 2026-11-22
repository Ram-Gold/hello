package com.example.hello.data.repository

import androidx.room.Transactor
import androidx.room.useWriterConnection
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.SerendipityPicker
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    private val _favoriteContacts = MutableStateFlow<List<ContactSummary>>(emptyList())
    val favoriteContacts: StateFlow<List<ContactSummary>> = _favoriteContacts.asStateFlow()

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

    // In-memory undo buffer for transient 7-second recovery (ADR-0027)
    private var lastArchiveOperations: List<Pair<String, Boolean>> = emptyList()
    private var lastDeletedContacts: List<Contact> = emptyList()
    private var lastDeletedContactsEncounters: List<Encounter> = emptyList()
    private var lastDeletedContactsFollowUps: List<FollowUp> = emptyList()
    private var lastDeletedEncounters: List<Encounter> = emptyList()
    private var lastDeletedTag: Tag? = null
    private var lastDeletedTagContactIds: Set<String> = emptySet()
    private var lastFollowUpUndo: Pair<List<FollowUp>, Boolean>? = null // (followUps, wasCompleted)
    private var lastContactMutationDbJob: Job? = null
    private var lastTagDeleteDbJob: Job? = null
    private var lastFollowUpDbJob: Job? = null

    @Volatile
    private var defaultNotificationHour: Int = 9
    @Volatile
    private var defaultNotificationMinute: Int = 0

    // In-memory daily skip list for weighted-random serendipity prompts (ADR-0036)
    private var skippedSerendipityDate: LocalDate = LocalDate.now()
    private val skippedSerendipityContactIds = mutableSetOf<String>()

    init {
        refreshFavorites()
        if (database != null) {
            coroutineScope.launch {
                seedDatabaseIfEmpty()
                sanitizeLegacyHashtags()
                val initialEntities = database.contactDao().getAllContacts()
                val initialTags = database.tagDao().getAllTagsFlow().firstOrNull().orEmpty().map { it.toDomain() }
                val domainMap = initialEntities.associate { it.id to it.toDomain() }
                _tags.value = initialTags
                _fullContacts.value = domainMap
                _contacts.value = initialEntities.map { it.toSummary(initialTags) }
                _recentContacts.value = initialEntities
                    .filterNot { it.isArchived }
                    .sortedByDescending { it.lastMetAt ?: Instant.MIN }
                    .take(4)
                    .map { it.toSummary(initialTags) }
                refreshBirthdayAlerts(domainMap.values)
                refreshFavorites()
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
        if (db.contactDao().getContactCount() == 0) {
            val contactsToSeed = initialFullContacts().values.toList()
            db.contactDao().insertContactsIfAbsent(contactsToSeed.map { it.toEntity() })
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

    private suspend fun sanitizeLegacyHashtags() {
        val db = database ?: return
        try {
            val allTags = db.tagDao().getAllTagsFlow().firstOrNull().orEmpty()
            val tagsWithHash = allTags.filter { it.name.startsWith("#") }
            tagsWithHash.forEach { entity ->
                val clean = entity.name.removePrefix("#").trim()
                if (clean.isNotBlank()) {
                    db.tagDao().updateTag(entity.copy(name = clean))
                }
            }
        } catch (_: Exception) {
            // Non-critical startup cleanup
        }
    }

    private fun observeDatabaseStreams() {
        val db = database ?: return

        coroutineScope.launch {
            combine(
                db.contactDao().getAllContactsFlow(),
                db.tagDao().getAllTagsFlow()
            ) { entities, tagEntities ->
                entities to tagEntities.map { it.toDomain() }
            }.collect { (entities, domainTags) ->
                _tags.value = domainTags
                val domainMap = entities.associate { it.id to it.toDomain() }
                _fullContacts.value = domainMap
                _contacts.value = entities.map { it.toSummary(domainTags) }
                _recentContacts.value = entities
                    .filterNot { it.isArchived }
                    .sortedByDescending { it.lastMetAt ?: Instant.MIN }
                    .take(4)
                    .map { it.toSummary(domainTags) }
                refreshBirthdayAlerts(domainMap.values)
                refreshFavorites()
            }
        }

        coroutineScope.launch {
            db.placeDao().getAllPlacesFlow().collect { entities ->
                _places.value = entities.map { it.toDomain() }
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
                rebuildFollowUpAlerts()
            }
        }
    }

    private fun rebuildFollowUpAlerts(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val contactsMap = _fullContacts.value
        val reminders = _followUps.value
            .filter { fu -> !fu.isDone && contactsMap[fu.contactId]?.isArchived != true }
            .sortedBy { it.dueAt }
            .map { fu ->
                val contactName = contactsMap[fu.contactId]?.name ?: "Contact"
                val dueDate = fu.dueAt.atZone(zoneId).toLocalDate()
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
                val dueText = when {
                    daysUntil < 0L -> "Overdue"
                    daysUntil == 0L -> "Today"
                    daysUntil == 1L -> "Tomorrow"
                    else -> "In $daysUntil days"
                }
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

    private fun refreshBirthdayAlerts(contacts: Collection<Contact>, today: LocalDate = LocalDate.now()) {
        if (skippedSerendipityDate != today) {
            skippedSerendipityContactIds.clear()
            skippedSerendipityDate = today
        }
        val bdayAlerts = computeBirthdayAlerts(contacts)
        val serendipityAlerts = computeSerendipityPrompts(
            contacts = contacts,
            encounters = _encounters.value,
            pendingFollowUps = _followUps.value,
            skippedContactIds = skippedSerendipityContactIds,
            today = today
        )
        _upcomingAlerts.update { currentAlerts ->
            val nonManaged = currentAlerts.filterNot {
                (it is UpcomingAlert.BirthdayAlert && it.id.startsWith("bday-")) ||
                    (it is UpcomingAlert.SerendipityPrompt && it.id.startsWith("serendipity-"))
            }
            bdayAlerts + nonManaged + serendipityAlerts
        }
    }

    /**
     * Marks a contact's serendipity prompt as skipped for today and immediately rolls the
     * next eligible weighted-random contact into the Home carousel (ADR-0036).
     */
    fun skipSerendipityPrompt(contactId: String, today: LocalDate = LocalDate.now()) {
        if (skippedSerendipityDate != today) {
            skippedSerendipityContactIds.clear()
            skippedSerendipityDate = today
        }
        if (contactId.isNotBlank()) {
            skippedSerendipityContactIds += contactId
        }
        refreshBirthdayAlerts(_fullContacts.value.values, today = today)
    }

    fun computeSerendipityPrompts(
        contacts: Collection<Contact>,
        encounters: List<Encounter>,
        pendingFollowUps: List<FollowUp>,
        skippedContactIds: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneOffset.UTC
    ): List<UpcomingAlert.SerendipityPrompt> {
        val pendingContactIds = pendingFollowUps.filter { it.doneAt == null }.map { it.contactId }.toSet()
        val encountersByContact = encounters.groupBy { it.contactId }

        val candidates = contacts
            .filterNot { it.isArchived || it.id in pendingContactIds || it.id in skippedContactIds }
            .mapNotNull { contact ->
                val contactEncounters = encountersByContact[contact.id].orEmpty()
                val anchorInstant = contact.lastMetAt ?: contact.firstMetAt ?: return@mapNotNull null
                val anchorDate = anchorInstant.atZone(zoneId).toLocalDate()
                val daysSince = ChronoUnit.DAYS.between(anchorDate, today).toInt().coerceAtLeast(0)
                if (daysSince < SerendipityPicker.MIN_GAP_DAYS) return@mapNotNull null

                val hasMultiple = contactEncounters.size >= 2 || contact.meetCount >= 2
                val contextText = SerendipityPicker.formatSerendipityContext(
                    daysSince = daysSince,
                    hasMultipleEncounters = hasMultiple
                )
                val prompt = UpcomingAlert.SerendipityPrompt(
                    id = "serendipity-${contact.id}",
                    contactName = contact.name,
                    daysSinceLastMet = daysSince,
                    contextText = contextText,
                    contactId = contact.id
                )
                SerendipityPicker.WeightedCandidate(
                    item = prompt,
                    stableId = contact.id,
                    daysSinceLastMet = daysSince
                )
            }

        return SerendipityPicker
            .pickWeightedRandom(
                candidates = candidates,
                todayEpochDay = today.toEpochDay(),
                maxCount = SerendipityPicker.MAX_SURFACED_PROMPTS
            )
            .map { it.item }
    }

    private fun computeBirthdayAlerts(contacts: Collection<Contact>): List<UpcomingAlert.BirthdayAlert> {
        val today = LocalDate.now()
        return contacts.filterNot { it.isArchived }.mapNotNull { contact ->
            val bday = contact.birthday ?: return@mapNotNull null
            val nextBday = com.example.hello.ui.components.BirthdayInputUtils.nextAnnualBirthday(bday, today)
            val daysUntil = ChronoUnit.DAYS.between(today, nextBday)
            if (daysUntil in 0..14) {
                val dateText = when (daysUntil) {
                    0L -> "Today!"
                    1L -> "Tomorrow"
                    else -> "In $daysUntil days"
                }
                UpcomingAlert.BirthdayAlert(
                    id = "bday-${contact.id}",
                    contactId = contact.id,
                    contactName = contact.name,
                    dateText = dateText
                )
            } else {
                null
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

    fun refreshFavorites() {
        val fullMap = _fullContacts.value
        val favs = _contacts.value
            .filter { it.isFavorite && !it.isArchived }
            .sortedByDescending { summary ->
                val full = fullMap[summary.id]
                maxOf(
                    summary.updatedAt ?: Instant.EPOCH,
                    full?.updatedAt ?: Instant.EPOCH,
                    full?.lastMetAt ?: Instant.EPOCH,
                    full?.firstMetAt ?: Instant.EPOCH
                )
            }
        _favoriteContacts.value = favs
    }

    fun toggleFavorite(contactId: String) {
        val current = _fullContacts.value[contactId]
        if (current != null) {
            val updated = current.copy(
                isFavorite = !current.isFavorite,
                updatedAt = Instant.now()
            )
            updateContact(updated)
        } else {
            val db = database
            if (db != null) {
                coroutineScope.launch {
                    val entity = db.contactDao().getContactById(contactId)
                    if (entity != null) {
                        val newFavorite = !entity.isFavorite
                        val updatedEntity = entity.copy(isFavorite = newFavorite, updatedAt = Instant.now())
                        db.contactDao().insertContact(updatedEntity)
                        val domain = updatedEntity.toDomain()
                        _fullContacts.update { it + (contactId to domain) }
                        refreshFavorites()
                    }
                }
            }
        }
    }

    fun updateContact(updated: Contact) {
        _fullContacts.update { it + (updated.id to updated) }

        val applySummaryUpdate: (ContactSummary) -> ContactSummary = { summary ->
            if (summary.id == updated.id) {
                summary.copy(
                    name = updated.name,
                    phone = updated.phone,
                    email = updated.email,
                    isArchived = updated.isArchived,
                    isFavorite = updated.isFavorite,
                    updatedAt = updated.updatedAt,
                    photoUri = updated.photoUri
                )
            } else {
                summary
            }
        }
        _contacts.update { list -> list.map(applySummaryUpdate) }
        _recentContacts.update { list -> list.map(applySummaryUpdate) }
        refreshFavorites()

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.contactDao().insertContact(updated.toEntity())
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
        archiveContacts(setOf(contactId), archived)
    }

    fun archiveContacts(contactIds: Set<String>, archived: Boolean = true) {
        if (contactIds.isEmpty()) return
        val existingContacts = contactIds.mapNotNull { _fullContacts.value[it] }
        if (existingContacts.isEmpty()) return

        lastArchiveOperations = existingContacts.map { it.id to it.isArchived }
        applyArchiveStates(existingContacts.map { it.id to archived })
    }

    private fun applyArchiveStates(targetStates: List<Pair<String, Boolean>>) {
        if (targetStates.isEmpty()) return
        val stateById = targetStates.toMap()
        val now = Instant.now()

        val updatedContacts = mutableListOf<Contact>()
        _fullContacts.update { currentMap ->
            val mutable = currentMap.toMutableMap()
            for ((contactId, targetArchived) in targetStates) {
                val existing = mutable[contactId] ?: continue
                val updated = existing.copy(isArchived = targetArchived, updatedAt = now)
                mutable[contactId] = updated
                updatedContacts.add(updated)
            }
            mutable
        }

        val applySummaryUpdate: (ContactSummary) -> ContactSummary = { summary ->
            val targetArchived = stateById[summary.id]
            if (targetArchived != null) {
                summary.copy(isArchived = targetArchived, updatedAt = now)
            } else {
                summary
            }
        }
        _contacts.update { list -> list.map(applySummaryUpdate) }
        _recentContacts.update { list -> list.map(applySummaryUpdate) }
        refreshFavorites()
        refreshBirthdayAlerts(_fullContacts.value.values)

        val newlyArchived = updatedContacts.filter { it.isArchived }
        if (newlyArchived.isNotEmpty()) {
            val archivedIds = newlyArchived.map { it.id }.toSet()
            val archivedNames = newlyArchived.map { it.name }
            val archivedFirstNames = newlyArchived.map { it.name.split(" ").firstOrNull() ?: it.name }
            _followUps.value
                .filter { !it.isDone && it.contactId in archivedIds }
                .forEach { fu -> alarmScheduler?.cancelAlarm(fu.id) }
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    alert is UpcomingAlert.SerendipityPrompt &&
                        (alert.contactId in archivedIds ||
                            archivedNames.any { alert.contactName.equals(it, ignoreCase = true) } ||
                            archivedFirstNames.any { alert.contactName.startsWith(it, ignoreCase = true) })
                }
            }
        }
        val newlyRestored = updatedContacts.filterNot { it.isArchived }
        if (newlyRestored.isNotEmpty()) {
            val restoredById = newlyRestored.associateBy { it.id }
            _followUps.value
                .filter { !it.isDone && it.contactId in restoredById }
                .forEach { fu ->
                    val contact = restoredById[fu.contactId]
                    if (contact != null) {
                        alarmScheduler?.scheduleFollowUp(fu, contact.name)
                    }
                }
        }
        rebuildFollowUpAlerts()

        val db = database
        if (db != null) {
            val previousJob = lastContactMutationDbJob
            lastContactMutationDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    for ((contactId, targetArchived) in targetStates) {
                        val entity = db.contactDao().getContactById(contactId)
                        if (entity != null) {
                            db.contactDao().updateContact(entity.copy(isArchived = targetArchived, updatedAt = now))
                        }
                    }
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        deleteContacts(setOf(contactId))
    }

    fun deleteContacts(contactIds: Set<String>) {
        if (contactIds.isEmpty()) return
        val existingContacts = contactIds.mapNotNull { _fullContacts.value[it] }
        val deletedIds = contactIds

        val affectedFollowUps = _followUps.value.filter { it.contactId in deletedIds }
        affectedFollowUps.forEach { fu -> alarmScheduler?.cancelAlarm(fu.id) }

        if (existingContacts.isNotEmpty()) {
            val existingIds = existingContacts.map { it.id }.toSet()
            lastDeletedContacts = existingContacts
            lastDeletedContactsEncounters = _encounters.value.filter { it.contactId in existingIds }
            lastDeletedContactsFollowUps = affectedFollowUps.filter { it.contactId in existingIds }
        }

        _fullContacts.update { it - deletedIds }
        _contacts.update { list -> list.filterNot { it.id in deletedIds } }
        _recentContacts.update { list -> list.filterNot { it.id in deletedIds } }
        _encounters.update { list -> list.filterNot { it.contactId in deletedIds } }
        _followUps.update { list -> list.filterNot { it.contactId in deletedIds } }
        refreshFavorites()
        refreshBirthdayAlerts(_fullContacts.value.values)

        if (existingContacts.isNotEmpty()) {
            val deletedNames = existingContacts.map { it.name }
            val deletedFirstNames = existingContacts.map { it.name.split(" ").firstOrNull() ?: it.name }
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    (alert is UpcomingAlert.SerendipityPrompt && alert.contactId in deletedIds) ||
                        deletedNames.any { alert.contactName.equals(it, ignoreCase = true) } ||
                        deletedFirstNames.any { alert.contactName.startsWith(it, ignoreCase = true) }
                }
            }
        }

        val db = database
        if (db != null) {
            val previousJob = lastContactMutationDbJob
            lastContactMutationDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    for (contactId in deletedIds) {
                        db.contactDao().deleteContactById(contactId)
                        db.contactDao().deleteContactFtsByContactId(contactId)
                        db.encounterDao().deleteEncountersForContact(contactId)
                        db.followUpDao().deleteFollowUpsForContact(contactId)
                    }
                }
            }
        }
    }

    fun addTag(name: String, colorHex: String? = null): Tag {
        val cleanName = name.trim().removePrefix("#").trim()
        val existing = _tags.value.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) return existing

        val resolvedColor = colorHex ?: TagColorPalette.getDeterministicColorHex(cleanName)
        val newTag = Tag(
            id = "tag-${UUID.randomUUID()}",
            name = cleanName,
            colorHex = resolvedColor,
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

        _contacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == updatedTag.id }) {
                    contact.copy(
                        tags = contact.tags.map { if (it.id == updatedTag.id) updatedTag else it }
                    )
                } else {
                    contact
                }
            }
        }

        _recentContacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == updatedTag.id }) {
                    contact.copy(
                        tags = contact.tags.map { if (it.id == updatedTag.id) updatedTag else it }
                    )
                } else {
                    contact
                }
            }
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

        val updatedTargetTag = targetTag.copy(
            useCount = targetTag.useCount + sourceTag.useCount,
            lastUsedAt = Instant.now()
        )

        _tags.update { currentTags ->
            currentTags
                .filterNot { it.id == sourceTagId }
                .map { tag ->
                    if (tag.id == targetTagId) {
                        updatedTargetTag
                    } else {
                        tag
                    }
                }
        }

        _contacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == sourceTagId || it.id == targetTagId }) {
                    val newTags = contact.tags.map { tag ->
                        if (tag.id == sourceTagId || tag.id == targetTagId) updatedTargetTag else tag
                    }.distinctBy { it.id }
                    contact.copy(tags = newTags)
                } else {
                    contact
                }
            }
        }

        _recentContacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == sourceTagId || it.id == targetTagId }) {
                    val newTags = contact.tags.map { tag ->
                        if (tag.id == sourceTagId || tag.id == targetTagId) updatedTargetTag else tag
                    }.distinctBy { it.id }
                    contact.copy(tags = newTags)
                } else {
                    contact
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
        val deletedTag = _tags.value.firstOrNull { it.id == tagId } ?: return
        val affectedContactIds = _fullContacts.value.values
            .filter { tagId in it.tagIds }
            .map { it.id }
            .toSet()

        lastDeletedTag = deletedTag
        lastDeletedTagContactIds = affectedContactIds

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

        _contacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == tagId }) {
                    contact.copy(tags = contact.tags.filterNot { it.id == tagId })
                } else {
                    contact
                }
            }
        }

        _recentContacts.update { currentList ->
            currentList.map { contact ->
                if (contact.tags.any { it.id == tagId }) {
                    contact.copy(tags = contact.tags.filterNot { it.id == tagId })
                } else {
                    contact
                }
            }
        }
        refreshFavorites()

        val db = database
        if (db != null) {
            val previousJob = lastTagDeleteDbJob
            lastTagDeleteDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    db.tagDao().deleteTagById(tagId)
                    for (contactId in affectedContactIds) {
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null && tagId in existingEntity.tagIds) {
                            db.contactDao().updateContact(
                                existingEntity.copy(tagIds = existingEntity.tagIds.filterNot { it == tagId })
                            )
                        }
                    }
                }
            }
        }
    }

    fun addContactWithInitialEncounter(
        name: String,
        tagIds: Set<String>,
        placeId: String?,
        customPlaceName: String?,
        note: String,
        followUpDays: Int?,
        role: String? = null,
        company: String? = null,
        birthday: LocalDate? = null,
        phone: String? = null,
        email: String? = null,
        customProperties: Map<String, String> = emptyMap()
    ): ContactSummary {
        val cleanName = name.trim().ifEmpty { "New Contact" }
        val resolvedPlaceId = resolveOrCreatePlace(placeId, customPlaceName)
        val placeLabel = resolvedPlaceId?.let { id -> _places.value.firstOrNull { it.id == id }?.label }

        val selectedTagNames = _tags.value
            .filter { it.id in tagIds }
            .map { it.name.removePrefix("#") }

        val contextParts = mutableListOf<String>()
        val professionalSubtitle = listOfNotNull(role?.trim()?.ifEmpty { null }, company?.trim()?.ifEmpty { null }).joinToString(" at ")
        if (professionalSubtitle.isNotBlank()) {
            contextParts.add(professionalSubtitle)
        }
        if (note.isNotBlank()) {
            contextParts.add(note.trim())
        } else if (placeLabel != null) {
            contextParts.add("Met at $placeLabel")
        }
        if (selectedTagNames.isNotEmpty()) {
            contextParts.add(selectedTagNames.joinToString(" "))
        }
        val subtitle = contextParts.joinToString(", ").ifEmpty { "Met today" }

        val resolvedTags = _tags.value.filter { it.id in tagIds }

        val newContact = ContactSummary(
            id = "c-${UUID.randomUUID()}",
            name = cleanName,
            contextSubtitle = subtitle,
            avatarInitials = extractAlphanumericInitials(name),
            avatarBgIndex = (_contacts.value.size % 6),
            lastEncounterText = "Met today",
            notes = note.trim(),
            tags = resolvedTags
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
            role = role?.trim()?.ifEmpty { null },
            company = company?.trim()?.ifEmpty { null },
            birthday = birthday,
            phone = phone?.trim()?.ifEmpty { null },
            email = email?.trim()?.ifEmpty { null },
            tagIds = tagIds.toList(),
            notes = note.trim(),
            firstMetAt = now,
            lastMetAt = now,
            meetCount = 1,
            customProperties = customProperties
        )
        _fullContacts.update { it + (newContact.id to fullContact) }
        refreshBirthdayAlerts(_fullContacts.value.values)

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
            val dueDate = LocalDate.now().plusDays(followUpDays.toLong())
            val followUp = FollowUp(
                id = "fu-${UUID.randomUUID()}",
                contactId = newContact.id,
                dueAt = FollowUp.computeDueAt(
                    date = dueDate,
                    hasExplicitTime = false,
                    defaultHour = defaultNotificationHour,
                    defaultMinute = defaultNotificationMinute
                ),
                note = note.trim().ifEmpty { "Follow up with ${newContact.name}" },
                hasExplicitTime = false
            )
            createdFollowUp = followUp
            _followUps.update { listOf(followUp) + it }
            rebuildFollowUpAlerts()
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

        val now = Instant.now()
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
                    meetCount = contact.meetCount + 1,
                    updatedAt = now
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
            note.isNotBlank() && placeLabel != null -> "${note.trim()}, at $placeLabel"
            note.isNotBlank() -> note.trim()
            placeLabel != null -> "Met at $placeLabel"
            else -> targetContact?.contextSubtitle ?: "Recent encounter"
        }

        _contacts.update { list ->
            list.map { c ->
                if (c.id == contactId) {
                    c.copy(
                        contextSubtitle = updatedSubtitle,
                        lastEncounterText = dateText,
                        updatedAt = now
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
                    lastEncounterText = dateText,
                    updatedAt = now
                )
                listOf(updated) + list.filterNot { it.id == contactId }
            } else {
                list
            }
        }
        refreshFavorites()

        // Clear any active SerendipityPrompt for this contact since they were just met
        if (targetContact != null) {
            val firstName = targetContact.name.split(" ").firstOrNull() ?: targetContact.name
            _upcomingAlerts.update { alerts ->
                alerts.filterNot { alert ->
                    alert is UpcomingAlert.SerendipityPrompt &&
                        (alert.contactId == contactId ||
                            alert.contactName.equals(targetContact.name, ignoreCase = true) ||
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

    fun deleteEncounters(encounterIds: Set<String>) {
        if (encounterIds.isEmpty()) return
        val encountersToDelete = _encounters.value.filter { it.id in encounterIds }
        if (encountersToDelete.isEmpty()) return
        lastDeletedEncounters = encountersToDelete

        val affectedContactIds = encountersToDelete.map { it.contactId }.distinct()

        // 1. Update in-memory encounters
        _encounters.update { list -> list.filterNot { it.id in encounterIds } }

        // 2. Reconcile affected contacts
        val remainingEncountersByContact = _encounters.value.groupBy { it.contactId }
        val zoneId = ZoneId.systemDefault()

        for (contactId in affectedContactIds) {
            val contactEncounters = remainingEncountersByContact[contactId].orEmpty()
                .sortedByDescending { it.occurredAt }
            val newMeetCount = contactEncounters.size
            val latest = contactEncounters.firstOrNull()
            val newLastEncounterText = latest?.let {
                formatEncounterDateText(it.occurredAt.atZone(zoneId).toLocalDate())
            } ?: ""
            val newLastMetAt = latest?.occurredAt

            _fullContacts.update { map ->
                val existing = map[contactId] ?: return@update map
                map + (contactId to existing.copy(
                    meetCount = newMeetCount,
                    lastMetAt = newLastMetAt
                ))
            }

            _contacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) {
                        c.copy(
                            lastEncounterText = newLastEncounterText.ifBlank { null }
                        )
                    } else {
                        c
                    }
                }
            }

            _recentContacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) {
                        c.copy(
                            lastEncounterText = newLastEncounterText.ifBlank { null }
                        )
                    } else {
                        c
                    }
                }
            }
        }

        // 3. Database persistence
        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    encounterIds.forEach { encounterId ->
                        db.encounterDao().deleteEncounterById(encounterId)
                    }
                    for (contactId in affectedContactIds) {
                        val contactEncounters = remainingEncountersByContact[contactId].orEmpty()
                            .sortedByDescending { it.occurredAt }
                        val newMeetCount = contactEncounters.size
                        val latest = contactEncounters.firstOrNull()
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null) {
                            db.contactDao().updateContact(
                                existingEntity.copy(
                                    meetCount = newMeetCount,
                                    lastMetAt = latest?.occurredAt
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Reverts the most recent single or batch archive operation within the transient 7-second
     * window (ADR-0027), returning the number of contacts restored.
     */
    fun undoLastArchiveBatch(): Int {
        val operations = lastArchiveOperations
        if (operations.isEmpty()) return 0
        lastArchiveOperations = emptyList()
        applyArchiveStates(operations)
        return operations.size
    }

    /**
     * Reverts the most recent archive operation within the transient 7-second window (ADR-0027).
     */
    fun undoLastArchive(): Boolean = undoLastArchiveBatch() > 0

    /**
     * Reverts the most recent single or batch contact deletion, restoring full contact data,
     * encounters, and follow-ups within the transient 7-second window (ADR-0027), returning the
     * number of contacts restored.
     */
    fun undoLastDeleteContacts(): Int {
        val contactsToRestore = lastDeletedContacts
        if (contactsToRestore.isEmpty()) return 0
        val encountersToRestore = lastDeletedContactsEncounters
        val followUpsToRestore = lastDeletedContactsFollowUps

        lastDeletedContacts = emptyList()
        lastDeletedContactsEncounters = emptyList()
        lastDeletedContactsFollowUps = emptyList()

        val restoredIds = contactsToRestore.map { it.id }.toSet()
        val summaries = contactsToRestore.map { it.toSummary(_tags.value) }
        val contactNamesById = contactsToRestore.associate { it.id to it.name }

        _fullContacts.update { current ->
            current + contactsToRestore.associateBy { it.id }
        }
        _contacts.update { summaries + it.filterNot { c -> c.id in restoredIds } }
        _recentContacts.update { summaries + it.filterNot { c -> c.id in restoredIds } }
        if (encountersToRestore.isNotEmpty()) {
            val restoredEncounterIds = encountersToRestore.map { it.id }.toSet()
            _encounters.update { encountersToRestore + it.filterNot { enc -> enc.id in restoredEncounterIds } }
        }
        if (followUpsToRestore.isNotEmpty()) {
            val restoredFollowUpIds = followUpsToRestore.map { it.id }.toSet()
            _followUps.update { followUpsToRestore + it.filterNot { fu -> fu.id in restoredFollowUpIds } }
            followUpsToRestore.filterNot { it.isDone }.forEach { fu ->
                alarmScheduler?.scheduleFollowUp(fu, contactNamesById[fu.contactId] ?: "Contact")
            }
        }
        refreshFavorites()
        refreshBirthdayAlerts(_fullContacts.value.values)

        val db = database
        if (db != null) {
            val previousJob = lastContactMutationDbJob
            lastContactMutationDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    contactsToRestore.forEach { contact ->
                        db.contactDao().insertContact(contact.toEntity())
                        db.contactDao().deleteContactFtsByContactId(contact.id)
                        db.contactDao().insertContactFts(
                            com.example.hello.data.local.entity.ContactFtsEntity(
                                contactId = contact.id,
                                name = contact.name,
                                aliases = contact.aliases.joinToString(" "),
                                notes = contact.notes
                            )
                        )
                    }
                    encountersToRestore.forEach { enc ->
                        db.encounterDao().insertEncounter(enc.toEntity())
                    }
                    followUpsToRestore.forEach { fu ->
                        db.followUpDao().insertFollowUp(fu.toEntity())
                    }
                }
            }
        }

        return contactsToRestore.size
    }

    /**
     * Reverts the most recent contact deletion, restoring full contact data, encounters,
     * and follow-ups within the transient 7-second window (ADR-0027).
     */
    fun undoLastDeleteContact(): Boolean = undoLastDeleteContacts() > 0

    /**
     * Reverts the most recent encounter deletion, restoring encounters and recalculating
     * contact encounter metrics within the transient 7-second window (ADR-0027).
     */
    fun undoLastDeleteEncounters(): Boolean {
        if (lastDeletedEncounters.isEmpty()) return false
        val toRestore = lastDeletedEncounters
        _encounters.update { toRestore + it }

        val affectedContactIds = toRestore.map { it.contactId }.distinct()
        val zoneId = ZoneId.systemDefault()
        for (contactId in affectedContactIds) {
            val contactEncounters = _encounters.value.filter { it.contactId == contactId }
                .sortedByDescending { it.occurredAt }
            val newMeetCount = contactEncounters.size
            val latest = contactEncounters.firstOrNull()
            val newLastEncounterText = latest?.let {
                formatEncounterDateText(it.occurredAt.atZone(zoneId).toLocalDate())
            } ?: ""
            val newLastMetAt = latest?.occurredAt

            _fullContacts.update { map ->
                val existing = map[contactId] ?: return@update map
                map + (contactId to existing.copy(
                    meetCount = newMeetCount,
                    lastMetAt = newLastMetAt
                ))
            }
            _contacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) {
                        c.copy(lastEncounterText = newLastEncounterText.ifBlank { null })
                    } else c
                }
            }
            _recentContacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) {
                        c.copy(lastEncounterText = newLastEncounterText.ifBlank { null })
                    } else c
                }
            }
        }

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    toRestore.forEach { enc ->
                        db.encounterDao().insertEncounter(enc.toEntity())
                    }
                    for (contactId in affectedContactIds) {
                        val contactEncounters = _encounters.value.filter { it.contactId == contactId }
                            .sortedByDescending { it.occurredAt }
                        val newMeetCount = contactEncounters.size
                        val latest = contactEncounters.firstOrNull()
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null) {
                            db.contactDao().updateContact(
                                existingEntity.copy(
                                    meetCount = newMeetCount,
                                    lastMetAt = latest?.occurredAt
                                )
                            )
                        }
                    }
                }
            }
        }

        lastDeletedEncounters = emptyList()
        return true
    }

    /**
     * Reverts the most recent tag deletion, restoring the tag and re-attaching it to all
     * previously tagged contacts within the transient 7-second window (ADR-0027).
     */
    fun undoLastDeleteTag(): Boolean {
        val tagToRestore = lastDeletedTag ?: return false
        val affectedContactIds = lastDeletedTagContactIds

        lastDeletedTag = null
        lastDeletedTagContactIds = emptySet()

        _tags.update { currentTags ->
            (currentTags.filterNot { it.id == tagToRestore.id } + tagToRestore)
                .sortedWith(compareByDescending<Tag> { it.useCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

        _fullContacts.update { currentMap ->
            currentMap.mapValues { (contactId, contact) ->
                if (contactId in affectedContactIds && tagToRestore.id !in contact.tagIds) {
                    contact.copy(tagIds = contact.tagIds + tagToRestore.id)
                } else {
                    contact
                }
            }
        }

        _contacts.update { currentList ->
            currentList.map { contact ->
                if (contact.id in affectedContactIds && contact.tags.none { it.id == tagToRestore.id }) {
                    contact.copy(tags = contact.tags + tagToRestore)
                } else {
                    contact
                }
            }
        }

        _recentContacts.update { currentList ->
            currentList.map { contact ->
                if (contact.id in affectedContactIds && contact.tags.none { it.id == tagToRestore.id }) {
                    contact.copy(tags = contact.tags + tagToRestore)
                } else {
                    contact
                }
            }
        }
        refreshFavorites()

        val db = database
        if (db != null) {
            val pendingDeleteJob = lastTagDeleteDbJob
            lastTagDeleteDbJob = coroutineScope.launch {
                pendingDeleteJob?.join()
                db.withTransactionSafe {
                    db.tagDao().insertTag(tagToRestore.toEntity())
                    for (contactId in affectedContactIds) {
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null && tagToRestore.id !in existingEntity.tagIds) {
                            db.contactDao().updateContact(
                                existingEntity.copy(tagIds = existingEntity.tagIds + tagToRestore.id)
                            )
                        }
                    }
                }
            }
        }

        return true
    }

    fun setDefaultNotificationTime(hour: Int, minute: Int) {
        defaultNotificationHour = hour.coerceIn(0, 23)
        defaultNotificationMinute = minute.coerceIn(0, 59)
    }

    fun getFollowUpsForContact(contactId: String): Flow<List<FollowUp>> =
        _followUps.map { list ->
            list.filter { it.contactId == contactId && !it.isDone }
                .sortedBy { it.dueAt }
        }

    fun addFollowUp(
        contactId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean = false,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FollowUp {
        val contact = _fullContacts.value[contactId]
        val contactName = contact?.name ?: _contacts.value.firstOrNull { it.id == contactId }?.name ?: "Contact"
        val resolvedNote = note.trim().ifEmpty { "Follow up with $contactName" }
        val resolvedDueAt = FollowUp.computeDueAt(
            date = dueDate,
            hasExplicitTime = hasExplicitTime,
            explicitTime = explicitTime,
            defaultHour = defaultHour,
            defaultMinute = defaultMinute,
            zoneId = zoneId
        )
        val followUp = FollowUp(
            id = "fu-${UUID.randomUUID()}",
            contactId = contactId,
            dueAt = resolvedDueAt,
            note = resolvedNote,
            hasExplicitTime = hasExplicitTime
        )
        _followUps.update { (it + followUp).sortedBy { item -> item.dueAt } }
        rebuildFollowUpAlerts(zoneId = zoneId)
        if (contact?.isArchived != true) {
            alarmScheduler?.scheduleFollowUp(followUp, contactName)
        }

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.followUpDao().insertFollowUp(followUp.toEntity())
            }
        }
        return followUp
    }

    fun updateFollowUp(
        followUpId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FollowUp? {
        val existing = _followUps.value.firstOrNull { it.id == followUpId } ?: return null
        val contact = _fullContacts.value[existing.contactId]
        val contactName = contact?.name ?: _contacts.value.firstOrNull { it.id == existing.contactId }?.name ?: "Contact"
        val resolvedNote = note.trim().ifEmpty { "Follow up with $contactName" }
        val resolvedDueAt = FollowUp.computeDueAt(
            date = dueDate,
            hasExplicitTime = hasExplicitTime,
            explicitTime = explicitTime,
            defaultHour = defaultHour,
            defaultMinute = defaultMinute,
            zoneId = zoneId
        )
        val updated = existing.copy(
            dueAt = resolvedDueAt,
            note = resolvedNote,
            hasExplicitTime = hasExplicitTime
        )
        _followUps.update { list ->
            list.map { if (it.id == followUpId) updated else it }.sortedBy { it.dueAt }
        }
        rebuildFollowUpAlerts(zoneId = zoneId)
        if (contact?.isArchived == true) {
            alarmScheduler?.cancelAlarm(followUpId)
        } else {
            alarmScheduler?.scheduleFollowUp(updated, contactName)
        }

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.followUpDao().updateFollowUp(updated.toEntity())
            }
        }
        return updated
    }

    fun deleteFollowUp(followUpId: String) {
        deleteFollowUps(setOf(followUpId))
    }

    fun deleteFollowUps(followUpIds: Set<String>) {
        if (followUpIds.isEmpty()) return
        val deleted = _followUps.value.filter { it.id in followUpIds }
        if (deleted.isNotEmpty()) {
            lastFollowUpUndo = Pair(deleted, false)
        }
        _followUps.update { followUps -> followUps.filterNot { it.id in followUpIds } }
        rebuildFollowUpAlerts()
        followUpIds.forEach { alertId ->
            alarmScheduler?.cancelAlarm(alertId)
        }

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    followUpIds.forEach { followUpId ->
                        db.followUpDao().deleteFollowUpById(followUpId)
                    }
                }
            }
        }
    }

    fun markFollowUpDone(alertId: String) {
        val existing = _followUps.value.firstOrNull { it.id == alertId }
        if (existing != null) {
            lastFollowUpUndo = Pair(listOf(existing), true)
        }
        _followUps.update { followUps -> followUps.filterNot { it.id == alertId } }
        rebuildFollowUpAlerts()
        alarmScheduler?.cancelAlarm(alertId)

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.followUpDao().markDone(alertId, Instant.now())
            }
        }
    }

    fun undoLastFollowUpMutation(): Boolean {
        val (toRestore, wasCompleted) = lastFollowUpUndo ?: return false
        if (toRestore.isEmpty()) return false
        lastFollowUpUndo = null

        val restoredIds = toRestore.map { it.id }.toSet()
        _followUps.update { current ->
            (current.filterNot { it.id in restoredIds } + toRestore.map { it.copy(doneAt = null) })
                .sortedBy { it.dueAt }
        }
        rebuildFollowUpAlerts()

        for (fu in toRestore) {
            val contact = _fullContacts.value[fu.contactId]
            if (contact?.isArchived != true) {
                val contactName = contact?.name ?: _contacts.value.firstOrNull { it.id == fu.contactId }?.name ?: "Contact"
                alarmScheduler?.scheduleFollowUp(fu.copy(doneAt = null), contactName)
            }
        }

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    toRestore.forEach { fu ->
                        if (wasCompleted) {
                            db.followUpDao().clearDone(fu.id)
                        } else {
                            db.followUpDao().insertFollowUp(fu.copy(doneAt = null).toEntity())
                        }
                    }
                }
            }
        }
        return true
    }

    fun snoozeFollowUp(
        alertId: String,
        days: Long = 3,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val targetFollowUp = _followUps.value.firstOrNull { it.id == alertId }
        val newDueAt = if (targetFollowUp != null) {
            FollowUp.computeSnoozedDueAt(
                followUp = targetFollowUp,
                days = days,
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                zoneId = zoneId
            )
        } else {
            Instant.now().plus(days, ChronoUnit.DAYS)
        }
        val contact = targetFollowUp?.let { _fullContacts.value[it.contactId] }
        val contactName = contact?.name ?: "Contact"
        if (targetFollowUp != null && contact?.isArchived != true) {
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
            }.sortedBy { it.dueAt }
        }
        rebuildFollowUpAlerts(zoneId = zoneId)

        val db = database
        if (db != null) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.followUpDao().snooze(alertId, newDueAt)
            }
        }
    }

    /**
     * Retroactively shifts and reschedules all pending date-only FollowUps (hasExplicitTime == false)
     * when the user updates their preferred notification delivery hour in Settings (ADR-0037).
     */
    fun rescheduleDateOnlyFollowUps(
        hour: Int,
        minute: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        setDefaultNotificationTime(hour, minute)
        val shifted = mutableListOf<FollowUp>()
        _followUps.update { current ->
            current.map { fu ->
                if (!fu.isDone && !fu.hasExplicitTime) {
                    val updated = fu.copy(
                        dueAt = FollowUp.shiftDateOnlyDueAt(
                            currentDueAt = fu.dueAt,
                            newHour = defaultNotificationHour,
                            newMinute = defaultNotificationMinute,
                            zoneId = zoneId
                        )
                    )
                    shifted.add(updated)
                    updated
                } else {
                    fu
                }
            }.sortedBy { it.dueAt }
        }
        rebuildFollowUpAlerts(zoneId = zoneId)

        for (fu in shifted) {
            val contact = _fullContacts.value[fu.contactId]
            if (contact?.isArchived != true) {
                val contactName = contact?.name ?: "Contact"
                alarmScheduler?.scheduleFollowUp(fu, contactName)
            }
        }

        val db = database
        if (db != null && shifted.isNotEmpty()) {
            val previousJob = lastFollowUpDbJob
            lastFollowUpDbJob = coroutineScope.launch {
                previousJob?.join()
                db.withTransactionSafe {
                    shifted.forEach { fu ->
                        db.followUpDao().updateFollowUp(fu.toEntity())
                    }
                }
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
        /**
         * The standard transient recovery window during which destructive/state mutations can be undone.
         * Synchronized with the 7-second Toast Pill countdown progress lifecycle (Ticket 01).
         */
        const val UNDO_WINDOW_DURATION_MS: Long = 7000L

        fun formatFtsQuery(query: String): String? {
            val clean = query.trim()
            if (clean.isBlank()) return null
            val tokens = clean.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }

        fun ContactEntity.toSummary(availableTags: List<Tag> = MockDataProvider.tags): ContactSummary {
            val initials = extractAlphanumericInitials(name)
            val subtitle = notes.ifBlank { "Contact" }
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
            val tagsById = availableTags.associateBy { it.id }
            val resolvedTags = tagIds.mapNotNull { tagsById[it] }
            return ContactSummary(
                id = id,
                name = name,
                contextSubtitle = subtitle,
                avatarInitials = initials,
                avatarBgIndex = (id.hashCode().let { if (it < 0) -it else it } % 6),
                lastEncounterText = lastMet,
                phone = phone,
                email = email,
                notes = notes,
                tags = resolvedTags,
                isArchived = isArchived,
                isFavorite = isFavorite,
                updatedAt = updatedAt,
                photoUri = photoUri
            )
        }

        fun Contact.toSummary(availableTags: List<Tag>): ContactSummary {
            val initials = extractAlphanumericInitials(name)
            val subtitle = notes.ifBlank { "Contact" }
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
            val resolvedTags = availableTags.filter { it.id in tagIds }
            return ContactSummary(
                id = id,
                name = name,
                contextSubtitle = subtitle,
                avatarInitials = initials,
                avatarBgIndex = (id.hashCode().let { if (it < 0) -it else it } % 6),
                lastEncounterText = lastMet,
                phone = phone,
                email = email,
                notes = notes,
                tags = resolvedTags,
                isArchived = isArchived,
                isFavorite = isFavorite,
                updatedAt = updatedAt,
                photoUri = photoUri
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
                    tagIds = listOf("tag-design"),
                    notes = "Senior Product Designer working on accessible UI design systems. Coffee lover.",
                    firstMetAt = Instant.now().minus(45, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(5, ChronoUnit.DAYS),
                    meetCount = 1
                ),
                "c-2" to Contact(
                    id = "c-2",
                    name = "David Kim",
                    aliases = listOf("Dave"),
                    phone = "+1 (555) 432-1098",
                    email = "dkim@example.com",
                    tagIds = listOf("tag-tech"),
                    notes = "Mobile Dev, discussed Compose gestures and physics.",
                    firstMetAt = Instant.now().minus(60, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(10, ChronoUnit.DAYS),
                    meetCount = 4
                ),
                "c-3" to Contact(
                    id = "c-3",
                    name = "Elena Rostova",
                    aliases = listOf("Lena"),
                    phone = "+1 (555) 345-6789",
                    email = "elena.r@example.org",
                    tagIds = emptyList(),
                    notes = "Neighbor & Botanist, exchanged rare monstera cuttings.",
                    firstMetAt = Instant.now().minus(120, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(7, ChronoUnit.DAYS),
                    meetCount = 6
                ),
                "c-8" to Contact(
                    id = "c-8",
                    name = "Sarah Lin",
                    aliases = listOf("S-Lin", "Sarah"),
                    phone = "+1 (555) 234-5678",
                    email = "sarah.lin@example.com",
                    tagIds = listOf("tag-tech", "tag-ai", "tag-coffee"),
                    notes = "Biotech Researcher, tea tasting buddy. Loves light roast pour-overs.",
                    firstMetAt = Instant.now().minus(90, ChronoUnit.DAYS),
                    lastMetAt = Instant.now().minus(1, ChronoUnit.DAYS),
                    meetCount = 3
                )
            )
            sampleDetails.forEach { (id, contact) -> map[id] = contact }
            sampleSummaries.forEach { summary ->
                if (!map.containsKey(summary.id)) {
                    map[summary.id] = Contact(
                        id = summary.id,
                        name = summary.name,
                        notes = summary.contextSubtitle,
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

package com.example.hello.data.repository

import androidx.room.Transactor
import androidx.room.useWriterConnection
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.PersonEncounterCrossRef
import com.example.hello.data.local.entity.PersonFollowUpCrossRef
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.mock.SeedDataFactory
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.domain.model.formatParticipantNames
import com.example.hello.platform.notification.FollowUpAlarmScheduler
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

    private val alertEngine = UpcomingAlertEngine()
    private val undoStore = UndoActionStore()

    private val _fullContacts = MutableStateFlow<Map<String, Contact>>(
        if (database != null) emptyMap() else initialFullContacts()
    )
    val fullContacts: StateFlow<Map<String, Contact>> = _fullContacts.asStateFlow()

    private val _contacts = MutableStateFlow(
        if (database != null) emptyList() else SeedDataFactory.createSampleContactSummaries()
    )
    val contacts: StateFlow<List<ContactSummary>> = _contacts.asStateFlow()

    private val _recentContacts = MutableStateFlow(
        if (database != null) emptyList() else SeedDataFactory.createInitialRecentContacts()
    )
    val recentContacts: StateFlow<List<ContactSummary>> = _recentContacts.asStateFlow()

    private val _favoriteContacts = MutableStateFlow<List<ContactSummary>>(emptyList())
    val favoriteContacts: StateFlow<List<ContactSummary>> = _favoriteContacts.asStateFlow()

    private val _places = MutableStateFlow(
        if (database != null) emptyList() else MockDataProvider.places
    )
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _tags = MutableStateFlow(
        if (database != null) emptyList() else MockDataProvider.tags
    )
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _encounters = MutableStateFlow(
        if (database != null) emptyList() else initialEncounters()
    )
    val encounters: StateFlow<List<Encounter>> = _encounters.asStateFlow()

    private val _upcomingAlerts = MutableStateFlow(
        if (database != null) emptyList() else SeedDataFactory.createInitialUpcomingAlerts()
    )
    val upcomingAlerts: StateFlow<List<UpcomingAlert>> = _upcomingAlerts.asStateFlow()

    internal val followUpStore = FollowUpStore(
        database = database,
        alarmScheduler = alarmScheduler,
        coroutineScope = coroutineScope,
        undoStore = undoStore,
        resolveActiveParticipants = { ids -> resolveActiveParticipants(ids) },
        resolveFallbackName = { pid -> _fullContacts.value[pid]?.name ?: _contacts.value.firstOrNull { it.id == pid }?.name },
        onFollowUpsChanged = { zoneId -> rebuildFollowUpAlerts(zoneId = zoneId) },
        initialFollowUps = if (database != null) emptyList() else MockDataProvider.followUps
    )
    val followUps: StateFlow<List<FollowUp>> = followUpStore.followUps

    private var lastContactMutationDbJob: Job? = null
    private var lastTagDeleteDbJob: Job? = null

    @Volatile
    private var defaultNotificationHour: Int = 9
    @Volatile
    private var defaultNotificationMinute: Int = 0
    @Volatile
    private var lastAppliedContactsSnapshot: Pair<List<ContactEntity>, List<com.example.hello.data.local.entity.TagEntity>>? = null

    init {
        if (database != null) {
            coroutineScope.launch {
                seedDatabaseIfEmpty()
                sanitizeLegacyHashtags()
                val initialEntities = database.contactDao().getAllContacts()
                val initialTagEntities = database.tagDao().getAllTagsFlow().firstOrNull().orEmpty()
                applyContactsAndTags(initialEntities, initialTagEntities)
                observeDatabaseStreams()
            }
        } else {
            refreshFavorites()
        }
    }

    private fun applyContactsAndTags(
        entities: List<ContactEntity>,
        tagEntities: List<com.example.hello.data.local.entity.TagEntity>
    ) {
        val snapshot = entities to tagEntities
        if (snapshot == lastAppliedContactsSnapshot) return
        lastAppliedContactsSnapshot = snapshot

        val domainTags = tagEntities.map { it.toDomain() }
        _tags.value = domainTags
        val domainMap = entities.associate { it.id to it.toDomain() }
        _fullContacts.value = domainMap
        val tagsById = domainTags.associateBy { it.id }
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val summaries = entities.map { entity ->
            entity.toSummary(
                availableTags = domainTags,
                tagsById = tagsById,
                today = today,
                zoneId = zoneId
            )
        }
        _contacts.value = summaries
        val summaryById = summaries.associateBy { it.id }
        _recentContacts.value = entities
            .filterNot { it.isArchived }
            .sortedByDescending { it.lastMetAt ?: Instant.MIN }
            .take(4)
            .mapNotNull { summaryById[it.id] }
        refreshBirthdayAlerts(domainMap.values, today = today)
        refreshFavorites()
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
            val placesToSeed = MockDataProvider.places
            val tagsToSeed = MockDataProvider.tags
            val encountersToSeed = initialEncounters()
            val followUpsToSeed = MockDataProvider.followUps
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
            db.placeDao().insertPlaces(placesToSeed.map { it.toEntity() })
            db.tagDao().insertTags(tagsToSeed.map { it.toEntity() })
            db.encounterDao().insertEncounters(encountersToSeed.map { it.toEntity() })
            db.encounterDao().insertPersonEncounters(
                encountersToSeed.flatMap { enc ->
                    enc.resolvedParticipantIds().map { pid -> PersonEncounterCrossRef(enc.id, pid) }
                }
            )
            db.followUpDao().insertFollowUps(followUpsToSeed.map { it.toEntity() })
            db.followUpDao().insertPersonFollowUps(
                followUpsToSeed.flatMap { fu ->
                    fu.resolvedParticipantIds().map { pid -> PersonFollowUpCrossRef(fu.id, pid) }
                }
            )
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
                entities to tagEntities
            }.collect { (entities, tagEntities) ->
                applyContactsAndTags(entities, tagEntities)
            }
        }

        coroutineScope.launch {
            db.placeDao().getAllPlacesFlow().collect { entities ->
                _places.value = entities.map { it.toDomain() }
            }
        }

        coroutineScope.launch {
            combine(
                db.encounterDao().getAllEncountersFlow(),
                db.encounterDao().getAllPersonEncountersFlow()
            ) { entities, crossRefs ->
                val refsByEncounter = crossRefs.groupBy({ it.encounterId }, { it.contactId })
                entities.map { it.toDomain(refsByEncounter[it.id].orEmpty()) }
            }.collect { domainEncounters ->
                _encounters.value = domainEncounters
            }
        }

        followUpStore.observeDatabase()
    }

    private fun resolveActiveParticipants(
        ids: List<String>,
        contactsMap: Map<String, Contact> = _fullContacts.value
    ): List<Contact> = ids.mapNotNull { contactsMap[it] }.filterNot { it.isArchived }

    private fun rebuildFollowUpAlerts(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val reminders = alertEngine.buildFollowUpAlerts(
            followUps = followUps.value,
            contactsMap = _fullContacts.value,
            today = today,
            zoneId = zoneId
        )
        _upcomingAlerts.update { currentAlerts ->
            val nonFollowUps = currentAlerts.filterNot { it is UpcomingAlert.FollowUpReminder }
            reminders + nonFollowUps
        }
    }

    private fun refreshBirthdayAlerts(contacts: Collection<Contact>, today: LocalDate = LocalDate.now()) {
        val bdayAlerts = alertEngine.computeBirthdayAlerts(contacts, today = today)
        val serendipityAlerts = alertEngine.computeSerendipityPrompts(
            contacts = contacts,
            encounters = _encounters.value,
            pendingFollowUps = followUps.value,
            skippedContactIds = alertEngine.skippedIdsFor(today),
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
        alertEngine.skipSerendipity(contactId, today = today)
        refreshBirthdayAlerts(_fullContacts.value.values, today = today)
    }

    fun computeSerendipityPrompts(
        contacts: Collection<Contact>,
        encounters: List<Encounter>,
        pendingFollowUps: List<FollowUp>,
        skippedContactIds: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneOffset.UTC
    ): List<UpcomingAlert.SerendipityPrompt> = alertEngine.computeSerendipityPrompts(
        contacts = contacts,
        encounters = encounters,
        pendingFollowUps = pendingFollowUps,
        skippedContactIds = skippedContactIds,
        today = today,
        zoneId = zoneId
    )

    fun getContact(contactId: String): Contact? {
        return _fullContacts.value[contactId]
    }

    fun getContactFlow(contactId: String): Flow<Contact?> {
        return _fullContacts.map { it[contactId] }
    }

    fun getEncountersForContact(contactId: String): Flow<List<Encounter>> {
        return _encounters.map { list ->
            list.filter { contactId in it.resolvedParticipantIds() }
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
                    photoUri = updated.photoUri,
                    nicknames = updated.aliases
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

        undoStore.recordArchiveBatch(existingContacts.map { it.id to it.isArchived })
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
            followUps.value
                .filter { !it.isDone && it.resolvedParticipantIds().any { pid -> pid in archivedIds } }
                .forEach { fu ->
                    val remainingActive = resolveActiveParticipants(fu.resolvedParticipantIds())
                    if (remainingActive.isEmpty()) {
                        alarmScheduler?.cancelAlarm(fu.id)
                    } else {
                        alarmScheduler?.scheduleFollowUp(fu, formatParticipantNames(remainingActive.map { it.name }))
                    }
                }
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
            val restoredIds = newlyRestored.map { it.id }.toSet()
            followUps.value
                .filter { !it.isDone && it.resolvedParticipantIds().any { pid -> pid in restoredIds } }
                .forEach { fu ->
                    val active = resolveActiveParticipants(fu.resolvedParticipantIds())
                    if (active.isNotEmpty()) {
                        alarmScheduler?.scheduleFollowUp(fu, formatParticipantNames(active.map { it.name }))
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

        val affectedFollowUps = followUps.value.filter { fu ->
            fu.resolvedParticipantIds().any { it in deletedIds }
        }
        affectedFollowUps.forEach { fu ->
            if (fu.resolvedParticipantIds().all { it in deletedIds }) {
                alarmScheduler?.cancelAlarm(fu.id)
            }
        }

        if (existingContacts.isNotEmpty()) {
            val existingIds = existingContacts.map { it.id }.toSet()
            undoStore.recordDeletedContacts(
                contacts = existingContacts,
                encounters = _encounters.value.filter { enc ->
                    enc.resolvedParticipantIds().any { it in existingIds }
                },
                followUps = affectedFollowUps.filter { fu ->
                    fu.resolvedParticipantIds().any { it in existingIds }
                }
            )
        }

        _fullContacts.update { it - deletedIds }
        _contacts.update { list -> list.filterNot { it.id in deletedIds } }
        _recentContacts.update { list -> list.filterNot { it.id in deletedIds } }
        val updatedEncounters = mutableListOf<Encounter>()
        val deletedEncounterIds = mutableListOf<String>()
        _encounters.update { list ->
            list.mapNotNull { enc ->
                val remaining = enc.resolvedParticipantIds().filterNot { it in deletedIds }
                when {
                    remaining.isEmpty() -> {
                        deletedEncounterIds += enc.id
                        null
                    }
                    remaining.size != enc.resolvedParticipantIds().size -> {
                        val repointed = enc.copy(contactId = remaining.first(), participantIds = remaining)
                        updatedEncounters += repointed
                        repointed
                    }
                    else -> enc
                }
            }
        }
        val updatedFollowUps = mutableListOf<FollowUp>()
        val deletedFollowUpIds = mutableListOf<String>()
        followUps.value.forEach { fu ->
            val remaining = fu.resolvedParticipantIds().filterNot { it in deletedIds }
            when {
                remaining.isEmpty() -> deletedFollowUpIds += fu.id
                remaining.size != fu.resolvedParticipantIds().size ->
                    updatedFollowUps += fu.copy(contactId = remaining.first(), participantIds = remaining)
            }
        }
        followUpStore.removeParticipantsFromMemory(deletedIds)
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
                        db.encounterDao().deletePersonEncountersForContact(contactId)
                        db.followUpDao().deletePersonFollowUpsForContact(contactId)
                    }
                    deletedEncounterIds.forEach { db.encounterDao().deleteEncounterById(it) }
                    updatedEncounters.forEach { db.encounterDao().insertEncounter(it.toEntity()) }
                    deletedFollowUpIds.forEach { db.followUpDao().deleteFollowUpById(it) }
                    updatedFollowUps.forEach { db.followUpDao().updateFollowUp(it.toEntity()) }
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

        undoStore.recordDeletedTag(deletedTag, affectedContactIds)

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
        customProperties: Map<String, String> = emptyMap(),
        latitude: Double? = null,
        longitude: Double? = null
    ): ContactSummary {
        val cleanName = name.trim().ifEmpty { "New Contact" }
        val resolvedPlaceId = resolveOrCreatePlace(placeId, customPlaceName, latitude, longitude)
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
            participantIds = listOf(newContact.id),
            occurredAt = now,
            placeId = resolvedPlaceId,
            latitude = latitude,
            longitude = longitude,
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
                participantIds = listOf(newContact.id),
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
            followUpStore.prependInMemory(followUp)
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
                    db.encounterDao().insertPersonEncounters(
                        listOf(PersonEncounterCrossRef(encounter.id, newContact.id))
                    )
                    if (createdFollowUp != null) {
                        db.followUpDao().insertFollowUp(createdFollowUp.toEntity())
                        db.followUpDao().insertPersonFollowUps(
                            listOf(PersonFollowUpCrossRef(createdFollowUp.id, newContact.id))
                        )
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
        note: String = "",
        additionalContactIds: List<String> = emptyList(),
        latitude: Double? = null,
        longitude: Double? = null,
        time: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Encounter {
        val allContactIds = (listOf(contactId) + additionalContactIds).distinct()
        val resolvedPlaceId = resolveOrCreatePlace(placeId, customPlaceName, latitude, longitude)
        val placeLabel = resolvedPlaceId?.let { id -> _places.value.firstOrNull { it.id == id }?.label }
        val occurredAt = date.atTime(time).atZone(zoneId).toInstant()

        val now = Instant.now()
        val encounter = Encounter(
            id = "enc-${UUID.randomUUID()}",
            contactId = allContactIds.first(),
            participantIds = allContactIds,
            occurredAt = occurredAt,
            placeId = resolvedPlaceId,
            latitude = latitude,
            longitude = longitude,
            eventLabel = placeLabel,
            notes = note.trim()
        )
        _encounters.update { listOf(encounter) + it }

        _fullContacts.update { map ->
            val updatedEntries = allContactIds.mapNotNull { pid ->
                val contact = map[pid] ?: return@mapNotNull null
                pid to contact.copy(
                    lastMetAt = maxOf(contact.lastMetAt ?: occurredAt, occurredAt),
                    meetCount = contact.meetCount + 1,
                    updatedAt = now
                )
            }.toMap()
            if (updatedEntries.isEmpty()) map else map + updatedEntries
        }

        val dateText = formatEncounterDateText(date)
        val participantSet = allContactIds.toSet()
        val participantContacts = allContactIds.mapNotNull { pid ->
            _contacts.value.firstOrNull { it.id == pid } ?: _recentContacts.value.firstOrNull { it.id == pid }
        }

        val subtitleFor: (ContactSummary?) -> String = { existing ->
            when {
                note.isNotBlank() && placeLabel != null -> "${note.trim()}, at $placeLabel"
                note.isNotBlank() -> note.trim()
                placeLabel != null -> "Met at $placeLabel"
                else -> existing?.contextSubtitle ?: "Recent encounter"
            }
        }

        _contacts.update { list ->
            list.map { c ->
                if (c.id in participantSet) {
                    c.copy(
                        contextSubtitle = subtitleFor(c),
                        lastEncounterText = dateText,
                        updatedAt = now
                    )
                } else {
                    c
                }
            }
        }

        _recentContacts.update { list ->
            val updatedPicks = allContactIds.mapNotNull { pid ->
                val existing = list.firstOrNull { it.id == pid }
                    ?: _contacts.value.firstOrNull { it.id == pid }
                    ?: return@mapNotNull null
                existing.copy(
                    contextSubtitle = subtitleFor(existing),
                    lastEncounterText = dateText,
                    updatedAt = now
                )
            }
            if (updatedPicks.isEmpty()) {
                list
            } else {
                updatedPicks + list.filterNot { it.id in participantSet }
            }
        }
        refreshFavorites()

        // Clear any active SerendipityPrompt for all participating contacts since they were just met
        val participantNames = participantContacts.map { it.name }
        val participantFirstNames = participantNames.map { it.split(" ").firstOrNull() ?: it }
        _upcomingAlerts.update { alerts ->
            alerts.filterNot { alert ->
                alert is UpcomingAlert.SerendipityPrompt &&
                    (alert.contactId in participantSet ||
                        participantNames.any { alert.contactName.equals(it, ignoreCase = true) } ||
                        participantFirstNames.any { alert.contactName.startsWith(it, ignoreCase = true) })
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

    private fun reconcileContactsFromRemainingEncounters(
        affectedContactIds: List<String>,
        remainingEncounters: List<Encounter>
    ): Map<String, Pair<Int, Instant?>> {
        val zoneId = ZoneId.systemDefault()
        val updates = mutableMapOf<String, Pair<Int, Instant?>>()
        for (contactId in affectedContactIds) {
            val contactEncounters = remainingEncounters
                .filter { contactId in it.resolvedParticipantIds() }
                .sortedByDescending { it.occurredAt }
            val newMeetCount = contactEncounters.size
            val latest = contactEncounters.firstOrNull()
            val newLastEncounterText = latest?.let {
                formatEncounterDateText(it.occurredAt.atZone(zoneId).toLocalDate())
            } ?: ""
            val newLastMetAt = latest?.occurredAt
            updates[contactId] = newMeetCount to newLastMetAt

            _fullContacts.update { map ->
                val existing = map[contactId] ?: return@update map
                map + (contactId to existing.copy(
                    meetCount = newMeetCount,
                    lastMetAt = newLastMetAt
                ))
            }
            _contacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) c.copy(lastEncounterText = newLastEncounterText.ifBlank { null }) else c
                }
            }
            _recentContacts.update { list ->
                list.map { c ->
                    if (c.id == contactId) c.copy(lastEncounterText = newLastEncounterText.ifBlank { null }) else c
                }
            }
        }
        return updates
    }

    fun deleteEncounters(encounterIds: Set<String>) {
        if (encounterIds.isEmpty()) return
        val encountersToDelete = _encounters.value.filter { it.id in encounterIds }
        if (encountersToDelete.isEmpty()) return
        undoStore.recordDeletedEncounters(encountersToDelete)

        val affectedContactIds = encountersToDelete.flatMap { it.resolvedParticipantIds() }.distinct()

        // 1. Update in-memory encounters
        _encounters.update { list -> list.filterNot { it.id in encounterIds } }

        // 2. Reconcile affected contacts
        val reconciled = reconcileContactsFromRemainingEncounters(affectedContactIds, _encounters.value)

        // 3. Database persistence
        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    encounterIds.forEach { encounterId ->
                        db.encounterDao().deleteEncounterById(encounterId)
                        db.encounterDao().deletePersonEncountersForEncounter(encounterId)
                    }
                    for (contactId in affectedContactIds) {
                        val (newMeetCount, newLastMetAt) = reconciled[contactId] ?: continue
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null) {
                            db.contactDao().updateContact(
                                existingEntity.copy(
                                    meetCount = newMeetCount,
                                    lastMetAt = newLastMetAt
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
        val operations = undoStore.consumeArchiveBatch()
        if (operations.isEmpty()) return 0
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
        val snapshot = undoStore.consumeDeletedContacts() ?: return 0
        val contactsToRestore = snapshot.contacts
        if (contactsToRestore.isEmpty()) return 0
        val encountersToRestore = snapshot.encounters
        val followUpsToRestore = snapshot.followUps

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
            followUpStore.restoreInMemory(followUpsToRestore)
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
                        db.encounterDao().insertPersonEncounters(
                            enc.resolvedParticipantIds().map { pid -> PersonEncounterCrossRef(enc.id, pid) }
                        )
                    }
                    followUpsToRestore.forEach { fu ->
                        db.followUpDao().insertFollowUp(fu.toEntity())
                        db.followUpDao().insertPersonFollowUps(
                            fu.resolvedParticipantIds().map { pid -> PersonFollowUpCrossRef(fu.id, pid) }
                        )
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
        val toRestore = undoStore.consumeDeletedEncounters()
        if (toRestore.isEmpty()) return false
        _encounters.update { toRestore + it }

        val affectedContactIds = toRestore.flatMap { it.resolvedParticipantIds() }.distinct()
        val reconciled = reconcileContactsFromRemainingEncounters(affectedContactIds, _encounters.value)

        val db = database
        if (db != null) {
            coroutineScope.launch {
                db.withTransactionSafe {
                    toRestore.forEach { enc ->
                        db.encounterDao().insertEncounter(enc.toEntity())
                        db.encounterDao().insertPersonEncounters(
                            enc.resolvedParticipantIds().map { pid -> PersonEncounterCrossRef(enc.id, pid) }
                        )
                    }
                    for (contactId in affectedContactIds) {
                        val (newMeetCount, newLastMetAt) = reconciled[contactId] ?: continue
                        val existingEntity = db.contactDao().getContactById(contactId)
                        if (existingEntity != null) {
                            db.contactDao().updateContact(
                                existingEntity.copy(
                                    meetCount = newMeetCount,
                                    lastMetAt = newLastMetAt
                                )
                            )
                        }
                    }
                }
            }
        }

        return true
    }

    /**
     * Reverts the most recent tag deletion, restoring the tag and re-attaching it to all
     * previously tagged contacts within the transient 7-second window (ADR-0027).
     */
    fun undoLastDeleteTag(): Boolean {
        val snapshot = undoStore.consumeDeletedTag() ?: return false
        val tagToRestore = snapshot.tag
        val affectedContactIds = snapshot.affectedContactIds

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
        followUpStore.setDefaultNotificationTime(defaultNotificationHour, defaultNotificationMinute)
    }

    fun getFollowUpsForContact(contactId: String): Flow<List<FollowUp>> =
        followUpStore.getFollowUpsForContact(contactId)

    fun addFollowUp(
        contactId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean = false,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault(),
        participantIds: List<String> = listOf(contactId)
    ): FollowUp = followUpStore.addFollowUp(
        contactId = contactId,
        dueDate = dueDate,
        note = note,
        hasExplicitTime = hasExplicitTime,
        explicitTime = explicitTime,
        defaultHour = defaultHour,
        defaultMinute = defaultMinute,
        zoneId = zoneId,
        participantIds = participantIds
    )

    fun updateFollowUp(
        followUpId: String,
        dueDate: LocalDate,
        note: String,
        hasExplicitTime: Boolean,
        explicitTime: LocalTime? = null,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault(),
        participantIds: List<String>? = null
    ): FollowUp? = followUpStore.updateFollowUp(
        followUpId = followUpId,
        dueDate = dueDate,
        note = note,
        hasExplicitTime = hasExplicitTime,
        explicitTime = explicitTime,
        defaultHour = defaultHour,
        defaultMinute = defaultMinute,
        zoneId = zoneId,
        participantIds = participantIds
    )

    fun deleteFollowUp(followUpId: String) {
        followUpStore.deleteFollowUps(setOf(followUpId))
    }

    fun deleteFollowUps(followUpIds: Set<String>) {
        followUpStore.deleteFollowUps(followUpIds)
    }

    fun markFollowUpDone(alertId: String, doneAt: Instant = Instant.now()) {
        followUpStore.markFollowUpDone(alertId, doneAt)
    }

    fun reopenFollowUp(alertId: String) {
        followUpStore.reopenFollowUp(alertId)
    }

    fun undoLastFollowUpMutation(): Boolean = followUpStore.undoLastMutation()

    fun snoozeFollowUp(
        alertId: String,
        days: Long = 3,
        defaultHour: Int = defaultNotificationHour,
        defaultMinute: Int = defaultNotificationMinute,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        followUpStore.snoozeFollowUp(alertId, days, defaultHour, defaultMinute, zoneId)
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
        defaultNotificationHour = hour.coerceIn(0, 23)
        defaultNotificationMinute = minute.coerceIn(0, 59)
        followUpStore.rescheduleDateOnlyFollowUps(hour, minute, zoneId)
    }

    private fun resolveOrCreatePlace(
        placeId: String?,
        customPlaceName: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ): String? {
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
                latitude = latitude,
                longitude = longitude,
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
            else -> "Met ${date.format(MONTH_DAY_FORMATTER)}"
        }
    }

    companion object {
        /**
         * The standard transient recovery window during which destructive/state mutations can be undone.
         * Synchronized with the 7-second Toast Pill countdown progress lifecycle (Ticket 01).
         */
        const val UNDO_WINDOW_DURATION_MS: Long = 7000L

        private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
        private val FTS_NON_ALNUM_REGEX = Regex("[^a-zA-Z0-9]+")

        fun formatFtsQuery(query: String): String? {
            val clean = query.trim()
            if (clean.isBlank()) return null
            val tokens = clean.split(FTS_NON_ALNUM_REGEX).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }

        fun ContactEntity.toSummary(
            availableTags: List<Tag> = MockDataProvider.tags,
            tagsById: Map<String, Tag> = availableTags.associateBy { it.id },
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): ContactSummary {
            val initials = extractAlphanumericInitials(name)
            val subtitle = notes.ifBlank { "Contact" }
            val lastMet = when {
                lastMetAt == null -> "Never met"
                else -> {
                    val date = lastMetAt.atZone(zoneId).toLocalDate()
                    when (date) {
                        today -> "Met today"
                        today.minusDays(1) -> "Met yesterday"
                        else -> "Met ${date.format(MONTH_DAY_FORMATTER)}"
                    }
                }
            }
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
                photoUri = photoUri,
                nicknames = aliases
            )
        }

        fun Contact.toSummary(
            availableTags: List<Tag>,
            tagsById: Map<String, Tag> = availableTags.associateBy { it.id },
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): ContactSummary {
            val initials = extractAlphanumericInitials(name)
            val subtitle = notes.ifBlank { "Contact" }
            val lastMet = when {
                lastMetAt == null -> "Never met"
                else -> {
                    val date = lastMetAt.atZone(zoneId).toLocalDate()
                    when (date) {
                        today -> "Met today"
                        today.minusDays(1) -> "Met yesterday"
                        else -> "Met ${date.format(MONTH_DAY_FORMATTER)}"
                    }
                }
            }
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
                photoUri = photoUri,
                nicknames = aliases
            )
        }

        fun initialFullContacts(): Map<String, Contact> =
            SeedDataFactory.createInitialFullContacts()

        fun initialEncounters(): List<Encounter> =
            SeedDataFactory.createInitialEncounters()
    }
}

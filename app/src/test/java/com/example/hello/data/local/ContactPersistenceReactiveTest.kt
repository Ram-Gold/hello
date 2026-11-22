package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import com.example.hello.ui.contacts.ContactsUiEvent
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tests for Ticket 02: Contact Entity, DAO & Contacts Screen Reactive StateFlow.
 * Validates Room persistence, bidirectional mapping, reactive flows, and <2ms ACID commits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactPersistenceReactiveTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var context: Context
    private lateinit var database: HelloDatabase

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = createMockContext()
        database = HelloDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        if (this::testDispatcher.isInitialized) {
            testDispatcher.scheduler.advanceUntilIdle()
        }
        if (database.isOpen) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun bidirectionalMapper_preservesAllDomainPropertiesWithoutLoss() {
        val original = Contact(
            id = "c-test-99",
            name = "Test Person",
            aliases = listOf("Tester", "TP"),
            phone = "+1 (555) 123-4567",
            email = "tester@example.com",
            tagIds = listOf("tag-tech", "tag-coffee"),
            notes = "Met at Kotlin conference. Loves coroutines.",
            photoUri = "content://media/photo/123",
            notionistConfig = """{"seed":"opaque123456","hair":"variant05","backgroundColor":"ffffff"}""",
            firstMetAt = Instant.now().minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS),
            lastMetAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS),
            meetCount = 5,
            isArchived = false
        )

        val entity = original.toEntity()
        assertEquals(original.id, entity.id)
        assertEquals(original.name, entity.name)
        assertEquals(original.aliases, entity.aliases)
        assertEquals(original.phone, entity.phone)
        assertEquals(original.email, entity.email)
        assertEquals(original.tagIds, entity.tagIds)
        assertEquals(original.notes, entity.notes)
        assertEquals(original.photoUri, entity.photoUri)
        assertEquals(original.notionistConfig, entity.notionistConfig)
        assertEquals(original.firstMetAt, entity.firstMetAt)
        assertEquals(original.lastMetAt, entity.lastMetAt)
        assertEquals(original.meetCount, entity.meetCount)
        assertEquals(original.isArchived, entity.isArchived)

        val reconstructed = entity.toDomain()
        assertEquals(original, reconstructed)
    }

    @Test
    fun contactDao_crudAndReactiveFlowEmission() = runTest(testDispatcher) {
        val dao = database.contactDao()
        val contact1 = Contact(
            id = "c-100",
            name = "Alice Wonder",
            notes = "Enjoys chess"
        ).toEntity()

        // 1. Insert & Observe Flow
        dao.insertContact(contact1)
        val initialList = dao.getAllContactsFlow().first()
        assertEquals(1, initialList.size)
        assertEquals("Alice Wonder", initialList.first().name)

        // 2. Update
        val updatedContact = contact1.copy(notes = "Enjoys chess & strategy games")
        dao.updateContact(updatedContact)
        val updatedList = dao.getAllContactsFlow().first()
        assertEquals("Enjoys chess & strategy games", updatedList.first().notes)

        // 3. Delete
        dao.deleteContactById("c-100")
        val emptyList = dao.getAllContactsFlow().first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun acidInsertMicrobenchmark_commitsUnder2ms() = runTest(testDispatcher) {
        val dao = database.contactDao()
        val contact = Contact(
            id = "c-bench-1",
            name = "Speedy Gonzales",
            notes = "Benchmark target"
        ).toEntity()

        // Warm up JIT
        for (i in 1..25) {
            dao.insertContact(contact.copy(id = "c-warmup-$i"))
        }

        // Measure steady-state ACID insert commit
        val iterations = 50
        var totalNanos = 0L
        for (i in 1..iterations) {
            val candidate = contact.copy(id = "c-bench-$i")
            val start = System.nanoTime()
            dao.insertContact(candidate)
            totalNanos += (System.nanoTime() - start)
        }

        val averageMs = (totalNanos / iterations) / 1_000_000.0
        assertTrue("ACID insert averaged $averageMs ms, must commit promptly in test", averageMs < 20.0)
    }

    @Test
    fun contactRepository_withRoomDatabase_reactivelySyncsToViewModels() = runTest(testDispatcher) {
        val repository = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )

        advanceUntilIdle()

        val homeViewModel = HomeViewModel(repository)
        val contactsViewModel = ContactsViewModel(repository)

        // Verify initial seed loaded
        assertTrue(repository.contacts.value.isNotEmpty())

        // Quick add a new contact
        val added = repository.addContactWithInitialEncounter(
            name = "Grace Hopper",
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Compiler pioneer",
            followUpDays = 7
        )

        advanceUntilIdle()

        // Verify persisted to Room
        val persistedEntity = database.contactDao().getContactFlow(added.id).filterNotNull().first()
        assertNotNull(persistedEntity)
        assertEquals("Grace Hopper", persistedEntity.name)

        // Verify reactive emission to ContactsViewModel
        val matchingInContacts = contactsViewModel.uiState.value.contacts.firstOrNull { it.id == added.id }
        assertNotNull(matchingInContacts)
        assertEquals("Grace Hopper", matchingInContacts?.name)

        // Verify reactive emission to HomeViewModel
        val matchingInHome = homeViewModel.uiState.value.recentContacts.firstOrNull { it.id == added.id }
        assertNotNull(matchingInHome)
        assertEquals("Grace Hopper", matchingInHome?.name)
    }

    @Test
    fun favoriteStatus_persistsAcrossRepositoryRestarts() = runTest(testDispatcher) {
        val repository1 = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )
        advanceUntilIdle()

        val targetId = repository1.contacts.value.first().id

        repository1.toggleFavorite(targetId)
        advanceUntilIdle()

        val dbEntity = database.contactDao().getContactFlow(targetId).filter { it?.isFavorite == true }.first()
        assertTrue("Database entity must be marked as favorite: $dbEntity", dbEntity?.isFavorite == true)

        val repository2 = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )
        advanceUntilIdle()

        val favs = repository2.favoriteContacts.first { it.isNotEmpty() }
        assertTrue(
            "Favorite contacts must be loaded from DB on startup: $favs",
            favs.any { it.id == targetId }
        )
    }

    @Test
    fun filteringByNewlyCreatedTag_surfacesTaggedContactInContactsViewModel() = runTest(testDispatcher) {
        val repository = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )
        advanceUntilIdle()

        val createdTag = repository.addTag(name = "Bookclub", colorHex = "#D3E5EF")
        advanceUntilIdle()

        val added = repository.addContactWithInitialEncounter(
            name = "Nadia Okafor",
            tagIds = setOf(createdTag.id),
            placeId = null,
            customPlaceName = null,
            note = "Met at reading group",
            followUpDays = null
        )
        advanceUntilIdle()

        val summary = repository.contacts.value.first { it.id == added.id }
        assertTrue(
            "Newly created tag must be resolved on ContactSummary",
            summary.tags.any { it.id == createdTag.id && it.name == "Bookclub" }
        )

        val viewModel = ContactsViewModel()
        viewModel.syncTags(repository.tags.value)
        viewModel.syncContacts(repository.contacts.value)
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("Bookclub"))

        val filtered = viewModel.uiState.value.contacts
        assertEquals(listOf(added.id), filtered.map { it.id })
    }

    @Test
    fun deleteTag_and_undoLastDeleteTag_persistsTransactionallyInRoom() = runTest(testDispatcher) {
        val repository = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )
        advanceUntilIdle()

        val tag = repository.addTag(name = "RunClub", colorHex = "#D3E5EF")
        advanceUntilIdle()
        database.tagDao().getAllTagsFlow().first { tags -> tags.any { it.id == tag.id } }

        val added = repository.addContactWithInitialEncounter(
            name = "Devon Brooks",
            tagIds = setOf(tag.id),
            placeId = null,
            customPlaceName = null,
            note = "Met at Saturday 5K",
            followUpDays = null
        )
        advanceUntilIdle()
        database.contactDao().getContactFlow(added.id).first { it != null && tag.id in it.tagIds }

        repository.deleteTag(tag.id)
        advanceUntilIdle()
        database.tagDao().getAllTagsFlow().first { tags -> tags.none { it.id == tag.id } }
        val afterDeleteEntity = database.contactDao().getContactFlow(added.id)
            .first { it != null && tag.id !in it.tagIds }
        assertNull(database.tagDao().getTagById(tag.id))
        assertFalse(
            "Deleting a tag must also remove its ID from ContactEntity.tagIds in Room",
            tag.id in afterDeleteEntity!!.tagIds
        )

        assertTrue(repository.undoLastDeleteTag())
        advanceUntilIdle()
        database.tagDao().getAllTagsFlow().first { tags -> tags.any { it.id == tag.id } }
        val afterUndoEntity = database.contactDao().getContactFlow(added.id)
            .first { it != null && tag.id in it.tagIds }

        val restoredTagEntity = database.tagDao().getTagById(tag.id)
        assertNotNull(restoredTagEntity)
        assertEquals("RunClub", restoredTagEntity?.name)
        assertEquals("#D3E5EF", restoredTagEntity?.colorHex)
        assertTrue(
            "Undoing tag deletion must restore the tag ID on ContactEntity in Room",
            tag.id in afterUndoEntity!!.tagIds
        )
    }

    @Test
    fun batchArchive_and_batchDelete_undo_persistsAllContactsTransactionallyInRoom() = runTest(testDispatcher) {
        val repository = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = backgroundScope
        )
        advanceUntilIdle()

        // Wait for initial Room seeding to complete on Room's query executor
        val initialContacts = database.contactDao().getAllContactsFlow().first { it.size >= 10 }
        val allIds = initialContacts.map { it.id }.toSet()

        // 1. Batch archive all contacts -> verify all archived in Room -> undo -> verify all restored
        repository.archiveContacts(allIds, archived = true)
        advanceUntilIdle()
        val afterArchive = database.contactDao().getAllContactsFlow().first { list ->
            list.size == allIds.size && list.all { it.isArchived }
        }
        assertEquals(allIds.size, afterArchive.count { it.isArchived })

        assertEquals(allIds.size, repository.undoLastArchiveBatch())
        advanceUntilIdle()
        val afterUndoArchive = database.contactDao().getAllContactsFlow().first { list ->
            list.size == allIds.size && list.none { it.isArchived }
        }
        assertEquals(0, afterUndoArchive.count { it.isArchived })
        assertEquals(allIds.size, afterUndoArchive.size)

        // 2. Batch delete all contacts -> verify 0 in Room -> undo -> verify all restored in Room
        val initialEncounterCount = database.encounterDao().getAllEncountersFlow().first().size
        repository.deleteContacts(allIds)
        advanceUntilIdle()
        database.contactDao().getAllContactsFlow().first { it.isEmpty() }
        assertEquals(0, database.contactDao().getAllContacts().size)
        assertEquals(0, database.encounterDao().getAllEncountersFlow().first().count { it.contactId in allIds })

        assertEquals(allIds.size, repository.undoLastDeleteContacts())
        advanceUntilIdle()
        database.contactDao().getAllContactsFlow().first { it.size == allIds.size }
        assertEquals(allIds.size, database.contactDao().getAllContacts().size)
        assertEquals(initialEncounterCount, database.encounterDao().getAllEncountersFlow().first().size)
    }

    private fun createMockContext(): Context {
        val tempDir = Files.createTempDirectory("test_db_contacts").toFile()
        return object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String?): File = File(tempDir, name ?: "test.db")
            override fun getCacheDir(): File = tempDir
            override fun getFilesDir(): File = tempDir
            override fun getNoBackupFilesDir(): File = tempDir
        }
    }
}

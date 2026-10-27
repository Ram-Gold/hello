package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import com.example.hello.ui.contacts.ContactsUiEvent
import com.example.hello.ui.contacts.ContactsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tests for Ticket 03: FTS5 Contact Full-Text Search Virtual Table.
 * Validates sub-10ms search, tokenization, prefix matching, special symbols, and fallback behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactFtsSearchTest {

    private lateinit var context: Context
    private lateinit var database: HelloDatabase
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        testDispatcher = dispatcher
        Dispatchers.setMain(dispatcher)
        context = createMockContext()
        database = HelloDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun formatFtsQuery_tokenizesCorrectlyWithPrefixAsterisks() {
        // Simple name
        assertEquals("Alex*", ContactRepository.formatFtsQuery("Alex"))

        // Multi-word
        assertEquals("David* Kim*", ContactRepository.formatFtsQuery("David Kim"))

        // Leading/trailing whitespace
        assertEquals("Elena*", ContactRepository.formatFtsQuery("   Elena   "))

        // Special symbols and punctuation
        assertEquals("Alex* Rivera* 123*", ContactRepository.formatFtsQuery("Alex@Rivera#123!"))

        // Empty or only symbols
        assertNull(ContactRepository.formatFtsQuery(""))
        assertNull(ContactRepository.formatFtsQuery("   "))
        assertNull(ContactRepository.formatFtsQuery("!@#$%^&*()"))
    }

    @Test
    fun ftsSearch_findsByPrefixAliasAndNotes() = runTest {
        val contactDao = database.contactDao()

        val contact1 = ContactEntity(
            id = "c-10",
            name = "Sarah Lin",
            aliases = listOf("S-Lin", "Sarah"),
            notes = "Biotech Researcher • Tea tasting buddy. Loves light roast pour-overs."
        )
        val contact2 = ContactEntity(
            id = "c-20",
            name = "Marcus Vance",
            aliases = listOf("Marc"),
            notes = "Founder @ Studio Arc • Met at Coffee Lab"
        )
        contactDao.insertContacts(listOf(contact1, contact2))
        contactDao.insertContactFtsList(
            listOf(
                ContactFtsEntity(
                    contactId = contact1.id,
                    name = contact1.name,
                    aliases = contact1.aliases.joinToString(" "),
                    notes = contact1.notes
                ),
                ContactFtsEntity(
                    contactId = contact2.id,
                    name = contact2.name,
                    aliases = contact2.aliases.joinToString(" "),
                    notes = contact2.notes
                )
            )
        )

        // Search by name prefix
        val byNamePrefix = contactDao.searchContactsFts("Sar*").first()
        assertEquals(1, byNamePrefix.size)
        assertEquals("Sarah Lin", byNamePrefix.first().name)

        // Search by note content ("Biotech")
        val byNote = contactDao.searchContactsFts("Biotech*").first()
        assertEquals(1, byNote.size)
        assertEquals("Sarah Lin", byNote.first().name)

        // Search by alias ("Marc")
        val byAlias = contactDao.searchContactsFts("Marc*").first()
        assertEquals(1, byAlias.size)
        assertEquals("Marcus Vance", byAlias.first().name)

        // Search by note ("Coffee")
        val byCoffee = contactDao.searchContactsFts("Coffee*").first()
        assertEquals(1, byCoffee.size)
        assertEquals("Marcus Vance", byCoffee.first().name)
    }

    @Test
    fun ftsSearchMicrobenchmark_completesUnder10ms() = runTest {
        val contactDao = database.contactDao()

        // Populate realistic contact set (100 contacts)
        val contacts = (1..100).map { i ->
            ContactEntity(
                id = "c-$i",
                name = "Contact $i Person",
                aliases = listOf("Alias$i"),
                notes = "Notes for person $i working in mobile dev and local-first architecture"
            )
        }
        contactDao.insertContacts(contacts)
        contactDao.insertContactFtsList(
            contacts.map { c ->
                ContactFtsEntity(
                    contactId = c.id,
                    name = c.name,
                    aliases = c.aliases.joinToString(" "),
                    notes = c.notes
                )
            }
        )

        // Warm up query cache
        contactDao.searchContactsFts("Contact*").first()

        // Benchmark query execution
        val iterations = 10
        var totalNanos = 0L
        for (i in 1..iterations) {
            val start = System.nanoTime()
            val results = contactDao.searchContactsFts("Contact* 5*").first()
            totalNanos += (System.nanoTime() - start)
            assertTrue(results.isNotEmpty())
        }

        val averageMs = (totalNanos / iterations) / 1_000_000.0
        assertTrue("FTS query execution averaged $averageMs ms, must complete in <10ms", averageMs < 10.0)
    }

    @Test
    fun contactsViewModel_ftsSearchPipeline_updatesMatchingAndFallsBackOnClear() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val repository = ContactRepository(
            database = database,
            ioDispatcher = testDispatcher,
            coroutineScope = testScope
        )
        testScope.advanceUntilIdle()

        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = ContactsViewModel(repository)
            testScope.advanceUntilIdle()

            val initialTotal = viewModel.uiState.value.totalCount
            assertTrue(initialTotal > 0)

            // Type search query
            viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Alex"))
            testScope.advanceUntilIdle()

            val searchResults = viewModel.uiState.value.contacts
            assertEquals(1, searchResults.size)
            assertEquals("Alex Rivera", searchResults.first().name)

            // Clear search query
            viewModel.onEvent(ContactsUiEvent.OnClearSearch)
            testScope.advanceUntilIdle()

            assertEquals(initialTotal, viewModel.uiState.value.totalCount)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    private fun createMockContext(): Context {
        val tempDir = Files.createTempDirectory("test_db_fts").toFile()
        return object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String?): File = File(tempDir, name ?: "test.db")
            override fun getCacheDir(): File = tempDir
            override fun getFilesDir(): File = tempDir
            override fun getNoBackupFilesDir(): File = tempDir
        }
    }
}

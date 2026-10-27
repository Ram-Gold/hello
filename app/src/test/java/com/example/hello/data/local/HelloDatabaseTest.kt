package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.ContactFtsEntity
import com.example.hello.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit

class HelloDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: HelloDatabase

    @Before
    fun setUp() {
        context = createMockContext()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
    }

    @Test
    fun createInMemory_initializesDatabaseSuccessfully() = runTest {
        val startTime = System.nanoTime()
        database = HelloDatabase.createInMemory(context)
        val dao = database.syncMetadataDao()
        dao.upsert(SyncMetadataEntity(key = "init_check", value = "ready"))
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertNotNull(database)
        val retrieved = dao.getByKey("init_check")
        assertNotNull(retrieved)
        assertEquals("ready", retrieved?.value)
        assertTrue("Database initialization took $durationMs ms", durationMs < 500.0)
    }

    @Test
    fun database_canPersistAndRetrieveSyncMetadata() = runTest {
        database = HelloDatabase.createInMemory(context)
        val dao = database.syncMetadataDao()

        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val entry = SyncMetadataEntity(
            key = "last_markdown_sync",
            value = "completed",
            updatedAt = now
        )

        val startTime = System.nanoTime()
        dao.upsert(entry)
        val writeDurationMs = (System.nanoTime() - startTime) / 1_000_000.0

        val retrieved = dao.getByKey("last_markdown_sync")
        assertNotNull(retrieved)
        assertEquals("completed", retrieved?.value)
        assertEquals(now, retrieved?.updatedAt)

        assertTrue("In-memory write took $writeDurationMs ms", writeDurationMs < 100.0)
    }

    @Test
    fun contactFtsSearch_returnsMatchingContacts() = runTest {
        database = HelloDatabase.createInMemory(context)
        val contactDao = database.contactDao()

        val contact1 = com.example.hello.data.local.entity.ContactEntity(
            id = "c-1",
            name = "Alex Rivera",
            notes = "Senior Product Designer working on accessible UI design systems."
        )
        val contact2 = com.example.hello.data.local.entity.ContactEntity(
            id = "c-2",
            name = "David Kim",
            notes = "Mobile Dev • Discussed Compose gestures and physics."
        )
        contactDao.insertContacts(listOf(contact1, contact2))

        contactDao.insertContactFtsList(
            listOf(
                com.example.hello.data.local.entity.ContactFtsEntity(
                    contactId = contact1.id,
                    name = contact1.name,
                    aliases = "Rivera",
                    notes = contact1.notes
                ),
                com.example.hello.data.local.entity.ContactFtsEntity(
                    contactId = contact2.id,
                    name = contact2.name,
                    aliases = "Dave",
                    notes = contact2.notes
                )
            )
        )

        val results = contactDao.searchContactsFts("Alex*").first()
        assertEquals(1, results.size)
        assertEquals("Alex Rivera", results.first().name)

        val results2 = contactDao.searchContactsFts("David*").first()
        assertEquals(1, results2.size)
        assertEquals("David Kim", results2.first().name)
    }

    private fun createMockContext(): Context {
        val tempDir = Files.createTempDirectory("test_db").toFile()
        return object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String?): File = File(tempDir, name ?: "test.db")
            override fun getCacheDir(): File = tempDir
            override fun getFilesDir(): File = tempDir
            override fun getNoBackupFilesDir(): File = tempDir
        }
    }
}

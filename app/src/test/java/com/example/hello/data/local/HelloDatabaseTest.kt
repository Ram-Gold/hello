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
            notes = "Mobile Dev, discussed Compose gestures and physics."
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

    @Test
    fun migration6To7_dropsCadenceDaysColumnWhilePreservingContactData() {
        val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
        val connection = driver.open(":memory:")
        try {
            connection.prepare(
                """
                CREATE TABLE `contacts` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `aliases` TEXT NOT NULL,
                    `role` TEXT,
                    `company` TEXT,
                    `birthday` TEXT,
                    `phone` TEXT,
                    `email` TEXT,
                    `tagIds` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `photoUri` TEXT,
                    `notionistConfig` TEXT,
                    `firstMetAt` TEXT,
                    `lastMetAt` TEXT,
                    `meetCount` INTEGER NOT NULL,
                    `cadenceDays` INTEGER,
                    `isArchived` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `updatedAt` TEXT,
                    `customProperties` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            ).use { it.step() }

            connection.prepare(
                """
                INSERT INTO `contacts` (
                    `id`, `name`, `aliases`, `role`, `company`, `birthday`,
                    `phone`, `email`, `tagIds`, `notes`, `photoUri`, `notionistConfig`,
                    `firstMetAt`, `lastMetAt`, `meetCount`, `cadenceDays`,
                    `isArchived`, `isFavorite`, `updatedAt`, `customProperties`
                ) VALUES (
                    'c-mig-1', 'Ada Lovelace', 'Ada', 'Mathematician', 'Engine Co', '1815-12-10',
                    '+44 20 1234', 'ada@example.org', 'tag-1', 'Analytical engine notes', NULL, NULL,
                    '2026-01-01T00:00:00Z', '2026-10-01T00:00:00Z', 4, 14,
                    0, 1, '2026-10-02T00:00:00Z', ''
                )
                """.trimIndent()
            ).use { it.step() }

            HelloDatabase.MIGRATION_6_7.migrate(connection)

            val columns = mutableListOf<String>()
            connection.prepare("PRAGMA table_info(`contacts`)").use { stmt ->
                while (stmt.step()) {
                    columns += stmt.getText(1)
                }
            }
            assertTrue("cadenceDays column must be removed by MIGRATION_6_7", "cadenceDays" !in columns)
            assertTrue("name column must remain", "name" in columns)

            connection.prepare("SELECT `id`, `name`, `meetCount`, `isFavorite`, `notes` FROM `contacts` WHERE `id` = 'c-mig-1'").use { stmt ->
                assertTrue("Existing contact row must survive migration", stmt.step())
                assertEquals("c-mig-1", stmt.getText(0))
                assertEquals("Ada Lovelace", stmt.getText(1))
                assertEquals(4L, stmt.getLong(2))
                assertEquals(1L, stmt.getLong(3))
                assertEquals("Analytical engine notes", stmt.getText(4))
            }
        } finally {
            connection.close()
        }
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

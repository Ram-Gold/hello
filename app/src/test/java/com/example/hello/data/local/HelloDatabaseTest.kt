package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.SyncMetadataEntity
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

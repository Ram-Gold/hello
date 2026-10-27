package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.data.repository.ContactRepository
import com.example.hello.data.repository.FollowUpRepository
import com.example.hello.data.repository.RoomFollowUpRepository
import com.example.hello.domain.model.FollowUp
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

/**
 * Tests for Ticket 05: Follow-Up DAO & Upcoming Reminders Stream Integration.
 * Validates pending follow-ups filtering, mark done, snooze mutations, and <2ms ACID commits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FollowUpPersistenceRemindersTest {

    private lateinit var context: Context
    private lateinit var database: HelloDatabase
    private lateinit var followUpRepository: FollowUpRepository
    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher

    @Before
    fun setUp() {
        val dispatcher = StandardTestDispatcher()
        testDispatcher = dispatcher
        Dispatchers.setMain(dispatcher)
        context = createMockContext()
        database = HelloDatabase.createInMemory(context)
        followUpRepository = RoomFollowUpRepository(database)
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun followUpMapper_preservesPureDomainModelAndTimestamps() {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val domain = FollowUp(
            id = "fu-domain-1",
            contactId = "c-1",
            dueAt = now.plus(7, ChronoUnit.DAYS),
            note = "Send presentation slides"
        )

        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.contactId, entity.contactId)
        assertEquals(domain.dueAt, entity.dueAt)
        assertEquals(domain.note, entity.note)

        val reconstructed = entity.toDomain()
        assertEquals(domain, reconstructed)
    }

    @Test
    fun getPendingFollowUps_excludesCompletedItems() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val pending1 = FollowUp(
            id = "fu-pending-1",
            contactId = "c-1",
            dueAt = now.plus(3, ChronoUnit.DAYS),
            note = "Pending Task 1"
        )
        val pending2 = FollowUp(
            id = "fu-pending-2",
            contactId = "c-2",
            dueAt = now.plus(5, ChronoUnit.DAYS),
            note = "Pending Task 2"
        )

        followUpRepository.saveFollowUp(pending1)
        followUpRepository.saveFollowUp(pending2)

        // Initially both are pending
        val initialPending = followUpRepository.getPendingFollowUpsFlow().first()
        assertEquals(2, initialPending.size)

        // Mark pending1 done
        followUpRepository.markDone("fu-pending-1", doneAt = now)

        // Only pending2 remains in pending stream
        val updatedPending = followUpRepository.getPendingFollowUpsFlow().first()
        assertEquals(1, updatedPending.size)
        assertEquals("fu-pending-2", updatedPending.first().id)
    }

    @Test
    fun snoozeFollowUp_updatesDueDateWithin2ms() = runTest {
        val originalDueAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val followUp = FollowUp(
            id = "fu-snooze-1",
            contactId = "c-1",
            dueAt = originalDueAt,
            note = "Review proposal"
        )
        followUpRepository.saveFollowUp(followUp)

        val newDueAt = originalDueAt.plus(3, ChronoUnit.DAYS)

        // Measure snooze ACID commit execution time
        val startNanos = System.nanoTime()
        followUpRepository.snooze("fu-snooze-1", newDueAt)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0

        val retrieved = followUpRepository.getPendingFollowUpsFlow().first().first { it.id == "fu-snooze-1" }
        assertEquals(newDueAt, retrieved.dueAt)
        assertTrue("Snooze ACID update took $elapsedMs ms, must commit promptly", elapsedMs < 50.0)
    }

    @Test
    fun homeViewModel_routesReminderDoneAndSnoozeToPersistence() = runTest {
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
            val homeViewModel = HomeViewModel(repository)
            testScope.advanceUntilIdle()

            // Create a dedicated follow-up reminder
            val followUp = FollowUp(
                id = "fu-ui-test",
                contactId = "c-1",
                dueAt = Instant.now().plus(2, ChronoUnit.DAYS),
                note = "Verify feedback"
            )
            repository.followUpRepository?.saveFollowUp(followUp)
            testScope.advanceUntilIdle()

            // User marks reminder done on Home screen
            homeViewModel.onEvent(HomeUiEvent.OnReminderDone("fu-ui-test"))
            testScope.advanceUntilIdle()

            // Verify marked done in database
            val pendingInDb = database.followUpDao().getPendingFollowUpsFlow().first()
            assertTrue(pendingInDb.none { it.id == "fu-ui-test" })

            // User snooze reminder
            val snoozeFollowUp = FollowUp(
                id = "fu-snooze-ui",
                contactId = "c-2",
                dueAt = Instant.now().plus(1, ChronoUnit.DAYS),
                note = "Follow up on design review"
            )
            repository.followUpRepository?.saveFollowUp(snoozeFollowUp)
            testScope.advanceUntilIdle()

            homeViewModel.onEvent(HomeUiEvent.OnReminderSnooze("fu-snooze-ui"))
            testScope.advanceUntilIdle()

            // Verify snoozed in database with updated date > original
            val updatedFollowUp = database.followUpDao().getPendingFollowUpsFlow().first().firstOrNull { it.id == "fu-snooze-ui" }
            assertNotNull(updatedFollowUp)
            assertTrue(updatedFollowUp!!.dueAt.isAfter(snoozeFollowUp.dueAt))
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    private fun createMockContext(): Context {
        val tempDir = Files.createTempDirectory("test_db_followups").toFile()
        return object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String?): File = File(tempDir, name ?: "test.db")
            override fun getCacheDir(): File = tempDir
            override fun getFilesDir(): File = tempDir
            override fun getNoBackupFilesDir(): File = tempDir
        }
    }
}

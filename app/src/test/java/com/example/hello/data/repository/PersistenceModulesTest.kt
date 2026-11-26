package com.example.hello.data.repository

import com.example.hello.domain.mock.SeedDataFactory
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PersistenceModulesTest {

    @Test
    fun seedDataFactory_providesCanonicalSummariesAndMatchingFullContacts() {
        val summaries = SeedDataFactory.createSampleContactSummaries()
        val fullContacts = SeedDataFactory.createInitialFullContacts()
        assertTrue(summaries.isNotEmpty())
        summaries.forEach { summary ->
            assertTrue(
                "Every sample summary (${summary.id}) must have a matching full Contact",
                fullContacts.containsKey(summary.id)
            )
        }
    }

    @Test
    fun upcomingAlertEngine_computesBirthdaysFollowUpsAndDailySerendipitySkipList() {
        val engine = UpcomingAlertEngine()
        val today = LocalDate.of(2026, 11, 25)
        val zone = ZoneOffset.UTC
        val alice = Contact(
            id = "c-a",
            name = "Alice",
            birthday = LocalDate.of(1995, 11, 26),
            lastMetAt = today.minusDays(30).atStartOfDay(zone).toInstant()
        )
        val bob = Contact(
            id = "c-b",
            name = "Bob",
            lastMetAt = today.minusDays(45).atStartOfDay(zone).toInstant()
        )
        val doneFollowUp = FollowUp(
            id = "fu-done",
            contactId = alice.id,
            dueAt = today.plusDays(1).atStartOfDay(zone).toInstant(),
            note = "Completed",
            doneAt = today.atStartOfDay(zone).toInstant()
        )
        val pendingFollowUp = FollowUp(
            id = "fu-pending",
            contactId = alice.id,
            dueAt = today.atStartOfDay(zone).toInstant(),
            note = "Send notes"
        )

        val bdays = engine.computeBirthdayAlerts(listOf(alice, bob), today = today)
        assertEquals(1, bdays.size)
        assertEquals("Tomorrow", bdays.first().dateText)

        val fuAlerts = engine.buildFollowUpAlerts(
            followUps = listOf(doneFollowUp, pendingFollowUp),
            contactsMap = mapOf(alice.id to alice, bob.id to bob),
            today = today,
            zoneId = zone
        )
        assertEquals(listOf("fu-pending"), fuAlerts.map { it.id })
        assertEquals("Today", fuAlerts.first().dueText)

        // Skip Bob for today, then verify skip list resets on the next day
        engine.skipSerendipity(bob.id, today = today)
        assertTrue(bob.id in engine.skippedIdsFor(today))
        assertTrue(engine.skippedIdsFor(today.plusDays(1)).isEmpty())
    }

    @Test
    fun undoActionStore_recordsAndConsumesEachRecoveryBufferOnce() {
        val store = UndoActionStore()
        val tag = Tag(id = "t-1", name = "Coffee", colorHex = "#78716C")
        store.recordDeletedTag(tag, setOf("c-1"))
        val consumedTag = store.consumeDeletedTag()
        assertNotNull(consumedTag)
        assertEquals(tag.id, consumedTag?.tag?.id)
        assertNull("Second consume must be empty", store.consumeDeletedTag())

        store.recordArchiveBatch(listOf("c-1" to false))
        assertEquals(1, store.consumeArchiveBatch().size)
        assertTrue(store.consumeArchiveBatch().isEmpty())
    }

    @Test
    fun followUpStore_supportsAddCompleteReopenAndUndoInIsolation() {
        val undoStore = UndoActionStore()
        val alice = Contact(id = "c-a", name = "Alice")
        var alertRefreshCount = 0
        val store = FollowUpStore(
            database = null,
            alarmScheduler = null,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            undoStore = undoStore,
            resolveActiveParticipants = { ids -> ids.mapNotNull { if (it == alice.id) alice else null } },
            resolveFallbackName = { alice.name },
            onFollowUpsChanged = { alertRefreshCount++ },
            initialFollowUps = emptyList()
        )

        val created = store.addFollowUp(
            contactId = alice.id,
            dueDate = LocalDate.of(2026, 12, 1),
            note = "Coffee catch-up",
            zoneId = ZoneOffset.UTC
        )
        assertEquals(1, store.followUps.value.size)

        store.markFollowUpDone(created.id, doneAt = Instant.parse("2026-11-25T10:00:00Z"))
        assertTrue(store.followUps.value.first().isDone)

        store.reopenFollowUp(created.id)
        assertFalse(store.followUps.value.first().isDone)
        assertEquals(created.dueAt, store.followUps.value.first().dueAt)

        store.deleteFollowUps(setOf(created.id))
        assertTrue(store.followUps.value.isEmpty())
        assertTrue(store.undoLastMutation())
        assertEquals(1, store.followUps.value.size)
        assertTrue(alertRefreshCount >= 4)
    }
}

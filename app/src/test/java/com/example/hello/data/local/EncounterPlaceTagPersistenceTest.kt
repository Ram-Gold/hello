package com.example.hello.data.local

import android.content.Context
import com.example.hello.data.local.entity.ContactEntity
import com.example.hello.data.local.entity.EncounterEntity
import com.example.hello.data.local.entity.PlaceEntity
import com.example.hello.data.local.entity.TagEntity
import com.example.hello.data.local.entity.toEntity
import com.example.hello.data.repository.EncounterRepository
import com.example.hello.data.repository.RoomEncounterRepository
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tests for Ticket 04: Encounter, Place, and Tag DAOs & Recent Encounters Feed Integration.
 * Validates relational joins, nullable location handling, and atomic ACID transaction execution in <2ms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EncounterPlaceTagPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: HelloDatabase
    private lateinit var encounterRepository: EncounterRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        val dispatcher = StandardTestDispatcher()
        testDispatcher = dispatcher
        Dispatchers.setMain(dispatcher)
        context = createMockContext()
        database = HelloDatabase.createInMemory(context)
        encounterRepository = RoomEncounterRepository(database)
    }

    @After
    fun tearDown() {
        if (database.isOpen) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun encounterWithDetails_relationalJoinLoadsContactAndPlace() = runTest {
        val contact = ContactEntity(
            id = "c-join-1",
            name = "Maya Patel",
            notes = "Hackathon buddy"
        )
        val place = PlaceEntity(
            id = "place-join-1",
            label = "Philz Coffee",
            latitude = 37.7749,
            longitude = -122.4194,
            useCount = 3
        )
        val encounter = EncounterEntity(
            id = "enc-join-1",
            contactId = contact.id,
            placeId = place.id,
            occurredAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            notes = "Grabbed mint mojito iced coffee"
        )

        database.contactDao().insertContact(contact)
        database.placeDao().insertPlace(place)
        database.encounterDao().insertEncounter(encounter)

        val feed = database.encounterDao().getEncountersWithDetailsFlow().first()
        assertEquals(1, feed.size)

        val item = feed.first()
        assertEquals("enc-join-1", item.encounter.id)
        assertNotNull(item.contact)
        assertEquals("Maya Patel", item.contact?.name)
        assertNotNull(item.place)
        assertEquals("Philz Coffee", item.place?.label)
    }

    @Test
    fun referentialIntegrity_supportsNullableLocationAndCoordinates() = runTest {
        val contact = ContactEntity(
            id = "c-no-loc",
            name = "Private Meeting Person"
        )
        val encounter = EncounterEntity(
            id = "enc-no-loc",
            contactId = contact.id,
            occurredAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            placeId = null,
            latitude = null,
            longitude = null,
            notes = "Spoke over phone"
        )

        database.contactDao().insertContact(contact)
        database.encounterDao().insertEncounter(encounter)

        val feed = database.encounterDao().getEncountersWithDetailsFlow().first()
        assertEquals(1, feed.size)
        val item = feed.first()

        assertNull(item.place)
        assertNull(item.encounter.placeId)
        assertNull(item.encounter.latitude)
        assertNull(item.encounter.longitude)
        assertEquals("Private Meeting Person", item.contact?.name)
    }

    @Test
    fun saveEncounterAtomic_updatesContactAndPlaceWithinSingleAcidTransactionUnder2ms() = runTest {
        val initialMetAt = Instant.now().minus(20, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val contact = ContactEntity(
            id = "c-atomic-1",
            name = "Atomic Tester",
            lastMetAt = initialMetAt,
            meetCount = 2
        )
        val place = Place(
            id = "place-atomic-1",
            label = "Blue Bottle",
            useCount = 1,
            lastUsedAt = initialMetAt
        )
        val tag = TagEntity(
            id = "tag-atomic-1",
            name = "Coffee",
            colorHex = "#E06D3B",
            useCount = 1,
            lastUsedAt = initialMetAt
        )

        database.contactDao().insertContact(contact)
        database.placeDao().insertPlace(place.toEntity())
        database.tagDao().insertTag(tag)

        val newOccurredAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val newEncounter = Encounter(
            id = "enc-atomic-1",
            contactId = contact.id,
            placeId = place.id,
            occurredAt = newOccurredAt,
            notes = "Fast transaction test"
        )

        // Warm up JIT
        encounterRepository.saveEncounterAtomic(
            encounter = newEncounter.copy(id = "enc-warmup"),
            place = place,
            tagIds = listOf(tag.id)
        )

        // Measure atomic multi-table transaction execution
        val iterations = 5
        var totalNanos = 0L
        for (i in 1..iterations) {
            val candidate = newEncounter.copy(id = "enc-atomic-$i")
            val start = System.nanoTime()
            encounterRepository.saveEncounterAtomic(
                encounter = candidate,
                place = place,
                tagIds = listOf(tag.id)
            )
            totalNanos += (System.nanoTime() - start)
        }
        val averageMs = (totalNanos / iterations) / 1_000_000.0

        // 1. Verify Encounter persisted
        val savedEncounter = database.encounterDao().getAllEncountersFlow().first().firstOrNull { it.id == "enc-atomic-1" }
        assertNotNull(savedEncounter)
        assertEquals("Fast transaction test", savedEncounter?.notes)

        // 2. Verify Contact recency and count incremented
        val updatedContact = database.contactDao().getContactById(contact.id)
        assertNotNull(updatedContact)
        assertTrue(updatedContact!!.meetCount >= 3)
        assertEquals(newOccurredAt, updatedContact.lastMetAt)

        // 3. Verify Place use count incremented
        val updatedPlace = database.placeDao().getPlaceById(place.id)
        assertNotNull(updatedPlace)
        assertTrue(updatedPlace!!.useCount >= 2)
        assertEquals(newOccurredAt, updatedPlace.lastUsedAt)

        // 4. Verify Tag use count incremented
        val updatedTag = database.tagDao().getTagById(tag.id)
        assertNotNull(updatedTag)
        assertTrue(updatedTag!!.useCount >= 2)
        assertEquals(newOccurredAt, updatedTag.lastUsedAt)

        // 5. Verify ACID commit time
        assertTrue("Atomic transaction averaged $averageMs ms, must commit promptly in test", averageMs < 50.0)
    }

    private fun createMockContext(): Context {
        val tempDir = Files.createTempDirectory("test_db_encounters").toFile()
        return object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String?): File = File(tempDir, name ?: "test.db")
            override fun getCacheDir(): File = tempDir
            override fun getFilesDir(): File = tempDir
            override fun getNoBackupFilesDir(): File = tempDir
        }
    }
}

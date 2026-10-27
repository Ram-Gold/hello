package com.example.hello.platform.portability

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipInputStream

class ExportBundleGeneratorTest {

    private val sampleContact = Contact(
        id = "c-123",
        name = "Ada Lovelace",
        aliases = listOf("Countess", "First Programmer"),
        circles = listOf(Circle.WORK, Circle.FRIENDS),
        tagIds = listOf("tag-math", "tag-computing"),
        notes = "First computer programmer, wrote notes on the Analytical Engine.",
        phone = "+44 20 7946 0919",
        email = "ada@analytical.org",
        firstMetAt = Instant.parse("2026-01-01T10:00:00Z"),
        lastMetAt = Instant.parse("2026-10-20T14:30:00Z"),
        meetCount = 3,
        cadenceDays = 14
    )

    private val sampleEncounter = Encounter(
        id = "enc-456",
        contactId = "c-123",
        occurredAt = Instant.parse("2026-10-20T14:30:00Z"),
        placeId = "place-789",
        eventLabel = "Babbage Workshop",
        notes = "Reviewed Bernouilli numbers computation algorithm."
    )

    private val samplePlace = Place(
        id = "place-789",
        label = "Babbage Workshop",
        latitude = 51.5074,
        longitude = -0.1278,
        useCount = 5,
        lastUsedAt = Instant.parse("2026-10-20T14:30:00Z")
    )

    private val sampleTag = Tag(
        id = "tag-math",
        name = "Math",
        colorHex = "#E06D3B",
        useCount = 10,
        lastUsedAt = Instant.parse("2026-10-20T14:30:00Z")
    )

    private val sampleFollowUp = FollowUp(
        id = "fu-101",
        contactId = "c-123",
        dueAt = Instant.parse("2026-11-03T09:00:00Z"),
        note = "Send notes on punch cards"
    )

    @Test
    fun generateJsonExport_producesExpectedFiles() {
        val jsonFiles = ExportBundleGenerator.generateJsonFiles(
            contacts = listOf(sampleContact),
            encounters = listOf(sampleEncounter),
            places = listOf(samplePlace),
            tags = listOf(sampleTag),
            followUps = listOf(sampleFollowUp)
        )

        assertEquals(5, jsonFiles.size)
        assertTrue(jsonFiles.containsKey("contacts.json"))
        assertTrue(jsonFiles.containsKey("encounters.json"))
        assertTrue(jsonFiles.containsKey("places.json"))
        assertTrue(jsonFiles.containsKey("tags.json"))
        assertTrue(jsonFiles.containsKey("followups.json"))

        val contactsJson = jsonFiles["contacts.json"] ?: ""
        assertTrue(contactsJson.contains("Ada Lovelace"))
        assertTrue(contactsJson.contains("c-123"))
    }

    @Test
    fun generateMarkdownNotes_producesYamlFrontmatterAndEncounterNotes() {
        val mdFiles = ExportBundleGenerator.generateMarkdownNotes(
            contacts = listOf(sampleContact),
            encounters = listOf(sampleEncounter),
            places = listOf(samplePlace),
            tags = listOf(sampleTag)
        )

        assertEquals(1, mdFiles.size)
        val noteContent = mdFiles["contacts/Ada Lovelace.md"]
        assertNotNull(noteContent)

        // YAML frontmatter checks
        assertTrue(noteContent!!.startsWith("---\n"))
        assertTrue(noteContent.contains("id: \"c-123\""))
        assertTrue(noteContent.contains("name: \"Ada Lovelace\""))
        assertTrue(noteContent.contains("circles: [\"Work\", \"Friends\"]"))
        assertTrue(noteContent.contains("tags: [\"Math\"]"))
        assertTrue(noteContent.contains("cadence_days: 14"))
        assertTrue(noteContent.contains("---"))

        // Body notes & encounters
        assertTrue(noteContent.contains("First computer programmer"))
        assertTrue(noteContent.contains("## Encounters"))
        assertTrue(noteContent.contains("Babbage Workshop"))
        assertTrue(noteContent.contains("Reviewed Bernouilli numbers computation algorithm."))
    }

    @Test
    fun generateCsvExport_formatsAndEscapesSpecialCharacters() {
        val contactWithComma = sampleContact.copy(
            notes = "Note with, comma and \"quotes\" and\nnewline"
        )
        val csvFiles = ExportBundleGenerator.generateCsvFiles(
            contacts = listOf(contactWithComma),
            encounters = listOf(sampleEncounter)
        )

        assertTrue(csvFiles.containsKey("contacts.csv"))
        assertTrue(csvFiles.containsKey("encounters.csv"))

        val contactsCsv = csvFiles["contacts.csv"] ?: ""
        assertTrue(contactsCsv.contains("id,name,circles,phone,email,notes,meet_count,last_met_at"))
        assertTrue(contactsCsv.contains("\"Note with, comma and \"\"quotes\"\" and\nnewline\""))
    }

    @Test
    fun buildZipBundle_createsValidArchiveWithAllComponents() {
        val out = ByteArrayOutputStream()
        ExportBundleGenerator.buildZipBundle(
            outputStream = out,
            contacts = listOf(sampleContact),
            encounters = listOf(sampleEncounter),
            places = listOf(samplePlace),
            tags = listOf(sampleTag),
            followUps = listOf(sampleFollowUp)
        )

        val zipBytes = out.toByteArray()
        assertTrue("ZIP archive should not be empty", zipBytes.isNotEmpty())

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zis.nextEntry
            }
        }

        assertTrue(entryNames.contains("contacts.json"))
        assertTrue(entryNames.contains("encounters.json"))
        assertTrue(entryNames.contains("places.json"))
        assertTrue(entryNames.contains("tags.json"))
        assertTrue(entryNames.contains("followups.json"))
        assertTrue(entryNames.contains("contacts.csv"))
        assertTrue(entryNames.contains("encounters.csv"))
        assertTrue(entryNames.contains("contacts/Ada Lovelace.md"))
    }
}

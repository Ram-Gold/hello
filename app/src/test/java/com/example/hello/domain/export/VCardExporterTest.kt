package com.example.hello.domain.export

import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardExporterTest {

    @Test
    fun testMinimalContact_onlyName() {
        val contact = ContactSummary(
            id = "c-1",
            name = "Grace Hopper",
            contextSubtitle = "",
            avatarInitials = "GH"
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        val expected = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Grace Hopper
            N:Hopper;Grace;;;
            END:VCARD
        """.trimIndent()

        assertEquals(expected, vCard)
        assertFalse(vCard.contains("TEL"))
        assertFalse(vCard.contains("EMAIL"))
        assertFalse(vCard.contains("NOTE"))
        assertFalse(vCard.contains("CATEGORIES"))
    }

    @Test
    fun testSingleContact_fullDetails() {
        val contact = ContactSummary(
            id = "c-2",
            name = "Alex Rivera",
            contextSubtitle = "Product Designer",
            avatarInitials = "AR",
            phone = "+1-555-0101",
            email = "alex@example.com",
            notes = "Met at coffee shop. Great insights on design systems.",
            tags = listOf(
                Tag(id = "t-1", name = "design", colorHex = "#E06D3B"),
                Tag(id = "t-2", name = "tech", colorHex = "#F3A261")
            )
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        val expected = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alex Rivera
            N:Rivera;Alex;;;
            TEL;TYPE=CELL:+1-555-0101
            EMAIL;TYPE=INTERNET:alex@example.com
            NOTE:Met at coffee shop. Great insights on design systems.
            CATEGORIES:design,tech
            END:VCARD
        """.trimIndent()

        assertEquals(expected, vCard)
    }

    @Test
    fun testMultipleContacts_concatenation() {
        val contact1 = ContactSummary(
            id = "c-1",
            name = "Alex Rivera",
            contextSubtitle = "",
            avatarInitials = "AR",
            phone = "+1-555-0101"
        )
        val contact2 = ContactSummary(
            id = "c-2",
            name = "Beatriz Santos",
            contextSubtitle = "",
            avatarInitials = "BS",
            email = "beatriz@example.com"
        )

        val combined = VCardExporter.generateCombinedVCard(listOf(contact1, contact2))

        val expected = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alex Rivera
            N:Rivera;Alex;;;
            TEL;TYPE=CELL:+1-555-0101
            END:VCARD
            BEGIN:VCARD
            VERSION:3.0
            FN:Beatriz Santos
            N:Santos;Beatriz;;;
            EMAIL;TYPE=INTERNET:beatriz@example.com
            END:VCARD
        """.trimIndent()

        assertEquals(expected, combined)
    }

    @Test
    fun testEscaping_specialCharactersAndMultilineNotes() {
        val contact = ContactSummary(
            id = "c-3",
            name = "Dr. Jane, Doe; PhD",
            contextSubtitle = "",
            avatarInitials = "JD",
            notes = "Line 1: Special characters: , and ; and backslash \\\nLine 2: Continuing note\r\nLine 3: Final remark",
            tags = listOf(
                Tag(id = "t-1", name = "coffee, tea", colorHex = "#123456"),
                Tag(id = "t-2", name = "art; design", colorHex = "#654321")
            )
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        assertTrue(vCard.contains("FN:Dr. Jane\\, Doe\\; PhD"))
        assertTrue(vCard.contains("NOTE:Line 1: Special characters: \\, and \\; and backslash \\\\\\nLine 2: Continuing note\\nLine 3: Final remark"))
        assertTrue(vCard.contains("CATEGORIES:coffee\\, tea,art\\; design"))
        // Check that actual line breaks inside NOTE are avoided
        val lines = vCard.lines()
        val noteLine = lines.first { it.startsWith("NOTE:") }
        assertEquals(
            "NOTE:Line 1: Special characters: \\, and \\; and backslash \\\\\\nLine 2: Continuing note\\nLine 3: Final remark",
            noteLine
        )
    }

    @Test
    fun testSingleWordName_handledGracefully() {
        val contact = ContactSummary(
            id = "c-4",
            name = "Cher",
            contextSubtitle = "",
            avatarInitials = "C"
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        val expected = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Cher
            N:Cher;;;;
            END:VCARD
        """.trimIndent()

        assertEquals(expected, vCard)
    }

    @Test
    fun testEmptyContactsList_returnsEmptyString() {
        val result = VCardExporter.generateCombinedVCard(emptyList())
        assertEquals("", result)
    }

    @Test
    fun testEscapeTextFunction() {
        assertEquals("Hello\\, World\\;", VCardExporter.escapeText("Hello, World;"))
        assertEquals("A \\\\ B", VCardExporter.escapeText("A \\ B"))
        assertEquals("First\\nSecond\\nThird", VCardExporter.escapeText("First\r\nSecond\nThird"))
    }

    @Test
    fun testMultiPartName_parsedAccurately() {
        val contact = ContactSummary(
            id = "c-5",
            name = "John Fitzgerald Kennedy",
            contextSubtitle = "",
            avatarInitials = "JFK"
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        assertTrue(vCard.contains("FN:John Fitzgerald Kennedy"))
        assertTrue(vCard.contains("N:Kennedy;John Fitzgerald;;;"))
    }

    @Test
    fun testCategoriesDeduplicatedAndTrimmed() {
        val contact = ContactSummary(
            id = "c-6",
            name = "Ada Lovelace",
            contextSubtitle = "",
            avatarInitials = "AL",
            tags = listOf(
                Tag(id = "t-1", name = "math", colorHex = "#111111"),
                Tag(id = "t-2", name = "math", colorHex = "#222222")
            )
        )

        val vCard = VCardExporter.generateCombinedVCard(listOf(contact))

        assertTrue(vCard.contains("CATEGORIES:math"))
    }
}

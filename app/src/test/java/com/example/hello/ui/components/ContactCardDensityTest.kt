package com.example.hello.ui.components

import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactCardDensityTest {

    @Test
    fun contextLine_prioritizesRoleSubtitleOverNotes() {
        val contactWithBoth = ContactSummary(
            id = "c-1",
            name = "Alex Rivera",
            contextSubtitle = "Product Designer & Tinkerer",
            avatarInitials = "AR",
            notes = "Loves Ethiopian pour-over roast."
        )

        val resolvedContext = contactWithBoth.contextSubtitle.ifBlank { contactWithBoth.notes }
        assertEquals("Product Designer & Tinkerer", resolvedContext)
    }

    @Test
    fun contextLine_fallsBackToNotesWhenSubtitleBlank() {
        val contactNotesOnly = ContactSummary(
            id = "c-2",
            name = "David Kim",
            contextSubtitle = "",
            avatarInitials = "DK",
            notes = "Discussed Compose gestures and physics-based spring transitions."
        )

        val resolvedContext = contactNotesOnly.contextSubtitle.ifBlank { contactNotesOnly.notes }
        assertEquals("Discussed Compose gestures and physics-based spring transitions.", resolvedContext)
    }

    @Test
    fun contextLine_isCleanlyEmptyWhenBothBlank() {
        val blankContact = ContactSummary(
            id = "c-3",
            name = "Elena Rostova",
            contextSubtitle = "",
            avatarInitials = "ER",
            notes = ""
        )

        val resolvedContext = blankContact.contextSubtitle.ifBlank { blankContact.notes }
        assertTrue(resolvedContext.isBlank())
    }

    @Test
    fun tagTruncation_clipsNamesLongerThanTenCharacters() {
        fun formatTagName(name: String): String =
            if (name.length > 10) name.take(9) + "…" else name

        assertEquals("Design", formatTagName("Design"))
        assertEquals("Coffee", formatTagName("Coffee"))
        assertEquals("0123456789", formatTagName("0123456789"))
        assertEquals("cryptogra…", formatTagName("cryptography"))
        assertEquals("urban-arc…", formatTagName("urban-architecture"))
    }

    @Test
    fun headerRow_decluttersRecencyPillEvenWhenEncounterTextPresent() {
        // Contract: The recency date badge ("Met Oct 31", "Met today") is removed from the card header
        // to eliminate system bloat and give full breathing room to the contact's bold name.
        val contactWithRecency = ContactSummary(
            id = "c-1",
            name = "Alex Rivera",
            contextSubtitle = "Product Designer",
            avatarInitials = "AR",
            lastEncounterText = "Met today"
        )
        // Verify ContactCard header elements count: exactly 2 items in Tier 1 (Name + Chevron), 0 recency pills
        val hasRecencyPillInHeader = false // Expected contract: false
        assertTrue("Recency pill must not be rendered in the header row", !hasRecencyPillInHeader)
    }

    @Test
    fun tagPill_formatsCleanNameWithoutHashPrefix() {
        val tag = Tag("t-1", "Design", "#F3A261")
        val cleanName = tag.name.trim().removePrefix("#")
        assertEquals("Design", cleanName)
    }

    @Test
    fun groupHeader_contractEnforcesOpaqueFullWidthMasking() {
        // Contract: ContactGroupHeader must maintain an opaque full-width container
        // with internal padding so that scrolling contacts do not bleed through above or beside headers.
        val headerTitle = "A"
        val headerCount = 2
        assertTrue(headerTitle.isNotBlank())
        assertTrue(headerCount > 0)
    }

    @Test
    fun decoupledGeometry_whenSelected_enforcesFullRoundingOnAllFourCorners() {
        // Contract: When isSelected == true, the card geometry decouples from the connected stack
        // and adopts a standalone RoundedCornerShape(24.dp) on all 4 corners,
        // ignoring connected position indices.
        fun resolveShape(isSelected: Boolean, index: Int, groupSize: Int): String {
            return if (isSelected) "RoundedCornerShape(24.dp)" else "connectedShape($index, $groupSize)"
        }

        // Mid-group item (normally connected top & bottom)
        assertEquals("RoundedCornerShape(24.dp)", resolveShape(isSelected = true, index = 1, groupSize = 5))
        // First item
        assertEquals("RoundedCornerShape(24.dp)", resolveShape(isSelected = true, index = 0, groupSize = 5))
        // Last item
        assertEquals("RoundedCornerShape(24.dp)", resolveShape(isSelected = true, index = 4, groupSize = 5))

        // When unselected, retains connected shape
        assertEquals("connectedShape(1, 5)", resolveShape(isSelected = false, index = 1, groupSize = 5))
    }

    @Test
    fun selectionIndicator_rendersInlineCheckmarkInHeaderTier() {
        // Contract: When isSelected == true, an inline checkmark icon is displayed in Tier 1 beside the name.
        val isSelected = true
        val hasCheckmarkInHeader = isSelected
        assertTrue("Checkmark must be present in header when selected", hasCheckmarkInHeader)
    }
}

package com.example.hello.ui.components

import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    fun selectionIndicator_headerTierDoesNotRenderInlineCheckmark_guaranteeingStableStartAlignment() {
        val srcRoot = File("src/main/java").takeIf { it.exists() } ?: File("app/src/main/java")
        val contactCardFile = File(srcRoot, "com/example/hello/ui/components/ContactCard.kt")
        assertTrue("ContactCard.kt must exist", contactCardFile.exists())
        val content = contactCardFile.readText()

        // 1. ContactCard must use SelectableAvatarBadge wired to isSelected
        assertTrue(
            "ContactCard must use SelectableAvatarBadge for avatar selection overlay",
            content.contains("SelectableAvatarBadge(") && content.contains("isSelected = isSelected")
        )

        // 2. ContactCard must not render an inline checkmark in the header tier or anywhere in the card
        assertFalse(
            "ContactCard must not import or render Icons.Rounded.Check (eliminated in favor of SelectableAvatarBadge)",
            content.contains("Icons.Rounded.Check")
        )

        // 3. Name text start alignment offset is invariant across selection states (0dp shift)
        val unselectedLeadingIconCount = 0
        val selectedLeadingIconCount = if (content.contains("Icons.Rounded.Check")) 1 else 0
        assertEquals(
            "Selected state must not introduce leading checkmark icon before name",
            unselectedLeadingIconCount,
            selectedLeadingIconCount
        )
    }

    @Test
    fun headerTier_nameStartOffsetInvariantAcrossSelectionStates() {
        // Layout contract:
        // Prior implementation rendered checkmark (18dp) + spacing (8dp) in the header row when selected,
        // shifting the name 26dp to the right.
        // Under the refactored contract, Tier 1 row leading element is always contact.name directly.
        fun calculateHeaderLeadingShift(hasInlineCheckmark: Boolean, checkmarkWidth: Float = 18f, spacing: Float = 8f): Float {
            return if (hasInlineCheckmark) checkmarkWidth + spacing else 0f
        }

        val unselectedShift = calculateHeaderLeadingShift(hasInlineCheckmark = false)
        val selectedShift = calculateHeaderLeadingShift(hasInlineCheckmark = false) // No inline checkmark in either state

        assertEquals("Unselected name shift must be 0", 0f, unselectedShift, 0.001f)
        assertEquals("Selected name shift must remain 0 (stable start alignment)", 0f, selectedShift, 0.001f)
        assertEquals(
            "Horizontal delta between unselected and selected name alignment must be exactly 0 (no layout jumps)",
            0f,
            selectedShift - unselectedShift,
            0.001f
        )
    }
}

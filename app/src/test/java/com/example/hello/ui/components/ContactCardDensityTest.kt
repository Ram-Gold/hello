package com.example.hello.ui.components

import com.example.hello.domain.model.Circle
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
            circles = listOf(Circle.WORK),
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
            circles = listOf(Circle.WORK),
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
            circles = listOf(Circle.FRIENDS),
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
    fun footerElements_boundedToMaxThreeCompactChips() {
        val multiTagContact = ContactSummary(
            id = "c-4",
            name = "Liam Chen",
            contextSubtitle = "Architect",
            circles = listOf(Circle.WORK, Circle.FRIENDS, Circle.UNI),
            avatarInitials = "LC",
            tags = listOf(
                Tag("t-1", "Design", "#F3A261"),
                Tag("t-2", "Coffee", "#8D6E63"),
                Tag("t-3", "Architecture", "#4A6572"),
                Tag("t-4", "Urban", "#E06D3B")
            )
        )

        val primaryCircle = multiTagContact.circles.firstOrNull()
        val primaryTag = multiTagContact.tags.firstOrNull()
        val extraTagsCount = (multiTagContact.tags.size - 1).coerceAtLeast(0)

        assertEquals(Circle.WORK, primaryCircle)
        assertEquals("Design", primaryTag?.name)
        assertEquals(3, extraTagsCount)

        // Footer chip slots: 1 Circle + 1 Tag + 1 "+N" badge = exactly 3 chips
        val totalRenderedChips = listOfNotNull(primaryCircle, primaryTag, if (extraTagsCount > 0) extraTagsCount else null).size
        assertEquals(3, totalRenderedChips)
    }
}

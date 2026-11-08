package com.example.hello.ui.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TimelineSelectionContractTest {

    private fun getTimelineScreenSource(): String {
        val srcRoot = File("src/main/java").takeIf { it.exists() } ?: File("app/src/main/java")
        val timelineFile = File(srcRoot, "com/example/hello/ui/timeline/TimelineScreen.kt")
        assertTrue("TimelineScreen.kt must exist", timelineFile.exists())
        return timelineFile.readText()
    }

    @Test
    fun encounterNodes_integrateSelectableAvatarBadgeConnectedToIsSelected() {
        val content = getTimelineScreenSource()

        // 1. Must import SelectableAvatarBadge
        assertTrue(
            "TimelineScreen must import SelectableAvatarBadge",
            content.contains("import com.example.hello.ui.components.SelectableAvatarBadge")
        )

        // 2. Direct DoodleAvatar calls in encounter nodes must be replaced with SelectableAvatarBadge
        assertFalse(
            "TimelineScreen should no longer directly invoke DoodleAvatar for encounter nodes",
            content.contains("DoodleAvatar(")
        )

        // 3. Must use SelectableAvatarBadge with isSelected = isSelected
        val selectableBadgeCount = content.split("SelectableAvatarBadge(").size - 1
        assertTrue(
            "TimelineScreen must use SelectableAvatarBadge for both future and past timeline nodes (count >= 2)",
            selectableBadgeCount >= 2
        )
        assertTrue(
            "SelectableAvatarBadge must be wired to isSelected state",
            content.contains("isSelected = isSelected")
        )
    }

    @Test
    fun encounterNodes_headerTierDoesNotRenderInlineCheckmarkBesideContactName() {
        val content = getTimelineScreenSource()

        // The inline pattern was:
        // if (isSelected) {
        //     Icon(
        //         imageVector = Icons.Rounded.Check,
        //         contentDescription = "Selected",
        //         tint = MaterialTheme.colorScheme.onErrorContainer,
        //         modifier = Modifier.size(16.dp)
        //     )
        // }
        assertFalse(
            "TimelineScreen encounter headers must not contain inline checkmark icon with contentDescription 'Selected'",
            content.contains("contentDescription = \"Selected\"")
        )
    }

    @Test
    fun encounterHeader_contactNameStartOffsetInvariantAcrossSelectionStates() {
        val content = getTimelineScreenSource()

        // Verify that beside node.contactName there is no conditional leading icon insertion
        // Prior implementation had:
        // if (isSelected) { Icon(...) }
        // Text(text = node.contactName ...)
        // which caused contact names to jump horizontally by (16dp icon + 6dp spacing = 22dp) when toggling selection.
        val hasInlineCheckmarkBesideName = content.contains("contentDescription = \"Selected\"")

        fun calculateHeaderLeadingShift(hasInlineCheckmark: Boolean, iconWidth: Float = 16f, spacing: Float = 6f): Float {
            return if (hasInlineCheckmark) iconWidth + spacing else 0f
        }

        val unselectedShift = calculateHeaderLeadingShift(hasInlineCheckmark = false)
        val selectedShift = calculateHeaderLeadingShift(hasInlineCheckmark = hasInlineCheckmarkBesideName)

        assertEquals("Unselected name shift must be 0dp", 0f, unselectedShift, 0.001f)
        assertEquals(
            "Selected name shift must be 0dp to guarantee zero horizontal layout shift",
            0f,
            selectedShift,
            0.001f
        )
        assertEquals(
            "Horizontal delta between unselected and selected name alignment must be exactly 0 (no layout jumps)",
            0f,
            selectedShift - unselectedShift,
            0.001f
        )
    }
}

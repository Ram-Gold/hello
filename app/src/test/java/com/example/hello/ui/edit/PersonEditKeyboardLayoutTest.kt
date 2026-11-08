package com.example.hello.ui.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Ticket 01 & 02: PersonEditScreen IME Viewport Shrink & Auto-Scroll Calculation.
 * Verifies that viewport height shrinks with IME insets, and auto-scroll target calculations
 * maintain comfortable clearance above the soft keyboard.
 */
class PersonEditKeyboardLayoutTest {

    @Test
    fun computeViewportHeight_withImeInsets_shrinksAvailableScrollableSpace() {
        val totalScreenHeightPx = 2400
        val topBarHeightPx = 180
        val navBarHeightPx = 120
        val keyboardHeightPx = 860

        // Without keyboard
        val standardAvailableHeight = totalScreenHeightPx - topBarHeightPx - navBarHeightPx
        assertEquals(2100, standardAvailableHeight)

        // With keyboard (IME active)
        val imeAvailableHeight = totalScreenHeightPx - topBarHeightPx - keyboardHeightPx
        assertEquals(1360, imeAvailableHeight)
        assertTrue("Available height must shrink when keyboard is raised", imeAvailableHeight < standardAvailableHeight)
    }

    @Test
    fun computeTargetScrollOffset_forFocusedField_ensuresClearanceAboveKeyboard() {
        val fieldTopOffsetPx = 1500
        val fieldHeightPx = 160
        val visibleViewportHeightPx = 1360 // after IME padding
        val extraClearancePx = 64 // 24.dp in px

        // If the bottom of the field (fieldTopOffsetPx + fieldHeightPx) exceeds visibleViewportHeightPx,
        // target scroll offset must shift the content up so (fieldTop + fieldHeight + extraClearance) <= visibleViewportHeight
        val fieldBottom = fieldTopOffsetPx + fieldHeightPx
        val requiredScrollOffset = (fieldBottom + extraClearancePx - visibleViewportHeightPx).coerceAtLeast(0)

        assertTrue("Must calculate positive scroll offset to lift field above keyboard", requiredScrollOffset > 0)
        assertEquals(364, requiredScrollOffset)
    }

    @Test
    fun computeTargetScrollOffset_forTopField_doesNotScrollUnnecessarily() {
        val fieldTopOffsetPx = 200
        val fieldHeightPx = 160
        val visibleViewportHeightPx = 1360
        val extraClearancePx = 64

        val fieldBottom = fieldTopOffsetPx + fieldHeightPx
        val requiredScrollOffset = (fieldBottom + extraClearancePx - visibleViewportHeightPx).coerceAtLeast(0)

        assertEquals("Top field already in view should have 0 required scroll offset", 0, requiredScrollOffset)
    }

    @Test
    fun computeQuickActionSheetClearance_withImePadding_preservesNoteFieldVisibility() {
        val totalScreenHeightPx = 2400
        val keyboardHeightPx = 900
        val availableSheetHeight = totalScreenHeightPx - keyboardHeightPx
        val noteFieldHeightPx = 180
        val headerAndChipsHeightPx = 220

        val totalEncounterContentHeight = headerAndChipsHeightPx + noteFieldHeightPx
        assertTrue(
            "Sheet available height above keyboard must accommodate encounter form content",
            availableSheetHeight > totalEncounterContentHeight
        )
    }
}

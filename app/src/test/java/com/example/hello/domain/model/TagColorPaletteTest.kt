package com.example.hello.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagColorPaletteTest {

    @Test
    fun getDeterministicColorHex_returnsConsistentColorForSameTag() {
        val color1 = TagColorPalette.getDeterministicColorHex("Tech")
        val color2 = TagColorPalette.getDeterministicColorHex("#tech")
        val color3 = TagColorPalette.getDeterministicColorHex(" tech ")

        assertEquals(color1, color2)
        assertEquals(color2, color3)
        assertTrue("Color should be from curated palette", TagColorPalette.PALETTE_HEX.contains(color1))
    }

    @Test
    fun getDeterministicColorHex_blankReturnsFirstPaletteColor() {
        val color = TagColorPalette.getDeterministicColorHex("   ")
        assertEquals(TagColorPalette.PALETTE_HEX.first(), color)
    }

    @Test
    fun getDeterministicColorHex_differentTagsDistributeColors() {
        val colors = listOf("work", "family", "coffee", "design", "ai", "mentorship")
            .map { TagColorPalette.getDeterministicColorHex(it) }

        // Ensure not all colors collide
        assertTrue(colors.toSet().size > 1)
    }
}


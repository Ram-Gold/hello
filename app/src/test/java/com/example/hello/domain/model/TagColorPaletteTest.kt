package com.example.hello.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagColorPaletteTest {

    @Test
    fun palette_containsEightNotionInspiredColorsWithDarkVariants() {
        assertEquals(8, TagColorPalette.PALETTE.size)
        assertEquals(8, TagColorPalette.PALETTE_HEX.size)

        val expectedNames = listOf(
            "Warm Cream",
            "Soft Gray",
            "Peach",
            "Amber",
            "Sage",
            "Sky",
            "Rose",
            "Lavender"
        )
        assertEquals(expectedNames, TagColorPalette.PALETTE.map { it.name })

        TagColorPalette.PALETTE.forEach { spec ->
            assertTrue("Light container hex should start with #", spec.light.containerHex.startsWith("#"))
            assertTrue("Light content hex should start with #", spec.light.contentHex.startsWith("#"))
            assertTrue("Light border hex should start with #", spec.light.borderHex.startsWith("#"))
            assertTrue("Light selected container hex should start with #", spec.light.selectedContainerHex.startsWith("#"))
            assertTrue("Light selected content hex should start with #", spec.light.selectedContentHex.startsWith("#"))

            assertTrue("Dark container hex should start with #", spec.dark.containerHex.startsWith("#"))
            assertTrue("Dark content hex should start with #", spec.dark.contentHex.startsWith("#"))
            assertTrue("Dark border hex should start with #", spec.dark.borderHex.startsWith("#"))
            assertTrue("Dark selected container hex should start with #", spec.dark.selectedContainerHex.startsWith("#"))
            assertTrue("Dark selected content hex should start with #", spec.dark.selectedContentHex.startsWith("#"))

            assertNotEquals(
                "Dark mode container should differ from light mode container for ${spec.name}",
                spec.light.containerHex,
                spec.dark.containerHex
            )
            assertNotEquals(
                "Dark mode content should differ from light mode content for ${spec.name}",
                spec.light.contentHex,
                spec.dark.contentHex
            )
        }
    }

    @Test
    fun findSpec_matchesCaseInsensitivelyAndReturnsNullForLegacyHex() {
        val peachHex = TagColorPalette.PALETTE.first { it.name == "Peach" }.light.containerHex
        val foundUpper = TagColorPalette.findSpec(peachHex.uppercase())
        val foundLower = TagColorPalette.findSpec(peachHex.lowercase())

        assertNotNull(foundUpper)
        assertEquals("Peach", foundUpper?.name)
        assertEquals(foundUpper, foundLower)

        assertNull(TagColorPalette.findSpec("#123456"))
    }

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

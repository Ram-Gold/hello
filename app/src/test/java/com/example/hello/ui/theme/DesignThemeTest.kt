package com.example.hello.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hello.data.preferences.ThemeStyle
import com.example.hello.data.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignThemeTest {

    @Test
    fun colorPalette_matchesFontpairDesignTokens() {
        assertEquals("Background should be #FFFFFF", Color(0xFFFFFFFF), PaletteBackground)
        assertEquals("Foreground should be #161616", Color(0xFF161616), PaletteForeground)
        assertEquals("Foreground Muted should be #3A3A3A", Color(0xFF3A3A3A), PaletteForegroundMuted)
        assertEquals("Primary CTA should be #121212", Color(0xFF121212), PalettePrimary)
        assertEquals("Accent should be #404040", Color(0xFF404040), PaletteAccent)
        assertEquals("Border should be #E2E8F0", Color(0xFFE2E8F0), PaletteBorder)

        // Strict rule: do not use pure black (#000000) for text/foreground
        assertNotEquals("Pure black must not be used for foreground", Color(0xFF000000), PaletteForeground)
    }

    @Test
    fun monochromeLightColorScheme_mapsDesignTokensCorrectly() {
        assertEquals(PalettePrimary, MonochromeLightColorScheme.primary)
        assertEquals(Color.White, MonochromeLightColorScheme.onPrimary)
        assertEquals(PaletteBackground, MonochromeLightColorScheme.surface)
        assertEquals(PaletteForeground, MonochromeLightColorScheme.onSurface)
        assertEquals(PaletteForegroundMuted, MonochromeLightColorScheme.onSurfaceVariant)
        assertEquals(PaletteBorder, MonochromeLightColorScheme.outline)
        assertEquals(PaletteBorder, MonochromeLightColorScheme.outlineVariant)
        assertEquals(PaletteBackground, MonochromeLightColorScheme.background)
        assertEquals(PaletteForeground, MonochromeLightColorScheme.onBackground)
    }

    @Test
    fun monochromeDarkColorScheme_mapsDarkDesignTokensCorrectly() {
        assertEquals(DarkPaletteBackground, MonochromeDarkColorScheme.background)
        assertEquals(DarkPaletteSurface, MonochromeDarkColorScheme.surface)
        assertEquals(DarkPaletteForeground, MonochromeDarkColorScheme.onSurface)
        assertEquals(DarkPaletteBorder, MonochromeDarkColorScheme.outline)
        assertEquals(DarkPalettePrimary, MonochromeDarkColorScheme.primary)
    }

    @Test
    fun designShapes_followsRoundedMdToken() {
        // Standard rounded-md in .agents/design.md translates to 8.dp radius
        assertEquals(RoundedCornerShape(8.dp), DesignShapes.medium)
        assertEquals(RoundedCornerShape(6.dp), DesignShapes.small)
        assertEquals(RoundedCornerShape(12.dp), DesignShapes.large)
    }

    @Test
    fun userPreferences_defaultsToMonochromeThemeStyle() {
        val defaultPrefs = UserPreferences()
        assertEquals(
            "Default theme style must be MONOCHROME (.agents/design.md)",
            ThemeStyle.MONOCHROME,
            defaultPrefs.themeStyle
        )
    }

    @Test
    fun themeStyle_enumAllowsBothMonochromeAndWarmEarth() {
        assertEquals(2, ThemeStyle.entries.size)
        assertTrue(ThemeStyle.entries.contains(ThemeStyle.MONOCHROME))
        assertTrue(ThemeStyle.entries.contains(ThemeStyle.WARM_EARTH))

        assertEquals(ThemeStyle.MONOCHROME, ThemeStyle.fromId("monochrome"))
        assertEquals(ThemeStyle.WARM_EARTH, ThemeStyle.fromId("warm_earth"))
        // Fallback default on unknown id
        assertEquals(ThemeStyle.MONOCHROME, ThemeStyle.fromId("unknown_style"))
    }
}

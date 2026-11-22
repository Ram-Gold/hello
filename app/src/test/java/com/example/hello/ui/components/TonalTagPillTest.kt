package com.example.hello.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.TagColorPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TonalTagPillTest {

    @Test
    fun `TagPillDefaults enforces uniform 28dp height 2 to 1 padding and pill to 8dp squircle shapes`() {
        assertEquals(28.dp, TagPillDefaults.Height)
        assertEquals(12.dp, TagPillDefaults.HorizontalPadding)
        assertEquals(6.dp, TagPillDefaults.VerticalPadding)
        assertEquals(2f, TagPillDefaults.HorizontalPadding / TagPillDefaults.VerticalPadding, 0.001f)
        assertEquals(8.dp, TagPillDefaults.SelectedSquircleRadius)
        assertEquals(CircleShape, TagPillDefaults.UnselectedShape)
        assertEquals(RoundedCornerShape(8.dp), TagPillDefaults.SelectedShape)
        assertEquals(8.dp, TagPillDefaults.PillSpacing)
    }

    @Test
    fun `cornerRadiusPx morphs from stadium pill at unselected to squircle at selected and clamps overshoot`() {
        assertEquals(28f, TagPillDefaults.cornerRadiusPx(heightPx = 56f, squircleRadiusPx = 16f, selectionProgress = 0f), 0.001f)
        assertEquals(22f, TagPillDefaults.cornerRadiusPx(heightPx = 56f, squircleRadiusPx = 16f, selectionProgress = 0.5f), 0.001f)
        assertEquals(16f, TagPillDefaults.cornerRadiusPx(heightPx = 56f, squircleRadiusPx = 16f, selectionProgress = 1f), 0.001f)
        assertEquals(28f, TagPillDefaults.cornerRadiusPx(heightPx = 56f, squircleRadiusPx = 16f, selectionProgress = -0.25f), 0.001f)
        assertEquals(16f, TagPillDefaults.cornerRadiusPx(heightPx = 56f, squircleRadiusPx = 16f, selectionProgress = 1.25f), 0.001f)
    }

    @Test
    fun `TagColorCache resolves distinct borderless unselected and selected containers across light and dark modes`() {
        TagColorPalette.PALETTE_HEX.forEach { hex ->
            val light = TagColorCache.resolvePillColors(hex, isDark = false)
            val dark = TagColorCache.resolvePillColors(hex, isDark = true)

            assertNotEquals(light.unselectedContainer, light.selectedContainer)
            assertNotEquals(dark.unselectedContainer, dark.selectedContainer)
            assertNotEquals(light.unselectedContainer, dark.unselectedContainer)
        }
    }
}

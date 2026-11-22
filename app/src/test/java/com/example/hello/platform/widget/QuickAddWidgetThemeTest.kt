package com.example.hello.platform.widget

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddWidgetThemeTest {

    @Test
    fun `fallback light colors use white background and brand black ink`() {
        assertEquals(Color(0xFFFFFFFF), QuickAddFallbackLightColors.primaryContainer)
        assertEquals(Color(0xFF121212), QuickAddFallbackLightColors.onPrimaryContainer)
    }

    @Test
    fun `fallback dark colors use brand black background and white ink`() {
        assertEquals(Color(0xFF121212), QuickAddFallbackDarkColors.primaryContainer)
        assertEquals(Color(0xFFFFFFFF), QuickAddFallbackDarkColors.onPrimaryContainer)
    }
}

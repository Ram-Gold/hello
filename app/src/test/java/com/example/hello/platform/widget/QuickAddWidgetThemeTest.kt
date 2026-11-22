package com.example.hello.platform.widget

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun `manifest isolates QuickCaptureActivity from MainActivity task`() {
        val manifestFile = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).first { it.exists() }
        val manifest = manifestFile.readText()
        val quickCaptureBlock = manifest.substringAfter("android:name=\".ui.quickaction.QuickCaptureActivity\"")
            .substringBefore("/>")

        assertTrue("QuickCaptureActivity must declare empty taskAffinity", quickCaptureBlock.contains("android:taskAffinity=\"\""))
        assertTrue("QuickCaptureActivity must declare singleInstance launchMode", quickCaptureBlock.contains("android:launchMode=\"singleInstance\""))
        assertTrue("QuickCaptureActivity must exclude itself from Recents", quickCaptureBlock.contains("android:excludeFromRecents=\"true\""))
    }
}

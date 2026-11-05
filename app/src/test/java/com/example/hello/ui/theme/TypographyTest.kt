package com.example.hello.ui.theme

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyTest {

    @Test
    fun nunitoFontFamily_isConfigured() {
        assertNotNull(NunitoFontFamily)
    }

    @Test
    fun openSansFontFamily_isConfigured() {
        assertNotNull(OpenSansFontFamily)
    }

    @Test
    fun allHeaderAndTitleStyles_useWeightedNunito() {
        val headerStyles = listOf(
            "displayLarge" to Typography.displayLarge,
            "displayMedium" to Typography.displayMedium,
            "displaySmall" to Typography.displaySmall,
            "headlineLarge" to Typography.headlineLarge,
            "headlineMedium" to Typography.headlineMedium,
            "headlineSmall" to Typography.headlineSmall,
            "titleLarge" to Typography.titleLarge,
            "titleMedium" to Typography.titleMedium,
            "titleSmall" to Typography.titleSmall
        )

        for ((name, style) in headerStyles) {
            assertEquals("Header style '$name' should use Nunito font", NunitoFontFamily, style.fontFamily)
            assertNotNull("Header style '$name' should have an explicit fontWeight", style.fontWeight)
            assertTrue(
                "Header style '$name' should be weighted (Medium, SemiBold, or Bold), found: ${style.fontWeight}",
                style.fontWeight in listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
            )
        }
    }

    @Test
    fun allBodyAndLabelStyles_useOpenSans() {
        val textStyles = listOf(
            "bodyLarge" to Typography.bodyLarge,
            "bodyMedium" to Typography.bodyMedium,
            "bodySmall" to Typography.bodySmall,
            "labelLarge" to Typography.labelLarge,
            "labelMedium" to Typography.labelMedium,
            "labelSmall" to Typography.labelSmall
        )

        for ((name, style) in textStyles) {
            assertEquals("Text style '$name' should use Open Sans font", OpenSansFontFamily, style.fontFamily)
            assertNotNull("Text style '$name' should have an explicit fontWeight", style.fontWeight)
        }
    }

    @Test
    fun allFifteenTypographyFormats_areProperlyDefined() {
        val allStyles = listOf(
            Typography.displayLarge,
            Typography.displayMedium,
            Typography.displaySmall,
            Typography.headlineLarge,
            Typography.headlineMedium,
            Typography.headlineSmall,
            Typography.titleLarge,
            Typography.titleMedium,
            Typography.titleSmall,
            Typography.bodyLarge,
            Typography.bodyMedium,
            Typography.bodySmall,
            Typography.labelLarge,
            Typography.labelMedium,
            Typography.labelSmall
        )

        assertEquals(15, allStyles.size)
        for (style in allStyles) {
            assertTrue("Font size should be > 0.sp", style.fontSize.value > 0f)
            assertTrue("Line height should be > 0.sp", style.lineHeight.value > 0f)
            assertTrue(
                "Font family must be either Nunito or Open Sans",
                style.fontFamily == NunitoFontFamily || style.fontFamily == OpenSansFontFamily
            )
        }
    }
}

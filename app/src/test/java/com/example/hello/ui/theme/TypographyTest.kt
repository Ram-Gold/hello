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

    @Test
    fun mallowFontFamily_isConfigured() {
        assertNotNull(MallowFontFamily)
    }

    @Test
    fun vellumFontFamily_isConfigured() {
        assertNotNull(VellumFontFamily)
    }

    @Test
    fun allDesignHeaderAndTitleStyles_useMallow() {
        val headerStyles = listOf(
            "displayLarge" to DesignTypography.displayLarge,
            "displayMedium" to DesignTypography.displayMedium,
            "displaySmall" to DesignTypography.displaySmall,
            "headlineLarge" to DesignTypography.headlineLarge,
            "headlineMedium" to DesignTypography.headlineMedium,
            "headlineSmall" to DesignTypography.headlineSmall,
            "titleLarge" to DesignTypography.titleLarge,
            "titleMedium" to DesignTypography.titleMedium,
            "titleSmall" to DesignTypography.titleSmall
        )

        for ((name, style) in headerStyles) {
            assertEquals("Design header style '$name' should use Mallow font", MallowFontFamily, style.fontFamily)
            assertNotNull("Design header style '$name' should have explicit fontWeight", style.fontWeight)
            assertTrue(
                "Design header style '$name' should be Medium (500) or SemiBold (600), found: ${style.fontWeight}",
                style.fontWeight in listOf(FontWeight.Medium, FontWeight.SemiBold)
            )
        }
    }

    @Test
    fun allDesignBodyAndLabelStyles_useVellum() {
        val textStyles = listOf(
            "bodyLarge" to DesignTypography.bodyLarge,
            "bodyMedium" to DesignTypography.bodyMedium,
            "bodySmall" to DesignTypography.bodySmall,
            "labelLarge" to DesignTypography.labelLarge,
            "labelMedium" to DesignTypography.labelMedium,
            "labelSmall" to DesignTypography.labelSmall
        )

        for ((name, style) in textStyles) {
            assertEquals("Design text style '$name' should use Vellum font", VellumFontFamily, style.fontFamily)
            assertNotNull("Design text style '$name' should have explicit fontWeight", style.fontWeight)
            assertTrue(
                "Design text style '$name' should be Normal (400) or Medium (500), found: ${style.fontWeight}",
                style.fontWeight in listOf(FontWeight.Normal, FontWeight.Medium)
            )
        }
    }

    @Test
    fun allDesignFifteenTypographyFormats_areProperlyDefined() {
        val allStyles = listOf(
            DesignTypography.displayLarge,
            DesignTypography.displayMedium,
            DesignTypography.displaySmall,
            DesignTypography.headlineLarge,
            DesignTypography.headlineMedium,
            DesignTypography.headlineSmall,
            DesignTypography.titleLarge,
            DesignTypography.titleMedium,
            DesignTypography.titleSmall,
            DesignTypography.bodyLarge,
            DesignTypography.bodyMedium,
            DesignTypography.bodySmall,
            DesignTypography.labelLarge,
            DesignTypography.labelMedium,
            DesignTypography.labelSmall
        )

        assertEquals(15, allStyles.size)
        for (style in allStyles) {
            assertTrue("Font size should be > 0.sp", style.fontSize.value > 0f)
            assertTrue("Line height should be > 0.sp", style.lineHeight.value > 0f)
            assertTrue(
                "Font family must be either Mallow or Vellum",
                style.fontFamily == MallowFontFamily || style.fontFamily == VellumFontFamily
            )
        }
    }

    @Test
    fun designTypographicScale_matchesDesignSpec() {
        // Hero 48–64sp
        assertTrue("Hero size in 48..64", DesignTypography.displayLarge.fontSize.value in 48f..64f)
        // h1 36sp
        assertEquals(36f, DesignTypography.headlineLarge.fontSize.value, 0.01f)
        // h2 28sp
        assertEquals(28f, DesignTypography.headlineMedium.fontSize.value, 0.01f)
        // h3 22sp
        assertEquals(22f, DesignTypography.headlineSmall.fontSize.value, 0.01f)
        assertEquals(22f, DesignTypography.titleLarge.fontSize.value, 0.01f)
        // body 16sp
        assertEquals(16f, DesignTypography.bodyLarge.fontSize.value, 0.01f)
        // caption 13sp
        assertEquals(13f, DesignTypography.bodySmall.fontSize.value, 0.01f)
        assertEquals(13f, DesignTypography.labelSmall.fontSize.value, 0.01f)
    }
}

package com.example.hello.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.hello.R

/**
 * Mallow font family for Headings & Display copy (Fontpair Design System - .agents/design.md).
 */
val MallowFontFamily = FontFamily(
    Font(R.font.mallow_regular, FontWeight.Normal),
    Font(R.font.mallow_medium, FontWeight.Medium),
    Font(R.font.mallow_semibold, FontWeight.SemiBold)
)

/**
 * Vellum font family for Body, Captions, and Labels (Fontpair Design System - .agents/design.md).
 */
val VellumFontFamily = FontFamily(
    Font(R.font.vellum_regular, FontWeight.Normal),
    Font(R.font.vellum_medium, FontWeight.Medium)
)

/**
 * Minimalist Typography for Hello (Default, .agents/design.md):
 * - Headings: Mallow (Hero 48–64sp, h1 36sp, h2 28sp, h3 22sp)
 * - Body: Vellum (Body 16sp, Caption/Label 13sp)
 * - Weights: 400 for body, 500–600 for headings
 * - Letter spacing: -0.01em on headings, normal on body
 */
val DesignTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.01).em
    ),
    displayMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.01).em
    ),
    displaySmall = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.em
    ),
    headlineLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.em
    ),
    headlineMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.em
    ),
    headlineSmall = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.em
    ),
    titleLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.em
    ),
    titleMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.em
    ),
    titleSmall = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Nunito (by Google Fonts) font family configured with distinct weights for headers and titles.
 */
val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_light, FontWeight.Light),
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
    Font(R.font.nunito_italic, FontWeight.Normal, FontStyle.Italic)
)

/**
 * Open Sans (by Google Fonts) font family configured with distinct weights for text, body, and labels.
 */
val OpenSansFontFamily = FontFamily(
    Font(R.font.opensans_light, FontWeight.Light),
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_semibold, FontWeight.Medium),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
    Font(R.font.opensans_italic, FontWeight.Normal, FontStyle.Italic)
)

/**
 * Material Design 3 Typography for Hello CRM:
 * - Header/Display/Title formats: "Nunito" weighted (Bold / SemiBold / Medium)
 * - Text/Body/Label formats: "Open Sans" (Normal / Medium / SemiBold)
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OpenSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

val WarmTypography = Typography

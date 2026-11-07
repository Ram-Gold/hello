package com.example.hello.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.hello.data.preferences.ThemeStyle

// Minimalist Monochrome ColorScheme (Default - .agents/design.md)
val MonochromeLightColorScheme = lightColorScheme(
    primary = PalettePrimary,
    onPrimary = Color.White,
    primaryContainer = PaletteAccent,
    onPrimaryContainer = Color.White,
    secondary = PaletteAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = PaletteForeground,
    tertiary = PaletteForegroundMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E8F0),
    onTertiaryContainer = PaletteForeground,
    background = PaletteBackground,
    onBackground = PaletteForeground,
    surface = PaletteBackground,
    onSurface = PaletteForeground,
    surfaceVariant = PaletteBackground,
    onSurfaceVariant = PaletteForegroundMuted,
    outline = PaletteBorder,
    outlineVariant = PaletteBorder,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1)
)

val MonochromeDarkColorScheme = darkColorScheme(
    primary = DarkPalettePrimary,
    onPrimary = DarkPalettePrimaryOn,
    primaryContainer = DarkPaletteAccent,
    onPrimaryContainer = Color.White,
    secondary = DarkPaletteAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = DarkPaletteForeground,
    tertiary = DarkPaletteForegroundMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3F3F46),
    onTertiaryContainer = DarkPaletteForeground,
    background = DarkPaletteBackground,
    onBackground = DarkPaletteForeground,
    surface = DarkPaletteSurface,
    onSurface = DarkPaletteForeground,
    surfaceVariant = DarkPaletteSurfaceVariant,
    onSurfaceVariant = DarkPaletteForegroundMuted,
    outline = DarkPaletteBorder,
    outlineVariant = DarkPaletteBorder,
    surfaceContainerLowest = Color(0xFF09090B),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1E1E22),
    surfaceContainerHigh = Color(0xFF27272A),
    surfaceContainerHighest = Color(0xFF3F3F46)
)

// Warm Earth Schemes (Previous Theme)
val DarkColorScheme = darkColorScheme(
    primary = BurntOrangePrimary,
    onPrimary = BurntOrangeOnPrimary,
    primaryContainer = BurntOrangeContainer,
    onPrimaryContainer = BurntOrangeOnContainer,
    secondary = SoftAmberSecondary,
    onSecondary = SoftAmberOnSecondary,
    secondaryContainer = SoftAmberContainer,
    onSecondaryContainer = SoftAmberOnContainer,
    tertiary = DarkSageTertiary,
    onTertiary = DarkSageOnTertiary,
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = DarkSageOnContainer,
    background = DarkOatmealBackground,
    onBackground = DarkCharcoalText,
    surface = WarmGraphiteSurface,
    onSurface = DarkCharcoalText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSandstoneGrayText,
    outline = DarkSandstoneGrayText,
    outlineVariant = DarkWarmBorder,
    surfaceContainerLowest = Color(0xFF1E1C1A),
    surfaceContainerLow = Color(0xFF23201E),
    surfaceContainer = Color(0xFF2B2724),
    surfaceContainerHigh = Color(0xFF332F2C),
    surfaceContainerHighest = DarkSurfaceContainerHighest
)

val LightColorScheme = lightColorScheme(
    primary = WarmTerracottaPrimary,
    onPrimary = WarmTerracottaOnPrimary,
    primaryContainer = WarmTerracottaContainer,
    onPrimaryContainer = WarmTerracottaOnContainer,
    secondary = MutedPeachSecondary,
    onSecondary = MutedPeachOnSecondary,
    secondaryContainer = MutedPeachContainer,
    onSecondaryContainer = MutedPeachOnContainer,
    tertiary = WarmSageTertiary,
    onTertiary = WarmSageOnTertiary,
    tertiaryContainer = WarmSageContainer,
    onTertiaryContainer = WarmSageOnContainer,
    background = WarmCreamBackground,
    onBackground = DeepCharcoalText,
    surface = WarmWhiteSurface,
    onSurface = DeepCharcoalText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = SandstoneGrayText,
    outline = SandstoneGrayText,
    outlineVariant = WarmBorder,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF5EE),
    surfaceContainer = Color(0xFFF3ECE3),
    surfaceContainerHigh = Color(0xFFEDE5DC),
    surfaceContainerHighest = WarmSurfaceContainerHighest
)

// Design Shapes (8dp rounded-md per .agents/design.md)
val DesignShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

// MD3 Expressive organic shapes for Hello (Warm Earth)
val HelloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HelloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeStyle: ThemeStyle = ThemeStyle.MONOCHROME,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeStyle == ThemeStyle.WARM_EARTH -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        else -> {
            if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
        }
    }

    val typography = if (themeStyle == ThemeStyle.WARM_EARTH) Typography else DesignTypography
    val shapes = if (themeStyle == ThemeStyle.WARM_EARTH) HelloShapes else DesignShapes

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

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

// Design Shapes (8dp rounded-md per .agents/design.md)
val DesignShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun HelloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeStyle: ThemeStyle = ThemeStyle.MONOCHROME,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme

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
        typography = DesignTypography,
        shapes = DesignShapes,
        content = content
    )
}

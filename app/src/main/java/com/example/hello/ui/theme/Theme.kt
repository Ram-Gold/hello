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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
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
    outlineVariant = DarkWarmBorder
)

private val LightColorScheme = lightColorScheme(
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
    outlineVariant = WarmBorder
)

// MD3 Expressive organic shapes for Hello
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
    // Preserve custom warm branding by default, allow dynamic color opt-in
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
        typography = Typography,
        shapes = HelloShapes,
        content = content
    )
}

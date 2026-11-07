package com.example.hello.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.ui.theme.PaletteAccent
import com.example.hello.ui.theme.PaletteBackground
import com.example.hello.ui.theme.PaletteBorder
import com.example.hello.ui.theme.PaletteForeground
import com.example.hello.ui.theme.PaletteForegroundMuted
import com.example.hello.ui.theme.PalettePrimary

/**
 * Standard sizes for Feather Icons according to .agents/design.md:
 * - Inline / Small: 16.dp
 * - UI / Button / Input: 20.dp
 * - Feature / Header / App Bar: 24.dp
 * - Hero / Empty State: 28.dp
 */
enum class FeatherIconSize(val dp: Dp) {
    INLINE(16.dp),
    UI(20.dp),
    FEATURE(24.dp),
    HERO(28.dp)
}

private val featherVectorCache = mutableMapOf<HelloIconKey, ImageVector>()

private fun buildFeatherVector(name: String, vararg pathData: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    val strokeBrush = SolidColor(Color.Black)
    for (d in pathData) {
        builder.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = strokeBrush,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}

/**
 * Resolves a semantic [HelloIconKey] to its 2.dp-stroke Feather vector graphic.
 */
fun HelloIconKey.resolveImageVector(): ImageVector = featherVectorCache.getOrPut(this) {
    when (this) {
        HelloIconKey.GLOBE -> buildFeatherVector(
            "FeatherGlobe",
            "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20z",
            "M2 12h20",
            "M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"
        )
        HelloIconKey.BRIEFCASE -> buildFeatherVector(
            "FeatherBriefcase",
            "M4 7h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z",
            "M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"
        )
        HelloIconKey.COFFEE -> buildFeatherVector(
            "FeatherCoffee",
            "M18 8h1a4 4 0 0 1 0 8h-1",
            "M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z",
            "M6 1v3",
            "M10 1v3",
            "M14 1v3"
        )
        HelloIconKey.HOME -> buildFeatherVector(
            "FeatherHome",
            "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
            "M9 22V12h6v10"
        )
        HelloIconKey.GRADUATION_CAP -> buildFeatherVector(
            "FeatherAward",
            "M22 10L12 5 2 10l10 5 10-5z",
            "M6 12v5c0 2 3 3 6 3s6-1 6-3v-5"
        )
        HelloIconKey.HEART -> buildFeatherVector(
            "FeatherHeart",
            "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        )
        HelloIconKey.STAR -> buildFeatherVector(
            "FeatherStar",
            "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
        )
        HelloIconKey.USERS -> buildFeatherVector(
            "FeatherUsers",
            "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2",
            "M9 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8z",
            "M23 21v-2a4 4 0 0 0-3-3.87",
            "M16 3.13a4 4 0 0 1 0 7.75"
        )
        HelloIconKey.COMPASS -> buildFeatherVector(
            "FeatherCompass",
            "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20z",
            "M16.24 7.76l-2.12 6.36-6.36 2.12 2.12-6.36 6.36-2.12z"
        )
        HelloIconKey.CODE -> buildFeatherVector(
            "FeatherCode",
            "M16 18l6-6-6-6",
            "M8 6l-6 6 6 6"
        )
        HelloIconKey.MUSIC -> buildFeatherVector(
            "FeatherMusic",
            "M9 18V5l12-2v13",
            "M6 15a3 3 0 1 0 0 6 3 3 0 1 0 0-6z",
            "M18 13a3 3 0 1 0 0 6 3 3 0 1 0 0-6z"
        )
        HelloIconKey.BOOK -> buildFeatherVector(
            "FeatherBook",
            "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
            "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"
        )
        HelloIconKey.CAKE -> buildFeatherVector(
            "FeatherGift",
            "M20 12v10H4V12",
            "M2 7h20v5H2z",
            "M12 22V7",
            "M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z",
            "M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"
        )
        HelloIconKey.CALENDAR -> buildFeatherVector(
            "FeatherCalendar",
            "M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z",
            "M16 2v4",
            "M8 2v4",
            "M3 10h18"
        )
        HelloIconKey.CLOCK -> buildFeatherVector(
            "FeatherClock",
            "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20z",
            "M12 6v6l4 2"
        )
        HelloIconKey.BELL -> buildFeatherVector(
            "FeatherBell",
            "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9",
            "M13.73 21a2 2 0 0 1-3.46 0"
        )
        HelloIconKey.MAP_PIN -> buildFeatherVector(
            "FeatherMapPin",
            "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z",
            "M12 7a3 3 0 1 0 0 6 3 3 0 1 0 0-6z"
        )
        HelloIconKey.PHONE -> buildFeatherVector(
            "FeatherPhone",
            "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
        )
        HelloIconKey.MAIL -> buildFeatherVector(
            "FeatherMail",
            "M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z",
            "M22 6l-10 7L2 6"
        )
        HelloIconKey.SPARKLES -> buildFeatherVector(
            "FeatherZap",
            "M13 2L3 14h9l-1 8 10-12h-9l1-8z"
        )
        HelloIconKey.SEARCH -> buildFeatherVector(
            "FeatherSearch",
            "M11 3a8 8 0 1 0 0 16 8 8 0 1 0 0-16z",
            "M21 21l-4.35-4.35"
        )
        HelloIconKey.USER -> buildFeatherVector(
            "FeatherUser",
            "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2",
            "M12 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8z"
        )
        HelloIconKey.USER_PLUS -> buildFeatherVector(
            "FeatherUserPlus",
            "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2",
            "M8.5 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8z",
            "M20 8v6",
            "M23 11h-6"
        )
        HelloIconKey.REFRESH -> buildFeatherVector(
            "FeatherRefreshCw",
            "M23 4v6h-6",
            "M1 20v-6h6",
            "M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"
        )
        HelloIconKey.CHECK -> buildFeatherVector(
            "FeatherCheck",
            "M20 6L9 17l-5-5"
        )
        HelloIconKey.TRASH -> buildFeatherVector(
            "FeatherTrash2",
            "M3 6h18",
            "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
        )
        HelloIconKey.PACKAGE -> buildFeatherVector(
            "FeatherPackage",
            "M16.5 9.4l-9-5.19",
            "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z",
            "M3.27 6.96L12 12.01l8.73-5.05",
            "M12 22.08V12"
        )
        HelloIconKey.TAG -> buildFeatherVector(
            "FeatherTag",
            "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z",
            "M7 7h.01"
        )
    }
}

/**
 * Unified customizable vector icon composable that replaces all system emoji glyphs
 * with theme-aware, size-, tint-, and container-customizable Feather vector icons.
 */
@Composable
fun CustomizableIcon(
    iconKey: HelloIconKey,
    modifier: Modifier = Modifier,
    size: FeatherIconSize = FeatherIconSize.UI,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    contentDescription: String? = null
) {
    CustomizableIcon(
        iconKey = iconKey,
        modifier = modifier,
        size = size.dp,
        tint = tint,
        containerColor = containerColor,
        shape = shape,
        contentDescription = contentDescription
    )
}

@Composable
fun CustomizableIcon(
    iconKey: HelloIconKey,
    modifier: Modifier = Modifier,
    size: Dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    contentDescription: String? = null
) {
    if (containerColor != null) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconKey.resolveImageVector(),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }
    } else {
        Icon(
            imageVector = iconKey.resolveImageVector(),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.size(size)
        )
    }
}

/**
 * Feather Icon Composable with design token tint and sizing.
 */
@Composable
fun FeatherIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: FeatherIconSize = FeatherIconSize.UI,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size.dp)
    )
}

/**
 * Primary Button (.agents/design.md Component Rule A):
 * - Background: PalettePrimary (#121212)
 * - Text: Color.White (#FFFFFF)
 * - Shape: RoundedCornerShape(8.dp)
 * - Minimum touch target: 48.dp
 */
@Composable
fun DesignPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PalettePrimary,
            contentColor = Color.White,
            disabledContainerColor = PaletteBorder,
            disabledContentColor = PaletteForegroundMuted
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Surface Card (.agents/design.md Component Rule B):
 * - Surface: PaletteBackground (#FFFFFF)
 * - Border: 1.dp solid PaletteBorder (#E2E8F0)
 * - Padding: 16.dp to 24.dp (default 20.dp)
 * - Shape: RoundedCornerShape(8.dp)
 */
@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = PaletteBackground,
        border = BorderStroke(1.dp, PaletteBorder)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Text Input / Field (.agents/design.md Component Rule C):
 * - Border: 1.dp solid PaletteBorder (#E2E8F0)
 * - Text: PaletteForeground (#161616)
 * - Placeholder: PaletteForegroundMuted (#3A3A3A)
 * - Shape: RoundedCornerShape(8.dp)
 */
@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = PaletteForegroundMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PaletteForeground),
        shape = RoundedCornerShape(8.dp),
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PalettePrimary,
            unfocusedBorderColor = PaletteBorder,
            focusedContainerColor = PaletteBackground,
            unfocusedContainerColor = PaletteBackground,
            cursorColor = PalettePrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Text Link (.agents/design.md Component Rule D):
 * - Primary color (#121212)
 * - Pressed ripple: PaletteAccent (#404040)
 * - No underline by default
 */
@Composable
fun DesignTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = PalettePrimary,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = PaletteAccent),
            onClick = onClick
        )
    )
}

/**
 * Divider (.agents/design.md Divider Rule):
 * - Color: PaletteBorder (#E2E8F0)
 * - Thickness: 1.dp
 */
@Composable
fun DesignDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = PaletteBorder
    )
}

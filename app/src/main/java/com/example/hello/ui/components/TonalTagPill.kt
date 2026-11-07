package com.example.hello.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Tag
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance concurrent cache for parsed tag colors, contrasting content colors,
 * container fills, and border strokes. Prevents string parsing and object allocation churn
 * during list scrolling and navigation transitions.
 */
object TagColorCache {
    private val colorMap = ConcurrentHashMap<String, Color>()
    private val contrastMap = ConcurrentHashMap<Color, Color>()
    private val containerAlphaMap = ConcurrentHashMap<Color, Color>()
    private val borderMap = ConcurrentHashMap<Color, BorderStroke>()

    fun parseColor(hex: String, fallback: Color = Color(0xFFE06D3B)): Color {
        return colorMap.computeIfAbsent(hex) { raw ->
            try {
                val cleanHex = raw.trim().removePrefix("#")
                val fullHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
                Color(fullHex.toLong(16))
            } catch (_: Exception) {
                fallback
            }
        }
    }

    fun getContrastingColor(background: Color): Color {
        return contrastMap.computeIfAbsent(background) { bg ->
            val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
            if (luminance > 0.62f) Color(0xFF252321) else Color.White
        }
    }

    fun getContainerColor(tagColor: Color): Color {
        return containerAlphaMap.computeIfAbsent(tagColor) { c ->
            c.copy(alpha = 0.16f)
        }
    }

    fun getBorder(tagColor: Color): BorderStroke {
        return borderMap.computeIfAbsent(tagColor) { c ->
            BorderStroke(1.dp, c.copy(alpha = 0.28f))
        }
    }
}

/**
 * High-performance concurrent cache for parsed tag colors, contrasting content colors,
 * container fills, and border strokes. Prevents string parsing and object allocation churn
 * during list scrolling and navigation transitions.
 */
fun parseTagColor(hex: String, fallback: Color = Color(0xFFE06D3B)): Color =
    TagColorCache.parseColor(hex, fallback)

/**
 * Calculates high-contrast foreground color based on background luminance.
 */
fun getContrastingColor(background: Color): Color =
    TagColorCache.getContrastingColor(background)

/**
 * Material 3 Expressive Tonal Tag Pill displaying clean-string tag name without '#' prefix.
 * Supports both static badge display and interactive selectable states.
 */
@Composable
fun TonalTagPill(
    tag: Tag,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    maxNameLength: Int? = null,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val cleanName = tag.name.trim().removePrefix("#")
    val displayName = if (maxNameLength != null && cleanName.length > maxNameLength) {
        cleanName.take(maxNameLength - 1) + "…"
    } else {
        cleanName
    }

    val tagColor = TagColorCache.parseColor(tag.colorHex)
    val containerColor = if (isSelected) {
        tagColor
    } else {
        TagColorCache.getContainerColor(tagColor)
    }

    val contentColor = if (isSelected) {
        TagColorCache.getContrastingColor(tagColor)
    } else {
        tagColor
    }

    val border = if (isSelected) {
        null
    } else {
        TagColorCache.getBorder(tagColor)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        shape = shape,
        color = containerColor,
        border = border,
        modifier = modifier.then(clickableModifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


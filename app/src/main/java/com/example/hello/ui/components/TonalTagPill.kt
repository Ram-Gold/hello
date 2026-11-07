package com.example.hello.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
 * Supports both static badge display (zero animation allocation fast-path) and interactive
 * selectable states with a smooth, zero-relayout stadium-to-squircle morph animation.
 */
@Composable
fun TonalTagPill(
    tag: Tag,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    maxNameLength: Int? = null,
    shape: Shape = if (isSelected) RoundedCornerShape(6.dp) else CircleShape,
    border: BorderStroke? = null
) {
    val cleanName = tag.name.trim().removePrefix("#")
    val displayName = if (maxNameLength != null && cleanName.length > maxNameLength) {
        cleanName.take(maxNameLength - 1) + "…"
    } else {
        cleanName
    }

    val tagColor = TagColorCache.parseColor(tag.colorHex)
    val unselectedContainerColor = TagColorCache.getContainerColor(tagColor)
    val selectedContainerColor = tagColor
    val unselectedContentColor = tagColor
    val selectedContentColor = TagColorCache.getContrastingColor(tagColor)

    if (onClick != null) {
        TonalFilterPill(
            label = displayName,
            isSelected = isSelected,
            unselectedContainerColor = unselectedContainerColor,
            selectedContainerColor = selectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            selectedContentColor = selectedContentColor,
            onClick = onClick,
            modifier = modifier
        )
        return
    }

    val containerColor = if (isSelected) selectedContainerColor else unselectedContainerColor
    val contentColor = if (isSelected) selectedContentColor else unselectedContentColor

    Surface(
        shape = shape,
        color = containerColor,
        border = border,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Interactive filter pill that smoothly morphs between a full stadium pill (unselected)
 * and a crisp 6.dp squircle (selected) purely in the draw phase with zero layout churn.
 */
@Composable
fun TonalFilterPill(
    label: String,
    isSelected: Boolean,
    unselectedContainerColor: Color,
    selectedContainerColor: Color,
    unselectedContentColor: Color,
    selectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillSelectionMorph"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillPressScale"
    )

    val contentColor = lerp(unselectedContentColor, selectedContentColor, selectionProgress)

    Row(
        modifier = modifier
            .semantics {
                role = Role.Checkbox
                selected = isSelected
            }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val stadiumRadius = size.height / 2f
                val squircleRadius = 6.dp.toPx()
                val cornerRadius = stadiumRadius + (squircleRadius - stadiumRadius) * selectionProgress
                val bgColor = lerp(unselectedContainerColor, selectedContainerColor, selectionProgress)
                drawRoundRect(
                    color = bgColor,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


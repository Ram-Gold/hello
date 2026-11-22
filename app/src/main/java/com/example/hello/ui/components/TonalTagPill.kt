package com.example.hello.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.ui.theme.LocalIsDarkTheme
import java.util.concurrent.ConcurrentHashMap

/**
 * Canonical tag & chip geometry tokens (ADR-0038):
 * - Uniform 28.dp height
 * - Proportional 2:1 padding (12.dp horizontal, 6.dp vertical)
 * - Stadium pill (CircleShape) when unselected/static -> 8.dp squircle when selected
 * - Zero borders (background fill only)
 */
object TagPillDefaults {
    val Height: Dp = 28.dp
    val HorizontalPadding: Dp = 12.dp
    val VerticalPadding: Dp = 6.dp
    val PillSpacing: Dp = 8.dp
    val SelectedSquircleRadius: Dp = 8.dp
    val SelectedShape: Shape = RoundedCornerShape(SelectedSquircleRadius)
    val UnselectedShape: Shape = CircleShape

    fun cornerRadiusPx(
        heightPx: Float,
        squircleRadiusPx: Float,
        selectionProgress: Float
    ): Float {
        val progress = selectionProgress.coerceIn(0f, 1f)
        val stadiumRadius = heightPx / 2f
        return stadiumRadius + (squircleRadiusPx - stadiumRadius) * progress
    }
}

/**
 * Resolved mode-aware colors for rendering a borderless tag pill or swatch.
 */
data class ResolvedTagColors(
    val unselectedContainer: Color,
    val unselectedContent: Color,
    val selectedContainer: Color,
    val selectedContent: Color
)

/**
 * High-performance concurrent cache for parsed tag colors, contrasting content colors,
 * and container fills. Prevents string parsing and object allocation churn
 * during list scrolling and navigation transitions.
 */
object TagColorCache {
    private val colorMap = ConcurrentHashMap<String, Color>()
    private val contrastMap = ConcurrentHashMap<Color, Color>()
    private val containerAlphaMap = ConcurrentHashMap<Color, Color>()
    private val resolvedMap = ConcurrentHashMap<String, ResolvedTagColors>()

    fun parseColor(hex: String, fallback: Color = Color(0xFF121212)): Color {
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

    fun resolvePillColors(hex: String, isDark: Boolean): ResolvedTagColors {
        val cacheKey = "${hex.trim().uppercase()}|$isDark"
        return resolvedMap.computeIfAbsent(cacheKey) {
            val spec = TagColorPalette.findSpec(hex)
            if (spec != null) {
                val tone = if (isDark) spec.dark else spec.light
                ResolvedTagColors(
                    unselectedContainer = parseColor(tone.containerHex),
                    unselectedContent = parseColor(tone.contentHex),
                    selectedContainer = parseColor(tone.selectedContainerHex),
                    selectedContent = parseColor(tone.selectedContentHex)
                )
            } else {
                // Graceful luminance-aware fallback for legacy or custom hexes
                val baseColor = parseColor(hex)
                val luminance = 0.299f * baseColor.red + 0.587f * baseColor.green + 0.114f * baseColor.blue
                val unselectedContent = if (isDark && luminance < 0.35f) {
                    lerp(baseColor, Color.White, 0.55f)
                } else {
                    baseColor
                }
                ResolvedTagColors(
                    unselectedContainer = getContainerColor(baseColor),
                    unselectedContent = unselectedContent,
                    selectedContainer = baseColor,
                    selectedContent = getContrastingColor(baseColor)
                )
            }
        }
    }
}

fun parseTagColor(hex: String, fallback: Color = Color(0xFF121212)): Color =
    TagColorCache.parseColor(hex, fallback)

fun getContrastingColor(background: Color): Color =
    TagColorCache.getContrastingColor(background)

/**
 * Borderless Material 3 Expressive Tonal Tag Pill displaying clean-string tag name without '#' prefix.
 * Uses uniform 28.dp height, 12.dp x 6.dp proportional padding, and natural text-relative width.
 * Renders as a full stadium pill when unselected/static and morphs to an 8.dp squircle when selected.
 */
@Composable
fun TonalTagPill(
    tag: Tag,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    maxNameLength: Int? = null,
    shape: Shape = if (isSelected) TagPillDefaults.SelectedShape else TagPillDefaults.UnselectedShape
) {
    val cleanName = tag.name.trim().removePrefix("#")
    val displayName = if (maxNameLength != null && cleanName.length > maxNameLength) {
        cleanName.take(maxNameLength - 1) + "…"
    } else {
        cleanName
    }

    val isDark = LocalIsDarkTheme.current
    val resolved = TagColorCache.resolvePillColors(tag.colorHex, isDark)
    val unselectedContainerColor = resolved.unselectedContainer
    val selectedContainerColor = resolved.selectedContainer
    val unselectedContentColor = resolved.unselectedContent
    val selectedContentColor = resolved.selectedContent

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
        modifier = modifier.height(TagPillDefaults.Height)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TagPillDefaults.HorizontalPadding,
                vertical = TagPillDefaults.VerticalPadding
            ),
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
 * Interactive borderless filter/suggestion pill that smoothly morphs between a full stadium pill
 * (unselected) and a crisp 8.dp squircle (selected) purely in the draw phase with zero layout churn.
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
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable (Color) -> Unit)? = null
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
            .height(TagPillDefaults.Height)
            .semantics {
                role = Role.Checkbox
                selected = isSelected
            }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val cornerRadius = TagPillDefaults.cornerRadiusPx(
                    heightPx = size.height,
                    squircleRadiusPx = TagPillDefaults.SelectedSquircleRadius.toPx(),
                    selectionProgress = selectionProgress
                )
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
            .padding(
                horizontal = TagPillDefaults.HorizontalPadding,
                vertical = TagPillDefaults.VerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon(contentColor)
        }
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

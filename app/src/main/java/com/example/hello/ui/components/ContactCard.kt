package com.example.hello.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.settings.TagColorPalette

/**
 * Material 3 Expressive connected Contact Card matching Settings styling (ADR-0008):
 * - Surface color: surfaceContainer with 0dp elevation and zero borders
 * - When isSelected: decoupled to RoundedCornerShape(24.dp) on all four corners,
 *   MaterialTheme.colorScheme.errorContainer background, inline checkmark icon next to bold name.
 * - 56dp circular avatar container
 * - 3-tier hierarchy:
 *     1. Header: Name + Date tonal pill badge (surfaceContainerHigh) + subtle chevron
 *     2. Body: Contextual Notes preview (adaptive collapse if blank)
 *     3. Footer: Tag pills (#tag with color dot)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactCard(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val cardShape = if (isSelected) RoundedCornerShape(24.dp) else shape
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. 56dp Circular Avatar
            DoodleAvatar(
                initials = contact.avatarInitials,
                bgIndex = contact.avatarBgIndex,
                size = 56.dp,
                shape = CircleShape
            )

            // 2. Structured Information Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tier 1: Bold Name + Subtle Chevron + Checkmark when selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = if (isSelected) {
                            secondaryContentColor.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tier 2: Role / Context Description (Strict 1 line, Adaptive collapse if blank)
                val contextText = contact.contextSubtitle.ifBlank { contact.notes }
                if (contextText.isNotBlank()) {
                    Text(
                        text = contextText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tier 3: Unified Tonal Tag Pills (single-row guarantee)
                val visibleTags = contact.tags.take(2)
                val extraTagsCount = (contact.tags.size - visibleTags.size).coerceAtLeast(0)

                if (visibleTags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        visibleTags.forEach { tag ->
                            TonalTagPill(
                                tag = tag,
                                maxNameLength = 10,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (extraTagsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ) {
                                Text(
                                    text = "+$extraTagsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact minimalist tonal pill displaying clean tag name.
 */
@Composable
fun ContactTagPill(
    tag: Tag,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    TonalTagPill(
        tag = tag,
        modifier = modifier,
        maxNameLength = 10
    )
}

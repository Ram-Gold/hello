package com.example.hello.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import com.example.hello.ui.home.components.CircleBadge
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.settings.TagColorPalette

/**
 * Material 3 Expressive connected Contact Card matching Settings styling (ADR-0008):
 * - Surface color: surfaceContainer with 0dp elevation and zero borders
 * - 56dp circular avatar container
 * - 3-tier hierarchy:
 *     1. Header: Name + Date tonal pill badge (surfaceContainerHigh) + subtle chevron
 *     2. Body: Contextual Notes preview (adaptive collapse if blank)
 *     3. Footer: Tag pills (#tag with color dot) + Circle chips (adaptive collapse if empty)
 */
@Composable
fun ContactCard(
    contact: ContactSummary,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
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
                // Tier 1: Name + Date Recency Pill + Subtle Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val recencyText = contact.lastEncounterText
                    if (!recencyText.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = recencyText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tier 2: Role / Context Description (Strict 1 line, Adaptive collapse if blank)
                val contextText = contact.contextSubtitle.ifBlank { contact.notes }
                if (contextText.isNotBlank()) {
                    Text(
                        text = contextText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tier 3: Tags & Circles (Single-row guarantee, Adaptive collapse if empty)
                val primaryCircle = contact.circles.firstOrNull()
                val primaryTag = contact.tags.firstOrNull()
                val extraTagsCount = (contact.tags.size - 1).coerceAtLeast(0)

                if (primaryCircle != null || primaryTag != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (primaryCircle != null) {
                            CircleBadge(circle = primaryCircle)
                        }

                        if (primaryTag != null) {
                            ContactTagPill(tag = primaryTag)
                        }

                        if (extraTagsCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "+$extraTagsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * Compact hashtag pill displaying tag name (truncated at 10 chars) and colored circular dot swatch.
 */
@Composable
fun ContactTagPill(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    val tagColor = TagColorPalette.parseColor(tag.colorHex)
    val displayName = if (tag.name.length > 10) {
        tag.name.take(9) + "…"
    } else {
        tag.name
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )
            Text(
                text = "#$displayName",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

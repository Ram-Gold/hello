package com.example.hello.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Tag
import com.example.hello.ui.detail.CadenceHealth
import com.example.hello.ui.detail.CadenceStatusInfo
import com.example.hello.ui.theme.AvatarSage
import com.example.hello.ui.theme.MutedPeachSecondary
import com.example.hello.ui.theme.WarmSageTertiary
import com.example.hello.ui.theme.WarmTerracottaPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertiesPanel(
    contact: Contact?,
    cadenceStatus: CadenceStatusInfo,
    allTags: List<Tag>,
    modifier: Modifier = Modifier
) {
    val circles = contact?.circles ?: emptyList()
    val tagIds = contact?.tagIds ?: emptyList()
    val matchingTags = allTags.filter { it.id in tagIds }
    val notes = contact?.notes.orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cadence Health Status Card
        CadenceHealthCard(cadenceStatus = cadenceStatus)

        // Circles & Tags Surface Card
        if (circles.isNotEmpty() || matchingTags.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circles Row
                    if (circles.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CIRCLES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                circles.forEach { circle ->
                                    CirclePillChip(circle = circle)
                                }
                            }
                        }
                    }

                    // Tags Row
                    if (matchingTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TAGS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                matchingTags.forEach { tag ->
                                    TagPillChip(tag = tag)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bio / Notes Card
        if (notes.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "NOTES & CONTEXT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CadenceHealthCard(cadenceStatus: CadenceStatusInfo) {
    val (statusColor, _, icon) = when (cadenceStatus.health) {
        CadenceHealth.ON_TRACK -> Triple(
            WarmSageTertiary,
            AvatarSage.copy(alpha = 0.35f),
            Icons.Default.CheckCircle
        )
        CadenceHealth.DUE_SOON -> Triple(
            MutedPeachSecondary,
            MutedPeachSecondary.copy(alpha = 0.15f),
            Icons.Default.AccessTime
        )
        CadenceHealth.OVERDUE -> Triple(
            WarmTerracottaPrimary,
            WarmTerracottaPrimary.copy(alpha = 0.15f),
            Icons.Default.Warning
        )
        CadenceHealth.NO_CADENCE -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Default.Info
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cadenceStatus.displayText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cadenceStatus.subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CirclePillChip(circle: Circle) {
    val emoji = when (circle) {
        Circle.WORK -> "💼"
        Circle.FRIENDS -> "☕"
        Circle.FAMILY -> "🏡"
        Circle.UNI -> "🎓"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = circle.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TagPillChip(tag: Tag) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(tag.colorHex))
    } catch (_: Exception) {
        WarmTerracottaPrimary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = parsedColor.copy(alpha = 0.14f)
    ) {
        Text(
            text = "#${tag.name}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = parsedColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

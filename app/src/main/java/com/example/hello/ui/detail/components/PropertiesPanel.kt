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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
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
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.domain.model.Tag
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.FeatherIconSize

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertiesPanel(
    contact: Contact?,
    allTags: List<Tag>,
    modifier: Modifier = Modifier
) {
    val tagIds = contact?.tagIds ?: emptyList()
    val matchingTags = allTags.filter { it.id in tagIds }
    val notes = contact?.notes.orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Extended Properties Surface Card (ADR-0012)
        val hasExtendedProps = !contact?.role.isNullOrBlank() ||
            !contact?.company.isNullOrBlank() ||
            contact?.birthday != null ||
            !contact?.phone.isNullOrBlank() ||
            !contact?.email.isNullOrBlank() ||
            !contact?.customProperties.isNullOrEmpty()

        if (hasExtendedProps) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PROPERTIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!contact?.role.isNullOrBlank() || !contact?.company.isNullOrBlank()) {
                            val roleCompany = listOfNotNull(contact?.role?.ifBlank { null }, contact?.company?.ifBlank { null }).joinToString(" at ")
                            PropertyChip(iconKey = HelloIconKey.BRIEFCASE, label = roleCompany)
                        }
                        if (contact?.birthday != null) {
                            val displayBday = com.example.hello.ui.components.BirthdayInputUtils.formatDisplayBirthday(contact.birthday)
                            PropertyChip(iconKey = HelloIconKey.CAKE, label = displayBday)
                        }
                        if (!contact?.phone.isNullOrBlank()) {
                            PropertyChip(iconKey = HelloIconKey.PHONE, label = "${contact.phone}")
                        }
                        if (!contact?.email.isNullOrBlank()) {
                            PropertyChip(iconKey = HelloIconKey.MAIL, label = "${contact.email}")
                        }
                        contact?.customProperties?.forEach { (key, value) ->
                            if (value.isNotBlank()) {
                                PropertyChip(iconKey = HelloIconKey.SPARKLES, label = "$key: $value")
                            }
                        }
                    }
                }
            }
        }

        // Context Tags Surface Card
        if (matchingTags.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CONTEXT #TAGS",
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
                                TonalTagPill(tag = tag)
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
private fun PropertyChip(iconKey: HelloIconKey, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CustomizableIcon(
                iconKey = iconKey,
                size = 14.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

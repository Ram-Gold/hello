package com.example.hello.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.home.components.DoodleAvatar

/**
 * Material 3 Expressive Contact Profile Header (ADR-0010):
 * - Displays 88dp circular/squircle avatar, prominent full name, and alias pills
 * - Minimalist relationship focus: communication actions (Call, Message, Email) removed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailCollapsingHeader(
    contact: Contact?,
    summary: ContactSummary?,
    modifier: Modifier = Modifier
) {
    val name = contact?.name ?: summary?.name ?: "Contact"
    val aliases = contact?.aliases ?: emptyList()
    val initials = summary?.avatarInitials ?: name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "👋" }
    val bgIndex = summary?.avatarBgIndex ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Avatar
        DoodleAvatar(
            initials = initials,
            bgIndex = bgIndex,
            size = 88.dp,
            shape = RoundedCornerShape(26.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Full Name
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Nicknames / Aliases
        if (aliases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                aliases.forEach { alias ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alias,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

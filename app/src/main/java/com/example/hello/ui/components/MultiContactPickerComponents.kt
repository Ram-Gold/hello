package com.example.hello.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.quickaction.components.MinimalFilledTextField

/**
 * Horizontal borderless ADR-0038 participant chip strip used in both "Met Again"
 * and Reminder sheets. Long-pressing any selected contact chip or tapping "+ Add"
 * opens [MultiContactPickerDialog].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactParticipantsStrip(
    selectedContacts: List<ContactSummary>,
    onRemoveContact: (String) -> Unit,
    onOpenMultiPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHelloHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        selectedContacts.forEach { contact ->
            Row(
                modifier = Modifier
                    .height(TagPillDefaults.Height)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.perform(SemanticHapticType.EDIT_ENTRY)
                            onOpenMultiPicker()
                        }
                    )
                    .padding(
                        start = TagPillDefaults.HorizontalPadding,
                        end = if (selectedContacts.size > 1) 8.dp else TagPillDefaults.HorizontalPadding,
                        top = TagPillDefaults.VerticalPadding,
                        bottom = TagPillDefaults.VerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedContacts.size > 1) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove ${contact.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onRemoveContact(contact.id)
                            }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .height(TagPillDefaults.Height)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                    onOpenMultiPicker()
                }
                .padding(
                    horizontal = TagPillDefaults.HorizontalPadding,
                    vertical = TagPillDefaults.VerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Searchable multi-select contact picker dialog requiring at least 1 selected Contact.
 */
@Composable
fun MultiContactPickerDialog(
    allContacts: List<ContactSummary>,
    initiallySelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<ContactSummary>) -> Unit,
    title: String = "Select people"
) {
    val haptics = LocalHelloHaptics.current
    var query by remember { mutableStateOf("") }
    var selectedIds by remember(initiallySelectedIds) { mutableStateOf(initiallySelectedIds) }

    val activeContacts = remember(allContacts) {
        allContacts.filterNot { it.isArchived }
    }
    val filtered = remember(activeContacts, query) {
        val clean = query.trim()
        if (clean.isBlank()) activeContacts
        else activeContacts.filter {
            it.name.contains(clean, ignoreCase = true) ||
                it.contextSubtitle.contains(clean, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MinimalFilledTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search name...",
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { contact ->
                        val isSelected = contact.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                                )
                                .clickable {
                                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                    selectedIds = if (isSelected) {
                                        selectedIds - contact.id
                                    } else {
                                        selectedIds + contact.id
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SelectableAvatarBadge(
                                isSelected = isSelected,
                                initials = contact.avatarInitials,
                                bgIndex = contact.avatarBgIndex,
                                size = 34.dp,
                                photoUri = contact.photoUri
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (contact.contextSubtitle.isNotBlank()) {
                                    Text(
                                        text = contact.contextSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val selectedList = activeContacts.filter { it.id in selectedIds }
            Button(
                enabled = selectedList.isNotEmpty(),
                onClick = { onConfirm(selectedList) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done (${selectedList.size})")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

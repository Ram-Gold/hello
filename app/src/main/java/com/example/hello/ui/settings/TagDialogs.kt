package com.example.hello.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Tag

object TagColorPalette {
    val PALETTE_HEX = listOf(
        "#E06D3B", // Terracotta
        "#F3A261", // Amber
        "#7DAA82", // Sage
        "#D87D7D", // Dusty Rose
        "#6B8BA4", // Slate
        "#D4A373", // Ochre
        "#8A9A5B", // Olive
        "#B07D62"  // Clay
    )

    fun parseColor(hex: String, fallback: Color = Color(0xFFE06D3B)): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val fullHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
            Color(fullHex.toLong(16))
        } catch (_: Exception) {
            fallback
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(TagColorPalette.PALETTE_HEX.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Tag",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. coffee, mentor, bookclub") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Palette Tone:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagColorPalette.PALETTE_HEX.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TagColorPalette.parseColor(hex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tagName, selectedColor) },
                enabled = tagName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit,
    onDelete: () -> Unit
) {
    var tagName by remember { mutableStateOf(tag.name) }
    var selectedColor by remember { mutableStateOf(tag.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit #${tag.name}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Palette Tone:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagColorPalette.PALETTE_HEX.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TagColorPalette.parseColor(hex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tag.copy(name = tagName.trim().removePrefix("#"), colorHex = selectedColor)) },
                enabled = tagName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun MergeTagDialog(
    sourceTag: Tag,
    availableTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirmMerge: (targetId: String) -> Unit
) {
    var selectedTargetId by remember { mutableStateOf(availableTags.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Merge #${sourceTag.name}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Merge #${sourceTag.name} (${sourceTag.useCount} contacts) into another tag. All contacts with #${sourceTag.name} will be migrated, and #${sourceTag.name} will be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (availableTags.isEmpty()) {
                    Text(
                        text = "No other tags available to merge into.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableTags.forEach { target ->
                            val isSelected = selectedTargetId == target.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTargetId = target.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(TagColorPalette.parseColor(target.colorHex))
                                    )
                                    Text(
                                        text = "#${target.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${target.useCount} contacts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedTargetId?.let(onConfirmMerge) },
                enabled = selectedTargetId != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

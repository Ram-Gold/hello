package com.example.hello.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Tag
import com.example.hello.ui.components.TagColorCache
import com.example.hello.ui.components.TagPillDefaults
import com.example.hello.ui.theme.LocalIsDarkTheme

object TagColorPalette {
    val PALETTE_HEX = com.example.hello.domain.model.TagColorPalette.PALETTE_HEX

    fun parseColor(hex: String, fallback: Color = Color(0xFF121212)): Color =
        TagColorCache.parseColor(hex, fallback)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagColorSwatchGrid(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TagColorPalette.PALETTE_HEX.forEach { hex ->
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            val resolved = TagColorCache.resolvePillColors(hex, isDark)
            val selectionProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "swatchSelectionMorph"
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind {
                        val circleRadius = size.height / 2f
                        val squircleRadius = TagPillDefaults.SelectedSquircleRadius.toPx()
                        val cornerRadius = circleRadius + (squircleRadius - circleRadius) * selectionProgress
                        val bgColor = lerp(resolved.unselectedContainer, resolved.selectedContainer, selectionProgress)
                        drawRoundRect(
                            color = bgColor,
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                    .clickable { onSelectHex(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = resolved.selectedContent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
    var userSelectedColor by remember { mutableStateOf<String?>(null) }
    val effectiveColor = userSelectedColor ?: com.example.hello.domain.model.TagColorPalette.getDeterministicColorHex(tagName)

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

                TagColorSwatchGrid(
                    selectedHex = effectiveColor,
                    onSelectHex = { userSelectedColor = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tagName.trim().removePrefix("#"), effectiveColor) },
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
                text = "Edit ${tag.name}",
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

                TagColorSwatchGrid(
                    selectedHex = selectedColor,
                    onSelectHex = { selectedColor = it }
                )
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
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(16.dp))
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
                text = "Merge ${sourceTag.name}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Merge ${sourceTag.name} (${sourceTag.useCount} contacts) into another tag. All contacts with ${sourceTag.name} will be migrated, and ${sourceTag.name} will be deleted.",
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
                                    val dotColors = TagColorCache.resolvePillColors(target.colorHex, LocalIsDarkTheme.current)
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(dotColors.unselectedContainer)
                                    )
                                    Text(
                                        text = target.name,
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

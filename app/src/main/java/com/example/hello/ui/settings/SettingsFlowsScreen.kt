package com.example.hello.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset

@Composable
fun SettingsFlowsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddComponentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(SettingsUiEvent.OnDismissUserMessage)
        }
    }

    val editingPreset = uiState.selectedEditingPreset

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PixelStyleTopBar(
                title = "Capture Flows",
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Active Preset Selection
            item {
                SettingsSubsectionHeader(title = "ACTIVE FLOW PRESET")
            }

            itemsIndexed(uiState.flowPresets) { index, preset ->
                val isActive = preset.id == uiState.activeFlowPresetId
                val isEditing = preset.id == uiState.selectedEditingPresetId

                Surface(
                    shape = shapeFor(index, uiState.flowPresets.size),
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(SettingsUiEvent.OnSelectActiveFlowPreset(preset.id))
                            onEvent(SettingsUiEvent.OnSelectPresetToEdit(preset.id))
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isActive,
                            onClick = {
                                onEvent(SettingsUiEvent.OnSelectActiveFlowPreset(preset.id))
                                onEvent(SettingsUiEvent.OnSelectPresetToEdit(preset.id))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (preset.isSystemPreset) {
                                    Text(
                                        text = "Built-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isEditing) {
                            OutlinedButton(
                                onClick = { onEvent(SettingsUiEvent.OnSelectPresetToEdit(preset.id)) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Edit Steps", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Create New Preset Action
            item {
                OutlinedButton(
                    onClick = { onEvent(SettingsUiEvent.OnOpenCreatePresetDialog) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New Flow Preset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Flow Preset")
                }
            }

            // Section 2: Edit Components of Selected Preset
            if (editingPreset != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSubsectionHeader(title = "EDITING: ${editingPreset.name.uppercase()}")
                }

                itemsIndexed(editingPreset.components) { compIndex, component ->
                    val isFirst = compIndex == 0
                    val isLast = compIndex == editingPreset.components.size - 1

                    Surface(
                        shape = shapeFor(compIndex, editingPreset.components.size),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Step order badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${compIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Component info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = component.type.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = component.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Up / Down Reorder Arrows
                            FilledIconButton(
                                onClick = { onEvent(SettingsUiEvent.OnMoveComponentUp(editingPreset.id, component.id)) },
                                enabled = !isFirst,
                                modifier = Modifier.size(34.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move Up",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = { onEvent(SettingsUiEvent.OnMoveComponentDown(editingPreset.id, component.id)) },
                                enabled = !isLast,
                                modifier = Modifier.size(34.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move Down",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Switch to enable/disable (disabled for NAME)
                            Switch(
                                checked = component.isEnabled,
                                onCheckedChange = { checked ->
                                    if (!component.isRequired) {
                                        onEvent(SettingsUiEvent.OnToggleComponentEnabled(editingPreset.id, component.id, checked))
                                    }
                                },
                                enabled = !component.isRequired,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // Preset Management Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddComponentDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Step")
                        }

                        if (editingPreset.isSystemPreset) {
                            OutlinedButton(
                                onClick = { onEvent(SettingsUiEvent.OnResetPresetToDefault(editingPreset.id)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Steps")
                            }
                        } else {
                            Button(
                                onClick = { onEvent(SettingsUiEvent.OnDeleteCustomPreset(editingPreset.id)) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Flow")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create New Flow Preset
    if (uiState.isCreatePresetDialogOpen) {
        var presetName by remember { mutableStateOf("") }
        var presetDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.OnCloseCreatePresetDialog) },
            title = { Text("New Capture Flow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Flow Name") },
                        placeholder = { Text("e.g. Meetup / Startup Club") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = presetDesc,
                        onValueChange = { presetDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g. Quick contact details for club meetups") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onEvent(SettingsUiEvent.OnCreateCustomPreset(presetName, presetDesc))
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnCloseCreatePresetDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Step Component
    if (showAddComponentDialog && editingPreset != null) {
        val existingTypes = editingPreset.components.map { it.type }.toSet()
        val availableTypes = CaptureComponentType.entries.filter { it == CaptureComponentType.CUSTOM || it !in existingTypes }

        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text("Add Step to ${editingPreset.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTypes.forEach { type ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEvent(SettingsUiEvent.OnAddComponentToPreset(editingPreset.id, type, type.defaultLabel))
                                    showAddComponentDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = type.defaultLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddComponentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

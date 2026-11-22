package com.example.hello.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import com.example.hello.ui.components.HelloToastDefaults
import com.example.hello.ui.components.HelloToastHost

@Composable
fun SettingsFlowsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var pendingDeletePreset by remember { mutableStateOf<FlowPreset?>(null) }

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
        snackbarHost = {
            HelloToastHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + HelloToastDefaults.BOTTOM_MARGIN_DP)
            )
        },
        topBar = {
            PixelStyleTopBar(
                title = "Capture Flows",
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                ),
                actions = {
                    FilledIconButton(
                        onClick = { onEvent(SettingsUiEvent.OnOpenCreatePresetDialog) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Flow Preset",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section 1: Active Preset Selection
            item {
                SettingsSubsection(title = "Active Flow Preset") {
                    val presets = uiState.flowPresets
                    presets.forEachIndexed { index, preset ->
                        val isActive = preset.id == uiState.activeFlowPresetId
                        SelectableSettingItem(
                            title = preset.name,
                            subtitle = preset.description,
                            selected = isActive,
                            onClick = {
                                onEvent(SettingsUiEvent.OnSelectActiveFlowPreset(preset.id))
                            },
                            shape = shapeFor(index, presets.size)
                        )
                    }
                }
            }

            // Section 2: Edit Components of Selected Preset
            if (editingPreset != null) {
                item {
                    SettingsSubsection(title = "${editingPreset.name} Steps") {
                        val components = editingPreset.components
                        components.forEachIndexed { compIndex, component ->
                            val isFirst = compIndex == 0
                            val isLast = compIndex == components.size - 1
                            val isActiveStep = component.isEnabled

                            Surface(
                                shape = shapeFor(compIndex, components.size),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 68.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${compIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(20.dp)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = component.type.displayTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isActiveStep) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            }
                                        )
                                        Text(
                                            text = component.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isActiveStep) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            }
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                onEvent(SettingsUiEvent.OnMoveComponentUp(editingPreset.id, component.id))
                                            },
                                            enabled = !isFirst,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (!isFirst) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                onEvent(SettingsUiEvent.OnMoveComponentDown(editingPreset.id, component.id))
                                            },
                                            enabled = !isLast,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (!isLast) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    ExpressiveSettingsSwitch(
                                        checked = component.isEnabled,
                                        onCheckedChange = { checked ->
                                            if (!component.isRequired) {
                                                onEvent(
                                                    SettingsUiEvent.OnToggleComponentEnabled(
                                                        editingPreset.id,
                                                        component.id,
                                                        checked
                                                    )
                                                )
                                            }
                                        },
                                        enabled = !component.isRequired
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Flow Actions
                item {
                    SettingsSubsection(title = "Flow Actions") {
                        SettingsItem(
                            title = "Add Step",
                            subtitle = "Add another field to this capture flow",
                            shape = shapeFor(0, 2),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = { showAddComponentDialog = true }
                        )

                        if (editingPreset.isSystemPreset) {
                            SettingsItem(
                                title = "Reset Steps",
                                subtitle = "Restore default step order and fields",
                                shape = shapeFor(1, 2),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    onEvent(SettingsUiEvent.OnResetPresetToDefault(editingPreset.id))
                                }
                            )
                        } else {
                            SettingsItem(
                                title = "Delete Flow",
                                subtitle = "Remove this custom capture flow",
                                shape = shapeFor(1, 2),
                                titleColor = MaterialTheme.colorScheme.error,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { pendingDeletePreset = editingPreset }
                            )
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
                TextButton(
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

    // Dialog: Confirm Delete Custom Flow Preset
    pendingDeletePreset?.let { presetToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeletePreset = null },
            title = { Text("Delete ${presetToDelete.name}?") },
            text = {
                Text("This custom capture flow will be removed and your active flow will switch to Default.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(SettingsUiEvent.OnDeleteCustomPreset(presetToDelete.id))
                        pendingDeletePreset = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePreset = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Step Component
    if (showAddComponentDialog && editingPreset != null) {
        val existingTypes = editingPreset.components.map { it.type }.toSet()
        val availableTypes = CaptureComponentType.entries.filter {
            it != CaptureComponentType.CADENCE && (it == CaptureComponentType.CUSTOM || it !in existingTypes)
        }

        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text("Add Step to ${editingPreset.name}") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    availableTypes.forEachIndexed { index, type ->
                        Surface(
                            onClick = {
                                onEvent(SettingsUiEvent.OnAddComponentToPreset(editingPreset.id, type, type.defaultLabel))
                                showAddComponentDialog = false
                            },
                            shape = shapeFor(index, availableTypes.size),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = type.displayTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = type.defaultLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
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

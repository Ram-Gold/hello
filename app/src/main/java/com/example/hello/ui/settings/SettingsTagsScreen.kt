package com.example.hello.ui.settings

import com.example.hello.R
import com.example.hello.ui.components.EmptyStateIllustration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.ui.components.HelloToastDefaults
import com.example.hello.ui.components.HelloToastHost
import com.example.hello.ui.components.TOAST_DURATION_ACTIONABLE_MS
import com.example.hello.ui.components.TOAST_DURATION_INFORMATIONAL_MS
import com.example.hello.ui.components.TagColorCache
import com.example.hello.ui.components.showHelloToast
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.theme.LocalIsDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTagsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHelloHaptics.current
    val toastScope = rememberCoroutineScope()
    val activeToastJob = remember { arrayOfNulls<Job>(1) }
    var showTaxonomyInfoDialog by remember { mutableStateOf(false) }
    var showTerminologySheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        onEvent(SettingsUiEvent.OnDismissUserMessage)
        activeToastJob[0]?.cancel()
        activeToastJob[0] = toastScope.launch {
            val hasUndo = msg.startsWith("Deleted tag", ignoreCase = true)
            val result = if (hasUndo) {
                snackbarHostState.showHelloToast(
                    message = msg,
                    actionLabel = "Undo",
                    durationMs = TOAST_DURATION_ACTIONABLE_MS
                )
            } else {
                snackbarHostState.showHelloToast(
                    message = msg,
                    durationMs = TOAST_DURATION_INFORMATIONAL_MS
                )
            }
            if (hasUndo && result == SnackbarResult.ActionPerformed) {
                haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                onEvent(SettingsUiEvent.OnUndoDeleteTag)
            }
        }
    }

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
                title = "Tag Taxonomy",
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                ),
                actions = {
                    IconButton(onClick = { showTaxonomyInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "About Tag Taxonomy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledIconButton(
                        onClick = { onEvent(SettingsUiEvent.OnOpenAddTagDialog) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Tag",
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
            item {
                SettingsItem(
                    title = "Display Terminology",
                    subtitle = "Directory tab label (${uiState.terminology.pluralTitle})",
                    shape = shapeFor(0, 1),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = uiState.terminology.pluralTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { showTerminologySheet = true }
                )
            }

            if (uiState.tags.isEmpty()) {
                item {
                    SettingsSubsection(title = "Tags") {
                        Surface(
                            shape = shapeFor(0, 1),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EmptyStateIllustration(
                                    drawableRes = R.drawable.ic_empty_tags,
                                    height = 116.dp
                                )
                                Text("No tags yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    "Tap '+' to create your first tag.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    SettingsSubsection(title = "Active Labels (${uiState.tags.size})") {
                        val tags = uiState.tags
                        tags.forEachIndexed { index, tag ->
                            Surface(
                                onClick = { onEvent(SettingsUiEvent.OnOpenEditTagDialog(tag)) },
                                shape = shapeFor(index, tags.size),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    val dotColors = TagColorCache.resolvePillColors(tag.colorHex, LocalIsDarkTheme.current)
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(dotColors.unselectedContainer)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tag.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${tag.useCount} contacts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { onEvent(SettingsUiEvent.OnOpenMergeTagDialog(tag)) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.CallMerge,
                                                contentDescription = "Merge Tag",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onEvent(SettingsUiEvent.OnOpenEditTagDialog(tag)) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit Tag",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddTagDialogOpen) {
        AddTagDialog(
            onDismiss = { onEvent(SettingsUiEvent.OnCloseTagDialogs) },
            onConfirm = { name, colorHex ->
                onEvent(SettingsUiEvent.OnCreateTag(name, colorHex))
            }
        )
    }

    uiState.editingTag?.let { tag ->
        EditTagDialog(
            tag = tag,
            onDismiss = { onEvent(SettingsUiEvent.OnCloseTagDialogs) },
            onSave = { updatedTag -> onEvent(SettingsUiEvent.OnUpdateTag(updatedTag)) },
            onDelete = { onEvent(SettingsUiEvent.OnDeleteTag(tag.id)) }
        )
    }

    uiState.mergingSourceTag?.let { sourceTag ->
        MergeTagDialog(
            sourceTag = sourceTag,
            availableTags = uiState.tags.filterNot { it.id == sourceTag.id },
            onDismiss = { onEvent(SettingsUiEvent.OnCloseTagDialogs) },
            onConfirmMerge = { targetId ->
                onEvent(SettingsUiEvent.OnConfirmMergeTag(sourceTag.id, targetId))
            }
        )
    }

    if (showTerminologySheet) {
        DisplayTerminologyModalSheet(
            selectedTerminology = uiState.terminology,
            onSelectTerminology = { option ->
                onEvent(SettingsUiEvent.OnSelectTerminology(option))
                showTerminologySheet = false
            },
            onDismiss = { showTerminologySheet = false }
        )
    }

    if (showTaxonomyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTaxonomyInfoDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Relationship Taxonomy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tags are lightweight freeform labels attached to Contacts. Edit names and palette tones, or merge duplicate tags without data loss.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Display Terminology sets your main tab name (${uiState.terminology.pluralTitle}), while tags organize how you know each person without overlapping labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaxonomyInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayTerminologyModalSheet(
    selectedTerminology: RelationshipTerminology,
    onSelectTerminology: (RelationshipTerminology) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Display Terminology",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sets your main directory tab name, while tags organize how you know each person.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsSubsection(title = "Directory Names (Recommended)") {
                val primaryEntries = RelationshipTerminology.primaryDirectoryOptions
                primaryEntries.forEachIndexed { index, option ->
                    val titleText = if (option == RelationshipTerminology.PEOPLE) {
                        "${option.pluralTitle} (Recommended)"
                    } else {
                        option.pluralTitle
                    }
                    SelectableSettingItem(
                        title = titleText,
                        subtitle = option.description,
                        selected = selectedTerminology == option,
                        onClick = { onSelectTerminology(option) },
                        shape = shapeFor(index, primaryEntries.size)
                    )
                }
            }

            SettingsSubsection(title = "Specialized Labels") {
                val secondaryEntries = RelationshipTerminology.entries.filterNot { it.isPrimaryDirectoryOption }
                secondaryEntries.forEachIndexed { index, option ->
                    SelectableSettingItem(
                        title = option.pluralTitle,
                        subtitle = option.description,
                        selected = selectedTerminology == option,
                        onClick = { onSelectTerminology(option) },
                        shape = shapeFor(index, secondaryEntries.size)
                    )
                }
            }
        }
    }
}

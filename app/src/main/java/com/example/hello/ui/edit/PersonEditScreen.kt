package com.example.hello.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Tag
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.WarmTerracottaPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonEditScreen(
    uiState: PersonEditUiState,
    onEvent: (PersonEditUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onLifecycleSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onEvent(PersonEditUiEvent.OnPhotoSelected(uri?.toString()))
    }

    // Intercept back key if unsaved modifications exist
    BackHandler(enabled = uiState.isModified) {
        onEvent(PersonEditUiEvent.OnBackClick)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaveSuccess()
        }
    }

    LaunchedEffect(uiState.isArchivedCompleted, uiState.isDeletedCompleted) {
        if (uiState.isArchivedCompleted || uiState.isDeletedCompleted) {
            onLifecycleSuccess()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(PersonEditUiEvent.OnDismissUserMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Contact",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isModified) {
                            onEvent(PersonEditUiEvent.OnBackClick)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onEvent(PersonEditUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && uiState.isModified,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Photo / Avatar Header with Camera Badge
            PhotoPickerSection(
                photoUri = uiState.photoUri,
                initials = uiState.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "👋" },
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = { onEvent(PersonEditUiEvent.OnRemovePhoto) }
            )

            // Full Name (Required)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onEvent(PersonEditUiEvent.OnNameChange(it)) },
                label = { Text("Full Name *") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = uiState.name.isBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Nicknames / Aliases Manager
            AliasesChipManager(
                aliases = uiState.aliases,
                aliasInput = uiState.aliasInput,
                onInputChange = { onEvent(PersonEditUiEvent.OnAliasInputChange(it)) },
                onAddAlias = { onEvent(PersonEditUiEvent.OnAddAlias(it)) },
                onRemoveAlias = { onEvent(PersonEditUiEvent.OnRemoveAlias(it)) }
            )

            // Phone Number
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { onEvent(PersonEditUiEvent.OnPhoneChange(it)) },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Email Address
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onEvent(PersonEditUiEvent.OnEmailChange(it)) },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Relationship Notes & Context
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onEvent(PersonEditUiEvent.OnNotesChange(it)) },
                label = { Text("Relationship Notes & Context") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Circles Multi-select
            CirclesSelector(
                selectedCircles = uiState.selectedCircles,
                onToggleCircle = { onEvent(PersonEditUiEvent.OnToggleCircle(it)) }
            )

            // Tags Manager
            TagsManager(
                availableTags = uiState.availableTags,
                selectedTagIds = uiState.selectedTagIds,
                onToggleTag = { onEvent(PersonEditUiEvent.OnToggleTag(it)) },
                onOpenNewTagDialog = { onEvent(PersonEditUiEvent.OnShowNewTagDialog(true)) }
            )

            // Cadence Interval Selector
            CadenceIntervalSelector(
                cadenceDays = uiState.cadenceDays,
                isCustomCadence = uiState.isCustomCadence,
                customInput = uiState.customCadenceInput,
                onSelectCadence = { onEvent(PersonEditUiEvent.OnSelectCadence(it)) },
                onCustomChange = { onEvent(PersonEditUiEvent.OnCustomCadenceChange(it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Lifecycle & Destructive Actions
            LifecycleActionsCard(
                onArchiveClick = { onEvent(PersonEditUiEvent.OnShowArchiveDialog) },
                onDeleteClick = { onEvent(PersonEditUiEvent.OnShowDeleteDialog) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Discard Changes Dialog
    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(PersonEditUiEvent.OnDismissDiscardDialog) },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved edits. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(PersonEditUiEvent.OnConfirmDiscard)
                    onNavigateBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PersonEditUiEvent.OnDismissDiscardDialog) }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Archive Dialog
    if (uiState.showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(PersonEditUiEvent.OnDismissArchiveDialog) },
            title = { Text("Archive ${uiState.name}?") },
            text = {
                Text("Cadence reminders for this contact will be paused, and they will be moved to your archived records.")
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(PersonEditUiEvent.OnConfirmArchive) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PersonEditUiEvent.OnDismissArchiveDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(PersonEditUiEvent.OnDismissDeleteDialog) },
            title = { Text("Permanently delete ${uiState.name}?") },
            text = {
                Text("This will permanently remove this contact and all recorded encounters from your device. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(PersonEditUiEvent.OnConfirmDelete) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PersonEditUiEvent.OnDismissDeleteDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Tag Dialog
    if (uiState.showNewTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onEvent(PersonEditUiEvent.OnShowNewTagDialog(false)) },
            title = { Text("Add New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onEvent(PersonEditUiEvent.OnCreateTag(newTagName.trim()))
                        }
                    },
                    enabled = newTagName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PersonEditUiEvent.OnShowNewTagDialog(false)) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PhotoPickerSection(
    photoUri: String?,
    initials: String,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center
        ) {
            DoodleAvatar(
                initials = initials,
                bgIndex = 0,
                size = 96.dp,
                shape = RoundedCornerShape(28.dp)
            )

            // Camera Icon Overlay Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Pick photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (photoUri != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onRemovePhoto) {
                Text(
                    text = "Reset to Doodle Avatar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to choose photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AliasesChipManager(
    aliases: List<String>,
    aliasInput: String,
    onInputChange: (String) -> Unit,
    onAddAlias: (String) -> Unit,
    onRemoveAlias: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "NICKNAMES & ALIASES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (aliases.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                aliases.forEach { alias ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = alias,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onRemoveAlias(alias) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove alias",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = aliasInput,
                onValueChange = onInputChange,
                placeholder = { Text("Add a nickname (e.g. S-Lin)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (aliasInput.isNotBlank()) onAddAlias(aliasInput)
                    }
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onAddAlias(aliasInput) },
                enabled = aliasInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CirclesSelector(
    selectedCircles: Set<Circle>,
    onToggleCircle: (Circle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Circle.entries.forEach { circle ->
                val isSelected = circle in selectedCircles
                val emoji = when (circle) {
                    Circle.WORK -> "💼 "
                    Circle.FRIENDS -> "☕ "
                    Circle.FAMILY -> "🏡 "
                    Circle.UNI -> "🎓 "
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCircle(circle) },
                    label = { Text("$emoji${circle.displayName}") },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsManager(
    availableTags: List<Tag>,
    selectedTagIds: Set<String>,
    onToggleTag: (String) -> Unit,
    onOpenNewTagDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TAGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onOpenNewTagDialog,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Tag", style = MaterialTheme.typography.labelMedium)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.forEach { tag ->
                val isSelected = tag.id in selectedTagIds
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleTag(tag.id) },
                    label = { Text("#${tag.name}") },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CadenceIntervalSelector(
    cadenceDays: Int?,
    isCustomCadence: Boolean,
    customInput: String,
    onSelectCadence: (Int?) -> Unit,
    onCustomChange: (String) -> Unit
) {
    val presets = listOf(
        7 to "Weekly (7d)",
        14 to "Bi-weekly (14d)",
        30 to "Monthly (30d)",
        90 to "Quarterly (90d)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "STAY-IN-TOUCH CADENCE",
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
            presets.forEach { (days, label) ->
                val isSelected = !isCustomCadence && cadenceDays == days
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCadence(days) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // None option
            FilterChip(
                selected = !isCustomCadence && cadenceDays == null,
                onClick = { onSelectCadence(null) },
                label = { Text("None") },
                shape = RoundedCornerShape(14.dp)
            )

            // Custom option
            FilterChip(
                selected = isCustomCadence,
                onClick = { onCustomChange(customInput.ifEmpty { "21" }) },
                label = { Text("Custom...") },
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (isCustomCadence) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = customInput,
                onValueChange = onCustomChange,
                label = { Text("Custom Cadence (days)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LifecycleActionsCard(
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "RELATIONSHIP LIFECYCLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Archive Button
            OutlinedButton(
                onClick = onArchiveClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Archive Contact")
            }

            // Delete Button
            Button(
                onClick = onDeleteClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Contact Permanently",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Person Edit Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PersonEditScreenPreview() {
    HelloTheme(darkTheme = false) {
        PersonEditScreen(
            uiState = PersonEditUiState(
                name = "Sarah Lin",
                aliases = listOf("S-Lin", "Sarah"),
                phone = "+1 (555) 234-5678",
                email = "sarah.lin@example.com",
                notes = "Co-founder & Staff Engineer.",
                selectedCircles = setOf(Circle.WORK),
                availableTags = MockDataProvider.tags,
                cadenceDays = 30
            ),
            onEvent = {},
            onNavigateBack = {},
            onSaveSuccess = {},
            onLifecycleSuccess = {}
        )
    }
}

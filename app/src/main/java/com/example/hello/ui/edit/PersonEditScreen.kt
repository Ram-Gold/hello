package com.example.hello.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Work
import java.time.LocalDate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import com.example.hello.ui.components.BirthdayInputUtils
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.hello.ui.components.ClearTextTrailingIcon
import com.example.hello.ui.components.HelloToastHost
import com.example.hello.ui.components.bringIntoViewOnFocus
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Tag
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.FeatherIconSize
import com.example.hello.ui.edit.components.AvatarCropDialog
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
    onLifecycleSuccess: (isArchived: Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val nameRequester = remember { BringIntoViewRequester() }
    val aliasRequester = remember { BringIntoViewRequester() }
    val roleRequester = remember { BringIntoViewRequester() }
    val companyRequester = remember { BringIntoViewRequester() }
    val phoneRequester = remember { BringIntoViewRequester() }
    val emailRequester = remember { BringIntoViewRequester() }
    val notesRequester = remember { BringIntoViewRequester() }
    val cadenceRequester = remember { BringIntoViewRequester() }
    val haptics = LocalHelloHaptics.current

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
            haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
            onSaveSuccess()
        }
    }

    LaunchedEffect(uiState.isArchivedCompleted, uiState.isDeletedCompleted) {
        if (uiState.isArchivedCompleted) {
            onLifecycleSuccess(true)
        } else if (uiState.isDeletedCompleted) {
            onLifecycleSuccess(false)
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
        snackbarHost = {
            HelloToastHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Photo / Avatar Header with Camera Badge
            PhotoPickerSection(
                photoUri = uiState.photoUri,
                initials = extractAlphanumericInitials(uiState.name),
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
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.name,
                        onClear = { onEvent(PersonEditUiEvent.OnNameChange("")) },
                        contentDescription = "Clear full name"
                    )
                },
                singleLine = true,
                isError = uiState.name.isBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, nameRequester)
            )

            // Nicknames / Aliases Manager
            AliasesChipManager(
                aliases = uiState.aliases,
                aliasInput = uiState.aliasInput,
                onInputChange = { onEvent(PersonEditUiEvent.OnAliasInputChange(it)) },
                onAddAlias = { onEvent(PersonEditUiEvent.OnAddAlias(it)) },
                onRemoveAlias = { onEvent(PersonEditUiEvent.OnRemoveAlias(it)) },
                coroutineScope = coroutineScope,
                bringIntoViewRequester = aliasRequester
            )

            // Role / Title
            OutlinedTextField(
                value = uiState.role,
                onValueChange = { onEvent(PersonEditUiEvent.OnRoleChange(it)) },
                label = { Text("Role / Title") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Work,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.role,
                        onClear = { onEvent(PersonEditUiEvent.OnRoleChange("")) },
                        contentDescription = "Clear role"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, roleRequester)
            )

            // Company / Organization
            OutlinedTextField(
                value = uiState.company,
                onValueChange = { onEvent(PersonEditUiEvent.OnCompanyChange(it)) },
                label = { Text("Company / Organization") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.company,
                        onClear = { onEvent(PersonEditUiEvent.OnCompanyChange("")) },
                        contentDescription = "Clear company"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, companyRequester)
            )

            // Birthday
            var showDatePicker by remember { mutableStateOf(false) }

            if (showDatePicker) {
                val initialMillis = uiState.birthday
                    ?.atStartOfDay(ZoneOffset.UTC)
                    ?.toInstant()
                    ?.toEpochMilli()
                    ?: System.currentTimeMillis()
                val selectableDates = remember {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            return utcTimeMillis <= System.currentTimeMillis()
                        }
                        override fun isSelectableYear(year: Int): Boolean {
                            return year <= LocalDate.now().year
                        }
                    }
                }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialMillis,
                    selectableDates = selectableDates
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val picked = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                    onEvent(PersonEditUiEvent.OnBirthdayChange(picked))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            val displayBirthday = BirthdayInputUtils.formatHumanReadable(uiState.birthday)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = displayBirthday,
                    onValueChange = {},
                    readOnly = true,
                    enabled = true,
                    label = { Text("Birthday") },
                    placeholder = { Text("Select birthday from calendar...") },
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Cake,
                                contentDescription = "Select birthday from date picker",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = {
                        ClearTextTrailingIcon(
                            value = displayBirthday,
                            onClear = { onEvent(PersonEditUiEvent.OnBirthdayChange(null)) },
                            contentDescription = "Clear birthday"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    if (interaction is PressInteraction.Release) {
                                        showDatePicker = true
                                    }
                                }
                            }
                        }
                )
            }

            // Phone Number
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { onEvent(PersonEditUiEvent.OnPhoneChange(it)) },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.phone,
                        onClear = { onEvent(PersonEditUiEvent.OnPhoneChange("")) },
                        contentDescription = "Clear phone number"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, phoneRequester)
            )

            // Email Address
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onEvent(PersonEditUiEvent.OnEmailChange(it)) },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.email,
                        onClear = { onEvent(PersonEditUiEvent.OnEmailChange("")) },
                        contentDescription = "Clear email address"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, emailRequester)
            )

            // Relationship Notes & Context
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onEvent(PersonEditUiEvent.OnNotesChange(it)) },
                label = { Text("Relationship Notes & Context") },
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = uiState.notes,
                        onClear = { onEvent(PersonEditUiEvent.OnNotesChange("")) },
                        contentDescription = "Clear notes"
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(coroutineScope, notesRequester)
            )

            // Tags Manager
            TagsManager(
                availableTags = uiState.availableTags,
                selectedTagIds = uiState.selectedTagIds,
                onToggleTag = { onEvent(PersonEditUiEvent.OnToggleTag(it)) },
                onOpenNewTagDialog = { onEvent(PersonEditUiEvent.OnShowNewTagDialog(true)) }
            )

            // Stay-in-touch Cadence
            CadenceIntervalSelector(
                cadenceDays = uiState.cadenceDays,
                isCustomCadence = uiState.isCustomCadence,
                customInput = uiState.customCadenceInput,
                onSelectCadence = { onEvent(PersonEditUiEvent.OnSelectCadence(it)) },
                onCustomChange = { onEvent(PersonEditUiEvent.OnCustomCadenceChange(it)) },
                coroutineScope = coroutineScope,
                bringIntoViewRequester = cadenceRequester
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Lifecycle & Destructive Actions
            LifecycleActionsCard(
                onArchiveClick = { onEvent(PersonEditUiEvent.OnShowArchiveDialog) },
                onDeleteClick = { onEvent(PersonEditUiEvent.OnShowDeleteDialog) }
            )

            Spacer(modifier = Modifier.height(80.dp))
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
                Text("Check-in suggestions for this contact will be paused, and they will be moved to your archived records.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
                        onEvent(PersonEditUiEvent.OnConfirmArchive)
                    },
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
                    onClick = {
                        haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
                        onEvent(PersonEditUiEvent.OnConfirmDelete)
                    },
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
                    trailingIcon = {
                        ClearTextTrailingIcon(
                            value = newTagName,
                            onClear = { newTagName = "" },
                            contentDescription = "Clear tag name"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newTagName.trim().removePrefix("#")
                        if (clean.isNotBlank()) {
                            onEvent(PersonEditUiEvent.OnCreateTag(clean))
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

    // Interactive 1:1 Circular Avatar Cropper
    uiState.pendingCropUri?.let { cropSourceUri ->
        AvatarCropDialog(
            sourceUri = cropSourceUri,
            onConfirmCrop = { croppedBytes ->
                onEvent(PersonEditUiEvent.OnPhotoCropped(croppedBytes))
            },
            onCancel = {
                onEvent(PersonEditUiEvent.OnCancelCrop)
            },
            onDecodeFailed = {
                onEvent(PersonEditUiEvent.OnPhotoDecodeFailed)
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
                shape = CircleShape,
                photoUri = photoUri
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
                    imageVector = Icons.Rounded.PhotoCamera,
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
    onRemoveAlias: (String) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "NICKNAMES",
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
                                    imageVector = Icons.Rounded.Close,
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

        OutlinedTextField(
            value = aliasInput,
            onValueChange = onInputChange,
            placeholder = { Text("Add a nickname (e.g. S-Lin)") },
            trailingIcon = {
                ClearTextTrailingIcon(
                    value = aliasInput,
                    onClear = { onInputChange("") },
                    contentDescription = "Clear nickname input"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (aliasInput.isNotBlank()) onAddAlias(aliasInput)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (coroutineScope != null && bringIntoViewRequester != null) {
                        Modifier.bringIntoViewOnFocus(coroutineScope, bringIntoViewRequester)
                    } else Modifier
                )
        )
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
                    imageVector = Icons.Rounded.Add,
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
                TonalTagPill(
                    tag = tag,
                    isSelected = isSelected,
                    onClick = { onToggleTag(tag.id) }
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
    onCustomChange: (String) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null
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
                trailingIcon = {
                    ClearTextTrailingIcon(
                        value = customInput,
                        onClear = { onCustomChange("") },
                        contentDescription = "Clear custom cadence"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (coroutineScope != null && bringIntoViewRequester != null) {
                            Modifier.bringIntoViewOnFocus(coroutineScope, bringIntoViewRequester)
                        } else Modifier
                    )
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
                text = "MANAGE CONTACT",
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
                    imageVector = Icons.Rounded.Archive,
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
                    imageVector = Icons.Rounded.Delete,
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

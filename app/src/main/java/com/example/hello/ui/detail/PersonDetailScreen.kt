package com.example.hello.ui.detail

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.core.app.ActivityCompat
import com.example.hello.platform.notification.FollowUpNotificationHelper
import com.example.hello.ui.components.HelloToastDefaults
import com.example.hello.ui.components.HelloToastHost
import com.example.hello.ui.components.NotificationExplainerDialog
import com.example.hello.ui.components.TOAST_DURATION_ACTIONABLE_MS
import com.example.hello.ui.components.TOAST_DURATION_INFORMATIONAL_MS
import com.example.hello.ui.components.showHelloToast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.detail.components.DetailCollapsingHeader
import com.example.hello.ui.detail.components.LivingTimeline
import com.example.hello.ui.detail.components.ProfileReminderOverlayPayload
import com.example.hello.ui.detail.components.ProfileReminderSheetOverlay
import com.example.hello.ui.detail.components.PropertiesPanel
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.predictiveSelectionTrayExit
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.timeline.components.TimelineSelectionActionTray
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    uiState: PersonDetailUiState,
    onEvent: (PersonDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onLogEncounterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    externalSnackbarHostState: SnackbarHostState? = null,
    onSelectionBackProgressChanged: (Float) -> Unit = {},
    onReminderOverlayChanged: ((ProfileReminderOverlayPayload?) -> Unit)? = null
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val localSnackbarHostState = remember { SnackbarHostState() }
    val snackbarHostState = externalSnackbarHostState ?: localSnackbarHostState
    val haptics = LocalHelloHaptics.current
    val isReducedMotion = LocalReducedMotion.current
    val context = LocalContext.current
    var selectionBackProgress by remember { mutableFloatStateOf(0f) }
    var showNotificationExplainer by remember { mutableStateOf(false) }
    var explainerOpensSettings by remember { mutableStateOf(false) }
    var hasPromptedNotificationPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasPromptedNotificationPermission = true
    }

    fun maybePromptNotificationPermission() {
        if (FollowUpNotificationHelper.isFullyDeliverable(context)) return
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !FollowUpNotificationHelper.hasNotificationPermission(context)
        if (needsRuntimePermission) {
            val activity = context.findActivity()
            val showRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            explainerOpensSettings = hasPromptedNotificationPermission && !showRationale
            showNotificationExplainer = true
        } else {
            explainerOpensSettings = true
            showNotificationExplainer = true
        }
    }

    ScrubbablePredictiveBackHandler(
        enabled = uiState.isSelectionMode,
        isReducedMotion = isReducedMotion,
        onProgressChanged = { progress ->
            selectionBackProgress = progress
            onSelectionBackProgressChanged(progress)
        },
        onCommit = {
            selectionBackProgress = 0f
            onSelectionBackProgressChanged(0f)
            onEvent(PersonDetailUiEvent.OnClearSelection)
        }
    )

    val toastScope = rememberCoroutineScope()
    val activeToastJob = remember { arrayOfNulls<Job>(1) }

    LaunchedEffect(uiState.userMessage, uiState.undoKind) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        val undoKind = uiState.undoKind
        onEvent(PersonDetailUiEvent.OnDismissUserMessage)
        activeToastJob[0]?.cancel()
        activeToastJob[0] = toastScope.launch {
            val hasUndo = undoKind != PersonDetailUndoKind.NONE || msg.startsWith("Deleted", ignoreCase = true)
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
            if (result == SnackbarResult.ActionPerformed) {
                haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                when (undoKind) {
                    PersonDetailUndoKind.REMINDER -> onEvent(PersonDetailUiEvent.OnUndoReminderMutation)
                    PersonDetailUndoKind.ENCOUNTER, PersonDetailUndoKind.NONE -> onEvent(PersonDetailUiEvent.OnUndoDeleteEncounter)
                }
            }
        }
    }

    val contactName = uiState.contact?.name ?: uiState.contactSummary?.name ?: "Contact"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedEncounterIds.size} selected",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { onEvent(PersonDetailUiEvent.OnClearSelection) },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel selection",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = contactName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(PersonDetailUiEvent.OnToggleFavorite)
                        }) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            haptics.perform(SemanticHapticType.EDIT_ENTRY)
                            onEditClick()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Contact",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        snackbarHost = {
            if (externalSnackbarHostState == null) {
                val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomClearance = if (uiState.isSelectionMode) {
                    navBarBottomPadding + HelloToastDefaults.BOTTOM_MARGIN_DP
                } else {
                    84.dp + navBarBottomPadding + HelloToastDefaults.BOTTOM_MARGIN_DP
                }
                HelloToastHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = bottomClearance)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "header") {
                        DetailCollapsingHeader(
                            contact = uiState.contact,
                            summary = uiState.contactSummary
                        )
                    }

                    item(key = "properties") {
                        PropertiesPanel(
                            contact = uiState.contact,
                            allTags = uiState.tagsList
                        )
                    }

                    item(key = "timeline") {
                        LivingTimeline(
                            encounters = uiState.encounters,
                            placesMap = uiState.placesMap,
                            onLogEncounterClick = onLogEncounterClick,
                            birthday = uiState.contact?.birthday,
                            selectedEncounterIds = uiState.selectedEncounterIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onEncounterClick = { onEvent(PersonDetailUiEvent.OnToggleSelectEncounter(it)) },
                            onEncounterLongClick = {
                                haptics.perform(SemanticHapticType.EDIT_ENTRY)
                                onEvent(PersonDetailUiEvent.OnEncounterLongClick(it))
                            },
                            contactName = contactName,
                            reminders = uiState.reminders,
                            onReminderClick = { followUp ->
                                onEvent(PersonDetailUiEvent.OnOpenEditReminderSheet(followUp))
                            },
                            onMarkReminderDone = { id ->
                                haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                                onEvent(PersonDetailUiEvent.OnMarkReminderDone(id))
                            },
                            onSnoozeReminder = { id ->
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(PersonDetailUiEvent.OnSnoozeReminder(id))
                            }
                        )
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                // Floating Action Tray on the right edge
                AnimatedVisibility(
                    visible = uiState.isSelectionMode,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .predictiveSelectionTrayExit { selectionBackProgress }
                ) {
                    TimelineSelectionActionTray(
                        onDeleteClick = { onEvent(PersonDetailUiEvent.OnRequestDeleteSelected) },
                        onSelectAllClick = { onEvent(PersonDetailUiEvent.OnSelectAllEncounters) }
                    )
                }

                // Batch Deletion Confirmation Dialog
                if (uiState.isDeleteConfirmationVisible) {
                    val count = uiState.selectedEncounterIds.size
                    AlertDialog(
                        onDismissRequest = { onEvent(PersonDetailUiEvent.OnDismissDeleteDialog) },
                        title = {
                            Text(
                                text = "Delete $count encounter${if (count > 1) "s" else ""}?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "These records will be permanently removed from this contact's history.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
                                    onEvent(PersonDetailUiEvent.OnConfirmDeleteSelected)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                               ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { onEvent(PersonDetailUiEvent.OnDismissDeleteDialog) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel")
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }

                // Create / Edit Reminder In-Hierarchy Overlay (ADR-0037, RAM-60, RAM-61)
                val latestOnEvent = rememberUpdatedState(onEvent)
                val latestHaptics = rememberUpdatedState(haptics)
                val latestMaybePromptNotificationPermission = rememberUpdatedState(::maybePromptNotificationPermission)
                val isReminderSheetOpen = uiState.isCreateReminderSheetOpen || uiState.editingReminder != null
                val reminderOverlayPayload = remember(
                    isReminderSheetOpen,
                    contactName,
                    uiState.editingReminder,
                    uiState.defaultNotificationHour,
                    uiState.defaultNotificationMinute
                ) {
                    if (!isReminderSheetOpen) {
                        null
                    } else {
                        val editing = uiState.editingReminder
                        ProfileReminderOverlayPayload(
                            contactName = contactName,
                            editingReminder = editing,
                            defaultHour = uiState.defaultNotificationHour,
                            defaultMinute = uiState.defaultNotificationMinute,
                            onDismiss = {
                                latestOnEvent.value(PersonDetailUiEvent.OnDismissReminderSheet)
                            },
                            onSave = { dueDate, note, hasExplicitTime, explicitTime ->
                                latestHaptics.value.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                                if (editing != null) {
                                    latestOnEvent.value(
                                        PersonDetailUiEvent.OnUpdateReminder(
                                            followUpId = editing.id,
                                            dueDate = dueDate,
                                            note = note,
                                            hasExplicitTime = hasExplicitTime,
                                            explicitTime = explicitTime
                                        )
                                    )
                                } else {
                                    latestOnEvent.value(
                                        PersonDetailUiEvent.OnCreateReminder(
                                            dueDate = dueDate,
                                            note = note,
                                            hasExplicitTime = hasExplicitTime,
                                            explicitTime = explicitTime
                                        )
                                    )
                                    latestMaybePromptNotificationPermission.value()
                                }
                            },
                            onDelete = { followUpId ->
                                latestHaptics.value.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
                                latestOnEvent.value(PersonDetailUiEvent.OnDeleteReminder(followUpId))
                            }
                        )
                    }
                }

                if (onReminderOverlayChanged != null) {
                    val latestOverlayCallback = rememberUpdatedState(onReminderOverlayChanged)
                    LaunchedEffect(reminderOverlayPayload) {
                        latestOverlayCallback.value(reminderOverlayPayload)
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            latestOverlayCallback.value(null)
                        }
                    }
                } else {
                    ProfileReminderSheetOverlay(payload = reminderOverlayPayload)
                }

                // In-App Notification Permission Explainer (Prime Directive 3, ADR-0037)
                if (showNotificationExplainer) {
                    NotificationExplainerDialog(
                        openSystemSettings = explainerOpensSettings,
                        onAllowClick = {
                            showNotificationExplainer = false
                            if (explainerOpensSettings) {
                                val settingsIntent = if (!FollowUpNotificationHelper.hasNotificationPermission(context)) {
                                    FollowUpNotificationHelper.createAppNotificationSettingsIntent(context)
                                } else {
                                    FollowUpNotificationHelper.createChannelNotificationSettingsIntent(context)
                                }
                                runCatching { context.startActivity(settingsIntent) }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                hasPromptedNotificationPermission = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDismissClick = { showNotificationExplainer = false }
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "Person Detail Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PersonDetailScreenPreview() {
    HelloTheme(darkTheme = false) {
        val sample = MockDataProvider.contactSarah
        PersonDetailScreen(
            uiState = PersonDetailUiState(
                contact = sample,
                contactSummary = ContactSummary(
                    id = sample.id,
                    name = sample.name,
                    contextSubtitle = "Staff Engineer",
                    avatarInitials = "SL",
                    avatarBgIndex = 1
                ),
                encounters = MockDataProvider.encounters.filter { it.contactId == sample.id },
                placesMap = MockDataProvider.places.associateBy { it.id },
                tagsList = MockDataProvider.tags,
                isLoading = false
            ),
            onEvent = {},
            onNavigateBack = {},
            onEditClick = {},
            onLogEncounterClick = {}
        )
    }
}

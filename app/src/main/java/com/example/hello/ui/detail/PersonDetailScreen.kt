package com.example.hello.ui.detail

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.hello.ui.components.HelloToastHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.detail.components.DetailCollapsingHeader
import com.example.hello.ui.detail.components.LivingTimeline
import com.example.hello.ui.detail.components.PropertiesPanel
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.timeline.components.TimelineSelectionActionTray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    uiState: PersonDetailUiState,
    onEvent: (PersonDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onLogEncounterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHelloHaptics.current

    BackHandler(enabled = uiState.isSelectionMode) {
        onEvent(PersonDetailUiEvent.OnClearSelection)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            val hasUndo = msg.startsWith("Deleted", ignoreCase = true)
            val result = if (hasUndo) {
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(msg)
            }
            if (result == SnackbarResult.ActionPerformed) {
                haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                onEvent(PersonDetailUiEvent.OnUndoDeleteEncounter)
            }
            onEvent(PersonDetailUiEvent.OnDismissUserMessage)
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
                        IconButton(onClick = { onEvent(PersonDetailUiEvent.OnToggleFavorite) }) {
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
            HelloToastHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            )
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
                            cadenceStatus = uiState.cadenceStatus,
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
                            onEncounterLongClick = { onEvent(PersonDetailUiEvent.OnEncounterLongClick(it)) }
                        )
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(48.dp))
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
            }
        }
    }
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
                cadenceStatus = CadenceStatusInfo(
                    health = CadenceHealth.ON_TRACK,
                    displayText = "On track • Due in 29 days",
                    subText = "Cadence: Every 30 days • Last met 1 day ago"
                ),
                isLoading = false
            ),
            onEvent = {},
            onNavigateBack = {},
            onEditClick = {},
            onLogEncounterClick = {}
        )
    }
}

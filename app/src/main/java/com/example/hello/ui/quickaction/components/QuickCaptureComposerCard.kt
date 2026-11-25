package com.example.hello.ui.quickaction.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.components.HelloDatePickerDialog
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.MultiContactPickerDialog
import com.example.hello.ui.components.SelectableAvatarBadge
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.quickaction.QuickActionMode
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import com.example.hello.ui.quickaction.QuickCaptureDetailKey
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DAY_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val SHORT_MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val SHORT_WEEKDAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
private val FULL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Unified Todoist-inspired single-surface floating Quick Capture card (ADR-0040 / RAM-91).
 * Serves both Add Contact (Flow A) and Met Again (Flow B) directly above the IME with
 * IME-safe inline detail trays, a floating '+' overflow popover, and Customize Quick Add.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureComposerCard(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val haptics = LocalHelloHaptics.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }

    val isAddContact = uiState.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT
    // Emil Kowalski asymmetric enter/exit timings (faster exit than enter, StrongEaseOut)
    val enterMs = if (isReducedMotion) 120 else 180
    val exitMs = if (isReducedMotion) 90 else 130
    val density = LocalDensity.current

    var showEncounterDatePicker by remember { mutableStateOf(false) }
    var showBirthdayDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPrimaryFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Keep the active InputConnection alive across mode swaps and Notes collapse:
    // if focus is currently in Notes or a floating popover field that is about to unmount,
    // transfer focus to the persistent primary field and wait 2 frames for IME rebind
    // before dispatching so Android never calls hide(ime).
    val runKeepingIme: (() -> Unit) -> Unit = { dispatchAction ->
        if (isPrimaryFocused) {
            keyboardController?.show()
            dispatchAction()
        } else {
            try {
                primaryFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            keyboardController?.show()
            coroutineScope.launch {
                withFrameNanos { }
                withFrameNanos { }
                dispatchAction()
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        try {
            if (!isAddContact && uiState.selectedContacts.isNotEmpty() && uiState.isNoteExpanded) {
                noteFocusRequester.requestFocus()
            } else {
                primaryFocusRequester.requestFocus()
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(uiState.currentMode) {
        if (!isPrimaryFocused) {
            try {
                if (!isAddContact && uiState.selectedContacts.isNotEmpty() && uiState.isNoteExpanded) {
                    noteFocusRequester.requestFocus()
                } else {
                    primaryFocusRequester.requestFocus()
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(uiState.isNoteExpanded) {
        if (uiState.isNoteExpanded) {
            withFrameNanos { }
            delay(30)
            try {
                noteFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    if (showEncounterDatePicker) {
        HelloDatePickerDialog(
            initialDate = uiState.encounterDate,
            onConfirm = { picked -> onEvent(QuickActionUiEvent.OnEncounterDateChange(picked)) },
            onDismissRequest = { showEncounterDatePicker = false },
            restrictToPastDates = false
        )
    }

    if (showBirthdayDatePicker) {
        HelloDatePickerDialog(
            initialDate = uiState.birthday,
            onConfirm = { picked -> onEvent(QuickActionUiEvent.OnBirthdayChange(picked)) },
            onDismissRequest = { showBirthdayDatePicker = false },
            restrictToPastDates = true
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.encounterTime.hour,
            initialMinute = uiState.encounterTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(
                            QuickActionUiEvent.OnEncounterTimeChange(
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (uiState.isFlowBParticipantPickerOpen) {
        val selectedIds = uiState.selectedContacts.ifEmpty { listOfNotNull(uiState.selectedContact) }
            .map { it.id }.toSet()
        MultiContactPickerDialog(
            allContacts = uiState.allContacts,
            initiallySelectedIds = selectedIds,
            onDismiss = { onEvent(QuickActionUiEvent.OnToggleFlowBParticipantPicker(false)) },
            onConfirm = { picked -> onEvent(QuickActionUiEvent.OnSetFlowBParticipants(picked)) }
        )
    }

    val selectedParticipants = uiState.selectedContacts.ifEmpty { listOfNotNull(uiState.selectedContact) }
    val canSave = if (isAddContact) {
        uiState.newContactName.isNotBlank()
    } else {
        selectedParticipants.isNotEmpty()
    }

    val submitCapture = {
        if (canSave) {
            haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
            if (isAddContact) {
                onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
            } else {
                onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)
            }
        }
    }

    val hasOpenPopover = uiState.isPlusPopoverOpen ||
        uiState.isCustomizeQuickAddOpen ||
        uiState.activeDetailTray != null

    val dismissOpenPopover = {
        when {
            uiState.isCustomizeQuickAddOpen -> onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false))
            uiState.isPlusPopoverOpen -> onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
            uiState.activeDetailTray != null -> onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
        }
    }

    var composerCardHeightPx by remember { mutableIntStateOf(0) }
    val popoverBottomPadding = with(density) {
        if (composerCardHeightPx > 0) {
            (composerCardHeightPx.toDp() - 10.dp).coerceAtLeast(116.dp)
        } else {
            136.dp
        }
    }

    // Subtle GPU-composited alpha dimming behind open popovers (zero per-frame BlurEffect reallocation)
    val backgroundContentAlpha by animateFloatAsState(
        targetValue = if (hasOpenPopover) 0.58f else 1f,
        animationSpec = tween(
            durationMillis = if (hasOpenPopover) enterMs else exitMs,
            easing = MotionTokens.StrongEaseOut
        ),
        label = "composerDepthAlpha"
    )

    val depthRecessionModifier = Modifier.graphicsLayer {
        alpha = backgroundContentAlpha
    }

    val statusBarTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxPopoverHeightDp = resolveMaxPopoverHeightDp(
            availableContainerHeightDp = if (maxHeight.value.isFinite()) maxHeight.value else 600f,
            popoverBottomPaddingDp = popoverBottomPadding.value,
            statusBarTopDp = statusBarTopDp.value
        ).dp

        // Layer 1 (Base): Stationary Participant Strip + Composer Card — NEVER shifts or shrinks when popovers open
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectedIds = remember(selectedParticipants) { selectedParticipants.map { it.id }.toSet() }
            val unselectedMatches = remember(uiState.filteredContacts, selectedIds) {
                uiState.filteredContacts.filterNot { it.id in selectedIds }
            }
            val showPredictiveStrip = !isAddContact && uiState.searchQuery.isNotBlank()

            AnimatedVisibility(
                visible = showPredictiveStrip,
                enter = if (isReducedMotion) {
                    fadeIn(tween(enterMs))
                } else {
                    fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut)) +
                        slideInVertically(tween(enterMs, easing = MotionTokens.StrongEaseOut)) { it / 3 }
                },
                exit = if (isReducedMotion) {
                    fadeOut(tween(exitMs))
                } else {
                    fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut)) +
                        slideOutVertically(tween(exitMs, easing = MotionTokens.StrongEaseOut)) { it / 3 }
                }
            ) {
                Box(modifier = depthRecessionModifier) {
                    MetAgainPredictiveStrip(
                        query = uiState.searchQuery.trim(),
                        matches = unselectedMatches,
                        onSelectContact = { contact ->
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(QuickActionUiEvent.OnCommitFlowBParticipant(contact))
                        },
                        onConvertToAddContact = { typedName ->
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            runKeepingIme {
                                onEvent(QuickActionUiEvent.OnConvertQueryToAddContact(typedName))
                            }
                        }
                    )
                }
            }

            // Floating Todoist-style Capture Card (stationary & compact; measured so popovers anchor at its top rim)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { composerCardHeightPx = it.height }
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .clickable(
                        enabled = hasOpenPopover,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { dismissOpenPopover() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upper input region stays 100% sharp while floating popovers sit cleanly above the top rim
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subtle Drag Handle (tapping toggles Notes while keeping the IME continuously up)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                                    if (uiState.isNoteExpanded) {
                                        runKeepingIme {
                                            onEvent(QuickActionUiEvent.OnSetNoteExpanded(false))
                                        }
                                    } else {
                                        onEvent(QuickActionUiEvent.OnSetNoteExpanded(true))
                                    }
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        }

                        // Editorial Clickable Header + Circular Dismiss Button (ADR-0040)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                        runKeepingIme {
                                            onEvent(QuickActionUiEvent.OnSwitchComposerMode)
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                AnimatedContent(
                                    targetState = isAddContact,
                                    transitionSpec = {
                                        fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut)) togetherWith
                                            fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut))
                                    },
                                    label = "composerModeTitle"
                                ) { addMode ->
                                    Text(
                                        text = if (addMode) "Add Contact" else "Met Again",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldMore,
                                    contentDescription = if (isAddContact) "Switch to Met Again" else "Switch to Add Contact",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                    onEvent(QuickActionUiEvent.OnDismiss)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Primary Input: single persistent BasicTextField across Add Contact and Met Again
                    // so swapping modes never disposes the active InputConnection or drops the IME.
                    val query = uiState.searchQuery
                    val ghostSuffix = remember(isAddContact, query, unselectedMatches) {
                        if (isAddContact) null else computePredictiveGhostSuffix(query, unselectedMatches.firstOrNull())
                    }
                    val showInlineChips = !isAddContact && selectedParticipants.isNotEmpty()
                    val primaryTextSize = if (showInlineChips) 17.sp else 20.sp
                    val primaryFieldValue = if (isAddContact) uiState.newContactName else query

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showInlineChips) {
                            selectedParticipants.forEach { participant ->
                                key(participant.id) {
                                    InlineParticipantChip(
                                        contact = participant,
                                        onRemove = {
                                            haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                                            onEvent(QuickActionUiEvent.OnRemoveSelectedContactFlowB(participant.id))
                                        }
                                    )
                                }
                            }
                        }

                        BasicTextField(
                            value = primaryFieldValue,
                            onValueChange = { value ->
                                if (isAddContact) {
                                    onEvent(QuickActionUiEvent.OnNameChange(value))
                                } else {
                                    onEvent(QuickActionUiEvent.OnSearchQueryChange(value))
                                }
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontSize = primaryTextSize,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = if (isAddContact) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (isAddContact) submitCapture() else onEvent(QuickActionUiEvent.OnFlowBEnterPressed)
                                },
                                onDone = {
                                    if (isAddContact) submitCapture() else onEvent(QuickActionUiEvent.OnFlowBEnterPressed)
                                }
                            ),
                            modifier = Modifier
                                .then(
                                    if (showInlineChips) {
                                        Modifier.widthIn(min = 120.dp)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                )
                                .focusRequester(primaryFocusRequester)
                                .onFocusChanged { isPrimaryFocused = it.isFocused }
                                .padding(vertical = 2.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (primaryFieldValue.isEmpty()) {
                                        Text(
                                            text = when {
                                                isAddContact -> "Person's name"
                                                selectedParticipants.isEmpty() -> "Who did you meet?"
                                                else -> "Add another..."
                                            },
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = primaryTextSize,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    } else if (ghostSuffix != null) {
                                        Text(
                                            text = query + ghostSuffix,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = primaryTextSize,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    if (uiState.nameError && isAddContact) {
                        Text(
                            text = "Please enter a name first",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Secondary Expandable Note Input (keyed by mode so switching Met Again -> Add Contact never flashes Notes)
                    key(uiState.currentMode) {
                        AnimatedVisibility(
                            visible = uiState.isNoteExpanded,
                            enter = if (isReducedMotion) {
                                fadeIn(tween(enterMs))
                            } else {
                                expandVertically(tween(enterMs, easing = MotionTokens.StrongEaseOut)) +
                                    fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut))
                            },
                            exit = if (isReducedMotion) {
                                fadeOut(tween(exitMs))
                            } else {
                                shrinkVertically(tween(exitMs, easing = MotionTokens.StrongEaseOut)) +
                                    fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut))
                            }
                        ) {
                            val noteValue = if (isAddContact) uiState.encounterNote else uiState.encounterNoteFlowB
                            BasicTextField(
                                value = noteValue,
                                onValueChange = { value ->
                                    if (isAddContact) {
                                        onEvent(QuickActionUiEvent.OnNoteChange(value))
                                    } else {
                                        onEvent(QuickActionUiEvent.OnEncounterNoteChange(value))
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitCapture() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(noteFocusRequester)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (noteValue.isEmpty()) {
                                            Text(
                                                text = if (isAddContact) "Add a note..." else "Caught up about...",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                    } // end upper input region (depth-recessed behind open popovers)

                    // Bottom Action Bar: [+] | [Scrollable Detail Buttons] | [Save]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left '+' Popover Trigger (tactile 0.96f press feedback)
                        val plusInteraction = remember { MutableInteractionSource() }
                        val plusPressed by plusInteraction.collectIsPressedAsState()
                        val plusScale by animateFloatAsState(
                            targetValue = if (plusPressed && !isReducedMotion) 0.96f else 1f,
                            animationSpec = tween(120, easing = MotionTokens.StrongEaseOut),
                            label = "plusPressScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .graphicsLayer {
                                    scaleX = plusScale
                                    scaleY = plusScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (uiState.isPlusPopoverOpen || uiState.isCustomizeQuickAddOpen) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                                .clickable(
                                    interactionSource = plusInteraction,
                                    indication = null
                                ) {
                                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                    onEvent(QuickActionUiEvent.OnTogglePlusPopover(!uiState.isPlusPopoverOpen))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "More details",
                                tint = if (uiState.isPlusPopoverOpen || uiState.isCustomizeQuickAddOpen) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Scrollable Detail Buttons with dynamic left & right edge fades
                        Box(modifier = Modifier.weight(1f)) {
                            val chipScrollState = rememberScrollState()
                            val surfaceColor = MaterialTheme.colorScheme.surface
                            val fadeDurationMs = if (isReducedMotion) 90 else 140
                            val leftFadeAlpha by animateFloatAsState(
                                targetValue = if (chipScrollState.canScrollBackward) 1f else 0f,
                                animationSpec = tween(fadeDurationMs, easing = MotionTokens.StrongEaseOut),
                                label = "detailBarLeftFade"
                            )
                            val rightFadeAlpha by animateFloatAsState(
                                targetValue = if (chipScrollState.canScrollForward) 1f else 0f,
                                animationSpec = tween(fadeDurationMs, easing = MotionTokens.StrongEaseOut),
                                label = "detailBarRightFade"
                            )

                            val visibleKeys = remember(uiState.pinnedDetailKeys, uiState, isAddContact) {
                                val allAllowed = if (isAddContact) {
                                    QuickCaptureDetailKey.entries
                                } else {
                                    listOf(
                                        QuickCaptureDetailKey.LOCATION,
                                        QuickCaptureDetailKey.TIME,
                                        QuickCaptureDetailKey.TAGS,
                                        QuickCaptureDetailKey.REMINDER
                                    )
                                }
                                allAllowed.filter { key ->
                                    key in uiState.pinnedDetailKeys || isDetailPopulated(key, uiState, isAddContact)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(chipScrollState)
                                    .padding(end = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                visibleKeys.forEach { key ->
                                    val populated = isDetailPopulated(key, uiState, isAddContact)
                                    val active = uiState.activeDetailTray == key
                                    val label = detailButtonLabel(key, uiState, isAddContact)
                                    DetailActionChip(
                                        icon = detailIconFor(key),
                                        label = label,
                                        isFilled = populated || active,
                                        showClear = populated,
                                        onClick = {
                                            haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                                            if (key == QuickCaptureDetailKey.BIRTHDAY) {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                                showBirthdayDatePicker = true
                                            } else {
                                                onEvent(QuickActionUiEvent.OnToggleDetailTray(key))
                                            }
                                        },
                                        onClear = {
                                            haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                                            onEvent(QuickActionUiEvent.OnClearDetailKey(key))
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(18.dp)
                                    .height(38.dp)
                                    .graphicsLayer { alpha = leftFadeAlpha }
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(surfaceColor, Color.Transparent)
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(18.dp)
                                    .height(38.dp)
                                    .graphicsLayer { alpha = rightFadeAlpha }
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, surfaceColor)
                                        )
                                    )
                            )
                        }

                        // Right Pinned Primary Squircle Save Button (tactile 0.96f press feedback)
                        val saveInteraction = remember { MutableInteractionSource() }
                        val savePressed by saveInteraction.collectIsPressedAsState()
                        val saveScale by animateFloatAsState(
                            targetValue = if (savePressed && canSave && !isReducedMotion) 0.96f else 1f,
                            animationSpec = tween(120, easing = MotionTokens.StrongEaseOut),
                            label = "savePressScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer {
                                    scaleX = saveScale
                                    scaleY = saveScale
                                }
                                .alpha(if (canSave) 1f else 0.35f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(
                                    enabled = canSave,
                                    interactionSource = saveInteraction,
                                    indication = null
                                ) { submitCapture() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = if (isAddContact) "Save contact" else "Save encounter",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Layer 2 (Overlay): Origin-aware Emil Kowalski popovers (StrongEaseOut, 180ms enter / 130ms exit, scale 0.94f + slide + fade)
        val plusEnter = if (isReducedMotion) {
            fadeIn(tween(enterMs))
        } else {
            scaleIn(
                initialScale = 0.94f,
                transformOrigin = TransformOrigin(0f, 1f),
                animationSpec = tween(enterMs, easing = MotionTokens.StrongEaseOut)
            ) + slideInVertically(tween(enterMs, easing = MotionTokens.StrongEaseOut)) { 14 } +
                fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut))
        }
        val plusExit = if (isReducedMotion) {
            fadeOut(tween(exitMs))
        } else {
            scaleOut(
                targetScale = 0.96f,
                transformOrigin = TransformOrigin(0f, 1f),
                animationSpec = tween(exitMs, easing = MotionTokens.StrongEaseOut)
            ) + slideOutVertically(tween(exitMs, easing = MotionTokens.StrongEaseOut)) { 8 } +
                fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut))
        }
        val centerPopoverEnter = if (isReducedMotion) {
            fadeIn(tween(enterMs))
        } else {
            scaleIn(
                initialScale = 0.94f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(enterMs, easing = MotionTokens.StrongEaseOut)
            ) + slideInVertically(tween(enterMs, easing = MotionTokens.StrongEaseOut)) { 14 } +
                fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut))
        }
        val centerPopoverExit = if (isReducedMotion) {
            fadeOut(tween(exitMs))
        } else {
            scaleOut(
                targetScale = 0.96f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(exitMs, easing = MotionTokens.StrongEaseOut)
            ) + slideOutVertically(tween(exitMs, easing = MotionTokens.StrongEaseOut)) { 8 } +
                fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut))
        }

        AnimatedVisibility(
            visible = uiState.isPlusPopoverOpen,
            enter = plusEnter,
            exit = plusExit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = popoverBottomPadding)
                .zIndex(2f)
        ) {
            PlusOverflowPopover(
                isAddContact = isAddContact,
                maxHeight = maxPopoverHeightDp,
                onSelectNote = { onEvent(QuickActionUiEvent.OnSetNoteExpanded(true)) },
                onSelectDetail = { key ->
                    if (key == QuickCaptureDetailKey.BIRTHDAY) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
                        showBirthdayDatePicker = true
                    } else {
                        onEvent(QuickActionUiEvent.OnToggleDetailTray(key))
                    }
                },
                onOpenCustomize = { onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(true)) }
            )
        }

        AnimatedVisibility(
            visible = uiState.isCustomizeQuickAddOpen,
            enter = centerPopoverEnter,
            exit = centerPopoverExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = popoverBottomPadding)
                .zIndex(2f)
        ) {
            CustomizeQuickAddPanel(
                uiState = uiState,
                isAddContact = isAddContact,
                maxHeight = maxPopoverHeightDp,
                onSelectPreset = { onEvent(QuickActionUiEvent.OnSelectFlowPreset(it)) },
                onTogglePin = { onEvent(QuickActionUiEvent.OnTogglePinDetailKey(it)) },
                onDone = { onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false)) }
            )
        }

        AnimatedVisibility(
            visible = uiState.activeDetailTray != null,
            enter = centerPopoverEnter,
            exit = centerPopoverExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = popoverBottomPadding)
                .zIndex(2f)
        ) {
            AnimatedContent(
                targetState = uiState.activeDetailTray,
                transitionSpec = {
                    fadeIn(tween(enterMs, easing = MotionTokens.StrongEaseOut)) togetherWith
                        fadeOut(tween(exitMs, easing = MotionTokens.StrongEaseOut))
                },
                label = "floatingPopoverContentSwap"
            ) { activeKey ->
                if (activeKey != null) {
                    FloatingDetailPopover(
                        detailKey = activeKey,
                        isAddContact = isAddContact,
                        uiState = uiState,
                        maxHeight = minOf(maxPopoverHeightDp, 280.dp),
                        onEvent = onEvent,
                        onOpenDatePicker = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                            showEncounterDatePicker = true
                        },
                        onOpenTimePicker = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                            showTimePicker = true
                        },
                        onOpenBirthdayPicker = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                            showBirthdayDatePicker = true
                        },
                        onDone = { onEvent(QuickActionUiEvent.OnToggleDetailTray(null)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineParticipantChip(
    contact: ContactSummary,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SelectableAvatarBadge(
                isSelected = false,
                initials = contact.avatarInitials,
                bgIndex = contact.avatarBgIndex,
                size = 22.dp,
                photoUri = contact.photoUri
            )
            Text(
                text = contact.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove ${contact.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun MetAgainPredictiveStrip(
    query: String,
    matches: List<ContactSummary>,
    onSelectContact: (ContactSummary) -> Unit,
    onConvertToAddContact: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (matches.isNotEmpty()) {
            items(matches.take(8), key = { it.id }) { contact ->
                val matchedNickname = remember(contact.nicknames, query) {
                    if (contact.name.contains(query, ignoreCase = true)) {
                        null
                    } else {
                        contact.nicknames.firstOrNull { it.contains(query, ignoreCase = true) }
                    }
                }
                val displayLabel = if (matchedNickname != null) {
                    "${contact.name} ($matchedNickname)"
                } else {
                    contact.name
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelectContact(contact) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectableAvatarBadge(
                            isSelected = false,
                            initials = contact.avatarInitials,
                            bgIndex = contact.avatarBgIndex,
                            size = 24.dp,
                            photoUri = contact.photoUri
                        )
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else if (query.isNotEmpty()) {
            item(key = "convert-to-add-contact") {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onConvertToAddContact(query) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Add \"$query\" as new contact",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun computePredictiveGhostSuffix(
    query: String,
    topMatch: ContactSummary?
): String? {
    if (query.isBlank() || topMatch == null) return null
    val trimmed = query.trimStart()
    if (trimmed.isEmpty()) return null
    val candidate = when {
        topMatch.name.startsWith(trimmed, ignoreCase = true) -> topMatch.name
        else -> topMatch.nicknames.firstOrNull { it.startsWith(trimmed, ignoreCase = true) }
    } ?: return null
    if (candidate.length <= trimmed.length) return null
    return candidate.substring(trimmed.length)
}

internal fun resolveDetailChipCornerRadiusDp(isSelectedOrActive: Boolean): Float =
    if (isSelectedOrActive) 10f else 19f

@Composable
private fun DetailActionChip(
    icon: ImageVector,
    label: String,
    isFilled: Boolean,
    showClear: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.96f else 1f,
        animationSpec = tween(120, easing = MotionTokens.StrongEaseOut),
        label = "detailChipPressScale"
    )
    val cornerRadiusState = animateDpAsState(
        targetValue = resolveDetailChipCornerRadiusDp(isFilled).dp,
        animationSpec = tween(if (isReducedMotion) 0 else 180, easing = MotionTokens.StrongEaseInOut),
        label = "detailChipCornerRadius"
    )
    val bg = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (isFilled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                shape = RoundedCornerShape(cornerRadiusState.value)
                clip = true
            }
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 11.dp, end = if (showClear) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showClear) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear $label",
                tint = fg.copy(alpha = 0.72f),
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
            )
        }
    }
}

/**
 * Computes the maximum safe height (in dp) for floating popovers hovering above
 * QuickCaptureComposerCard so they stay below the status bar on compact/older phones.
 */
internal fun resolveMaxPopoverHeightDp(
    availableContainerHeightDp: Float,
    popoverBottomPaddingDp: Float,
    statusBarTopDp: Float,
    topClearanceDp: Float = 12f,
    minHeightDp: Float = 120f,
    maxCapDp: Float = 360f
): Float {
    if (!availableContainerHeightDp.isFinite() || availableContainerHeightDp <= 0f) {
        return 280f.coerceIn(minHeightDp, maxCapDp)
    }
    val remaining = availableContainerHeightDp -
        popoverBottomPaddingDp.coerceAtLeast(0f) -
        statusBarTopDp.coerceAtLeast(0f) -
        topClearanceDp.coerceAtLeast(0f)
    return remaining.coerceIn(minHeightDp, maxCapDp)
}

@Composable
private fun ScrollableFadeContainer(
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.ScrollState) -> Unit
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        content(scrollState)
        if (scrollState.canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Brush.verticalGradient(colors = listOf(surfaceColor, Color.Transparent)))
            )
        }
        if (scrollState.canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, surfaceColor)))
            )
        }
    }
}

@Composable
private fun PlusOverflowPopover(
    isAddContact: Boolean,
    maxHeight: androidx.compose.ui.unit.Dp,
    onSelectNote: () -> Unit,
    onSelectDetail: (QuickCaptureDetailKey) -> Unit,
    onOpenCustomize: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        shadowElevation = 10.dp,
        modifier = Modifier
            .width(228.dp)
            .heightIn(max = maxHeight)
    ) {
        ScrollableFadeContainer(surfaceColor = surfaceColor) { scrollState ->
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(6.dp)
            ) {
                PopoverMenuRow(icon = Icons.Rounded.Edit, label = "Notes", onClick = onSelectNote)
                if (isAddContact) {
                    PopoverMenuRow(icon = Icons.Rounded.Work, label = "Occupation", onClick = { onSelectDetail(QuickCaptureDetailKey.OCCUPATION) })
                    PopoverMenuRow(icon = Icons.Rounded.Phone, label = "Phone & Email", onClick = { onSelectDetail(QuickCaptureDetailKey.CONTACT_INFO) })
                    PopoverMenuRow(icon = Icons.Rounded.Cake, label = "Birthday", onClick = { onSelectDetail(QuickCaptureDetailKey.BIRTHDAY) })
                    PopoverMenuRow(icon = Icons.Rounded.Tune, label = "Custom field", onClick = { onSelectDetail(QuickCaptureDetailKey.CUSTOM) })
                } else {
                    PopoverMenuRow(icon = Icons.Rounded.Place, label = "Location", onClick = { onSelectDetail(QuickCaptureDetailKey.LOCATION) })
                    PopoverMenuRow(icon = Icons.Rounded.Schedule, label = "Date & Time", onClick = { onSelectDetail(QuickCaptureDetailKey.TIME) })
                    PopoverMenuRow(icon = Icons.Rounded.Notifications, label = "Reminder", onClick = { onSelectDetail(QuickCaptureDetailKey.REMINDER) })
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                PopoverMenuRow(icon = Icons.Rounded.Tune, label = "Customize Quick Add", onClick = onOpenCustomize)
            }
        }
    }
}

@Composable
private fun PopoverMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CustomizeQuickAddPanel(
    uiState: QuickActionUiState,
    isAddContact: Boolean,
    maxHeight: androidx.compose.ui.unit.Dp,
    onSelectPreset: (String) -> Unit,
    onTogglePin: (QuickCaptureDetailKey) -> Unit,
    onDone: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOMIZE QUICK ADD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDone)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            ScrollableFadeContainer(
                surfaceColor = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) { scrollState ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAddContact && uiState.availableFlowPresets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.availableFlowPresets.forEach { preset ->
                                MinimalSelectableChip(
                                    label = preset.name,
                                    selected = preset.id == uiState.activeFlowPreset?.id,
                                    onClick = { onSelectPreset(preset.id) }
                                )
                            }
                        }
                    }

                    val toggleableKeys = if (isAddContact) {
                        listOf(
                            QuickCaptureDetailKey.LOCATION,
                            QuickCaptureDetailKey.TAGS,
                            QuickCaptureDetailKey.REMINDER,
                            QuickCaptureDetailKey.OCCUPATION,
                            QuickCaptureDetailKey.CONTACT_INFO,
                            QuickCaptureDetailKey.BIRTHDAY,
                            QuickCaptureDetailKey.CUSTOM
                        )
                    } else {
                        listOf(
                            QuickCaptureDetailKey.LOCATION,
                            QuickCaptureDetailKey.TIME,
                            QuickCaptureDetailKey.TAGS,
                            QuickCaptureDetailKey.REMINDER
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        toggleableKeys.forEach { key ->
                            val pinned = key in uiState.pinnedDetailKeys
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = key.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = pinned,
                                    onCheckedChange = { onTogglePin(key) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingDetailPopover(
    detailKey: QuickCaptureDetailKey,
    isAddContact: Boolean,
    uiState: QuickActionUiState,
    maxHeight: androidx.compose.ui.unit.Dp,
    onEvent: (QuickActionUiEvent) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onOpenBirthdayPicker: () -> Unit,
    onDone: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        ScrollableFadeContainer(surfaceColor = surfaceColor) { scrollState ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
            when (detailKey) {
                QuickCaptureDetailKey.LOCATION -> {
                    val gpsLabel = when {
                        uiState.isLocatingGps -> "Locating..."
                        uiState.capturedLocationLabel != null -> uiState.capturedLocationLabel
                        else -> "Current GPS"
                    }
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Place,
                        label = gpsLabel,
                        meta = if (uiState.capturedLatitude != null) "GPS" else "Foreground",
                        isSelected = uiState.isLocatingGps || (uiState.capturedLatitude != null && uiState.capturedLongitude != null),
                        onClick = {
                            onEvent(QuickActionUiEvent.OnRequestLocation)
                            onDone()
                        }
                    )
                    uiState.suggestedPlaces.take(5).forEach { place ->
                        val selected = if (isAddContact) {
                            uiState.selectedPlaceId == place.id
                        } else {
                            uiState.encounterPlaceId == place.id
                        }
                        FloatingPopoverOptionRow(
                            icon = Icons.Rounded.Place,
                            label = place.label,
                            meta = "Saved",
                            isSelected = selected,
                            onClick = {
                                if (isAddContact) {
                                    onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id))
                                } else {
                                    onEvent(QuickActionUiEvent.OnEncounterPlaceChange(if (selected) null else place.id, ""))
                                }
                                onDone()
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    val customPlaceVal = if (isAddContact) uiState.customPlaceName else uiState.encounterCustomPlaceName
                    PopoverInputRow(
                        icon = Icons.Rounded.Add,
                        value = customPlaceVal,
                        onValueChange = { text ->
                            if (isAddContact) {
                                onEvent(QuickActionUiEvent.OnCustomPlaceNameChange(text))
                            } else {
                                onEvent(QuickActionUiEvent.OnEncounterPlaceChange(null, text))
                            }
                        },
                        placeholder = "Type a custom place...",
                        actionLabel = if (customPlaceVal.isNotBlank()) "Done" else null,
                        onAction = onDone
                    )
                }

                QuickCaptureDetailKey.TIME -> {
                    val today = LocalDate.now()
                    val yesterday = today.minusDays(1)
                    val isToday = uiState.encounterDate == today
                    val isYesterday = uiState.encounterDate == yesterday
                    val timeLabel = remember(uiState.encounterTime) {
                        uiState.encounterTime.format(TIME_FORMATTER)
                    }
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Today",
                        meta = today.format(DAY_MONTH_FORMATTER),
                        isSelected = isToday && !uiState.isEncounterTimeCustomized,
                        onClick = {
                            onEvent(QuickActionUiEvent.OnEncounterDateChange(today))
                            onDone()
                        }
                    )
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Yesterday",
                        meta = yesterday.format(DAY_MONTH_FORMATTER),
                        isSelected = isYesterday,
                        onClick = {
                            onEvent(QuickActionUiEvent.OnEncounterDateChange(yesterday))
                            onDone()
                        }
                    )
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Pick time",
                        meta = timeLabel,
                        isSelected = uiState.isEncounterTimeCustomized,
                        onClick = onOpenTimePicker
                    )
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Schedule,
                        label = "Custom date…",
                        meta = if (!isToday && !isYesterday) {
                            uiState.encounterDate.format(SHORT_MONTH_DAY_FORMATTER)
                        } else {
                            "Calendar"
                        },
                        isSelected = !isToday && !isYesterday,
                        onClick = onOpenDatePicker
                    )
                }

                QuickCaptureDetailKey.TAGS -> {
                    if (uiState.suggestedTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.suggestedTags.take(12).forEach { tag ->
                                TonalTagPill(
                                    tag = tag,
                                    isSelected = tag.id in uiState.selectedTagIds,
                                    onClick = { onEvent(QuickActionUiEvent.OnToggleTag(tag.id)) }
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                    PopoverInputRow(
                        icon = Icons.Rounded.Add,
                        value = uiState.customTagInput,
                        onValueChange = { onEvent(QuickActionUiEvent.OnCustomTagInputChange(it)) },
                        placeholder = "Create new tag...",
                        actionLabel = if (uiState.customTagInput.isNotBlank()) "Add" else "Done",
                        onAction = {
                            if (uiState.customTagInput.isNotBlank()) {
                                onEvent(QuickActionUiEvent.OnAddCustomTag)
                            } else {
                                onDone()
                            }
                        }
                    )
                }

                QuickCaptureDetailKey.REMINDER -> {
                    val today = LocalDate.now()
                    val options = listOf(
                        Triple(1, "Tomorrow", today.plusDays(1).format(SHORT_WEEKDAY_FORMATTER)),
                        Triple(3, "In 3 days", today.plusDays(3).format(DAY_MONTH_FORMATTER)),
                        Triple(7, "In 1 week", today.plusDays(7).format(SHORT_MONTH_DAY_FORMATTER)),
                        Triple(30, "In 1 month", today.plusDays(30).format(SHORT_MONTH_DAY_FORMATTER))
                    )
                    options.forEach { (days, label, meta) ->
                        FloatingPopoverOptionRow(
                            icon = Icons.Rounded.Notifications,
                            label = label,
                            meta = meta,
                            isSelected = uiState.selectedFollowUpDays == days,
                            onClick = {
                                onEvent(QuickActionUiEvent.OnSelectFollowUp(days))
                                onDone()
                            }
                        )
                    }
                }

                QuickCaptureDetailKey.OCCUPATION -> {
                    PopoverInputRow(
                        icon = Icons.Rounded.Work,
                        value = uiState.role,
                        onValueChange = { onEvent(QuickActionUiEvent.OnRoleChange(it)) },
                        placeholder = "Role (e.g. Designer)"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    PopoverInputRow(
                        icon = Icons.Rounded.Edit,
                        value = uiState.company,
                        onValueChange = { onEvent(QuickActionUiEvent.OnCompanyChange(it)) },
                        placeholder = "Company",
                        actionLabel = "Done",
                        onAction = onDone
                    )
                }

                QuickCaptureDetailKey.CONTACT_INFO -> {
                    PopoverInputRow(
                        icon = Icons.Rounded.Phone,
                        value = uiState.phone,
                        onValueChange = { onEvent(QuickActionUiEvent.OnPhoneChange(it)) },
                        placeholder = "Phone number",
                        keyboardType = KeyboardType.Phone
                    )
                    uiState.phoneError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    PopoverInputRow(
                        icon = Icons.Rounded.Edit,
                        value = uiState.email,
                        onValueChange = { onEvent(QuickActionUiEvent.OnEmailChange(it)) },
                        placeholder = "Email address",
                        keyboardType = KeyboardType.Email,
                        actionLabel = "Done",
                        onAction = onDone
                    )
                }

                QuickCaptureDetailKey.BIRTHDAY -> {
                    FloatingPopoverOptionRow(
                        icon = Icons.Rounded.Cake,
                        label = uiState.birthday?.format(FULL_DATE_FORMATTER) ?: "Pick birthday…",
                        meta = "Calendar",
                        isSelected = uiState.birthday != null,
                        onClick = onOpenBirthdayPicker
                    )
                }

                QuickCaptureDetailKey.CUSTOM -> {
                    PopoverInputRow(
                        icon = Icons.Rounded.Tune,
                        value = uiState.customPropertyKey,
                        onValueChange = { onEvent(QuickActionUiEvent.OnCustomPropertyKeyChange(it)) },
                        placeholder = "Field label"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    val canAddCustom = uiState.customPropertyKey.isNotBlank() && uiState.customPropertyValue.isNotBlank()
                    PopoverInputRow(
                        icon = Icons.Rounded.Edit,
                        value = uiState.customPropertyValue,
                        onValueChange = { onEvent(QuickActionUiEvent.OnCustomPropertyValueChange(it)) },
                        placeholder = "Field value",
                        actionLabel = if (canAddCustom) "Add" else "Done",
                        onAction = {
                            if (canAddCustom) {
                                onEvent(QuickActionUiEvent.OnAddCustomProperty)
                            } else {
                                onDone()
                            }
                        }
                    )
                    if (uiState.customProperties.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.customProperties.forEach { (k, v) ->
                                MinimalSelectableChip(
                                    label = "$k: $v ×",
                                    selected = true,
                                    onClick = { onEvent(QuickActionUiEvent.OnRemoveCustomProperty(k)) }
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

@Composable
private fun PopoverInputRow(
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAction() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FloatingPopoverOptionRow(
    icon: ImageVector,
    label: String,
    meta: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactTrayInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

private fun detailIconFor(key: QuickCaptureDetailKey): ImageVector = when (key) {
    QuickCaptureDetailKey.LOCATION -> Icons.Rounded.Place
    QuickCaptureDetailKey.TIME -> Icons.Rounded.Schedule
    QuickCaptureDetailKey.TAGS -> Icons.Rounded.Edit
    QuickCaptureDetailKey.REMINDER -> Icons.Rounded.Notifications
    QuickCaptureDetailKey.OCCUPATION -> Icons.Rounded.Work
    QuickCaptureDetailKey.CONTACT_INFO -> Icons.Rounded.Phone
    QuickCaptureDetailKey.BIRTHDAY -> Icons.Rounded.Cake
    QuickCaptureDetailKey.CUSTOM -> Icons.Rounded.Tune
}

private fun isDetailPopulated(
    key: QuickCaptureDetailKey,
    uiState: QuickActionUiState,
    isAddContact: Boolean
): Boolean = when (key) {
    QuickCaptureDetailKey.LOCATION -> {
        if (isAddContact) {
            uiState.selectedPlaceId != null || uiState.customPlaceName.isNotBlank() || uiState.capturedLocationLabel != null
        } else {
            uiState.encounterPlaceId != null || uiState.encounterCustomPlaceName.isNotBlank() || uiState.capturedLocationLabel != null
        }
    }
    QuickCaptureDetailKey.TIME -> uiState.isEncounterTimeCustomized || uiState.encounterDate != LocalDate.now()
    QuickCaptureDetailKey.TAGS -> uiState.selectedTagIds.isNotEmpty()
    QuickCaptureDetailKey.REMINDER -> uiState.selectedFollowUpDays != null
    QuickCaptureDetailKey.OCCUPATION -> uiState.role.isNotBlank() || uiState.company.isNotBlank()
    QuickCaptureDetailKey.CONTACT_INFO -> uiState.phone.isNotBlank() || uiState.email.isNotBlank()
    QuickCaptureDetailKey.BIRTHDAY -> uiState.birthday != null
    QuickCaptureDetailKey.CUSTOM -> uiState.customProperties.isNotEmpty()
}

private fun detailButtonLabel(
    key: QuickCaptureDetailKey,
    uiState: QuickActionUiState,
    isAddContact: Boolean
): String = when (key) {
    QuickCaptureDetailKey.LOCATION -> {
        val placeId = if (isAddContact) uiState.selectedPlaceId else uiState.encounterPlaceId
        val custom = if (isAddContact) uiState.customPlaceName else uiState.encounterCustomPlaceName
        uiState.suggestedPlaces.firstOrNull { it.id == placeId }?.label
            ?: custom.takeIf { it.isNotBlank() }
            ?: uiState.capturedLocationLabel
            ?: "Location"
    }
    QuickCaptureDetailKey.TIME -> {
        if (uiState.isEncounterTimeCustomized || uiState.encounterDate != LocalDate.now()) {
            uiState.encounterTime.format(TIME_FORMATTER)
        } else {
            "Now"
        }
    }
    QuickCaptureDetailKey.TAGS -> {
        val n = uiState.selectedTagIds.size
        if (n > 0) "$n tag${if (n > 1) "s" else ""}" else "Tags"
    }
    QuickCaptureDetailKey.REMINDER -> {
        when (val d = uiState.selectedFollowUpDays) {
            null -> "Reminder"
            1 -> "Tomorrow"
            7 -> "In 1w"
            30 -> "In 1m"
            else -> "In ${d}d"
        }
    }
    QuickCaptureDetailKey.OCCUPATION -> {
        when {
            uiState.role.isNotBlank() && uiState.company.isNotBlank() -> "${uiState.role} @ ${uiState.company}"
            uiState.role.isNotBlank() -> uiState.role
            uiState.company.isNotBlank() -> uiState.company
            else -> "Occupation"
        }
    }
    QuickCaptureDetailKey.CONTACT_INFO -> {
        when {
            uiState.phone.isNotBlank() && uiState.email.isNotBlank() -> "Phone & Email"
            uiState.phone.isNotBlank() -> uiState.phone
            uiState.email.isNotBlank() -> uiState.email
            else -> "Phone & Email"
        }
    }
    QuickCaptureDetailKey.BIRTHDAY -> {
        uiState.birthday?.format(SHORT_MONTH_DAY_FORMATTER) ?: "Birthday"
    }
    QuickCaptureDetailKey.CUSTOM -> {
        val n = uiState.customProperties.size
        if (n > 0) "$n custom" else "Custom"
    }
}

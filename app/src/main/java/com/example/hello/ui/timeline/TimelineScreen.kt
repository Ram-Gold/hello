package com.example.hello.ui.timeline

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.ui.components.SelectableAvatarBadge
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.timeline.components.TimelineSelectionActionTray

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    uiState: TimelineUiState,
    onEvent: (TimelineUiEvent) -> Unit,
    onContactClick: (String) -> Unit,
    onLogEncounterForContact: (String, String?) -> Unit,
    onLogEncounterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showFuture = uiState.selectedFilter == TimelineFilter.ALL || uiState.selectedFilter == TimelineFilter.FUTURE
    val showPast = uiState.selectedFilter == TimelineFilter.ALL || uiState.selectedFilter == TimelineFilter.PAST
    val showTodayDivider = uiState.selectedFilter == TimelineFilter.ALL
    val haptics = LocalHelloHaptics.current

    BackHandler(enabled = uiState.isSelectionMode) {
        onEvent(TimelineUiEvent.OnClearSelection)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() / 2)
        ) {
            // 1. Minimal Header + Temporal Filter Strip / Selection Contextual Header
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                label = "TimelineHeaderTransition"
            ) { isSelectionMode ->
                if (isSelectionMode) {
                    TimelineSelectionHeader(
                        selectedCount = uiState.selectedNodeIds.size,
                        onClearSelection = { onEvent(TimelineUiEvent.OnClearSelection) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                } else {
                    TimelineHeader(
                        selectedFilter = uiState.selectedFilter,
                        onSelectFilter = { onEvent(TimelineUiEvent.OnSelectFilter(it)) },
                        onLogEncounterClick = onLogEncounterClick
                    )
                }
            }

            // 2. Continuous Vertical Living Timeline Spine with Overlay Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = 128.dp
                    )
                ) {
                    // Future Horizon Section (Soonest-First, Dashed Spine)
                    if (showFuture) {
                        if (uiState.futureNodes.isEmpty() && uiState.selectedFilter == TimelineFilter.FUTURE) {
                            item(key = "empty-future") {
                                EmptyTimelineStateCard(
                                    title = "No upcoming events in your horizon",
                                    subtitle = "Scheduled follow-ups, birthdays, and cadence check-ins will appear here."
                                )
                            }
                        } else if (uiState.futureNodes.isNotEmpty()) {
                            itemsIndexed(
                                items = uiState.futureNodes,
                                key = { _, node -> node.id }
                            ) { index, node ->
                                val isFirstFuture = index == 0
                                val isLastFuture = index == uiState.futureNodes.lastIndex && !showTodayDivider
                                val isSelected = node.id in uiState.selectedNodeIds
                                val isFollowUp = node is FutureTimelineNode.FollowUpNode

                                FutureTimelineNodeRow(
                                    node = node,
                                    isFirst = isFirstFuture,
                                    isLast = isLastFuture,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            if (isFollowUp) {
                                                onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                            }
                                        } else {
                                            onContactClick(node.contactId)
                                        }
                                    },
                                    onLongClick = if (isFollowUp) {
                                        { onEvent(TimelineUiEvent.OnNodeLongClick(node.id)) }
                                    } else null,
                                    onMarkDone = { followUpId ->
                                        onEvent(TimelineUiEvent.OnMarkFollowUpDone(followUpId))
                                    },
                                    onSnooze = { followUpId ->
                                        onEvent(TimelineUiEvent.OnSnoozeFollowUp(followUpId))
                                    },
                                    onLogMet = {
                                        val note = if (node is FutureTimelineNode.BirthdayNode) {
                                            "Celebrated ${node.contactName}'s Birthday"
                                        } else null
                                        onLogEncounterForContact(node.contactId, note)
                                    }
                                )
                            }
                        }
                    }

                    // TODAY Divider Anchor
                    if (showTodayDivider) {
                        item(key = "today-divider-anchor") {
                            TodayTimelineAnchor(
                                todayLabel = uiState.todayLabel,
                                hasPastBelow = uiState.pastNodes.isNotEmpty()
                            )
                        }
                    }

                    // Past Log Section (Newest-First, Solid Spine, First Met vs Met Again)
                    if (showPast) {
                        if (uiState.pastNodes.isEmpty()) {
                            item(key = "empty-past") {
                                EmptyTimelineStateCard(
                                    title = "No encounters logged yet",
                                    subtitle = "People you add or meet again will form your living timeline."
                                )
                            }
                        } else {
                            if (uiState.selectedFilter == TimelineFilter.PAST) {
                                item(key = "past-section-header") {
                                    TimelineSectionLabel(
                                        title = "PAST LOG",
                                        subtitle = "People you first met and caught up with again"
                                    )
                                }
                            }
                            itemsIndexed(
                                items = uiState.pastNodes,
                                key = { _, node -> node.id }
                            ) { index, node ->
                                val isFirstPast = index == 0
                                val isLastPast = index == uiState.pastNodes.lastIndex
                                val isSelected = node.id in uiState.selectedNodeIds

                                PastTimelineNodeRow(
                                    node = node,
                                    isFirst = isFirstPast,
                                    isLast = isLastPast,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                        } else {
                                            onContactClick(node.contactId)
                                        }
                                    },
                                    onLongClick = {
                                        haptics.perform(SemanticHapticType.EDIT_ENTRY)
                                        onEvent(TimelineUiEvent.OnNodeLongClick(node.id))
                                    }
                                )
                            }
                        }
                    }
                }

                // 16.dp vertical gradient scrim floating directly over the top of the scrolling list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    Color.Transparent
                                )
                            )
                        )
                )
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
                onDeleteClick = { onEvent(TimelineUiEvent.OnRequestDeleteSelected) },
                onSelectAllClick = { onEvent(TimelineUiEvent.OnSelectAllVisibleNodes) }
            )
        }

        // Batch Deletion Confirmation Dialog
        if (uiState.isDeleteConfirmationVisible) {
            val encCount = uiState.pastNodes.count { it.id in uiState.selectedNodeIds }
            val fuCount = uiState.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>().count { it.id in uiState.selectedNodeIds }
            val titleText = when {
                encCount > 0 && fuCount > 0 -> "Delete $encCount encounter${if (encCount > 1) "s" else ""} and $fuCount follow-up${if (fuCount > 1) "s" else ""}?"
                encCount > 0 -> "Delete $encCount encounter${if (encCount > 1) "s" else ""}?"
                fuCount > 0 -> "Delete $fuCount follow-up${if (fuCount > 1) "s" else ""}?"
                else -> "Delete selected items?"
            }

            AlertDialog(
                onDismissRequest = { onEvent(TimelineUiEvent.OnDismissDeleteDialog) },
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "These records will be permanently removed from your timeline and device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
                            onEvent(TimelineUiEvent.OnConfirmDeleteSelected)
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
                        onClick = { onEvent(TimelineUiEvent.OnDismissDeleteDialog) },
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

@Composable
private fun TimelineSelectionHeader(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        FilledIconButton(
            onClick = onClearSelection,
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
    }
}

@Composable
private fun TimelineHeader(
    selectedFilter: TimelineFilter,
    onSelectFilter: (TimelineFilter) -> Unit,
    onLogEncounterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onLogEncounterClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Log Met",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Ultra-minimal Temporal Filter Strip: All | Future | Past
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectFilter(filter) }
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineSectionLabel(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayTimelineAnchor(
    todayLabel: String,
    hasPastBelow: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spine Anchor Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }
            if (hasPastBelow) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = todayLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FutureTimelineNodeRow(
    node: FutureTimelineNode,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMarkDone: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onLogMet: () -> Unit
) {
    val spineColor = MaterialTheme.colorScheme.onSurface
    val dotFillColor = MaterialTheme.colorScheme.surface
    val dashColor = MaterialTheme.colorScheme.outlineVariant

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Dashed Future Spine & Centered Hollow Node Dot
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Dashed Spine Column: Top half down to center, bottom half down from center
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                ) {
                    val brush = if (isFirst) {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, dashColor),
                            startY = 0f,
                            endY = size.height
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(dashColor, dashColor)
                        )
                    }
                    drawLine(
                        brush = brush,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = size.width,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }
                if (!isLast) {
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                    ) {
                        drawLine(
                            color = dashColor,
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = size.width,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Hollow Center Node Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotFillColor)
                    .border(
                        width = 2.dp,
                        color = spineColor,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Future Node Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Contact Avatar + Name + Future Type Pill + Date Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        SelectableAvatarBadge(
                            isSelected = isSelected,
                            initials = node.avatarInitials,
                            bgIndex = node.avatarBgIndex,
                            size = 36.dp,
                            shape = CircleShape,
                            photoUri = node.photoUri
                        )
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = node.contactName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) secondaryContentColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FutureNodeTypeBadge(node = node)
                }

                // Body text
                when (node) {
                    is FutureTimelineNode.FollowUpNode -> {
                        Text(
                            text = node.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryContentColor,
                            lineHeight = 20.sp
                        )
                        if (!isSelectionMode) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { onMarkDone(node.followUpId) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Done",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onSnooze(node.followUpId) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Snooze,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Snooze",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    is FutureTimelineNode.BirthdayNode -> {
                        val isToday = node.daysUntil == 0L
                        val bdayDescription = when {
                            isToday && node.turningAge != null -> "Turning ${node.turningAge} today! Send warm wishes and celebrate"
                            isToday -> "Today is ${node.contactName}'s birthday! Celebrate together"
                            node.turningAge != null -> "Turning ${node.turningAge} · Send a warm birthday note"
                            else -> "Upcoming birthday · Send a warm birthday note"
                        }
                        Text(
                            text = bdayDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryContentColor,
                            lineHeight = 20.sp
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onLogMet,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Log Encounter",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureNodeTypeBadge(node: FutureTimelineNode) {
    val (icon, label) = when (node) {
        is FutureTimelineNode.FollowUpNode -> Icons.Rounded.NotificationsActive to "Next Note"
        is FutureTimelineNode.BirthdayNode -> Icons.Rounded.Cake to "Birthday"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PastTimelineNodeRow(
    node: PastTimelineNode,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isFirstMet = node.badge == PastEncounterBadge.FIRST_MET
    val spineColor = MaterialTheme.colorScheme.onSurface
    val dotFillColor = MaterialTheme.colorScheme.surface

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // High-Contrast Past Timeline Spine & Centered Hollow Node Dot
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Spine Column: Top half down to center, bottom half down from center
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val topBrush = if (isFirst) {
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, spineColor)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, spineColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(topBrush)
                )

                val bottomBrush = if (isLast) {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, Color.Transparent)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, spineColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(bottomBrush)
                )
            }

            // Hollow Center Node Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotFillColor)
                    .border(
                        width = 2.dp,
                        color = spineColor,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Past Encounter Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Avatar + Name + Date + First Met / Met Again Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        SelectableAvatarBadge(
                            isSelected = isSelected,
                            initials = node.avatarInitials,
                            bgIndex = node.avatarBgIndex,
                            size = 36.dp,
                            shape = CircleShape,
                            photoUri = node.photoUri
                        )
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = node.contactName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryContentColor
                            )
                        }
                    }

                    // First Met vs Met Again Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        } else if (isFirstMet) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = node.badge.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else if (isFirstMet) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Place Chip (if present)
                if (!node.placeLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = node.placeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = secondaryContentColor
                            )
                        }
                    }
                }

                // Encounter Notes
                if (node.notes.isNotBlank()) {
                    Text(
                        text = node.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryContentColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelineStateCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(name = "Timeline Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TimelineScreenLightPreview() {
    HelloTheme(darkTheme = false) {
        TimelineScreen(
            uiState = TimelineViewModel.createInitialState(),
            onEvent = {},
            onContactClick = {},
            onLogEncounterForContact = { _, _ -> },
            onLogEncounterClick = {}
        )
    }
}

@Preview(name = "Timeline Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
private fun TimelineScreenDarkPreview() {
    HelloTheme(darkTheme = true) {
        TimelineScreen(
            uiState = TimelineViewModel.createInitialState(),
            onEvent = {},
            onContactClick = {},
            onLogEncounterForContact = { _, _ -> },
            onLogEncounterClick = {}
        )
    }
}

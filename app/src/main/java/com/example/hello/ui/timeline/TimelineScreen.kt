package com.example.hello.ui.timeline

import androidx.annotation.DrawableRes
import com.example.hello.R
import com.example.hello.ui.components.EmptyStateIllustration

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.hello.ui.components.HelloDatePickerDialog
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.ParticipantAvatar
import com.example.hello.ui.components.SelectableAvatarBadge
import com.example.hello.ui.components.StackedAvatarBadges
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.predictiveSelectionTrayExit
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.timeline.components.TimelineCalendarHeader
import com.example.hello.ui.timeline.components.TimelineSelectionActionTray
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.LocalDate

private const val CONTENT_TYPE_QUIET_DAY = "timeline_quiet_day"
private const val CONTENT_TYPE_DAY_HEADER = "timeline_day_header"
private const val CONTENT_TYPE_FUTURE_FOLLOW_UP = "timeline_future_follow_up"
private const val CONTENT_TYPE_FUTURE_BIRTHDAY = "timeline_future_birthday"
private const val CONTENT_TYPE_TODAY_ANCHOR = "timeline_today_anchor"
private const val CONTENT_TYPE_PAST_ENCOUNTER = "timeline_past_encounter"
private const val CONTENT_TYPE_PAST_COMPLETED_REMINDER = "timeline_past_completed_reminder"
private const val CONTENT_TYPE_EMPTY_PAST = "timeline_empty_past"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    uiState: TimelineUiState,
    onEvent: (TimelineUiEvent) -> Unit,
    onContactClick: (String) -> Unit,
    onLogEncounterForContact: (String, String?) -> Unit,
    onLogEncounterClick: () -> Unit,
    selectionBackProgress: Float = 0f,
    registerSelectionBackHandler: Boolean = true,
    modifier: Modifier = Modifier
) {
    val today = uiState.today
    val futureGroups = remember(uiState.futureDayGroups, uiState.futureNodes, today) {
        uiState.futureDayGroups.ifEmpty { TimelineViewModel.groupFutureNodesByDay(uiState.futureNodes, today) }
    }
    val pastGroups = remember(uiState.pastDayGroups, uiState.pastNodes, today) {
        uiState.pastDayGroups.ifEmpty { TimelineViewModel.groupPastNodesByDay(uiState.pastNodes, today) }
    }
    val futureAboveTodayGroups = remember(futureGroups, today) {
        futureGroups.filter { it.date.isAfter(today) }
    }
    val todayFutureItems = remember(futureGroups, today) {
        futureGroups.firstOrNull { it.date == today }?.items.orEmpty()
    }
    val todayPastItems = remember(pastGroups, today) {
        pastGroups.firstOrNull { it.date == today }?.items.orEmpty()
    }
    val pastGroupsByDate = remember(pastGroups) {
        pastGroups.associateBy { it.date }
    }
    val pastBelowTodayDates = remember(pastGroups, uiState.pastBirthdaysByDate, today) {
        (pastGroups.map { it.date } + uiState.pastBirthdaysByDate.keys)
            .filter { it.isBefore(today) }
            .distinct()
            .sortedDescending()
    }
    val showQuietFuture = uiState.selectedDate.isAfter(today) &&
        futureAboveTodayGroups.none { it.date == uiState.selectedDate }
    val showQuietPast = uiState.selectedDate.isBefore(today) &&
        pastBelowTodayDates.none { it == uiState.selectedDate }

    // Build deterministic key -> LocalDate map & ordered key list for two-way scroll sync
    val (orderedKeys, dateByKey) = remember(
        futureAboveTodayGroups,
        todayFutureItems,
        todayPastItems,
        pastBelowTodayDates,
        pastGroupsByDate,
        uiState.pastBirthdaysByDate,
        showQuietFuture,
        showQuietPast,
        uiState.selectedDate,
        today
    ) {
        val keys = mutableListOf<Any>()
        val map = mutableMapOf<Any, LocalDate>()
        var insertedQuietFuture = false

        futureAboveTodayGroups.forEach { group ->
            if (showQuietFuture && !insertedQuietFuture && uiState.selectedDate.isAfter(group.date)) {
                val qKey = "quiet-${uiState.selectedDate}"
                keys += qKey
                map[qKey] = uiState.selectedDate
                insertedQuietFuture = true
            }
            val hKey = "future-header-${group.date}"
            keys += hKey
            map[hKey] = group.date
            group.items.forEach { node ->
                keys += node.id
                map[node.id] = group.date
            }
        }
        if (showQuietFuture && !insertedQuietFuture) {
            val qKey = "quiet-${uiState.selectedDate}"
            keys += qKey
            map[qKey] = uiState.selectedDate
        }

        val todayKey = "today-divider-anchor"
        keys += todayKey
        map[todayKey] = today

        todayFutureItems.forEach { node ->
            keys += node.id
            map[node.id] = today
        }
        todayPastItems.forEach { node ->
            keys += node.id
            map[node.id] = today
        }

        var insertedQuietPast = false
        pastBelowTodayDates.forEach { date ->
            if (showQuietPast && !insertedQuietPast && uiState.selectedDate.isAfter(date)) {
                val qKey = "quiet-${uiState.selectedDate}"
                keys += qKey
                map[qKey] = uiState.selectedDate
                insertedQuietPast = true
            }
            val hKey = "past-header-$date"
            keys += hKey
            map[hKey] = date
            uiState.pastBirthdaysByDate[date].orEmpty().forEach { bday ->
                keys += bday.id
                map[bday.id] = date
            }
            pastGroupsByDate[date]?.items.orEmpty().forEach { node ->
                keys += node.id
                map[node.id] = date
            }
        }
        if (showQuietPast && !insertedQuietPast) {
            val qKey = "quiet-${uiState.selectedDate}"
            keys += qKey
            map[qKey] = uiState.selectedDate
        }

        keys.toList() to map.toMap()
    }

    val listState = rememberLazyListState()
    val haptics = LocalHelloHaptics.current
    val isReducedMotion = LocalReducedMotion.current
    var localSelectionBackProgress by remember { mutableFloatStateOf(0f) }
    var chooserParticipants by remember { mutableStateOf<List<ParticipantAvatar>?>(null) }
    var hasAnchoredInitially by remember { mutableStateOf(false) }
    val effectiveSelectionBackProgress = if (registerSelectionBackHandler) localSelectionBackProgress else selectionBackProgress

    val latestSelectedDate = rememberUpdatedState(uiState.selectedDate)
    val latestDateByKey = rememberUpdatedState(dateByKey)
    val latestOnEvent = rememberUpdatedState(onEvent)

    // Initial anchor at TODAY on first composition
    LaunchedEffect(Unit) {
        if (!hasAnchoredInitially) {
            val todayIdx = orderedKeys.indexOf("today-divider-anchor")
            if (todayIdx >= 0) {
                listState.scrollToItem(todayIdx)
            }
            hasAnchoredInitially = true
        }
    }

    // Programmatic scroll when header changes selectedDate (tap, arrows, week swipe, picker, Today pill)
    LaunchedEffect(uiState.scrollRequestId) {
        if (uiState.scrollRequestId > 0L) {
            val targetDate = uiState.selectedDate
            val targetIdx = if (targetDate == today) {
                orderedKeys.indexOf("today-divider-anchor")
            } else {
                orderedKeys.indexOfFirst { dateByKey[it] == targetDate }
            }
            if (targetIdx >= 0) {
                if (isReducedMotion) {
                    listState.scrollToItem(targetIdx)
                } else {
                    listState.animateScrollToItem(targetIdx)
                }
            }
        }
    }

    // Passive scroll-to-header sync during user drag/fling
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
        }
            .filter { (isScrolling, _) -> isScrolling }
            .distinctUntilChanged()
            .collect { (_, topKey) ->
                val visibleDate = topKey?.let { latestDateByKey.value[it] } ?: return@collect
                if (visibleDate != latestSelectedDate.value) {
                    latestOnEvent.value(TimelineUiEvent.OnSelectDate(visibleDate, fromScroll = true))
                }
            }
    }

    if (registerSelectionBackHandler) {
        ScrubbablePredictiveBackHandler(
            enabled = uiState.isSelectionMode,
            isReducedMotion = isReducedMotion,
            onProgressChanged = { localSelectionBackProgress = it },
            onCommit = { onEvent(TimelineUiEvent.OnClearSelection) }
        )
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
            // 1. Interactive Timeline Calendar Header / Selection Contextual Header
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
                    TimelineCalendarHeader(
                        selectedDate = uiState.selectedDate,
                        today = today,
                        firstDayOfWeek = uiState.firstDayOfWeek,
                        activeEntryDates = uiState.activeEntryDates,
                        recurringBirthdayMonthDays = uiState.recurringBirthdayMonthDays,
                        onSelectDate = { onEvent(TimelineUiEvent.OnSelectDate(it, fromScroll = false)) },
                        onStepDay = { onEvent(TimelineUiEvent.OnStepDay(it)) },
                        onStepWeek = { onEvent(TimelineUiEvent.OnStepWeek(it)) },
                        onJumpToToday = { onEvent(TimelineUiEvent.OnJumpToToday) },
                        onOpenDatePicker = { onEvent(TimelineUiEvent.OnOpenDatePicker) }
                    )
                }
            }

            // 2. Continuous Monotonic Descending Living Timeline Spine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = 128.dp
                    )
                ) {
                    // Future Horizon Section (Monotonic Descending: Furthest Future -> Tomorrow)
                    var insertedQuietFutureInList = false
                    futureAboveTodayGroups.forEachIndexed { groupIndex, group ->
                        if (showQuietFuture && !insertedQuietFutureInList && uiState.selectedDate.isAfter(group.date)) {
                            item(key = "quiet-${uiState.selectedDate}", contentType = CONTENT_TYPE_QUIET_DAY) {
                                QuietDayTimelineRow(
                                    headerLabel = TimelineViewModel.formatDayHeaderLabel(uiState.selectedDate, today),
                                    isFuture = true
                                )
                            }
                            insertedQuietFutureInList = true
                        }
                        stickyHeader(key = "future-header-${group.date}", contentType = CONTENT_TYPE_DAY_HEADER) {
                            TimelineDayStickyHeader(
                                label = group.headerLabel,
                                isFuture = true
                            )
                        }
                        itemsIndexed(
                            items = group.items,
                            key = { _, node -> node.id },
                            contentType = { _, node ->
                                when (node) {
                                    is FutureTimelineNode.FollowUpNode -> CONTENT_TYPE_FUTURE_FOLLOW_UP
                                    is FutureTimelineNode.BirthdayNode -> CONTENT_TYPE_FUTURE_BIRTHDAY
                                }
                            }
                        ) { index, node ->
                            val isFirstInGroup = index == 0
                            val isLastInGroup = index == group.items.lastIndex && groupIndex < futureAboveTodayGroups.lastIndex
                            val isSelected = node.id in uiState.selectedNodeIds
                            val isFollowUp = node is FutureTimelineNode.FollowUpNode

                            FutureTimelineNodeRow(
                                node = node,
                                isFirst = isFirstInGroup,
                                isLast = isLastInGroup,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        if (isFollowUp) {
                                            onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                        }
                                    } else if (node is FutureTimelineNode.FollowUpNode && node.participants.size > 1) {
                                        chooserParticipants = node.participants
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
                    if (showQuietFuture && !insertedQuietFutureInList) {
                        item(key = "quiet-${uiState.selectedDate}", contentType = CONTENT_TYPE_QUIET_DAY) {
                            QuietDayTimelineRow(
                                headerLabel = TimelineViewModel.formatDayHeaderLabel(uiState.selectedDate, today),
                                isFuture = true
                            )
                        }
                    }

                    // TODAY Divider Anchor
                    item(key = "today-divider-anchor", contentType = CONTENT_TYPE_TODAY_ANCHOR) {
                        TodayTimelineAnchor(
                            todayLabel = uiState.todayLabel,
                            hasPastBelow = todayFutureItems.isNotEmpty() || todayPastItems.isNotEmpty() || pastBelowTodayDates.isNotEmpty()
                        )
                    }

                    // Today's scheduled items & encounters
                    itemsIndexed(
                        items = todayFutureItems,
                        key = { _, node -> node.id },
                        contentType = { _, node ->
                            when (node) {
                                is FutureTimelineNode.FollowUpNode -> CONTENT_TYPE_FUTURE_FOLLOW_UP
                                is FutureTimelineNode.BirthdayNode -> CONTENT_TYPE_FUTURE_BIRTHDAY
                            }
                        }
                    ) { index, node ->
                        val isSelected = node.id in uiState.selectedNodeIds
                        val isFollowUp = node is FutureTimelineNode.FollowUpNode
                        FutureTimelineNodeRow(
                            node = node,
                            isFirst = index == 0,
                            isLast = index == todayFutureItems.lastIndex && todayPastItems.isEmpty(),
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    if (isFollowUp) onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                } else if (node is FutureTimelineNode.FollowUpNode && node.participants.size > 1) {
                                    chooserParticipants = node.participants
                                } else {
                                    onContactClick(node.contactId)
                                }
                            },
                            onLongClick = if (isFollowUp) {
                                { onEvent(TimelineUiEvent.OnNodeLongClick(node.id)) }
                            } else null,
                            onMarkDone = { onEvent(TimelineUiEvent.OnMarkFollowUpDone(it)) },
                            onSnooze = { onEvent(TimelineUiEvent.OnSnoozeFollowUp(it)) },
                            onLogMet = {
                                val note = if (node is FutureTimelineNode.BirthdayNode) {
                                    "Celebrated ${node.contactName}'s Birthday"
                                } else null
                                onLogEncounterForContact(node.contactId, note)
                            }
                        )
                    }

                    itemsIndexed(
                        items = todayPastItems,
                        key = { _, node -> node.id },
                        contentType = { _, node ->
                            if (node.isCompletedReminder) CONTENT_TYPE_PAST_COMPLETED_REMINDER else CONTENT_TYPE_PAST_ENCOUNTER
                        }
                    ) { index, node ->
                        val isSelected = node.id in uiState.selectedNodeIds
                        PastTimelineNodeRow(
                            node = node,
                            isFirst = index == 0 && todayFutureItems.isEmpty(),
                            isLast = index == todayPastItems.lastIndex && pastBelowTodayDates.isEmpty(),
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                } else if (node.participants.size > 1) {
                                    chooserParticipants = node.participants
                                } else {
                                    onContactClick(node.contactId)
                                }
                            },
                            onLongClick = {
                                haptics.perform(SemanticHapticType.EDIT_ENTRY)
                                onEvent(TimelineUiEvent.OnNodeLongClick(node.id))
                            },
                            onReopen = { followUpId ->
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(TimelineUiEvent.OnReopenFollowUp(followUpId))
                            }
                        )
                    }

                    // Past Log Section (Newest-First, Solid Spine, Grouped by Day)
                    if (pastBelowTodayDates.isEmpty() && todayPastItems.isEmpty() && todayFutureItems.isEmpty() && futureAboveTodayGroups.isEmpty() && !showQuietPast) {
                        item(key = "empty-past", contentType = CONTENT_TYPE_EMPTY_PAST) {
                            EmptyTimelineStateCard(
                                title = "No encounters logged yet",
                                subtitle = "People you add or meet again will form your living timeline.",
                                illustrationRes = R.drawable.ic_empty_timeline_past,
                                illustrationHeight = 112.dp
                            )
                        }
                    } else {
                        var insertedQuietPastInList = false
                        pastBelowTodayDates.forEach { date ->
                            if (showQuietPast && !insertedQuietPastInList && uiState.selectedDate.isAfter(date)) {
                                item(key = "quiet-${uiState.selectedDate}", contentType = CONTENT_TYPE_QUIET_DAY) {
                                    QuietDayTimelineRow(
                                        headerLabel = TimelineViewModel.formatDayHeaderLabel(uiState.selectedDate, today),
                                        isFuture = false
                                    )
                                }
                                insertedQuietPastInList = true
                            }
                            val dayLabel = pastGroupsByDate[date]?.headerLabel
                                ?: TimelineViewModel.formatDayHeaderLabel(date, today)
                            stickyHeader(key = "past-header-$date", contentType = CONTENT_TYPE_DAY_HEADER) {
                                TimelineDayStickyHeader(
                                    label = dayLabel,
                                    isFuture = false
                                )
                            }
                            val bdays = uiState.pastBirthdaysByDate[date].orEmpty()
                            val pastItems = pastGroupsByDate[date]?.items.orEmpty()
                            itemsIndexed(
                                items = bdays,
                                key = { _, bday -> bday.id },
                                contentType = { _, _ -> CONTENT_TYPE_FUTURE_BIRTHDAY }
                            ) { index, bday ->
                                FutureTimelineNodeRow(
                                    node = bday,
                                    isFirst = index == 0,
                                    isLast = index == bdays.lastIndex && pastItems.isEmpty(),
                                    isSelected = false,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = { onContactClick(bday.contactId) },
                                    onLongClick = null,
                                    onMarkDone = {},
                                    onSnooze = {},
                                    onLogMet = {
                                        onLogEncounterForContact(bday.contactId, "Celebrated ${bday.contactName}'s Birthday")
                                    }
                                )
                            }
                            itemsIndexed(
                                items = pastItems,
                                key = { _, node -> node.id },
                                contentType = { _, node ->
                                    if (node.isCompletedReminder) CONTENT_TYPE_PAST_COMPLETED_REMINDER else CONTENT_TYPE_PAST_ENCOUNTER
                                }
                            ) { index, node ->
                                val isFirstInGroup = index == 0 && bdays.isEmpty()
                                val isLastInGroup = index == pastItems.lastIndex
                                val isSelected = node.id in uiState.selectedNodeIds

                                PastTimelineNodeRow(
                                    node = node,
                                    isFirst = isFirstInGroup,
                                    isLast = isLastInGroup,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onEvent(TimelineUiEvent.OnToggleSelectNode(node.id))
                                        } else if (node.participants.size > 1) {
                                            chooserParticipants = node.participants
                                        } else {
                                            onContactClick(node.contactId)
                                        }
                                    },
                                    onLongClick = {
                                        haptics.perform(SemanticHapticType.EDIT_ENTRY)
                                        onEvent(TimelineUiEvent.OnNodeLongClick(node.id))
                                    },
                                    onReopen = { followUpId ->
                                        haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                        onEvent(TimelineUiEvent.OnReopenFollowUp(followUpId))
                                    }
                                )
                            }
                        }
                        if (showQuietPast && !insertedQuietPastInList) {
                            item(key = "quiet-${uiState.selectedDate}", contentType = CONTENT_TYPE_QUIET_DAY) {
                                QuietDayTimelineRow(
                                    headerLabel = TimelineViewModel.formatDayHeaderLabel(uiState.selectedDate, today),
                                    isFuture = false
                                )
                            }
                        }
                    }
                }
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
                .predictiveSelectionTrayExit { effectiveSelectionBackProgress }
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

        // Quick profile chooser when tapping a multi-person Timeline card
        chooserParticipants?.let { people ->
            AlertDialog(
                onDismissRequest = { chooserParticipants = null },
                title = {
                    Text(
                        text = "Open profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        people.forEach { person ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable {
                                        chooserParticipants = null
                                        onContactClick(person.contactId)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SelectableAvatarBadge(
                                    isSelected = false,
                                    initials = person.initials,
                                    bgIndex = person.bgIndex,
                                    size = 32.dp,
                                    photoUri = person.photoUri
                                )
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { chooserParticipants = null }) {
                        Text("Close")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        if (uiState.isDatePickerOpen) {
            HelloDatePickerDialog(
                initialDate = uiState.selectedDate,
                restrictToPastDates = false,
                onConfirm = { pickedDate ->
                    onEvent(TimelineUiEvent.OnSelectDate(pickedDate, fromScroll = false))
                },
                onDismissRequest = {
                    onEvent(TimelineUiEvent.OnDismissDatePicker)
                }
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
private fun QuietDayTimelineRow(
    headerLabel: String,
    isFuture: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TimelineDayStickyHeader(label = headerLabel, isFuture = isFuture)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Quiet day — nothing scheduled or logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineDayStickyHeader(
    label: String,
    isFuture: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFuture) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
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
                        val futureParticipants = (node as? FutureTimelineNode.FollowUpNode)?.participants.orEmpty()
                        if (futureParticipants.size > 1 && !isSelected) {
                            StackedAvatarBadges(
                                participants = futureParticipants,
                                isSelected = false,
                                fallbackInitials = node.avatarInitials,
                                fallbackBgIndex = node.avatarBgIndex,
                                fallbackPhotoUri = node.photoUri,
                                size = 36.dp,
                                ringColor = containerColor
                            )
                        } else {
                            SelectableAvatarBadge(
                                isSelected = isSelected,
                                initials = node.avatarInitials,
                                bgIndex = node.avatarBgIndex,
                                size = 36.dp,
                                shape = CircleShape,
                                photoUri = node.photoUri
                            )
                        }
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
                            node.turningAge != null -> "Turning ${node.turningAge}. Send a warm birthday note"
                            else -> "Upcoming birthday. Send a warm birthday note"
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
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onReopen: ((String) -> Unit)? = null
) {
    val isFirstMet = node.badge == PastEncounterBadge.FIRST_MET
    val isCompletedReminder = node.isCompletedReminder
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
                        if (node.participants.size > 1 && !isSelected) {
                            StackedAvatarBadges(
                                participants = node.participants,
                                isSelected = false,
                                fallbackInitials = node.avatarInitials,
                                fallbackBgIndex = node.avatarBgIndex,
                                fallbackPhotoUri = node.photoUri,
                                size = 36.dp,
                                ringColor = containerColor
                            )
                        } else {
                            SelectableAvatarBadge(
                                isSelected = isSelected,
                                initials = node.avatarInitials,
                                bgIndex = node.avatarBgIndex,
                                size = 36.dp,
                                shape = CircleShape,
                                photoUri = node.photoUri
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = node.contactName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.timeLabel.ifBlank { node.dateLabel },
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryContentColor
                            )
                        }
                    }

                    // First Met / Met Again / Completed Badge
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
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isCompletedReminder) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = node.badge.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else if (isFirstMet) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
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

                // Encounter Notes or Completed Reminder Note + Inline Reopen
                if (isCompletedReminder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (node.notes.isNotBlank()) {
                            Text(
                                text = node.notes,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                ),
                                color = secondaryContentColor,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        val followUpId = node.followUpId
                        if (!isSelectionMode && followUpId != null && onReopen != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = { onReopen(followUpId) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay,
                                        contentDescription = "Reopen reminder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Reopen",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (node.notes.isNotBlank()) {
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
    subtitle: String,
    @DrawableRes illustrationRes: Int,
    illustrationHeight: androidx.compose.ui.unit.Dp = 112.dp
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
            EmptyStateIllustration(
                drawableRes = illustrationRes,
                height = illustrationHeight
            )
            Spacer(modifier = Modifier.height(14.dp))
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

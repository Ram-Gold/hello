package com.example.hello.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.domain.model.formatParticipantNames
import com.example.hello.ui.components.BirthdayInputUtils
import com.example.hello.ui.components.ParticipantAvatar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TimelineViewModel(
    val repository: ContactRepository? = null,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private data class TimelineSourceSnapshot(
        val contactsMap: Map<String, Contact>,
        val encounters: List<Encounter>,
        val placesMap: Map<String, Place>,
        val followUps: List<FollowUp>
    )

    private var latestSource: TimelineSourceSnapshot = if (repository == null) {
        TimelineSourceSnapshot(
            contactsMap = MockDataProvider.contacts.associateBy { it.id },
            encounters = MockDataProvider.encounters,
            placesMap = MockDataProvider.places.associateBy { it.id },
            followUps = MockDataProvider.followUps
        )
    } else {
        TimelineSourceSnapshot(emptyMap(), emptyList(), emptyMap(), emptyList())
    }

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        if (preferencesRepository != null) {
            viewModelScope.launch {
                preferencesRepository.userPreferencesFlow.collect { prefs ->
                    _uiState.update { it.copy(firstDayOfWeek = prefs.firstDayOfWeek) }
                }
            }
        }

        if (repository != null) {
            viewModelScope.launch {
                combine(
                    repository.fullContacts,
                    repository.encounters,
                    repository.places,
                    repository.followUps
                ) { contactsMap, encounters, places, followUps ->
                    TimelineSourceSnapshot(
                        contactsMap = contactsMap,
                        encounters = encounters,
                        placesMap = places.associateBy { it.id },
                        followUps = followUps
                    )
                }.collect { snapshot ->
                    latestSource = snapshot
                    recomputeTimelineState(anchorDate = _uiState.value.selectedDate)
                }
            }
        }
    }

    private fun recomputeTimelineState(
        anchorDate: LocalDate,
        incrementScrollRequest: Boolean = false
    ) {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val (futureNodes, pastNodes) = buildTimelineNodes(
            contactsMap = latestSource.contactsMap,
            encounters = latestSource.encounters,
            placesMap = latestSource.placesMap,
            followUps = latestSource.followUps,
            today = today,
            zoneId = zoneId,
            anchorDate = anchorDate
        )
        val pastBirthdays = buildPastBirthdaysForWindow(
            contactsMap = latestSource.contactsMap,
            anchorDate = anchorDate,
            today = today
        )
        val activeDates = computeActiveEntryDates(futureNodes, pastNodes, pastBirthdays, zoneId)
        val recurringBirthdays = computeRecurringBirthdayMonthDays(latestSource.contactsMap)

        _uiState.update { state ->
            val allNodeIds = (futureNodes.map { it.id } + pastNodes.map { it.id }).toSet()
            val prunedSelected = state.selectedNodeIds.filter { it in allNodeIds }.toSet()
            state.copy(
                today = today,
                selectedDate = anchorDate,
                scrollRequestId = if (incrementScrollRequest) state.scrollRequestId + 1L else state.scrollRequestId,
                futureNodes = futureNodes,
                pastNodes = pastNodes,
                futureDayGroups = groupFutureNodesByDay(futureNodes, today),
                pastDayGroups = groupPastNodesByDay(pastNodes, today, zoneId),
                pastBirthdaysByDate = pastBirthdays,
                activeEntryDates = activeDates,
                recurringBirthdayMonthDays = recurringBirthdays,
                selectedNodeIds = prunedSelected,
                todayLabel = formatTodayDividerLabel(today)
            )
        }
    }

    private fun selectDate(date: LocalDate, fromScroll: Boolean) {
        val current = _uiState.value
        val needsRematerialize = date != current.selectedDate &&
            (date.isBefore(current.today) || date.isAfter(current.today.plusMonths(11)))
        if (needsRematerialize) {
            recomputeTimelineState(anchorDate = date, incrementScrollRequest = !fromScroll)
        } else {
            _uiState.update { state ->
                state.copy(
                    selectedDate = date,
                    scrollRequestId = if (!fromScroll) state.scrollRequestId + 1L else state.scrollRequestId
                )
            }
        }
    }

    fun onEvent(event: TimelineUiEvent) {
        when (event) {
            is TimelineUiEvent.OnSelectDate -> {
                selectDate(date = event.date, fromScroll = event.fromScroll)
            }
            is TimelineUiEvent.OnStepDay -> {
                selectDate(
                    date = _uiState.value.selectedDate.plusDays(event.deltaDays),
                    fromScroll = false
                )
            }
            is TimelineUiEvent.OnStepWeek -> {
                selectDate(
                    date = _uiState.value.selectedDate.plusWeeks(event.deltaWeeks),
                    fromScroll = false
                )
            }
            is TimelineUiEvent.OnJumpToToday -> {
                selectDate(date = _uiState.value.today, fromScroll = false)
            }
            is TimelineUiEvent.OnOpenDatePicker -> {
                _uiState.update { it.copy(isDatePickerOpen = true) }
            }
            is TimelineUiEvent.OnDismissDatePicker -> {
                _uiState.update { it.copy(isDatePickerOpen = false) }
            }
            is TimelineUiEvent.OnMarkFollowUpDone -> {
                val now = Instant.now()
                repository?.markFollowUpDone(event.followUpId, doneAt = now)
                _uiState.update { state ->
                    val completedFuture = state.futureNodes
                        .filterIsInstance<FutureTimelineNode.FollowUpNode>()
                        .firstOrNull { it.followUpId == event.followUpId }
                    val updatedFuture = state.futureNodes.filterNot { node ->
                        node is FutureTimelineNode.FollowUpNode && node.followUpId == event.followUpId
                    }
                    val completedNodeId = "completed-followup-${event.followUpId}"
                    val updatedPast = if (completedFuture != null && state.pastNodes.none { it.id == completedNodeId }) {
                        val completedPastNode = PastTimelineNode(
                            id = completedNodeId,
                            contactId = completedFuture.contactId,
                            contactName = completedFuture.contactName,
                            avatarInitials = completedFuture.avatarInitials,
                            avatarBgIndex = completedFuture.avatarBgIndex,
                            occurredAt = now,
                            dateLabel = formatCompletedDateLabel(now, state.today),
                            placeLabel = null,
                            notes = completedFuture.note.ifBlank { "Scheduled reminder" },
                            badge = PastEncounterBadge.COMPLETED_REMINDER,
                            photoUri = completedFuture.photoUri,
                            timeLabel = formatPastTimeLabel(now),
                            participants = completedFuture.participants,
                            followUpId = completedFuture.followUpId
                        )
                        (listOf(completedPastNode) + state.pastNodes)
                            .sortedWith(compareByDescending<PastTimelineNode> { it.occurredAt }.thenBy { it.id })
                    } else {
                        state.pastNodes
                    }
                    state.copy(
                        futureNodes = updatedFuture,
                        pastNodes = updatedPast,
                        futureDayGroups = groupFutureNodesByDay(updatedFuture, state.today),
                        pastDayGroups = groupPastNodesByDay(updatedPast, state.today),
                        activeEntryDates = computeActiveEntryDates(updatedFuture, updatedPast, state.pastBirthdaysByDate),
                        userMessage = "Reminder completed"
                    )
                }
            }
            is TimelineUiEvent.OnReopenFollowUp -> {
                repository?.reopenFollowUp(event.followUpId)
                _uiState.update { state ->
                    val completedNodeId = "completed-followup-${event.followUpId}"
                    val updatedPast = if (repository == null) {
                        state.pastNodes.filterNot { it.id == completedNodeId || it.followUpId == event.followUpId }
                    } else {
                        state.pastNodes
                    }
                    state.copy(
                        pastNodes = updatedPast,
                        pastDayGroups = groupPastNodesByDay(updatedPast, state.today),
                        activeEntryDates = computeActiveEntryDates(state.futureNodes, updatedPast, state.pastBirthdaysByDate),
                        userMessage = "Reminder reopened"
                    )
                }
            }
            is TimelineUiEvent.OnSnoozeFollowUp -> {
                repository?.snoozeFollowUp(event.followUpId)
                _uiState.update { state ->
                    val today = state.today
                    val updatedFuture = state.futureNodes.map { node ->
                        if (node is FutureTimelineNode.FollowUpNode && node.followUpId == event.followUpId) {
                            val newDays = (node.daysUntil + 3).coerceAtLeast(3)
                            node.copy(
                                daysUntil = newDays,
                                dateLabel = formatFutureDaysLabel(newDays),
                                targetDate = today.plusDays(newDays)
                            )
                        } else {
                            node
                        }
                    }.sortedBy { it.daysUntil }
                    state.copy(
                        futureNodes = updatedFuture,
                        futureDayGroups = groupFutureNodesByDay(updatedFuture, today),
                        activeEntryDates = computeActiveEntryDates(updatedFuture, state.pastNodes, state.pastBirthdaysByDate),
                        userMessage = "Snoozed for 3 days"
                    )
                }
            }
            is TimelineUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
            is TimelineUiEvent.OnNodeLongClick -> {
                _uiState.update { state ->
                    val selectable = getSelectableNodeIds(state)
                    if (event.nodeId in selectable) {
                        state.copy(selectedNodeIds = state.selectedNodeIds + event.nodeId)
                    } else {
                        state
                    }
                }
            }
            is TimelineUiEvent.OnToggleSelectNode -> {
                _uiState.update { state ->
                    val selectable = getSelectableNodeIds(state)
                    if (event.nodeId in selectable) {
                        val next = if (event.nodeId in state.selectedNodeIds) {
                            state.selectedNodeIds - event.nodeId
                        } else {
                            state.selectedNodeIds + event.nodeId
                        }
                        state.copy(selectedNodeIds = next)
                    } else {
                        state
                    }
                }
            }
            is TimelineUiEvent.OnClearSelection -> {
                _uiState.update { it.copy(selectedNodeIds = emptySet(), isDeleteConfirmationVisible = false) }
            }
            is TimelineUiEvent.OnSelectAllVisibleNodes -> {
                _uiState.update { state ->
                    state.copy(selectedNodeIds = getSelectableNodeIds(state))
                }
            }
            is TimelineUiEvent.OnRequestDeleteSelected -> {
                if (_uiState.value.selectedNodeIds.isNotEmpty()) {
                    _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
                }
            }
            is TimelineUiEvent.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
            }
            is TimelineUiEvent.OnConfirmDeleteSelected -> {
                val state = _uiState.value
                val selected = state.selectedNodeIds
                if (selected.isEmpty()) return

                val encounterIds = state.pastNodes
                    .filter { !it.isCompletedReminder && it.id in selected }
                    .map { it.id }
                    .toSet()
                val futureFollowUpIds = state.futureNodes
                    .filterIsInstance<FutureTimelineNode.FollowUpNode>()
                    .filter { it.id in selected }
                    .map { it.followUpId }
                val completedFollowUpIds = state.pastNodes
                    .filter { it.isCompletedReminder && it.id in selected }
                    .mapNotNull { it.followUpId }
                val followUpIds = (futureFollowUpIds + completedFollowUpIds).toSet()

                repository?.deleteEncounters(encounterIds)
                repository?.deleteFollowUps(followUpIds)

                val remainingFuture = state.futureNodes.filterNot { it.id in selected }
                val remainingPast = state.pastNodes.filterNot { it.id in selected }

                val encCount = encounterIds.size
                val fuCount = followUpIds.size
                val message = when {
                    encCount > 0 && fuCount > 0 -> "Deleted $encCount encounter${if (encCount > 1) "s" else ""} and $fuCount follow-up${if (fuCount > 1) "s" else ""}"
                    encCount > 0 -> "Deleted $encCount encounter${if (encCount > 1) "s" else ""}"
                    fuCount > 0 -> "Deleted $fuCount follow-up${if (fuCount > 1) "s" else ""}"
                    else -> "Deleted selected items"
                }

                _uiState.update { current ->
                    val nextFuture = if (repository != null) current.futureNodes else remainingFuture
                    val nextPast = if (repository != null) current.pastNodes else remainingPast
                    current.copy(
                        futureNodes = nextFuture,
                        pastNodes = nextPast,
                        futureDayGroups = groupFutureNodesByDay(nextFuture, current.today),
                        pastDayGroups = groupPastNodesByDay(nextPast, current.today),
                        activeEntryDates = computeActiveEntryDates(nextFuture, nextPast, current.pastBirthdaysByDate),
                        selectedNodeIds = emptySet(),
                        isDeleteConfirmationVisible = false,
                        userMessage = message
                    )
                }
            }
        }
    }

    private fun getSelectableNodeIds(state: TimelineUiState): Set<String> {
        val encounters = state.pastNodes.map { it.id }
        val followUps = state.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>().map { it.id }
        return (encounters + followUps).toSet()
    }

    companion object {
        fun createInitialState(
            today: LocalDate = LocalDate.now(),
            firstDayOfWeek: WeekStartDay = WeekStartDay.MONDAY
        ): TimelineUiState {
            val contactsMap = MockDataProvider.contacts.associateBy { it.id }
            val placesMap = MockDataProvider.places.associateBy { it.id }
            val (futureNodes, pastNodes) = buildTimelineNodes(
                contactsMap = contactsMap,
                encounters = MockDataProvider.encounters,
                placesMap = placesMap,
                followUps = MockDataProvider.followUps,
                today = today,
                anchorDate = today
            )
            val pastBirthdays = buildPastBirthdaysForWindow(contactsMap, today, today)
            return TimelineUiState(
                today = today,
                selectedDate = today,
                firstDayOfWeek = firstDayOfWeek,
                futureNodes = futureNodes,
                pastNodes = pastNodes,
                futureDayGroups = groupFutureNodesByDay(futureNodes, today),
                pastDayGroups = groupPastNodesByDay(pastNodes, today),
                pastBirthdaysByDate = pastBirthdays,
                activeEntryDates = computeActiveEntryDates(futureNodes, pastNodes, pastBirthdays),
                recurringBirthdayMonthDays = computeRecurringBirthdayMonthDays(contactsMap),
                todayLabel = formatTodayDividerLabel(today)
            )
        }

        fun buildTimelineNodes(
            contactsMap: Map<String, Contact>,
            encounters: List<Encounter>,
            placesMap: Map<String, Place>,
            followUps: List<FollowUp>,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            anchorDate: LocalDate = today
        ): Pair<List<FutureTimelineNode>, List<PastTimelineNode>> {
            val activeContacts = contactsMap.values.filterNot { it.isArchived }
            val activeContactIds = activeContacts.map { it.id }.toSet()
            val avatarIndexByContactId = activeContacts
                .sortedBy { it.id }
                .mapIndexed { idx, c -> c.id to (idx % 6) }
                .toMap()

            // 1. Past Nodes: Determine earliest chronological encounter per Contact -> FIRST_MET vs MET_AGAIN
            val validEncounters = encounters.filter { enc ->
                enc.resolvedParticipantIds().any { it in activeContactIds }
            }
            val earliestEncounterIdPerContact = mutableMapOf<String, String>()
            activeContactIds.forEach { cid ->
                val earliest = validEncounters
                    .filter { cid in it.resolvedParticipantIds() }
                    .minWithOrNull(compareBy<Encounter> { it.occurredAt }.thenBy { it.id })
                if (earliest != null) {
                    earliestEncounterIdPerContact[cid] = earliest.id
                }
            }

            fun buildParticipantAvatars(participantIds: List<String>): List<ParticipantAvatar> =
                participantIds.mapNotNull { pid ->
                    val c = contactsMap[pid]?.takeIf { !it.isArchived } ?: return@mapNotNull null
                    ParticipantAvatar(
                        contactId = c.id,
                        name = c.name,
                        initials = extractInitials(c.name),
                        bgIndex = avatarIndexByContactId[c.id] ?: 0,
                        photoUri = c.photoUri
                    )
                }

            val encounterPastNodes = validEncounters
                .mapNotNull { encounter ->
                    val participants = buildParticipantAvatars(encounter.resolvedParticipantIds())
                    val primary = participants.firstOrNull() ?: return@mapNotNull null
                    val isFirstMet = participants.all { earliestEncounterIdPerContact[it.contactId] == encounter.id }
                    val placeLabel = encounter.eventLabel ?: encounter.placeId?.let { placesMap[it]?.label }
                    PastTimelineNode(
                        id = encounter.id,
                        contactId = primary.contactId,
                        contactName = formatParticipantNames(participants.map { it.name }),
                        avatarInitials = primary.initials,
                        avatarBgIndex = primary.bgIndex,
                        occurredAt = encounter.occurredAt,
                        dateLabel = formatPastDateLabel(encounter.occurredAt, today, zoneId),
                        placeLabel = placeLabel,
                        notes = encounter.notes,
                        badge = if (isFirstMet) PastEncounterBadge.FIRST_MET else PastEncounterBadge.MET_AGAIN,
                        photoUri = primary.photoUri,
                        timeLabel = formatPastTimeLabel(encounter.occurredAt, zoneId),
                        participants = participants
                    )
                }

            val completedFollowUpPastNodes = followUps
                .filter { it.isDone && it.resolvedParticipantIds().any { pid -> pid in activeContactIds } }
                .mapNotNull { followUp ->
                    val doneAt = followUp.doneAt ?: return@mapNotNull null
                    val participants = buildParticipantAvatars(followUp.resolvedParticipantIds())
                    val primary = participants.firstOrNull() ?: return@mapNotNull null
                    PastTimelineNode(
                        id = "completed-followup-${followUp.id}",
                        contactId = primary.contactId,
                        contactName = formatParticipantNames(participants.map { it.name }),
                        avatarInitials = primary.initials,
                        avatarBgIndex = primary.bgIndex,
                        occurredAt = doneAt,
                        dateLabel = formatCompletedDateLabel(doneAt, today, zoneId),
                        placeLabel = null,
                        notes = followUp.note.ifBlank { "Scheduled reminder" },
                        badge = PastEncounterBadge.COMPLETED_REMINDER,
                        photoUri = primary.photoUri,
                        timeLabel = formatPastTimeLabel(doneAt, zoneId),
                        participants = participants,
                        followUpId = followUp.id
                    )
                }

            val pastNodes = (encounterPastNodes + completedFollowUpPastNodes)
                .sortedWith(compareByDescending<PastTimelineNode> { it.occurredAt }.thenBy { it.id })

            // 2. Future Nodes: FollowUps + Birthdays across next 12 months & navigated future window
            val futureList = mutableListOf<FutureTimelineNode>()

            followUps
                .filter { !it.isDone && it.resolvedParticipantIds().any { pid -> pid in activeContactIds } }
                .forEach { followUp ->
                    val participants = buildParticipantAvatars(followUp.resolvedParticipantIds())
                    val primary = participants.firstOrNull() ?: return@forEach
                    val dueDate = followUp.dueAt.atZone(zoneId).toLocalDate()
                    val daysUntil = ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0L)
                    futureList += FutureTimelineNode.FollowUpNode(
                        id = "future-followup-${followUp.id}",
                        contactId = primary.contactId,
                        contactName = formatParticipantNames(participants.map { it.name }),
                        avatarInitials = primary.initials,
                        avatarBgIndex = primary.bgIndex,
                        daysUntil = daysUntil,
                        dateLabel = formatFutureDaysLabel(daysUntil),
                        followUpId = followUp.id,
                        note = followUp.note,
                        photoUri = primary.photoUri,
                        targetDate = dueDate,
                        participants = participants
                    )
                }

            val defaultEnd = today.plusYears(1).minusDays(1)
            val seenBirthdayKeys = mutableSetOf<Pair<String, LocalDate>>()
            val candidateYears = buildSet {
                add(today.year)
                add(today.year + 1)
                if (anchorDate.isAfter(today.plusMonths(11))) {
                    add(anchorDate.minusMonths(1).year)
                    add(anchorDate.year)
                    add(anchorDate.plusMonths(1).year)
                }
            }
            val anchorWindowStart = anchorDate.minusMonths(1)
            val anchorWindowEnd = anchorDate.plusMonths(1)

            activeContacts.forEach { contact ->
                val bday = contact.birthday ?: return@forEach
                val bgIndex = avatarIndexByContactId[contact.id] ?: 0
                val initials = extractInitials(contact.name)
                candidateYears.forEach { year ->
                    val targetDate = BirthdayInputUtils.toYearSafely(bday, year)
                    val inDefaultYear = !targetDate.isBefore(today) && !targetDate.isAfter(defaultEnd)
                    val inAnchorFutureWindow = !targetDate.isBefore(today) &&
                        !targetDate.isBefore(anchorWindowStart) &&
                        !targetDate.isAfter(anchorWindowEnd)
                    if ((inDefaultYear || inAnchorFutureWindow) && seenBirthdayKeys.add(contact.id to targetDate)) {
                        val daysUntil = ChronoUnit.DAYS.between(today, targetDate)
                        val turningAge = (targetDate.year - bday.year).takeIf { it in 1..130 && bday.year > 1900 }
                        val dateLabel = if (daysUntil == 0L) "Today!" else formatFutureDaysLabel(daysUntil)
                        futureList += FutureTimelineNode.BirthdayNode(
                            id = "future-bday-${contact.id}-${targetDate}",
                            contactId = contact.id,
                            contactName = contact.name,
                            avatarInitials = initials,
                            avatarBgIndex = bgIndex,
                            daysUntil = daysUntil,
                            dateLabel = dateLabel,
                            turningAge = turningAge,
                            photoUri = contact.photoUri,
                            targetDate = targetDate
                        )
                    }
                }
            }

            val sortedFuture = futureList.sortedWith(
                compareBy<FutureTimelineNode> { it.daysUntil }
                    .thenBy {
                        when (it) {
                            is FutureTimelineNode.BirthdayNode -> 0
                            is FutureTimelineNode.FollowUpNode -> 1
                        }
                    }
                    .thenBy { it.contactName }
            )

            return Pair(sortedFuture, pastNodes)
        }

        fun buildPastBirthdaysForWindow(
            contactsMap: Map<String, Contact>,
            anchorDate: LocalDate,
            today: LocalDate = LocalDate.now()
        ): Map<LocalDate, List<FutureTimelineNode.BirthdayNode>> {
            if (!anchorDate.minusMonths(1).isBefore(today)) return emptyMap()
            val activeContacts = contactsMap.values.filterNot { it.isArchived }
            val avatarIndexByContactId = activeContacts
                .sortedBy { it.id }
                .mapIndexed { idx, c -> c.id to (idx % 6) }
                .toMap()
            val windowStart = anchorDate.minusMonths(1)
            val windowEnd = anchorDate.plusMonths(1)
            val candidateYears = setOf(windowStart.year, anchorDate.year, windowEnd.year)
            val result = mutableMapOf<LocalDate, MutableList<FutureTimelineNode.BirthdayNode>>()

            activeContacts.forEach { contact ->
                val bday = contact.birthday ?: return@forEach
                val bgIndex = avatarIndexByContactId[contact.id] ?: 0
                val initials = extractInitials(contact.name)
                candidateYears.forEach { year ->
                    val targetDate = BirthdayInputUtils.toYearSafely(bday, year)
                    if (targetDate.isBefore(today) && !targetDate.isBefore(windowStart) && !targetDate.isAfter(windowEnd)) {
                        val turningAge = (targetDate.year - bday.year).takeIf { it in 1..130 && bday.year > 1900 }
                        val daysAgo = ChronoUnit.DAYS.between(targetDate, today)
                        val node = FutureTimelineNode.BirthdayNode(
                            id = "past-bday-${contact.id}-${targetDate}",
                            contactId = contact.id,
                            contactName = contact.name,
                            avatarInitials = initials,
                            avatarBgIndex = bgIndex,
                            daysUntil = -daysAgo,
                            dateLabel = formatDayHeaderLabel(targetDate, today),
                            turningAge = turningAge,
                            photoUri = contact.photoUri,
                            targetDate = targetDate
                        )
                        result.getOrPut(targetDate) { mutableListOf() }.add(node)
                    }
                }
            }
            return result
        }

        fun computeActiveEntryDates(
            futureNodes: List<FutureTimelineNode>,
            pastNodes: List<PastTimelineNode>,
            pastBirthdays: Map<LocalDate, List<FutureTimelineNode.BirthdayNode>> = emptyMap(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Set<LocalDate> = buildSet {
            futureNodes.forEach { add(it.targetDate) }
            pastNodes.forEach { add(it.occurredAt.atZone(zoneId).toLocalDate()) }
            addAll(pastBirthdays.keys)
        }

        fun computeRecurringBirthdayMonthDays(contactsMap: Map<String, Contact>): Set<MonthDay> =
            contactsMap.values
                .filterNot { it.isArchived }
                .mapNotNull { it.birthday?.let { b -> MonthDay.of(b.monthValue, b.dayOfMonth) } }
                .toSet()

        fun hasTimelineEntryOnDate(
            date: LocalDate,
            activeEntryDates: Set<LocalDate>,
            recurringBirthdays: Set<MonthDay>
        ): Boolean {
            if (date in activeEntryDates) return true
            if (MonthDay.of(date.monthValue, date.dayOfMonth) in recurringBirthdays) return true
            if (date.monthValue == 2 && date.dayOfMonth == 28 && !Year.isLeap(date.year.toLong())) {
                return MonthDay.of(2, 29) in recurringBirthdays
            }
            return false
        }

        private fun extractInitials(name: String): String = extractAlphanumericInitials(name)

        fun formatFutureDaysLabel(daysUntil: Long): String = when {
            daysUntil <= 0L -> "Today"
            daysUntil == 1L -> "Tomorrow"
            else -> "In $daysUntil days"
        }

        fun formatPastDateLabel(
            instant: Instant,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): String {
            val date = instant.atZone(zoneId).toLocalDate()
            val daysBetween = ChronoUnit.DAYS.between(date, today)
            return when {
                daysBetween <= 0L -> "Met today"
                daysBetween == 1L -> "Met yesterday"
                daysBetween in 2..6 -> "$daysBetween days ago"
                date.year == today.year -> date.format(sameYearDayFormatter)
                else -> date.format(crossYearDayFormatter)
            }
        }

        fun formatCompletedDateLabel(
            instant: Instant,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): String {
            val date = instant.atZone(zoneId).toLocalDate()
            val daysBetween = ChronoUnit.DAYS.between(date, today)
            return when {
                daysBetween <= 0L -> "Completed today"
                daysBetween == 1L -> "Completed yesterday"
                daysBetween in 2..6 -> "Completed $daysBetween days ago"
                date.year == today.year -> "Completed ${date.format(sameYearDayFormatter)}"
                else -> "Completed ${date.format(crossYearDayFormatter)}"
            }
        }

        fun formatTodayDividerLabel(today: LocalDate = LocalDate.now()): String {
            val formatted = today.format(sameYearDayFormatter).uppercase()
            return "TODAY, $formatted"
        }

        private val sameYearDayFormatter = DateTimeFormatter.ofPattern("MMM d")
        private val crossYearDayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        private val timeOfDayFormatter = DateTimeFormatter.ofPattern("h:mm a")
        private val headerSameYearFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
        private val headerCrossYearFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

        fun formatCalendarHeaderTitle(
            selectedDate: LocalDate,
            today: LocalDate = LocalDate.now()
        ): String {
            val formatter = if (selectedDate.year == today.year) {
                headerSameYearFormatter
            } else {
                headerCrossYearFormatter
            }
            return selectedDate.format(formatter)
        }

        fun formatDayHeaderLabel(
            date: LocalDate,
            today: LocalDate = LocalDate.now()
        ): String {
            val formattedDate = date.format(sameYearDayFormatter)
            return when (date) {
                today -> "Today, $formattedDate"
                today.minusDays(1) -> "Yesterday, $formattedDate"
                today.plusDays(1) -> "Tomorrow, $formattedDate"
                else -> if (date.year == today.year) {
                    formattedDate
                } else {
                    date.format(crossYearDayFormatter)
                }
            }
        }

        fun formatPastTimeLabel(
            instant: Instant,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): String = instant.atZone(zoneId).toLocalTime().format(timeOfDayFormatter)

        fun groupFutureNodesByDay(
            nodes: List<FutureTimelineNode>,
            today: LocalDate = LocalDate.now()
        ): List<TimelineDayGroup<FutureTimelineNode>> {
            if (nodes.isEmpty()) return emptyList()
            return nodes
                .groupBy { it.targetDate }
                .entries
                .sortedByDescending { it.key }
                .map { (date, items) ->
                    TimelineDayGroup(
                        date = date,
                        headerLabel = formatDayHeaderLabel(date, today),
                        items = items
                    )
                }
        }

        fun groupPastNodesByDay(
            nodes: List<PastTimelineNode>,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<TimelineDayGroup<PastTimelineNode>> {
            if (nodes.isEmpty()) return emptyList()
            return nodes
                .groupBy { it.occurredAt.atZone(zoneId).toLocalDate() }
                .entries
                .sortedByDescending { it.key }
                .map { (date, items) ->
                    TimelineDayGroup(
                        date = date,
                        headerLabel = formatDayHeaderLabel(date, today),
                        items = items
                    )
                }
        }
    }
}

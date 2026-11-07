package com.example.hello.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.extractAlphanumericInitials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TimelineViewModel(
    val repository: ContactRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                combine(
                    repository.fullContacts,
                    repository.encounters,
                    repository.places,
                    repository.followUps
                ) { contactsMap, encounters, places, followUps ->
                    buildTimelineNodes(
                        contactsMap = contactsMap,
                        encounters = encounters,
                        placesMap = places.associateBy { it.id },
                        followUps = followUps
                    )
                }.collect { (futureNodes, pastNodes) ->
                    _uiState.update { state ->
                        val allNodeIds = (futureNodes.map { it.id } + pastNodes.map { it.id }).toSet()
                        val prunedSelected = state.selectedNodeIds.filter { it in allNodeIds }.toSet()
                        state.copy(
                            futureNodes = futureNodes,
                            pastNodes = pastNodes,
                            selectedNodeIds = prunedSelected,
                            todayLabel = formatTodayDividerLabel(LocalDate.now())
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: TimelineUiEvent) {
        when (event) {
            is TimelineUiEvent.OnSelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
            is TimelineUiEvent.OnMarkFollowUpDone -> {
                repository?.markFollowUpDone(event.followUpId)
                _uiState.update { state ->
                    state.copy(
                        futureNodes = state.futureNodes.filterNot { node ->
                            node is FutureTimelineNode.FollowUpNode && node.followUpId == event.followUpId
                        },
                        userMessage = "Follow-up marked as done!"
                    )
                }
            }
            is TimelineUiEvent.OnSnoozeFollowUp -> {
                repository?.snoozeFollowUp(event.followUpId)
                _uiState.update { state ->
                    val updatedFuture = state.futureNodes.map { node ->
                        if (node is FutureTimelineNode.FollowUpNode && node.followUpId == event.followUpId) {
                            val newDays = (node.daysUntil + 3).coerceAtLeast(3)
                            node.copy(
                                daysUntil = newDays,
                                dateLabel = formatFutureDaysLabel(newDays)
                            )
                        } else {
                            node
                        }
                    }.sortedBy { it.daysUntil }
                    state.copy(
                        futureNodes = updatedFuture,
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
                    val visibleSelectable = getVisibleSelectableNodeIds(state)
                    state.copy(selectedNodeIds = visibleSelectable)
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

                val encounterIds = state.pastNodes.map { it.id }.filter { it in selected }.toSet()
                val followUpMap = state.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>()
                    .associate { it.id to it.followUpId }
                val followUpIds = followUpMap.filterKeys { it in selected }.values.toSet()

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
                    current.copy(
                        futureNodes = if (repository != null) current.futureNodes else remainingFuture,
                        pastNodes = if (repository != null) current.pastNodes else remainingPast,
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

    private fun getVisibleSelectableNodeIds(state: TimelineUiState): Set<String> {
        val pastIds = if (state.selectedFilter == TimelineFilter.ALL || state.selectedFilter == TimelineFilter.PAST) {
            state.pastNodes.map { it.id }
        } else emptyList()

        val futureIds = if (state.selectedFilter == TimelineFilter.ALL || state.selectedFilter == TimelineFilter.FUTURE) {
            state.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>().map { it.id }
        } else emptyList()

        return (pastIds + futureIds).toSet()
    }

    companion object {
        fun createInitialState(today: LocalDate = LocalDate.now()): TimelineUiState {
            val contactsMap = MockDataProvider.contacts.associateBy { it.id }
            val placesMap = MockDataProvider.places.associateBy { it.id }
            val (futureNodes, pastNodes) = buildTimelineNodes(
                contactsMap = contactsMap,
                encounters = MockDataProvider.encounters,
                placesMap = placesMap,
                followUps = MockDataProvider.followUps,
                today = today
            )
            return TimelineUiState(
                selectedFilter = TimelineFilter.ALL,
                futureNodes = futureNodes,
                pastNodes = pastNodes,
                todayLabel = formatTodayDividerLabel(today)
            )
        }

        fun buildTimelineNodes(
            contactsMap: Map<String, Contact>,
            encounters: List<Encounter>,
            placesMap: Map<String, Place>,
            followUps: List<FollowUp>,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Pair<List<FutureTimelineNode>, List<PastTimelineNode>> {
            val activeContacts = contactsMap.values.filterNot { it.isArchived }
            val activeContactIds = activeContacts.map { it.id }.toSet()
            val avatarIndexByContactId = activeContacts
                .sortedBy { it.id }
                .mapIndexed { idx, c -> c.id to (idx % 6) }
                .toMap()

            // 1. Past Nodes: Determine earliest chronological encounter per Contact -> FIRST_MET vs MET_AGAIN
            val validEncounters = encounters.filter { it.contactId in activeContactIds }
            val earliestEncounterIdPerContact = validEncounters
                .groupBy { it.contactId }
                .mapValues { (_, list) ->
                    list.minWithOrNull(compareBy<Encounter> { it.occurredAt }.thenBy { it.id })?.id
                }

            val pastNodes = validEncounters
                .sortedWith(compareByDescending<Encounter> { it.occurredAt }.thenBy { it.id })
                .mapNotNull { encounter ->
                    val contact = contactsMap[encounter.contactId] ?: return@mapNotNull null
                    val isFirstMet = earliestEncounterIdPerContact[encounter.contactId] == encounter.id
                    val placeLabel = encounter.eventLabel ?: encounter.placeId?.let { placesMap[it]?.label }
                    PastTimelineNode(
                        id = encounter.id,
                        contactId = contact.id,
                        contactName = contact.name,
                        avatarInitials = extractInitials(contact.name),
                        avatarBgIndex = avatarIndexByContactId[contact.id] ?: 0,
                        occurredAt = encounter.occurredAt,
                        dateLabel = formatPastDateLabel(encounter.occurredAt, today, zoneId),
                        placeLabel = placeLabel,
                        notes = encounter.notes,
                        badge = if (isFirstMet) PastEncounterBadge.FIRST_MET else PastEncounterBadge.MET_AGAIN,
                        photoUri = contact.photoUri
                    )
                }

            // 2. Future Nodes: Soonest-first (FollowUps + Birthdays within 60d + Projected Cadence Reconnections)
            val futureList = mutableListOf<FutureTimelineNode>()

            // 2a. Pending FollowUps
            followUps
                .filter { !it.isDone && it.contactId in activeContactIds }
                .forEach { followUp ->
                    val contact = contactsMap[followUp.contactId] ?: return@forEach
                    val dueDate = followUp.dueAt.atZone(zoneId).toLocalDate()
                    val daysUntil = ChronoUnit.DAYS.between(today, dueDate).coerceAtLeast(0L)
                    futureList += FutureTimelineNode.FollowUpNode(
                        id = "future-followup-${followUp.id}",
                        contactId = contact.id,
                        contactName = contact.name,
                        avatarInitials = extractInitials(contact.name),
                        avatarBgIndex = avatarIndexByContactId[contact.id] ?: 0,
                        daysUntil = daysUntil,
                        dateLabel = formatFutureDaysLabel(daysUntil),
                        followUpId = followUp.id,
                        note = followUp.note,
                        photoUri = contact.photoUri
                    )
                }

            // 2b. Set-in-Stone Upcoming Birthdays (365-day year-round horizon)
            activeContacts.forEach { contact ->
                val bday = contact.birthday ?: return@forEach
                var nextBday = bday.withYear(today.year)
                if (nextBday.isBefore(today)) {
                    nextBday = bday.withYear(today.year + 1)
                }
                val daysUntil = ChronoUnit.DAYS.between(today, nextBday)
                if (daysUntil in 0..365) {
                    val turningAge = (nextBday.year - bday.year).takeIf { it in 1..130 }
                    val formattedMonthDay = nextBday.format(DateTimeFormatter.ofPattern("MMM d"))
                    val dateLabel = when {
                        daysUntil == 0L -> "Today!"
                        daysUntil <= 30L -> formatFutureDaysLabel(daysUntil)
                        else -> "$formattedMonthDay • In $daysUntil days"
                    }
                    futureList += FutureTimelineNode.BirthdayNode(
                        id = "future-bday-${contact.id}",
                        contactId = contact.id,
                        contactName = contact.name,
                        avatarInitials = extractInitials(contact.name),
                        avatarBgIndex = avatarIndexByContactId[contact.id] ?: 0,
                        daysUntil = daysUntil,
                        dateLabel = dateLabel,
                        turningAge = turningAge,
                        photoUri = contact.photoUri
                    )
                }
            }

            // Order Future Horizon soonest-first (ADR-0014)
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
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
                else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }

        fun formatTodayDividerLabel(today: LocalDate = LocalDate.now()): String {
            val formatted = today.format(DateTimeFormatter.ofPattern("MMM d")).uppercase()
            return "TODAY · $formatted"
        }
    }
}

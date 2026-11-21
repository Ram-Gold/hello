package com.example.hello.ui.timeline

import java.time.Instant
import java.time.LocalDate

enum class TimelineFilter(val label: String) {
    ALL("All"),
    FUTURE("Future"),
    PAST("Past")
}

enum class PastEncounterBadge(val label: String) {
    FIRST_MET("First Met"),
    MET_AGAIN("Met Again")
}

sealed interface FutureTimelineNode {
    val id: String
    val contactId: String
    val contactName: String
    val avatarInitials: String
    val avatarBgIndex: Int
    val daysUntil: Long
    val dateLabel: String
    val photoUri: String?
    val targetDate: LocalDate

    data class FollowUpNode(
        override val id: String,
        override val contactId: String,
        override val contactName: String,
        override val avatarInitials: String,
        override val avatarBgIndex: Int,
        override val daysUntil: Long,
        override val dateLabel: String,
        val followUpId: String,
        val note: String,
        override val photoUri: String? = null,
        override val targetDate: LocalDate = LocalDate.now().plusDays(daysUntil.coerceAtLeast(0L))
    ) : FutureTimelineNode

    data class BirthdayNode(
        override val id: String,
        override val contactId: String,
        override val contactName: String,
        override val avatarInitials: String,
        override val avatarBgIndex: Int,
        override val daysUntil: Long,
        override val dateLabel: String,
        val turningAge: Int? = null,
        override val photoUri: String? = null,
        override val targetDate: LocalDate = LocalDate.now().plusDays(daysUntil.coerceAtLeast(0L))
    ) : FutureTimelineNode
}

data class PastTimelineNode(
    val id: String,
    val contactId: String,
    val contactName: String,
    val avatarInitials: String,
    val avatarBgIndex: Int,
    val occurredAt: Instant,
    val dateLabel: String,
    val placeLabel: String?,
    val notes: String,
    val badge: PastEncounterBadge,
    val photoUri: String? = null,
    val timeLabel: String = ""
)

data class TimelineDayGroup<T>(
    val date: LocalDate,
    val headerLabel: String,
    val items: List<T>
)

data class TimelineUiState(
    val selectedFilter: TimelineFilter = TimelineFilter.ALL,
    val futureNodes: List<FutureTimelineNode> = emptyList(),
    val pastNodes: List<PastTimelineNode> = emptyList(),
    val futureDayGroups: List<TimelineDayGroup<FutureTimelineNode>> = emptyList(),
    val pastDayGroups: List<TimelineDayGroup<PastTimelineNode>> = emptyList(),
    val todayLabel: String = "TODAY",
    val userMessage: String? = null,
    val selectedNodeIds: Set<String> = emptySet(),
    val isDeleteConfirmationVisible: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedNodeIds.isNotEmpty()
}

sealed interface TimelineUiEvent {
    data class OnSelectFilter(val filter: TimelineFilter) : TimelineUiEvent
    data class OnMarkFollowUpDone(val followUpId: String) : TimelineUiEvent
    data class OnSnoozeFollowUp(val followUpId: String) : TimelineUiEvent
    data object OnDismissUserMessage : TimelineUiEvent
    data class OnNodeLongClick(val nodeId: String) : TimelineUiEvent
    data class OnToggleSelectNode(val nodeId: String) : TimelineUiEvent
    data object OnClearSelection : TimelineUiEvent
    data object OnSelectAllVisibleNodes : TimelineUiEvent
    data object OnRequestDeleteSelected : TimelineUiEvent
    data object OnConfirmDeleteSelected : TimelineUiEvent
    data object OnDismissDeleteDialog : TimelineUiEvent
}
